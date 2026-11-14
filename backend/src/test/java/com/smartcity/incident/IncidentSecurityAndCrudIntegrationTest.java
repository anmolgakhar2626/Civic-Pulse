package com.smartcity.incident;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcity.common.GlobalExceptionHandler;
import com.smartcity.common.ResourceNotFoundException;
import com.smartcity.incident.dto.CreateIncidentRequest;
import com.smartcity.incident.dto.IncidentResponse;
import com.smartcity.incident.dto.UpdateIncidentRequest;
import com.smartcity.incident.dto.UpdateIncidentStatusRequest;
import com.smartcity.priority.PriorityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
@DisplayName("Incident Security, Validation & CRUD Integration Tests")
class IncidentSecurityAndCrudIntegrationTest {

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;

        @Mock
        private IncidentService incidentService;

        @Mock
        private PriorityService priorityService;

        @BeforeEach
        void setUp() {
                objectMapper = new ObjectMapper();
                objectMapper.findAndRegisterModules();

                IncidentController controller = new IncidentController(incidentService, priorityService);
                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setCustomArgumentResolvers(
                                                new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();
        }

        @Test
        @DisplayName("POST /api/incidents with valid payload should create incident and return 201 Created")
        void createIncident_validPayload_returns201() throws Exception {
                CreateIncidentRequest request = new CreateIncidentRequest();
                request.setTitle("Severe Flash Flood");
                request.setDescription("Submerged street under 2ft water");
                request.setCategory("Flood");
                request.setSeverity("CRITICAL");
                request.setLocationName("Outer Ring Road Junction");
                request.setLatitude(12.9352);
                request.setLongitude(77.6245);

                UUID generatedId = UUID.randomUUID();
                IncidentResponse response = new IncidentResponse();
                response.setId(generatedId);
                response.setIncidentNumber("INC-20261028-A1B2C3D4");
                response.setTitle("Severe Flash Flood");
                response.setCategory("Flood");
                response.setDescription("Submerged street under 2ft water");
                response.setLatitude(12.9352);
                response.setLongitude(77.6245);
                response.setLocationName("Outer Ring Road Junction");
                response.setStatus(IncidentStatus.OPEN);
                response.setCreatedAt(Instant.now());

                when(incidentService.createIncident(any(CreateIncidentRequest.class))).thenReturn(response);

                mockMvc.perform(post("/api/incidents")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id", is(generatedId.toString())))
                                .andExpect(jsonPath("$.incidentNumber", is("INC-20261028-A1B2C3D4")))
                                .andExpect(jsonPath("$.title", is("Severe Flash Flood")))
                                .andExpect(jsonPath("$.category", is("Flood")))
                                .andExpect(jsonPath("$.status", is("OPEN")));
        }

        @Test
        @DisplayName("POST /api/incidents with latitude > 90 should be rejected with 400 Bad Request")
        void createIncident_invalidLatitudeHigh_returns400() throws Exception {
                CreateIncidentRequest request = new CreateIncidentRequest();
                request.setTitle("Invalid Geo Incident");
                request.setDescription("Testing latitude bounds");
                request.setLatitude(91.5); // Invalid: > 90
                request.setLongitude(77.62);

                mockMvc.perform(post("/api/incidents")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status", is(400)))
                                .andExpect(jsonPath("$.errors.latitude",
                                                containsString("Latitude must be between -90 and 90")));
        }

        @Test
        @DisplayName("POST /api/incidents with latitude < -90 should be rejected with 400 Bad Request")
        void createIncident_invalidLatitudeLow_returns400() throws Exception {
                CreateIncidentRequest request = new CreateIncidentRequest();
                request.setTitle("Invalid Geo Incident");
                request.setDescription("Testing latitude bounds");
                request.setLatitude(-95.0); // Invalid: < -90
                request.setLongitude(77.62);

                mockMvc.perform(post("/api/incidents")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status", is(400)))
                                .andExpect(jsonPath("$.errors.latitude",
                                                containsString("Latitude must be between -90 and 90")));
        }

        @Test
        @DisplayName("POST /api/incidents with longitude > 180 should be rejected with 400 Bad Request")
        void createIncident_invalidLongitudeHigh_returns400() throws Exception {
                CreateIncidentRequest request = new CreateIncidentRequest();
                request.setTitle("Invalid Geo Incident");
                request.setDescription("Testing longitude bounds");
                request.setLatitude(12.92);
                request.setLongitude(185.0); // Invalid: > 180

                mockMvc.perform(post("/api/incidents")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status", is(400)))
                                .andExpect(jsonPath("$.errors.longitude",
                                                containsString("Longitude must be between -180 and 180")));
        }

        @Test
        @DisplayName("POST /api/incidents with missing description should be rejected with 400 Bad Request")
        void createIncident_missingDescription_returns400() throws Exception {
                CreateIncidentRequest request = new CreateIncidentRequest();
                request.setTitle("Gas Leak Hazard");
                request.setLatitude(12.92);
                request.setLongitude(77.68);
                // Missing description

                mockMvc.perform(post("/api/incidents")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status", is(400)))
                                .andExpect(jsonPath("$.errors.description", containsString("Description is required")));
        }

        @Test
        @DisplayName("GET /api/incidents with search and filters should return 200 and filtered page")
        void getAllIncidents_withFilters_returns200() throws Exception {
                IncidentResponse item = new IncidentResponse();
                item.setId(UUID.randomUUID());
                item.setIncidentNumber("INC-20261028-1001");
                item.setTitle("Wildfire near Park");
                item.setCategory("Fire");
                item.setStatus(IncidentStatus.OPEN);

                when(incidentService.getFilteredIncidents(eq("Wildfire"), eq("Fire"), eq("CRITICAL"), eq("OPEN"),
                                any()))
                                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

                mockMvc.perform(get("/api/incidents")
                                .param("search", "Wildfire")
                                .param("category", "Fire")
                                .param("severity", "CRITICAL")
                                .param("status", "OPEN"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(1)))
                                .andExpect(jsonPath("$.content[0].title", is("Wildfire near Park")))
                                .andExpect(jsonPath("$.totalElements", is(1)));
        }

        @Test
        @DisplayName("GET /api/incidents/{id} for existing incident should return 200 OK")
        void getIncidentById_existing_returns200() throws Exception {
                UUID id = UUID.randomUUID();
                IncidentResponse response = new IncidentResponse();
                response.setId(id);
                response.setTitle("Live Wire Hazard");
                response.setCategory("Infrastructure");
                response.setStatus(IncidentStatus.OPEN);

                when(incidentService.getIncidentById(id)).thenReturn(response);

                mockMvc.perform(get("/api/incidents/{id}", id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id", is(id.toString())))
                                .andExpect(jsonPath("$.title", is("Live Wire Hazard")));
        }

        @Test
        @DisplayName("GET /api/incidents/{id} for nonexistent incident should return 404 Not Found")
        void getIncidentById_nonexistent_returns404() throws Exception {
                UUID id = UUID.randomUUID();
                when(incidentService.getIncidentById(id))
                                .thenThrow(new ResourceNotFoundException("Incident", "id", id));

                mockMvc.perform(get("/api/incidents/{id}", id))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status", is(404)))
                                .andExpect(jsonPath("$.errorCode", is("INCIDENT_NOT_FOUND")))
                                .andExpect(jsonPath("$.message", containsString("Incident not found")));
        }

        @Test
        @DisplayName("PUT /api/incidents/{id} should update and return 200 OK")
        void updateIncident_valid_returns200() throws Exception {
                UUID id = UUID.randomUUID();
                UpdateIncidentRequest updateReq = new UpdateIncidentRequest();
                updateReq.setTitle("Updated Fire Report");
                updateReq.setSeverity("HIGH");

                IncidentResponse response = new IncidentResponse();
                response.setId(id);
                response.setTitle("Updated Fire Report");

                when(incidentService.updateIncident(eq(id), any(UpdateIncidentRequest.class))).thenReturn(response);

                mockMvc.perform(put("/api/incidents/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateReq)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title", is("Updated Fire Report")));
        }

        @Test
        @DisplayName("PUT /api/incidents/{id} for nonexistent incident should return 404 Not Found")
        void updateIncident_nonexistent_returns404() throws Exception {
                UUID id = UUID.randomUUID();
                UpdateIncidentRequest updateReq = new UpdateIncidentRequest();
                updateReq.setTitle("Updated Title");

                when(incidentService.updateIncident(eq(id), any(UpdateIncidentRequest.class)))
                                .thenThrow(new ResourceNotFoundException("Incident", "id", id));

                mockMvc.perform(put("/api/incidents/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateReq)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status", is(404)));
        }

        @Test
        @DisplayName("PATCH /api/incidents/{id}/status should update status and return 200 OK")
        void updateStatus_valid_returns200() throws Exception {
                UUID id = UUID.randomUUID();
                UpdateIncidentStatusRequest statusReq = new UpdateIncidentStatusRequest();
                statusReq.setStatus(IncidentStatus.RESOLVED);
                statusReq.setRemarks("Issue resolved by emergency crew");

                IncidentResponse response = new IncidentResponse();
                response.setId(id);
                response.setStatus(IncidentStatus.RESOLVED);

                when(incidentService.updateIncidentStatus(eq(id), any(UpdateIncidentStatusRequest.class)))
                                .thenReturn(response);

                mockMvc.perform(patch("/api/incidents/{id}/status", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(statusReq)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status", is("RESOLVED")));
        }

        @Test
        @DisplayName("PATCH /api/incidents/{id}/status for nonexistent incident should return 404 Not Found")
        void updateStatus_nonexistent_returns404() throws Exception {
                UUID id = UUID.randomUUID();
                UpdateIncidentStatusRequest statusReq = new UpdateIncidentStatusRequest();
                statusReq.setStatus(IncidentStatus.CLOSED);

                when(incidentService.updateIncidentStatus(eq(id), any(UpdateIncidentStatusRequest.class)))
                                .thenThrow(new ResourceNotFoundException("Incident", "id", id));

                mockMvc.perform(patch("/api/incidents/{id}/status", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(statusReq)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status", is(404)));
        }

        @Test
        @DisplayName("DELETE /api/incidents/{id} should delete incident and return 204 No Content")
        void deleteIncident_existing_returns204() throws Exception {
                UUID id = UUID.randomUUID();
                doNothing().when(incidentService).deleteIncident(id);

                mockMvc.perform(delete("/api/incidents/{id}", id))
                                .andExpect(status().isNoContent());

                verify(incidentService).deleteIncident(id);
        }

        @Test
        @DisplayName("DELETE /api/incidents/{id} for nonexistent incident should return 404 Not Found")
        void deleteIncident_nonexistent_returns404() throws Exception {
                UUID id = UUID.randomUUID();
                doThrow(new ResourceNotFoundException("Incident", "id", id)).when(incidentService).deleteIncident(id);

                mockMvc.perform(delete("/api/incidents/{id}", id))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status", is(404)));
        }
}
