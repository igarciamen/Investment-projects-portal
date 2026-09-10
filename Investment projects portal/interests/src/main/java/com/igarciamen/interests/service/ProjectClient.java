package com.igarciamen.interests.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Objects;

@Component
public class ProjectClient {

    private final RestTemplate http;
    private final String projectsBase;

    public ProjectClient(RestTemplate http, @Value("${projects.base-url}") String projectsBaseUrl) {
        this.http = http;
        this.projectsBase = projectsBaseUrl;
    }

    // Calls the PUBLIC detail endpoint (GET /api/projects/public/{id}), not the
    // protected GET /api/projects/{id}: an investor is neither the project's
    // owner nor an admin, so the protected endpoint would always answer 403 for
    // them. The public endpoint only ever returns a project that is APPROVED,
    // so a successful response here already proves both "exists" and "approved".
    public void verifyApprovedOrThrow(Long projectId) {
        try {
            var body = http.getForObject(projectsBase + "/public/" + projectId, Map.class);
            if (body == null || !Objects.equals(((Number) body.getOrDefault("id", -1)).longValue(), projectId)) {
                throw new EntityNotFoundException("Approved project not found: " + projectId);
            }
        } catch (RestClientException e) {
            throw new EntityNotFoundException("Approved project not found: " + projectId);
        }
    }

    // For "who is interested in my project" (GET /api/interests/project/{id}):
    // calls the PROTECTED GET /api/projects/{id}, forwarding the caller's own
    // token, and simply propagates whatever status projects returns (200 if
    // the caller is the owning promoter or an admin, 403/404 otherwise). The
    // owner/admin decision is NOT duplicated here, same principle as
    // documents' ProjectClient.
    public void verifyAccessOrThrow(Long projectId) {
        try {
            http.getForObject(projectsBase + "/" + projectId, Map.class);
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getStatusCode().value()),
                    "No access to project " + projectId);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not verify access to the project: " + e.getMessage());
        }
    }
}
