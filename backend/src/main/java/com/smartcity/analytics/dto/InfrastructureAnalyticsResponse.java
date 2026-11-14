package com.smartcity.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Municipal infrastructure health, operational status, and maintenance analytics")
public class InfrastructureAnalyticsResponse {

    @Schema(description = "Total registered infrastructure assets", example = "150")
    private long totalAssets;

    @Schema(description = "Assets in full OPERATIONAL status", example = "135")
    private long operationalCount;

    @Schema(description = "Assets in DEGRADED performance status", example = "8")
    private long degradedCount;

    @Schema(description = "Assets that are currently OFFLINE", example = "3")
    private long offlineCount;

    @Schema(description = "Assets undergoing active MAINTENANCE", example = "4")
    private long maintenanceCount;

    @Schema(description = "Operational asset health percentage: (operational / total) * 100", example = "90.0")
    private double operationalRate;

    @Schema(description = "Distribution of assets by operational status")
    private Map<String, Long> statusDistribution;

    @Schema(description = "Distribution of assets by infrastructure category (ROAD, BRIDGE, HOSPITAL, etc.)")
    private Map<String, Long> categoryDistribution;

    @Schema(description = "Distribution of maintenance priorities (LOW, MEDIUM, HIGH, CRITICAL)")
    private Map<String, Long> priorityDistribution;

    public InfrastructureAnalyticsResponse() {}

    public long getTotalAssets() { return totalAssets; }
    public void setTotalAssets(long totalAssets) { this.totalAssets = totalAssets; }

    public long getOperationalCount() { return operationalCount; }
    public void setOperationalCount(long operationalCount) { this.operationalCount = operationalCount; }

    public long getDegradedCount() { return degradedCount; }
    public void setDegradedCount(long degradedCount) { this.degradedCount = degradedCount; }

    public long getOfflineCount() { return offlineCount; }
    public void setOfflineCount(long offlineCount) { this.offlineCount = offlineCount; }

    public long getMaintenanceCount() { return maintenanceCount; }
    public void setMaintenanceCount(long maintenanceCount) { this.maintenanceCount = maintenanceCount; }

    public double getOperationalRate() { return operationalRate; }
    public void setOperationalRate(double operationalRate) { this.operationalRate = operationalRate; }

    public Map<String, Long> getStatusDistribution() { return statusDistribution; }
    public void setStatusDistribution(Map<String, Long> statusDistribution) { this.statusDistribution = statusDistribution; }

    public Map<String, Long> getCategoryDistribution() { return categoryDistribution; }
    public void setCategoryDistribution(Map<String, Long> categoryDistribution) { this.categoryDistribution = categoryDistribution; }

    public Map<String, Long> getPriorityDistribution() { return priorityDistribution; }
    public void setPriorityDistribution(Map<String, Long> priorityDistribution) { this.priorityDistribution = priorityDistribution; }
}

