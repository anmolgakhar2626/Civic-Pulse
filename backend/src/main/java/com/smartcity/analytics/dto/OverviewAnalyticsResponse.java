package com.smartcity.analytics.dto;

import java.util.List;
import java.util.Map;

public class OverviewAnalyticsResponse {
    private long totalIncidents;
    private long activeIncidents;
    private long resolvedIncidents;
    private long criticalIncidents;
    private double slaComplianceRate;
    private long slaBreaches;
    private long activeSensors;
    private long sensorAlertsCount;
    private double avgResolutionTimeHours;
    private double avgFirstResponseMinutes;
    private double avgResolutionTimeMinutes;
    private long highSeverityIncidents;
    private long operationalInfrastructure;
    private long degradedInfrastructure;
    private Map<String, Long> priorityDistribution;
    private List<DepartmentWorkloadDTO> departmentWorkload;
    private Map<String, PeriodComparisonDTO> comparisons;

    public OverviewAnalyticsResponse() {}

    public long getTotalIncidents() { return totalIncidents; }
    public void setTotalIncidents(long totalIncidents) { this.totalIncidents = totalIncidents; }

    public long getActiveIncidents() { return activeIncidents; }
    public void setActiveIncidents(long activeIncidents) { this.activeIncidents = activeIncidents; }

    public long getResolvedIncidents() { return resolvedIncidents; }
    public void setResolvedIncidents(long resolvedIncidents) { this.resolvedIncidents = resolvedIncidents; }

    public long getCriticalIncidents() { return criticalIncidents; }
    public void setCriticalIncidents(long criticalIncidents) { this.criticalIncidents = criticalIncidents; }

    public double getSlaComplianceRate() { return slaComplianceRate; }
    public void setSlaComplianceRate(double slaComplianceRate) { this.slaComplianceRate = slaComplianceRate; }

    public long getSlaBreaches() { return slaBreaches; }
    public void setSlaBreaches(long slaBreaches) { this.slaBreaches = slaBreaches; }

    public long getActiveSensors() { return activeSensors; }
    public void setActiveSensors(long activeSensors) { this.activeSensors = activeSensors; }

    public long getSensorAlertsCount() { return sensorAlertsCount; }
    public void setSensorAlertsCount(long sensorAlertsCount) { this.sensorAlertsCount = sensorAlertsCount; }

    public double getAvgResolutionTimeHours() { return avgResolutionTimeHours; }
    public void setAvgResolutionTimeHours(double avgResolutionTimeHours) { this.avgResolutionTimeHours = avgResolutionTimeHours; }

    public double getAvgFirstResponseMinutes() { return avgFirstResponseMinutes; }
    public void setAvgFirstResponseMinutes(double avgFirstResponseMinutes) { this.avgFirstResponseMinutes = avgFirstResponseMinutes; }

    public double getAvgResolutionTimeMinutes() { return avgResolutionTimeMinutes; }
    public void setAvgResolutionTimeMinutes(double avgResolutionTimeMinutes) { this.avgResolutionTimeMinutes = avgResolutionTimeMinutes; }

    public long getHighSeverityIncidents() { return highSeverityIncidents; }
    public void setHighSeverityIncidents(long highSeverityIncidents) { this.highSeverityIncidents = highSeverityIncidents; }

    public long getOperationalInfrastructure() { return operationalInfrastructure; }
    public void setOperationalInfrastructure(long operationalInfrastructure) { this.operationalInfrastructure = operationalInfrastructure; }

    public long getDegradedInfrastructure() { return degradedInfrastructure; }
    public void setDegradedInfrastructure(long degradedInfrastructure) { this.degradedInfrastructure = degradedInfrastructure; }

    public Map<String, PeriodComparisonDTO> getComparisons() { return comparisons; }
    public void setComparisons(Map<String, PeriodComparisonDTO> comparisons) { this.comparisons = comparisons; }

    public Map<String, Long> getPriorityDistribution() { return priorityDistribution; }
    public void setPriorityDistribution(Map<String, Long> priorityDistribution) { this.priorityDistribution = priorityDistribution; }

    public List<DepartmentWorkloadDTO> getDepartmentWorkload() { return departmentWorkload; }
    public void setDepartmentWorkload(List<DepartmentWorkloadDTO> departmentWorkload) { this.departmentWorkload = departmentWorkload; }
}
