package com.igarciamen.projects.payloads.response;

import com.igarciamen.projects.model.Project;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProjectResponse {

    private Long id;
    private Long promoterId;
    private Long sectorId;
    private String title;
    private String description;
    private String country;
    private BigDecimal requestedAmount;
    private String status;
    private String rejectionReason;
    private LocalDateTime submittedAt;
    private LocalDateTime evaluationDeadline;
    private LocalDateTime createdAt;

    public ProjectResponse() {}

    public ProjectResponse(Long id, Long promoterId, Long sectorId, String title, String description,
                           String country, BigDecimal requestedAmount, String status, String rejectionReason,
                           LocalDateTime submittedAt, LocalDateTime evaluationDeadline, LocalDateTime createdAt) {
        this.id = id;
        this.promoterId = promoterId;
        this.sectorId = sectorId;
        this.title = title;
        this.description = description;
        this.country = country;
        this.requestedAmount = requestedAmount;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.submittedAt = submittedAt;
        this.evaluationDeadline = evaluationDeadline;
        this.createdAt = createdAt;
    }

    public static ProjectResponse from(Project p) {
        return new ProjectResponse(
                p.getId(),
                p.getPromoterId(),
                p.getSectorId(),
                p.getTitle(),
                p.getDescription(),
                p.getCountry(),
                p.getRequestedAmount(),
                p.getStatus().name(),
                p.getRejectionReason(),
                p.getSubmittedAt(),
                p.getEvaluationDeadline(),
                p.getCreatedAt()
        );
    }

    public Long getId() { return id; }
    public Long getPromoterId() { return promoterId; }
    public Long getSectorId() { return sectorId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCountry() { return country; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public String getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getEvaluationDeadline() { return evaluationDeadline; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
