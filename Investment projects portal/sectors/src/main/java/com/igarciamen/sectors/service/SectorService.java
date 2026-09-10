package com.igarciamen.sectors.service;

import com.igarciamen.sectors.model.Sector;
import com.igarciamen.sectors.payloads.request.SectorRequest;
import com.igarciamen.sectors.repository.SectorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SectorService {

    private final SectorRepository sectorRepo;

    public SectorService(SectorRepository sectorRepo) {
        this.sectorRepo = sectorRepo;
    }

    // Public catalog: active sectors only.
    public List<Sector> listPublic() {
        return sectorRepo.findAllByActiveTrueOrderByNameAsc();
    }

    // Admin panel: every sector, active and inactive.
    public List<Sector> listAllForAdmin() {
        return sectorRepo.findAllByOrderByNameAsc();
    }

    public Sector getById(Long id) {
        return sectorRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sector not found: " + id));
    }

    public Sector create(SectorRequest req) {
        if (sectorRepo.existsByNameIgnoreCase(req.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sector already exists: " + req.getName());
        }
        Sector sector = new Sector(req.getName(), req.getDescription());
        return sectorRepo.save(sector);
    }

    public Sector update(Long id, SectorRequest req) {
        Sector sector = getById(id);
        sector.setName(req.getName());
        sector.setDescription(req.getDescription());
        return sectorRepo.save(sector);
    }

    // Soft delete: the row is not removed, so projects that already reference
    // this sector (by id) can keep resolving its name via SectorClient.
    public Sector deactivate(Long id) {
        Sector sector = getById(id);
        sector.setActive(false);
        return sectorRepo.save(sector);
    }

    public Sector activate(Long id) {
        Sector sector = getById(id);
        sector.setActive(true);
        return sectorRepo.save(sector);
    }
}
