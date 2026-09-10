package com.igarciamen.users.controller;

import com.igarciamen.users.payloads.request.LoginRequest;
import com.igarciamen.users.payloads.request.SignupRequest;
import com.igarciamen.users.payloads.response.JwtResponse;
import com.igarciamen.users.payloads.response.MessageResponse;
import com.igarciamen.users.service.AuthService;
import com.igarciamen.users.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;


    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void register_ok() {
        SignupRequest req = new SignupRequest("john", "john@example.com", "1234", "PROMOTER");

        ResponseEntity<MessageResponse> response = authController.register(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getMessage()).isEqualTo("User registered successfully");

        verify(userService).registerUser("john", "john@example.com", "1234", "PROMOTER");

        System.out.println("=== register_ok ===");
        System.out.println("Status : " + response.getStatusCode());
        System.out.println("Message: " + response.getBody().getMessage());
    }

    @Test
    void login_ok() {
        LoginRequest req = new LoginRequest("john", "1234");
        when(authService.authenticate("john", "1234")).thenReturn("token-123");

        ResponseEntity<JwtResponse> response = authController.login(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getToken()).isEqualTo("token-123");

        System.out.println("=== login_ok ===");
        System.out.println("Status: " + response.getStatusCode());
        System.out.println("Token : " + response.getBody().getToken());
    }
}
