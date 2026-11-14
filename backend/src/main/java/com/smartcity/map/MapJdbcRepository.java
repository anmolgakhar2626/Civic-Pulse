package com.smartcity.map;

import com.smartcity.map.dto.MapIncidentMarkerResponse;
import com.smartcity.map.dto.MapInfrastructureResponse;
import com.smartcity.map.dto.MapShelterResponse;
import com.smartcity.map.dto.MapSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@Primary
public class MapJdbcRepository implements MapDao {

    private static final Logger log = LoggerFactory.getLogger(MapJdbcRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public MapJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<MapIncidentMarkerResponse> incidentMarkerRowMapper = (rs, rowNum) -> {
        MapIncidentMarkerResponse marker = new MapIncidentMarkerResponse();
        String idStr = rs.getString("id");
        if (idStr != null) {
            marker.setId(UUID.fromString(idStr));
        }
        marker.setIncidentNumber(rs.getString("incident_number"));
        marker.setTitle(rs.getString("title"));
        marker.setCategory(rs.getString("category"));
        marker.setSeverity(rs.getString("severity"));
        marker.setStatus(rs.getString("status"));
        marker.setLatitude(rs.getDouble("latitude"));
        marker.setLongitude(rs.getDouble("longitude"));
        marker.setLocationName(rs.getString("location_name"));
        marker.setAddress(rs.getString("address"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) marker.setCreatedAt(createdAt.toInstant());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) marker.setUpdatedAt(updatedAt.toInstant());

        String repBy = rs.getString("reported_by");
        if (repBy != null) marker.setReportedBy(UUID.fromString(repBy));

        String assTo = rs.getString("assigned_to");
        if (assTo != null) marker.setAssignedTo(UUID.fromString(assTo));

        return marker;
    };

    private final RowMapper<MapInfrastructureResponse> infrastructureRowMapper = (rs, rowNum) -> {
        MapInfrastructureResponse inf = new MapInfrastructureResponse();
        String idStr = rs.getString("id");
        if (idStr != null) inf.setId(UUID.fromString(idStr));

        inf.setCode(rs.getString("code"));
        inf.setName(rs.getString("name"));
        inf.setCategory(rs.getString("infrastructure_type"));
        inf.setStatus(rs.getString("status"));
        inf.setImportanceLevel(rs.getString("importance_level"));
        inf.setLatitude(rs.getDouble("latitude"));
        inf.setLongitude(rs.getDouble("longitude"));
        inf.setAddress(rs.getString("address"));
        inf.setMaintenancePriority(rs.getString("maintenance_priority"));
        inf.setNotes(rs.getString("notes"));

        Timestamp lastInsp = rs.getTimestamp("last_inspected_at");
        if (lastInsp != null) inf.setLastInspectedAt(lastInsp.toInstant());

        return inf;
    };

    private final RowMapper<MapShelterResponse> shelterRowMapper = (rs, rowNum) -> {
        MapShelterResponse shelter = new MapShelterResponse();
        String idStr = rs.getString("id");
        if (idStr != null) shelter.setId(UUID.fromString(idStr));

        shelter.setName(rs.getString("name"));
        shelter.setResourceType(rs.getString("resource_type"));
        shelter.setLatitude(rs.getDouble("latitude"));
        shelter.setLongitude(rs.getDouble("longitude"));
        shelter.setAddress(rs.getString("address"));
        shelter.setContactNumber(rs.getString("contact_number"));
        shelter.setAvailable(rs.getBoolean("available"));
        shelter.setCapacity(rs.getInt("capacity"));
        shelter.setOperationalHours(rs.getString("operational_hours"));
        shelter.setStatus(rs.getString("status"));

        return shelter;
    };

    @Override
    public List<MapIncidentMarkerResponse> findIncidents(
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng,
            String category,
            String severity,
            String status,
            Instant fromDate,
            Instant toDate,
            Integer limit
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                id, incident_number,
                COALESCE(title, incident_type, 'Smart City Incident') AS title,
                COALESCE(category, incident_type, 'General') AS category,
                COALESCE(severity, 'MEDIUM') AS severity,
                COALESCE(status, current_status, 'OPEN') AS status,
                latitude, longitude,
                COALESCE(location_name, address, 'Command Centre') AS location_name,
                address, created_at, updated_at, reported_by, assigned_to
            FROM incidents
            WHERE latitude IS NOT NULL AND longitude IS NOT NULL
        """);

        List<Object> params = new ArrayList<>();

        if (minLat != null && maxLat != null && minLng != null && maxLng != null) {
            sql.append(" AND latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?");
            params.add(Math.min(minLat, maxLat));
            params.add(Math.max(minLat, maxLat));
            params.add(Math.min(minLng, maxLng));
            params.add(Math.max(minLng, maxLng));
        }

        if (category != null && !category.isBlank()) {
            sql.append(" AND LOWER(COALESCE(category, incident_type)) = LOWER(?)");
            params.add(category.trim());
        }

        if (severity != null && !severity.isBlank()) {
            sql.append(" AND UPPER(COALESCE(severity, 'MEDIUM')) = UPPER(?)");
            params.add(severity.trim());
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND UPPER(COALESCE(status, current_status, 'OPEN')) = UPPER(?)");
            params.add(status.trim());
        }

        if (fromDate != null) {
            sql.append(" AND created_at >= ?");
            params.add(Timestamp.from(fromDate));
        }

        if (toDate != null) {
            sql.append(" AND created_at <= ?");
            params.add(Timestamp.from(toDate));
        }

        sql.append(" ORDER BY created_at DESC LIMIT ?");
        int safeLimit = limit != null ? Math.max(1, Math.min(limit, 1000)) : 200;
        params.add(safeLimit);

        return jdbcTemplate.query(sql.toString(), incidentMarkerRowMapper, params.toArray());
    }

    @Override
    public List<MapInfrastructureResponse> findInfrastructure(
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng,
            String category,
            String status,
            Integer limit
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                id, code, name, infrastructure_type, status, importance_level,
                latitude, longitude, address, maintenance_priority, last_inspected_at, notes
            FROM infrastructure
            WHERE latitude IS NOT NULL AND longitude IS NOT NULL
        """);

        List<Object> params = new ArrayList<>();

        if (minLat != null && maxLat != null && minLng != null && maxLng != null) {
            sql.append(" AND latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?");
            params.add(Math.min(minLat, maxLat));
            params.add(Math.max(minLat, maxLat));
            params.add(Math.min(minLng, maxLng));
            params.add(Math.max(minLng, maxLng));
        }

        if (category != null && !category.isBlank()) {
            sql.append(" AND LOWER(infrastructure_type) = LOWER(?)");
            params.add(category.trim());
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND UPPER(status) = UPPER(?)");
            params.add(status.trim());
        }

        sql.append(" ORDER BY name ASC LIMIT ?");
        int safeLimit = limit != null ? Math.max(1, Math.min(limit, 500)) : 100;
        params.add(safeLimit);

        return jdbcTemplate.query(sql.toString(), infrastructureRowMapper, params.toArray());
    }

