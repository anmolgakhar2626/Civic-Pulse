package com.smartcity.infrastructure.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(description = "Request payload to update an existing infrastructure asset")
public class UpdateInfrastructureRequest {

    @Size(max = 150, message = "Name must not exceed 150 characters")
    @Schema(description = "Infrastructure asset name", example = "Sector 22 Civil Hospital & Emergency Unit")
    private String name;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    @Schema(description = "Category: ROAD, BRIDGE, HOSPITAL, WATER_PIPELINE, DRAINAGE, SUBSTATION, SHELTER", example = "HOSPITAL")
    private String category;

    @Size(max = 50, message = "Status must not exceed 50 characters")
    @Schema(description = "Operational status: OPERATIONAL, DEGRADED, OFFLINE, UNDER_MAINTENANCE", example = "OPERATIONAL")
    private String status;

    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
    @Schema(description = "Latitude coordinate", example = "30.7320")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
    @Schema(description = "Longitude coordinate", example = "76.7710")
    private Double longitude;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    @Schema(description = "Physical address or landmark", example = "Sector 22-A Medical Complex, Chandigarh")
    private String address;

    @Size(max = 50, message = "Importance level must not exceed 50 characters")
    @Schema(description = "Importance level: CRITICAL, PRIMARY, SECONDARY", example = "CRITICAL")
    private String importanceLevel;

    @Size(max = 50, message = "Maintenance priority must not exceed 50 characters")
    @Schema(description = "Maintenance priority: LOW, MEDIUM, HIGH, CRITICAL", example = "HIGH")
    private String maintenancePriority;

    @Schema(description = "Timestamp when the asset was last inspected")
    private Instant lastInspectedAt;

    @Schema(description = "Operational notes, condition observations, or specifications")
    private String notes;

    public UpdateInfrastructureRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getImportanceLevel() { return importanceLevel; }
    public void setImportanceLevel(String importanceLevel) { this.importanceLevel = importanceLevel; }

    public String getMaintenancePriority() { return maintenancePriority; }
    public void setMaintenancePriority(String maintenancePriority) { this.maintenancePriority = maintenancePriority; }

    public Instant getLastInspectedAt() { return lastInspectedAt; }
    public void setLastInspectedAt(Instant lastInspectedAt) { this.lastInspectedAt = lastInspectedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

