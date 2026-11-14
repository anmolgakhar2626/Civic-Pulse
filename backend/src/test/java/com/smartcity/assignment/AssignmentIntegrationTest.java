package com.smartcity.assignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcity.assignment.dto.AssignIncidentRequest;
import com.smartcity.assignment.dto.CreateResponseActionRequest;
import com.smartcity.assignment.dto.UpdateResponseActionStatusRequest;
import com.smartcity.department.Department;
import com.smartcity.department.DepartmentRepository;
import com.smartcity.department.Team;
import com.smartcity.department.TeamRepository;
import com.smartcity.incident.Incident;
import com.smartcity.incident.IncidentRepository;
import com.smartcity.incident.IncidentSource;
import com.smartcity.incident.IncidentStatus;
import com.smartcity.priority.PriorityTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = {"ADMIN", "COMMAND_OPERATOR"})
@DisplayName("Department Assignment & Response Action Integration Tests")
class AssignmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private ResponseActionRepository responseActionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Incident trafficIncident;
    private Incident accidentIncident;
    private Department trafficDept;
    private Team trafficTeam;

    @BeforeEach
    void setUp() {
        trafficDept = departmentRepository.findByCode("TRAFFIC").orElseGet(() ->
                departmentRepository.save(new Department("TRAFFIC", "Traffic Department", "Traffic", "trf@city.gov", "1095")));

        trafficTeam = teamRepository.findByCode("TRAFFIC_OPS").orElseGet(() ->
                teamRepository.save(new Team(trafficDept, "TRAFFIC_OPS", "Traffic Operations Response Team", "1095")));

        trafficIncident = new Incident();
        trafficIncident.setIncidentNumber("INC-TEST-TRF-" + UUID.randomUUID().toString().substring(0, 8));
        trafficIncident.setIncidentType("TRAFFIC_SIGNAL_FAILURE");
        trafficIncident.setDescription("Traffic light completely dead at major intersection");
        trafficIncident.setSource(IncidentSource.IOT);
        trafficIncident.setLatitude(30.7380);
        trafficIncident.setLongitude(76.7840);
        trafficIncident.setStatus(IncidentStatus.PRIORITIZED);
        trafficIncident.setTier(PriorityTier.HIGH);
        trafficIncident = incidentRepository.save(trafficIncident);

        accidentIncident = new Incident();
        accidentIncident.setIncidentNumber("INC-TEST-ACC-" + UUID.randomUUID().toString().substring(0, 8));
        accidentIncident.setIncidentType("MAJOR_ROAD_ACCIDENT");
        accidentIncident.setDescription("Head-on collision with injuries");
        accidentIncident.setSource(IncidentSource.CITIZEN);
        accidentIncident.setLatitude(30.7320);
        accidentIncident.setLongitude(76.7810);
        accidentIncident.setStatus(IncidentStatus.PRIORITIZED);
        accidentIncident.setTier(PriorityTier.CRITICAL);
        accidentIncident = incidentRepository.save(accidentIncident);
    }

    @Test
    @DisplayName("POST /api/incidents/{id}/auto-assign successfully assigns Traffic Department and team")
    void testAutoAssignEndpoint() throws Exception {
        mockMvc.perform(post("/api/incidents/{id}/auto-assign", trafficIncident.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId", is(trafficIncident.getIncidentNumber())))
                .andExpect(jsonPath("$.department", is("Traffic Department")))
                .andExpect(jsonPath("$.team", is("Traffic Operations Response Team")))
                .andExpect(jsonPath("$.reason", notNullValue()));

        Incident updated = incidentRepository.findById(trafficIncident.getId()).orElseThrow();
        assertEquals(IncidentStatus.ASSIGNED, updated.getStatus());
        assertEquals("Traffic Department", updated.getAssignedDepartment().getName());
        assertEquals("Traffic Operations Response Team", updated.getAssignedTeam().getName());
        assertNotNull(updated.getAssignedAt());
        assertEquals("AUTO_ASSIGNER", updated.getAssignedBy());
    }

    @Test
    @DisplayName("POST /api/incidents/{id}/assign with manual department and team payload")
    void testManualAssignEndpoint() throws Exception {
        AssignIncidentRequest request = new AssignIncidentRequest(
                trafficDept.getId(), trafficTeam.getId(), "OPERATOR_JANE", "Manual dispatch to intersection"
        );

        mockMvc.perform(post("/api/incidents/{id}/assign", trafficIncident.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department", is("Traffic Department")))
                .andExpect(jsonPath("$.team", is("Traffic Operations Response Team")))
                .andExpect(jsonPath("$.reason", is("Manual dispatch to intersection")));

        Incident updated = incidentRepository.findById(trafficIncident.getId()).orElseThrow();
        assertEquals("OPERATOR_JANE", updated.getAssignedBy());
        assertEquals("Manual dispatch to intersection", updated.getAssignmentReason());
    }

    @Test
    @DisplayName("GET /api/incidents/{id}/assignment-history retrieves audit trail")
    void testGetAssignmentHistoryEndpoint() throws Exception {
        // Run auto-assign first to generate history
        mockMvc.perform(post("/api/incidents/{id}/auto-assign", trafficIncident.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/incidents/{id}/assignment-history", trafficIncident.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].assignedBy", is("AUTO_ASSIGNER")))
                .andExpect(jsonPath("$[0].newDepartmentName", is("Traffic Department")));
    }

    @Test
    @DisplayName("POST /api/incidents/{id}/generate-response-plan generates recommended actions")
    void testGenerateResponsePlanEndpoint() throws Exception {
        mockMvc.perform(post("/api/incidents/{id}/generate-response-plan", accidentIncident.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$[?(@.actionType == 'EMERGENCY_RESPONSE')]", notNullValue()))
                .andExpect(jsonPath("$[?(@.actionType == 'ASSIGN_FIELD_TEAM')]", notNullValue()));
    }

    @Test
    @DisplayName("GET and POST /api/incidents/{id}/actions and PATCH /api/actions/{id}/status")
    void testResponseActionsCrudEndpoints() throws Exception {
        CreateResponseActionRequest createRequest = new CreateResponseActionRequest(
                ResponseActionType.MARK_GIS_CAUTION_ZONE,
                "Erect safety cones around accident locus",
                ResponseActionStatus.RECOMMENDED,
                "DISPATCH_LEAD"
        );

        String createJson = mockMvc.perform(post("/api/incidents/{id}/actions", accidentIncident.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.actionType", is("MARK_GIS_CAUTION_ZONE")))
                .andExpect(jsonPath("$.status", is("RECOMMENDED")))
                .andReturn().getResponse().getContentAsString();

        UUID actionId = UUID.fromString(objectMapper.readTree(createJson).get("id").asText());

        // Update status to IN_PROGRESS then COMPLETED
        UpdateResponseActionStatusRequest updateRequest = new UpdateResponseActionStatusRequest(ResponseActionStatus.COMPLETED);

        mockMvc.perform(patch("/api/actions/{id}/status", actionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.completedAt", notNullValue()));

        // Query actions for incident
        mockMvc.perform(get("/api/incidents/{id}/actions", accidentIncident.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[?(@.id == '" + actionId + "')].status", hasItem("COMPLETED")));
    }
}
