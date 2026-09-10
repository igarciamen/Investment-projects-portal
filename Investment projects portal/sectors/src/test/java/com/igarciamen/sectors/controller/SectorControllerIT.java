package com.igarciamen.sectors.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igarciamen.sectors.repository.SectorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// NOTE: objectMapper is instantiated locally instead of @Autowired, same
// reasoning as in projects/ProjectControllerIT -- Spring Boot 4.1+ no longer
// auto-configures a com.fasterxml.jackson.databind.ObjectMapper bean.
@SpringBootTest
@AutoConfigureMockMvc
class SectorControllerIT {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SectorRepository sectorRepo;

    @AfterEach
    void cleanUp() {
        sectorRepo.deleteAll();
    }

    @Test
    void listAll_isPublicAndReturnsOnlyActiveSectors() throws Exception {
        createSector("Renewable Energy", "Solar, wind, hydro");

        mockMvc.perform(get("/api/sectors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Renewable Energy"));
    }

    @Test
    void getOne_isPublic() throws Exception {
        Long id = createSector("Agriculture", "Agro-food projects");

        mockMvc.perform(get("/api/sectors/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Agriculture"));
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", "Transport", "description", "Mobility projects"));

        mockMvc.perform(post("/api/sectors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_withPromoterRole_returns403() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", "Transport", "description", "Mobility projects"));

        mockMvc.perform(post("/api/sectors")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROMOTER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withAdminRole_returns201() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", "Transport", "description", "Mobility projects"));

        mockMvc.perform(post("/api/sectors")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Transport"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void create_duplicateName_returns409() throws Exception {
        createSector("Transport", "Mobility projects");
        String body = objectMapper.writeValueAsString(Map.of("name", "Transport", "description", "Another one"));

        mockMvc.perform(post("/api/sectors")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void deactivate_hidesItFromThePublicListingButKeepsItGettable() throws Exception {
        Long id = createSector("Transport", "Mobility projects");

        mockMvc.perform(delete("/api/sectors/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/sectors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/sectors/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Transport"));
    }

    @Test
    void listAllForAdmin_onlyAccessibleToAdmin() throws Exception {
        createSector("Transport", "Mobility projects");

        mockMvc.perform(get("/api/sectors/all"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/sectors/all")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    private Long createSector(String name, String description) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", name, "description", description));

        String response = mockMvc.perform(post("/api/sectors")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }
}
