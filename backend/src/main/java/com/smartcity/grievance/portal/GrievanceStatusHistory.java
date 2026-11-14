package com.smartcity.grievance.portal;

import java.time.Instant;
import java.util.UUID;

public class GrievanceStatusHistory {

    private UUID id;
    private UUID externalGrievanceId;
    private String status;
    private String remarks;
    private String actionTakenBy;
    private Instant eventTimestamp = Instant.now();
    private String rawPayload;

    public GrievanceStatusHistory() {}

    public GrievanceStatusHistory(UUID externalGrievanceId, String status, String remarks,
                                  String actionTakenBy, String rawPayload) {
        this.id = UUID.randomUUID();
        this.externalGrievanceId = externalGrievanceId;
        this.status = status;
        this.remarks = remarks;
        this.actionTakenBy = actionTakenBy;
        this.rawPayload = rawPayload;
        this.eventTimestamp = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getExternalGrievanceId() { return externalGrievanceId; }
    public void setExternalGrievanceId(UUID externalGrievanceId) { this.externalGrievanceId = externalGrievanceId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getActionTakenBy() { return actionTakenBy; }
    public void setActionTakenBy(String actionTakenBy) { this.actionTakenBy = actionTakenBy; }

    public Instant getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(Instant eventTimestamp) { this.eventTimestamp = eventTimestamp; }

    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }
}
