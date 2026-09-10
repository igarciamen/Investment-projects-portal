package com.igarciamen.documents.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;

// Same pattern as in "projects": forwards the incoming request's Authorization
// header to the outgoing call to "projects", so ProjectClient can check access
// with the same token the promoter/admin used to call documents.
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(Collections.singletonList(authForwardingInterceptor()));
        return restTemplate;
    }

    private ClientHttpRequestInterceptor authForwardingInterceptor() {
        return (request, body, execution) -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest incoming = attrs.getRequest();
                String authHeader = incoming.getHeader(HttpHeaders.AUTHORIZATION);
                if (authHeader != null && !authHeader.isBlank()
                        && !request.getHeaders().containsHeader(HttpHeaders.AUTHORIZATION)) {
                    request.getHeaders().set(HttpHeaders.AUTHORIZATION, authHeader);
                }
            }
            return execution.execute(request, body);
        };
    }
}
