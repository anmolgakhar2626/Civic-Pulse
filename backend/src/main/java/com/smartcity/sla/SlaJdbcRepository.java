package com.smartcity.sla;

import com.smartcity.incident.Incident;
import com.smartcity.priority.PriorityTier;
import com.smartcity.sla.dto.SlaEscalationResponse;
import com.smartcity.sla.dto.SlaOverviewResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring JdbcTemplate implementation of SlaDao.
 * Executes parameterized SQL for all SLA policies, incident tracking,
 * and escalation audits.
 */
@Repository
@Primary
public class SlaJdbcRepository implements SlaDao {

    private static final Logger log = LoggerFactory.getLogger(SlaJdbcRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public SlaJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // === ROW MAPPERS ===

    private final RowMapper<SLAPolicy> policyRowMapper = (rs, rowNum) -> {
        SLAPolicy policy = new SLAPolicy();
        String idStr = rs.getString("id");
        if (idStr != null) {
            policy.setId(UUID.fromString(idStr));
        }
        policy.setCategory(rs.getString("category"));
        policy.setSeverity(rs.getString("severity"));
        policy.setResponseTimeMinutes(rs.getInt("response_time_minutes"));
        policy.setResolutionTimeMinutes(rs.getInt("resolution_time_minutes"));
        policy.setEnabled(rs.getBoolean("enabled"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) policy.setCreatedAt(createdAt.toInstant());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) policy.setUpdatedAt(updatedAt.toInstant());

        return policy;
    };

    private final RowMapper<SLARecord> recordRowMapper = (rs, rowNum) -> {
        SLARecord record = new SLARecord();

        String idStr = rs.getString("id");
        if (idStr != null) {
            record.setId(UUID.fromString(idStr));
        }

        String incidentIdStr = rs.getString("incident_id");
        if (incidentIdStr != null) {
            Incident inc = new Incident();
            inc.setId(UUID.fromString(incidentIdStr));
            // Check if incident table columns joined
            if (hasColumn(rs, "incident_number")) {
                inc.setIncidentNumber(rs.getString("incident_number"));
            }
            if (hasColumn(rs, "title")) {
                inc.setTitle(rs.getString("title"));
            }
            if (hasColumn(rs, "incident_category")) {
                inc.setCategory(rs.getString("incident_category"));
            } else if (hasColumn(rs, "category")) {
                inc.setCategory(rs.getString("category"));
            }
            if (hasColumn(rs, "incident_severity")) {
                inc.setSeverityIndicator(rs.getString("incident_severity"));
            } else if (hasColumn(rs, "severity")) {
                inc.setSeverityIndicator(rs.getString("severity"));
            }
            record.setIncident(inc);
        }

        String policyIdStr = rs.getString("policy_id");
        if (policyIdStr != null) {
            SLAPolicy p = new SLAPolicy();
            p.setId(UUID.fromString(policyIdStr));
            record.setPolicy(p);
        }

        String tierStr = rs.getString("priority_tier");
        if (tierStr != null) {
            record.setPriorityTier(PriorityTier.fromString(tierStr));
        }

        int targetMin = rs.getInt("target_minutes");
        if (!rs.wasNull()) {
            record.setTargetMinutes(targetMin);
        } else {
            int targetSla = rs.getInt("target_sla_minutes");
            if (!rs.wasNull()) record.setTargetMinutes(targetSla);
        }

        Timestamp started = rs.getTimestamp("started_at");
        if (started != null) record.setStartedAt(started.toInstant());

        Timestamp deadline = rs.getTimestamp("deadline_at");
        if (deadline != null) record.setDeadlineAt(deadline.toInstant());

        Timestamp respDeadline = rs.getTimestamp("response_deadline");
        if (respDeadline != null) record.setResponseDeadline(respDeadline.toInstant());

        Timestamp resDeadline = rs.getTimestamp("resolution_deadline");
        if (resDeadline != null) {
            record.setResolutionDeadline(resDeadline.toInstant());
        } else if (deadline != null) {
            record.setResolutionDeadline(deadline.toInstant());
        }

        Timestamp firstResp = rs.getTimestamp("first_responded_at");
        if (firstResp != null) {
            record.setFirstRespondedAt(firstResp.toInstant());
            record.setAcknowledgedAt(firstResp.toInstant());
        } else {
            Timestamp ack = rs.getTimestamp("acknowledged_at");
            if (ack != null) {
                record.setFirstRespondedAt(ack.toInstant());
                record.setAcknowledgedAt(ack.toInstant());
            }
        }

        Timestamp resolved = rs.getTimestamp("resolved_at");
        if (resolved != null) record.setResolvedAt(resolved.toInstant());

        String statusStr = rs.getString("status");
        if (statusStr == null) statusStr = rs.getString("current_sla_status");
        record.setStatus(SLAStatus.fromString(statusStr));

        long remMin = rs.getLong("remaining_minutes");
        if (!rs.wasNull()) {
            record.setRemainingMinutes(remMin);
        } else {
            long remSec = rs.getLong("remaining_seconds");
            if (!rs.wasNull()) record.setRemainingMinutes(remSec / 60);
        }

        String escStr = rs.getString("escalation_level");
        if (escStr != null) {
            try {
                record.setEscalationLevel(EscalationLevel.valueOf(escStr.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                record.setEscalationLevel(EscalationLevel.NONE);
            }
        }

        Timestamp lastEsc = rs.getTimestamp("last_escalated_at");
        if (lastEsc != null) record.setLastEscalatedAt(lastEsc.toInstant());

        record.setBreached(rs.getBoolean("breached"));
        Timestamp breachedAt = rs.getTimestamp("breached_at");
        if (breachedAt != null) record.setBreachedAt(breachedAt.toInstant());

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) record.setCreatedAt(createdAt.toInstant());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) record.setUpdatedAt(updatedAt.toInstant());

        return record;
    };

    private static boolean hasColumn(ResultSet rs, String columnName) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    // === POLICY MANAGEMENT ===

    @Override
    public SLAPolicy savePolicy(SLAPolicy policy) {
        if (policy.getId() == null) {
            policy.setId(UUID.randomUUID());
        }
        Instant now = Instant.now();
        if (policy.getCreatedAt() == null) policy.setCreatedAt(now);
        if (policy.getUpdatedAt() == null) policy.setUpdatedAt(now);
        if (policy.getEnabled() == null) policy.setEnabled(true);

        Optional<SLAPolicy> existing = findPolicyByCategoryAndSeverity(policy.getCategory(), policy.getSeverity());
        if (existing.isPresent()) {
            policy.setId(existing.get().getId());
            updatePolicy(policy.getId(), policy);
            return policy;
        }

        String sql = """
            INSERT INTO sla_policies (id, category, severity, response_time_minutes, resolution_time_minutes, enabled, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        jdbcTemplate.update(
                sql,
                policy.getId(),
                policy.getCategory(),
                policy.getSeverity(),
                policy.getResponseTimeMinutes(),
                policy.getResolutionTimeMinutes(),
                policy.getEnabled(),
                Timestamp.from(policy.getCreatedAt()),
                Timestamp.from(policy.getUpdatedAt())
        );

        return policy;
    }

    @Override
    public int updatePolicy(UUID id, SLAPolicy policy) {
        StringBuilder sql = new StringBuilder("UPDATE sla_policies SET updated_at = ?");
        List<Object> params = new ArrayList<>();
        params.add(Timestamp.from(Instant.now()));

        if (policy.getCategory() != null) {
            sql.append(", category = ?");
            params.add(policy.getCategory());
        }
        if (policy.getSeverity() != null) {
            sql.append(", severity = ?");
            params.add(policy.getSeverity());
        }
        if (policy.getResponseTimeMinutes() != null) {
            sql.append(", response_time_minutes = ?");
            params.add(policy.getResponseTimeMinutes());
        }
        if (policy.getResolutionTimeMinutes() != null) {
            sql.append(", resolution_time_minutes = ?");
            params.add(policy.getResolutionTimeMinutes());
        }
        if (policy.getEnabled() != null) {
            sql.append(", enabled = ?");
            params.add(policy.getEnabled());
        }

        sql.append(" WHERE id = ?");
        params.add(id);

        return jdbcTemplate.update(sql.toString(), params.toArray());
    }

    @Override
    public Optional<SLAPolicy> findPolicyById(UUID id) {
        String sql = "SELECT * FROM sla_policies WHERE id = ?";
        try {
            SLAPolicy policy = jdbcTemplate.queryForObject(sql, policyRowMapper, id);
            return Optional.ofNullable(policy);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<SLAPolicy> findPolicyByCategoryAndSeverity(String category, String severity) {
        if (severity == null) return Optional.empty();

        // 1. Exact match on category and severity
        if (category != null && !category.isBlank()) {
            String sql = "SELECT * FROM sla_policies WHERE LOWER(category) = LOWER(?) AND UPPER(severity) = UPPER(?) AND enabled = true LIMIT 1";
            try {
                SLAPolicy policy = jdbcTemplate.queryForObject(sql, policyRowMapper, category.trim(), severity.trim());
                return Optional.ofNullable(policy);
            } catch (EmptyResultDataAccessException ignored) {
            }
        }

        // 2. Fallback to DEFAULT policy for severity
        String fallbackSql = "SELECT * FROM sla_policies WHERE category = 'DEFAULT' AND UPPER(severity) = UPPER(?) AND enabled = true LIMIT 1";
        try {
            SLAPolicy policy = jdbcTemplate.queryForObject(fallbackSql, policyRowMapper, severity.trim());
            return Optional.ofNullable(policy);
        } catch (EmptyResultDataAccessException ignored) {
        }

        // 3. Fallback to any enabled policy for severity
        String anySql = "SELECT * FROM sla_policies WHERE UPPER(severity) = UPPER(?) AND enabled = true ORDER BY created_at ASC LIMIT 1";
        try {
            SLAPolicy policy = jdbcTemplate.queryForObject(anySql, policyRowMapper, severity.trim());
            return Optional.ofNullable(policy);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<SLAPolicy> findAllPolicies() {
        String sql = "SELECT * FROM sla_policies ORDER BY category ASC, CASE severity WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 ELSE 4 END ASC";
        return jdbcTemplate.query(sql, policyRowMapper);
    }

    // === SLA RECORD TRACKING ===

    private static final String BASE_SELECT_RECORD_SQL = """
        SELECT s.*, 
               i.incident_number, i.title, i.category AS incident_category, i.severity AS incident_severity
        FROM sla_records s
        LEFT JOIN incidents i ON s.incident_id = i.id
    """;

    @Override
    public SLARecord saveRecord(SLARecord record) {
        if (record.getId() == null) {
            record.setId(UUID.randomUUID());
        }
        Instant now = Instant.now();
        if (record.getCreatedAt() == null) record.setCreatedAt(now);
        if (record.getUpdatedAt() == null) record.setUpdatedAt(now);
        if (record.getStartedAt() == null) record.setStartedAt(now);
        if (record.getStatus() == null) record.setStatus(SLAStatus.ON_TRACK);
        if (record.getEscalationLevel() == null) record.setEscalationLevel(EscalationLevel.NONE);
        if (record.getBreached() == null) record.setBreached(false);

        Optional<SLARecord> existing = findRecordByIncidentId(record.getIncidentId());
        if (existing.isPresent()) {
            record.setId(existing.get().getId());
            updateRecord(record);
            return record;
        }

        String sql = """
            INSERT INTO sla_records (
                id, incident_id, policy_id, priority_tier, target_minutes, target_sla_minutes,
                started_at, deadline_at, response_deadline, resolution_deadline,
                first_responded_at, acknowledged_at, resolved_at,
                status, current_sla_status, remaining_minutes, remaining_seconds,
                escalation_level, last_escalated_at, breached, breached_at,
                created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        jdbcTemplate.update(
                sql,
                record.getId(),
                record.getIncidentId(),
                record.getPolicy() != null ? record.getPolicy().getId() : null,
                record.getPriorityTier() != null ? record.getPriorityTier().name() : null,
                record.getTargetMinutes(),
                record.getTargetMinutes(),
                Timestamp.from(record.getStartedAt()),
                record.getDeadlineAt() != null ? Timestamp.from(record.getDeadlineAt()) : null,
                record.getResponseDeadline() != null ? Timestamp.from(record.getResponseDeadline()) : null,
                record.getResolutionDeadline() != null ? Timestamp.from(record.getResolutionDeadline()) : null,
                record.getFirstRespondedAt() != null ? Timestamp.from(record.getFirstRespondedAt()) : null,
                record.getAcknowledgedAt() != null ? Timestamp.from(record.getAcknowledgedAt()) : null,
                record.getResolvedAt() != null ? Timestamp.from(record.getResolvedAt()) : null,
                record.getStatus() != null ? record.getStatus().name() : "ON_TRACK",
                record.getStatus() != null ? record.getStatus().name() : "ON_TRACK",
                record.getRemainingMinutes(),
                record.getRemainingMinutes() != null ? record.getRemainingMinutes() * 60 : null,
                record.getEscalationLevel() != null ? record.getEscalationLevel().name() : "NONE",
                record.getLastEscalatedAt() != null ? Timestamp.from(record.getLastEscalatedAt()) : null,
                record.getBreached() != null ? record.getBreached() : false,
                record.getBreachedAt() != null ? Timestamp.from(record.getBreachedAt()) : null,
                Timestamp.from(record.getCreatedAt()),
                Timestamp.from(record.getUpdatedAt())
        );

        return record;
    }

    @Override
    public int updateRecord(SLARecord record) {
        String sql = """
            UPDATE sla_records SET
                policy_id = ?,
                priority_tier = ?,
                target_minutes = ?,
                target_sla_minutes = ?,
                deadline_at = ?,
                response_deadline = ?,
                resolution_deadline = ?,
                first_responded_at = ?,
                acknowledged_at = ?,
                resolved_at = ?,
                status = ?,
                current_sla_status = ?,
                remaining_minutes = ?,
                remaining_seconds = ?,
                escalation_level = ?,
                last_escalated_at = ?,
                breached = ?,
                breached_at = ?,
                updated_at = ?
            WHERE id = ?
        """;

        return jdbcTemplate.update(
                sql,
                record.getPolicy() != null ? record.getPolicy().getId() : null,
                record.getPriorityTier() != null ? record.getPriorityTier().name() : null,
                record.getTargetMinutes(),
                record.getTargetMinutes(),
                record.getDeadlineAt() != null ? Timestamp.from(record.getDeadlineAt()) : null,
                record.getResponseDeadline() != null ? Timestamp.from(record.getResponseDeadline()) : null,
                record.getResolutionDeadline() != null ? Timestamp.from(record.getResolutionDeadline()) : null,
                record.getFirstRespondedAt() != null ? Timestamp.from(record.getFirstRespondedAt()) : null,
                record.getAcknowledgedAt() != null ? Timestamp.from(record.getAcknowledgedAt()) : null,
                record.getResolvedAt() != null ? Timestamp.from(record.getResolvedAt()) : null,
                record.getStatus() != null ? record.getStatus().name() : "ON_TRACK",
                record.getStatus() != null ? record.getStatus().name() : "ON_TRACK",
                record.getRemainingMinutes(),
                record.getRemainingMinutes() != null ? record.getRemainingMinutes() * 60 : null,
                record.getEscalationLevel() != null ? record.getEscalationLevel().name() : "NONE",
                record.getLastEscalatedAt() != null ? Timestamp.from(record.getLastEscalatedAt()) : null,
                record.getBreached() != null ? record.getBreached() : false,
                record.getBreachedAt() != null ? Timestamp.from(record.getBreachedAt()) : null,
                Timestamp.from(Instant.now()),
                record.getId()
        );
    }

    @Override
    public Optional<SLARecord> findRecordById(UUID id) {
        String sql = BASE_SELECT_RECORD_SQL + " WHERE s.id = ?";
        try {
            SLARecord record = jdbcTemplate.queryForObject(sql, recordRowMapper, id);
            return Optional.ofNullable(record);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<SLARecord> findRecordByIncidentId(UUID incidentId) {
        String sql = BASE_SELECT_RECORD_SQL + " WHERE s.incident_id = ?";
        try {
            SLARecord record = jdbcTemplate.queryForObject(sql, recordRowMapper, incidentId);
            return Optional.ofNullable(record);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<SLARecord> findAllActiveRecords() {
        String sql = BASE_SELECT_RECORD_SQL + " WHERE UPPER(COALESCE(s.status, s.current_sla_status, '')) NOT IN ('COMPLETED', 'RESOLVED') AND s.resolved_at IS NULL ORDER BY s.deadline_at ASC";
        return jdbcTemplate.query(sql, recordRowMapper);
    }

    @Override
    public List<SLARecord> findRecordsByStatus(SLAStatus status) {
        if (status == SLAStatus.BREACHED) {
            String sql = BASE_SELECT_RECORD_SQL + """
                WHERE (s.breached = TRUE 
                       OR UPPER(COALESCE(s.status, '')) = 'BREACHED' 
                       OR UPPER(COALESCE(s.current_sla_status, '')) = 'BREACHED'
                       OR (s.deadline_at < CURRENT_TIMESTAMP AND s.resolved_at IS NULL))
                  AND UPPER(COALESCE(s.status, s.current_sla_status, '')) NOT IN ('COMPLETED', 'RESOLVED')
                  AND s.resolved_at IS NULL
                ORDER BY s.deadline_at ASC
            """;
            return jdbcTemplate.query(sql, recordRowMapper);
        }
        if (status == SLAStatus.WARNING || status == SLAStatus.AT_RISK) {
            String sql = BASE_SELECT_RECORD_SQL + " WHERE (s.breached IS NOT TRUE) AND (s.status IN ('WARNING', 'AT_RISK') OR s.current_sla_status IN ('WARNING', 'AT_RISK')) AND (s.deadline_at IS NULL OR s.deadline_at >= CURRENT_TIMESTAMP) AND s.resolved_at IS NULL ORDER BY s.deadline_at ASC";
            return jdbcTemplate.query(sql, recordRowMapper);
        }
        if (status == SLAStatus.COMPLETED || status == SLAStatus.RESOLVED) {
            String sql = BASE_SELECT_RECORD_SQL + " WHERE s.status IN ('COMPLETED', 'RESOLVED') OR s.current_sla_status IN ('COMPLETED', 'RESOLVED') OR s.resolved_at IS NOT NULL ORDER BY s.updated_at DESC";
            return jdbcTemplate.query(sql, recordRowMapper);
        }
        String sql = BASE_SELECT_RECORD_SQL + " WHERE s.status = ? ORDER BY s.deadline_at ASC";
        return jdbcTemplate.query(sql, recordRowMapper, status.name());
    }

    @Override
    public int recordFirstResponse(UUID incidentId, Instant respondedAt) {
        Instant ts = respondedAt != null ? respondedAt : Instant.now();
        String sql = """
            UPDATE sla_records
            SET first_responded_at = COALESCE(first_responded_at, ?),
                acknowledged_at = COALESCE(acknowledged_at, ?),
                updated_at = ?
            WHERE incident_id = ?
        """;
        return jdbcTemplate.update(sql, Timestamp.from(ts), Timestamp.from(ts), Timestamp.from(Instant.now()), incidentId);
    }

    @Override
    public int recordResolution(UUID incidentId, Instant resolvedAt) {
        Instant ts = resolvedAt != null ? resolvedAt : Instant.now();
        String sql = """
            UPDATE sla_records
            SET resolved_at = COALESCE(resolved_at, ?),
                status = 'COMPLETED',
                current_sla_status = 'COMPLETED',
                escalation_level = 'NONE',
                remaining_minutes = 0,
                remaining_seconds = 0,
                updated_at = ?
            WHERE incident_id = ?
        """;
        return jdbcTemplate.update(sql, Timestamp.from(ts), Timestamp.from(Instant.now()), incidentId);
    }

    @Override
    public int updateRecordStatusAndEscalation(UUID id, SLAStatus status, Long remainingMinutes, EscalationLevel level, Boolean breached, Instant breachedAt, Instant lastEscalatedAt) {
        String sql = """
            UPDATE sla_records
            SET status = ?,
                current_sla_status = ?,
                remaining_minutes = ?,
                remaining_seconds = ?,
                escalation_level = ?,
                breached = ?,
                breached_at = ?,
                last_escalated_at = COALESCE(?, last_escalated_at),
                updated_at = ?
            WHERE id = ?
        """;
        return jdbcTemplate.update(
                sql,
                status.name(),
                status.name(),
                remainingMinutes,
                remainingMinutes != null ? remainingMinutes * 60 : null,
                level != null ? level.name() : "NONE",
                breached != null ? breached : false,
                breachedAt != null ? Timestamp.from(breachedAt) : null,
                lastEscalatedAt != null ? Timestamp.from(lastEscalatedAt) : null,
                Timestamp.from(Instant.now()),
                id
        );
    }

    // === ESCALATION AUDIT & HISTORY ===

    @Override
    public void recordEscalation(UUID incidentId, UUID slaRecordId, String prevEscalation, String newEscalation, String prevStatus, String newStatus, String reason, Instant escalatedAt) {
        String sql = """
            INSERT INTO sla_escalation_history (id, incident_id, sla_record_id, previous_escalation_level, new_escalation_level, previous_status, new_status, reason, escalated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(
                sql,
                UUID.randomUUID(),
                incidentId,
                slaRecordId,
                prevEscalation,
                newEscalation,
                prevStatus,
                newStatus,
                reason,
                Timestamp.from(escalatedAt != null ? escalatedAt : Instant.now())
        );
    }

    @Override
    public boolean hasRecentEscalation(UUID incidentId, String newEscalationLevel) {
        String sql = """
            SELECT COUNT(*) FROM sla_escalation_history 
            WHERE incident_id = ? AND new_escalation_level = ?
        """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, incidentId, newEscalationLevel);
        return count != null && count > 0;
    }

    @Override
    public List<SlaEscalationResponse> getEscalationHistory(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        String sql = """
            SELECT h.*, i.incident_number, i.title AS incident_title
            FROM sla_escalation_history h
            LEFT JOIN incidents i ON h.incident_id = i.id
            ORDER BY h.escalated_at DESC
            LIMIT ?
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            SlaEscalationResponse resp = new SlaEscalationResponse();
            String idStr = rs.getString("id");
            if (idStr != null) resp.setId(UUID.fromString(idStr));

            String incIdStr = rs.getString("incident_id");
            if (incIdStr != null) resp.setIncidentId(UUID.fromString(incIdStr));

            resp.setIncidentNumber(rs.getString("incident_number"));
            resp.setIncidentTitle(rs.getString("incident_title"));

            String slaIdStr = rs.getString("sla_record_id");
            if (slaIdStr != null) resp.setSlaRecordId(UUID.fromString(slaIdStr));

            resp.setPreviousEscalationLevel(rs.getString("previous_escalation_level"));
            resp.setNewEscalationLevel(rs.getString("new_escalation_level"));
            resp.setPreviousStatus(rs.getString("previous_status"));
            resp.setNewStatus(rs.getString("new_status"));
            resp.setReason(rs.getString("reason"));

            Timestamp escAt = rs.getTimestamp("escalated_at");
            if (escAt != null) resp.setEscalatedAt(escAt.toInstant());

            return resp;
        }, safeLimit);
    }

    // === INCIDENT METADATA SYNC ===

    @Override
    public int updateIncidentEscalationState(UUID incidentId, String escalationState) {
        String sql = "UPDATE incidents SET escalation_state = ?, updated_at = ? WHERE id = ?";
        return jdbcTemplate.update(sql, escalationState, Timestamp.from(Instant.now()), incidentId);
    }

    @Override
    public int updateIncidentSlaDeadline(UUID incidentId, Integer slaMinutes, Instant slaDeadline) {
        String sql = "UPDATE incidents SET sla_minutes = ?, sla_deadline = ?, updated_at = ? WHERE id = ?";
        return jdbcTemplate.update(sql, slaMinutes, slaDeadline != null ? Timestamp.from(slaDeadline) : null, Timestamp.from(Instant.now()), incidentId);
    }

    // === OVERVIEW METRICS ===

    @Override
    public SlaOverviewResponse getOverviewMetrics() {
        String sql = """
            SELECT 
                COUNT(*) FILTER (
                    WHERE UPPER(COALESCE(status, current_sla_status, '')) NOT IN ('COMPLETED', 'RESOLVED')
                      AND resolved_at IS NULL
                ) AS total_active,
                COUNT(*) FILTER (
                    WHERE (UPPER(COALESCE(status, current_sla_status, '')) = 'ON_TRACK')
                      AND (breached IS NOT TRUE)
                      AND (deadline_at IS NULL OR deadline_at >= CURRENT_TIMESTAMP)
                      AND resolved_at IS NULL
                ) AS on_track_count,
                COUNT(*) FILTER (
                    WHERE (UPPER(COALESCE(status, current_sla_status, '')) IN ('WARNING', 'AT_RISK'))
                      AND (breached IS NOT TRUE)
                      AND (deadline_at IS NULL OR deadline_at >= CURRENT_TIMESTAMP)
                      AND resolved_at IS NULL
                ) AS warning_count,
                COUNT(*) FILTER (
                    WHERE (breached = TRUE 
                           OR UPPER(COALESCE(status, '')) = 'BREACHED' 
                           OR UPPER(COALESCE(current_sla_status, '')) = 'BREACHED'
                           OR (deadline_at < CURRENT_TIMESTAMP AND resolved_at IS NULL))
                      AND UPPER(COALESCE(status, current_sla_status, '')) NOT IN ('COMPLETED', 'RESOLVED')
                      AND resolved_at IS NULL
                ) AS breached_count,
                COUNT(*) FILTER (
                    WHERE UPPER(COALESCE(status, current_sla_status, '')) IN ('COMPLETED', 'RESOLVED')
                       OR resolved_at IS NOT NULL
                ) AS completed_count
            FROM sla_records
        """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new SlaOverviewResponse(
                rs.getLong("total_active"),
                rs.getLong("on_track_count"),
                rs.getLong("warning_count"),
                rs.getLong("breached_count"),
                rs.getLong("completed_count")
        ));
    }
}