    @Override
    public List<MapShelterResponse> findShelters(
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng,
            Boolean availableOnly,
            Integer limit
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                id, name, resource_type, latitude, longitude, address,
                contact_number, available, capacity, operational_hours, 'OPERATIONAL' AS status
            FROM emergency_resources
            WHERE latitude IS NOT NULL AND longitude IS NOT NULL
              AND (resource_type = 'EMERGENCY_SHELTER' OR resource_type = 'SHELTER')
        """);

        List<Object> params = new ArrayList<>();

        if (minLat != null && maxLat != null && minLng != null && maxLng != null) {
            sql.append(" AND latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?");
            params.add(Math.min(minLat, maxLat));
            params.add(Math.max(minLat, maxLat));
            params.add(Math.min(minLng, maxLng));
            params.add(Math.max(minLng, maxLng));
        }

        if (Boolean.TRUE.equals(availableOnly)) {
            sql.append(" AND available = true");
        }

        sql.append("""
            UNION ALL
            SELECT
                id, name, 'EMERGENCY_SHELTER' AS resource_type, latitude, longitude, address,
                NULL AS contact_number, (status = 'OPERATIONAL') AS available, 250 AS capacity, '24/7' AS operational_hours, status
            FROM infrastructure
            WHERE latitude IS NOT NULL AND longitude IS NOT NULL
              AND infrastructure_type = 'SHELTER'
        """);

        if (minLat != null && maxLat != null && minLng != null && maxLng != null) {
            sql.append(" AND latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?");
            params.add(Math.min(minLat, maxLat));
            params.add(Math.max(minLat, maxLat));
            params.add(Math.min(minLng, maxLng));
            params.add(Math.max(minLng, maxLng));
        }

        if (Boolean.TRUE.equals(availableOnly)) {
            sql.append(" AND status = 'OPERATIONAL'");
        }

        sql.append(" ORDER BY name ASC LIMIT ?");
        int safeLimit = limit != null ? Math.max(1, Math.min(limit, 200)) : 50;
        params.add(safeLimit);

        return jdbcTemplate.query(sql.toString(), shelterRowMapper, params.toArray());
    }

    @Override
    public MapSummaryResponse getSummary() {
        MapSummaryResponse summary = new MapSummaryResponse();

        try {
            Long totalInc = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM incidents WHERE latitude IS NOT NULL AND longitude IS NOT NULL", Long.class);
            summary.setTotalIncidents(totalInc != null ? totalInc : 0L);

            Long activeInc = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM incidents WHERE status IN ('OPEN', 'IN_PROGRESS') OR current_status IN ('OPEN', 'IN_PROGRESS', 'DETECTED', 'ASSIGNED')", Long.class);
            summary.setActiveIncidents(activeInc != null ? activeInc : 0L);

            Long critInc = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM incidents WHERE (UPPER(severity) = 'CRITICAL' OR UPPER(severity_indicator) = 'CRITICAL') AND (status IN ('OPEN', 'IN_PROGRESS') OR current_status IN ('OPEN', 'IN_PROGRESS'))", Long.class);
            summary.setCriticalIncidents(critInc != null ? critInc : 0L);

            Long resInc = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM incidents WHERE status IN ('RESOLVED', 'CLOSED') OR current_status IN ('RESOLVED', 'CLOSED')", Long.class);
            summary.setResolvedIncidents(resInc != null ? resInc : 0L);
        } catch (Exception e) {
            log.warn("Error calculating incident map summary: {}", e.getMessage());
        }

        try {
            Long totalInf = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM infrastructure", Long.class);
            summary.setTotalInfrastructure(totalInf != null ? totalInf : 0L);

            Long opInf = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM infrastructure WHERE status = 'OPERATIONAL'", Long.class);
            summary.setOperationalInfrastructure(opInf != null ? opInf : 0L);

            Long degInf = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM infrastructure WHERE status IN ('DEGRADED', 'OFFLINE')", Long.class);
            summary.setDegradedInfrastructure(degInf != null ? degInf : 0L);

            Long maintInf = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM infrastructure WHERE status = 'UNDER_MAINTENANCE'", Long.class);
            summary.setMaintenanceInfrastructure(maintInf != null ? maintInf : 0L);
        } catch (Exception e) {
            log.warn("Error calculating infrastructure map summary: {}", e.getMessage());
        }

        try {
            Long totalShelters = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM emergency_resources WHERE resource_type IN ('EMERGENCY_SHELTER', 'SHELTER')", Long.class);
            Long availShelters = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM emergency_resources WHERE resource_type IN ('EMERGENCY_SHELTER', 'SHELTER') AND available = true", Long.class);
            Long totalCap = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(capacity), 0) FROM emergency_resources WHERE resource_type IN ('EMERGENCY_SHELTER', 'SHELTER')", Long.class);

            summary.setTotalShelters(totalShelters != null ? totalShelters : 0L);
            summary.setAvailableShelters(availShelters != null ? availShelters : 0L);
            summary.setTotalShelterCapacity(totalCap != null ? totalCap : 0L);
        } catch (Exception e) {
            log.warn("Error calculating shelter map summary: {}", e.getMessage());
        }

        try {
            Long activeSensors = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sensors_gis WHERE status = 'ONLINE'", Long.class);
            summary.setActiveSensors(activeSensors != null ? activeSensors : 0L);
        } catch (Exception e) {
            summary.setActiveSensors(0L);
        }

        summary.setLastUpdated(Instant.now());
        return summary;
    }
}

