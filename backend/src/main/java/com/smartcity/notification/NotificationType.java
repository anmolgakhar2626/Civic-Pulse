package com.smartcity.notification;

public enum NotificationType {
    INCIDENT_CREATED,
    CRITICAL_ALERT,
    INCIDENT_ASSIGNED,
    STATUS_CHANGED,
    SLA_WARNING,
    SLA_BREACHED,
    INCIDENT_ESCALATED,
    INCIDENT_RESOLVED,
    
    // Legacy aliases for backward compatibility
    SLA_AT_RISK,
    LEVEL_3_ESCALATION,
    INCIDENT_ASSIGNMENT,
    STATUS_UPDATE
}
