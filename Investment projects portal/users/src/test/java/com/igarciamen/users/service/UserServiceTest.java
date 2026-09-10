package com.igarciamen.users.service;

import com.igarciamen.users.enums.ERole;
import com.igarciamen.users.model.Role;
import com.igarciamen.users.model.User;
import com.igarciamen.users.repository.RoleRepository;
import com.igarciamen.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepo;
    @Mock private RoleRepository roleRepo;
    @Mock private PasswordEncoder encoder;

    @InjectMocks private UserService userService;

    @Test
    void registerUser_createsPromoterWithRoleAndEncryptedPassword() {
        when(userRepo.existsByUsername("pepe")).thenReturn(false);
        when(userRepo.existsByEmail("pepe@mail.com")).thenReturn(false);
        when(encoder.encode("123456")).thenReturn("HASH_BCRYPT");
        when(roleRepo.findByName(ERole.ROLE_PROMOTER)).thenReturn(Optional.of(new Role(ERole.ROLE_PROMOTER)));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.registerUser("pepe", "pepe@mail.com", "123456", "PROMOTER");

        assertEquals("pepe", result.getUsername());
        assertEquals("HASH_BCRYPT", result.getPassword());
        assertEquals(1, result.getRoles().size());

        System.out.println("=== registerUser: successful signup (PROMOTER) ===");
        System.out.println("User created        : " + result.getUsername());
        System.out.println("Password stored      : " + result.getPassword() + " (encrypted, not plain text)");
        System.out.println("Roles assigned        : " + result.getRoles().size() + " (ROLE_PROMOTER)");
    }

    @Test
    void registerUser_createsInvestorWithRoleAndEncryptedPassword() {
        when(userRepo.existsByUsername("ana")).thenReturn(false);
        when(userRepo.existsByEmail("ana@mail.com")).thenReturn(false);
        when(encoder.encode("123456")).thenReturn("HASH_BCRYPT");
        when(roleRepo.findByName(ERole.ROLE_INVESTOR)).thenReturn(Optional.of(new Role(ERole.ROLE_INVESTOR)));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.registerUser("ana", "ana@mail.com", "123456", "INVESTOR");

        assertEquals("ana", result.getUsername());
        assertEquals(1, result.getRoles().size());

        System.out.println("=== registerUser: successful signup (INVESTOR) ===");
        System.out.println("Roles assigned        : " + result.getRoles().size() + " (ROLE_INVESTOR)");
    }

    @Test
    void registerUser_failsWhenUserTypeIsInvalid() {
        when(userRepo.existsByUsername("pepe")).thenReturn(false);
        when(userRepo.existsByEmail("pepe@mail.com")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser("pepe", "pepe@mail.com", "123456", "ADMIN"));

        verify(userRepo, never()).save(any());
        System.out.println("=== registerUser: invalid userType ===");
        System.out.println("Signup rejected with message: " + ex.getMessage());
    }

    @Test
    void registerUser_failsWhenUsernameAlreadyExists() {
        when(userRepo.existsByUsername("pepe")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser("pepe", "x@mail.com", "123456", "PROMOTER"));

        verify(userRepo, never()).save(any());
        System.out.println("=== registerUser: duplicate username ===");
        System.out.println("Signup rejected with message: " + ex.getMessage());
    }

    @Test
    void registerUser_failsWhenEmailIsAlreadyInUse() {
        when(userRepo.existsByUsername("pepe")).thenReturn(false);
        when(userRepo.existsByEmail("pepe@mail.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser("pepe", "pepe@mail.com", "123456", "PROMOTER"));

        verify(userRepo, never()).save(any());
        System.out.println("=== registerUser: email already in use ===");
        System.out.println("Signup rejected with message: " + ex.getMessage());
    }

    @Test
    void findByUsernameOrEmail_returnsUserWhenItExists() {
        User u = new User("ana", "ana@mail.com", "h");
        when(userRepo.findByUsernameOrEmail("ana", "ana")).thenReturn(Optional.of(u));

        User result = userService.findByUsernameOrEmail("ana");

        assertEquals("ana", result.getUsername());
        System.out.println("=== findByUsernameOrEmail: found ===");
        System.out.println("Searched 'ana' and found user: " + result.getUsername());
    }

    @Test
    void findByUsernameOrEmail_throwsWhenNotFound() {
        when(userRepo.findByUsernameOrEmail("nobody", "nobody")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.findByUsernameOrEmail("nobody"));

        System.out.println("=== findByUsernameOrEmail: not found ===");
        System.out.println("Error message: " + ex.getMessage());
    }

    @Test
    void findById_returnsUserWhenItExists() {
        User u = new User("ana", "ana@mail.com", "h");
        when(userRepo.findById(5L)).thenReturn(Optional.of(u));

        User result = userService.findById(5L);

        assertEquals("ana", result.getUsername());
        System.out.println("=== findById: found ===");
        System.out.println("Searched id=5 and found: " + result.getUsername());
    }

    @Test
    void findById_throwsWhenNotFound() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.findById(99L));

        System.out.println("=== findById: not found ===");
        System.out.println("Error message: " + ex.getMessage());
    }
}
