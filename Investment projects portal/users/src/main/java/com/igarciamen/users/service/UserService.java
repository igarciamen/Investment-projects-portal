package com.igarciamen.users.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.igarciamen.users.enums.ERole;
import com.igarciamen.users.model.Role;
import com.igarciamen.users.model.User;
import com.igarciamen.users.payloads.request.UpdateProfileRequest;
import com.igarciamen.users.repository.RoleRepository;
import com.igarciamen.users.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;

    public UserService(UserRepository userRepo,
                       RoleRepository roleRepo,
                       PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.encoder  = encoder;
    }

    public User registerUser(String username, String email, String rawPassword, String userType) {

        if (userRepo.existsByUsername(username)) {
            throw new IllegalArgumentException("User already exists: " + username);
        }
        if (userRepo.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is in use: " + email);
        }

        String encoded = encoder.encode(rawPassword);
        User user = new User(username, email, encoded);

        // Public signup only allows PROMOTER or INVESTOR; ROLE_ADMIN stays
        // reserved for the user seeded by DataLoader.
        ERole role = toAssignableRole(userType);
        Role userRole = roleRepo.findByName(role)
                .orElseThrow(() -> new IllegalStateException(role + " does not exist"));
        user.getRoles().add(userRole);

        return userRepo.save(user);
    }

    private ERole toAssignableRole(String userType) {
        if ("PROMOTER".equalsIgnoreCase(userType)) {
            return ERole.ROLE_PROMOTER;
        }
        if ("INVESTOR".equalsIgnoreCase(userType)) {
            return ERole.ROLE_INVESTOR;
        }
        throw new IllegalArgumentException("Invalid userType: " + userType);
    }

    // Partial update: only the profile/organisation fields, on purpose --
    // username/email/password/roles are never touched here, this endpoint
    // has nothing to do with account security or identity.
    public User updateProfile(Long userId, UpdateProfileRequest req) {
        User user = findById(userId);
        user.setCountry(req.getCountry());
        user.setOccupation(req.getOccupation());
        user.setPreferredContactLanguage(req.getPreferredContactLanguage());
        user.setOrganisationName(req.getOrganisationName());
        user.setOrganisationCountry(req.getOrganisationCountry());
        return userRepo.save(user);
    }

    public User findByUsername(String username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    public User findByUsernameOrEmail(String login) {
        return userRepo.findByUsernameOrEmail(login, login)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + login));
    }

    public User findById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }
}