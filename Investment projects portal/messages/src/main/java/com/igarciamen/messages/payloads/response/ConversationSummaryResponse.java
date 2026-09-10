package com.igarciamen.messages.payloads.response;

import com.igarciamen.messages.model.Conversation;

import java.time.LocalDateTime;

public class ConversationSummaryResponse {

    private Long projectId;
    private Long investorId;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;

    public ConversationSummaryResponse() {}

    public ConversationSummaryResponse(Long projectId, Long investorId, LocalDateTime createdAt, LocalDateTime lastMessageAt) {
        this.projectId = projectId;
        this.investorId = investorId;
        this.createdAt = createdAt;
        this.lastMessageAt = lastMessageAt;
    }

    public static ConversationSummaryResponse from(Conversation c) {
        return new ConversationSummaryResponse(c.getProjectId(), c.getInvestorId(), c.getCreatedAt(), c.getLastMessageAt());
    }

    public Long getProjectId() { return projectId; }
    public Long getInvestorId() { return investorId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
}
