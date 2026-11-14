package com.smartcity.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Aggregated GIS Map metrics and summary KPI counts")
public class MapSummaryResponse {

    @Schema(description = "Total number of mapped incidents")
    private long totalIncidents;

    @Schema(description = "Total number of active/unresolved incidents (OPEN, IN_PROGRESS)")
    private long activeIncidents;

    @Schema(description = "Count of critical severity incidents")
    private long criticalIncidents;

    @Schema(description = "Count of resolved or closed incidents")
    private long resolvedIncidents;

    @Schema(description = "Total tracked infrastructure assets")
    private long totalInfrastructure;

    @Schema(description = "Operational infrastructure assets count")
    private long operationalInfrastructure;

    @Schema(description = "Degraded or offline infrastructure count")
    private long degradedInfrastructure;

    @Schema(description = "Infrastructure assets under maintenance")
    private long maintenanceInfrastructure;

    @Schema(description = "Total public shelters and emergency facilities")
    private long totalShelters;

    @Schema(description = "Available shelters ready for occupancy")
    private long availableShelters;

    @Schema(description = "Total combined capacity across all shelters")
    private long totalShelterCapacity;

    @Schema(description = "Active IoT telemetry sensors")
    private long activeSensors;

    @Schema(description = "Server calculation timestamp")
    private Instant lastUpdated;

    public MapSummaryResponse() {}

    public long getTotalIncidents() { return totalIncidents; }
    public void setTotalIncidents(long totalIncidents) { this.totalIncidents = totalIncidents; }

    public long getActiveIncidents() { return activeIncidents; }
    public void setActiveIncidents(long activeIncidents) { this.activeIncidents = activeIncidents; }

    public long getCriticalIncidents() { return criticalIncidents; }
    public void setCriticalIncidents(long criticalIncidents) { this.criticalIncidents = criticalIncidents; }

    public long getResolvedIncidents() { return resolvedIncidents; }
    public void setResolvedIncidents(long resolvedIncidents) { this.resolvedIncidents = resolvedIncidents; }

    public long getTotalInfrastructure() { return totalInfrastructure; }
    public void setTotalInfrastructure(long totalInfrastructure) { this.totalInfrastructure = totalInfrastructure; }

    public long getOperationalInfrastructure() { return operationalInfrastructure; }
    public void setOperationalInfrastructure(long operationalInfrastructure) { this.operationalInfrastructure = operationalInfrastructure; }

    public long getDegradedInfrastructure() { return degradedInfrastructure; }
    public void setDegradedInfrastructure(long degradedInfrastructure) { this.degradedInfrastructure = degradedInfrastructure; }

    public long getMaintenanceInfrastructure() { return maintenanceInfrastructure; }
    public void setMaintenanceInfrastructure(long maintenanceInfrastructure) { this.maintenanceInfrastructure = maintenanceInfrastructure; }

    public long getTotalShelters() { return totalShelters; }
    public void setTotalShelters(long totalShelters) { this.totalShelters = totalShelters; }

    public long getAvailableShelters() { return availableShelters; }
    public void setAvailableShelters(long availableShelters) { this.availableShelters = availableShelters; }

    public long getTotalShelterCapacity() { return totalShelterCapacity; }
    public void setTotalShelterCapacity(long totalShelterCapacity) { this.totalShelterCapacity = totalShelterCapacity; }

    public long getActiveSensors() { return activeSensors; }
    public void setActiveSensors(long activeSensors) { this.activeSensors = activeSensors; }

    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
}

