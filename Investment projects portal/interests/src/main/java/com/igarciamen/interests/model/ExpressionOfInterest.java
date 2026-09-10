package com.igarciamen.interests.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// An investor's expression of interest in a project. This is deliberately
// lightweight -- InvestEU's real EIPP portal only registers interest and
// facilitates first contact (see messages, the next microservice); it does
// NOT process any financial commitment. One investor can express interest
// in the same project only once (see the unique constraint).
@Entity
@Table(
        name = "expressions_of_interest",
        schema = "public",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "investor_id"})
)
public class ExpressionOfInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    @Column(length = 1000)
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ExpressionOfInterest() {}

    public ExpressionOfInterest(Long projectId, Long investorId, String message) {
        this.projectId = projectId;
        this.investorId = investorId;
        this.message = message;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getInvestorId() { return investorId; }
    public void setInvestorId(Long investorId) { this.investorId = investorId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
