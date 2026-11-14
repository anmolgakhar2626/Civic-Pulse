package com.smartcity.incident;

import com.smartcity.priority.Severity;
import com.smartcity.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IncidentJdbcRepository & SQL Parameterization Integration Tests")
class IncidentJdbcRepositoryTest {

    private JdbcTemplate jdbcTemplate;
    private IncidentJdbcRepository incidentJdbcRepository;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:incident_jdbc_test_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        jdbcTemplate = new JdbcTemplate(dataSource);
        incidentJdbcRepository = new IncidentJdbcRepository(jdbcTemplate);

        // Setup users table
        jdbcTemplate.execute("""
            CREATE TABLE users (
                id UUID PRIMARY KEY,
                full_name VARCHAR(150) NOT NULL,
                email VARCHAR(150) NOT NULL UNIQUE,
                username VARCHAR(100) UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                role VARCHAR(50) NOT NULL DEFAULT 'CITIZEN',
                enabled BOOLEAN NOT NULL DEFAULT TRUE,
                phone VARCHAR(50),
                mobile_number VARCHAR(50),
                is_mobile_verified BOOLEAN DEFAULT FALSE,
                genuine_complaint_count INT DEFAULT 0,
                is_verified_citizen BOOLEAN DEFAULT FALSE,
                department_id UUID,
                team_id UUID,
                role_id UUID,
                active BOOLEAN DEFAULT TRUE,
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL
            )
        """);

        // Setup departments and teams table
        jdbcTemplate.execute("""
            CREATE TABLE departments (
                id UUID PRIMARY KEY,
                code VARCHAR(50),
                name VARCHAR(100)
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE teams (
                id UUID PRIMARY KEY,
                code VARCHAR(50),
                name VARCHAR(100)
            )
        """);

        // Setup incidents table matching V10 schema
        jdbcTemplate.execute("""
            CREATE TABLE incidents (
                id UUID PRIMARY KEY,
                incident_number VARCHAR(50) NOT NULL UNIQUE,
                title VARCHAR(200),
                category VARCHAR(100),
                incident_type VARCHAR(100),
                description TEXT NOT NULL,
                severity VARCHAR(50),
                severity_indicator VARCHAR(50),
                status VARCHAR(50) DEFAULT 'OPEN',
                current_status VARCHAR(50) DEFAULT 'OPEN',
                location_name VARCHAR(255),
                address VARCHAR(255),
                latitude DOUBLE PRECISION NOT NULL,
                longitude DOUBLE PRECISION NOT NULL,
                source VARCHAR(50) DEFAULT 'CITIZEN',
                reported_by UUID,
                created_by_user_id UUID,
                assigned_to UUID,
                department_id UUID,
                assigned_team_id UUID,
                escalation_state VARCHAR(50) DEFAULT 'NONE',
                evidence_url VARCHAR(500),
                reason_notes TEXT,
                resolution_notes TEXT,
                reported_time TIMESTAMP NOT NULL,
                detection_time TIMESTAMP NOT NULL,
                resolution_time TIMESTAMP,
                priority VARCHAR(20),
                priority_label VARCHAR(50),
                priority_reason TEXT,
                safety_impact VARCHAR(100),
                priority_rule_id VARCHAR(100),
                sla_minutes INT,
                sla_deadline TIMESTAMP,
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL
            )
        """);

