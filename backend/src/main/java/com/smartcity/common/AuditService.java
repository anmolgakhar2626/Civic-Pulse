package com.smartcity.common;

import java.util.List;
import java.util.UUID;

public interface AuditService {

    AuditLog record(String entityType, String entityId, String action, String performedBy, String detailsJson, String ipAddress);

    List<AuditLog> getRecentLogs(int limit);

    List<AuditLog> getLogsByEntity(String entityType, String entityId);
}
