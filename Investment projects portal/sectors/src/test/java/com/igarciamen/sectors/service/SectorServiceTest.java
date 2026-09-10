package com.igarciamen.sectors.service;

import com.igarciamen.sectors.model.Sector;
import com.igarciamen.sectors.payloads.request.SectorRequest;
import com.igarciamen.sectors.repository.SectorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SectorServiceTest {

    @Mock private SectorRepository sectorRepo;

    @InjectMocks private SectorService sectorService;

    @Test
    void create_createsANewActiveSector() {
        when(sectorRepo.existsByNameIgnoreCase("Renewable Energy")).thenReturn(false);
        when(sectorRepo.save(any(Sector.class))).thenAnswer(inv -> {
            Sector s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        Sector result = sectorService.create(new SectorRequest("Renewable Energy", "Solar, wind, and hydro projects"));

        assertEquals("Renewable Energy", result.getName());
        assertTrue(result.isActive());
        System.out.println("=== create: sector created and active ===");
        System.out.println("Name: " + result.getName() + " | Active: " + result.isActive());
    }

    @Test
    void create_rejectsADuplicateName() {
        when(sectorRepo.existsByNameIgnoreCase("Renewable Energy")).thenReturn(true);

        org.springframework.web.server.ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> sectorService.create(new SectorRequest("Renewable Energy", "desc")));

        assertEquals(409, ex.getStatusCode().value());
        verify(sectorRepo, never()).save(any());
        System.out.println("=== create: 409 for a duplicate name ===");
    }

    @Test
    void listPublic_returnsOnlyActiveSectors() {
        when(sectorRepo.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(new Sector("A", "d")));

        List<Sector> result = sectorService.listPublic();

        assertEquals(1, result.size());
        verify(sectorRepo).findAllByActiveTrueOrderByNameAsc();
        System.out.println("=== listPublic: only active sectors returned ===");
    }

    @Test
    void listAllForAdmin_returnsActiveAndInactive() {
        when(sectorRepo.findAllByOrderByNameAsc()).thenReturn(List.of(new Sector("A", "d"), new Sector("B", "d")));

        List<Sector> result = sectorService.listAllForAdmin();

        assertEquals(2, result.size());
        verify(sectorRepo).findAllByOrderByNameAsc();
        System.out.println("=== listAllForAdmin: active and inactive sectors returned ===");
    }

    @Test
    void getById_throws404WhenNotFound() {
        when(sectorRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> sectorService.getById(99L));

        assertEquals(404, ex.getStatusCode().value());
        System.out.println("=== getById: 404 when the sector does not exist ===");
    }

    @Test
    void deactivate_setsActiveToFalseWithoutDeletingTheRow() {
        Sector sector = new Sector("Agriculture", "Agro-food projects");
        sector.setId(1L);
        when(sectorRepo.findById(1L)).thenReturn(Optional.of(sector));
        when(sectorRepo.save(any(Sector.class))).thenAnswer(inv -> inv.getArgument(0));

        Sector result = sectorService.deactivate(1L);

        assertFalse(result.isActive());
        verify(sectorRepo, never()).delete(any());
        verify(sectorRepo, never()).deleteById(any());
        System.out.println("=== deactivate: soft delete, row is not removed ===");
    }

    @Test
    void activate_setsActiveBackToTrue() {
        Sector sector = new Sector("Agriculture", "Agro-food projects");
        sector.setId(1L);
        sector.setActive(false);
        when(sectorRepo.findById(1L)).thenReturn(Optional.of(sector));
        when(sectorRepo.save(any(Sector.class))).thenAnswer(inv -> inv.getArgument(0));

        Sector result = sectorService.activate(1L);

        assertTrue(result.isActive());
        System.out.println("=== activate: sector reactivated ===");
    }

    @Test
    void update_changesNameAndDescription() {
        Sector sector = new Sector("Old name", "Old description");
        sector.setId(1L);
        when(sectorRepo.findById(1L)).thenReturn(Optional.of(sector));
        when(sectorRepo.save(any(Sector.class))).thenAnswer(inv -> inv.getArgument(0));

        Sector result = sectorService.update(1L, new SectorRequest("New name", "New description"));

        assertEquals("New name", result.getName());
        assertEquals("New description", result.getDescription());
        System.out.println("=== update: name and description updated ===");
    }
}
