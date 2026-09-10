package com.igarciamen.messages.controller;

import com.igarciamen.messages.payloads.request.SendMessageRequest;
import com.igarciamen.messages.payloads.response.ConversationSummaryResponse;
import com.igarciamen.messages.payloads.response.MessageResponse;
import com.igarciamen.messages.payloads.response.ThreadResponse;
import com.igarciamen.messages.service.MessagingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessagingController {

    private final MessagingService messagingService;

    public MessagingController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Operation(
            summary = "Gets the message thread for a project+investor pair (that investor, the project's promoter, or an admin)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/projects/{projectId}/investors/{investorId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ThreadResponse> thread(@AuthenticationPrincipal Jwt jwt, Authentication authentication,
                                                  @PathVariable Long projectId, @PathVariable Long investorId) {
        return ResponseEntity.ok(messagingService.getThread(projectId, investorId, extractUserId(jwt), isInvestor(authentication)));
    }

    @Operation(
            summary = "Sends a message in the thread for a project+investor pair (that investor, the project's promoter, or an admin)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(path = "/projects/{projectId}/investors/{investorId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageResponse> send(@AuthenticationPrincipal Jwt jwt, Authentication authentication,
                                                 @PathVariable Long projectId, @PathVariable Long investorId,
                                                 @Valid @RequestBody SendMessageRequest req) {
        String role = isInvestor(authentication) ? "ROLE_INVESTOR" : "ROLE_PROMOTER";
        MessageResponse saved = messagingService.sendMessage(projectId, investorId, extractUserId(jwt),
                isInvestor(authentication), role, req.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(
            summary = "Marks the other side's messages in this thread as read",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping(path = "/projects/{projectId}/investors/{investorId}/read", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> markAsRead(@AuthenticationPrincipal Jwt jwt, Authentication authentication,
                                                            @PathVariable Long projectId, @PathVariable Long investorId) {
        int updated = messagingService.markAsRead(projectId, investorId, extractUserId(jwt), isInvestor(authentication));
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    @Operation(
            summary = "Number of unread messages in this thread, for the caller",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/projects/{projectId}/investors/{investorId}/unread-count", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> unreadCount(@AuthenticationPrincipal Jwt jwt, Authentication authentication,
                                                          @PathVariable Long projectId, @PathVariable Long investorId) {
        long count = messagingService.unreadCount(projectId, investorId, extractUserId(jwt), isInvestor(authentication));
        return ResponseEntity.ok(Map.of("count", count));
    }

    @Operation(
            summary = "Lists every investor thread open on a project (its owning promoter, or an admin)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/projects/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ConversationSummaryResponse>> conversationsForProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(messagingService.listConversationsForProject(projectId));
    }

    private Long extractUserId(Jwt jwt) {
        Object claim = jwt.getClaim("userId");
        if (claim == null) {
            throw new IllegalStateException("The token does not contain the 'userId' claim");
        }
        return ((Number) claim).longValue();
    }

    private boolean isInvestor(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_INVESTOR"));
    }
}
