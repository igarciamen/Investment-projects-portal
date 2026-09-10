package com.igarciamen.notifications.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igarciamen.notifications.service.IEmailService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// IEmailService is replaced with a mock: no real email is ever sent during
// the tests, regardless of the (dummy) SMTP settings in the test properties.
@SpringBootTest
@AutoConfigureMockMvc
class EmailControllerIT {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private IEmailService emailService;

    @Test
    void sendEmail_withoutToken_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "to", "promoter@mail.com", "subject", "Test", "message", "Hello"));

        mockMvc.perform(post("/api/notifications/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sendEmail_withAnyAuthenticatedRole_callsTheServiceAndReturns200() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "to", "promoter@mail.com", "subject", "Project submitted",
                "message", "Your project has been submitted for evaluation."));

        mockMvc.perform(post("/api/notifications/email")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(emailService).sendEmail(Mockito.any());
    }

    @Test
    void sendEmail_calledByAnAdmin_alsoReturns200() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "to", "admin@mail.com", "subject", "New project submitted",
                "message", "A new project has just been submitted for review."));

        mockMvc.perform(post("/api/notifications/email")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(emailService).sendEmail(Mockito.any());
    }

    @Test
    void sendEmail_missingRequiredField_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "to", "promoter@mail.com", "message", "Hello"));

        mockMvc.perform(post("/api/notifications/email")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendEmail_invalidEmailAddress_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "to", "not-an-email", "subject", "Test", "message", "Hello"));

        mockMvc.perform(post("/api/notifications/email")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
