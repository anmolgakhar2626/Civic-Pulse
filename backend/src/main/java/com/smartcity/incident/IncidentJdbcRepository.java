package com.smartcity.incident;

import com.smartcity.department.Department;
import com.smartcity.department.Team;
import com.smartcity.priority.IncidentPriority;
import com.smartcity.priority.IncidentTier;
import com.smartcity.priority.PriorityTier;
import com.smartcity.priority.Severity;
import com.smartcity.user.Role;
import com.smartcity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring JdbcTemplate implementation of IncidentDao.
 * Uses explicit, parameterized SQL queries for all persistence operations,
 * ensuring high performance, SQL injection prevention, and zero JPA overhead.
 */
@Repository
@Primary
public class IncidentJdbcRepository implements IncidentDao {

    private static final Logger log = LoggerFactory.getLogger(IncidentJdbcRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public IncidentJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Incident> incidentRowMapper = (rs, rowNum) -> {
        Incident incident = new Incident();

        String idStr = rs.getString("id");
        if (idStr != null) {
            incident.setId(UUID.fromString(idStr));
        }

        incident.setIncidentNumber(rs.getString("incident_number"));
        incident.setTitle(rs.getString("title"));
        incident.setCategory(rs.getString("category"));
        incident.setIncidentType(rs.getString("incident_type"));
        incident.setDescription(rs.getString("description"));

        String sourceStr = rs.getString("source");
        incident.setSource(parseSource(sourceStr));

        incident.setLatitude(rs.getDouble("latitude"));
        incident.setLongitude(rs.getDouble("longitude"));
        incident.setLocationName(rs.getString("location_name"));
        incident.setAddress(rs.getString("address"));

        String statusStr = rs.getString("status");
        if (statusStr == null || statusStr.isBlank()) {
            statusStr = rs.getString("current_status");
        }
        incident.setStatus(parseStatus(statusStr));

        String sevStr = rs.getString("severity");
        if (sevStr == null || sevStr.isBlank()) {
            sevStr = rs.getString("severity_indicator");
        }
        incident.setSeverity(parseSeverity(sevStr));
        incident.setSeverityIndicator(sevStr);

        incident.setEscalationState(rs.getString("escalation_state"));
        incident.setEvidenceUrl(rs.getString("evidence_url"));
        incident.setReasonNotes(rs.getString("reason_notes"));
        incident.setResolutionNotes(rs.getString("resolution_notes"));

        Timestamp reportedTime = rs.getTimestamp("reported_time");
        if (reportedTime != null) {
            incident.setReportedAt(reportedTime.toInstant());
        }

        Timestamp detectionTime = rs.getTimestamp("detection_time");
        if (detectionTime != null) {
            incident.setDetectedAt(detectionTime.toInstant());
        }

        Timestamp resolutionTime = rs.getTimestamp("resolution_time");
        if (resolutionTime != null) {
            incident.setResolutionTime(resolutionTime.toInstant());
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            incident.setCreatedAt(createdAt.toInstant());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            incident.setUpdatedAt(updatedAt.toInstant());
        }

        // Priority engine fields
        incident.setPriorityLabel(rs.getString("priority_label"));
        incident.setPriorityReason(rs.getString("priority_reason"));
        incident.setSafetyImpact(rs.getString("safety_impact"));
        incident.setPriorityRuleId(rs.getString("priority_rule_id"));

        int slaMin = rs.getInt("sla_minutes");
        if (!rs.wasNull()) {
            incident.setSlaMinutes(slaMin);
        }

        Timestamp slaDeadline = rs.getTimestamp("sla_deadline");
        if (slaDeadline != null) {
            incident.setSlaDeadline(slaDeadline.toInstant());
        }

        // Assigned Department
        String deptIdStr = rs.getString("department_id");
        if (deptIdStr != null) {
            Department dept = new Department();
            dept.setId(UUID.fromString(deptIdStr));
            if (hasColumn(rs, "department_name")) {
                dept.setName(rs.getString("department_name"));
            }
            if (hasColumn(rs, "department_code")) {
                dept.setCode(rs.getString("department_code"));
            }
            incident.setAssignedDepartment(dept);
        }

        // Assigned Team
        String teamIdStr = rs.getString("assigned_team_id");
        if (teamIdStr != null) {
            Team team = new Team();
            team.setId(UUID.fromString(teamIdStr));
            if (hasColumn(rs, "team_name")) {
                team.setName(rs.getString("team_name"));
            }
            incident.setAssignedTeam(team);
        }

        // Reporter User
        String repIdStr = rs.getString("reported_by");
        if (repIdStr == null) {
            repIdStr = rs.getString("created_by_user_id");
        }
        if (repIdStr != null) {
            User repUser = new User();
            repUser.setId(UUID.fromString(repIdStr));
            if (hasColumn(rs, "reported_by_name")) {
                repUser.setFullName(rs.getString("reported_by_name"));
            }
            if (hasColumn(rs, "reported_by_email")) {
                repUser.setEmail(rs.getString("reported_by_email"));
            }
            if (hasColumn(rs, "reported_by_username")) {
                repUser.setUsername(rs.getString("reported_by_username"));
            }
            incident.setReportedBy(repUser);
            incident.setCreatedBy(repUser);
        }

        // Assigned To User
        String asgnIdStr = rs.getString("assigned_to");
        if (asgnIdStr != null) {
            User asgnUser = new User();
            asgnUser.setId(UUID.fromString(asgnIdStr));
            if (hasColumn(rs, "assigned_to_name")) {
                asgnUser.setFullName(rs.getString("assigned_to_name"));
            }
            if (hasColumn(rs, "assigned_to_email")) {
                asgnUser.setEmail(rs.getString("assigned_to_email"));
            }
            if (hasColumn(rs, "assigned_to_username")) {
                asgnUser.setUsername(rs.getString("assigned_to_username"));
            }
            incident.setAssignedTo(asgnUser);
        }

        return incident;
    };

    private static final String BASE_SELECT_SQL = """
        SELECT i.*,
               u_rep.full_name AS reported_by_name, u_rep.email AS reported_by_email, u_rep.username AS reported_by_username,
               u_asgn.full_name AS assigned_to_name, u_asgn.email AS assigned_to_email, u_asgn.username AS assigned_to_username,
               d.name AS department_name, d.code AS department_code,
               t.name AS team_name
        FROM incidents i
        LEFT JOIN users u_rep ON (i.reported_by = u_rep.id OR (i.reported_by IS NULL AND i.created_by_user_id = u_rep.id))
        LEFT JOIN users u_asgn ON i.assigned_to = u_asgn.id
        LEFT JOIN departments d ON i.department_id = d.id
        LEFT JOIN teams t ON i.assigned_team_id = t.id
        """;

    @Override
    public Incident save(Incident incident) {
        if (incident.getId() == null) {
            incident.setId(UUID.randomUUID());
        }
        if (incident.getIncidentNumber() == null || incident.getIncidentNumber().isBlank()) {
            incident.setIncidentNumber(generateIncidentNumber());
        }

        String title = incident.getTitle();
        String description = incident.getDescription();
        String category = incident.getCategory() != null ? incident.getCategory() : incident.getIncidentType();
        String incidentType = incident.getIncidentType() != null ? incident.getIncidentType() : category;
        String severity = incident.getSeverity() != null ? incident.getSeverity().name() :
                (incident.getSeverityIndicator() != null ? incident.getSeverityIndicator() : "MEDIUM");
        String status = incident.getStatus() != null ? incident.getStatus().name() : "OPEN";
        String locationName = incident.getLocationName() != null ? incident.getLocationName() : incident.getAddress();
        String address = incident.getAddress() != null ? incident.getAddress() : locationName;
        Double latitude = incident.getLatitude();
        Double longitude = incident.getLongitude();
        UUID reportedBy = incident.getReportedBy() != null ? incident.getReportedBy().getId() :
                (incident.getCreatedBy() != null ? incident.getCreatedBy().getId() : null);
        UUID assignedTo = incident.getAssignedTo() != null ? incident.getAssignedTo().getId() : null;
        UUID deptId = incident.getAssignedDepartment() != null ? incident.getAssignedDepartment().getId() : null;
        UUID teamId = incident.getAssignedTeam() != null ? incident.getAssignedTeam().getId() : null;
        String source = incident.getSource() != null ? incident.getSource().name() : "CITIZEN";
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp createdAt = incident.getCreatedAt() != null ? Timestamp.from(incident.getCreatedAt()) : now;
        Timestamp updatedAt = incident.getUpdatedAt() != null ? Timestamp.from(incident.getUpdatedAt()) : now;
        Timestamp reportedAt = incident.getReportedAt() != null ? Timestamp.from(incident.getReportedAt()) : now;
        Timestamp detectedAt = incident.getDetectedAt() != null ? Timestamp.from(incident.getDetectedAt()) : now;

        String insertSql = """
            INSERT INTO incidents (
                id, incident_number, title, description, category, incident_type,
                severity, severity_indicator, status, current_status,
                location_name, address, latitude, longitude,
                reported_by, created_by_user_id, assigned_to,
                department_id, assigned_team_id, source,
                reported_time, detection_time, created_at, updated_at,
                escalation_state
            ) VALUES (
                ?, ?, ?, ?, ?, ?,
                ?, ?, ?, ?,
                ?, ?, ?, ?,
                ?, ?, ?,
                ?, ?, ?,
                ?, ?, ?, ?,
                ?
            )
            """;

        log.debug("Executing parameterized INSERT for incident: {}", incident.getIncidentNumber());

        jdbcTemplate.update(
                insertSql,
                incident.getId(),
                incident.getIncidentNumber(),
                title,
                description,
                category,
                incidentType,
                severity,
                severity,
                status,
                status,
                locationName,
                address,
                latitude,
                longitude,
                reportedBy,
                reportedBy,
                assignedTo,
                deptId,
                teamId,
                source,
                reportedAt,
                detectedAt,
                createdAt,
                updatedAt,
                incident.getEscalationState() != null ? incident.getEscalationState() : "NONE"
        );

        return findById(incident.getId()).orElse(incident);
    }

    @Override
    public Optional<Incident> findById(UUID id) {
        String sql = BASE_SELECT_SQL + " WHERE i.id = ?";
        try {
            Incident incident = jdbcTemplate.queryForObject(sql, incidentRowMapper, id);
            return Optional.ofNullable(incident);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Incident> findByIncidentNumber(String incidentNumber) {
        String sql = BASE_SELECT_SQL + " WHERE i.incident_number = ?";
        try {
            Incident incident = jdbcTemplate.queryForObject(sql, incidentRowMapper, incidentNumber);
            return Optional.ofNullable(incident);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsById(UUID id) {
        String sql = "SELECT COUNT(1) FROM incidents WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public int update(UUID id, Incident incident) {
        String title = incident.getTitle();
        String description = incident.getDescription();
        String category = incident.getCategory();
        String incidentType = incident.getIncidentType() != null ? incident.getIncidentType() : category;
        String severity = incident.getSeverityIndicator() != null ? incident.getSeverityIndicator() :
                (incident.getSeverity() != null ? incident.getSeverity().name() : null);
        String locationName = incident.getLocationName();
        String address = incident.getAddress() != null ? incident.getAddress() : locationName;
        Double latitude = incident.getLatitude();
        Double longitude = incident.getLongitude();
        UUID assignedTo = incident.getAssignedTo() != null ? incident.getAssignedTo().getId() : null;
        UUID deptId = incident.getAssignedDepartment() != null ? incident.getAssignedDepartment().getId() : null;
        UUID teamId = incident.getAssignedTeam() != null ? incident.getAssignedTeam().getId() : null;

        String sql = """
            UPDATE incidents SET
                title = COALESCE(?, title),
                description = COALESCE(?, description),
                category = COALESCE(?, category),
                incident_type = COALESCE(?, incident_type),
                severity = COALESCE(?, severity),
                severity_indicator = COALESCE(?, severity_indicator),
                location_name = COALESCE(?, location_name),
                address = COALESCE(?, address),
                latitude = COALESCE(?, latitude),
                longitude = COALESCE(?, longitude),
                assigned_to = COALESCE(?, assigned_to),
                department_id = COALESCE(?, department_id),
                assigned_team_id = COALESCE(?, assigned_team_id),
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

        log.debug("Executing parameterized UPDATE for incident: {}", id);

        return jdbcTemplate.update(
                sql,
                title,
                description,
                category,
                incidentType,
                severity,
                severity,
                locationName,
                address,
                latitude,
                longitude,
                assignedTo,
                deptId,
                teamId,
                id
        );
    }

    @Override
    public int updateStatus(UUID id, String status, String remarks, String changedBy) {
        String sql = """
            UPDATE incidents SET
                status = ?,
                current_status = ?,
                reason_notes = COALESCE(?, reason_notes),
                resolution_time = CASE WHEN ? IN ('RESOLVED', 'CLOSED') AND resolution_time IS NULL THEN CURRENT_TIMESTAMP ELSE resolution_time END,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

        log.debug("Executing parameterized status UPDATE for incident {}: status={}", id, status);

        return jdbcTemplate.update(sql, status, status, remarks, status, id);
    }

    @Override
    public int deleteById(UUID id) {
        String sql = "DELETE FROM incidents WHERE id = ?";
        log.debug("Executing parameterized DELETE for incident: {}", id);
        return jdbcTemplate.update(sql, id);
    }

    @Override
    public List<Incident> findPaginatedAndFiltered(String search, String category, String severity, String status, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE_SELECT_SQL);
        sql.append(" WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        applyFilters(sql, params, search, category, severity, status);

        sql.append(" ORDER BY i.created_at DESC LIMIT ? OFFSET ? ");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), incidentRowMapper, params.toArray());
    }

    @Override
    public long countFiltered(String search, String category, String severity, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM incidents i WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        applyFilters(sql, params, search, category, severity, status);

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    @Override
    public List<Incident> findAll() {
        String sql = BASE_SELECT_SQL + " ORDER BY i.created_at DESC";
        return jdbcTemplate.query(sql, incidentRowMapper);
    }

    private void applyFilters(StringBuilder sql, List<Object> params, String search, String category, String severity, String status) {
        if (search != null && !search.trim().isEmpty()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            sql.append(" AND (LOWER(COALESCE(i.title, '')) LIKE ? OR LOWER(COALESCE(i.description, '')) LIKE ? OR LOWER(COALESCE(i.location_name, '')) LIKE ? OR LOWER(COALESCE(i.address, '')) LIKE ? OR LOWER(COALESCE(i.incident_number, '')) LIKE ?) ");
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (category != null && !category.trim().isEmpty()) {
            String cat = category.trim().toLowerCase();
            sql.append(" AND (LOWER(COALESCE(i.category, '')) = ? OR LOWER(COALESCE(i.incident_type, '')) = ?) ");
            params.add(cat);
            params.add(cat);
        }

        if (severity != null && !severity.trim().isEmpty()) {
            String sev = severity.trim().toUpperCase();
            sql.append(" AND (UPPER(COALESCE(i.severity, '')) = ? OR UPPER(COALESCE(i.severity_indicator, '')) = ? OR UPPER(COALESCE(i.priority, '')) = ?) ");
            params.add(sev);
            params.add(sev);
            params.add(sev);
        }

        if (status != null && !status.trim().isEmpty()) {
            String stat = status.trim().toUpperCase();
            sql.append(" AND (UPPER(COALESCE(i.status, '')) = ? OR UPPER(COALESCE(i.current_status, '')) = ?) ");
            params.add(stat);
            params.add(stat);
        }
    }

    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private IncidentStatus parseStatus(String val) {
        if (val == null || val.isBlank()) return IncidentStatus.OPEN;
        try {
            return IncidentStatus.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            if ("OPEN".equalsIgnoreCase(val)) return IncidentStatus.OPEN;
            return IncidentStatus.DETECTED;
        }
    }

    private IncidentSource parseSource(String val) {
        if (val == null || val.isBlank()) return IncidentSource.CITIZEN;
        try {
            return IncidentSource.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return IncidentSource.CITIZEN;
        }
    }

    private Severity parseSeverity(String val) {
        if (val == null || val.isBlank()) return Severity.MEDIUM;
        try {
            return Severity.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Severity.MEDIUM;
        }
    }

    private String generateIncidentNumber() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String shortId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "INC-" + datePrefix + "-" + shortId;
    }
}

