package com.igarciamen.sectors.controller;

import com.igarciamen.sectors.model.Sector;
import com.igarciamen.sectors.payloads.request.SectorRequest;
import com.igarciamen.sectors.payloads.response.SectorResponse;
import com.igarciamen.sectors.service.SectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sectors")
public class SectorController {

    private final SectorService sectorService;

    public SectorController(SectorService sectorService) {
        this.sectorService = sectorService;
    }

    @Operation(summary = "Lists the active sectors (public, no token needed)")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SectorResponse>> listAll() {
        return ResponseEntity.ok(toResponseList(sectorService.listPublic()));
    }

    @Operation(
            summary = "Lists EVERY sector, active and inactive (ROLE_ADMIN only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SectorResponse>> listAllForAdmin() {
        return ResponseEntity.ok(toResponseList(sectorService.listAllForAdmin()));
    }

    // Also public: consumed by the frontend and, above all, by "projects"
    // (RestTemplate) to validate the sector when creating a project. Returns
    // the sector even if it is inactive: an existing project must still be
    // able to display its sector's name.
    @Operation(summary = "Gets a sector by id (public, no token needed)")
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SectorResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(SectorResponse.from(sectorService.getById(id)));
    }

    @Operation(
            summary = "Creates a sector (ROLE_ADMIN only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SectorResponse> create(@Valid @RequestBody SectorRequest req) {
        Sector created = sectorService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(SectorResponse.from(created));
    }

    @Operation(
            summary = "Edits an existing sector (ROLE_ADMIN only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SectorResponse> update(@PathVariable Long id, @Valid @RequestBody SectorRequest req) {
        return ResponseEntity.ok(SectorResponse.from(sectorService.update(id, req)));
    }

    @Operation(
            summary = "Deactivates a sector (soft delete, ROLE_ADMIN only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<SectorResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(SectorResponse.from(sectorService.deactivate(id)));
    }

    @Operation(
            summary = "Reactivates a sector (ROLE_ADMIN only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping(path = "/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SectorResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(SectorResponse.from(sectorService.activate(id)));
    }

    private List<SectorResponse> toResponseList(List<Sector> sectors) {
        return sectors.stream().map(SectorResponse::from).collect(Collectors.toList());
    }
}
