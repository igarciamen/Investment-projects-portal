package com.igarciamen.users.utils;

import com.igarciamen.users.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtUtils {

    private final Key key;
    private final int jwtExpirationMs;

    public JwtUtils(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expirationMs}") int jwtExpirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = jwtExpirationMs;
    }

    public String generateJwtToken(User user) {
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("roles", roles)
                .claim("userId", user.getId())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key)
                .compact();
    }
}
