package com.smartcity.incident;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcity.common.GlobalExceptionHandler;
import com.smartcity.common.ResourceNotFoundException;
import com.smartcity.incident.dto.CreateIncidentRequest;
import com.smartcity.incident.dto.IncidentResponse;
import com.smartcity.incident.dto.UpdateIncidentRequest;
import com.smartcity.incident.dto.UpdateIncidentStatusRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = IncidentController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {
                        com.smartcity.security.SecurityConfig.class,
                        com.smartcity.security.AuthTokenFilter.class,
                        com.smartcity.security.SecurityDataInitializer.class
                }
        ),
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
        }
)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IncidentService incidentService;

    @MockBean
    private com.smartcity.priority.PriorityService priorityService;

    @Test
    @DisplayName("POST /api/incidents creates incident and returns 201 Created")
    void createIncident_validRequest_returns201() throws Exception {
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setIncidentType("TRAFFIC_JAM");
        request.setDescription("Severe gridlock on outer ring road");
        request.setSource(IncidentSource.IOT);
        request.setLatitude(12.92);
        request.setLongitude(77.68);
        request.setAddress("Bellandur Junction");

        UUID generatedId = UUID.randomUUID();
        IncidentResponse response = new IncidentResponse();
        response.setId(generatedId);
        response.setIncidentNumber("INC-20261003-8899AABB");
        response.setIncidentType("TRAFFIC_JAM");
        response.setDescription("Severe gridlock on outer ring road");
        response.setSource(IncidentSource.IOT);
        response.setLatitude(12.92);
        response.setLongitude(77.68);
        response.setStatus(IncidentStatus.DETECTED);
        response.setCreatedAt(Instant.now());

        when(incidentService.createIncident(any(CreateIncidentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(generatedId.toString())))
                .andExpect(jsonPath("$.incidentNumber", is("INC-20261003-8899AABB")))
                .andExpect(jsonPath("$.status", is("DETECTED")));
    }

    @Test
    @DisplayName("POST /api/incidents with invalid body returns 400 Bad Request with standardized error structure")
    void createIncident_invalidRequest_returns400() throws Exception {
        CreateIncidentRequest request = new CreateIncidentRequest();
        // Missing required fields: incidentType, description, source, latitude, longitude

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.path", is("/api/incidents")))
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/incidents returns 200 OK with paged results")
    void getAllIncidents_returnsPagedResult() throws Exception {
        IncidentResponse response = new IncidentResponse();
        response.setId(UUID.randomUUID());
        response.setIncidentNumber("INC-20261003-001");
        response.setIncidentType("FIRE");
        response.setStatus(IncidentStatus.DETECTED);

        when(incidentService.getAllIncidents(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/incidents?page=0&size=20&sort=createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].incidentNumber", is("INC-20261003-001")));
    }

    @Test
    @DisplayName("GET /api/incidents/{id} returns 200 OK when incident exists")
    void getIncidentById_exists_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        IncidentResponse response = new IncidentResponse();
        response.setId(id);
        response.setIncidentNumber("INC-20261003-002");
        response.setStatus(IncidentStatus.ASSIGNED);

        when(incidentService.getIncidentById(id)).thenReturn(response);

        mockMvc.perform(get("/api/incidents/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.status", is("ASSIGNED")));
    }

    @Test
    @DisplayName("GET /api/incidents/{id} returns 404 Not Found with error body when not found")
    void getIncidentById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(incidentService.getIncidentById(id))
                .thenThrow(new ResourceNotFoundException("Incident", "id", id));

        mockMvc.perform(get("/api/incidents/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Incident not found with id")));
    }

    @Test
    @DisplayName("PATCH /api/incidents/{id}/status updates status and returns 200 OK")
    void updateIncidentStatus_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateIncidentStatusRequest request = new UpdateIncidentStatusRequest();
        request.setStatus(IncidentStatus.IN_PROGRESS);
        request.setChangedBy("operator_smith");
        request.setRemarks("Response unit at scene");

        IncidentResponse response = new IncidentResponse();
        response.setId(id);
        response.setStatus(IncidentStatus.IN_PROGRESS);

        when(incidentService.updateIncidentStatus(eq(id), any(UpdateIncidentStatusRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/incidents/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
    }

    @Test
    @DisplayName("DELETE /api/incidents/{id} returns 204 No Content")
    void deleteIncident_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(incidentService).deleteIncident(id);

        mockMvc.perform(delete("/api/incidents/" + id))
                .andExpect(status().isNoContent());

        verify(incidentService).deleteIncident(id);
    }
}
