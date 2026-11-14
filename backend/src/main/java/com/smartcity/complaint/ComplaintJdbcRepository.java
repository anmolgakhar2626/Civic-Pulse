package com.smartcity.complaint;

import com.smartcity.complaint.dto.AdminComplaintStatsDTO;
import com.smartcity.complaint.dto.ComplaintStatusHistoryDTO;
import com.smartcity.complaint.dto.DepartmentComplaintStatsDTO;
import com.smartcity.department.Department;
import com.smartcity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.Year;
import java.util.*;

@Repository
public class ComplaintJdbcRepository implements ComplaintDao {

    private static final Logger log = LoggerFactory.getLogger(ComplaintJdbcRepository.class);
    private final JdbcTemplate jdbcTemplate;

    public ComplaintJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Complaint> complaintRowMapper = (rs, rowNum) -> {
        Complaint c = new Complaint();
        String idStr = rs.getString("id");
        if (idStr != null) c.setId(UUID.fromString(idStr));
        c.setComplaintNumber(rs.getString("complaint_number"));

        String citizenIdStr = rs.getString("citizen_id");
        if (citizenIdStr != null) {
            User citizen = new User();
            citizen.setId(UUID.fromString(citizenIdStr));
            citizen.setFullName(rs.getString("citizen_name"));
            c.setCitizen(citizen);
        }
        c.setCitizenName(rs.getString("citizen_name"));
        c.setCitizenPhone(rs.getString("citizen_phone"));
        c.setTitle(rs.getString("title"));
        c.setCategory(rs.getString("category"));
        c.setSubcategory(rs.getString("subcategory"));
        c.setDescription(rs.getString("description"));

        double lat = rs.getDouble("latitude");
        if (!rs.wasNull()) c.setLatitude(lat);
        double lon = rs.getDouble("longitude");
        if (!rs.wasNull()) c.setLongitude(lon);

        c.setAddress(rs.getString("address"));
        c.setStatus(rs.getString("status"));
        c.setComplaintVerificationStatus(rs.getString("complaint_verification_status"));

        String verifierIdStr = rs.getString("verified_by");
        if (verifierIdStr != null) {
            User verifier = new User();
            verifier.setId(UUID.fromString(verifierIdStr));
            c.setVerifiedBy(verifier);
        }

        Timestamp verAt = rs.getTimestamp("verified_at");
        if (verAt != null) c.setVerifiedAt(verAt.toInstant());
        c.setVerificationNotes(rs.getString("verification_notes"));
        c.setEvidenceUrl(rs.getString("evidence_url"));

        String deptIdStr = rs.getString("department_id");
        if (deptIdStr != null) {
            Department d = new Department();
            d.setId(UUID.fromString(deptIdStr));
            try {
                d.setCode(rs.getString("dept_code"));
                d.setName(rs.getString("dept_name"));
            } catch (SQLException ignored) {}
            c.setDepartment(d);
        }

        String assignedToStr = rs.getString("assigned_to_user_id");
        if (assignedToStr != null) {
            User assigned = new User();
            assigned.setId(UUID.fromString(assignedToStr));
            try {
                assigned.setFullName(rs.getString("assignee_name"));
                String assigneeRole = rs.getString("assignee_role");
                if (assigneeRole != null) {
                    assigned.setRoleName(assigneeRole);
                }
            } catch (SQLException ignored) {}
            c.setAssignedTo(assigned);
        }

        c.setPriority(rs.getString("priority"));
        int slaMin = rs.getInt("sla_minutes");
        if (!rs.wasNull()) c.setSlaMinutes(slaMin);

        Timestamp slaDead = rs.getTimestamp("sla_deadline");
        if (slaDead != null) c.setSlaDeadline(slaDead.toInstant());
        c.setSlaStatus(rs.getString("sla_status"));

        c.setResolutionNotes(rs.getString("resolution_notes"));
        c.setResolutionEvidenceUrl(rs.getString("resolution_evidence_url"));

        Timestamp resAt = rs.getTimestamp("resolved_at");
        if (resAt != null) c.setResolvedAt(resAt.toInstant());
        Timestamp clsAt = rs.getTimestamp("closed_at");
        if (clsAt != null) c.setClosedAt(clsAt.toInstant());

        Timestamp crAt = rs.getTimestamp("created_at");
        if (crAt != null) c.setCreatedAt(crAt.toInstant());
        Timestamp upAt = rs.getTimestamp("updated_at");
        if (upAt != null) c.setUpdatedAt(upAt.toInstant());

        return c;
    };

