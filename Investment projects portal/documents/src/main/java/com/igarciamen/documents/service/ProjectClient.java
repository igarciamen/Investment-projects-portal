package com.igarciamen.documents.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

// Calls "projects" (GET /api/projects/{id}) to check whether the caller has
// access to that project. That logic is NOT duplicated here: "projects"
// already knows who the owning promoter is (and whether the caller is an
// admin), documents just asks -- forwarding the caller's own token, which
// RestTemplateConfig's interceptor takes care of.
@Component
public class ProjectClient {

    private final RestTemplate http;
    private final String projectsBase;

    public ProjectClient(RestTemplate http, @Value("${projects.base-url}") String projectsBaseUrl) {
        this.http = http;
        this.projectsBase = projectsBaseUrl;
    }

    // Throws the SAME status "projects" returned (403/404) if there is no
    // access, or 503 if "projects" is unreachable. Returns normally if the
    // caller does have access.
    public void verifyAccessOrThrow(Long projectId) {
        fetchProjectOrThrow(projectId);
    }

    // Same call as verifyAccessOrThrow, but returning the project's data
    // (namely promoterId) instead of discarding it -- used to notify the
    // promoter when an admin uploads a document.
    public Map<String, Object> fetchProjectOrThrow(Long projectId) {
        try {
            return http.getForObject(projectsBase + "/" + projectId, Map.class);
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getStatusCode().value()),
                    "No access to project " + projectId);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not verify access to the project: " + e.getMessage());
        }
    }
}