package com.smartcity.analytics.dto;

import java.util.Map;

public class SlaAnalyticsResponse {
    private long totalMonitored;
    private double complianceRate;
    private long onTrack;
    private long atRisk;
    private long breached;
    private long resolved;
    private Map<String, Long> escalationDistribution;
    private double avgResolutionMinutes;

    public SlaAnalyticsResponse() {}

    public long getTotalMonitored() { return totalMonitored; }
    public void setTotalMonitored(long totalMonitored) { this.totalMonitored = totalMonitored; }

    public double getComplianceRate() { return complianceRate; }
    public void setComplianceRate(double complianceRate) { this.complianceRate = complianceRate; }

    public long getOnTrack() { return onTrack; }
    public void setOnTrack(long onTrack) { this.onTrack = onTrack; }

    public long getAtRisk() { return atRisk; }
    public void setAtRisk(long atRisk) { this.atRisk = atRisk; }

    public long getBreached() { return breached; }
    public void setBreached(long breached) { this.breached = breached; }

    public long getResolved() { return resolved; }
    public void setResolved(long resolved) { this.resolved = resolved; }

    public Map<String, Long> getEscalationDistribution() { return escalationDistribution; }
    public void setEscalationDistribution(Map<String, Long> escalationDistribution) { this.escalationDistribution = escalationDistribution; }

    public double getAvgResolutionMinutes() { return avgResolutionMinutes; }
    public void setAvgResolutionMinutes(double avgResolutionMinutes) { this.avgResolutionMinutes = avgResolutionMinutes; }
}
