package com.igarciamen.documents.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

// Local filesystem storage (no MinIO/S3 for this block, same call as in
// SecGest's documents). Each project gets its own subfolder, and every file
// is stored under a generated name (UUID), never the original one -- this
// avoids any path-traversal issue or weird characters in a user-supplied name.
@Service
public class DocumentStorageService {

    @Value("${documents.storage-path}")
    private String storagePath;

    public String store(Long projectId, MultipartFile file) {
        try {
            Path projectDir = Path.of(storagePath, projectId.toString());
            Files.createDirectories(projectDir);

            String extension = extractExtension(file.getOriginalFilename());
            String storedFilename = UUID.randomUUID() + extension;

            Path target = projectDir.resolve(storedFilename);
            file.transferTo(target);

            return storedFilename;
        } catch (IOException e) {
            throw new IllegalStateException("Could not store the file: " + e.getMessage(), e);
        }
    }

    public Resource load(Long projectId, String storedFilename) {
        Path file = Path.of(storagePath, projectId.toString(), storedFilename);
        if (!Files.exists(file)) {
            throw new IllegalStateException("The file no longer exists on disk: " + storedFilename);
        }
        return new FileSystemResource(file);
    }

    public void delete(Long projectId, String storedFilename) {
        Path file = Path.of(storagePath, projectId.toString(), storedFilename);
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("Could not delete the file: " + e.getMessage(), e);
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}
