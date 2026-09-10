package com.igarciamen.messages.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igarciamen.messages.repository.ConversationRepository;
import com.igarciamen.messages.service.ProjectClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ProjectClient is mocked here (@MockitoBean): this test exercises messages'
// own logic and authorization rules, not the projects integration itself.
@SpringBootTest
@AutoConfigureMockMvc
class MessagingControllerIT {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ConversationRepository conversationRepo;

    @MockitoBean
    private ProjectClient projectClient;

    @AfterEach
    void cleanUp() {
        conversationRepo.deleteAll();
    }

    @Test
    void send_withoutToken_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("content", "Hello"));

        mockMvc.perform(post("/api/messages/projects/1/investors/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void send_investorOpensAThreadOnTheirOwnBehalf_returns201() throws Exception {
        doNothing().when(projectClient).verifyApprovedOrThrow(1L);
        String body = objectMapper.writeValueAsString(Map.of("content", "I'm interested in this project!"));

        mockMvc.perform(post("/api/messages/projects/1/investors/20")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INVESTOR"))
                                .jwt(j -> j.claim("userId", 20)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderRole").value("ROLE_INVESTOR"))
                .andExpect(jsonPath("$.content").value("I'm interested in this project!"));
    }

    @Test
    void send_anotherInvestorCannotPostInSomeoneElsesThread_returns403() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("content", "Trying to sneak in"));

        mockMvc.perform(post("/api/messages/projects/1/investors/20")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INVESTOR"))
                                .jwt(j -> j.claim("userId", 999)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void send_promoterRepliesInTheInvestorsThread_returns201() throws Exception {
        doNothing().when(projectClient).verifyApprovedOrThrow(1L);
        createInvestorMessage(1L, 20, "Initial message");

        doNothing().when(projectClient).verifyAccessOrThrow(1L);
        String body = objectMapper.writeValueAsString(Map.of("content", "Thanks for reaching out!"));

        mockMvc.perform(post("/api/messages/projects/1/investors/20")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 10)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderRole").value("ROLE_PROMOTER"));
    }

    @Test
    void send_aPromoterWhoDoesNotOwnTheProject_returns403() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(projectClient).verifyAccessOrThrow(1L);
        String body = objectMapper.writeValueAsString(Map.of("content", "Not my project"));

        mockMvc.perform(post("/api/messages/projects/1/investors/20")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 999)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void thread_returnsMessagesInOrder() throws Exception {
        doNothing().when(projectClient).verifyApprovedOrThrow(1L);
        createInvestorMessage(1L, 20, "First message");

        doNothing().when(projectClient).verifyAccessOrThrow(1L);
        mockMvc.perform(post("/api/messages/projects/1/investors/20")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 10)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Reply"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/messages/projects/1/investors/20")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[0].content").value("First message"))
                .andExpect(jsonPath("$.messages[1].content").value("Reply"));
    }

    @Test
    void conversationsForProject_listsEveryInvestorThread() throws Exception {
        doNothing().when(projectClient).verifyApprovedOrThrow(1L);
        createInvestorMessage(1L, 20, "Investor A");
        createInvestorMessage(1L, 30, "Investor B");

        doNothing().when(projectClient).verifyAccessOrThrow(1L);
        mockMvc.perform(get("/api/messages/projects/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    private void createInvestorMessage(Long projectId, int investorId, String content) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("content", content));

        mockMvc.perform(post("/api/messages/projects/" + projectId + "/investors/" + investorId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INVESTOR"))
                                .jwt(j -> j.claim("userId", investorId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }
}
