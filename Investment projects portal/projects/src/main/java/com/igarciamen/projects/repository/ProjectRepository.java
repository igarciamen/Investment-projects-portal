package com.igarciamen.projects.repository;

import com.igarciamen.projects.enums.ProjectStatus;
import com.igarciamen.projects.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    // The authenticated promoter's own projects ("My projects").
    List<Project> findByPromoterIdOrderByCreatedAtDesc(Long promoterId);

    // Admin dashboard (Block 6): all projects, or filtered by status.
    List<Project> findAllByOrderByCreatedAtDesc();
    List<Project> findByStatusOrderByCreatedAtDesc(ProjectStatus status);

    // "Pending evaluation" queue: SUBMITTED and UNDER_REVIEW, oldest first, so
    // whichever project has been waiting the longest for an admin shows first.
    List<Project> findByStatusInOrderBySubmittedAtAsc(List<ProjectStatus> statuses);

    long countByStatus(ProjectStatus status);
}
