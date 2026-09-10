package com.igarciamen.projects.payloads.request;

import jakarta.validation.constraints.Size;

public class RejectProjectRequest {

    @Size(max = 500)
    private String reason;

    public RejectProjectRequest() {}

    public RejectProjectRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
