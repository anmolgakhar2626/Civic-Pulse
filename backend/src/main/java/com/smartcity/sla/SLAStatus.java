package com.smartcity.sla;

/**
 * SLA compliance statuses for incident resolution and response deadlines.
 * Prompt 4 core statuses: ON_TRACK, WARNING, BREACHED, COMPLETED.
 * Legacy aliases AT_RISK (mapped to WARNING) and RESOLVED (mapped to COMPLETED)
 * are retained for backward compatibility.
 */
public enum SLAStatus {
    ON_TRACK,
    WARNING,
    AT_RISK,
    BREACHED,
    COMPLETED,
    RESOLVED,
    PAUSED;

    public boolean isTerminal() {
        return this == COMPLETED || this == RESOLVED;
    }

    public boolean isWarning() {
        return this == WARNING || this == AT_RISK;
    }

    public boolean isBreached() {
        return this == BREACHED;
    }

    public static SLAStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return ON_TRACK;
        }
        String normalized = value.trim().toUpperCase();
        if ("AT_RISK".equals(normalized)) {
            return WARNING;
        }
        if ("RESOLVED".equals(normalized)) {
            return COMPLETED;
        }
        try {
            return SLAStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return ON_TRACK;
        }
    }
}
