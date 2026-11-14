package com.smartcity.sla.dto;

import com.smartcity.priority.PriorityTier;
import com.smartcity.sla.EscalationLevel;
import com.smartcity.sla.SLARecord;
import com.smartcity.sla.SLAStatus;

import java.time.Instant;
import java.util.UUID;

public class SLAResponse {

    private UUID id;
    private String incidentId;
    private UUID incidentUuid;
    private String incidentNumber;
    private String incidentTitle;
    private String category;
    private String severity;
    private UUID policyId;
    private PriorityTier priorityTier;
    private Integer targetMinutes;
    private Instant startedAt;
    private Instant deadlineAt;
    private Instant responseDeadline;
    private Instant resolutionDeadline;
    private Instant firstRespondedAt;
    private Instant acknowledgedAt;
    private Instant resolvedAt;
    private SLAStatus status;
    private Long remainingMinutes;
    private Long remainingResponseMinutes;
    private Long remainingResolutionMinutes;
    private EscalationLevel escalationLevel;
    private Instant lastEscalatedAt;
    private Boolean breached;
    private Instant breachedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public SLAResponse() {}

    public static SLAResponse fromEntity(SLARecord record) {
        if (record == null) {
            return null;
        }

        SLAResponse dto = new SLAResponse();
        dto.setId(record.getId());
        if (record.getIncident() != null) {
            dto.setIncidentUuid(record.getIncident().getId());
            dto.setIncidentNumber(record.getIncident().getIncidentNumber());
            dto.setIncidentTitle(record.getIncident().getTitle());
            dto.setCategory(record.getIncident().getCategory());
            dto.setSeverity(record.getIncident().getSeverityIndicator());
            dto.setIncidentId(record.getIncident().getIncidentNumber() != null 
                    ? record.getIncident().getIncidentNumber() 
                    : record.getIncident().getId().toString());
        }
        if (record.getPolicy() != null) {
            dto.setPolicyId(record.getPolicy().getId());
        }
        dto.setPriorityTier(record.getPriorityTier());
        dto.setTargetMinutes(record.getTargetMinutes());
        dto.setStartedAt(record.getStartedAt());
        dto.setDeadlineAt(record.getDeadlineAt());
        dto.setResponseDeadline(record.getResponseDeadline());
        dto.setResolutionDeadline(record.getResolutionDeadline() != null ? record.getResolutionDeadline() : record.getDeadlineAt());
        dto.setFirstRespondedAt(record.getFirstRespondedAt() != null ? record.getFirstRespondedAt() : record.getAcknowledgedAt());
        dto.setAcknowledgedAt(dto.getFirstRespondedAt());
        dto.setResolvedAt(record.getResolvedAt());
        dto.setStatus(record.getStatus());
        dto.setRemainingMinutes(record.getRemainingMinutes());
        dto.setRemainingResolutionMinutes(record.getRemainingMinutes());
        dto.setEscalationLevel(record.getEscalationLevel());
        dto.setLastEscalatedAt(record.getLastEscalatedAt());
        dto.setBreached(record.getBreached());
        dto.setBreachedAt(record.getBreachedAt());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public UUID getIncidentUuid() { return incidentUuid; }
    public void setIncidentUuid(UUID incidentUuid) { this.incidentUuid = incidentUuid; }

    public String getIncidentNumber() { return incidentNumber; }
    public void setIncidentNumber(String incidentNumber) { this.incidentNumber = incidentNumber; }

    public String getIncidentTitle() { return incidentTitle; }
    public void setIncidentTitle(String incidentTitle) { this.incidentTitle = incidentTitle; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public UUID getPolicyId() { return policyId; }
    public void setPolicyId(UUID policyId) { this.policyId = policyId; }

    public PriorityTier getPriorityTier() { return priorityTier; }
    public void setPriorityTier(PriorityTier priorityTier) { this.priorityTier = priorityTier; }

    public Integer getTargetMinutes() { return targetMinutes; }
    public void setTargetMinutes(Integer targetMinutes) { this.targetMinutes = targetMinutes; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(Instant deadlineAt) { this.deadlineAt = deadlineAt; }

    public Instant getResponseDeadline() { return responseDeadline; }
    public void setResponseDeadline(Instant responseDeadline) { this.responseDeadline = responseDeadline; }

    public Instant getResolutionDeadline() { return resolutionDeadline; }
    public void setResolutionDeadline(Instant resolutionDeadline) { this.resolutionDeadline = resolutionDeadline; }

    public Instant getFirstRespondedAt() { return firstRespondedAt; }
    public void setFirstRespondedAt(Instant firstRespondedAt) { this.firstRespondedAt = firstRespondedAt; }

    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(Instant acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public SLAStatus getStatus() { return status; }
    public void setStatus(SLAStatus status) { this.status = status; }

    public Long getRemainingMinutes() { return remainingMinutes; }
    public void setRemainingMinutes(Long remainingMinutes) { this.remainingMinutes = remainingMinutes; }

    public Long getRemainingResponseMinutes() { return remainingResponseMinutes; }
    public void setRemainingResponseMinutes(Long remainingResponseMinutes) { this.remainingResponseMinutes = remainingResponseMinutes; }

    public Long getRemainingResolutionMinutes() { return remainingResolutionMinutes; }
    public void setRemainingResolutionMinutes(Long remainingResolutionMinutes) { this.remainingResolutionMinutes = remainingResolutionMinutes; }

    public EscalationLevel getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(EscalationLevel escalationLevel) { this.escalationLevel = escalationLevel; }

    public Instant getLastEscalatedAt() { return lastEscalatedAt; }
    public void setLastEscalatedAt(Instant lastEscalatedAt) { this.lastEscalatedAt = lastEscalatedAt; }

    public Boolean getBreached() { return breached; }
    public void setBreached(Boolean breached) { this.breached = breached; }

    public Instant getBreachedAt() { return breachedAt; }
    public void setBreachedAt(Instant breachedAt) { this.breachedAt = breachedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
