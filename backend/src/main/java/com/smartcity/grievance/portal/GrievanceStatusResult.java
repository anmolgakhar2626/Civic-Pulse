package com.smartcity.grievance.portal;

import java.time.Instant;

public class GrievanceStatusResult {

    private final boolean success;
    private final String acknowledgementNumber;
    private final String externalStatus;
    private final String remarks;
    private final String actionTakenBy;
    private final Instant timestamp;
    private final String errorMessage;
    private final boolean timeout;

    public GrievanceStatusResult(boolean success, String acknowledgementNumber, String externalStatus,
                                 String remarks, String actionTakenBy, Instant timestamp, String errorMessage, boolean timeout) {
        this.success = success;
        this.acknowledgementNumber = acknowledgementNumber;
        this.externalStatus = externalStatus;
        this.remarks = remarks;
        this.actionTakenBy = actionTakenBy;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.errorMessage = errorMessage;
        this.timeout = timeout;
    }

    public static GrievanceStatusResult success(String ackNumber, String status, String remarks, String actionTakenBy) {
        return new GrievanceStatusResult(true, ackNumber, status, remarks, actionTakenBy, Instant.now(), null, false);
    }

    public static GrievanceStatusResult failure(String errorMessage) {
        return new GrievanceStatusResult(false, null, null, null, null, Instant.now(), errorMessage, false);
    }

    public static GrievanceStatusResult timeout(String errorMessage) {
        return new GrievanceStatusResult(false, null, null, null, null, Instant.now(), errorMessage, true);
    }

    public boolean isSuccess() { return success; }
    public String getAcknowledgementNumber() { return acknowledgementNumber; }
    public String getExternalStatus() { return externalStatus; }
    public String getRemarks() { return remarks; }
    public String getActionTakenBy() { return actionTakenBy; }
    public Instant getTimestamp() { return timestamp; }
    public String getErrorMessage() { return errorMessage; }
    public boolean isTimeout() { return timeout; }
}
