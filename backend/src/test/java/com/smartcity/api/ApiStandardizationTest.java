package com.smartcity.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcity.assignment.ResponseAction;
import com.smartcity.assignment.ResponseActionRepository;
import com.smartcity.assignment.ResponseActionStatus;
import com.smartcity.assignment.ResponseActionType;
import com.smartcity.assignment.dto.UpdateResponseActionStatusRequest;
import com.smartcity.department.Department;
import com.smartcity.department.DepartmentRepository;
import com.smartcity.department.Team;
import com.smartcity.department.TeamRepository;
import com.smartcity.incident.Incident;
import com.smartcity.incident.IncidentRepository;
import com.smartcity.incident.IncidentSource;
import com.smartcity.incident.IncidentStatus;
import com.smartcity.incident.dto.CreateIncidentRequest;
import com.smartcity.priority.PriorityTier;
import com.smartcity.priority.dto.EvaluatePriorityRequest;
import com.smartcity.sla.SLAService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = {"ADMIN", "COMMAND_OPERATOR"})
@DisplayName("API Standardization, Documentation, Filtering, Sorting, and Validation Tests")
class ApiStandardizationTest {

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
    private SLAService slaService;

    @Autowired
    private ObjectMapper objectMapper;

    private Incident testIncident;
    private Department testDept;
    private Team testTeam;

    @BeforeEach
    void setUp() {
        testDept = departmentRepository.findByCode("TRAFFIC").orElseGet(() ->
                departmentRepository.save(new Department("TRAFFIC", "Traffic Department", "Traffic", "traffic@city.gov", "1095")));

        testTeam = teamRepository.findByCode("TRAFFIC_OPS").orElseGet(() ->
                teamRepository.save(new Team(testDept, "TRAFFIC_OPS", "Traffic Operations Response Team", "1095")));

        testIncident = new Incident();
        testIncident.setIncidentNumber("INC-API-" + UUID.randomUUID().toString().substring(0, 8));
        testIncident.setIncidentType("TRAFFIC_SIGNAL_FAILURE");
        testIncident.setDescription("Traffic signal outage at junction");
        testIncident.setSource(IncidentSource.IOT);
        testIncident.setLatitude(12.92);
        testIncident.setLongitude(77.68);
        testIncident.setStatus(IncidentStatus.PRIORITIZED);
        testIncident.setTier(PriorityTier.HIGH);
        testIncident.setAssignedDepartment(testDept);
        testIncident.setAssignedTeam(testTeam);
        testIncident = incidentRepository.saveAndFlush(testIncident);
    }

    @Test
    @DisplayName("GET /api/incidents returns 200 with standard pagination metadata and content")
    void testGetIncidentsPagination() throws Exception {
        mockMvc.perform(get("/api/incidents?page=0&size=5&sort=createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(5)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /api/incidents with unsafe sort field returns 400 Bad Request with standardized error response")
    void testGetIncidentsUnsafeSortRejection() throws Exception {
        mockMvc.perform(get("/api/incidents?sort=malicious_property;DROP TABLE,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errorCode", is("INVALID_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Invalid sort field")));
    }

    @Test
    @DisplayName("GET /api/incidents supports multi-attribute filtering (status, incidentType, source, tier, department)")
    void testGetIncidentsFiltering() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .param("status", "PRIORITIZED")
                        .param("incidentType", "TRAFFIC_SIGNAL_FAILURE")
                        .param("source", "IOT")
                        .param("tier", "HIGH")
                        .param("department", "TRAFFIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].incidentType", is("TRAFFIC_SIGNAL_FAILURE")))
                .andExpect(jsonPath("$.content[0].departmentCode", is("TRAFFIC")));
    }

    @Test
    @DisplayName("POST /api/incidents validation failure returns 400 with field-level errors map")
    void testCreateIncidentValidationFailure() throws Exception {
        CreateIncidentRequest invalidRequest = new CreateIncidentRequest();
        // Missing incidentType, description, source, latitude, longitude

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.errors.titleOrIncidentTypeProvided", notNullValue()))
                .andExpect(jsonPath("$.errors.description", notNullValue()))
                .andExpect(jsonPath("$.errors.latitude", notNullValue()))
                .andExpect(jsonPath("$.errors.longitude", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/incidents/{id} for non-existent incident returns 404 with standard error body")
    void testGetIncidentNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/incidents/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.errorCode", is("INCIDENT_NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("Incident not found")));
    }

    @Test
    @DisplayName("GET /api/departments returns 200 with paginated departments and active teams")
    void testGetDepartmentsPagination() throws Exception {
        mockMvc.perform(get("/api/departments?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(10)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].name", notNullValue()))
                .andExpect(jsonPath("$.content[0].teams", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/departments/{id} returns 200 with specific department details")
    void testGetDepartmentById() throws Exception {
        mockMvc.perform(get("/api/departments/{id}", testDept.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testDept.getId().toString())))
                .andExpect(jsonPath("$.code", is("TRAFFIC")))
                .andExpect(jsonPath("$.teams", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /api/departments/{id} for non-existent UUID returns 404")
    void testGetDepartmentNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/departments/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("GET /api/sla/active supports pagination parameters and returns standard pagination metadata")
    void testGetActiveSLAsPagination() throws Exception {
        slaService.createSLAForIncident(testIncident.getId().toString());

        mockMvc.perform(get("/api/sla/active?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(5)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("POST /api/priority/evaluate returns 200 with priority calculation")
    void testEvaluatePriorityEndpoint() throws Exception {
        EvaluatePriorityRequest request = new EvaluatePriorityRequest();
        request.setIncidentType("TRAFFIC_SIGNAL_FAILURE");
        request.setMajorJunction(true);

        mockMvc.perform(post("/api/priority/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priorityTier", is("HIGH")))
                .andExpect(jsonPath("$.slaMinutes", is(240)));
    }

    @Test
    @DisplayName("PATCH /api/actions/{id}/status for non-existent action returns 404")
    void testPatchActionNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        UpdateResponseActionStatusRequest request = new UpdateResponseActionStatusRequest(ResponseActionStatus.COMPLETED);

        mockMvc.perform(patch("/api/actions/{id}/status", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("GET /api-docs returns 200 and valid OpenAPI JSON specification")
    void testOpenApiDocsAvailable() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi", notNullValue()))
                .andExpect(jsonPath("$.info.title", containsString("CivicPulse")))
                .andExpect(jsonPath("$.paths['/api/incidents']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/priority/evaluate']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/departments']", notNullValue()))
                .andExpect(jsonPath("$.tags[?(@.name == 'Incidents')]", notNullValue()))
                .andExpect(jsonPath("$.tags[?(@.name == 'Priority')]", notNullValue()))
                .andExpect(jsonPath("$.tags[?(@.name == 'SLA')]", notNullValue()))
                .andExpect(jsonPath("$.tags[?(@.name == 'Departments')]", notNullValue()))
                .andExpect(jsonPath("$.tags[?(@.name == 'Assignments')]", notNullValue()))
                .andExpect(jsonPath("$.tags[?(@.name == 'Response Actions')]", notNullValue()));
    }
}
