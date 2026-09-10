package com.igarciamen.users.utils;

import com.igarciamen.users.enums.ERole;
import com.igarciamen.users.model.Role;
import com.igarciamen.users.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private static final String SECRET =
            "test-secret-key-for-jwt-1234567890-abcdefghijklmnopqrstuvwxyz";
    private static final int EXPIRATION_MS = 86_400_000; // 24 hours

    @Test
    void generateJwtToken_containsTheExpectedClaims() {
        JwtUtils jwtUtils = new JwtUtils(SECRET, EXPIRATION_MS);

        User user = new User("isabel", "isabel@admin.local", "hashed");
        user.setId(1L);
        user.getRoles().add(new Role(ERole.ROLE_ADMIN));

        String token = jwtUtils.generateJwtToken(user);

        assertNotNull(token);
        // A JWT has three parts separated by dots: header.payload.signature
        assertEquals(3, token.split("\\.").length);

        Claims claims = parse(token);
        assertEquals("isabel", claims.getSubject());
        assertEquals(1, ((Number) claims.get("userId")).intValue());

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        assertTrue(roles.contains("ROLE_ADMIN"));

        System.out.println("=== Test 1: token claims ===");
        System.out.println("Generated token: " + token);
        System.out.println("subject : " + claims.getSubject());
        System.out.println("userId  : " + claims.get("userId"));
        System.out.println("roles   : " + roles);
    }

    @Test
    void generateJwtToken_setsIssuedAtAndExpiration() {
        JwtUtils jwtUtils = new JwtUtils(SECRET, EXPIRATION_MS);

        User user = new User("marco", "marco@mail.com", "hashed");
        user.setId(2L);
        user.getRoles().add(new Role(ERole.ROLE_INVESTOR));

        String token = jwtUtils.generateJwtToken(user);
        Claims claims = parse(token);

        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();
        assertNotNull(issuedAt);
        assertNotNull(expiration);

        // Expiration should be roughly 24h after issuance.
        long diff = expiration.getTime() - issuedAt.getTime();
        assertTrue(Math.abs(diff - EXPIRATION_MS) <= 1000);
        assertTrue(expiration.after(new Date()));

        System.out.println("=== Test 2: issued at and expiration ===");
        System.out.println("Issued at : " + issuedAt);
        System.out.println("Expires at: " + expiration);
        System.out.println("Duration  : " + (diff / 1000 / 60 / 60) + " hours");
    }

    @Test
    void generateJwtToken_invalidSignatureWithAnotherKey() {
        JwtUtils jwtUtils = new JwtUtils(SECRET, EXPIRATION_MS);
        User user = new User("lucia", "lucia@mail.com", "hashed");
        user.setId(3L);
        user.getRoles().add(new Role(ERole.ROLE_PROMOTER));

        String token = jwtUtils.generateJwtToken(user);

        // With a different secret, signature verification must fail.
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "a-totally-different-secret-key-1234567890-abcdefgh".getBytes(StandardCharsets.UTF_8));
        Exception ex = assertThrows(Exception.class, () ->
                Jwts.parser().verifyWith(otherKey).build().parseSignedClaims(token));

        System.out.println("=== Test 3: invalid signature with another key ===");
        System.out.println("The wrong key rejects the token: "
                + ex.getClass().getSimpleName());
    }

    private Claims parse(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
