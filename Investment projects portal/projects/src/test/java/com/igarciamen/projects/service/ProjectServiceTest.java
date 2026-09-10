package com.igarciamen.projects.service;

import com.igarciamen.projects.enums.ProjectStatus;
import com.igarciamen.projects.model.Project;
import com.igarciamen.projects.payloads.request.CreateProjectRequest;
import com.igarciamen.projects.payloads.request.UpdateProjectRequest;
import com.igarciamen.projects.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock private ProjectRepository projectRepo;
    @Mock private SectorClient sectorClient;
    @Mock private UserClient userClient;
    @Mock private EmailClient emailClient;

    @InjectMocks private ProjectService projectService;

    // @Value("${app.admin.notification-email}") has no Spring context to
    // resolve in a plain Mockito unit test, so it's set by hand after
    // construction (@InjectMocks leaves it null otherwise).
    private void setAdminEmail() {
        ReflectionTestUtils.setField(projectService, "adminNotificationEmail", "isabel@admin.local");
    }

    private CreateProjectRequest createReq() {
        CreateProjectRequest req = new CreateProjectRequest();
        req.setSectorId(1L);
        req.setTitle("Solar plant in Extremadura");
        req.setDescription("5MW photovoltaic installation");
        req.setCountry("Spain");
        req.setRequestedAmount(new BigDecimal("250000.00"));
        return req;
    }

    private Project approvableProject() {
        Project p = new Project(10L, 1L, "T", "D", "Spain", BigDecimal.TEN);
        p.setId(1L);
        return p;
    }

    // ---------------- create() (unchanged behaviour) ----------------

    @Test
    void create_createsProjectInDraftWhenSectorExists() {
        when(sectorClient.fetchSectorOrThrow(1L)).thenReturn(Map.of("id", 1, "name", "Energy"));
        when(projectRepo.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        Project result = projectService.create(10L, createReq());

        assertEquals(10L, result.getPromoterId());
        assertEquals(ProjectStatus.DRAFT, result.getStatus());
        verify(sectorClient).fetchSectorOrThrow(1L);
        verifyNoInteractions(userClient, emailClient);
    }

    @Test
    void create_throws404WhenSectorDoesNotExist() {
        when(sectorClient.fetchSectorOrThrow(1L))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("Sector not found: 1"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> projectService.create(10L, createReq()));

        assertEquals(404, ex.getStatusCode().value());
        verify(projectRepo, never()).save(any());
    }

    // ---------------- listMine / getOne / update (unchanged behaviour) ----------------

    @Test
    void getOne_allowsTheOwningPromoter() {
        Project p = approvableProject();
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));

        Project result = projectService.getOne(1L, 10L, false);

        assertEquals(1L, result.getId());
    }

    @Test
    void update_allowsEditingWhileInDraft() {
        Project p = approvableProject();
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));
        when(projectRepo.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProjectRequest req = new UpdateProjectRequest();
        req.setSectorId(2L);
        req.setTitle("New title");
        req.setDescription("New description");
        req.setCountry("Portugal");
        req.setRequestedAmount(new BigDecimal("99999.00"));

        Project updated = projectService.update(1L, 10L, req);

        assertEquals("New title", updated.getTitle());
        verifyNoInteractions(userClient, emailClient);
    }

    // ---------------- status transitions + notifications (Block 4) ----------------

    @Test
    void submit_movesFromDraftToSubmittedAndNotifiesPromoterAndAdmin() {
        setAdminEmail();
        Project p = approvableProject();
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));
        when(projectRepo.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.fetchUserOrThrow(10L)).thenReturn(Map.of("id", 10, "email", "ana@promoter.com"));

        Project result = projectService.submit(1L, 10L);

        assertEquals(ProjectStatus.SUBMITTED, result.getStatus());
        verify(emailClient).sendGenericEmail(eq("ana@promoter.com"), anyString(), anyString());
        verify(emailClient).sendGenericEmail(eq("isabel@admin.local"), anyString(), anyString());
        System.out.println("=== submit: DRAFT -> SUBMITTED, promoter and admin notified ===");
    }

    @Test
    void submit_stillSucceedsWhenNotifyingThePromoterFails() {
        setAdminEmail();
        Project p = approvableProject();
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));
        when(projectRepo.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.fetchUserOrThrow(10L)).thenThrow(new IllegalStateException("users unreachable"));

        Project result = projectService.submit(1L, 10L);

        assertEquals(ProjectStatus.SUBMITTED, result.getStatus());
        System.out.println("=== submit: status transition succeeds even if the users lookup fails ===");
    }

    @Test
    void submit_rejectsWhenNotThePromoter() {
        Project p = approvableProject();
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> projectService.submit(1L, 999L));

        assertEquals(403, ex.getStatusCode().value());
        verifyNoInteractions(userClient, emailClient);
    }

    @Test
    void submit_rejectsWhenNotInDraft() {
        Project p = approvableProject();
        p.setStatus(ProjectStatus.SUBMITTED);
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> projectService.submit(1L, 10L));

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void review_movesFromSubmittedToUnderReviewAndNotifiesThePromoter() {
        Project p = approvableProject();
        p.setStatus(ProjectStatus.SUBMITTED);
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));
        when(projectRepo.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.fetchUserOrThrow(10L)).thenReturn(Map.of("id", 10, "email", "ana@promoter.com"));

        Project result = projectService.review(1L);

        assertEquals(ProjectStatus.UNDER_REVIEW, result.getStatus());
        verify(emailClient).sendGenericEmail(eq("ana@promoter.com"), anyString(), anyString());
    }

    @Test
    void review_rejectsWhenNotSubmitted() {
        Project p = approvableProject(); // still DRAFT
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> projectService.review(1L));

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void approve_movesFromUnderReviewToApprovedAndNotifiesThePromoter() {
        Project p = approvableProject();
        p.setStatus(ProjectStatus.UNDER_REVIEW);
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));
        when(projectRepo.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.fetchUserOrThrow(10L)).thenReturn(Map.of("id", 10, "email", "ana@promoter.com"));

        Project result = projectService.approve(1L);

        assertEquals(ProjectStatus.APPROVED, result.getStatus());
        verify(emailClient).sendGenericEmail(eq("ana@promoter.com"), anyString(), anyString());
    }

    @Test
    void approve_rejectsWhenNotUnderReview() {
        Project p = approvableProject(); // still DRAFT
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> projectService.approve(1L));

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void reject_movesFromUnderReviewToRejectedStoresTheReasonAndNotifiesThePromoter() {
        Project p = approvableProject();
        p.setStatus(ProjectStatus.UNDER_REVIEW);
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));
        when(projectRepo.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.fetchUserOrThrow(10L)).thenReturn(Map.of("id", 10, "email", "ana@promoter.com"));

        Project result = projectService.reject(1L, "Missing financial documentation");

        assertEquals(ProjectStatus.REJECTED, result.getStatus());
        assertEquals("Missing financial documentation", result.getRejectionReason());
        verify(emailClient).sendGenericEmail(eq("ana@promoter.com"), anyString(),
                org.mockito.ArgumentMatchers.contains("Missing financial documentation"));
    }

    @Test
    void reject_rejectsWhenNotUnderReview() {
        Project p = approvableProject(); // still DRAFT
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> projectService.reject(1L, "any reason"));

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void listAllForAdmin_withFilterReturnsByStatus() {
        when(projectRepo.findByStatusOrderByCreatedAtDesc(ProjectStatus.SUBMITTED)).thenReturn(java.util.List.of());

        projectService.listAllForAdmin(ProjectStatus.SUBMITTED);

        verify(projectRepo).findByStatusOrderByCreatedAtDesc(ProjectStatus.SUBMITTED);
    }

    @Test
    void listPublic_delegatesToTheRepositoryWithASpecification() {
        when(projectRepo.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Project>>any()))
                .thenReturn(java.util.List.of(approvableProject()));

        var result = projectService.listPublic(1L, "Spain", BigDecimal.ONE, BigDecimal.TEN);

        assertEquals(1, result.size());
    }

    @Test
    void getOnePublic_returnsAnApprovedProject() {
        Project p = approvableProject();
        p.setStatus(ProjectStatus.APPROVED);
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));

        Project result = projectService.getOnePublic(1L);

        assertEquals(ProjectStatus.APPROVED, result.getStatus());
    }

    @Test
    void getOnePublic_throws404WhenNotApproved() {
        Project p = approvableProject(); // still DRAFT
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> projectService.getOnePublic(1L));

        assertEquals(404, ex.getStatusCode().value());
        System.out.println("=== getOnePublic: 404 for a non-approved project (hides its existence) ===");
    }

    // ---------------- admin panel (Block 6) ----------------

    @Test
    void submit_stampsSubmittedAtAndAFourteenDayEvaluationDeadline() {
        setAdminEmail();
        Project p = approvableProject();
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));
        when(projectRepo.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        Project result = projectService.submit(1L, 10L);

        assertNotNull(result.getSubmittedAt());
        assertNotNull(result.getEvaluationDeadline());
        assertEquals(14, java.time.Duration.between(result.getSubmittedAt(), result.getEvaluationDeadline()).toDays());
        System.out.println("=== submit: submittedAt/evaluationDeadline stamped (14-day window) ===");
    }

    @Test
    void listPendingEvaluation_delegatesToTheRepositoryWithBothWaitingStatuses() {
        when(projectRepo.findByStatusInOrderBySubmittedAtAsc(
                java.util.List.of(ProjectStatus.SUBMITTED, ProjectStatus.UNDER_REVIEW)))
                .thenReturn(java.util.List.of(approvableProject()));

        var result = projectService.listPendingEvaluation();

        assertEquals(1, result.size());
        System.out.println("=== listPendingEvaluation: queries SUBMITTED + UNDER_REVIEW, oldest first ===");
    }

    @Test
    void getMetrics_countsEveryStatus() {
        when(projectRepo.countByStatus(ProjectStatus.DRAFT)).thenReturn(3L);
        when(projectRepo.countByStatus(ProjectStatus.SUBMITTED)).thenReturn(2L);
        when(projectRepo.countByStatus(ProjectStatus.UNDER_REVIEW)).thenReturn(1L);
        when(projectRepo.countByStatus(ProjectStatus.APPROVED)).thenReturn(5L);
        when(projectRepo.countByStatus(ProjectStatus.REJECTED)).thenReturn(1L);

        var result = projectService.getMetrics();

        assertEquals(3, result.getDraft());
        assertEquals(5, result.getApproved());
        assertEquals(12, result.getTotal());
        System.out.println("=== getMetrics: total = sum of every status count ===");
    }
}
