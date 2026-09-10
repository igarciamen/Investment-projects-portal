package com.igarciamen.users.controller;

import com.igarciamen.users.payloads.request.LoginRequest;
import com.igarciamen.users.payloads.request.SignupRequest;
import com.igarciamen.users.payloads.response.JwtResponse;
import com.igarciamen.users.payloads.response.MessageResponse;
import com.igarciamen.users.service.AuthService;
import com.igarciamen.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping(path = "/signup",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody SignupRequest req) {

        userService.registerUser(req.getUsername(), req.getEmail(), req.getPassword(), req.getUserType());
        return ResponseEntity.ok(new MessageResponse("User registered successfully"));
    }

    @PostMapping(path = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest req) {
        String token = authService.authenticate(req.getLogin(), req.getPassword());
        return ResponseEntity.ok(new JwtResponse(token));
    }
}
