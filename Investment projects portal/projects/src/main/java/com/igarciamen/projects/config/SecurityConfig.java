package com.igarciamen.projects.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", "/webjars/**").permitAll()
                        // Public catalog for investors (Block 5): no token needed, same as
                        // InvestEU's real EIPP portal, which lets anyone browse projects.
                        .requestMatchers(HttpMethod.GET, "/api/projects/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/projects/public/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/projects").hasAuthority("ROLE_PROMOTER")
                        .requestMatchers(HttpMethod.PUT, "/api/projects/*").hasAuthority("ROLE_PROMOTER")
                        // The promoter submits their own project; the admin reviews/decides.
                        .requestMatchers(HttpMethod.PATCH, "/api/projects/*/submit").hasAuthority("ROLE_PROMOTER")
                        .requestMatchers(HttpMethod.PATCH, "/api/projects/*/review").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/projects/*/approve").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/projects/*/reject").hasAuthority("ROLE_ADMIN")
                        // Global listing (all promoters): ROLE_ADMIN only.
                        .requestMatchers(HttpMethod.GET, "/api/projects/all").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/projects/pending-evaluation").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/projects/metrics").hasAuthority("ROLE_ADMIN")
                        // /api/projects/mine and /api/projects/{id}: any authenticated user
                        // (ownership/permission is checked in ProjectService, not here).
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        SecretKey key = new SecretKeySpec(
                jwtSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    private JwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter granted = new JwtGrantedAuthoritiesConverter();
        granted.setAuthoritiesClaimName("roles");
        granted.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(granted);
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        // PATCH added: the four status-transition endpoints (submit/review/
        // approve/reject) use @PatchMapping. Without PATCH here, the browser's
        // CORS preflight (OPTIONS) for those requests gets rejected before the
        // real request is even attempted -- POST/PUT/GET/DELETE calls were
        // never affected by this, only PATCH ones.
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}