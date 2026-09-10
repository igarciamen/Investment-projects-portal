package com.igarciamen.notifications.service;

import com.igarciamen.notifications.payloads.request.EmailRequest;

public interface IEmailService {

    // Generic email (the "email" template). Every future trigger (status
    // change, new expression of interest, new message...) reuses this same
    // method -- only the subject/message text changes, built by whichever
    // service calls this one (currently only "projects").
    void sendEmail(EmailRequest emailRequest);
}
