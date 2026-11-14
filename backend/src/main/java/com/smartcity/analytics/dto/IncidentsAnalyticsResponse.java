package com.smartcity.analytics.dto;

import java.util.List;
import java.util.Map;

public class IncidentsAnalyticsResponse {
    private long totalIncidents;
    private long activeIncidents;
    private long resolvedIncidents;
    private double avgResolutionTimeHours;
    private Map<String, Long> priorityDistribution;
    private Map<String, Long> statusDistribution;
    private Map<String, Long> sourceDistribution;
    private Map<String, Long> departmentDistribution;
    private List<TrendPoint> incidentTrends;

    public IncidentsAnalyticsResponse() {}

    public long getTotalIncidents() { return totalIncidents; }
    public void setTotalIncidents(long totalIncidents) { this.totalIncidents = totalIncidents; }

    public long getActiveIncidents() { return activeIncidents; }
    public void setActiveIncidents(long activeIncidents) { this.activeIncidents = activeIncidents; }

    public long getResolvedIncidents() { return resolvedIncidents; }
    public void setResolvedIncidents(long resolvedIncidents) { this.resolvedIncidents = resolvedIncidents; }

    public double getAvgResolutionTimeHours() { return avgResolutionTimeHours; }
    public void setAvgResolutionTimeHours(double avgResolutionTimeHours) { this.avgResolutionTimeHours = avgResolutionTimeHours; }

    public Map<String, Long> getPriorityDistribution() { return priorityDistribution; }
    public void setPriorityDistribution(Map<String, Long> priorityDistribution) { this.priorityDistribution = priorityDistribution; }

    public Map<String, Long> getStatusDistribution() { return statusDistribution; }
    public void setStatusDistribution(Map<String, Long> statusDistribution) { this.statusDistribution = statusDistribution; }

    public Map<String, Long> getSourceDistribution() { return sourceDistribution; }
    public void setSourceDistribution(Map<String, Long> sourceDistribution) { this.sourceDistribution = sourceDistribution; }

    public Map<String, Long> getDepartmentDistribution() { return departmentDistribution; }
    public void setDepartmentDistribution(Map<String, Long> departmentDistribution) { this.departmentDistribution = departmentDistribution; }

    public List<TrendPoint> getIncidentTrends() { return incidentTrends; }
    public void setIncidentTrends(List<TrendPoint> incidentTrends) { this.incidentTrends = incidentTrends; }
}
