package com.igarciamen.users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igarciamen.users.payloads.request.LoginRequest;
import com.igarciamen.users.payloads.request.SignupRequest;
import com.igarciamen.users.service.AuthService;
import com.igarciamen.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @Test
    void signup_ok() throws Exception {
        SignupRequest req = new SignupRequest("john", "john@example.com", "1234", "PROMOTER");

        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andReturn();

        System.out.println("=== POST /api/auth/signup ===");
        System.out.println("Status: " + result.getResponse().getStatus());
        System.out.println("Body  : " + result.getResponse().getContentAsString());
    }

    @Test
    void login_ok() throws Exception {
        LoginRequest req = new LoginRequest("john", "1234");
        when(authService.authenticate("john", "1234")).thenReturn("token-123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-123"))
                .andReturn();

        System.out.println("=== POST /api/auth/login ===");
        System.out.println("Status: " + result.getResponse().getStatus());
        System.out.println("Body  : " + result.getResponse().getContentAsString());
    }
}