        testUserId = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO users (id, full_name, email, username, password_hash, role, enabled, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """, testUserId, "Commander Vikram", "vikram@civicpulse.gov", "vikram", "hashed", "COMMAND_OPERATOR", true);
    }

    @Test
    @DisplayName("Should insert incident using parameterized SQL and retrieve by ID")
    void shouldInsertAndFindIncidentById() {
        Incident incident = new Incident();
        incident.setTitle("Flash Flood at Downtown Underpass");
        incident.setDescription("Water level exceeded 1.5m, traffic halted");
        incident.setCategory("Flood");
        incident.setSeverity(Severity.CRITICAL);
        incident.setStatus(IncidentStatus.OPEN);
        incident.setLocationName("Downtown Railway Underpass");
        incident.setLatitude(12.9716);
        incident.setLongitude(77.5946);

        User reporter = new User();
        reporter.setId(testUserId);
        incident.setReportedBy(reporter);

        Incident saved = incidentJdbcRepository.save(incident);

        assertNotNull(saved.getId());
        assertNotNull(saved.getIncidentNumber());
        assertTrue(saved.getIncidentNumber().startsWith("INC-"));

        Optional<Incident> retrievedOpt = incidentJdbcRepository.findById(saved.getId());
        assertTrue(retrievedOpt.isPresent());

        Incident retrieved = retrievedOpt.get();
        assertEquals("Flash Flood at Downtown Underpass", retrieved.getTitle());
        assertEquals("Flood", retrieved.getCategory());
        assertEquals(Severity.CRITICAL, retrieved.getSeverity());
        assertEquals(IncidentStatus.OPEN, retrieved.getStatus());
        assertEquals("Downtown Railway Underpass", retrieved.getLocationName());
        assertEquals(12.9716, retrieved.getLatitude(), 0.0001);
        assertEquals(77.5946, retrieved.getLongitude(), 0.0001);
        assertNotNull(retrieved.getReportedBy());
        assertEquals(testUserId, retrieved.getReportedBy().getId());
        assertEquals("Commander Vikram", retrieved.getReportedBy().getFullName());
    }

    @Test
    @DisplayName("Should retrieve incident by incident number")
    void shouldFindByIncidentNumber() {
        Incident incident = new Incident();
        incident.setIncidentNumber("INC-20261028-TEST001");
        incident.setTitle("Major Power Grid Outage");
        incident.setDescription("Substation transformer fault affecting Sector 4");
        incident.setCategory("Infrastructure");
        incident.setLatitude(28.6139);
        incident.setLongitude(77.2090);

        incidentJdbcRepository.save(incident);

        Optional<Incident> retrieved = incidentJdbcRepository.findByIncidentNumber("INC-20261028-TEST001");
        assertTrue(retrieved.isPresent());
        assertEquals("Major Power Grid Outage", retrieved.get().getTitle());
        assertEquals("Infrastructure", retrieved.get().getCategory());
    }

    @Test
    @DisplayName("Should update incident fields using parameterized SQL")
    void shouldUpdateIncidentFields() {
        Incident incident = new Incident();
        incident.setTitle("Traffic Signal Glitch");
        incident.setDescription("Amber light flashing continuously");
        incident.setCategory("Traffic");
        incident.setSeverity(Severity.LOW);
        incident.setStatus(IncidentStatus.OPEN);
        incident.setLocationName("Crossroad 5");
        incident.setLatitude(19.0760);
        incident.setLongitude(72.8777);

        Incident saved = incidentJdbcRepository.save(incident);

        // Update fields
        saved.setTitle("Traffic Signal Complete Failure");
        saved.setDescription("Signal is fully dark during rush hour");
        saved.setSeverity(Severity.HIGH);
        saved.setLocationName("Crossroad 5 Main Junction");

        int rows = incidentJdbcRepository.update(saved.getId(), saved);
        assertEquals(1, rows);

        Incident updated = incidentJdbcRepository.findById(saved.getId()).orElseThrow();
        assertEquals("Traffic Signal Complete Failure", updated.getTitle());
        assertEquals("Signal is fully dark during rush hour", updated.getDescription());
        assertEquals(Severity.HIGH, updated.getSeverity());
        assertEquals("Crossroad 5 Main Junction", updated.getLocationName());
    }

    @Test
    @DisplayName("Should update status and record resolution timestamp")
    void shouldUpdateStatusAndResolutionTime() {
        Incident incident = new Incident();
        incident.setTitle("Hazardous Chemical Spill");
        incident.setDescription("Industrial solvent leaking from transport truck");
        incident.setCategory("Pollution");
        incident.setStatus(IncidentStatus.OPEN);
        incident.setLatitude(13.0827);
        incident.setLongitude(80.2707);

        Incident saved = incidentJdbcRepository.save(incident);
        assertEquals(IncidentStatus.OPEN, saved.getStatus());
        assertNull(saved.getResolutionTime());

        // Transition to IN_PROGRESS
        incidentJdbcRepository.updateStatus(saved.getId(), "IN_PROGRESS", "Hazmat team dispatched", "vikram");
        Incident inProgress = incidentJdbcRepository.findById(saved.getId()).orElseThrow();
        assertEquals(IncidentStatus.IN_PROGRESS, inProgress.getStatus());
        assertNull(inProgress.getResolutionTime());

        // Transition to RESOLVED
        incidentJdbcRepository.updateStatus(saved.getId(), "RESOLVED", "Area neutralized and decontaminated", "vikram");
        Incident resolved = incidentJdbcRepository.findById(saved.getId()).orElseThrow();
        assertEquals(IncidentStatus.RESOLVED, resolved.getStatus());
        assertNotNull(resolved.getResolutionTime());
    }

    @Test
    @DisplayName("Should filter and paginate incidents with parameterized queries")
    void shouldFilterAndPaginateIncidents() {
        createTestIncident("Severe Highway Fire", "Fire", "CRITICAL", "OPEN", "Highway 44 Mile 12");
        createTestIncident("Brush Fire near Residential Area", "Fire", "HIGH", "IN_PROGRESS", "Green Valley Sector 2");
        createTestIncident("Traffic Congestion on Main Arterial", "Traffic", "MEDIUM", "OPEN", "North Ring Road");
        createTestIncident("Severe Air Pollution Spike", "Pollution", "CRITICAL", "RESOLVED", "Industrial Zone East");
        createTestIncident("Water Pipeline Rupture", "Infrastructure", "HIGH", "CLOSED", "Waterworks Boulevard");

        // 1. Filter by category
        List<Incident> fires = incidentJdbcRepository.findPaginatedAndFiltered(null, "Fire", null, null, 0, 10);
        assertEquals(2, fires.size());
        long fireCount = incidentJdbcRepository.countFiltered(null, "Fire", null, null);
        assertEquals(2, fireCount);

        // 2. Filter by severity
        List<Incident> criticals = incidentJdbcRepository.findPaginatedAndFiltered(null, null, "CRITICAL", null, 0, 10);
        assertEquals(2, criticals.size());

        // 3. Filter by status
        List<Incident> openOnes = incidentJdbcRepository.findPaginatedAndFiltered(null, null, null, "OPEN", 0, 10);
        assertEquals(2, openOnes.size());

        // 4. Search by title / location text
        List<Incident> searchResults = incidentJdbcRepository.findPaginatedAndFiltered("Highway", null, null, null, 0, 10);
        assertEquals(1, searchResults.size());
        assertEquals("Severe Highway Fire", searchResults.get(0).getTitle());

        // 5. Pagination
        List<Incident> page1 = incidentJdbcRepository.findPaginatedAndFiltered(null, null, null, null, 0, 2);
        assertEquals(2, page1.size());

        List<Incident> page2 = incidentJdbcRepository.findPaginatedAndFiltered(null, null, null, null, 2, 2);
        assertEquals(2, page2.size());

        long totalCount = incidentJdbcRepository.countFiltered(null, null, null, null);
        assertEquals(5, totalCount);
    }

    @Test
    @DisplayName("Should delete incident by ID")
    void shouldDeleteIncident() {
        Incident incident = new Incident();
        incident.setTitle("Temporary False Alarm");
        incident.setDescription("Sensor glitch detected and confirmed false alarm");
        incident.setCategory("Traffic");
        incident.setLatitude(12.0);
        incident.setLongitude(77.0);

        Incident saved = incidentJdbcRepository.save(incident);
        assertTrue(incidentJdbcRepository.existsById(saved.getId()));

        int deletedRows = incidentJdbcRepository.deleteById(saved.getId());
        assertEquals(1, deletedRows);

        assertFalse(incidentJdbcRepository.existsById(saved.getId()));
        assertTrue(incidentJdbcRepository.findById(saved.getId()).isEmpty());
    }

    @Test
    @DisplayName("Should handle nonexistent record gracefully")
    void shouldHandleNonexistentRecord() {
        UUID nonExistentId = UUID.randomUUID();
        assertFalse(incidentJdbcRepository.existsById(nonExistentId));
        assertTrue(incidentJdbcRepository.findById(nonExistentId).isEmpty());
        int deleted = incidentJdbcRepository.deleteById(nonExistentId);
        assertEquals(0, deleted);
    }

    private void createTestIncident(String title, String category, String severity, String status, String location) {
        Incident inc = new Incident();
        inc.setTitle(title);
        inc.setDescription("Detailed observations for " + title);
        inc.setCategory(category);
        inc.setSeverity(Severity.valueOf(severity));
        inc.setStatus(IncidentStatus.valueOf(status));
        inc.setLocationName(location);
        inc.setLatitude(12.9 + Math.random() * 0.1);
        inc.setLongitude(77.5 + Math.random() * 0.1);
        incidentJdbcRepository.save(inc);
    }
}
