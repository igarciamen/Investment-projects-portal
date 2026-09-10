package com.igarciamen.messages.payloads.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SendMessageRequest {

    @NotBlank
    @Size(max = 2000)
    private String content;

    public SendMessageRequest() {}

    public SendMessageRequest(String content) {
        this.content = content;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
