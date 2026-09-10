package com.igarciamen.documents.service;

import com.igarciamen.documents.enums.DocumentType;
import com.igarciamen.documents.model.Document;
import com.igarciamen.documents.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepo;
    @Mock private DocumentStorageService storageService;
    @Mock private ProjectClient projectClient;

    @InjectMocks private DocumentService documentService;

    private Document savedDocument() {
        Document d = new Document(1L, 10L, "ROLE_PROMOTER", DocumentType.BUSINESS_PLAN,
                "plan.pdf", "uuid-123.pdf", "application/pdf", 1024L);
        d.setId(1L);
        return d;
    }

    @Test
    void upload_verifiesAccessAndStoresTheFile() {
        MockMultipartFile file = new MockMultipartFile("file", "plan.pdf", "application/pdf", "content".getBytes());
        when(storageService.store(1L, file)).thenReturn("uuid-123.pdf");
        when(documentRepo.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });

        Document result = documentService.upload(1L, 10L, "ROLE_PROMOTER", DocumentType.BUSINESS_PLAN, file);

        assertEquals("plan.pdf", result.getOriginalFilename());
        assertEquals(DocumentType.BUSINESS_PLAN, result.getDocumentType());
        verify(projectClient).verifyAccessOrThrow(1L);
        System.out.println("=== upload: access verified, file stored ===");
    }

    @Test
    void upload_rejectsAnEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> documentService.upload(1L, 10L, "ROLE_PROMOTER", DocumentType.BUSINESS_PLAN, empty));

        assertEquals(400, ex.getStatusCode().value());
        verify(documentRepo, never()).save(any());
        System.out.println("=== upload: 400 for an empty file ===");
    }

    @Test
    void upload_rejectsAFileThatIsTooLarge() {
        byte[] tooLarge = new byte[11 * 1024 * 1024]; // 11 MB
        MockMultipartFile big = new MockMultipartFile("file", "big.pdf", "application/pdf", tooLarge);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> documentService.upload(1L, 10L, "ROLE_PROMOTER", DocumentType.BUSINESS_PLAN, big));

        assertEquals(413, ex.getStatusCode().value());
        System.out.println("=== upload: 413 when the file exceeds 10 MB ===");
    }

    @Test
    void upload_rejectsADisallowedContentType() {
        MockMultipartFile exe = new MockMultipartFile("file", "installer.exe", "application/x-msdownload", "x".getBytes());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> documentService.upload(1L, 10L, "ROLE_PROMOTER", DocumentType.BUSINESS_PLAN, exe));

        assertEquals(415, ex.getStatusCode().value());
        System.out.println("=== upload: 415 for a disallowed content type ===");
    }

    @Test
    void upload_propagatesTheAccessErrorFromProjectClient() {
        MockMultipartFile file = new MockMultipartFile("file", "plan.pdf", "application/pdf", "content".getBytes());
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN))
                .when(projectClient).verifyAccessOrThrow(1L);

        assertThrows(ResponseStatusException.class,
                () -> documentService.upload(1L, 999L, "ROLE_PROMOTER", DocumentType.BUSINESS_PLAN, file));

        verify(documentRepo, never()).save(any());
        System.out.println("=== upload: access errors from projects propagate as-is ===");
    }

    @Test
    void listForProject_verifiesAccessAndReturnsTheDocuments() {
        when(documentRepo.findByProjectIdOrderByUploadedAtDesc(1L)).thenReturn(List.of(savedDocument()));

        List<Document> result = documentService.listForProject(1L);

        assertEquals(1, result.size());
        verify(projectClient).verifyAccessOrThrow(1L);
    }

    @Test
    void delete_allowsTheUploaderToDeleteTheirOwnDocument() {
        Document d = savedDocument();
        when(documentRepo.findById(1L)).thenReturn(Optional.of(d));

        documentService.delete(1L, 10L, false);

        verify(storageService).delete(1L, "uuid-123.pdf");
        verify(documentRepo).delete(d);
    }

    @Test
    void delete_allowsAnAdminToDeleteAnyDocument() {
        Document d = savedDocument();
        when(documentRepo.findById(1L)).thenReturn(Optional.of(d));

        documentService.delete(1L, 999L, true);

        verify(storageService).delete(anyLong(), any());
    }

    @Test
    void delete_rejectsAnUnrelatedNonAdminUser() {
        Document d = savedDocument();
        when(documentRepo.findById(1L)).thenReturn(Optional.of(d));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> documentService.delete(1L, 999L, false));

        assertEquals(403, ex.getStatusCode().value());
        verify(storageService, never()).delete(any(), any());
        System.out.println("=== delete: 403 when neither the uploader nor an admin ===");
    }

    @Test
    void delete_throws404WhenTheDocumentDoesNotExist() {
        when(documentRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> documentService.delete(99L, 10L, false));

        assertEquals(404, ex.getStatusCode().value());
    }
}
