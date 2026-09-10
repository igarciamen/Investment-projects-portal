package com.igarciamen.notifications.controller;

import com.igarciamen.notifications.payloads.request.EmailRequest;
import com.igarciamen.notifications.service.IEmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// This service is not called by the frontend directly, only by other
// microservices (currently "projects", via RestTemplate, forwarding the
// caller's own token). It still validates the JWT like everyone else, so
// nobody can trigger an email send without authenticating.
@RestController
@RequestMapping("/api/notifications")
public class EmailController {

    private final IEmailService emailService;

    public EmailController(IEmailService emailService) {
        this.emailService = emailService;
    }

    @Operation(
            summary = "Sends a generic email (any authenticated caller)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(path = "/email", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> sendEmail(@Valid @RequestBody EmailRequest emailRequest) {
        emailService.sendEmail(emailRequest);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
