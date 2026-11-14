package com.smartcity.map;

import com.smartcity.map.dto.MapIncidentMarkerResponse;
import com.smartcity.map.dto.MapInfrastructureResponse;
import com.smartcity.map.dto.MapShelterResponse;
import com.smartcity.map.dto.MapSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MapJdbcRepository GIS Map & Bounding-box Query Tests")
class MapJdbcRepositoryTest {

    private JdbcTemplate jdbcTemplate;
    private MapJdbcRepository mapRepository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:map_jdbc_test_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        jdbcTemplate = new JdbcTemplate(dataSource);
        mapRepository = new MapJdbcRepository(jdbcTemplate);

        jdbcTemplate.execute("""
            CREATE TABLE incidents (
                id UUID PRIMARY KEY,
                incident_number VARCHAR(50) NOT NULL UNIQUE,
                title VARCHAR(200),
                category VARCHAR(100),
                incident_type VARCHAR(100),
                severity VARCHAR(50),
                severity_indicator VARCHAR(50),
                status VARCHAR(50),
                current_status VARCHAR(50),
                latitude DOUBLE PRECISION,
                longitude DOUBLE PRECISION,
                location_name VARCHAR(255),
                address VARCHAR(255),
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL,
                reported_by UUID,
                assigned_to UUID
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE infrastructure (
                id UUID PRIMARY KEY,
                code VARCHAR(50) NOT NULL UNIQUE,
                name VARCHAR(150) NOT NULL,
                infrastructure_type VARCHAR(100) NOT NULL,
                latitude DOUBLE PRECISION,
                longitude DOUBLE PRECISION,
                address VARCHAR(255),
                status VARCHAR(50) NOT NULL DEFAULT 'OPERATIONAL',
                importance_level VARCHAR(50) NOT NULL DEFAULT 'PRIMARY',
                metadata_json TEXT,
                last_inspected_at TIMESTAMP,
                maintenance_priority VARCHAR(50) DEFAULT 'MEDIUM',
                notes TEXT,
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE emergency_resources (
                id UUID PRIMARY KEY,
                name VARCHAR(150) NOT NULL,
                resource_type VARCHAR(100) NOT NULL,
                latitude DOUBLE PRECISION,
                longitude DOUBLE PRECISION,
                address VARCHAR(255),
                contact_number VARCHAR(50),
                available BOOLEAN NOT NULL DEFAULT TRUE,
                capacity INT DEFAULT 0,
                operational_hours VARCHAR(100) DEFAULT '24/7',
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE sensors_gis (
                id UUID PRIMARY KEY,
                sensor_id VARCHAR(100) NOT NULL UNIQUE,
                sensor_type VARCHAR(100) NOT NULL,
                location_name VARCHAR(255),
                latitude DOUBLE PRECISION NOT NULL,
                longitude DOUBLE PRECISION NOT NULL,
                status VARCHAR(50) NOT NULL DEFAULT 'ONLINE',
                last_value DOUBLE PRECISION,
                unit VARCHAR(50),
                last_seen_at TIMESTAMP,
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL
            )
        """);
    }

    @Test
    @DisplayName("findIncidents returns incidents with coordinates and strictly excludes NULL coordinates")
    void testFindIncidentsExcludesNullCoordinates() {
        // Incident 1: valid coordinates
        jdbcTemplate.update("""
            INSERT INTO incidents (id, incident_number, title, category, severity, status, latitude, longitude, created_at, updated_at)
            VALUES (?, 'INC-001', 'Traffic Jam', 'TRAFFIC', 'HIGH', 'OPEN', 30.7333, 76.7794, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """, UUID.randomUUID());

        // Incident 2: NULL coordinates (must NOT appear on map)
        jdbcTemplate.update("""
            INSERT INTO incidents (id, incident_number, title, category, severity, status, latitude, longitude, created_at, updated_at)
            VALUES (?, 'INC-002', 'General Complaint', 'COMPLAINT', 'LOW', 'OPEN', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """, UUID.randomUUID());

        List<MapIncidentMarkerResponse> markers = mapRepository.findIncidents(
                null, null, null, null, null, null, null, null, null, 10
        );

        assertEquals(1, markers.size());
        assertEquals("INC-001", markers.get(0).getIncidentNumber());
        assertEquals(30.7333, markers.get(0).getLatitude());
        assertEquals(76.7794, markers.get(0).getLongitude());
    }

    @Test
    @DisplayName("findIncidents filters accurately by bounding box, category, severity, and status")
    void testFindIncidentsWithBoundingBoxAndFilters() {
        jdbcTemplate.update("""
            INSERT INTO incidents (id, incident_number, title, category, severity, status, latitude, longitude, created_at, updated_at)
            VALUES (?, 'INC-IN-BOX', 'Water Burst', 'WATER', 'CRITICAL', 'OPEN', 30.745, 76.785, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """, UUID.randomUUID());

        jdbcTemplate.update("""
            INSERT INTO incidents (id, incident_number, title, category, severity, status, latitude, longitude, created_at, updated_at)
            VALUES (?, 'INC-OUT-BOX', 'Minor Leak', 'WATER', 'LOW', 'RESOLVED', 30.710, 76.720, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """, UUID.randomUUID());

        // Bounding box query
        List<MapIncidentMarkerResponse> inBox = mapRepository.findIncidents(
                30.740, 30.750, 76.780, 76.790, null, null, null, null, null, 10
        );
        assertEquals(1, inBox.size());
        assertEquals("INC-IN-BOX", inBox.get(0).getIncidentNumber());

        // Category & Severity filter
        List<MapIncidentMarkerResponse> critical = mapRepository.findIncidents(
                null, null, null, null, "WATER", "CRITICAL", "OPEN", null, null, 10
        );
        assertEquals(1, critical.size());
        assertEquals("INC-IN-BOX", critical.get(0).getIncidentNumber());
    }

    @Test
    @DisplayName("findInfrastructure filters by category and status")
    void testFindInfrastructure() {
        jdbcTemplate.update("""
            INSERT INTO infrastructure (id, code, name, infrastructure_type, status, latitude, longitude, created_at, updated_at)
            VALUES (?, 'INF-SUB-01', 'Power Substation 17', 'SUBSTATION', 'OPERATIONAL', 30.739, 76.784, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """, UUID.randomUUID());

        jdbcTemplate.update("""
            INSERT INTO infrastructure (id, code, name, infrastructure_type, status, latitude, longitude, created_at, updated_at)
            VALUES (?, 'INF-PIPE-01', 'Trunk Pipeline', 'WATER_PIPELINE', 'DEGRADED', 30.731, 76.795, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """, UUID.randomUUID());

        List<MapInfrastructureResponse> substations = mapRepository.findInfrastructure(
                null, null, null, null, "SUBSTATION", null, 10
        );
        assertEquals(1, substations.size());
        assertEquals("INF-SUB-01", substations.get(0).getCode());

        List<MapInfrastructureResponse> operational = mapRepository.findInfrastructure(
                null, null, null, null, null, "OPERATIONAL", 10
        );
        assertEquals(1, operational.size());
        assertEquals("INF-SUB-01", operational.get(0).getCode());
    }

    @Test
    @DisplayName("findShelters returns public refuge shelters from emergency_resources and infrastructure")
    void testFindShelters() {
        jdbcTemplate.update("""
            INSERT INTO emergency_resources (id, name, resource_type, latitude, longitude, address, contact_number, available, capacity, operational_hours, created_at, updated_at)
            VALUES (?, 'Community Center Shelter 22', 'EMERGENCY_SHELTER', 30.735, 76.772, 'Sector 22-B', '123456', true, 350, '24/7', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """, UUID.randomUUID());

        jdbcTemplate.update("""
            INSERT INTO infrastructure (id, code, name, infrastructure_type, status, latitude, longitude, address, created_at, updated_at)
            VALUES (?, 'INF-SHL-01', 'Sector 19 Disaster Shelter', 'SHELTER', 'OPERATIONAL', 30.738, 76.791, 'Sector 19-C', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """, UUID.randomUUID());

        List<MapShelterResponse> shelters = mapRepository.findShelters(
                null, null, null, null, true, 10
        );
        assertEquals(2, shelters.size());
        assertThat(shelters).extracting(MapShelterResponse::getName)
                .containsExactlyInAnyOrder("Community Center Shelter 22", "Sector 19 Disaster Shelter");
    }

    @Test
    @DisplayName("getSummary aggregates counts accurately")
    void testGetSummary() {
        jdbcTemplate.update("""
            INSERT INTO incidents (id, incident_number, title, category, severity, status, latitude, longitude, created_at, updated_at)
            VALUES (?, 'INC-1', 'Active Critical', 'FIRE', 'CRITICAL', 'OPEN', 30.73, 76.77, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                   (?, 'INC-2', 'Resolved Low', 'WASTE', 'LOW', 'RESOLVED', 30.74, 76.78, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """, UUID.randomUUID(), UUID.randomUUID());

        jdbcTemplate.update("""
            INSERT INTO infrastructure (id, code, name, infrastructure_type, status, latitude, longitude, created_at, updated_at)
            VALUES (?, 'INF-1', 'Bridge', 'BRIDGE', 'OPERATIONAL', 30.73, 76.77, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                   (?, 'INF-2', 'Substation', 'SUBSTATION', 'DEGRADED', 30.74, 76.78, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """, UUID.randomUUID(), UUID.randomUUID());

        jdbcTemplate.update("""
            INSERT INTO emergency_resources (id, name, resource_type, latitude, longitude, available, capacity, created_at, updated_at)
            VALUES (?, 'Shelter 1', 'EMERGENCY_SHELTER', 30.73, 76.77, true, 500, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """, UUID.randomUUID());

        jdbcTemplate.update("""
            INSERT INTO sensors_gis (id, sensor_id, sensor_type, latitude, longitude, status, created_at, updated_at)
            VALUES (?, 'AQI-01', 'AIR_QUALITY', 30.73, 76.77, 'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """, UUID.randomUUID());

        MapSummaryResponse summary = mapRepository.getSummary();
        assertNotNull(summary);
        assertEquals(2, summary.getTotalIncidents());
        assertEquals(1, summary.getActiveIncidents());
        assertEquals(1, summary.getCriticalIncidents());
        assertEquals(1, summary.getResolvedIncidents());
        assertEquals(2, summary.getTotalInfrastructure());
        assertEquals(1, summary.getOperationalInfrastructure());
        assertEquals(1, summary.getDegradedInfrastructure());
        assertEquals(1, summary.getTotalShelters());
        assertEquals(1, summary.getAvailableShelters());
        assertEquals(500, summary.getTotalShelterCapacity());
        assertEquals(1, summary.getActiveSensors());
        assertNotNull(summary.getLastUpdated());
    }
}

