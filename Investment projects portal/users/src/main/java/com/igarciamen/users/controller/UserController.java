package com.igarciamen.users.controller;

import com.igarciamen.users.model.User;
import com.igarciamen.users.payloads.request.UpdateProfileRequest;
import com.igarciamen.users.payloads.response.UserInfoResponse;
import com.igarciamen.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "Returns user info",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserInfoResponse> me(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        return ResponseEntity.ok(toResponse(user));
    }

    @Operation(
            summary = "Updates the signed-in user's own profile (organisation/contact fields)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping(path = "/me/profile", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserInfoResponse> updateMyProfile(Authentication authentication,
                                                            @Valid @RequestBody UpdateProfileRequest req) {
        User user = userService.findByUsername(authentication.getName());
        User updated = userService.updateProfile(user.getId(), req);
        return ResponseEntity.ok(toResponse(updated));
    }

    @Operation(
            summary = "Returns user by ID",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserInfoResponse> getById(@PathVariable Long id) {
        try {
            User user = userService.findById(id);
            return ResponseEntity.ok(toResponse(user));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id);
        }
    }

    @Operation(summary = "Returns public user info (id and username only)")
    @GetMapping(path = "/{id}/public", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getPublicById(@PathVariable Long id) {
        try {
            User user = userService.findById(id);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("id", user.getId());
            body.put("username", user.getUsername());
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id);
        }
    }

    private UserInfoResponse toResponse(User user) {
        Set<String> roles = user.getRoles()
                .stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        return new UserInfoResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roles,
                user.getCountry(),
                user.getOccupation(),
                user.getPreferredContactLanguage(),
                user.getOrganisationName(),
                user.getOrganisationCountry()
        );
    }
}