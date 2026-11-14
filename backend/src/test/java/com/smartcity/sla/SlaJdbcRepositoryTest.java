package com.smartcity.sla;

import com.smartcity.incident.Incident;
import com.smartcity.priority.PriorityTier;
import com.smartcity.sla.dto.SlaEscalationResponse;
import com.smartcity.sla.dto.SlaOverviewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SLA JDBC Repository & SQL Parameterization Integration Tests")
class SlaJdbcRepositoryTest {

    private JdbcTemplate jdbcTemplate;
    private SlaJdbcRepository slaJdbcRepository;
    private UUID testIncidentId;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:sla_jdbc_test_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        jdbcTemplate = new JdbcTemplate(dataSource);
        slaJdbcRepository = new SlaJdbcRepository(jdbcTemplate);

        // Setup mock incidents table
        jdbcTemplate.execute("""
            CREATE TABLE incidents (
                id UUID PRIMARY KEY,
                incident_number VARCHAR(50) NOT NULL UNIQUE,
                title VARCHAR(255) NOT NULL,
                category VARCHAR(100) NOT NULL,
                severity VARCHAR(50) NOT NULL,
                current_status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL
            )
        """);

        // Setup sla_policies table
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
                CONSTRAINT uq_policy UNIQUE (category, severity)
            )
        """);

        // Setup sla_records table
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

        // Setup sla_escalation_history table
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

        // Insert test incident
        testIncidentId = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO incidents (id, incident_number, title, category, severity, current_status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """, testIncidentId, "INC-TEST-001", "Chemical Leak Alert", "Pollution", "CRITICAL", "OPEN");
    }

    @Test
    @DisplayName("1. Persist and retrieve SLA Policy using parameterized SQL")
    void testSaveAndFindPolicy() {
        SLAPolicy policy = new SLAPolicy("Pollution", "CRITICAL", 15, 60, true);
        SLAPolicy saved = slaJdbcRepository.savePolicy(policy);

        assertNotNull(saved.getId());
        Optional<SLAPolicy> fetched = slaJdbcRepository.findPolicyById(saved.getId());
        assertTrue(fetched.isPresent());
        assertEquals("Pollution", fetched.get().getCategory());
        assertEquals("CRITICAL", fetched.get().getSeverity());
        assertEquals(15, fetched.get().getResponseTimeMinutes());
        assertEquals(60, fetched.get().getResolutionTimeMinutes());
        assertTrue(fetched.get().getEnabled());
    }

    @Test
    @DisplayName("2. Lookup policy by category and severity with fallback to DEFAULT")
    void testPolicyLookupWithFallback() {
        // Save a default policy
        SLAPolicy defaultHigh = new SLAPolicy("DEFAULT", "HIGH", 30, 120, true);
        slaJdbcRepository.savePolicy(defaultHigh);

        // Lookup with unmapped category 'UnknownCat' and severity 'HIGH'
        Optional<SLAPolicy> fallback = slaJdbcRepository.findPolicyByCategoryAndSeverity("UnknownCat", "HIGH");
        assertTrue(fallback.isPresent());
        assertEquals("DEFAULT", fallback.get().getCategory());
        assertEquals(30, fallback.get().getResponseTimeMinutes());
        assertEquals(120, fallback.get().getResolutionTimeMinutes());
    }

    @Test
    @DisplayName("3. Create, update, and retrieve SLA record with response & resolution deadlines")
    void testSaveAndRetrieveSlaRecord() {
        Instant now = Instant.now();
        Instant respDeadline = now.plus(15, ChronoUnit.MINUTES);
        Instant resDeadline = now.plus(60, ChronoUnit.MINUTES);

        Incident incident = new Incident();
        incident.setId(testIncidentId);

        SLARecord record = new SLARecord();
        record.setId(UUID.randomUUID());
        record.setIncident(incident);
        record.setPriorityTier(PriorityTier.CRITICAL);
        record.setTargetMinutes(60);
        record.setStartedAt(now);
        record.setDeadlineAt(resDeadline);
        record.setResponseDeadline(respDeadline);
        record.setResolutionDeadline(resDeadline);
        record.setStatus(SLAStatus.ON_TRACK);
        record.setRemainingMinutes(60L);
        record.setEscalationLevel(EscalationLevel.NONE);
        record.setBreached(false);

        slaJdbcRepository.saveRecord(record);

        Optional<SLARecord> fetched = slaJdbcRepository.findRecordByIncidentId(testIncidentId);
        assertTrue(fetched.isPresent());
        assertEquals(testIncidentId, fetched.get().getIncidentId());
        assertEquals(SLAStatus.ON_TRACK, fetched.get().getStatus());
        assertEquals(PriorityTier.CRITICAL, fetched.get().getPriorityTier());
        assertEquals(60, fetched.get().getTargetMinutes());
        assertEquals("INC-TEST-001", fetched.get().getIncident().getIncidentNumber());
    }

    @Test
    @DisplayName("4. Record first response timestamp stops first-response tracking")
    void testRecordFirstResponse() {
        Instant now = Instant.now();
        Incident incident = new Incident();
        incident.setId(testIncidentId);

        SLARecord record = new SLARecord();
        record.setId(UUID.randomUUID());
        record.setIncident(incident);
        record.setPriorityTier(PriorityTier.CRITICAL);
        record.setTargetMinutes(60);
        record.setStartedAt(now);
        record.setResponseDeadline(now.plus(15, ChronoUnit.MINUTES));
        record.setResolutionDeadline(now.plus(60, ChronoUnit.MINUTES));
        record.setStatus(SLAStatus.ON_TRACK);
        slaJdbcRepository.saveRecord(record);

        Instant responseTime = now.plus(5, ChronoUnit.MINUTES);
        int updated = slaJdbcRepository.recordFirstResponse(testIncidentId, responseTime);
        assertEquals(1, updated);

        Optional<SLARecord> fetched = slaJdbcRepository.findRecordByIncidentId(testIncidentId);
        assertTrue(fetched.isPresent());
        assertNotNull(fetched.get().getFirstRespondedAt());
    }

    @Test
    @DisplayName("5. Record incident resolution marks SLA COMPLETED and resets escalation")
    void testRecordResolution() {
        Instant now = Instant.now();
        Incident incident = new Incident();
        incident.setId(testIncidentId);

        SLARecord record = new SLARecord();
        record.setId(UUID.randomUUID());
        record.setIncident(incident);
        record.setPriorityTier(PriorityTier.HIGH);
        record.setTargetMinutes(120);
        record.setStartedAt(now);
        record.setDeadlineAt(now.plus(120, ChronoUnit.MINUTES));
        record.setStatus(SLAStatus.WARNING);
        record.setEscalationLevel(EscalationLevel.LEVEL_1);
        slaJdbcRepository.saveRecord(record);

        Instant resolvedTime = now.plus(45, ChronoUnit.MINUTES);
        int updated = slaJdbcRepository.recordResolution(testIncidentId, resolvedTime);
        assertEquals(1, updated);

        Optional<SLARecord> fetched = slaJdbcRepository.findRecordByIncidentId(testIncidentId);
        assertTrue(fetched.isPresent());
        assertEquals(SLAStatus.COMPLETED, fetched.get().getStatus());
        assertEquals(EscalationLevel.NONE, fetched.get().getEscalationLevel());
        assertEquals(0L, fetched.get().getRemainingMinutes());
        assertNotNull(fetched.get().getResolvedAt());
    }

    @Test
    @DisplayName("6. Escalation history prevents duplicate audits and retrieves audit logs")
    void testEscalationHistoryAndDuplicatePrevention() {
        UUID slaRecordId = UUID.randomUUID();

        assertFalse(slaJdbcRepository.hasRecentEscalation(testIncidentId, "LEVEL_1"));

        slaJdbcRepository.recordEscalation(
                testIncidentId,
                slaRecordId,
                "NONE",
                "LEVEL_1",
                "ON_TRACK",
                "WARNING",
                "Approaching 20% remaining threshold",
                Instant.now()
        );

        assertTrue(slaJdbcRepository.hasRecentEscalation(testIncidentId, "LEVEL_1"));
        assertFalse(slaJdbcRepository.hasRecentEscalation(testIncidentId, "LEVEL_2"));

        List<SlaEscalationResponse> history = slaJdbcRepository.getEscalationHistory(10);
        assertThat(history).hasSize(1);
        assertEquals("LEVEL_1", history.get(0).getNewEscalationLevel());
        assertEquals("INC-TEST-001", history.get(0).getIncidentNumber());
    }

    @Test
    @DisplayName("7. Overview metrics aggregate counts across active, warning, breached, and completed")
    void testOverviewMetrics() {
        Instant now = Instant.now();

        // 1 Active On-Track
        SLARecord r1 = new SLARecord();
        r1.setId(UUID.randomUUID());
        Incident inc1 = new Incident(); inc1.setId(testIncidentId);
        r1.setIncident(inc1);
        r1.setStartedAt(now);
        r1.setTargetMinutes(30);
        r1.setStatus(SLAStatus.ON_TRACK);
        slaJdbcRepository.saveRecord(r1);

        SlaOverviewResponse overview = slaJdbcRepository.getOverviewMetrics();
        assertNotNull(overview);
        assertEquals(1, overview.getTotalActive());
        assertEquals(1, overview.getOnTrackCount());
        assertEquals(0, overview.getWarningCount());
        assertEquals(0, overview.getBreachedCount());
        assertEquals(0, overview.getCompletedCount());
    }
}

