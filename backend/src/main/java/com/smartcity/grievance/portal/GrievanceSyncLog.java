package com.smartcity.grievance.portal;

import java.time.Instant;
import java.util.UUID;

public class GrievanceSyncLog {

    private UUID id;
    private UUID externalGrievanceId;
    private String portalCode;
    private String syncType; // SUBMISSION, STATUS_POLL, RETRY
    private String status; // SUCCESS, FAILED, TIMEOUT
    private String idempotencyKey;
    private String requestPayload;
    private String responsePayload;
    private String errorMessage;
    private int retryCount;
    private Instant syncTimestamp = Instant.now();

    public GrievanceSyncLog() {}

    public GrievanceSyncLog(UUID externalGrievanceId, String portalCode, String syncType,
                            String status, String idempotencyKey, String requestPayload,
                            String responsePayload, String errorMessage, int retryCount) {
        this.id = UUID.randomUUID();
        this.externalGrievanceId = externalGrievanceId;
        this.portalCode = portalCode;
        this.syncType = syncType;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.requestPayload = requestPayload;
        this.responsePayload = responsePayload;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.syncTimestamp = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getExternalGrievanceId() { return externalGrievanceId; }
    public void setExternalGrievanceId(UUID externalGrievanceId) { this.externalGrievanceId = externalGrievanceId; }

    public String getPortalCode() { return portalCode; }
    public void setPortalCode(String portalCode) { this.portalCode = portalCode; }

    public String getSyncType() { return syncType; }
    public void setSyncType(String syncType) { this.syncType = syncType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getRequestPayload() { return requestPayload; }
    public void setRequestPayload(String requestPayload) { this.requestPayload = requestPayload; }

    public String getResponsePayload() { return responsePayload; }
    public void setResponsePayload(String responsePayload) { this.responsePayload = responsePayload; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public Instant getSyncTimestamp() { return syncTimestamp; }
    public void setSyncTimestamp(Instant syncTimestamp) { this.syncTimestamp = syncTimestamp; }
}