    private final RowMapper<ComplaintAttachment> attachmentRowMapper = (rs, rowNum) -> {
        ComplaintAttachment a = new ComplaintAttachment();
        String idStr = rs.getString("id");
        if (idStr != null) a.setId(UUID.fromString(idStr));
        String cIdStr = rs.getString("complaint_id");
        if (cIdStr != null) a.setComplaintId(UUID.fromString(cIdStr));
        a.setFileName(rs.getString("file_name"));
        a.setOriginalFileName(rs.getString("original_file_name"));
        a.setFilePath(rs.getString("file_path"));
        a.setFileSize(rs.getLong("file_size"));
        a.setContentType(rs.getString("content_type"));
        a.setAttachmentType(rs.getString("attachment_type"));

        String upByStr = rs.getString("uploaded_by");
        if (upByStr != null) {
            User u = new User();
            u.setId(UUID.fromString(upByStr));
            a.setUploadedBy(u);
        }

        Timestamp cr = rs.getTimestamp("created_at");
        if (cr != null) a.setCreatedAt(cr.toInstant());
        return a;
    };

    private final RowMapper<ComplaintReceipt> receiptRowMapper = (rs, rowNum) -> {
        ComplaintReceipt r = new ComplaintReceipt();
        String idStr = rs.getString("id");
        if (idStr != null) r.setId(UUID.fromString(idStr));
        String cIdStr = rs.getString("complaint_id");
        if (cIdStr != null) r.setComplaintId(UUID.fromString(cIdStr));
        r.setReceiptNumber(rs.getString("receipt_number"));
        r.setComplaintRecordNumber(rs.getString("complaint_record_number"));
        r.setCitizenName(rs.getString("citizen_name"));
        r.setCitizenPhone(rs.getString("citizen_phone"));
        r.setTitle(rs.getString("title"));
        r.setCategory(rs.getString("category"));
        r.setDepartmentName(rs.getString("department_name"));
        r.setLocationAddress(rs.getString("location_address"));
        r.setPriority(rs.getString("priority"));
        r.setStatus(rs.getString("status"));

        Timestamp dead = rs.getTimestamp("sla_deadline");
        if (dead != null) r.setSlaDeadline(dead.toInstant());
        int expH = rs.getInt("expected_resolution_hours");
        if (!rs.wasNull()) r.setExpectedResolutionHours(expH);

        r.setAcknowledgementMessage(rs.getString("acknowledgement_message"));
        Timestamp gen = rs.getTimestamp("generated_at");
        if (gen != null) r.setGeneratedAt(gen.toInstant());
        r.setMetadataJson(rs.getString("metadata_json"));
        return r;
    };

    private final RowMapper<ComplaintStatusHistory> statusHistoryRowMapper = (rs, rowNum) -> {
        ComplaintStatusHistory h = new ComplaintStatusHistory();
        String idStr = rs.getString("id");
        if (idStr != null) h.setId(UUID.fromString(idStr));
        String compId = rs.getString("complaint_id");
        if (compId != null) h.setComplaintId(UUID.fromString(compId));
        h.setPreviousStatus(rs.getString("previous_status"));
        h.setNewStatus(rs.getString("new_status"));

        String chBy = rs.getString("changed_by");
        if (chBy != null) h.setChangedBy(UUID.fromString(chBy));
        h.setChangedByName(rs.getString("changed_by_name"));
        h.setChangedByRole(rs.getString("changed_by_role"));
        h.setChangeReason(rs.getString("change_reason"));
        h.setRemarks(rs.getString("remarks"));

        Timestamp cr = rs.getTimestamp("created_at");
        if (cr != null) h.setCreatedAt(cr.toInstant());
        return h;
    };

    private final RowMapper<ComplaintVerificationHistory> verificationHistoryRowMapper = (rs, rowNum) -> {
        ComplaintVerificationHistory h = new ComplaintVerificationHistory();
        String idStr = rs.getString("id");
        if (idStr != null) h.setId(UUID.fromString(idStr));
        String compId = rs.getString("complaint_id");
        if (compId != null) h.setComplaintId(UUID.fromString(compId));
        String citId = rs.getString("citizen_id");
        if (citId != null) h.setCitizenId(UUID.fromString(citId));
        h.setPreviousStatus(rs.getString("previous_status"));
        h.setNewStatus(rs.getString("new_status"));
        h.setDecisionReason(rs.getString("decision_reason"));
        String verBy = rs.getString("verified_by");
        if (verBy != null) h.setVerifiedBy(UUID.fromString(verBy));
        h.setVerifiedByName(rs.getString("verified_by_name"));
        Timestamp cr = rs.getTimestamp("created_at");
        if (cr != null) h.setCreatedAt(cr.toInstant());
        return h;
    };

