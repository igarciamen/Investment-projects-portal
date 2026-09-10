package com.igarciamen.projects.enums;

// Full lifecycle of a project (see the project roadmap, Block 2).
// This block (Week 1) only uses DRAFT: a project is born as DRAFT and the
// promoter can edit it. The remaining statuses and the transition endpoints
// (submit/review/approve/reject) are activated in Block 2.
public enum ProjectStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED
}
