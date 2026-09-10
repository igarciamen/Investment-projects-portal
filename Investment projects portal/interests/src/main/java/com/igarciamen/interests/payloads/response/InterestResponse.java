package com.igarciamen.interests.payloads.response;

import com.igarciamen.interests.model.ExpressionOfInterest;

import java.time.LocalDateTime;

public class InterestResponse {

    private Long id;
    private Long projectId;
    private Long investorId;
    private String message;
    private LocalDateTime createdAt;

    public InterestResponse() {}

    public InterestResponse(Long id, Long projectId, Long investorId, String message, LocalDateTime createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.investorId = investorId;
        this.message = message;
        this.createdAt = createdAt;
    }

    public static InterestResponse from(ExpressionOfInterest e) {
        return new InterestResponse(e.getId(), e.getProjectId(), e.getInvestorId(), e.getMessage(), e.getCreatedAt());
    }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public Long getInvestorId() { return investorId; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
