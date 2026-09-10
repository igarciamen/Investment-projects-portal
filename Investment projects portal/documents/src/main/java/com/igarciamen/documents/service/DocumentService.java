package com.igarciamen.documents.service;

import com.igarciamen.documents.enums.DocumentType;
import com.igarciamen.documents.model.Document;
import com.igarciamen.documents.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    // Deliberately conservative limit for a school project: reference
    // attachments (business plans, accounts, technical reports), not heavy
    // media files.
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/png", "image/jpeg",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain",
            // .zip: different browsers/operating systems send a slightly
            // different content-type for the same file type; all three are accepted.
            "application/zip",
            "application/x-zip-compressed",
            "multipart/x-zip"
    );

    private final DocumentRepository documentRepo;
    private final DocumentStorageService storageService;
    private final ProjectClient projectClient;
    private final UserClient userClient;
    private final EmailClient emailClient;
    private final String adminNotificationEmail;

    public DocumentService(DocumentRepository documentRepo, DocumentStorageService storageService,
                           ProjectClient projectClient, UserClient userClient, EmailClient emailClient,
                           @Value("${app.admin.notification-email}") String adminNotificationEmail) {
        this.documentRepo = documentRepo;
        this.storageService = storageService;
        this.projectClient = projectClient;
        this.userClient = userClient;
        this.emailClient = emailClient;
        this.adminNotificationEmail = adminNotificationEmail;
    }

    public Document upload(Long projectId, Long uploaderUserId, String uploaderRole,
                           DocumentType documentType, MultipartFile file) {
        projectClient.verifyAccessOrThrow(projectId);
        validateFile(file);

        String storedFilename = storageService.store(projectId, file);

        Document document = new Document(projectId, uploaderUserId, uploaderRole, documentType,
                file.getOriginalFilename(), storedFilename, file.getContentType(), file.getSize());
        document = documentRepo.save(document);

        notifyAboutNewDocument(document);

        return document;
    }

    // If the promoter uploads, the fixed admin inbox is notified (same
    // pattern as "new project submitted" in projects, Block 4) -- there is
    // no single admin id to resolve an email for, so a configured address is
    // used instead. If an admin uploads (e.g. feedback during evaluation),
    // the project's own promoter is notified instead. Any failure here
    // (users/notifications unreachable, etc.) is swallowed and logged: the
    // upload itself is already persisted, a notification issue must never
    // turn into a failed upload.
    private void notifyAboutNewDocument(Document document) {
        try {
            String subject = "New document uploaded on InvestEU";
            String message = "A new " + document.getDocumentType() + " document (\"" + document.getOriginalFilename()
                    + "\") has been uploaded for project " + document.getProjectId() + ".";

            if ("ROLE_ADMIN".equals(document.getUploaderRole())) {
                Map<String, Object> project = projectClient.fetchProjectOrThrow(document.getProjectId());
                Object promoterId = project.get("promoterId");
                if (promoterId == null) {
                    log.warn("Could not notify: project {} has no promoterId", document.getProjectId());
                    return;
                }
                var user = userClient.fetchUserOrThrow(((Number) promoterId).longValue());
                Object email = user.get("email");
                if (email == null) {
                    log.warn("Could not notify promoter of project {}: no email on file", document.getProjectId());
                    return;
                }
                emailClient.sendGenericEmail(email.toString(), subject, message);
            } else {
                emailClient.sendGenericEmail(adminNotificationEmail, subject, message);
            }
        } catch (Exception e) {
            log.warn("Failed to notify about document {} for project {}: {}",
                    document.getId(), document.getProjectId(), e.getMessage());
        }
    }

    public List<Document> listForProject(Long projectId) {
        projectClient.verifyAccessOrThrow(projectId);
        return documentRepo.findByProjectIdOrderByUploadedAtDesc(projectId);
    }

    public DownloadedFile download(Long documentId) {
        Document document = findOrThrow(documentId);

        projectClient.verifyAccessOrThrow(document.getProjectId());

        Resource resource = storageService.load(document.getProjectId(), document.getStoredFilename());
        return new DownloadedFile(resource, document.getOriginalFilename(), document.getContentType());
    }

    // Only whoever uploaded the file, or an admin, can delete it -- a promoter
    // should not be able to remove, say, an admin's evaluation attachment (once
    // that use case exists).
    public void delete(Long documentId, Long requesterId, boolean isAdmin) {
        Document document = findOrThrow(documentId);

        projectClient.verifyAccessOrThrow(document.getProjectId());

        if (!isAdmin && !document.getUploaderUserId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only whoever uploaded the document (or an admin) can delete it");
        }

        storageService.delete(document.getProjectId(), document.getStoredFilename());
        documentRepo.delete(document);
    }

    private Document findOrThrow(Long documentId) {
        return documentRepo.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + documentId));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file was received");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "The file exceeds the maximum allowed size (10 MB)");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "File type not allowed: " + file.getContentType());
        }
    }

    public record DownloadedFile(Resource resource, String originalFilename, String contentType) {}
}