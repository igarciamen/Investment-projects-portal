package com.igarciamen.messages.payloads.response;

import com.igarciamen.messages.model.Message;

import java.time.LocalDateTime;

public class MessageResponse {

    private Long id;
    private Long senderId;
    private String senderRole;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public MessageResponse() {}

    public MessageResponse(Long id, Long senderId, String senderRole, String content,
                           LocalDateTime createdAt, LocalDateTime readAt) {
        this.id = id;
        this.senderId = senderId;
        this.senderRole = senderRole;
        this.content = content;
        this.createdAt = createdAt;
        this.readAt = readAt;
    }

    public static MessageResponse from(Message m) {
        return new MessageResponse(m.getId(), m.getSenderId(), m.getSenderRole(), m.getContent(),
                m.getCreatedAt(), m.getReadAt());
    }

    public Long getId() { return id; }
    public Long getSenderId() { return senderId; }
    public String getSenderRole() { return senderRole; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReadAt() { return readAt; }
}
