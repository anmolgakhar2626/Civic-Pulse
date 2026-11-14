package com.smartcity.incident.dto;

import com.smartcity.incident.Incident;
import com.smartcity.incident.IncidentSource;
import com.smartcity.incident.IncidentStatus;
import com.smartcity.priority.PriorityTier;
import com.smartcity.priority.PublicExposure;
import com.smartcity.priority.Severity;

import java.time.Instant;
import java.util.UUID;

public class IncidentResponse {

    private UUID id;
    private String incidentNumber;
    private String title;
    private String category;
    private String incidentType;
    private String description;
    private IncidentSource source;
    private Double latitude;
    private Double longitude;
    private String locationName;
    private String address;
    private Instant reportedAt;
    private Instant detectedAt;
    private IncidentStatus status;
    private UUID reportedBy;
    private String reportedByName;
    private String reportedByUsername;
    private UUID assignedTo;
    private String assignedToName;
    private String assignedToUsername;
    private UUID departmentId;
    private String departmentName;
    private String departmentCode;
    private UUID assignedTeamId;
    private String assignedTeamName;
    private UUID createdByUserId;
    private String createdByUsername;
    private String priority;
    private String assignedTier;
    private PriorityTier tier;
    private String priorityLabel;
    private String priorityReason;
    private Severity severity;
    private PublicExposure publicExposure;
    private String safetyImpact;
    private Instant priorityEvaluatedAt;
    private String priorityRuleId;
    private Integer slaMinutes;
    private Instant slaDeadline;
    private String escalationState;
    private Instant resolutionTime;
    private String resolutionNotes;
    private String evidenceUrl;
    private String severityIndicator;
    private String populationExposure;
    private String infrastructureImportance;
    private Instant assignedAt;
    private String assignedBy;
    private String assignmentReason;
    private Instant createdAt;
    private Instant updatedAt;

    public IncidentResponse() {}

    public static IncidentResponse fromEntity(Incident incident) {
        if (incident == null) {
            return null;
        }

        IncidentResponse response = new IncidentResponse();
        response.setId(incident.getId());
        response.setIncidentNumber(incident.getIncidentNumber());
        response.setTitle(incident.getTitle());
        response.setCategory(incident.getCategory());
        response.setIncidentType(incident.getIncidentType());
        response.setDescription(incident.getDescription());
        response.setSource(incident.getSource());
        response.setLatitude(incident.getLatitude());
        response.setLongitude(incident.getLongitude());
        response.setLocationName(incident.getLocationName());
        response.setAddress(incident.getAddress());
        response.setReportedAt(incident.getReportedAt());
        response.setDetectedAt(incident.getDetectedAt());
        response.setStatus(incident.getStatus());

        if (incident.getReportedBy() != null) {
            response.setReportedBy(incident.getReportedBy().getId());
            response.setReportedByName(incident.getReportedBy().getFullName());
            response.setReportedByUsername(incident.getReportedBy().getUsername());
            response.setCreatedByUserId(incident.getReportedBy().getId());
            response.setCreatedByUsername(incident.getReportedBy().getUsername());
        } else if (incident.getCreatedBy() != null) {
            response.setReportedBy(incident.getCreatedBy().getId());
            response.setReportedByName(incident.getCreatedBy().getFullName());
            response.setReportedByUsername(incident.getCreatedBy().getUsername());
            response.setCreatedByUserId(incident.getCreatedBy().getId());
            response.setCreatedByUsername(incident.getCreatedBy().getUsername());
        }

        if (incident.getAssignedTo() != null) {
            response.setAssignedTo(incident.getAssignedTo().getId());
            response.setAssignedToName(incident.getAssignedTo().getFullName());
            response.setAssignedToUsername(incident.getAssignedTo().getUsername());
        }

        if (incident.getAssignedDepartment() != null) {
            response.setDepartmentId(incident.getAssignedDepartment().getId());
            response.setDepartmentName(incident.getAssignedDepartment().getName());
            response.setDepartmentCode(incident.getAssignedDepartment().getCode());
        }

        if (incident.getAssignedTeam() != null) {
            response.setAssignedTeamId(incident.getAssignedTeam().getId());
            response.setAssignedTeamName(incident.getAssignedTeam().getName());
        }

        if (incident.getCreatedBy() != null) {
            response.setCreatedByUserId(incident.getCreatedBy().getId());
            response.setCreatedByUsername(incident.getCreatedBy().getUsername());
        }

        if (incident.getPriority() != null) {
            response.setPriority(incident.getPriority().name());
        }
        if (incident.getAssignedTier() != null) {
            response.setAssignedTier(incident.getAssignedTier().name());
        }

        response.setTier(incident.getTier());
        response.setPriorityLabel(incident.getPriorityLabel());
        response.setPriorityReason(incident.getPriorityReason());
        response.setSeverity(incident.getSeverity());
        response.setPublicExposure(incident.getPublicExposure());
        response.setSafetyImpact(incident.getSafetyImpact());
        response.setPriorityEvaluatedAt(incident.getPriorityEvaluatedAt());
        response.setPriorityRuleId(incident.getPriorityRuleId());

        response.setSlaMinutes(incident.getSlaMinutes());
        response.setSlaDeadline(incident.getSlaDeadline());

        response.setEscalationState(incident.getEscalationState());
        response.setResolutionTime(incident.getResolutionTime());
        response.setResolutionNotes(incident.getResolutionNotes());
        response.setEvidenceUrl(incident.getEvidenceUrl());
        response.setSeverityIndicator(incident.getSeverityIndicator());
        response.setPopulationExposure(incident.getPopulationExposure());
        response.setInfrastructureImportance(incident.getInfrastructureImportance());
        response.setAssignedAt(incident.getAssignedAt());
        response.setAssignedBy(incident.getAssignedBy());
        response.setAssignmentReason(incident.getAssignmentReason());
        response.setCreatedAt(incident.getCreatedAt());
        response.setUpdatedAt(incident.getUpdatedAt());

        return response;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getIncidentNumber() { return incidentNumber; }
    public void setIncidentNumber(String incidentNumber) { this.incidentNumber = incidentNumber; }

    public String getIncidentType() { return incidentType; }
    public void setIncidentType(String incidentType) { this.incidentType = incidentType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public IncidentSource getSource() { return source; }
    public void setSource(IncidentSource source) { this.source = source; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Instant getReportedAt() { return reportedAt; }
    public void setReportedAt(Instant reportedAt) { this.reportedAt = reportedAt; }

    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }

    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }

    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public UUID getAssignedTeamId() { return assignedTeamId; }
    public void setAssignedTeamId(UUID assignedTeamId) { this.assignedTeamId = assignedTeamId; }

    public String getAssignedTeamName() { return assignedTeamName; }
    public void setAssignedTeamName(String assignedTeamName) { this.assignedTeamName = assignedTeamName; }

    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID createdByUserId) { this.createdByUserId = createdByUserId; }

