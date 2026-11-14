package com.smartcity.incident.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.smartcity.incident.IncidentSource;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public class CreateIncidentRequest {

    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @Size(max = 100, message = "Category cannot exceed 100 characters")
    private String category;

    @Size(max = 50, message = "Severity cannot exceed 50 characters")
    private String severity;

    @Size(max = 50, message = "Status cannot exceed 50 characters")
    private String status;

    @JsonAlias("location_name")
    @Size(max = 255, message = "Location name cannot exceed 255 characters")
    private String locationName;

    @JsonAlias("assigned_to")
    private UUID assignedTo;

    @Size(max = 100, message = "Incident type cannot exceed 100 characters")
    private String incidentType;

    @NotBlank(message = "Description is required")
    private String description;

    private IncidentSource source = IncidentSource.CITIZEN;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    private UUID departmentId;
    private UUID assignedTeamId;
    private UUID createdByUserId;
    private String createdByUsername;
    private Instant reportedAt;
    private Instant detectedAt;
    private String severityIndicator;
    private String evidenceUrl;
    private String populationExposure;
    private String infrastructureImportance;

    public CreateIncidentRequest() {}

    @jakarta.validation.constraints.AssertTrue(message = "Title or incident type is required")
    public boolean isTitleOrIncidentTypeProvided() {
        return (title != null && !title.trim().isEmpty())
                || (incidentType != null && !incidentType.trim().isEmpty())
                || (category != null && !category.trim().isEmpty());
    }

    public String getTitle() {
        if (title != null && !title.isBlank()) return title;
        if (incidentType != null && !incidentType.isBlank()) return incidentType;
        return category;
    }

    public void setTitle(String title) {
        this.title = title;
        if (this.incidentType == null) this.incidentType = title;
    }

    public String getCategory() {
        if (category != null && !category.isBlank()) return category;
        if (incidentType != null && !incidentType.isBlank()) return incidentType;
        return title;
    }

    public void setCategory(String category) {
        this.category = category;
        if (this.incidentType == null) this.incidentType = category;
    }

    public String getSeverity() {
        if (severity != null && !severity.isBlank()) return severity;
        return severityIndicator;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
        if (this.severityIndicator == null) this.severityIndicator = severity;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLocationName() {
        if (locationName != null && !locationName.isBlank()) return locationName;
        return address;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
        if (this.address == null) this.address = locationName;
    }

    public UUID getAssignedTo() { return assignedTo; }
    public void setAssignedTo(UUID assignedTo) { this.assignedTo = assignedTo; }

    public String getIncidentType() {
        if (incidentType != null && !incidentType.isBlank()) return incidentType;
        if (title != null && !title.isBlank()) return title;
        return category;
    }

    public void setIncidentType(String incidentType) {
        this.incidentType = incidentType;
        if (this.title == null) this.title = incidentType;
        if (this.category == null) this.category = incidentType;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public IncidentSource getSource() {
        return source != null ? source : IncidentSource.CITIZEN;
    }
    public void setSource(IncidentSource source) { this.source = source; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getAddress() {
        if (address != null && !address.isBlank()) return address;
        return locationName;
    }
    public void setAddress(String address) {
        this.address = address;
        if (this.locationName == null) this.locationName = address;
    }

    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }

    public UUID getAssignedTeamId() { return assignedTeamId; }
    public void setAssignedTeamId(UUID assignedTeamId) { this.assignedTeamId = assignedTeamId; }

    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID createdByUserId) { this.createdByUserId = createdByUserId; }

    public String getCreatedByUsername() { return createdByUsername; }
    public void setCreatedByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; }

    public Instant getReportedAt() { return reportedAt; }
    public void setReportedAt(Instant reportedAt) { this.reportedAt = reportedAt; }

    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }

    public String getSeverityIndicator() {
        if (severityIndicator != null && !severityIndicator.isBlank()) return severityIndicator;
        return severity;
    }
    public void setSeverityIndicator(String severityIndicator) {
        this.severityIndicator = severityIndicator;
        if (this.severity == null) this.severity = severityIndicator;
    }

    public String getEvidenceUrl() { return evidenceUrl; }
    public void setEvidenceUrl(String evidenceUrl) { this.evidenceUrl = evidenceUrl; }

    public String getPopulationExposure() { return populationExposure; }
    public void setPopulationExposure(String populationExposure) { this.populationExposure = populationExposure; }

    public String getInfrastructureImportance() { return infrastructureImportance; }
    public void setInfrastructureImportance(String infrastructureImportance) { this.infrastructureImportance = infrastructureImportance; }
}