    @Override
    @Transactional
    public Complaint save(Complaint c) {
        if (c.getId() == null) {
            c.setId(UUID.randomUUID());
        }
        if (c.getCreatedAt() == null) {
            c.setCreatedAt(Instant.now());
        }
        c.setUpdatedAt(Instant.now());

        if (c.getTitle() == null && c.getDescription() != null) {
            c.setTitle(c.getDescription().substring(0, Math.min(c.getDescription().length(), 60)));
        }

        String sql = """
            INSERT INTO complaints (
                id, complaint_number, citizen_id, citizen_name, citizen_phone,
                title, category, subcategory, description, latitude, longitude, address,
                status, complaint_verification_status, verified_by, verified_at,
                verification_notes, department_id, assigned_to_user_id, priority,
                sla_minutes, sla_deadline, sla_status, resolution_notes, resolution_evidence_url,
                resolved_at, closed_at, evidence_url, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                title = EXCLUDED.title,
                category = EXCLUDED.category,
                subcategory = EXCLUDED.subcategory,
                description = EXCLUDED.description,
                status = EXCLUDED.status,
                complaint_verification_status = EXCLUDED.complaint_verification_status,
                verified_by = EXCLUDED.verified_by,
                verified_at = EXCLUDED.verified_at,
                verification_notes = EXCLUDED.verification_notes,
                department_id = EXCLUDED.department_id,
                assigned_to_user_id = EXCLUDED.assigned_to_user_id,
                priority = EXCLUDED.priority,
                sla_minutes = EXCLUDED.sla_minutes,
                sla_deadline = EXCLUDED.sla_deadline,
                sla_status = EXCLUDED.sla_status,
                resolution_notes = EXCLUDED.resolution_notes,
                resolution_evidence_url = EXCLUDED.resolution_evidence_url,
                resolved_at = EXCLUDED.resolved_at,
                closed_at = EXCLUDED.closed_at,
                evidence_url = EXCLUDED.evidence_url,
                updated_at = EXCLUDED.updated_at
        """;

        UUID citId = c.getCitizen() != null ? c.getCitizen().getId() : null;
        UUID verId = c.getVerifiedBy() != null ? c.getVerifiedBy().getId() : null;
        UUID deptId = c.getDepartment() != null ? c.getDepartment().getId() : null;
        UUID assId = c.getAssignedTo() != null ? c.getAssignedTo().getId() : null;

        jdbcTemplate.update(
                sql,
                c.getId(),
                c.getComplaintNumber(),
                citId,
                c.getCitizenName(),
                c.getCitizenPhone(),
                c.getTitle(),
                c.getCategory(),
                c.getSubcategory(),
                c.getDescription(),
                c.getLatitude(),
                c.getLongitude(),
                c.getAddress(),
                c.getStatus(),
                c.getComplaintVerificationStatus(),
                verId,
                c.getVerifiedAt() != null ? Timestamp.from(c.getVerifiedAt()) : null,
                c.getVerificationNotes(),
                deptId,
                assId,
                c.getPriority(),
                c.getSlaMinutes(),
                c.getSlaDeadline() != null ? Timestamp.from(c.getSlaDeadline()) : null,
                c.getSlaStatus(),
                c.getResolutionNotes(),
                c.getResolutionEvidenceUrl(),
                c.getResolvedAt() != null ? Timestamp.from(c.getResolvedAt()) : null,
                c.getClosedAt() != null ? Timestamp.from(c.getClosedAt()) : null,
                c.getEvidenceUrl(),
                Timestamp.from(c.getCreatedAt()),
                Timestamp.from(c.getUpdatedAt())
        );

        return c;
    }

