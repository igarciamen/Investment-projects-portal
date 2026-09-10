package com.igarciamen.users.controller;

import com.igarciamen.users.enums.ERole;
import com.igarciamen.users.model.Role;
import com.igarciamen.users.model.User;
import com.igarciamen.users.payloads.response.UserInfoResponse;
import com.igarciamen.users.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

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
    void me_authenticated_return_user() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("john");

        User user = new User("john", "john@example.com", "1234");
        user.setId(1L);
        user.setRoles(Set.of(new Role(ERole.ROLE_PROMOTER)));
        when(userService.findByUsername("john")).thenReturn(user);

        ResponseEntity<UserInfoResponse> response = userController.me(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getUsername()).isEqualTo("john");
        assertThat(response.getBody().getRoles()).containsExactly("ROLE_PROMOTER");

        System.out.println("=== me_authenticated_return_user ===");
        System.out.println("Status  : " + response.getStatusCode());
        System.out.println("Username: " + response.getBody().getUsername());
        System.out.println("Roles   : " + response.getBody().getRoles());
    }

    @Test
    void getById_user_exist() {
        User user = new User("jane", "jane@example.com", "1234");
        user.setId(5L);
        user.setRoles(Set.of(new Role(ERole.ROLE_PROMOTER)));
        when(userService.findById(5L)).thenReturn(user);

        ResponseEntity<UserInfoResponse> response = userController.getById(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getUsername()).isEqualTo("jane");

        System.out.println("=== getById_user_exist ===");
        System.out.println("Status  : " + response.getStatusCode());
        System.out.println("Username: " + response.getBody().getUsername());
    }
}
