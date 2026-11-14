package com.smartcity.sla;

import com.smartcity.incident.Incident;
import com.smartcity.incident.IncidentRepository;
import com.smartcity.incident.IncidentStatus;
import com.smartcity.priority.PriorityTier;
import com.smartcity.sla.dto.CreateSlaPolicyRequest;
import com.smartcity.sla.dto.SLAResponse;
import com.smartcity.sla.dto.SlaEscalationResponse;
import com.smartcity.sla.dto.SlaPolicyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CivicPulse Prompt 4: SLA Tracking & Automatic Escalation Integration Tests")
class SlaTrackingAndEscalationTest {

    private JdbcTemplate jdbcTemplate;
    private SlaJdbcRepository slaJdbcRepository;
    private SLACalculator slaCalculator;
    private SLAServiceImpl slaService;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private SLARepository slaRepository;

    private final Instant baseTime = Instant.parse("2026-10-28T12:00:00Z");

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:sla_tracking_test_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        jdbcTemplate = new JdbcTemplate(dataSource);
        slaJdbcRepository = new SlaJdbcRepository(jdbcTemplate);
        slaCalculator = new SLACalculator();

        // Create in-memory tables
        jdbcTemplate.execute("""
            CREATE TABLE incidents (
                id UUID PRIMARY KEY,
                incident_number VARCHAR(50) NOT NULL UNIQUE,
                title VARCHAR(255) NOT NULL,
                category VARCHAR(100) NOT NULL,
                severity VARCHAR(50) NOT NULL,
                current_status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
                sla_minutes INT,
                sla_deadline TIMESTAMP,
                escalation_state VARCHAR(50),
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE sla_policies (
                id UUID PRIMARY KEY,
                category VARCHAR(100) NOT NULL,
                severity VARCHAR(50) NOT NULL,
                response_time_minutes INT NOT NULL,
                resolution_time_minutes INT NOT NULL,
                enabled BOOLEAN NOT NULL DEFAULT TRUE,
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL,
                CONSTRAINT uq_pol UNIQUE (category, severity)
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE sla_records (
                id UUID PRIMARY KEY,
                incident_id UUID NOT NULL UNIQUE,
                policy_id UUID,
                priority_tier VARCHAR(20),
                target_minutes INT,
                target_sla_minutes INT,
                started_at TIMESTAMP NOT NULL,
                deadline_at TIMESTAMP,
                response_deadline TIMESTAMP,
                resolution_deadline TIMESTAMP,
                first_responded_at TIMESTAMP,
                acknowledged_at TIMESTAMP,
                resolved_at TIMESTAMP,
                status VARCHAR(50) NOT NULL DEFAULT 'ON_TRACK',
                current_sla_status VARCHAR(50) NOT NULL DEFAULT 'ON_TRACK',
                remaining_minutes BIGINT,
                remaining_seconds BIGINT,
                escalation_level VARCHAR(50) NOT NULL DEFAULT 'NONE',
                last_escalated_at TIMESTAMP,
                breached BOOLEAN NOT NULL DEFAULT FALSE,
                breached_at TIMESTAMP,
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE sla_escalation_history (
                id UUID PRIMARY KEY,
                incident_id UUID NOT NULL,
                sla_record_id UUID,
                previous_escalation_level VARCHAR(50),
                new_escalation_level VARCHAR(50) NOT NULL,
                previous_status VARCHAR(50),
                new_status VARCHAR(50) NOT NULL,
                reason TEXT,
                escalated_at TIMESTAMP NOT NULL
            )
        """);

