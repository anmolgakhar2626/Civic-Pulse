package com.smartcity.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "SLA compliance, resolution limits, and breach analytics")
public class SlaPerformanceResponse {

    @Schema(description = "Total number of SLA records tracked in period", example = "50")
    private long totalTracked;

    @Schema(description = "Count of incidents meeting SLA response and resolution targets", example = "45")
    private long compliantCount;

    @Schema(description = "Count of breached SLAs", example = "5")
    private long breachedCount;

    @Schema(description = "Count of SLAs approaching deadline (warning state)", example = "3")
    private long atRiskCount;

    @Schema(description = "SLA compliance percentage: (compliant / total) * 100", example = "90.0")
    private double complianceRate;

    @Schema(description = "Average first response time in minutes", example = "8.5")
    private double avgResponseTimeMinutes;

    @Schema(description = "Average resolution time in minutes", example = "45.2")
    private double avgResolutionTimeMinutes;

    @Schema(description = "Distribution of active escalation levels (NONE, LEVEL_1, LEVEL_2, CRITICAL_ESCALATION)")
    private Map<String, Long> escalationDistribution;

    @Schema(description = "Historical daily or monthly SLA compliance trends")
    private List<TrendMetricDTO> complianceTrends;

    public SlaPerformanceResponse() {}

    public long getTotalTracked() { return totalTracked; }
    public void setTotalTracked(long totalTracked) { this.totalTracked = totalTracked; }

    public long getCompliantCount() { return compliantCount; }
    public void setCompliantCount(long compliantCount) { this.compliantCount = compliantCount; }

    public long getBreachedCount() { return breachedCount; }
    public void setBreachedCount(long breachedCount) { this.breachedCount = breachedCount; }

    public long getAtRiskCount() { return atRiskCount; }
    public void setAtRiskCount(long atRiskCount) { this.atRiskCount = atRiskCount; }

    public double getComplianceRate() { return complianceRate; }
    public void setComplianceRate(double complianceRate) { this.complianceRate = complianceRate; }

    public double getAvgResponseTimeMinutes() { return avgResponseTimeMinutes; }
    public void setAvgResponseTimeMinutes(double avgResponseTimeMinutes) { this.avgResponseTimeMinutes = avgResponseTimeMinutes; }

    public double getAvgResolutionTimeMinutes() { return avgResolutionTimeMinutes; }
    public void setAvgResolutionTimeMinutes(double avgResolutionTimeMinutes) { this.avgResolutionTimeMinutes = avgResolutionTimeMinutes; }

    public Map<String, Long> getEscalationDistribution() { return escalationDistribution; }
    public void setEscalationDistribution(Map<String, Long> escalationDistribution) { this.escalationDistribution = escalationDistribution; }

    public List<TrendMetricDTO> getComplianceTrends() { return complianceTrends; }
    public void setComplianceTrends(List<TrendMetricDTO> complianceTrends) { this.complianceTrends = complianceTrends; }
}

