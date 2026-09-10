package com.igarciamen.users.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.igarciamen.users.model.User;
import com.igarciamen.users.utils.JwtUtils;


@Service
public class AuthService {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserService userService,
            JwtUtils jwtUtils,
            PasswordEncoder passwordEncoder
    ) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
    }

    public String authenticate(String login, String rawPassword) {
        User user;
        try {
            user = userService.findByUsernameOrEmail(login);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong user or password");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong user or password");
        }
        return jwtUtils.generateJwtToken(user);
    }

}
