package com.igarciamen.sectors.payloads.response;

import com.igarciamen.sectors.model.Sector;

public class SectorResponse {

    private Long id;
    private String name;
    private String description;
    private boolean active;

    public SectorResponse() {}

    public SectorResponse(Long id, String name, String description, boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.active = active;
    }

    public static SectorResponse from(Sector sector) {
        return new SectorResponse(sector.getId(), sector.getName(), sector.getDescription(), sector.isActive());
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
}
