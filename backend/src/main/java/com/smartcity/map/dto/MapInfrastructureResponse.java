package com.smartcity.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Infrastructure asset representation for GIS Map display")
public class MapInfrastructureResponse {

    @Schema(description = "Asset UUID")
    private UUID id;

    @Schema(description = "Asset tracking code", example = "INF-SUB-01")
    private String code;

    @Schema(description = "Asset name", example = "Sector 17 Central Grid Power Substation")
    private String name;

    @Schema(description = "Category name", example = "SUBSTATION")
    private String category;

    @Schema(description = "Operational status: OPERATIONAL, DEGRADED, OFFLINE, UNDER_MAINTENANCE", example = "OPERATIONAL")
    private String status;

    @Schema(description = "Importance level: CRITICAL, PRIMARY, SECONDARY", example = "CRITICAL")
    private String importanceLevel;

    @Schema(description = "Latitude coordinate", example = "30.7395")
    private Double latitude;

    @Schema(description = "Longitude coordinate", example = "76.7840")
    private Double longitude;

    @Schema(description = "Street address or location", example = "Sector 17 Utility Zone, Chandigarh")
    private String address;

    @Schema(description = "Maintenance priority: LOW, MEDIUM, HIGH, CRITICAL", example = "HIGH")
    private String maintenancePriority;

    @Schema(description = "Last inspected timestamp")
    private Instant lastInspectedAt;

    @Schema(description = "Operational notes")
    private String notes;

    public MapInfrastructureResponse() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getImportanceLevel() { return importanceLevel; }
    public void setImportanceLevel(String importanceLevel) { this.importanceLevel = importanceLevel; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getMaintenancePriority() { return maintenancePriority; }
    public void setMaintenancePriority(String maintenancePriority) { this.maintenancePriority = maintenancePriority; }

    public Instant getLastInspectedAt() { return lastInspectedAt; }
    public void setLastInspectedAt(Instant lastInspectedAt) { this.lastInspectedAt = lastInspectedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

