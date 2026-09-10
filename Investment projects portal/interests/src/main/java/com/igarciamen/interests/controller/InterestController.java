package com.igarciamen.interests.controller;

import com.igarciamen.interests.model.ExpressionOfInterest;
import com.igarciamen.interests.payloads.request.CreateInterestRequest;
import com.igarciamen.interests.payloads.response.InterestResponse;
import com.igarciamen.interests.service.InterestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/interests")
public class InterestController {

    private final InterestService interestService;

    public InterestController(InterestService interestService) {
        this.interestService = interestService;
    }

    @Operation(
            summary = "Expresses interest in an APPROVED project (ROLE_INVESTOR only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InterestResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                    @Valid @RequestBody CreateInterestRequest req) {
        ExpressionOfInterest saved = interestService.create(extractUserId(jwt), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(InterestResponse.from(saved));
    }

    @Operation(
            summary = "Lists who has expressed interest in a project (its owning promoter, or any admin)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/project/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InterestResponse>> listByProject(@PathVariable Long projectId) {
        List<InterestResponse> body = interestService.listByProject(projectId).stream()
                .map(InterestResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @Operation(
            summary = "Lists the authenticated investor's own expressions of interest (\"My expressions of interest\")",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/investor/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InterestResponse>> listMine(@AuthenticationPrincipal Jwt jwt) {
        List<InterestResponse> body = interestService.listByInvestor(extractUserId(jwt)).stream()
                .map(InterestResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    private Long extractUserId(Jwt jwt) {
        Object claim = jwt.getClaim("userId");
        if (claim == null) {
            throw new IllegalStateException("The token does not contain the 'userId' claim");
        }
        return ((Number) claim).longValue();
    }
}
