package com.smartcity.grievance.portal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class GovernmentGrievanceJdbcRepository implements GovernmentGrievanceDao {

    private static final Logger log = LoggerFactory.getLogger(GovernmentGrievanceJdbcRepository.class);
    private final JdbcTemplate jdbcTemplate;

    public GovernmentGrievanceJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<GovernmentPortal> portalRowMapper = (rs, rowNum) -> {
        GovernmentPortal p = new GovernmentPortal();
        String idStr = rs.getString("id");
        if (idStr != null) p.setId(UUID.fromString(idStr));
        p.setPortalCode(rs.getString("portal_code"));
        p.setPortalName(rs.getString("portal_name"));
        p.setEndpointUrl(rs.getString("endpoint_url"));
        p.setActive(rs.getBoolean("is_active"));
        p.setSandbox(rs.getBoolean("is_sandbox"));
        p.setAuthType(rs.getString("auth_type"));
        p.setConfigMetadata(rs.getString("config_metadata"));
        Timestamp cr = rs.getTimestamp("created_at");
        if (cr != null) p.setCreatedAt(cr.toInstant());
        return p;
    };

    private final RowMapper<ExternalGrievance> grievanceRowMapper = (rs, rowNum) -> {
        ExternalGrievance g = new ExternalGrievance();
        String idStr = rs.getString("id");
        if (idStr != null) g.setId(UUID.fromString(idStr));
        String compStr = rs.getString("complaint_id");
        if (compStr != null) g.setComplaintId(UUID.fromString(compStr));
        String portStr = rs.getString("portal_id");
        if (portStr != null) g.setPortalId(UUID.fromString(portStr));
        g.setPortalCode(rs.getString("portal_code"));
        g.setAcknowledgementNumber(rs.getString("acknowledgement_number"));
        g.setIdempotencyKey(rs.getString("idempotency_key"));
        g.setExternalStatus(rs.getString("external_status"));
        g.setStatusDescription(rs.getString("status_description"));
        g.setOfficerInCharge(rs.getString("officer_in_charge"));
        Timestamp tg = rs.getTimestamp("target_resolution_date");
        if (tg != null) g.setTargetResolutionDate(tg.toInstant());
        Timestamp sb = rs.getTimestamp("submitted_at");
        if (sb != null) g.setSubmittedAt(sb.toInstant());
        Timestamp ls = rs.getTimestamp("last_synced_at");
        if (ls != null) g.setLastSyncedAt(ls.toInstant());
        Timestamp cr = rs.getTimestamp("created_at");
        if (cr != null) g.setCreatedAt(cr.toInstant());
        Timestamp up = rs.getTimestamp("updated_at");
        if (up != null) g.setUpdatedAt(up.toInstant());
        return g;
    };

    private final RowMapper<GrievanceStatusHistory> statusHistoryRowMapper = (rs, rowNum) -> {
        GrievanceStatusHistory h = new GrievanceStatusHistory();
        String idStr = rs.getString("id");
        if (idStr != null) h.setId(UUID.fromString(idStr));
        String grvStr = rs.getString("external_grievance_id");
        if (grvStr != null) h.setExternalGrievanceId(UUID.fromString(grvStr));
        h.setStatus(rs.getString("status"));
        h.setRemarks(rs.getString("remarks"));
        h.setActionTakenBy(rs.getString("action_taken_by"));
        Timestamp ev = rs.getTimestamp("event_timestamp");
        if (ev != null) h.setEventTimestamp(ev.toInstant());
        h.setRawPayload(rs.getString("raw_payload"));
        return h;
    };

    private final RowMapper<GrievanceSyncLog> syncLogRowMapper = (rs, rowNum) -> {
        GrievanceSyncLog l = new GrievanceSyncLog();
        String idStr = rs.getString("id");
        if (idStr != null) l.setId(UUID.fromString(idStr));
        String grvStr = rs.getString("external_grievance_id");
        if (grvStr != null) l.setExternalGrievanceId(UUID.fromString(grvStr));
        l.setPortalCode(rs.getString("portal_code"));
        l.setSyncType(rs.getString("sync_type"));
        l.setStatus(rs.getString("status"));
        l.setIdempotencyKey(rs.getString("idempotency_key"));
        l.setRequestPayload(rs.getString("request_payload"));
        l.setResponsePayload(rs.getString("response_payload"));
        l.setErrorMessage(rs.getString("error_message"));
        l.setRetryCount(rs.getInt("retry_count"));
        Timestamp ts = rs.getTimestamp("sync_timestamp");
        if (ts != null) l.setSyncTimestamp(ts.toInstant());
        return l;
    };

    private final RowMapper<AcknowledgementDocument> documentRowMapper = (rs, rowNum) -> {
        AcknowledgementDocument d = new AcknowledgementDocument();
        String idStr = rs.getString("id");
        if (idStr != null) d.setId(UUID.fromString(idStr));
        String grvStr = rs.getString("external_grievance_id");
        if (grvStr != null) d.setExternalGrievanceId(UUID.fromString(grvStr));
        d.setDocumentReference(rs.getString("document_reference"));
        d.setDocumentType(rs.getString("document_type"));
        d.setDocumentTitle(rs.getString("document_title"));
        d.setDocumentUrl(rs.getString("document_url"));
        Timestamp is = rs.getTimestamp("issued_at");
        if (is != null) d.setIssuedAt(is.toInstant());
        d.setFileSizeBytes(rs.getLong("file_size_bytes"));
        return d;
    };

    @Override
    @Transactional(readOnly = true)
    public Optional<GovernmentPortal> findPortalByCode(String portalCode) {
        String sql = "SELECT * FROM government_portals WHERE portal_code = ? AND is_active = TRUE";
        try {
            GovernmentPortal p = jdbcTemplate.queryForObject(sql, portalRowMapper, portalCode.trim());
            return Optional.ofNullable(p);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<GovernmentPortal> findAllActivePortals() {
        String sql = "SELECT * FROM government_portals WHERE is_active = TRUE ORDER BY portal_code ASC";
        return jdbcTemplate.query(sql, portalRowMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExternalGrievance> findByComplaintId(UUID complaintId) {
        String sql = "SELECT * FROM external_grievances WHERE complaint_id = ? ORDER BY submitted_at DESC LIMIT 1";
        try {
            ExternalGrievance g = jdbcTemplate.queryForObject(sql, grievanceRowMapper, complaintId);
            return Optional.ofNullable(g);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExternalGrievance> findById(UUID id) {
        String sql = "SELECT * FROM external_grievances WHERE id = ?";
        try {
            ExternalGrievance g = jdbcTemplate.queryForObject(sql, grievanceRowMapper, id);
            return Optional.ofNullable(g);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExternalGrievance> findByIdempotencyKey(String idempotencyKey) {
        String sql = "SELECT * FROM external_grievances WHERE idempotency_key = ?";
        try {
            ExternalGrievance g = jdbcTemplate.queryForObject(sql, grievanceRowMapper, idempotencyKey.trim());
            return Optional.ofNullable(g);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExternalGrievance> findByAcknowledgementNumber(String acknowledgementNumber) {
        String sql = "SELECT * FROM external_grievances WHERE acknowledgement_number = ?";
        try {
            ExternalGrievance g = jdbcTemplate.queryForObject(sql, grievanceRowMapper, acknowledgementNumber.trim());
            return Optional.ofNullable(g);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public ExternalGrievance saveExternalGrievance(ExternalGrievance g) {
        if (g.getId() == null) {
            g.setId(UUID.randomUUID());
        }
        if (g.getCreatedAt() == null) {
            g.setCreatedAt(Instant.now());
        }
        if (g.getSubmittedAt() == null) {
            g.setSubmittedAt(Instant.now());
        }
        if (g.getLastSyncedAt() == null) {
            g.setLastSyncedAt(Instant.now());
        }
        g.setUpdatedAt(Instant.now());

        String sql = """
            INSERT INTO external_grievances (
                id, complaint_id, portal_id, portal_code, acknowledgement_number,
                idempotency_key, external_status, status_description, officer_in_charge,
                target_resolution_date, submitted_at, last_synced_at, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (acknowledgement_number) DO UPDATE SET
                external_status = EXCLUDED.external_status,
                status_description = EXCLUDED.status_description,
                officer_in_charge = EXCLUDED.officer_in_charge,
                target_resolution_date = EXCLUDED.target_resolution_date,
                last_synced_at = EXCLUDED.last_synced_at,
                updated_at = EXCLUDED.updated_at
        """;

        jdbcTemplate.update(
                sql,
                g.getId(),
                g.getComplaintId(),
                g.getPortalId(),
                g.getPortalCode(),
                g.getAcknowledgementNumber(),
                g.getIdempotencyKey(),
                g.getExternalStatus(),
                g.getStatusDescription(),
                g.getOfficerInCharge(),
                g.getTargetResolutionDate() != null ? Timestamp.from(g.getTargetResolutionDate()) : null,
                Timestamp.from(g.getSubmittedAt()),
                Timestamp.from(g.getLastSyncedAt()),
                Timestamp.from(g.getCreatedAt()),
                Timestamp.from(g.getUpdatedAt())
        );

        return g;
    }

    @Override
    @Transactional
    public int updateExternalGrievanceStatus(UUID grievanceId, String newStatus, String statusDescription, Instant lastSyncedAt) {
        String sql = """
            UPDATE external_grievances SET
                external_status = ?,
                status_description = COALESCE(?, status_description),
                last_synced_at = ?,
                updated_at = ?
            WHERE id = ?
        """;
        Instant now = Instant.now();
        Instant syncTime = lastSyncedAt != null ? lastSyncedAt : now;
        return jdbcTemplate.update(sql, newStatus, statusDescription, Timestamp.from(syncTime), Timestamp.from(now), grievanceId);
    }

    @Override
    @Transactional
    public void saveStatusHistory(GrievanceStatusHistory h) {
        if (h.getId() == null) {
            h.setId(UUID.randomUUID());
        }
        if (h.getEventTimestamp() == null) {
            h.setEventTimestamp(Instant.now());
        }
        String sql = """
            INSERT INTO grievance_status_history (
                id, external_grievance_id, status, remarks, action_taken_by, event_timestamp, raw_payload
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(
                sql,
                h.getId(),
                h.getExternalGrievanceId(),
                h.getStatus(),
                h.getRemarks(),
                h.getActionTakenBy(),
                Timestamp.from(h.getEventTimestamp()),
                h.getRawPayload()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<GrievanceStatusHistory> findStatusHistory(UUID externalGrievanceId) {
        String sql = "SELECT * FROM grievance_status_history WHERE external_grievance_id = ? ORDER BY event_timestamp ASC";
        return jdbcTemplate.query(sql, statusHistoryRowMapper, externalGrievanceId);
    }

    @Override
    @Transactional
    public void saveSyncLog(GrievanceSyncLog log) {
        if (log.getId() == null) {
            log.setId(UUID.randomUUID());
        }
        if (log.getSyncTimestamp() == null) {
            log.setSyncTimestamp(Instant.now());
        }
        String sql = """
            INSERT INTO grievance_sync_logs (
                id, external_grievance_id, portal_code, sync_type, status,
                idempotency_key, request_payload, response_payload, error_message, retry_count, sync_timestamp
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(
                sql,
                log.getId(),
                log.getExternalGrievanceId(),
                log.getPortalCode(),
                log.getSyncType(),
                log.getStatus(),
                log.getIdempotencyKey(),
                log.getRequestPayload(),
                log.getResponsePayload(),
                log.getErrorMessage(),
                log.getRetryCount(),
                Timestamp.from(log.getSyncTimestamp())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<GrievanceSyncLog> findSyncLogs(UUID externalGrievanceId) {
        String sql = "SELECT * FROM grievance_sync_logs WHERE external_grievance_id = ? ORDER BY sync_timestamp DESC";
        return jdbcTemplate.query(sql, syncLogRowMapper, externalGrievanceId);
    }

    @Override
    @Transactional
    public void saveDocument(AcknowledgementDocument doc) {
        if (doc.getId() == null) {
            doc.setId(UUID.randomUUID());
        }
        if (doc.getIssuedAt() == null) {
            doc.setIssuedAt(Instant.now());
        }
        String sql = """
            INSERT INTO acknowledgement_documents (
                id, external_grievance_id, document_reference, document_type, document_title, document_url, issued_at, file_size_bytes
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(
                sql,
                doc.getId(),
                doc.getExternalGrievanceId(),
                doc.getDocumentReference(),
                doc.getDocumentType(),
                doc.getDocumentTitle(),
                doc.getDocumentUrl(),
                Timestamp.from(doc.getIssuedAt()),
                doc.getFileSizeBytes()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcknowledgementDocument> findDocuments(UUID externalGrievanceId) {
        String sql = "SELECT * FROM acknowledgement_documents WHERE external_grievance_id = ? ORDER BY issued_at DESC";
        return jdbcTemplate.query(sql, documentRowMapper, externalGrievanceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AcknowledgementDocument> findDocumentByReference(String reference) {
        String sql = "SELECT * FROM acknowledgement_documents WHERE document_reference = ?";
        try {
            AcknowledgementDocument doc = jdbcTemplate.queryForObject(sql, documentRowMapper, reference.trim());
            return Optional.ofNullable(doc);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
