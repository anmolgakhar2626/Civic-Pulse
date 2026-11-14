package com.smartcity.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Incident marker data representation for GIS Map display")
public class MapIncidentMarkerResponse {

    @Schema(description = "Incident UUID")
    private UUID id;

    @Schema(description = "Incident tracking number", example = "INC-20261028-A1B2C3D4")
    private String incidentNumber;

    @Schema(description = "Incident title", example = "Water Main Burst on Sector 22 Corridor")
    private String title;

    @Schema(description = "Incident category", example = "WATER_SUPPLY")
    private String category;

    @Schema(description = "Severity level: CRITICAL, HIGH, MEDIUM, LOW", example = "CRITICAL")
    private String severity;

    @Schema(description = "Operational status: OPEN, IN_PROGRESS, RESOLVED, CLOSED", example = "OPEN")
    private String status;

    @Schema(description = "Latitude coordinate", example = "30.7333")
    private Double latitude;

    @Schema(description = "Longitude coordinate", example = "76.7794")
    private Double longitude;

    @Schema(description = "Location name or landmark", example = "Sector 22 Market Inner Road")
    private String locationName;

    @Schema(description = "Street address", example = "Sector 22-B, Chandigarh")
    private String address;

    @Schema(description = "Reporting timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;

    @Schema(description = "User ID who reported this incident")
    private UUID reportedBy;

    @Schema(description = "Operator or field worker assigned to this incident")
    private UUID assignedTo;

    public MapIncidentMarkerResponse() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getIncidentNumber() { return incidentNumber; }
    public void setIncidentNumber(String incidentNumber) { this.incidentNumber = incidentNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public UUID getReportedBy() { return reportedBy; }
    public void setReportedBy(UUID reportedBy) { this.reportedBy = reportedBy; }

    public UUID getAssignedTo() { return assignedTo; }
    public void setAssignedTo(UUID assignedTo) { this.assignedTo = assignedTo; }
}

