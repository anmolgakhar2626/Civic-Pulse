package com.smartcity.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    // Regex pattern to redact passwords, JWT tokens, and credentials from audit metadata
    private static final Pattern SENSITIVE_KEY_PATTERN = Pattern.compile(
            "\"(password|passwordHash|token|jwt|secret|apiKey|authorization)\"\\s*:\\s*\"[^\"]*\"",
            Pattern.CASE_INSENSITIVE
    );

    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog record(String entityType, String entityId, String action, String performedBy, String detailsJson, String ipAddress) {
        try {
            String sanitizedDetails = sanitize(detailsJson);
            AuditLog auditLog = new AuditLog(
                    entityType != null ? entityType : "SYSTEM",
                    entityId,
                    action != null ? action : "OPERATION",
                    performedBy != null ? performedBy : "ANONYMOUS",
                    sanitizedDetails,
                    ipAddress != null ? ipAddress : "127.0.0.1"
            );
            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to write audit log record for entity {}: {}", entityType, e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getRecentLogs(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return auditLogRepository.findTop50ByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByEntity(String entityType, String entityId) {
        if (entityId != null) {
            return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
        }
        return auditLogRepository.findByEntityTypeOrderByCreatedAtDesc(entityType);
    }

    private String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return "{}";
        }
        return SENSITIVE_KEY_PATTERN.matcher(input).replaceAll("\"$1\":\"[REDACTED]\"");
    }
}
