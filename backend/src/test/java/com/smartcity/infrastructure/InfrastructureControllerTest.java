package com.smartcity.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcity.common.GlobalExceptionHandler;
import com.smartcity.common.PagedResponse;
import com.smartcity.common.ResourceNotFoundException;
import com.smartcity.infrastructure.dto.CreateInfrastructureRequest;
import com.smartcity.infrastructure.dto.InfrastructureResponse;
import com.smartcity.infrastructure.dto.UpdateInfrastructureRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InfrastructureController CRUD & Validation Tests")
class InfrastructureControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private InfrastructureService infrastructureService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        InfrastructureController controller = new InfrastructureController(infrastructureService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/infrastructure returns 200 with paginated list")
    void testGetInfrastructureList() throws Exception {
        InfrastructureResponse item = new InfrastructureResponse();
        item.setId(UUID.randomUUID());
        item.setCode("INF-HOSP-01");
        item.setName("Sector 22 Civil Hospital");
        item.setCategory("HOSPITAL");
        item.setStatus("OPERATIONAL");
        item.setLatitude(30.732);
        item.setLongitude(76.771);

        PagedResponse<InfrastructureResponse> pagedResponse =
                new PagedResponse<>(List.of(item), 0, 20, 1L, 1, true, true);

        when(infrastructureService.getInfrastructureList(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/infrastructure")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].code", is("INF-HOSP-01")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    @DisplayName("GET /api/infrastructure/{id} returns 200 when found and 404 when missing")
    void testGetInfrastructureById() throws Exception {
        UUID id = UUID.randomUUID();
        InfrastructureResponse item = new InfrastructureResponse();
        item.setId(id);
        item.setCode("INF-SUB-01");
        item.setName("Power Substation");

        when(infrastructureService.getInfrastructureById(id)).thenReturn(item);

        mockMvc.perform(get("/api/infrastructure/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("INF-SUB-01")));

        UUID missingId = UUID.randomUUID();
        when(infrastructureService.getInfrastructureById(missingId))
                .thenThrow(new ResourceNotFoundException("Infrastructure", "id", missingId));

        mockMvc.perform(get("/api/infrastructure/{id}", missingId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/infrastructure returns 201 on valid payload and 400 on invalid payload")
    void testCreateInfrastructure() throws Exception {
        CreateInfrastructureRequest request = new CreateInfrastructureRequest();
        request.setName("New Bridge");
        request.setCategory("BRIDGE");
        request.setStatus("OPERATIONAL");
        request.setLatitude(30.74);
        request.setLongitude(76.78);

        InfrastructureResponse created = new InfrastructureResponse();
        created.setId(UUID.randomUUID());
        created.setCode("INF-BRID-01");
        created.setName("New Bridge");
        created.setCategory("BRIDGE");
        created.setStatus("OPERATIONAL");
        created.setLatitude(30.74);
        created.setLongitude(76.78);

        when(infrastructureService.createInfrastructure(any(CreateInfrastructureRequest.class)))
                .thenReturn(created);

        mockMvc.perform(post("/api/infrastructure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("New Bridge")))
                .andExpect(jsonPath("$.code", is("INF-BRID-01")));

        // Invalid payload (missing name and coordinates)
        CreateInfrastructureRequest invalid = new CreateInfrastructureRequest();
        mockMvc.perform(post("/api/infrastructure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/infrastructure/{id} updates existing asset")
    void testUpdateInfrastructure() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateInfrastructureRequest updateReq = new UpdateInfrastructureRequest();
        updateReq.setStatus("DEGRADED");
        updateReq.setMaintenancePriority("HIGH");

        InfrastructureResponse updated = new InfrastructureResponse();
        updated.setId(id);
        updated.setStatus("DEGRADED");
        updated.setMaintenancePriority("HIGH");

        when(infrastructureService.updateInfrastructure(eq(id), any(UpdateInfrastructureRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/infrastructure/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DEGRADED")))
                .andExpect(jsonPath("$.maintenancePriority", is("HIGH")));
    }

    @Test
    @DisplayName("DELETE /api/infrastructure/{id} returns 204 on success")
    void testDeleteInfrastructure() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(infrastructureService).deleteInfrastructure(id);

        mockMvc.perform(delete("/api/infrastructure/{id}", id))
                .andExpect(status().isNoContent());

        verify(infrastructureService, times(1)).deleteInfrastructure(id);
    }
}

