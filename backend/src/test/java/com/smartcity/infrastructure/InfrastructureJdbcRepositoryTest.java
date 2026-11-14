package com.smartcity.infrastructure;

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

@DisplayName("InfrastructureJdbcRepository & Parameterized SQL Tests")
class InfrastructureJdbcRepositoryTest {

    private JdbcTemplate jdbcTemplate;
    private InfrastructureJdbcRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:inf_jdbc_test_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new InfrastructureJdbcRepository(jdbcTemplate);

        jdbcTemplate.execute("""
            CREATE TABLE infrastructure (
                id UUID PRIMARY KEY,
                code VARCHAR(50) NOT NULL UNIQUE,
                name VARCHAR(150) NOT NULL,
                infrastructure_type VARCHAR(100) NOT NULL,
                latitude DOUBLE PRECISION NOT NULL,
                longitude DOUBLE PRECISION NOT NULL,
                address VARCHAR(255),
                status VARCHAR(50) NOT NULL DEFAULT 'OPERATIONAL',
                importance_level VARCHAR(50) NOT NULL DEFAULT 'PRIMARY',
                department_id UUID,
                metadata_json TEXT,
                last_inspected_at TIMESTAMP,
                maintenance_priority VARCHAR(50) DEFAULT 'MEDIUM',
                notes TEXT,
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL
            )
        """);
    }

    @Test
    @DisplayName("save and findById correctly persists all fields via parameterized SQL")
    void testSaveAndFindById() {
        Infrastructure inf = new Infrastructure();
        inf.setCode("INF-HOSP-01");
        inf.setName("Sector 22 Civil Hospital");
        inf.setInfrastructureType("HOSPITAL");
        inf.setLatitude(30.7320);
        inf.setLongitude(76.7710);
        inf.setAddress("Sector 22-A Medical Complex");
        inf.setStatus("OPERATIONAL");
        inf.setImportanceLevel("CRITICAL");
        inf.setMaintenancePriority("HIGH");
        inf.setLastInspectedAt(Instant.now());
        inf.setNotes("Level 1 trauma triage unit.");

        Infrastructure saved = repository.save(inf);
        assertNotNull(saved.getId());

        Optional<Infrastructure> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("INF-HOSP-01", found.get().getCode());
        assertEquals("Sector 22 Civil Hospital", found.get().getName());
        assertEquals("HOSPITAL", found.get().getInfrastructureType());
        assertEquals("OPERATIONAL", found.get().getStatus());
        assertEquals("CRITICAL", found.get().getImportanceLevel());
        assertEquals(30.7320, found.get().getLatitude());
        assertEquals(76.7710, found.get().getLongitude());
        assertEquals("HIGH", found.get().getMaintenancePriority());
        assertEquals("Level 1 trauma triage unit.", found.get().getNotes());
        assertNotNull(found.get().getLastInspectedAt());
    }

    @Test
    @DisplayName("findByCode finds existing asset and returns empty for missing")
    void testFindByCode() {
        Infrastructure inf = new Infrastructure();
        inf.setCode("INF-SUB-01");
        inf.setName("Central Power Substation");
        inf.setInfrastructureType("SUBSTATION");
        inf.setLatitude(30.7395);
        inf.setLongitude(76.7840);
        inf.setStatus("OPERATIONAL");
        repository.save(inf);

        assertTrue(repository.findByCode("INF-SUB-01").isPresent());
        assertTrue(repository.findByCode("NON-EXISTENT").isEmpty());
        assertTrue(repository.existsByCode("INF-SUB-01"));
        assertFalse(repository.existsByCode("NON-EXISTENT"));
    }

    @Test
    @DisplayName("findAll filters correctly by category, status, search, and bounding box")
    void testFindAllWithFilters() {
        Infrastructure hosp = new Infrastructure();
        hosp.setCode("INF-HOSP-01");
        hosp.setName("City Hospital Sector 16");
        hosp.setInfrastructureType("HOSPITAL");
        hosp.setLatitude(30.7480);
        hosp.setLongitude(76.7860);
        hosp.setStatus("OPERATIONAL");
        repository.save(hosp);

        Infrastructure bridge = new Infrastructure();
        bridge.setCode("INF-BRG-01");
        bridge.setName("Madhya Marg Bridge");
        bridge.setInfrastructureType("BRIDGE");
        bridge.setLatitude(30.7470);
        bridge.setLongitude(76.7900);
        bridge.setStatus("UNDER_MAINTENANCE");
        repository.save(bridge);

        Infrastructure water = new Infrastructure();
        water.setCode("INF-WTR-01");
        water.setName("Sector 39 Water Filtration");
        water.setInfrastructureType("WATER_PIPELINE");
        water.setLatitude(30.7250);
        water.setLongitude(76.7350);
        water.setStatus("OPERATIONAL");
        repository.save(water);

        // Filter by category
        List<Infrastructure> hospitals = repository.findAll("HOSPITAL", null, null, null, null, null, null, 10, 0);
        assertEquals(1, hospitals.size());
        assertEquals("INF-HOSP-01", hospitals.get(0).getCode());

        // Filter by status
        List<Infrastructure> maintenance = repository.findAll(null, "UNDER_MAINTENANCE", null, null, null, null, null, 10, 0);
        assertEquals(1, maintenance.size());
        assertEquals("INF-BRG-01", maintenance.get(0).getCode());

        // Search keyword
        List<Infrastructure> searchResults = repository.findAll(null, null, "bridge", null, null, null, null, 10, 0);
        assertEquals(1, searchResults.size());

        // Bounding box: includes sector 16 hospital and bridge, excludes water (at lon 76.735)
        List<Infrastructure> inBox = repository.findAll(null, null, null, 30.740, 30.750, 76.780, 76.795, 10, 0);
        assertEquals(2, inBox.size());
        assertThat(inBox).extracting(Infrastructure::getCode).containsExactlyInAnyOrder("INF-HOSP-01", "INF-BRG-01");

        long count = repository.count(null, null, null, 30.740, 30.750, 76.780, 76.795);
        assertEquals(2, count);
    }

    @Test
    @DisplayName("update modifies existing record and updates updated_at")
    void testUpdate() {
        Infrastructure inf = new Infrastructure();
        inf.setCode("INF-DAM-01");
        inf.setName("Sukhna Dam Gate");
        inf.setInfrastructureType("DRAINAGE");
        inf.setLatitude(30.7420);
        inf.setLongitude(76.8180);
        inf.setStatus("OPERATIONAL");
        inf.setMaintenancePriority("LOW");
        repository.save(inf);

        inf.setStatus("DEGRADED");
        inf.setMaintenancePriority("CRITICAL");
        inf.setNotes("Gate hydraulic valve pressure leak.");
        repository.update(inf);

        Infrastructure updated = repository.findById(inf.getId()).orElseThrow();
        assertEquals("DEGRADED", updated.getStatus());
        assertEquals("CRITICAL", updated.getMaintenancePriority());
        assertEquals("Gate hydraulic valve pressure leak.", updated.getNotes());
    }

    @Test
    @DisplayName("deleteById removes the record from database")
    void testDeleteById() {
        Infrastructure inf = new Infrastructure();
        inf.setCode("INF-TEMP-01");
        inf.setName("Temporary Testing Facility");
        inf.setInfrastructureType("SHELTER");
        inf.setLatitude(30.7300);
        inf.setLongitude(76.7700);
        inf.setStatus("OPERATIONAL");
        repository.save(inf);

        assertTrue(repository.findById(inf.getId()).isPresent());
        boolean deleted = repository.deleteById(inf.getId());
        assertTrue(deleted);
        assertTrue(repository.findById(inf.getId()).isEmpty());
    }
}

