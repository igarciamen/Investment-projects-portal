package com.igarciamen.documents.repository;

import com.igarciamen.documents.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByProjectIdOrderByUploadedAtDesc(Long projectId);
}
