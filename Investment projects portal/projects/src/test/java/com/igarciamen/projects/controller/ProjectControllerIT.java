package com.igarciamen.projects.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igarciamen.projects.repository.ProjectRepository;
import com.igarciamen.projects.service.EmailClient;
import com.igarciamen.projects.service.SectorClient;
import com.igarciamen.projects.service.UserClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// SectorClient is mocked here (@MockitoBean) instead of hitting a real
// "sectors" service over HTTP: this test only exercises projects' own logic
// and authorization rules, not the sectors integration itself.
@SpringBootTest
@AutoConfigureMockMvc
class ProjectControllerIT {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ProjectRepository projectRepo;

    @MockitoBean
    private SectorClient sectorClient;

    @MockitoBean
    private UserClient userClient;

    @MockitoBean
    private EmailClient emailClient;

    @AfterEach
    void cleanUp() {
        projectRepo.deleteAll();
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "sectorId", 1, "title", "Solar plant", "country", "Spain", "requestedAmount", 10000));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_withInvestorRole_returns403() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "sectorId", 1, "title", "Solar plant", "country", "Spain", "requestedAmount", 10000));

        mockMvc.perform(post("/api/projects")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INVESTOR"))
                                .jwt(j -> j.claim("userId", 1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withPromoterRole_returns201() throws Exception {
        when(sectorClient.fetchSectorOrThrow(anyLong())).thenReturn(Map.of("id", 1, "name", "Energy"));

        String body = objectMapper.writeValueAsString(Map.of(
                "sectorId", 1, "title", "Solar plant in Extremadura",
                "description", "5MW photovoltaic installation",
                "country", "Spain", "requestedAmount", 250000));

        mockMvc.perform(post("/api/projects")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 10)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.promoterId").value(10))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.title").value("Solar plant in Extremadura"));
    }

    @Test
    void create_withUnknownSector_returns404() throws Exception {
        when(sectorClient.fetchSectorOrThrow(anyLong()))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("Sector not found: 99"));

        String body = objectMapper.writeValueAsString(Map.of(
                "sectorId", 99, "title", "Solar plant", "country", "Spain", "requestedAmount", 10000));

        mockMvc.perform(post("/api/projects")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 10)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_withoutTitle_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "sectorId", 1, "country", "Spain", "requestedAmount", 10000));

        mockMvc.perform(post("/api/projects")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 10)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mine_returnsOnlyTheAuthenticatedPromotersProjects() throws Exception {
        createProject(10, "Ana's project");
        createProject(20, "Luis's project");

        mockMvc.perform(get("/api/projects/mine")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Ana's project"));
    }

    @Test
    void getById_allowsTheAdminToSeeAnyProject() throws Exception {
        Long id = createProject(10, "Ana's project");

        mockMvc.perform(get("/api/projects/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.claim("userId", 999))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Ana's project"));
    }

    @Test
    void getById_rejectsAPromoterWhoIsNotTheOwner() throws Exception {
        Long id = createProject(10, "Ana's project");

        mockMvc.perform(get("/api/projects/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 20))))
                .andExpect(status().isForbidden());
    }

    @Test
    void all_onlyAccessibleToAdmin() throws Exception {
        createProject(10, "Ana's project");

        mockMvc.perform(get("/api/projects/all")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 10))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/projects/all")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.claim("userId", 999))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ---------------- admin panel (Block 6) ----------------

    @Test
    void pendingEvaluation_onlyAccessibleToAdminAndOnlyListsWaitingProjects() throws Exception {
        Long draftId = createProject(10, "Still a draft");

        Long submittedId = createProject(10, "Submitted project");
        submitAsOwner(submittedId, 10);

        Long approvedId = createProject(10, "Already approved");
        submitAsOwner(approvedId, 10);
        reviewAsAdmin(approvedId);
        mockMvc.perform(patch("/api/projects/" + approvedId + "/approve")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/projects/pending-evaluation")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 10))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/projects/pending-evaluation")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Submitted project"));
    }

    @Test
    void submit_populatesSubmittedAtAndEvaluationDeadlineInTheResponse() throws Exception {
        Long id = createProject(10, "Ana's project");

        mockMvc.perform(patch("/api/projects/" + id + "/submit")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submittedAt").exists())
                .andExpect(jsonPath("$.evaluationDeadline").exists());
    }

    @Test
    void metrics_onlyAccessibleToAdminAndCountsEveryStatus() throws Exception {
        createProject(10, "Draft project");
        Long submittedId = createProject(10, "Submitted project");
        submitAsOwner(submittedId, 10);

        mockMvc.perform(get("/api/projects/metrics")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 10))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/projects/metrics")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draft").value(1))
                .andExpect(jsonPath("$.submitted").value(1))
                .andExpect(jsonPath("$.total").value(2));
    }

    // ---------------- public listing (Block 5) ----------------

    @Test
    void listPublic_withoutToken_returnsOnlyApprovedProjects() throws Exception {
        Long draftId = createProject(10, "Still a draft");

        Long approvedId = createProject(10, "Approved solar project");
        submitAsOwner(approvedId, 10);
        reviewAsAdmin(approvedId);
        mockMvc.perform(patch("/api/projects/" + approvedId + "/approve")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/projects/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Approved solar project"));
    }

    @Test
    void listPublic_filtersByCountryAndAmountRange() throws Exception {
        Long id = createProject(10, "Approved solar project");
        submitAsOwner(id, 10);
        reviewAsAdmin(id);
        mockMvc.perform(patch("/api/projects/" + id + "/approve")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());

        // Matching filters -> found
        mockMvc.perform(get("/api/projects/public")
                        .param("country", "Spain")
                        .param("minAmount", "1000")
                        .param("maxAmount", "100000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Non-matching country -> not found
        mockMvc.perform(get("/api/projects/public").param("country", "France"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Non-matching amount range -> not found
        mockMvc.perform(get("/api/projects/public").param("minAmount", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---------------- status transitions (Block 2) ----------------

    @Test
    void submit_ownerMovesFromDraftToSubmitted() throws Exception {
        Long id = createProject(10, "Ana's project");

        mockMvc.perform(patch("/api/projects/" + id + "/submit")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void submit_rejectsANonOwningPromoter() throws Exception {
        Long id = createProject(10, "Ana's project");

        mockMvc.perform(patch("/api/projects/" + id + "/submit")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 20))))
                .andExpect(status().isForbidden());
    }

    // userClient/emailClient are left unstubbed in every other transition
    // test on purpose: an unstubbed Mockito mock returns null, which
    // ProjectService.notifyPromoter() catches and just logs (see the
    // try/catch there) -- so the transition itself still succeeds and the
    // status assertions below are unaffected. This test is the one place
    // that stubs them, specifically to prove the notification is actually
    // attempted with the right data.
    @Test
    void submit_triggersANotificationEmailToThePromoterAndTheAdmin() throws Exception {
        when(userClient.fetchUserOrThrow(10L)).thenReturn(Map.of("id", 10, "email", "ana@promoter.com"));
        Long id = createProject(10, "Ana's project");

        mockMvc.perform(patch("/api/projects/" + id + "/submit")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 10))))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(emailClient)
                .sendGenericEmail(org.mockito.ArgumentMatchers.eq("ana@promoter.com"),
                        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        org.mockito.Mockito.verify(emailClient)
                .sendGenericEmail(org.mockito.ArgumentMatchers.eq("isabel@admin.local"),
                        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void review_requiresAdminAndSubmittedStatus() throws Exception {
        Long id = createProject(10, "Ana's project");

        // Not admin -> 403
        mockMvc.perform(patch("/api/projects/" + id + "/review")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 10))))
                .andExpect(status().isForbidden());

        // Admin, but project still in DRAFT -> 409
        mockMvc.perform(patch("/api/projects/" + id + "/review")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict());

        submitAsOwner(id, 10);

        mockMvc.perform(patch("/api/projects/" + id + "/review")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
    }

    @Test
    void approve_fullHappyPath() throws Exception {
        Long id = createProject(10, "Ana's project");
        submitAsOwner(id, 10);
        reviewAsAdmin(id);

        mockMvc.perform(patch("/api/projects/" + id + "/approve")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void reject_storesTheReason() throws Exception {
        Long id = createProject(10, "Ana's project");
        submitAsOwner(id, 10);
        reviewAsAdmin(id);

        String body = objectMapper.writeValueAsString(Map.of("reason", "Missing financial documentation"));

        mockMvc.perform(patch("/api/projects/" + id + "/reject")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Missing financial documentation"));
    }

    @Test
    void reject_withoutBody_isStillAccepted() throws Exception {
        Long id = createProject(10, "Ana's project");
        submitAsOwner(id, 10);
        reviewAsAdmin(id);

        mockMvc.perform(patch("/api/projects/" + id + "/reject")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    // ---------------- helpers ----------------

    private Long createProject(int promoterId, String title) throws Exception {
        when(sectorClient.fetchSectorOrThrow(anyLong())).thenReturn(Map.of("id", 1, "name", "Energy"));

        String body = objectMapper.writeValueAsString(Map.of(
                "sectorId", 1, "title", title, "country", "Spain", "requestedAmount", 50000));

        String response = mockMvc.perform(post("/api/projects")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", promoterId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private void submitAsOwner(Long projectId, int promoterId) throws Exception {
        mockMvc.perform(patch("/api/projects/" + projectId + "/submit")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", promoterId))))
                .andExpect(status().isOk());
    }

    private void reviewAsAdmin(Long projectId) throws Exception {
        mockMvc.perform(patch("/api/projects/" + projectId + "/review")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }
}
