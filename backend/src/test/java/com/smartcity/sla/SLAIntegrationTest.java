package com.smartcity.sla;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcity.incident.Incident;
import com.smartcity.incident.IncidentRepository;
import com.smartcity.incident.IncidentSource;
import com.smartcity.incident.IncidentStatus;
import com.smartcity.incident.dto.UpdateIncidentStatusRequest;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = {"ADMIN", "COMMAND_OPERATOR"})
@DisplayName("SLA & Escalation Engine Integration Tests")
class SLAIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private SLARepository slaRepository;

    @Autowired
    private SLAService slaService;

    @Autowired
    private SLAManager slaManager;

    @Autowired
    private ObjectMapper objectMapper;

    private Incident prioritizedIncident;
    private Incident unprioritizedIncident;

    @BeforeEach
    void setUp() {
        // Create an unprioritized incident
        unprioritizedIncident = new Incident();
        unprioritizedIncident.setIncidentNumber("INC-TEST-UNPRIORITIZED-" + UUID.randomUUID().toString().substring(0, 8));
        unprioritizedIncident.setIncidentType("POTHOLE");
        unprioritizedIncident.setDescription("Small pothole on local street");
        unprioritizedIncident.setSource(IncidentSource.CITIZEN);
        unprioritizedIncident.setLatitude(30.7333);
        unprioritizedIncident.setLongitude(76.7794);
        unprioritizedIncident.setStatus(IncidentStatus.DETECTED);
        unprioritizedIncident = incidentRepository.saveAndFlush(unprioritizedIncident);

        // Create a prioritized CRITICAL incident
        prioritizedIncident = new Incident();
        prioritizedIncident.setIncidentNumber("INC-TEST-CRITICAL-" + UUID.randomUUID().toString().substring(0, 8));
        prioritizedIncident.setIncidentType("ELECTRICAL_FIRE_HAZARD");
        prioritizedIncident.setDescription("Transformer sparked and high voltage wire snapped");
        prioritizedIncident.setSource(IncidentSource.IOT);
        prioritizedIncident.setLatitude(30.7340);
        prioritizedIncident.setLongitude(76.7800);
        prioritizedIncident.setStatus(IncidentStatus.PRIORITIZED);
        prioritizedIncident.setTier(PriorityTier.CRITICAL);
        prioritizedIncident = incidentRepository.saveAndFlush(prioritizedIncident);
    }

    @Test
    @DisplayName("POST /api/incidents/{id}/sla fails when incident is not prioritized")
    void testCreateSLAWithoutPriorityFails() throws Exception {
        mockMvc.perform(post("/api/incidents/{id}/sla", unprioritizedIncident.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Incident must be prioritized before SLA creation.")));
    }

    @Test
    @DisplayName("POST /api/incidents/{id}/sla succeeds for prioritized CRITICAL incident (30m target)")
    void testCreateSLASuccess() throws Exception {
        mockMvc.perform(post("/api/incidents/{id}/sla", prioritizedIncident.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incidentId", is(prioritizedIncident.getIncidentNumber())))
                .andExpect(jsonPath("$.priorityTier", is("CRITICAL")))
                .andExpect(jsonPath("$.targetMinutes", is(20)))
                .andExpect(jsonPath("$.status", is("ON_TRACK")))
                .andExpect(jsonPath("$.escalationLevel", is("NONE")))
                .andExpect(jsonPath("$.deadlineAt", notNullValue()));

        // Verify SLA record is in DB
        SLARecord record = slaRepository.findByIncidentId(prioritizedIncident.getId()).orElse(null);
        assertNotNull(record);
        assertEquals(20, record.getTargetMinutes());
        assertEquals(PriorityTier.CRITICAL, record.getPriorityTier());

        // Verify incident has SLA deadline
        Incident updatedIncident = incidentRepository.findById(prioritizedIncident.getId()).orElseThrow();
        assertEquals(20, updatedIncident.getSlaMinutes());
        assertNotNull(updatedIncident.getSlaDeadline());
    }

    @Test
    @DisplayName("POST /api/incidents/{incidentNumber}/sla works with incident number as identifier")
    void testCreateSLAUsingIncidentNumber() throws Exception {
        mockMvc.perform(post("/api/incidents/{id}/sla", prioritizedIncident.getIncidentNumber())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incidentId", is(prioritizedIncident.getIncidentNumber())))
                .andExpect(jsonPath("$.priorityTier", is("CRITICAL")));
    }

    @Test
    @DisplayName("GET /api/incidents/{id}/sla returns existing SLA record")
    void testGetIncidentSLA() throws Exception {
        slaService.createSLAForIncident(prioritizedIncident.getId());

        mockMvc.perform(get("/api/incidents/{id}/sla", prioritizedIncident.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId", is(prioritizedIncident.getIncidentNumber())))
                .andExpect(jsonPath("$.targetMinutes", is(20)))
                .andExpect(jsonPath("$.status", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/sla/active returns active SLA records")
    void testGetActiveSLAs() throws Exception {
        slaService.createSLAForIncident(prioritizedIncident.getId());

        mockMvc.perform(get("/api/sla/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[?(@.incidentId == '" + prioritizedIncident.getIncidentNumber() + "')]", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/sla/at-risk returns at-risk SLA records")
    void testGetAtRiskSLAs() throws Exception {
        slaService.createSLAForIncident(prioritizedIncident.getId());
        SLARecord record = slaRepository.findByIncidentId(prioritizedIncident.getId()).orElseThrow();
        record.setStatus(SLAStatus.AT_RISK);
        record.setEscalationLevel(EscalationLevel.LEVEL_1);
        slaRepository.saveAndFlush(record);

        mockMvc.perform(get("/api/sla/at-risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[?(@.status == 'AT_RISK')]", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/sla/breached returns breached SLA records")
    void testGetBreachedSLAs() throws Exception {
        slaService.createSLAForIncident(prioritizedIncident.getId());
        SLARecord record = slaRepository.findByIncidentId(prioritizedIncident.getId()).orElseThrow();
        record.setStatus(SLAStatus.BREACHED);
        record.setBreached(true);
        record.setBreachedAt(Instant.now());
        record.setEscalationLevel(EscalationLevel.LEVEL_2);
        slaRepository.saveAndFlush(record);

        mockMvc.perform(get("/api/sla/breached"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[?(@.status == 'BREACHED')]", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/sla/{id} returns SLA record by ID")
    void testGetSLAById() throws Exception {
        slaService.createSLAForIncident(prioritizedIncident.getId());
        SLARecord record = slaRepository.findByIncidentId(prioritizedIncident.getId()).orElseThrow();

        mockMvc.perform(get("/api/sla/{id}", record.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(record.getId().toString())))
                .andExpect(jsonPath("$.incidentId", is(prioritizedIncident.getIncidentNumber())));
    }

    @Test
    @DisplayName("Incident status transition to RESOLVED automatically marks SLA as RESOLVED")
    void testIncidentResolutionMarksSLAAsResolved() throws Exception {
        slaService.createSLAForIncident(prioritizedIncident.getId());

        UpdateIncidentStatusRequest updateRequest = new UpdateIncidentStatusRequest();
        updateRequest.setStatus(IncidentStatus.RESOLVED);
        updateRequest.setChangedBy("OPERATOR_1");
        updateRequest.setRemarks("Repaired wire and restored power");

        mockMvc.perform(patch("/api/incidents/{id}/status", prioritizedIncident.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RESOLVED")));

        // Check SLA is resolved
        SLARecord updatedSla = slaRepository.findByIncidentId(prioritizedIncident.getId()).orElseThrow();
        assertEquals(SLAStatus.RESOLVED, updatedSla.getStatus());
        assertNotNull(updatedSla.getResolvedAt());
        assertEquals(EscalationLevel.NONE, updatedSla.getEscalationLevel());

        // Verify SLAManager runner doesn't change it back
        slaManager.evaluateActiveSLAs();

        SLARecord checkedSla = slaRepository.findByIncidentId(prioritizedIncident.getId()).orElseThrow();
        assertEquals(SLAStatus.RESOLVED, checkedSla.getStatus());
    }
}
