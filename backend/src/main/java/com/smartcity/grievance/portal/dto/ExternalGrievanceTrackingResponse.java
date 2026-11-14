package com.smartcity.grievance.portal.dto;

import com.smartcity.grievance.portal.AcknowledgementDocument;
import com.smartcity.grievance.portal.ExternalGrievance;
import com.smartcity.grievance.portal.GrievanceStatusHistory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ExternalGrievanceTrackingResponse {

    private UUID grievanceId;
    private UUID complaintId;
    private String portalCode;
    private String portalName;
    private String acknowledgementNumber;
    private String idempotencyKey;
    private String externalStatus;
    private String statusDescription;
    private String officerInCharge;
    private Instant targetResolutionDate;
    private Instant submittedAt;
    private Instant lastSyncedAt;
    private boolean isSandbox;
    private List<GrievanceStatusHistory> timeline;
    private List<AcknowledgementDocument> documents;

    public ExternalGrievanceTrackingResponse() {}

    public ExternalGrievanceTrackingResponse(ExternalGrievance g, String portalName, boolean isSandbox,
                                            List<GrievanceStatusHistory> timeline,
                                            List<AcknowledgementDocument> documents) {
        if (g != null) {
            this.grievanceId = g.getId();
            this.complaintId = g.getComplaintId();
            this.portalCode = g.getPortalCode();
            this.acknowledgementNumber = g.getAcknowledgementNumber();
            this.idempotencyKey = g.getIdempotencyKey();
            this.externalStatus = g.getExternalStatus();
            this.statusDescription = g.getStatusDescription();
            this.officerInCharge = g.getOfficerInCharge();
            this.targetResolutionDate = g.getTargetResolutionDate();
            this.submittedAt = g.getSubmittedAt();
            this.lastSyncedAt = g.getLastSyncedAt();
        }
        this.portalName = portalName;
        this.isSandbox = isSandbox;
        this.timeline = timeline != null ? timeline : List.of();
        this.documents = documents != null ? documents : List.of();
    }

    public UUID getGrievanceId() { return grievanceId; }
    public void setGrievanceId(UUID grievanceId) { this.grievanceId = grievanceId; }

    public UUID getComplaintId() { return complaintId; }
    public void setComplaintId(UUID complaintId) { this.complaintId = complaintId; }

    public String getPortalCode() { return portalCode; }
    public void setPortalCode(String portalCode) { this.portalCode = portalCode; }

    public String getPortalName() { return portalName; }
    public void setPortalName(String portalName) { this.portalName = portalName; }

    public String getAcknowledgementNumber() { return acknowledgementNumber; }
    public void setAcknowledgementNumber(String acknowledgementNumber) { this.acknowledgementNumber = acknowledgementNumber; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getExternalStatus() { return externalStatus; }
    public void setExternalStatus(String externalStatus) { this.externalStatus = externalStatus; }

    public String getStatusDescription() { return statusDescription; }
    public void setStatusDescription(String statusDescription) { this.statusDescription = statusDescription; }

    public String getOfficerInCharge() { return officerInCharge; }
    public void setOfficerInCharge(String officerInCharge) { this.officerInCharge = officerInCharge; }

    public Instant getTargetResolutionDate() { return targetResolutionDate; }
    public void setTargetResolutionDate(Instant targetResolutionDate) { this.targetResolutionDate = targetResolutionDate; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public boolean isSandbox() { return isSandbox; }
    public boolean getIsSandbox() { return isSandbox; }
    public void setSandbox(boolean sandbox) { isSandbox = sandbox; }
    public void setIsSandbox(boolean sandbox) { isSandbox = sandbox; }

    public List<GrievanceStatusHistory> getTimeline() { return timeline; }
    public void setTimeline(List<GrievanceStatusHistory> timeline) { this.timeline = timeline; }

    public List<AcknowledgementDocument> getDocuments() { return documents; }
    public void setDocuments(List<AcknowledgementDocument> documents) { this.documents = documents; }
}
