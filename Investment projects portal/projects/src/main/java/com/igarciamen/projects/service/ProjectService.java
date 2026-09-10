package com.igarciamen.projects.service;

import com.igarciamen.projects.enums.ProjectStatus;
import com.igarciamen.projects.model.Project;
import com.igarciamen.projects.payloads.request.CreateProjectRequest;
import com.igarciamen.projects.payloads.request.UpdateProjectRequest;
import com.igarciamen.projects.payloads.response.ProjectMetricsResponse;
import com.igarciamen.projects.repository.ProjectRepository;
import com.igarciamen.projects.repository.ProjectSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    // How many days an admin has to evaluate a project after it is submitted.
    // Purely informational (calendar data for the admin panel) -- nothing
    // enforces this deadline, a late review still works exactly the same way.
    private static final long EVALUATION_WINDOW_DAYS = 14;

    private final ProjectRepository projectRepo;
    private final SectorClient sectorClient;
    private final UserClient userClient;
    private final EmailClient emailClient;
    private final String adminNotificationEmail;

    public ProjectService(ProjectRepository projectRepo, SectorClient sectorClient,
                          UserClient userClient, EmailClient emailClient,
                          @Value("${app.admin.notification-email}") String adminNotificationEmail) {
        this.projectRepo = projectRepo;
        this.sectorClient = sectorClient;
        this.userClient = userClient;
        this.emailClient = emailClient;
        this.adminNotificationEmail = adminNotificationEmail;
    }

    public Project create(Long promoterId, CreateProjectRequest req) {
        // Validates the sector against the "sectors" microservice before creating
        // the project. Throws 404 if the sector does not exist.
        try {
            sectorClient.fetchSectorOrThrow(req.getSectorId());
        } catch (jakarta.persistence.EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sector not found: " + req.getSectorId());
        }

        Project project = new Project(
                promoterId,
                req.getSectorId(),
                req.getTitle(),
                req.getDescription(),
                req.getCountry(),
                req.getRequestedAmount()
        );
        return projectRepo.save(project);
    }

    public List<Project> listMine(Long promoterId) {
        return projectRepo.findByPromoterIdOrderByCreatedAtDesc(promoterId);
    }

    // Detail of a single project: accessible to the owning promoter, or to any
    // ROLE_ADMIN. Any other case returns 403 (for example, an investor should
    // not yet be able to see a project that isn't APPROVED; the public endpoint
    // for approved projects arrives in Block 5).
    public Project getOne(Long projectId, Long requesterId, boolean isAdmin) {
        Project project = findOrThrow(projectId);

        if (!isAdmin && !project.getPromoterId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this project");
        }
        return project;
    }

    // Editing is only allowed while the project is in DRAFT (once SUBMITTED,
    // changing the data would require going through review again; that
    // reopening logic is out of scope for this block).
    public Project update(Long projectId, Long promoterId, UpdateProjectRequest req) {
        Project project = findOrThrow(projectId);

        if (!project.getPromoterId().equals(promoterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the promoter of this project");
        }
        requireStatus(project, ProjectStatus.DRAFT, "edited");

        project.setSectorId(req.getSectorId());
        project.setTitle(req.getTitle());
        project.setDescription(req.getDescription());
        project.setCountry(req.getCountry());
        project.setRequestedAmount(req.getRequestedAmount());

        return projectRepo.save(project);
    }

    public List<Project> listAllForAdmin(ProjectStatus status) {
        if (status == null) {
            return projectRepo.findAllByOrderByCreatedAtDesc();
        }
        return projectRepo.findByStatusOrderByCreatedAtDesc(status);
    }

    // Admin panel (Block 6): projects waiting for an admin action, oldest
    // submission first -- the queue an admin works through.
    public List<Project> listPendingEvaluation() {
        return projectRepo.findByStatusInOrderBySubmittedAtAsc(
                List.of(ProjectStatus.SUBMITTED, ProjectStatus.UNDER_REVIEW));
    }

    // Admin dashboard headline numbers (Block 6): same role as
    // tasks/MetricsResponse in SecGest.
    public ProjectMetricsResponse getMetrics() {
        return new ProjectMetricsResponse(
                projectRepo.countByStatus(ProjectStatus.DRAFT),
                projectRepo.countByStatus(ProjectStatus.SUBMITTED),
                projectRepo.countByStatus(ProjectStatus.UNDER_REVIEW),
                projectRepo.countByStatus(ProjectStatus.APPROVED),
                projectRepo.countByStatus(ProjectStatus.REJECTED)
        );
    }

    // Public catalog for investors (Block 5): APPROVED projects only, with
    // optional filters. No authentication required -- this is the InvestEU
    // "discovery" step, open to anyone.
    public List<Project> listPublic(Long sectorId, String country, BigDecimal minAmount, BigDecimal maxAmount) {
        var spec = ProjectSpecifications.publicFilters(sectorId, country, minAmount, maxAmount);
        return projectRepo.findAll(spec);
    }

    // Detail of a single project, public catalog version: no ownership check,
    // but only ever returns a project that is APPROVED. Used both by the
    // public frontend detail page, and by "interests" (ProjectClient) to
    // check that a project accepts expressions of interest.
    public Project getOnePublic(Long projectId) {
        Project project = findOrThrow(projectId);
        if (project.getStatus() != ProjectStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + projectId);
        }
        return project;
    }

    // ---------------- status transitions (Block 2) ----------------

    // Promoter submits their own project for evaluation: DRAFT -> SUBMITTED.
    public Project submit(Long projectId, Long promoterId) {
        Project project = findOrThrow(projectId);

        if (!project.getPromoterId().equals(promoterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the promoter of this project");
        }
        requireStatus(project, ProjectStatus.DRAFT, "submitted");

        project.setStatus(ProjectStatus.SUBMITTED);
        LocalDateTime now = LocalDateTime.now();
        project.setSubmittedAt(now);
        project.setEvaluationDeadline(now.plusDays(EVALUATION_WINDOW_DAYS));
        Project saved = projectRepo.save(project);

        notifyPromoter(saved, "Your project has been submitted",
                "Your project \"" + saved.getTitle() + "\" has been submitted and is now waiting to be reviewed.");
        notifyAdmin("New project submitted for review",
                "Project \"" + saved.getTitle() + "\" (id " + saved.getId() + ") has just been submitted and is waiting for an admin to start the review.");

        return saved;
    }

    // Admin starts the evaluation: SUBMITTED -> UNDER_REVIEW.
    public Project review(Long projectId) {
        Project project = findOrThrow(projectId);
        requireStatus(project, ProjectStatus.SUBMITTED, "moved to review");

        project.setStatus(ProjectStatus.UNDER_REVIEW);
        Project saved = projectRepo.save(project);

        notifyPromoter(saved, "Your project is now under review",
                "Your project \"" + saved.getTitle() + "\" is now being reviewed by an admin.");

        return saved;
    }

    // Admin approves the project: UNDER_REVIEW -> APPROVED.
    // TODO (Block 5): from this point on the project is eligible for the
    // public investor-facing listing.
    public Project approve(Long projectId) {
        Project project = findOrThrow(projectId);
        requireStatus(project, ProjectStatus.UNDER_REVIEW, "approved");

        project.setStatus(ProjectStatus.APPROVED);
        Project saved = projectRepo.save(project);

        notifyPromoter(saved, "Your project has been approved",
                "Congratulations! Your project \"" + saved.getTitle() + "\" has been approved.");

        return saved;
    }

    // Admin rejects the project: UNDER_REVIEW -> REJECTED, with an optional reason.
    public Project reject(Long projectId, String reason) {
        Project project = findOrThrow(projectId);
        requireStatus(project, ProjectStatus.UNDER_REVIEW, "rejected");

        project.setStatus(ProjectStatus.REJECTED);
        project.setRejectionReason(reason);
        Project saved = projectRepo.save(project);

        String message = "Your project \"" + saved.getTitle() + "\" has been rejected."
                + (reason != null && !reason.isBlank() ? " Reason: " + reason : "");
        notifyPromoter(saved, "Your project has been rejected", message);

        return saved;
    }

    // ---------------- notifications ----------------

    // Looks up the promoter's email via "users" and sends the email via
    // "notifications". Deliberately swallows ANY failure (user lookup fails,
    // notifications is unreachable, etc.): a project's status transition is
    // already persisted by the time this runs, and a notification problem
    // must never turn into a failed request for the promoter/admin who just
    // performed a legitimate action.
    private void notifyPromoter(Project project, String subject, String message) {
        try {
            var user = userClient.fetchUserOrThrow(project.getPromoterId());
            Object email = user.get("email");
            if (email == null) {
                log.warn("Could not notify promoter {}: no email returned by users", project.getPromoterId());
                return;
            }
            emailClient.sendGenericEmail(email.toString(), subject, message);
        } catch (Exception e) {
            log.warn("Failed to notify the promoter of project {}: {}", project.getId(), e.getMessage());
        }
    }

    private void notifyAdmin(String subject, String message) {
        try {
            emailClient.sendGenericEmail(adminNotificationEmail, subject, message);
        } catch (Exception e) {
            log.warn("Failed to notify the admin: {}", e.getMessage());
        }
    }

    // ---------------- helpers ----------------

    private Project findOrThrow(Long projectId) {
        return projectRepo.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + projectId));
    }

    private void requireStatus(Project project, ProjectStatus expected, String actionPastTense) {
        if (project.getStatus() != expected) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A project can only be " + actionPastTense + " from " + expected
                            + " status (current status: " + project.getStatus() + ")");
        }
    }
}
