package com.igarciamen.projects.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

// Calls the "notifications" microservice to trigger the generic email
// (its email.html template). A failure here must NEVER roll back or block a
// status transition that has already been persisted -- ProjectService wraps
// every call to this client in a try/catch and only logs the failure. This
// mirrors the same principle used in SecGest's tasks/EmailClient.
@Component
public class EmailClient {

    private final RestTemplate http;
    private final String notificationsBase;

    public EmailClient(RestTemplate http, @Value("${notifications.base-url}") String notificationsBaseUrl) {
        this.http = http;
        this.notificationsBase = notificationsBaseUrl;
    }

    public void sendGenericEmail(String to, String subject, String message) {
        try {
            Map<String, Object> body = Map.of(
                    "to", to,
                    "subject", subject,
                    "message", message
            );
            http.postForObject(notificationsBase + "/email", body, String.class);
        } catch (RestClientException e) {
            throw new IllegalStateException("Error calling the notifications service: " + e.getMessage(), e);
        }
    }
}
