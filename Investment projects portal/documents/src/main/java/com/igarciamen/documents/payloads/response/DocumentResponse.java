package com.igarciamen.documents.payloads.response;

import com.igarciamen.documents.model.Document;

import java.time.LocalDateTime;

public class DocumentResponse {

    private Long id;
    private Long projectId;
    private Long uploaderUserId;
    private String uploaderRole;
    private String documentType;
    private String originalFilename;
    private String contentType;
    private long sizeBytes;
    private LocalDateTime uploadedAt;

    public DocumentResponse() {}

    public DocumentResponse(Long id, Long projectId, Long uploaderUserId, String uploaderRole, String documentType,
                             String originalFilename, String contentType, long sizeBytes, LocalDateTime uploadedAt) {
        this.id = id;
        this.projectId = projectId;
        this.uploaderUserId = uploaderUserId;
        this.uploaderRole = uploaderRole;
        this.documentType = documentType;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploadedAt = uploadedAt;
    }

    public static DocumentResponse from(Document d) {
        return new DocumentResponse(d.getId(), d.getProjectId(), d.getUploaderUserId(), d.getUploaderRole(),
                d.getDocumentType().name(), d.getOriginalFilename(), d.getContentType(), d.getSizeBytes(),
                d.getUploadedAt());
    }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public Long getUploaderUserId() { return uploaderUserId; }
    public String getUploaderRole() { return uploaderRole; }
    public String getDocumentType() { return documentType; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}
