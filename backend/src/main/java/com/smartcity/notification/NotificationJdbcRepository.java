package com.smartcity.notification;

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
import java.util.*;

@Repository
@Primary
public class NotificationJdbcRepository implements NotificationDao {

    private static final Logger log = LoggerFactory.getLogger(NotificationJdbcRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public NotificationJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<NotificationDTO> rowMapper = new RowMapper<>() {
        @Override
        public NotificationDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            NotificationDTO dto = new NotificationDTO();
            dto.setId(UUID.fromString(rs.getString("id")));

            String recipientIdStr = rs.getString("recipient_user_id");
            if (recipientIdStr != null) {
                dto.setRecipientUserId(UUID.fromString(recipientIdStr));
            }

            if (hasColumn(rs, "recipient_username")) {
                dto.setRecipientUsername(rs.getString("recipient_username"));
            }

            dto.setRecipientRole(rs.getString("recipient_role"));
            dto.setTitle(rs.getString("title"));
            dto.setMessage(rs.getString("message"));
            dto.setNotificationType(rs.getString("notification_type"));

            if (hasColumn(rs, "incident_id")) {
                String incIdStr = rs.getString("incident_id");
                if (incIdStr != null) {
                    dto.setIncidentId(UUID.fromString(incIdStr));
                }
            }

            if (hasColumn(rs, "complaint_id")) {
                String compIdStr = rs.getString("complaint_id");
                if (compIdStr != null) {
                    dto.setComplaintId(UUID.fromString(compIdStr));
                }
            }

            if (hasColumn(rs, "complaint_number")) {
                dto.setComplaintNumber(rs.getString("complaint_number"));
            }

            if (hasColumn(rs, "severity")) {
                dto.setSeverity(rs.getString("severity"));
            }

            dto.setChannel(rs.getString("channel"));
            dto.setStatus(rs.getString("status"));

            if (hasColumn(rs, "is_read")) {
                dto.setRead(rs.getBoolean("is_read"));
            } else {
                dto.setRead("READ".equalsIgnoreCase(dto.getStatus()));
            }

            if (hasColumn(rs, "dedup_key")) {
                dto.setDedupKey(rs.getString("dedup_key"));
            }

            Timestamp sentAt = rs.getTimestamp("sent_at");
            if (sentAt != null) {
                dto.setSentAt(sentAt.toInstant());
            }

            Timestamp readAt = rs.getTimestamp("read_at");
            if (readAt != null) {
                dto.setReadAt(readAt.toInstant());
            }

            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                dto.setCreatedAt(createdAt.toInstant());
            }

            return dto;
        }
    };

    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException ignored) {
            return false;
        }
    }

    @Override
    public NotificationDTO save(Notification notification) {
        if (notification.getId() == null) {
            notification.setId(UUID.randomUUID());
        }
        Instant now = Instant.now();
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(now);
        }
        if (notification.getSentAt() == null) {
            notification.setSentAt(now);
        }
        if (notification.getStatus() == null) {
            notification.setStatus("UNREAD");
        }
        if (notification.getChannel() == null) {
            notification.setChannel("IN_APP");
        }
        if (notification.getSeverity() == null) {
            notification.setSeverity("MEDIUM");
        }

        UUID recipientId = notification.getRecipientUser() != null ? notification.getRecipientUser().getId() : null;

        String sql = """
            INSERT INTO notifications (
                id, recipient_user_id, recipient_role, title, message,
                notification_type, incident_id, severity, channel,
                status, is_read, dedup_key, sent_at, read_at, created_at,
                complaint_id, complaint_number
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        jdbcTemplate.update(
                sql,
                notification.getId(),
                recipientId,
                notification.getRecipientRole(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getNotificationType(),
                notification.getIncidentId(),
                notification.getSeverity(),
                notification.getChannel(),
                notification.getStatus(),
                notification.isRead(),
                notification.getDedupKey(),
                Timestamp.from(notification.getSentAt()),
                notification.getReadAt() != null ? Timestamp.from(notification.getReadAt()) : null,
                Timestamp.from(notification.getCreatedAt()),
                notification.getComplaintId(),
                notification.getComplaintNumber()
        );

        return findById(notification.getId()).orElse(NotificationDTO.fromEntity(notification));
    }

    @Override
    public Optional<NotificationDTO> findById(UUID id) {
        String sql = """
            SELECT n.*, u.username AS recipient_username
            FROM notifications n
            LEFT JOIN users u ON n.recipient_user_id = u.id
            WHERE n.id = ?
        """;
        try {
            NotificationDTO dto = jdbcTemplate.queryForObject(sql, rowMapper, id);
            return Optional.ofNullable(dto);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<NotificationDTO> findByRecipient(
            UUID userId,
            List<String> roles,
            Boolean unreadOnly,
            String type,
            String severity,
            int offset,
            int limit
    ) {
        return findByRecipient(userId, roles, unreadOnly, type, severity, false, offset, limit);
    }

    @Override
    public List<NotificationDTO> findByRecipient(
            UUID userId,
            List<String> roles,
            Boolean unreadOnly,
            String type,
            String severity,
            Boolean directOnly,
            int offset,
            int limit
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT n.*, u.username AS recipient_username
            FROM notifications n
            LEFT JOIN users u ON n.recipient_user_id = u.id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (Boolean.TRUE.equals(directOnly)) {
            sql.append(" AND n.recipient_user_id = ?");
            params.add(userId);
        } else if (userId != null) {
            if (roles != null && !roles.isEmpty()) {
                String rolePlaceholders = String.join(",", Collections.nCopies(roles.size(), "?"));
                sql.append(" AND (n.recipient_user_id = ? OR (n.recipient_user_id IS NULL AND (n.recipient_role IN (")
                   .append(rolePlaceholders).append(") OR UPPER(REPLACE(n.recipient_role, 'ROLE_', '')) IN (")
                   .append(rolePlaceholders).append("))))");
                params.add(userId);
                params.addAll(roles);
                for (String r : roles) {
                    params.add(r.toUpperCase().replace("ROLE_", ""));
                }
            } else {
                sql.append(" AND n.recipient_user_id = ?");
                params.add(userId);
            }
        } else if (roles != null && !roles.isEmpty()) {
            String rolePlaceholders = String.join(",", Collections.nCopies(roles.size(), "?"));
            sql.append(" AND (n.recipient_role IN (").append(rolePlaceholders)
               .append(") OR UPPER(REPLACE(n.recipient_role, 'ROLE_', '')) IN (")
               .append(rolePlaceholders).append("))");
            params.addAll(roles);
            for (String r : roles) {
                params.add(r.toUpperCase().replace("ROLE_", ""));
            }
        }

        if (Boolean.TRUE.equals(unreadOnly)) {
            sql.append(" AND (n.is_read = false OR UPPER(n.status) = 'UNREAD')");
        }

        if (type != null && !type.isBlank()) {
            sql.append(" AND UPPER(n.notification_type) = UPPER(?)");
            params.add(type.trim());
        }

        if (severity != null && !severity.isBlank()) {
            sql.append(" AND UPPER(n.severity) = UPPER(?)");
            params.add(severity.trim());
        }

        sql.append(" ORDER BY n.created_at DESC LIMIT ? OFFSET ?");
        params.add(Math.max(1, Math.min(limit, 100)));
        params.add(Math.max(0, offset));

        return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
    }

    @Override
    public long countUnread(UUID userId, List<String> roles) {
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(*)
            FROM notifications n
            WHERE (n.is_read = false OR UPPER(n.status) = 'UNREAD')
        """);

        List<Object> params = new ArrayList<>();

        if (userId != null) {
            if (roles != null && !roles.isEmpty()) {
                String rolePlaceholders = String.join(",", Collections.nCopies(roles.size(), "?"));
                sql.append(" AND (n.recipient_user_id = ? OR (n.recipient_user_id IS NULL AND (n.recipient_role IN (")
                   .append(rolePlaceholders).append(") OR UPPER(REPLACE(n.recipient_role, 'ROLE_', '')) IN (")
                   .append(rolePlaceholders).append("))))");
                params.add(userId);
                params.addAll(roles);
                for (String r : roles) {
                    params.add(r.toUpperCase().replace("ROLE_", ""));
                }
            } else {
                sql.append(" AND n.recipient_user_id = ?");
                params.add(userId);
            }
        } else if (roles != null && !roles.isEmpty()) {
            String rolePlaceholders = String.join(",", Collections.nCopies(roles.size(), "?"));
            sql.append(" AND (n.recipient_role IN (").append(rolePlaceholders)
               .append(") OR UPPER(REPLACE(n.recipient_role, 'ROLE_', '')) IN (")
               .append(rolePlaceholders).append("))");
            params.addAll(roles);
            for (String r : roles) {
                params.add(r.toUpperCase().replace("ROLE_", ""));
            }
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    @Override
    public int markAsRead(UUID id, UUID userId) {
        Instant now = Instant.now();
        if (userId != null) {
            String sql = """
                UPDATE notifications
                SET is_read = true, status = 'READ', read_at = ?
                WHERE id = ? AND (recipient_user_id = ? OR recipient_user_id IS NULL)
            """;
            return jdbcTemplate.update(sql, Timestamp.from(now), id, userId);
        } else {
            String sql = "UPDATE notifications SET is_read = true, status = 'READ', read_at = ? WHERE id = ?";
            return jdbcTemplate.update(sql, Timestamp.from(now), id);
        }
    }

    @Override
    public int markAsUnread(UUID id, UUID userId) {
        if (userId != null) {
            String sql = """
                UPDATE notifications
                SET is_read = false, status = 'UNREAD', read_at = NULL
                WHERE id = ? AND (recipient_user_id = ? OR recipient_user_id IS NULL)
            """;
            return jdbcTemplate.update(sql, id, userId);
        } else {
            String sql = "UPDATE notifications SET is_read = false, status = 'UNREAD', read_at = NULL WHERE id = ?";
            return jdbcTemplate.update(sql, id);
        }
    }

    @Override
    public int markAllAsRead(UUID userId, List<String> roles) {
        Instant now = Instant.now();
        StringBuilder sql = new StringBuilder("""
            UPDATE notifications
            SET is_read = true, status = 'READ', read_at = ?
            WHERE (is_read = false OR UPPER(status) = 'UNREAD')
        """);

        List<Object> params = new ArrayList<>();
        params.add(Timestamp.from(now));

        if (userId != null) {
            if (roles != null && !roles.isEmpty()) {
                String rolePlaceholders = String.join(",", Collections.nCopies(roles.size(), "?"));
                sql.append(" AND (recipient_user_id = ? OR (recipient_user_id IS NULL AND (recipient_role IN (")
                   .append(rolePlaceholders).append(") OR UPPER(REPLACE(recipient_role, 'ROLE_', '')) IN (")
                   .append(rolePlaceholders).append("))))");
                params.add(userId);
                params.addAll(roles);
                for (String r : roles) {
                    params.add(r.toUpperCase().replace("ROLE_", ""));
                }
            } else {
                sql.append(" AND recipient_user_id = ?");
                params.add(userId);
            }
        }

        return jdbcTemplate.update(sql.toString(), params.toArray());
    }

    @Override
    public boolean deleteById(UUID id, UUID userId) {
        if (userId != null) {
            String sql = "DELETE FROM notifications WHERE id = ? AND (recipient_user_id = ? OR recipient_user_id IS NULL)";
            return jdbcTemplate.update(sql, id, userId) > 0;
        } else {
            String sql = "DELETE FROM notifications WHERE id = ?";
            return jdbcTemplate.update(sql, id) > 0;
        }
    }

    @Override
    public boolean existsByDedupKey(String dedupKey) {
        if (dedupKey == null || dedupKey.isBlank()) {
            return false;
        }
        String sql = "SELECT COUNT(*) FROM notifications WHERE dedup_key = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, dedupKey);
        return count != null && count > 0;
    }
}
