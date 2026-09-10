package com.igarciamen.messages.payloads.response;

import java.util.List;

public class ThreadResponse {

    private Long projectId;
    private Long investorId;
    private List<MessageResponse> messages;

    public ThreadResponse() {}

    public ThreadResponse(Long projectId, Long investorId, List<MessageResponse> messages) {
        this.projectId = projectId;
        this.investorId = investorId;
        this.messages = messages;
    }

    public Long getProjectId() { return projectId; }
    public Long getInvestorId() { return investorId; }
    public List<MessageResponse> getMessages() { return messages; }
}
