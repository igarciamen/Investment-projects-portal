package com.igarciamen.projects.controller;

import com.igarciamen.projects.enums.ProjectStatus;
import com.igarciamen.projects.model.Project;
import com.igarciamen.projects.payloads.request.CreateProjectRequest;
import com.igarciamen.projects.payloads.request.RejectProjectRequest;
import com.igarciamen.projects.payloads.request.UpdateProjectRequest;
import com.igarciamen.projects.payloads.response.ProjectMetricsResponse;
import com.igarciamen.projects.payloads.response.ProjectResponse;
import com.igarciamen.projects.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Operation(
            summary = "Creates a new project in DRAFT status (ROLE_PROMOTER only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProjectResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody CreateProjectRequest req) {
        Long promoterId = extractUserId(jwt);
        Project created = projectService.create(promoterId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponse.from(created));
    }

    @Operation(
            summary = "Lists the authenticated promoter's projects (\"My projects\")",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/mine", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProjectResponse>> mine(@AuthenticationPrincipal Jwt jwt) {
        Long promoterId = extractUserId(jwt);
        List<ProjectResponse> body = projectService.listMine(promoterId).stream()
                .map(ProjectResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @Operation(
            summary = "Detail of a single project (the owning promoter, or any ROLE_ADMIN)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProjectResponse> getOne(@AuthenticationPrincipal Jwt jwt,
                                                  Authentication authentication,
                                                  @PathVariable Long id) {
        boolean isAdmin = isAdmin(authentication);
        Project project = projectService.getOne(id, extractUserId(jwt), isAdmin);
        return ResponseEntity.ok(ProjectResponse.from(project));
    }

    @Operation(
            summary = "Edits an own project while it is in DRAFT status (ROLE_PROMOTER only, project owner)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProjectResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                  @Valid @RequestBody UpdateProjectRequest req) {
        Project updated = projectService.update(id, extractUserId(jwt), req);
        return ResponseEntity.ok(ProjectResponse.from(updated));
    }

    @Operation(
            summary = "Lists every project, from all promoters; optionally filtered by status (ROLE_ADMIN only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProjectResponse>> all(@RequestParam(required = false) ProjectStatus status) {
        List<ProjectResponse> body = projectService.listAllForAdmin(status).stream()
                .map(ProjectResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @Operation(
            summary = "Lists projects waiting for an admin action (SUBMITTED or UNDER_REVIEW), oldest first (ROLE_ADMIN only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/pending-evaluation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProjectResponse>> pendingEvaluation() {
        List<ProjectResponse> body = projectService.listPendingEvaluation().stream()
                .map(ProjectResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @Operation(
            summary = "Project counts per status, for the admin dashboard (ROLE_ADMIN only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProjectMetricsResponse> metrics() {
        return ResponseEntity.ok(projectService.getMetrics());
    }

    @Operation(
            summary = "Submits the project for evaluation: DRAFT -> SUBMITTED (ROLE_PROMOTER only, project owner)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping(path = "/{id}/submit", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProjectResponse> submit(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        Project updated = projectService.submit(id, extractUserId(jwt));
        return ResponseEntity.ok(ProjectResponse.from(updated));
    }

    @Operation(
            summary = "Starts the evaluation: SUBMITTED -> UNDER_REVIEW (ROLE_ADMIN only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping(path = "/{id}/review", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProjectResponse> review(@PathVariable Long id) {
        Project updated = projectService.review(id);
        return ResponseEntity.ok(ProjectResponse.from(updated));
    }

    @Operation(
            summary = "Approves the project: UNDER_REVIEW -> APPROVED (ROLE_ADMIN only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping(path = "/{id}/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProjectResponse> approve(@PathVariable Long id) {
        Project updated = projectService.approve(id);
        return ResponseEntity.ok(ProjectResponse.from(updated));
    }

    @Operation(
            summary = "Rejects the project, with an optional reason: UNDER_REVIEW -> REJECTED (ROLE_ADMIN only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping(path = "/{id}/reject", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProjectResponse> reject(@PathVariable Long id,
                                                  @RequestBody(required = false) RejectProjectRequest req) {
        String reason = req != null ? req.getReason() : null;
        Project updated = projectService.reject(id, reason);
        return ResponseEntity.ok(ProjectResponse.from(updated));
    }

    @Operation(
            summary = "Public catalog of APPROVED projects, with optional filters (no token needed)"
    )
    @GetMapping(path = "/public", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProjectResponse>> listPublic(
            @RequestParam(required = false) Long sector,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount) {
        List<ProjectResponse> body = projectService.listPublic(sector, country, minAmount, maxAmount).stream()
                .map(ProjectResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @Operation(
            summary = "Public detail of a single APPROVED project (no token needed)"
    )
    @GetMapping(path = "/public/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProjectResponse> getOnePublic(@PathVariable Long id) {
        Project project = projectService.getOnePublic(id);
        return ResponseEntity.ok(ProjectResponse.from(project));
    }

    // The "userId" claim is set by JwtUtils (in users) as a number; depending on
    // the parser it can arrive as Long or Integer, so it is converted via
    // Number instead of casting directly.
    private Long extractUserId(Jwt jwt) {
        Object claim = jwt.getClaim("userId");
        if (claim == null) {
            throw new IllegalStateException("The token does not contain the 'userId' claim");
        }
        return ((Number) claim).longValue();
    }

    // Reads the role from Spring Security's resolved authorities (the same
    // source SecurityConfig uses for hasAuthority(...)), instead of
    // re-parsing the raw "roles" claim from the JWT by hand. This keeps a
    // single source of truth for "what role does this request have" and
    // avoids subtle mismatches between the two.
    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}