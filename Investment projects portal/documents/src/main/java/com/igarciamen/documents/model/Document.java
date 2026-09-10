package com.igarciamen.documents.model;

import com.igarciamen.documents.enums.DocumentType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

// Metadata ONLY here; the file itself is stored on the filesystem (see
// DocumentStorageService), not in the database.
@Entity
@Table(name = "documents", schema = "public")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long uploaderUserId;

    // "ROLE_PROMOTER" or "ROLE_ADMIN": lets the frontend show who uploaded
    // each file without having to call "users".
    @Column(nullable = false, length = 20)
    private String uploaderRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentType documentType;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    // Real filename on disk: a UUID, never the original name -- avoids
    // collisions and path-traversal / weird-character issues.
    @Column(nullable = false, length = 100, unique = true)
    private String storedFilename;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public Document() {}

    public Document(Long projectId, Long uploaderUserId, String uploaderRole, DocumentType documentType,
                     String originalFilename, String storedFilename, String contentType, long sizeBytes) {
        this.projectId = projectId;
        this.uploaderUserId = uploaderUserId;
        this.uploaderRole = uploaderRole;
        this.documentType = documentType;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getUploaderUserId() { return uploaderUserId; }
    public void setUploaderUserId(Long uploaderUserId) { this.uploaderUserId = uploaderUserId; }

    public String getUploaderRole() { return uploaderRole; }
    public void setUploaderRole(String uploaderRole) { this.uploaderRole = uploaderRole; }

    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
