package com.smartcity.grievance.portal;

import java.time.Instant;

public class GrievanceSubmissionResult {

    private final boolean success;
    private final String acknowledgementNumber;
    private final String externalStatus;
    private final String statusDescription;
    private final String officerInCharge;
    private final Instant targetResolutionDate;
    private final String documentReference;
    private final String documentUrl;
    private final String errorMessage;
    private final boolean timeout;

    public GrievanceSubmissionResult(boolean success, String acknowledgementNumber, String externalStatus,
                                     String statusDescription, String officerInCharge, Instant targetResolutionDate,
                                     String documentReference, String documentUrl, String errorMessage, boolean timeout) {
        this.success = success;
        this.acknowledgementNumber = acknowledgementNumber;
        this.externalStatus = externalStatus;
        this.statusDescription = statusDescription;
        this.officerInCharge = officerInCharge;
        this.targetResolutionDate = targetResolutionDate;
        this.documentReference = documentReference;
        this.documentUrl = documentUrl;
        this.errorMessage = errorMessage;
        this.timeout = timeout;
    }

    public static GrievanceSubmissionResult success(String ackNumber, String status, String desc,
                                                    String officer, Instant targetDate, String docRef, String docUrl) {
        return new GrievanceSubmissionResult(true, ackNumber, status, desc, officer, targetDate, docRef, docUrl, null, false);
    }

    public static GrievanceSubmissionResult failure(String errorMessage) {
        return new GrievanceSubmissionResult(false, null, "FAILED", errorMessage, null, null, null, null, errorMessage, false);
    }

    public static GrievanceSubmissionResult timeout(String errorMessage) {
        return new GrievanceSubmissionResult(false, null, "TIMEOUT", errorMessage, null, null, null, null, errorMessage, true);
    }

    public boolean isSuccess() { return success; }
    public String getAcknowledgementNumber() { return acknowledgementNumber; }
    public String getExternalStatus() { return externalStatus; }
    public String getStatusDescription() { return statusDescription; }
    public String getOfficerInCharge() { return officerInCharge; }
    public Instant getTargetResolutionDate() { return targetResolutionDate; }
    public String getDocumentReference() { return documentReference; }
    public String getDocumentUrl() { return documentUrl; }
    public String getErrorMessage() { return errorMessage; }
    public boolean isTimeout() { return timeout; }
}