    private String getBaseSelectQuery() {
        return """
            SELECT c.*,
                   d.code AS dept_code, d.name AS dept_name,
                   u.full_name AS assignee_name, u.role AS assignee_role
            FROM complaints c
            LEFT JOIN departments d ON c.department_id = d.id
            LEFT JOIN users u ON c.assigned_to_user_id = u.id
        """;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Complaint> findById(UUID id) {
        String sql = getBaseSelectQuery() + " WHERE c.id = ?";
        try {
            Complaint c = jdbcTemplate.queryForObject(sql, complaintRowMapper, id);
            return Optional.ofNullable(c);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Complaint> findByComplaintNumber(String complaintNumber) {
        String sql = getBaseSelectQuery() + " WHERE c.complaint_number = ?";
        try {
            Complaint c = jdbcTemplate.queryForObject(sql, complaintRowMapper, complaintNumber.trim());
            return Optional.ofNullable(c);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Complaint> findByCitizenId(UUID citizenId) {
        String sql = getBaseSelectQuery() + " WHERE c.citizen_id = ? ORDER BY c.created_at DESC";
        return jdbcTemplate.query(sql, complaintRowMapper, citizenId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Complaint> findAll(String status, String category, String verificationStatus,
                                   UUID citizenId, UUID departmentId, UUID assignedToUserId,
                                   String priority, String search, String slaStatus, Boolean unassigned,
                                   int limit, int offset) {
        StringBuilder sql = new StringBuilder(getBaseSelectQuery() + " WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            if ("BREACHED".equalsIgnoreCase(status.trim())) {
                sql.append("AND (c.sla_status = 'BREACHED' OR (c.sla_deadline < CURRENT_TIMESTAMP AND c.status NOT IN ('RESOLVED', 'CLOSED'))) ");
            } else {
                sql.append("AND c.status = ? ");
                params.add(status.trim().toUpperCase());
            }
        }
        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
            sql.append("AND c.category = ? ");
            params.add(category.trim());
        }
        if (verificationStatus != null && !verificationStatus.isBlank() && !"ALL".equalsIgnoreCase(verificationStatus)) {
            sql.append("AND c.complaint_verification_status = ? ");
            params.add(verificationStatus.trim().toUpperCase());
        }
        if (citizenId != null) {
            sql.append("AND c.citizen_id = ? ");
            params.add(citizenId);
        }
        if (departmentId != null) {
            sql.append("AND c.department_id = ? ");
            params.add(departmentId);
        }
        if (assignedToUserId != null) {
            sql.append("AND c.assigned_to_user_id = ? ");
            params.add(assignedToUserId);
        }
        if (priority != null && !priority.isBlank() && !"ALL".equalsIgnoreCase(priority)) {
            sql.append("AND c.priority = ? ");
            params.add(priority.trim().toUpperCase());
        }
        if (slaStatus != null && !slaStatus.isBlank() && !"ALL".equalsIgnoreCase(slaStatus)) {
            String sNorm = slaStatus.trim().toUpperCase();
            if ("BREACHED".equalsIgnoreCase(sNorm)) {
                sql.append("AND (c.sla_status = 'BREACHED' OR (c.sla_deadline < CURRENT_TIMESTAMP AND c.status NOT IN ('RESOLVED', 'CLOSED'))) ");
            } else if ("AT_RISK".equalsIgnoreCase(sNorm) || "WARNING".equalsIgnoreCase(sNorm)) {
                sql.append("AND (c.sla_status = 'AT_RISK' OR (c.sla_deadline BETWEEN CURRENT_TIMESTAMP AND CURRENT_TIMESTAMP + INTERVAL '2 hours' AND c.status NOT IN ('RESOLVED', 'CLOSED'))) ");
            } else if ("ON_TRACK".equalsIgnoreCase(sNorm)) {
                sql.append("AND (c.sla_status = 'ON_TRACK' AND (c.sla_deadline IS NULL OR c.sla_deadline > CURRENT_TIMESTAMP + INTERVAL '2 hours') AND c.status NOT IN ('RESOLVED', 'CLOSED')) ");
            } else {
                sql.append("AND c.sla_status = ? ");
                params.add(sNorm);
            }
        }
        if (Boolean.TRUE.equals(unassigned)) {
            sql.append("AND (c.assigned_to_user_id IS NULL OR c.status = 'SUBMITTED') ");
        }
        if (search != null && !search.isBlank()) {
            String q = "%" + search.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(c.complaint_number) LIKE ? OR LOWER(c.title) LIKE ? OR LOWER(c.description) LIKE ? OR LOWER(c.address) LIKE ? OR LOWER(c.citizen_name) LIKE ?) ");
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
        }

        sql.append("ORDER BY c.created_at DESC LIMIT ? OFFSET ?");
        params.add(limit > 0 ? limit : 50);
        params.add(offset >= 0 ? offset : 0);

        return jdbcTemplate.query(sql.toString(), complaintRowMapper, params.toArray());
    }

    @Override
    @Transactional(readOnly = true)
    public int countAll(String status, String category, String verificationStatus,
                        UUID citizenId, UUID departmentId, UUID assignedToUserId,
                        String priority, String search, String slaStatus, Boolean unassigned) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM complaints c WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            if ("BREACHED".equalsIgnoreCase(status.trim())) {
                sql.append("AND (c.sla_status = 'BREACHED' OR (c.sla_deadline < CURRENT_TIMESTAMP AND c.status NOT IN ('RESOLVED', 'CLOSED'))) ");
            } else {
                sql.append("AND c.status = ? ");
                params.add(status.trim().toUpperCase());
            }
        }
        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
            sql.append("AND c.category = ? ");
            params.add(category.trim());
        }
        if (verificationStatus != null && !verificationStatus.isBlank() && !"ALL".equalsIgnoreCase(verificationStatus)) {
            sql.append("AND c.complaint_verification_status = ? ");
            params.add(verificationStatus.trim().toUpperCase());
        }
        if (citizenId != null) {
            sql.append("AND c.citizen_id = ? ");
            params.add(citizenId);
        }
        if (departmentId != null) {
            sql.append("AND c.department_id = ? ");
            params.add(departmentId);
        }
        if (assignedToUserId != null) {
            sql.append("AND c.assigned_to_user_id = ? ");
            params.add(assignedToUserId);
        }
        if (priority != null && !priority.isBlank() && !"ALL".equalsIgnoreCase(priority)) {
            sql.append("AND c.priority = ? ");
            params.add(priority.trim().toUpperCase());
        }
        if (slaStatus != null && !slaStatus.isBlank() && !"ALL".equalsIgnoreCase(slaStatus)) {
            String sNorm = slaStatus.trim().toUpperCase();
            if ("BREACHED".equalsIgnoreCase(sNorm)) {
                sql.append("AND (c.sla_status = 'BREACHED' OR (c.sla_deadline < CURRENT_TIMESTAMP AND c.status NOT IN ('RESOLVED', 'CLOSED'))) ");
            } else if ("AT_RISK".equalsIgnoreCase(sNorm) || "WARNING".equalsIgnoreCase(sNorm)) {
                sql.append("AND (c.sla_status = 'AT_RISK' OR (c.sla_deadline BETWEEN CURRENT_TIMESTAMP AND CURRENT_TIMESTAMP + INTERVAL '2 hours' AND c.status NOT IN ('RESOLVED', 'CLOSED'))) ");
            } else if ("ON_TRACK".equalsIgnoreCase(sNorm)) {
                sql.append("AND (c.sla_status = 'ON_TRACK' AND (c.sla_deadline IS NULL OR c.sla_deadline > CURRENT_TIMESTAMP + INTERVAL '2 hours') AND c.status NOT IN ('RESOLVED', 'CLOSED')) ");
            } else {
                sql.append("AND c.sla_status = ? ");
                params.add(sNorm);
            }
        }
        if (Boolean.TRUE.equals(unassigned)) {
            sql.append("AND (c.assigned_to_user_id IS NULL OR c.status = 'SUBMITTED') ");
        }
        if (search != null && !search.isBlank()) {
            String q = "%" + search.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(c.complaint_number) LIKE ? OR LOWER(c.title) LIKE ? OR LOWER(c.description) LIKE ? OR LOWER(c.address) LIKE ? OR LOWER(c.citizen_name) LIKE ?) ");
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
        }

        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    @Override
    @Transactional
    public int updateVerificationStatus(UUID complaintId, String newStatus, UUID verifiedBy, String notes) {
        String sql = """
            UPDATE complaints SET
                complaint_verification_status = ?,
                verified_by = ?,
                verified_at = ?,
                verification_notes = ?,
                updated_at = ?
            WHERE id = ?
        """;
        Instant now = Instant.now();
        return jdbcTemplate.update(sql, newStatus, verifiedBy, Timestamp.from(now), notes, Timestamp.from(now), complaintId);
    }

    @Override
    @Transactional(readOnly = true)
    public int countGenuineComplaintsByCitizen(UUID citizenId) {
        String sql = """
            SELECT COUNT(DISTINCT id)
            FROM complaints
            WHERE citizen_id = ?
              AND complaint_verification_status = 'GENUINE'
        """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, citizenId);
        return count != null ? count : 0;
    }

