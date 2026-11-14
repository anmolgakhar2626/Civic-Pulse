package com.smartcity.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcity.infrastructure.dto.CreateInfrastructureRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Infrastructure & Map Security Integration Tests")
class InfrastructureSecurityAndMapIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Unauthenticated request to /api/map/incidents returns 401 Unauthorized")
    void testUnauthenticatedMapAccessDenied() throws Exception {
        mockMvc.perform(get("/api/map/incidents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "citizen_user", roles = {"CITIZEN"})
    @DisplayName("CITIZEN role can view /api/map/incidents and /api/infrastructure")
    void testCitizenCanViewMapAndInfrastructure() throws Exception {
        mockMvc.perform(get("/api/map/incidents"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/map/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncidents", greaterThanOrEqualTo(0)));

        mockMvc.perform(get("/api/infrastructure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @WithMockUser(username = "citizen_user", roles = {"CITIZEN"})
    @DisplayName("CITIZEN role cannot create infrastructure - returns 403 Forbidden")
    void testCitizenCannotCreateInfrastructure() throws Exception {
        CreateInfrastructureRequest req = new CreateInfrastructureRequest();
        req.setName("Unauthorized Asset");
        req.setCategory("ROAD");
        req.setStatus("OPERATIONAL");
        req.setLatitude(30.73);
        req.setLongitude(76.77);

        mockMvc.perform(post("/api/infrastructure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "citizen_user", roles = {"CITIZEN"})
    @DisplayName("CITIZEN role cannot delete infrastructure - returns 403 Forbidden")
    void testCitizenCannotDeleteInfrastructure() throws Exception {
        mockMvc.perform(delete("/api/infrastructure/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("ADMIN role can create and delete infrastructure")
    void testAdminCanCreateAndDeleteInfrastructure() throws Exception {
        CreateInfrastructureRequest req = new CreateInfrastructureRequest();
        req.setName("Admin Registered Hospital Wing");
        req.setCategory("HOSPITAL");
        req.setStatus("OPERATIONAL");
        req.setLatitude(30.7410);
        req.setLongitude(76.7850);
        req.setAddress("Sector 17 Utility Zone");

        String responseContent = mockMvc.perform(post("/api/infrastructure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Admin Registered Hospital Wing")))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        String idStr = objectMapper.readTree(responseContent).get("id").asText();
        UUID id = UUID.fromString(idStr);

        mockMvc.perform(delete("/api/infrastructure/{id}", id))
                .andExpect(status().isNoContent());
    }
}

