package com.igarciamen.interests.payloads.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateInterestRequest {

    @NotNull
    private Long projectId;

    @Size(max = 1000)
    private String message;

    public CreateInterestRequest() {}

    public CreateInterestRequest(Long projectId, String message) {
        this.projectId = projectId;
        this.message = message;
    }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