    @Override
    @Transactional
    public void saveVerificationHistory(ComplaintVerificationHistory history) {
        if (history.getId() == null) {
            history.setId(UUID.randomUUID());
        }
        if (history.getCreatedAt() == null) {
            history.setCreatedAt(Instant.now());
        }

        String sql = """
            INSERT INTO complaint_verification_history (
                id, complaint_id, citizen_id, previous_status, new_status,
                decision_reason, verified_by, verified_by_name, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        jdbcTemplate.update(
                sql,
                history.getId(),
                history.getComplaintId(),
                history.getCitizenId(),
                history.getPreviousStatus(),
                history.getNewStatus(),
                history.getDecisionReason(),
                history.getVerifiedBy(),
                history.getVerifiedByName(),
                Timestamp.from(history.getCreatedAt())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintVerificationHistory> findVerificationHistory(UUID complaintId) {
        String sql = "SELECT * FROM complaint_verification_history WHERE complaint_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, verificationHistoryRowMapper, complaintId);
    }

    @Override
    public String generateNextComplaintRecordNumber() {
        try {
            Long nextVal = jdbcTemplate.queryForObject("SELECT nextval('complaint_record_seq')", Long.class);
            if (nextVal == null) nextVal = 1L;
            int year = Year.now().getValue();
            return String.format("CP-%d-%06d", year, nextVal);
        } catch (Exception e) {
            log.warn("Sequence complaint_record_seq not available, fallback: {}", e.getMessage());
            long fallbackNum = (System.currentTimeMillis() % 900000) + 100000;
            return String.format("CP-%d-%06d", Year.now().getValue(), fallbackNum);
        }
    }

    @Override
    @Transactional
    public ComplaintAttachment saveAttachment(ComplaintAttachment att) {
        if (att.getId() == null) {
            att.setId(UUID.randomUUID());
        }
        if (att.getCreatedAt() == null) {
            att.setCreatedAt(Instant.now());
        }

        String sql = """
            INSERT INTO complaint_attachments (
                id, complaint_id, file_name, original_file_name, file_path,
                file_size, content_type, attachment_type, uploaded_by, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        UUID upBy = att.getUploadedBy() != null ? att.getUploadedBy().getId() : null;

        jdbcTemplate.update(
                sql,
                att.getId(),
                att.getComplaintId(),
                att.getFileName(),
                att.getOriginalFileName(),
                att.getFilePath(),
                att.getFileSize(),
                att.getContentType(),
                att.getAttachmentType(),
                upBy,
                Timestamp.from(att.getCreatedAt())
        );
        return att;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintAttachment> findAttachmentsByComplaintId(UUID complaintId) {
        String sql = "SELECT * FROM complaint_attachments WHERE complaint_id = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, attachmentRowMapper, complaintId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ComplaintAttachment> findAttachmentById(UUID attachmentId) {
        String sql = "SELECT * FROM complaint_attachments WHERE id = ?";
        try {
            ComplaintAttachment att = jdbcTemplate.queryForObject(sql, attachmentRowMapper, attachmentId);
            return Optional.ofNullable(att);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public ComplaintReceipt saveReceipt(ComplaintReceipt r) {
        if (r.getId() == null) {
            r.setId(UUID.randomUUID());
        }
        if (r.getGeneratedAt() == null) {
            r.setGeneratedAt(Instant.now());
        }

        String sql = """
            INSERT INTO complaint_receipts (
                id, complaint_id, receipt_number, complaint_record_number, citizen_name,
                citizen_phone, title, category, department_name, location_address,
                priority, status, sla_deadline, expected_resolution_hours,
                acknowledgement_message, generated_at, metadata_json
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (complaint_id) DO UPDATE SET
                status = EXCLUDED.status,
                metadata_json = EXCLUDED.metadata_json
        """;

        jdbcTemplate.update(
                sql,
                r.getId(),
                r.getComplaintId(),
                r.getReceiptNumber(),
                r.getComplaintRecordNumber(),
                r.getCitizenName(),
                r.getCitizenPhone(),
                r.getTitle(),
                r.getCategory(),
                r.getDepartmentName(),
                r.getLocationAddress(),
                r.getPriority(),
                r.getStatus(),
                r.getSlaDeadline() != null ? Timestamp.from(r.getSlaDeadline()) : null,
                r.getExpectedResolutionHours(),
                r.getAcknowledgementMessage(),
                Timestamp.from(r.getGeneratedAt()),
                r.getMetadataJson()
        );
        return r;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ComplaintReceipt> findReceiptByComplaintId(UUID complaintId) {
        String sql = "SELECT * FROM complaint_receipts WHERE complaint_id = ?";
        try {
            ComplaintReceipt r = jdbcTemplate.queryForObject(sql, receiptRowMapper, complaintId);
            return Optional.ofNullable(r);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ComplaintReceipt> findReceiptByNumber(String receiptNumber) {
        String sql = "SELECT * FROM complaint_receipts WHERE receipt_number = ?";
        try {
            ComplaintReceipt r = jdbcTemplate.queryForObject(sql, receiptRowMapper, receiptNumber.trim());
            return Optional.ofNullable(r);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public ComplaintStatusHistory saveStatusHistory(ComplaintStatusHistory history) {
        if (history.getId() == null) {
            history.setId(UUID.randomUUID());
        }
        if (history.getCreatedAt() == null) {
            history.setCreatedAt(Instant.now());
        }

        String sql = """
            INSERT INTO complaint_status_history (
                id, complaint_id, previous_status, new_status, changed_by,
                changed_by_name, changed_by_role, change_reason, remarks, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        jdbcTemplate.update(
                sql,
                history.getId(),
                history.getComplaintId(),
                history.getPreviousStatus(),
                history.getNewStatus(),
                history.getChangedBy(),
                history.getChangedByName(),
                history.getChangedByRole(),
                history.getChangeReason(),
                history.getRemarks(),
                Timestamp.from(history.getCreatedAt())
        );
        return history;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintStatusHistory> findStatusHistoryByComplaintId(UUID complaintId) {
        String sql = "SELECT * FROM complaint_status_history WHERE complaint_id = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, statusHistoryRowMapper, complaintId);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminComplaintStatsDTO getAdminComplaintStats() {
        AdminComplaintStatsDTO stats = new AdminComplaintStatsDTO();

        // 1. Total registered citizens
        Integer citizens = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users u JOIN user_roles ur ON u.id = ur.user_id JOIN roles r ON ur.role_id = r.id WHERE r.name = 'CITIZEN' OR u.role = 'CITIZEN'",
                Integer.class
        );
        stats.setTotalRegisteredCitizens(citizens != null ? citizens : 0);

        // 2. Complaint counts by status
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM complaints", Integer.class);
        stats.setTotalComplaints(total != null ? total : 0);

        Integer pending = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM complaints WHERE status IN ('SUBMITTED', 'ASSIGNED', 'OPEN')",
                Integer.class
        );
        stats.setPendingComplaints(pending != null ? pending : 0);

        Integer inProgress = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM complaints WHERE status IN ('IN_PROGRESS', 'RESOLUTION_PENDING_VERIFICATION')",
                Integer.class
        );
        stats.setInProgressComplaints(inProgress != null ? inProgress : 0);

        Integer resolved = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM complaints WHERE status = 'RESOLVED'",
                Integer.class
        );
        stats.setResolvedComplaints(resolved != null ? resolved : 0);

        Integer closed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM complaints WHERE status = 'CLOSED'",
                Integer.class
        );
        stats.setClosedComplaints(closed != null ? closed : 0);

        // Overdue SLA complaints: sla_status = 'BREACHED' or (deadline passed and status not in RESOLVED, CLOSED)
        Integer overdue = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM complaints WHERE sla_status = 'BREACHED' OR (sla_deadline < CURRENT_TIMESTAMP AND status NOT IN ('RESOLVED', 'CLOSED'))",
                Integer.class
        );
        stats.setOverdueSlaComplaints(overdue != null ? overdue : 0);

        // 3. Priority distribution
        Map<String, Long> prioMap = new HashMap<>();
        prioMap.put("CRITICAL", 0L);
        prioMap.put("HIGH", 0L);
        prioMap.put("MEDIUM", 0L);
        prioMap.put("LOW", 0L);

        jdbcTemplate.query("SELECT priority, COUNT(*) as cnt FROM complaints GROUP BY priority", (rs) -> {
            String p = rs.getString("priority");
            long c = rs.getLong("cnt");
            if (p != null) prioMap.put(p.toUpperCase(), c);
        });
        stats.setPriorityDistribution(prioMap);

        // 4. Status distribution
        Map<String, Long> statusMap = new HashMap<>();
        jdbcTemplate.query("SELECT status, COUNT(*) as cnt FROM complaints GROUP BY status", (rs) -> {
            String s = rs.getString("status");
            long c = rs.getLong("cnt");
            if (s != null) statusMap.put(s.toUpperCase(), c);
        });
        stats.setStatusDistribution(statusMap);

        // 5. Department-wise complaint stats
        stats.setDepartmentStats(getAllDepartmentStats());

        // 6. Recent activities (status transitions)
        String histSql = """
            SELECT * FROM complaint_status_history
            ORDER BY created_at DESC LIMIT 15
        """;
        List<ComplaintStatusHistory> historyList = jdbcTemplate.query(histSql, statusHistoryRowMapper);
        List<ComplaintStatusHistoryDTO> dtos = historyList.stream().map(ComplaintStatusHistoryDTO::new).toList();
        stats.setRecentActivities(dtos);

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentComplaintStatsDTO> getAllDepartmentStats() {
        String sql = """
            SELECT d.id AS dept_id, d.code AS dept_code, d.name AS dept_name,
                   COUNT(c.id) AS total_assigned,
                   COUNT(CASE WHEN c.status IN ('SUBMITTED', 'ASSIGNED') THEN 1 END) AS newly_received,
                   COUNT(CASE WHEN c.status IN ('SUBMITTED', 'ASSIGNED', 'OPEN') THEN 1 END) AS pending,
                   COUNT(CASE WHEN c.status IN ('IN_PROGRESS', 'RESOLUTION_PENDING_VERIFICATION') THEN 1 END) AS in_progress,
                   COUNT(CASE WHEN c.status IN ('RESOLVED', 'CLOSED') THEN 1 END) AS resolved,
                   COUNT(CASE WHEN c.sla_status = 'AT_RISK' OR (c.sla_deadline BETWEEN CURRENT_TIMESTAMP AND CURRENT_TIMESTAMP + INTERVAL '2 hours' AND c.status NOT IN ('RESOLVED', 'CLOSED')) THEN 1 END) AS approaching_deadline,
                   COUNT(CASE WHEN c.sla_status = 'BREACHED' OR (c.sla_deadline < CURRENT_TIMESTAMP AND c.status NOT IN ('RESOLVED', 'CLOSED')) THEN 1 END) AS breached_deadline,
                   COALESCE(AVG(CASE WHEN c.resolved_at IS NOT NULL AND c.created_at IS NOT NULL THEN EXTRACT(EPOCH FROM (c.resolved_at - c.created_at))/3600.0 END), 0.0) AS avg_resolution_hours
            FROM departments d
            LEFT JOIN complaints c ON c.department_id = d.id
            GROUP BY d.id, d.code, d.name
            ORDER BY d.name ASC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String deptIdStr = rs.getString("dept_id");
            UUID deptId = deptIdStr != null ? UUID.fromString(deptIdStr) : null;
            return new DepartmentComplaintStatsDTO(
                    deptId,
                    rs.getString("dept_code"),
                    rs.getString("dept_name"),
                    rs.getLong("total_assigned"),
                    rs.getLong("newly_received"),
                    rs.getLong("pending"),
                    rs.getLong("in_progress"),
                    rs.getLong("resolved"),
                    rs.getLong("approaching_deadline"),
                    rs.getLong("breached_deadline"),
                    Math.round(rs.getDouble("avg_resolution_hours") * 10.0) / 10.0
            );
        });
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentComplaintStatsDTO getDepartmentComplaintStats(UUID departmentId) {
        String sql = """
            SELECT d.id AS dept_id, d.code AS dept_code, d.name AS dept_name,
                   COUNT(c.id) AS total_assigned,
                   COUNT(CASE WHEN c.status IN ('SUBMITTED', 'ASSIGNED') THEN 1 END) AS newly_received,
                   COUNT(CASE WHEN c.status IN ('SUBMITTED', 'ASSIGNED', 'OPEN') THEN 1 END) AS pending,
                   COUNT(CASE WHEN c.status IN ('IN_PROGRESS', 'RESOLUTION_PENDING_VERIFICATION') THEN 1 END) AS in_progress,
                   COUNT(CASE WHEN c.status IN ('RESOLVED', 'CLOSED') THEN 1 END) AS resolved,
                   COUNT(CASE WHEN c.sla_status = 'AT_RISK' OR (c.sla_deadline BETWEEN CURRENT_TIMESTAMP AND CURRENT_TIMESTAMP + INTERVAL '2 hours' AND c.status NOT IN ('RESOLVED', 'CLOSED')) THEN 1 END) AS approaching_deadline,
                   COUNT(CASE WHEN c.sla_status = 'BREACHED' OR (c.sla_deadline < CURRENT_TIMESTAMP AND c.status NOT IN ('RESOLVED', 'CLOSED')) THEN 1 END) AS breached_deadline,
                   COALESCE(AVG(CASE WHEN c.resolved_at IS NOT NULL AND c.created_at IS NOT NULL THEN EXTRACT(EPOCH FROM (c.resolved_at - c.created_at))/3600.0 END), 0.0) AS avg_resolution_hours
            FROM departments d
            LEFT JOIN complaints c ON c.department_id = d.id
            WHERE d.id = ?
            GROUP BY d.id, d.code, d.name
        """;

        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                String deptIdStr = rs.getString("dept_id");
                UUID deptId = deptIdStr != null ? UUID.fromString(deptIdStr) : null;
                return new DepartmentComplaintStatsDTO(
                        deptId,
                        rs.getString("dept_code"),
                        rs.getString("dept_name"),
                        rs.getLong("total_assigned"),
                        rs.getLong("newly_received"),
                        rs.getLong("pending"),
                        rs.getLong("in_progress"),
                        rs.getLong("resolved"),
                        rs.getLong("approaching_deadline"),
                        rs.getLong("breached_deadline"),
                        Math.round(rs.getDouble("avg_resolution_hours") * 10.0) / 10.0
                );
            }, departmentId);
        } catch (EmptyResultDataAccessException e) {
            return new DepartmentComplaintStatsDTO(departmentId, "UNKNOWN", "Department", 0, 0, 0, 0, 0, 0, 0, 0.0);
        }
    }
}
