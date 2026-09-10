package com.igarciamen.documents.controller;

import com.igarciamen.documents.repository.DocumentRepository;
import com.igarciamen.documents.service.ProjectClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ProjectClient is mocked here (@MockitoBean) instead of hitting a real
// "projects" service over HTTP: this test exercises documents' own upload
// validation, storage, and delete-permission logic, not the projects
// integration itself.
@SpringBootTest
@AutoConfigureMockMvc
class DocumentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepo;

    @MockitoBean
    private ProjectClient projectClient;

    @AfterEach
    void cleanUp() {
        documentRepo.deleteAll();
    }

    @Test
    void upload_withoutToken_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "plan.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/documents/projects/1")
                        .file(file)
                        .param("documentType", "BUSINESS_PLAN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upload_withAccessToTheProject_returns201() throws Exception {
        doNothing().when(projectClient).verifyAccessOrThrow(1L);
        MockMultipartFile file = new MockMultipartFile("file", "plan.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/documents/projects/1")
                        .file(file)
                        .param("documentType", "BUSINESS_PLAN")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 10))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("plan.pdf"))
                .andExpect(jsonPath("$.documentType").value("BUSINESS_PLAN"))
                .andExpect(jsonPath("$.uploaderRole").value("ROLE_PROMOTER"));
    }

    @Test
    void upload_withoutAccessToTheProject_returns403() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to project 1"))
                .when(projectClient).verifyAccessOrThrow(1L);
        MockMultipartFile file = new MockMultipartFile("file", "plan.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/documents/projects/1")
                        .file(file)
                        .param("documentType", "BUSINESS_PLAN")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 20))))
                .andExpect(status().isForbidden());
    }

    @Test
    void upload_invalidDocumentType_returns400() throws Exception {
        doNothing().when(projectClient).verifyAccessOrThrow(1L);
        MockMultipartFile file = new MockMultipartFile("file", "plan.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/documents/projects/1")
                        .file(file)
                        .param("documentType", "NOT_A_REAL_TYPE")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 10))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listForProject_returnsTheUploadedDocuments() throws Exception {
        doNothing().when(projectClient).verifyAccessOrThrow(1L);
        uploadOne(1L, 10);

        mockMvc.perform(get("/api/documents/projects/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void delete_allowsTheUploaderToDeleteTheirOwnDocument() throws Exception {
        doNothing().when(projectClient).verifyAccessOrThrow(1L);
        Long id = uploadOne(1L, 10);

        mockMvc.perform(delete("/api/documents/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 10))))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_rejectsAnUnrelatedPromoter() throws Exception {
        doNothing().when(projectClient).verifyAccessOrThrow(1L);
        Long id = uploadOne(1L, 10);

        mockMvc.perform(delete("/api/documents/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 20))))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_allowsAnAdmin() throws Exception {
        doNothing().when(projectClient).verifyAccessOrThrow(1L);
        Long id = uploadOne(1L, 10);

        mockMvc.perform(delete("/api/documents/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.claim("userId", 999))))
                .andExpect(status().isNoContent());
    }

    private Long uploadOne(Long projectId, int promoterId) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "plan.pdf", "application/pdf", "content".getBytes());

        String response = mockMvc.perform(multipart("/api/documents/projects/" + projectId)
                        .file(file)
                        .param("documentType", "BUSINESS_PLAN")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", promoterId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asLong();
    }
}
