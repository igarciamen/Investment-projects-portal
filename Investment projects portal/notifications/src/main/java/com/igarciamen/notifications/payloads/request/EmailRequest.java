package com.igarciamen.notifications.payloads.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Generic, general-purpose email request (reusable for future blocks:
// project status changes, expressions of interest, new messages...). Uses
// the "email" template.
public class EmailRequest {

    @NotBlank
    @Email
    private String to;

    @NotBlank
    private String subject;

    @NotBlank
    private String message;

    public EmailRequest() {}

    public EmailRequest(String to, String subject, String message) {
        this.to = to;
        this.subject = subject;
        this.message = message;
    }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