        slaService = new SLAServiceImpl(slaRepository, incidentRepository, slaCalculator, slaJdbcRepository);
        slaService.setClock(Clock.fixed(baseTime, ZoneOffset.UTC));
    }

    private Incident createIncident(String category, String severity, PriorityTier tier) {
        Incident incident = new Incident();
        incident.setId(UUID.randomUUID());
        incident.setIncidentNumber("INC-SLA-" + (System.currentTimeMillis() % 10000 + (int)(Math.random() * 1000)));
        incident.setTitle("Active Incident: " + category);
        incident.setCategory(category);
        incident.setSeverityIndicator(severity);
        incident.setTier(tier);
        incident.setReportedAt(baseTime);
        incident.setStatus(IncidentStatus.OPEN);

        jdbcTemplate.update("""
            INSERT INTO incidents (id, incident_number, title, category, severity, current_status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """, incident.getId(), incident.getIncidentNumber(), incident.getTitle(), incident.getCategory(), incident.getSeverityIndicator(), incident.getStatus().name());

        return incident;
    }

    @Test
    @DisplayName("1. Policy CRUD: Create and retrieve SLA policies")
    void testPolicyCrud() {
        CreateSlaPolicyRequest req = new CreateSlaPolicyRequest("Flood", "CRITICAL", 10, 30, true);
        SlaPolicyResponse policy = slaService.createPolicy(req);

        assertNotNull(policy.getId());
        assertEquals("Flood", policy.getCategory());
        assertEquals("CRITICAL", policy.getSeverity());
        assertEquals(10, policy.getResponseTimeMinutes());
        assertEquals(30, policy.getResolutionTimeMinutes());

        List<SlaPolicyResponse> all = slaService.getAllPolicies();
        assertThat(all).hasSize(1);
    }

    @Test
    @DisplayName("2. Incident creation assigns configured SLA Policy and sets response/resolution deadlines")
    void testAssignPolicyAndDeadlines() {
        // Seed policy
        slaService.createPolicy(new CreateSlaPolicyRequest("Traffic", "HIGH", 20, 90, true));

        Incident incident = createIncident("Traffic", "HIGH", PriorityTier.HIGH);

        SLAResponse sla = slaService.assignPolicyAndCreateSLA(incident);

        assertNotNull(sla);
        assertEquals(90, sla.getTargetMinutes());
        assertEquals(baseTime.plus(20, ChronoUnit.MINUTES), sla.getResponseDeadline());
        assertEquals(baseTime.plus(90, ChronoUnit.MINUTES), sla.getResolutionDeadline());
        assertEquals(SLAStatus.ON_TRACK, sla.getStatus());
        assertEquals(EscalationLevel.NONE, sla.getEscalationLevel());
        assertNull(sla.getFirstRespondedAt());
    }

    @Test
    @DisplayName("3. Approaching deadline triggers WARNING and escalates to LEVEL_1")
    void testWarningTransitionAndLevel1Escalation() {
        slaService.createPolicy(new CreateSlaPolicyRequest("Fire", "CRITICAL", 10, 30, true));
        Incident incident = createIncident("Fire", "CRITICAL", PriorityTier.CRITICAL);
        slaService.assignPolicyAndCreateSLA(incident);

        // Record first response on-time at 12:05 (within 10m response deadline)
        slaService.recordFirstResponse(incident.getId(), baseTime.plus(5, ChronoUnit.MINUTES));

        // At 12:25 (5 minutes remaining out of 30, ratio ~ 16.7% <= 25%) -> WARNING and LEVEL_1
        slaService.setClock(Clock.fixed(baseTime.plus(25, ChronoUnit.MINUTES), ZoneOffset.UTC));

        int evaluated = slaService.evaluateAllActiveSLAs();
        assertEquals(1, evaluated);

        SLAResponse updated = slaService.getSLAByIncidentId(incident.getId());
        assertTrue(updated.getStatus().isWarning());
        assertEquals(EscalationLevel.LEVEL_1, updated.getEscalationLevel());
        assertEquals(5L, updated.getRemainingMinutes());

        // Check escalation history audit
        List<SlaEscalationResponse> escalations = slaService.getEscalationHistory(10);
        assertThat(escalations).hasSize(1);
        assertEquals("LEVEL_1", escalations.get(0).getNewEscalationLevel());
    }

    @Test
    @DisplayName("4. Breached deadline escalates to LEVEL_2 and then LEVEL_3 on repeated breach")
    void testBreachProgressionLevel2AndLevel3() {
        slaService.createPolicy(new CreateSlaPolicyRequest("Infrastructure", "CRITICAL", 10, 30, true));
        Incident incident = createIncident("Infrastructure", "CRITICAL", PriorityTier.CRITICAL);
        slaService.assignPolicyAndCreateSLA(incident);

        // Step 1: At 12:35 (5 minutes past deadline) -> BREACHED & LEVEL_2
        slaService.setClock(Clock.fixed(baseTime.plus(35, ChronoUnit.MINUTES), ZoneOffset.UTC));
        slaService.evaluateAllActiveSLAs();

        SLAResponse breached = slaService.getSLAByIncidentId(incident.getId());
        assertEquals(SLAStatus.BREACHED, breached.getStatus());
        assertEquals(EscalationLevel.LEVEL_2, breached.getEscalationLevel());
        assertTrue(breached.getBreached());

        // Step 2: Next scheduler run (unresolved) -> escalates to LEVEL_3
        slaService.setClock(Clock.fixed(baseTime.plus(45, ChronoUnit.MINUTES), ZoneOffset.UTC));
        slaService.evaluateAllActiveSLAs();

        SLAResponse escalated = slaService.getSLAByIncidentId(incident.getId());
        assertEquals(SLAStatus.BREACHED, escalated.getStatus());
        assertEquals(EscalationLevel.LEVEL_3, escalated.getEscalationLevel());

        // Check escalation history records both steps without duplicates
        List<SlaEscalationResponse> history = slaService.getEscalationHistory(10);
        assertThat(history).hasSize(2);
        assertEquals("LEVEL_3", history.get(0).getNewEscalationLevel());
        assertEquals("LEVEL_2", history.get(1).getNewEscalationLevel());
    }

    @Test
    @DisplayName("5. Idempotent scheduler runs do not generate duplicate escalation history")
    void testSchedulerIdempotency() {
        slaService.createPolicy(new CreateSlaPolicyRequest("Pollution", "HIGH", 30, 120, true));
        Incident incident = createIncident("Pollution", "HIGH", PriorityTier.HIGH);
        slaService.assignPolicyAndCreateSLA(incident);

        // Record first response within 30 min limit
        slaService.recordFirstResponse(incident.getId(), baseTime.plus(15, ChronoUnit.MINUTES));

        // Advance into warning period (100 min elapsed out of 120 min)
        slaService.setClock(Clock.fixed(baseTime.plus(100, ChronoUnit.MINUTES), ZoneOffset.UTC));

        // Run evaluation 3 consecutive times
        slaService.evaluateAllActiveSLAs();
        slaService.evaluateAllActiveSLAs();
        slaService.evaluateAllActiveSLAs();

        // Must only have 1 escalation history entry for LEVEL_1
        List<SlaEscalationResponse> history = slaService.getEscalationHistory(10);
        assertThat(history).hasSize(1);
    }

    @Test
    @DisplayName("6. First response records timestamp and halts response tracking")
    void testFirstResponseTracking() {
        slaService.createPolicy(new CreateSlaPolicyRequest("Flood", "MEDIUM", 45, 360, true));
        Incident incident = createIncident("Flood", "MEDIUM", PriorityTier.MEDIUM);
        slaService.assignPolicyAndCreateSLA(incident);

        Instant responseTime = baseTime.plus(15, ChronoUnit.MINUTES);
        SLAResponse resp = slaService.recordFirstResponse(incident.getId(), responseTime);

        assertNotNull(resp);
        assertNotNull(resp.getFirstRespondedAt());
        assertEquals(responseTime, resp.getFirstRespondedAt());
    }

    @Test
    @DisplayName("7. Incident resolution marks SLA COMPLETED, resets escalation to NONE, and stops further escalations")
    void testResolutionStopsEscalation() {
        slaService.createPolicy(new CreateSlaPolicyRequest("Fire", "HIGH", 15, 60, true));
        Incident incident = createIncident("Fire", "HIGH", PriorityTier.HIGH);
        slaService.assignPolicyAndCreateSLA(incident);

        Instant resolvedAt = baseTime.plus(40, ChronoUnit.MINUTES);
        SLAResponse resolved = slaService.markIncidentResolved(incident.getId(), resolvedAt);

        assertNotNull(resolved);
        assertTrue(resolved.getStatus().isTerminal());
        assertEquals(EscalationLevel.NONE, resolved.getEscalationLevel());
        assertEquals(0L, resolved.getRemainingMinutes());

        // Subsequent scheduler run even far past deadline must not re-escalate or change status
        slaService.setClock(Clock.fixed(baseTime.plus(180, ChronoUnit.MINUTES), ZoneOffset.UTC));
        incident.setStatus(IncidentStatus.RESOLVED);

        slaService.evaluateAllActiveSLAs();

        SLAResponse finalState = slaService.getSLAByIncidentId(incident.getId());
        assertTrue(finalState.getStatus().isTerminal());
        assertEquals(EscalationLevel.NONE, finalState.getEscalationLevel());
    }
}
