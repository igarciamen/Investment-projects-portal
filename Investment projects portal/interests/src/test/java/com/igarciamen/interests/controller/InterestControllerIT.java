package com.igarciamen.interests.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igarciamen.interests.repository.ExpressionOfInterestRepository;
import com.igarciamen.interests.service.ProjectClient;
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

// ProjectClient is mocked here (@MockitoBean): this test exercises interests'
// own logic, not the projects integration itself.
@SpringBootTest
@AutoConfigureMockMvc
class InterestControllerIT {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ExpressionOfInterestRepository interestRepo;

    @MockitoBean
    private ProjectClient projectClient;

    @AfterEach
    void cleanUp() {
        interestRepo.deleteAll();
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("projectId", 1, "message", "Interested"));

        mockMvc.perform(post("/api/interests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_withPromoterRole_returns403() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("projectId", 1, "message", "Interested"));

        mockMvc.perform(post("/api/interests")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withInvestorRoleOnAnApprovedProject_returns201() throws Exception {
        doNothing().when(projectClient).verifyApprovedOrThrow(1L);
        String body = objectMapper.writeValueAsString(Map.of("projectId", 1, "message", "Interested in this project"));

        mockMvc.perform(post("/api/interests")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INVESTOR"))
                                .jwt(j -> j.claim("userId", 20)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.investorId").value(20));
    }

    @Test
    void create_onANonApprovedProject_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(projectClient).verifyApprovedOrThrow(99L);
        String body = objectMapper.writeValueAsString(Map.of("projectId", 99, "message", "Interested"));

        mockMvc.perform(post("/api/interests")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INVESTOR"))
                                .jwt(j -> j.claim("userId", 20)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_twiceOnTheSameProject_returns409TheSecondTime() throws Exception {
        doNothing().when(projectClient).verifyApprovedOrThrow(1L);
        String body = objectMapper.writeValueAsString(Map.of("projectId", 1, "message", "Interested"));

        mockMvc.perform(post("/api/interests")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INVESTOR"))
                                .jwt(j -> j.claim("userId", 20)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/interests")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INVESTOR"))
                                .jwt(j -> j.claim("userId", 20)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void listByProject_verifiesAccessThroughProjects() throws Exception {
        doNothing().when(projectClient).verifyApprovedOrThrow(1L);
        createInterest(1L, 20);
        doNothing().when(projectClient).verifyAccessOrThrow(1L);

        mockMvc.perform(get("/api/interests/project/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listByProject_deniedByProjects_returns403() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(projectClient).verifyAccessOrThrow(1L);

        mockMvc.perform(get("/api/interests/project/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER"))
                                .jwt(j -> j.claim("userId", 999))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listMine_returnsOnlyTheAuthenticatedInvestorsExpressions() throws Exception {
        doNothing().when(projectClient).verifyApprovedOrThrow(1L);
        createInterest(1L, 20);

        mockMvc.perform(get("/api/interests/investor/me")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INVESTOR"))
                                .jwt(j -> j.claim("userId", 20))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].investorId").value(20));
    }

    private void createInterest(Long projectId, int investorId) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("projectId", projectId, "message", "Interested"));

        mockMvc.perform(post("/api/interests")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INVESTOR"))
                                .jwt(j -> j.claim("userId", investorId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }
}
