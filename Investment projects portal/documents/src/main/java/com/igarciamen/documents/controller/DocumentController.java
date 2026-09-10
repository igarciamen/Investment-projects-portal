package com.igarciamen.documents.controller;

import com.igarciamen.documents.enums.DocumentType;
import com.igarciamen.documents.model.Document;
import com.igarciamen.documents.payloads.response.DocumentResponse;
import com.igarciamen.documents.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(
            summary = "Uploads a document attached to a project (the owning promoter, or any admin)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(path = "/projects/{projectId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentResponse> upload(@AuthenticationPrincipal Jwt jwt,
                                                   Authentication authentication,
                                                   @PathVariable Long projectId,
                                                   @RequestParam("documentType") DocumentType documentType,
                                                   @RequestParam("file") MultipartFile file) {
        Document saved = documentService.upload(projectId, extractUserId(jwt), extractRole(authentication),
                documentType, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentResponse.from(saved));
    }

    @Operation(
            summary = "Lists the documents attached to a project (the owning promoter, or any admin)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/projects/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DocumentResponse>> listForProject(@PathVariable Long projectId) {
        List<DocumentResponse> body = documentService.listForProject(projectId).stream()
                .map(DocumentResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @Operation(
            summary = "Downloads a document by id (the owning promoter of the project, or any admin)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        DocumentService.DownloadedFile file = documentService.download(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.originalFilename()).build().toString())
                .body(file.resource());
    }

    @Operation(
            summary = "Deletes a document (only whoever uploaded it, or an admin)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, Authentication authentication,
                                       @PathVariable Long id) {
        documentService.delete(id, extractUserId(jwt), isAdmin(authentication));
        return ResponseEntity.noContent().build();
    }

    private Long extractUserId(Jwt jwt) {
        Object claim = jwt.getClaim("userId");
        if (claim == null) {
            throw new IllegalStateException("The token does not contain the 'userId' claim");
        }
        return ((Number) claim).longValue();
    }

    // Reads the role from Spring Security's resolved authorities (same
    // reasoning as in ProjectController.isAdmin): a single source of truth,
    // instead of re-parsing the raw "roles" claim from the JWT by hand.
    private String extractRole(Authentication authentication) {
        return isAdmin(authentication) ? "ROLE_ADMIN" : "ROLE_PROMOTER";
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
