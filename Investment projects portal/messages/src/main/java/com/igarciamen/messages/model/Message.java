package com.igarciamen.messages.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages", schema = "public")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id")
    @JsonIgnore
    private Conversation conversation;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    // "ROLE_PROMOTER" or "ROLE_INVESTOR": lets the frontend render the bubble
    // on the correct side without resolving anything else.
    @Column(name = "sender_role", nullable = false, length = 20)
    private String senderRole;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public Message() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
