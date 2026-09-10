package com.igarciamen.messages.service;

import com.igarciamen.messages.model.Conversation;
import com.igarciamen.messages.model.Message;
import com.igarciamen.messages.repository.ConversationRepository;
import com.igarciamen.messages.repository.MessageRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessagingServiceTest {

    @Mock private ConversationRepository convRepo;
    @Mock private MessageRepository msgRepo;
    @Mock private ProjectClient projectClient;

    @InjectMocks private MessagingService messagingService;

    @Test
    void getThread_theInvestorCanOpenTheirOwnThreadOnAnApprovedProject() {
        doNothing().when(projectClient).verifyApprovedOrThrow(1L);
        Conversation conv = new Conversation(1L, 20L);
        conv.setId(100L);
        when(convRepo.findByProjectIdAndInvestorId(1L, 20L)).thenReturn(Optional.of(conv));
        when(msgRepo.findByConversationIdOrderByCreatedAtAsc(100L)).thenReturn(List.of());

        var result = messagingService.getThread(1L, 20L, 20L, true);

        assertEquals(1L, result.getProjectId());
        assertEquals(20L, result.getInvestorId());
    }

    @Test
    void getThread_investorCannotOpenAnotherInvestorsThread() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> messagingService.getThread(1L, 20L, 999L, true));

        assertEquals(403, ex.getStatusCode().value());
        verifyNoInteractions(convRepo, msgRepo);
    }

    @Test
    void getThread_investorCannotOpenAThreadOnANonApprovedProject() {
        doThrow(new EntityNotFoundException("not found")).when(projectClient).verifyApprovedOrThrow(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> messagingService.getThread(1L, 20L, 20L, true));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void getThread_thePromoterCreatesTheConversationOnFirstRead() {
        doNothing().when(projectClient).verifyAccessOrThrow(1L);
        when(convRepo.findByProjectIdAndInvestorId(1L, 20L)).thenReturn(Optional.empty());
        when(convRepo.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });
        when(msgRepo.findByConversationIdOrderByCreatedAtAsc(100L)).thenReturn(List.of());

        var result = messagingService.getThread(1L, 20L, 10L, false);

        assertEquals(0, result.getMessages().size());
        verify(convRepo).save(any(Conversation.class));
        System.out.println("=== getThread: conversation auto-created on first access ===");
    }

    @Test
    void getThread_promoterDeniedByProjectsPropagatesAs403() {
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN))
                .when(projectClient).verifyAccessOrThrow(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> messagingService.getThread(1L, 20L, 999L, false));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void sendMessage_investorSendsAMessageInTheirOwnThread() {
        doNothing().when(projectClient).verifyApprovedOrThrow(1L);
        Conversation conv = new Conversation(1L, 20L);
        conv.setId(100L);
        when(convRepo.findByProjectIdAndInvestorId(1L, 20L)).thenReturn(Optional.of(conv));
        when(msgRepo.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        var result = messagingService.sendMessage(1L, 20L, 20L, true, "ROLE_INVESTOR", "Hello, I'm interested!");

        assertEquals("Hello, I'm interested!", result.getContent());
        assertEquals("ROLE_INVESTOR", result.getSenderRole());
        assertEquals(20L, result.getSenderId());
        System.out.println("=== sendMessage: investor message saved ===");
    }

    @Test
    void sendMessage_promoterReplies() {
        doNothing().when(projectClient).verifyAccessOrThrow(1L);
        Conversation conv = new Conversation(1L, 20L);
        conv.setId(100L);
        when(convRepo.findByProjectIdAndInvestorId(1L, 20L)).thenReturn(Optional.of(conv));
        when(msgRepo.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(2L);
            return m;
        });

        var result = messagingService.sendMessage(1L, 20L, 10L, false, "ROLE_PROMOTER", "Thanks for your interest!");

        assertEquals("ROLE_PROMOTER", result.getSenderRole());
        assertEquals(10L, result.getSenderId());
    }

    @Test
    void listConversationsForProject_requiresAccessAndReturnsEveryThread() {
        doNothing().when(projectClient).verifyAccessOrThrow(1L);
        Conversation c1 = new Conversation(1L, 20L);
        Conversation c2 = new Conversation(1L, 30L);
        when(convRepo.findByProjectIdOrderByLastMessageAtDesc(1L)).thenReturn(List.of(c1, c2));

        var result = messagingService.listConversationsForProject(1L);

        assertEquals(2, result.size());
        verify(projectClient).verifyAccessOrThrow(1L);
    }

    @Test
    void unreadCount_returnsZeroWhenNoConversationExistsYet() {
        doNothing().when(projectClient).verifyAccessOrThrow(1L);
        when(convRepo.findByProjectIdAndInvestorId(1L, 20L)).thenReturn(Optional.empty());

        long count = messagingService.unreadCount(1L, 20L, 10L, false);

        assertEquals(0, count);
        verifyNoInteractions(msgRepo);
    }
}
