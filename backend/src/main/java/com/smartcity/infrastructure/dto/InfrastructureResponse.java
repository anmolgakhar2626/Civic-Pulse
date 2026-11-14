package com.smartcity.infrastructure.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Infrastructure asset representation")
public class InfrastructureResponse {

    @Schema(description = "Unique UUID identifier")
    private UUID id;

    @Schema(description = "Asset tracking code", example = "INF-HOSP-01")
    private String code;

    @Schema(description = "Infrastructure asset name", example = "Sector 22 Civil Hospital & Emergency Unit")
    private String name;

    @Schema(description = "Category name", example = "HOSPITAL")
    private String category;

    @Schema(description = "Underlying infrastructure type", example = "HOSPITAL")
    private String infrastructureType;

    @Schema(description = "Operational status: OPERATIONAL, DEGRADED, OFFLINE, UNDER_MAINTENANCE", example = "OPERATIONAL")
    private String status;

    @Schema(description = "Latitude coordinate", example = "30.7320")
    private Double latitude;

    @Schema(description = "Longitude coordinate", example = "76.7710")
    private Double longitude;

    @Schema(description = "Physical address or landmark", example = "Sector 22-A Medical Complex, Chandigarh")
    private String address;

    @Schema(description = "Importance level: CRITICAL, PRIMARY, SECONDARY", example = "CRITICAL")
    private String importanceLevel;

    @Schema(description = "Maintenance priority: LOW, MEDIUM, HIGH, CRITICAL", example = "HIGH")
    private String maintenancePriority;

    @Schema(description = "Timestamp when the asset was last inspected")
    private Instant lastInspectedAt;

    @Schema(description = "Operational notes and maintenance observations")
    private String notes;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last updated timestamp")
    private Instant updatedAt;

    public InfrastructureResponse() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getInfrastructureType() { return infrastructureType; }
    public void setInfrastructureType(String infrastructureType) { this.infrastructureType = infrastructureType; }

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

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

