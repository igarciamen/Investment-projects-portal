package com.igarciamen.messages.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

// Calls the "notifications" microservice's generic email endpoint. Same
// pattern as EmailClient in projects (Block 4): a failure here must never
// block the message from being sent -- MessagingService wraps every call
// to this client in a try/catch and only logs the failure.
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
            Map<String, Object> body = Map.of("to", to, "subject", subject, "message", message);
            http.postForObject(notificationsBase + "/email", body, String.class);
        } catch (RestClientException e) {
            throw new IllegalStateException("Error calling the notifications service: " + e.getMessage(), e);
        }
    }
}