    public String getCreatedByUsername() { return createdByUsername; }
    public void setCreatedByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getAssignedTier() { return assignedTier; }
    public void setAssignedTier(String assignedTier) { this.assignedTier = assignedTier; }

    public PriorityTier getTier() { return tier; }
    public void setTier(PriorityTier tier) { this.tier = tier; }

    public String getPriorityLabel() { return priorityLabel; }
    public void setPriorityLabel(String priorityLabel) { this.priorityLabel = priorityLabel; }

    public String getPriorityReason() { return priorityReason; }
    public void setPriorityReason(String priorityReason) { this.priorityReason = priorityReason; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public PublicExposure getPublicExposure() { return publicExposure; }
    public void setPublicExposure(PublicExposure publicExposure) { this.publicExposure = publicExposure; }

    public String getSafetyImpact() { return safetyImpact; }
    public void setSafetyImpact(String safetyImpact) { this.safetyImpact = safetyImpact; }

    public Instant getPriorityEvaluatedAt() { return priorityEvaluatedAt; }
    public void setPriorityEvaluatedAt(Instant priorityEvaluatedAt) { this.priorityEvaluatedAt = priorityEvaluatedAt; }

    public String getPriorityRuleId() { return priorityRuleId; }
    public void setPriorityRuleId(String priorityRuleId) { this.priorityRuleId = priorityRuleId; }

    public Integer getSlaMinutes() { return slaMinutes; }
    public void setSlaMinutes(Integer slaMinutes) { this.slaMinutes = slaMinutes; }

    public Instant getSlaDeadline() { return slaDeadline; }
    public void setSlaDeadline(Instant slaDeadline) { this.slaDeadline = slaDeadline; }

    public String getEscalationState() { return escalationState; }
    public void setEscalationState(String escalationState) { this.escalationState = escalationState; }

    public Instant getResolutionTime() { return resolutionTime; }
    public void setResolutionTime(Instant resolutionTime) { this.resolutionTime = resolutionTime; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public String getEvidenceUrl() { return evidenceUrl; }
    public void setEvidenceUrl(String evidenceUrl) { this.evidenceUrl = evidenceUrl; }

    public String getSeverityIndicator() { return severityIndicator; }
    public void setSeverityIndicator(String severityIndicator) { this.severityIndicator = severityIndicator; }

    public String getPopulationExposure() { return populationExposure; }
    public void setPopulationExposure(String populationExposure) { this.populationExposure = populationExposure; }

    public String getInfrastructureImportance() { return infrastructureImportance; }
    public void setInfrastructureImportance(String infrastructureImportance) { this.infrastructureImportance = infrastructureImportance; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public String getAssignmentReason() { return assignmentReason; }
    public void setAssignmentReason(String assignmentReason) { this.assignmentReason = assignmentReason; }

    public String getTitle() { return title != null ? title : incidentType; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category != null ? category : incidentType; }
    public void setCategory(String category) { this.category = category; }

    public String getLocationName() { return locationName != null ? locationName : address; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public UUID getReportedBy() { return reportedBy != null ? reportedBy : createdByUserId; }
    public void setReportedBy(UUID reportedBy) { this.reportedBy = reportedBy; }

    public String getReportedByName() { return reportedByName != null ? reportedByName : createdByUsername; }
    public void setReportedByName(String reportedByName) { this.reportedByName = reportedByName; }

    public String getReportedByUsername() { return reportedByUsername != null ? reportedByUsername : createdByUsername; }
    public void setReportedByUsername(String reportedByUsername) { this.reportedByUsername = reportedByUsername; }

    public UUID getAssignedTo() { return assignedTo; }
    public void setAssignedTo(UUID assignedTo) { this.assignedTo = assignedTo; }

    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }

    public String getAssignedToUsername() { return assignedToUsername; }
    public void setAssignedToUsername(String assignedToUsername) { this.assignedToUsername = assignedToUsername; }
}
