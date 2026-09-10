package com.igarciamen.sectors.payloads.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SectorRequest {

    @NotBlank
    @Size(max = 80)
    private String name;

    @Size(max = 255)
    private String description;

    public SectorRequest() {}

    public SectorRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
