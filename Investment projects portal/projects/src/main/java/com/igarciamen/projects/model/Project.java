package com.igarciamen.projects.model;

import com.igarciamen.projects.enums.ProjectStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "projects", schema = "public")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The promoter lives in the "users" microservice; here we only store its id
    // (extracted from the "userId" claim of the JWT), without calling users for it.
    @Column(nullable = false)
    private Long promoterId;

    // The sector will live in the "sectors" microservice (Block 2, different
    // database). That's why there is NO @ManyToOne relation here, only the id.
    // Validating the id via SectorClient (RestTemplate) will be wired in once
    // "sectors" exists; until then the id is stored without validation.
    @Column(nullable = false)
    private Long sectorId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal requestedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectStatus status = ProjectStatus.DRAFT;

    // Only set when an admin rejects the project (see ProjectService.reject()).
    @Column(length = 500)
    private String rejectionReason;

    // Set when the project is submitted (see ProjectService.submit()). Used by
    // the admin panel's "pending evaluation" list and calendar (Block 6): with
    // no dedicated calendar microservice, these two dates are simple fields on
    // Project, exactly as the roadmap suggested.
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    // Deadline for the admin to review the project, computed as submittedAt +
    // a fixed window (see ProjectService.EVALUATION_WINDOW_DAYS). Not a hard
    // constraint -- nothing currently blocks evaluating a project past this
    // date -- it exists purely as calendar data for the admin panel.
    @Column(name = "evaluation_deadline")
    private LocalDateTime evaluationDeadline;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Project() {}

    public Project(Long promoterId, Long sectorId, String title, String description,
                   String country, BigDecimal requestedAmount) {
        this.promoterId = promoterId;
        this.sectorId = sectorId;
        this.title = title;
        this.description = description;
        this.country = country;
        this.requestedAmount = requestedAmount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPromoterId() { return promoterId; }
    public void setPromoterId(Long promoterId) { this.promoterId = promoterId; }

    public Long getSectorId() { return sectorId; }
    public void setSectorId(Long sectorId) { this.sectorId = sectorId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(BigDecimal requestedAmount) { this.requestedAmount = requestedAmount; }

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getEvaluationDeadline() { return evaluationDeadline; }
    public void setEvaluationDeadline(LocalDateTime evaluationDeadline) { this.evaluationDeadline = evaluationDeadline; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
