package com.igarciamen.sectors.repository;

import com.igarciamen.sectors.model.Sector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectorRepository extends JpaRepository<Sector, Long> {
    boolean existsByNameIgnoreCase(String name);

    // Public listing: active sectors only (used by the "create project" form
    // and by the future public project catalog / filters).
    List<Sector> findAllByActiveTrueOrderByNameAsc();

    // Admin listing: every sector, active and inactive, so it can be managed.
    List<Sector> findAllByOrderByNameAsc();
}
