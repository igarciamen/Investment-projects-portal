package com.igarciamen.sectors.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "sectors",
        schema = "public",
        uniqueConstraints = @UniqueConstraint(columnNames = "name")
)
public class Sector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 255)
    private String description;

    // Soft delete: instead of deleting the row, it gets deactivated. This way
    // a project that already references this sector (by id) can still resolve
    // its name via SectorClient, and past data stays consistent.
    @Column(nullable = false)
    private boolean active = true;

    public Sector() {}

    public Sector(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
