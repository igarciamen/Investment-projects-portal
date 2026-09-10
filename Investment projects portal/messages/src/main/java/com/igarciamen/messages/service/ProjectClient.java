package com.igarciamen.messages.service;

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

    // An investor can only open/use a thread on a project that is APPROVED --
    // same reasoning as interests: the public detail endpoint only ever
    // returns approved projects, so a successful call already proves it.
    public void verifyApprovedOrThrow(Long projectId) {
        fetchApprovedProjectOrThrow(projectId);
    }

    // Same public GET /api/projects/public/{id} call as verifyApprovedOrThrow,
    // but returning the project's data (namely promoterId) instead of
    // discarding it. Crucially, this is the PUBLIC endpoint -- it needs no
    // particular role -- so it is safe to call even when the current caller
    // is the investor themselves (who has no access to the protected
    // GET /api/projects/{id}). Used by MessagingService to notify the
    // promoter of a new message from an investor.
    public Map<String, Object> fetchApprovedProjectOrThrow(Long projectId) {
        try {
            var body = http.getForObject(projectsBase + "/public/" + projectId, Map.class);
            if (body == null || !Objects.equals(((Number) body.getOrDefault("id", -1)).longValue(), projectId)) {
                throw new EntityNotFoundException("Approved project not found: " + projectId);
            }
            return body;
        } catch (RestClientException e) {
            throw new EntityNotFoundException("Approved project not found: " + projectId);
        }
    }

    // For the promoter/admin listing endpoint: delegates to projects' own
    // GET /api/projects/{id} (owner-or-admin), forwarding the caller's token.
    // The ownership decision is never duplicated here. Unlike
    // fetchApprovedProjectOrThrow above, this one DOES require the caller to
    // actually be the owner or an admin -- it must only ever be called with
    // a promoter/admin token, never with an investor's.
    public void verifyAccessOrThrow(Long projectId) {
        fetchProjectOrThrow(projectId);
    }

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