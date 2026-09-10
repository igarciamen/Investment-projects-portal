package com.igarciamen.messages.service;

import com.igarciamen.messages.model.Conversation;
import com.igarciamen.messages.model.Message;
import com.igarciamen.messages.payloads.response.ConversationSummaryResponse;
import com.igarciamen.messages.payloads.response.MessageResponse;
import com.igarciamen.messages.payloads.response.ThreadResponse;
import com.igarciamen.messages.repository.ConversationRepository;
import com.igarciamen.messages.repository.MessageRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class MessagingService {

    private static final Logger log = LoggerFactory.getLogger(MessagingService.class);

    private final ConversationRepository convRepo;
    private final MessageRepository msgRepo;
    private final ProjectClient projectClient;
    private final UserClient userClient;
    private final EmailClient emailClient;

    public MessagingService(ConversationRepository convRepo, MessageRepository msgRepo, ProjectClient projectClient,
                            UserClient userClient, EmailClient emailClient) {
        this.convRepo = convRepo;
        this.msgRepo = msgRepo;
        this.projectClient = projectClient;
        this.userClient = userClient;
        this.emailClient = emailClient;
    }

    // No explicit "start conversation": it is created the first time it is
    // needed (first read of the thread, or first message sent).
    private Conversation getOrCreateConversation(Long projectId, Long investorId) {
        return convRepo.findByProjectIdAndInvestorId(projectId, investorId)
                .orElseGet(() -> convRepo.save(new Conversation(projectId, investorId)));
    }

    // Checks that the caller is allowed to use this specific (project, investor)
    // thread: either the investor themselves, or the project's owning promoter
    // (or an admin) -- verified by delegating to projects, never duplicated here.
    private void checkAccess(Long projectId, Long investorId, Long callerId, boolean callerIsInvestor) {
        if (callerIsInvestor) {
            if (!Objects.equals(callerId, investorId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your conversation");
            }
            try {
                projectClient.verifyApprovedOrThrow(projectId);
            } catch (EntityNotFoundException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Approved project not found: " + projectId);
            }
        } else {
            projectClient.verifyAccessOrThrow(projectId);
        }
    }

    public ThreadResponse getThread(Long projectId, Long investorId, Long callerId, boolean callerIsInvestor) {
        checkAccess(projectId, investorId, callerId, callerIsInvestor);
        Conversation conversation = getOrCreateConversation(projectId, investorId);

        List<MessageResponse> messages = msgRepo.findByConversationIdOrderByCreatedAtAsc(conversation.getId())
                .stream().map(MessageResponse::from).toList();

        return new ThreadResponse(projectId, investorId, messages);
    }

    public MessageResponse sendMessage(Long projectId, Long investorId, Long callerId, boolean callerIsInvestor,
                                       String senderRole, String content) {
        checkAccess(projectId, investorId, callerId, callerIsInvestor);
        Conversation conversation = getOrCreateConversation(projectId, investorId);

        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderId(callerId);
        message.setSenderRole(senderRole);
        message.setContent(content.trim());
        message = msgRepo.save(message);

        conversation.setLastMessageAt(LocalDateTime.now());

        notifyTheOtherParty(projectId, investorId, callerIsInvestor);

        return MessageResponse.from(message);
    }

    // Notifies whoever did NOT send this message. A failure here (users or
    // notifications unreachable, no email on file, etc.) is deliberately
    // swallowed: the message itself is already persisted by the time this
    // runs, and a notification problem must never turn into a failed
    // request for someone who just sent a legitimate message.
    private void notifyTheOtherParty(Long projectId, Long investorId, boolean senderIsInvestor) {
        try {
            Long recipientId;
            if (senderIsInvestor) {
                // Uses the PUBLIC endpoint here -- the caller is the
                // investor, who has no access to the protected
                // GET /api/projects/{id}. Using that one here (as an
                // earlier version of this method did) caused a real 403,
                // silently swallowed by this same try/catch: the email
                // never went out, but nothing visibly failed either.
                Map<String, Object> project = projectClient.fetchApprovedProjectOrThrow(projectId);
                Object promoterId = project.get("promoterId");
                if (promoterId == null) {
                    log.warn("Could not notify: project {} has no promoterId", projectId);
                    return;
                }
                recipientId = ((Number) promoterId).longValue();
            } else {
                recipientId = investorId;
            }

            var user = userClient.fetchUserOrThrow(recipientId);
            Object email = user.get("email");
            if (email == null) {
                log.warn("Could not notify user {}: no email returned by users", recipientId);
                return;
            }

            emailClient.sendGenericEmail(email.toString(), "New message on InvestEU",
                    "You have received a new message about project " + projectId + ". Log in to the portal to read and reply.");
        } catch (Exception e) {
            log.warn("Failed to notify the other party in project {} / investor {}: {}", projectId, investorId, e.getMessage());
        }
    }

    public int markAsRead(Long projectId, Long investorId, Long callerId, boolean callerIsInvestor) {
        checkAccess(projectId, investorId, callerId, callerIsInvestor);
        Conversation conversation = convRepo.findByProjectIdAndInvestorId(projectId, investorId).orElse(null);
        if (conversation == null) {
            return 0;
        }
        return msgRepo.markAsRead(conversation.getId(), callerId, LocalDateTime.now());
    }

    public long unreadCount(Long projectId, Long investorId, Long callerId, boolean callerIsInvestor) {
        checkAccess(projectId, investorId, callerId, callerIsInvestor);
        Conversation conversation = convRepo.findByProjectIdAndInvestorId(projectId, investorId).orElse(null);
        if (conversation == null) {
            return 0;
        }
        return msgRepo.countUnread(conversation.getId(), callerId);
    }

    // Promoter/admin only: every investor thread open on a project.
    public List<ConversationSummaryResponse> listConversationsForProject(Long projectId) {
        projectClient.verifyAccessOrThrow(projectId);
        return convRepo.findByProjectIdOrderByLastMessageAtDesc(projectId).stream()
                .map(ConversationSummaryResponse::from)
                .toList();
    }
}