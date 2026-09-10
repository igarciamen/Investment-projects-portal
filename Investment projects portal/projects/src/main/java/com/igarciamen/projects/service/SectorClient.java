package com.igarciamen.projects.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;

// Isolates the HTTP call to the "sectors" microservice from the rest of the
// business code, same pattern as CategoryClient in tasks (SecGest).
@Component
public class SectorClient {

    private final RestTemplate http;
    private final String sectorsBase;

    public SectorClient(RestTemplate http,
                        @Value("${sectors.base-url}") String sectorsBaseUrl) {
        this.http = http;
        this.sectorsBase = sectorsBaseUrl;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchSectorOrThrow(Long id) {
        try {
            var body = http.getForObject(sectorsBase + "/" + id, Map.class);
            if (body == null || !Objects.equals(((Number) body.getOrDefault("id", -1)).longValue(), id)) {
                throw new EntityNotFoundException("Sector not found: " + id);
            }
            return body;
        } catch (RestClientException e) {
            throw new IllegalStateException("Error calling the sectors service: " + e.getMessage(), e);
        }
    }
}
