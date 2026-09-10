package com.igarciamen.users.service;

import com.igarciamen.users.model.User;
import com.igarciamen.users.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserService userService;
    @Mock private JwtUtils jwtUtils;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AuthService authService;

    @Test
    void authenticate_returnsTokenWithValidCredentials() {
        User user = new User("isabel", "isabel@admin.local", "HASH");
        when(userService.findByUsernameOrEmail("isabel")).thenReturn(user);
        when(passwordEncoder.matches("123456", "HASH")).thenReturn(true);
        when(jwtUtils.generateJwtToken(user)).thenReturn("token-xyz");

        String token = authService.authenticate("isabel", "123456");

        assertEquals("token-xyz", token);
        System.out.println("=== authenticate: correct credentials ===");
        System.out.println("Login 'isabel' + correct password => token: " + token);
    }

    @Test
    void authenticate_wrongPasswordReturns401() {
        User user = new User("isabel", "isabel@admin.local", "HASH");
        when(userService.findByUsernameOrEmail("isabel")).thenReturn(user);
        when(passwordEncoder.matches("terrible", "HASH")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.authenticate("isabel", "terrible"));

        assertEquals(401, ex.getStatusCode().value());
        verify(jwtUtils, never()).generateJwtToken(any());
        System.out.println("=== authenticate: wrong password ===");
        System.out.println("Result: " + ex.getStatusCode().value() + " (no token generated)");
    }

    @Test
    void authenticate_nonExistentUserReturns401() {
        when(userService.findByUsernameOrEmail("ghost"))
                .thenThrow(new IllegalArgumentException("User not found: ghost"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.authenticate("ghost", "123456"));

        assertEquals(401, ex.getStatusCode().value());
        System.out.println("=== authenticate: non-existent user ===");
        System.out.println("Result: " + ex.getStatusCode().value()
                + " with the same generic message 'Wrong user or password'");
    }
}
