package com.igarciamen.messages.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// One conversation per (project, investor) PAIR -- not per project alone.
// This is the key adaptation versus the SecGest reference (messages/Conversation),
// where a task has at most one client and one admin, so task_id alone was
// enough to identify the thread. Here, a single APPROVED project can receive
// interest from several different investors, each of whom needs their own
// private thread with the promoter -- hence the composite unique constraint.
@Entity
@Table(
        name = "conversations",
        schema = "public",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "investor_id"})
)
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    // The promoter's id is NOT stored here: it is resolved via ProjectClient
    // whenever needed (same principle as SecGest resolving the admin via
    // TaskClient) -- projects is the single source of truth for who owns a
    // project, this microservice never duplicates that.
    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Message> messages = new ArrayList<>();

    public Conversation() {}

    public Conversation(Long projectId, Long investorId) {
        this.projectId = projectId;
        this.investorId = investorId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getInvestorId() { return investorId; }
    public void setInvestorId(Long investorId) { this.investorId = investorId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }
    public List<Message> getMessages() { return messages; }
}
