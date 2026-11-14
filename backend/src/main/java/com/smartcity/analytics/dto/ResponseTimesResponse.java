package com.smartcity.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "First response and resolution time distributions and metrics")
public class ResponseTimesResponse {

    @Schema(description = "Average first response time in minutes", example = "12.4")
    private double avgFirstResponseMinutes;

    @Schema(description = "Median first response time in minutes", example = "10.0")
    private double medianFirstResponseMinutes;

    @Schema(description = "Average resolution time in hours", example = "4.2")
    private double avgResolutionHours;

    @Schema(description = "Average resolution time in minutes", example = "252.0")
    private double avgResolutionMinutes;

    @Schema(description = "Median resolution time in minutes", example = "180.0")
    private double medianResolutionMinutes;

    @Schema(description = "Minimum resolution time recorded in minutes", example = "15.0")
    private double minResolutionMinutes;

    @Schema(description = "Maximum resolution time recorded in minutes", example = "720.0")
    private double maxResolutionMinutes;

    @Schema(description = "Response time bracket distribution (<15m, 15-30m, 30-60m, >60m)")
    private Map<String, Long> responseTimeDistribution;

    @Schema(description = "Resolution time bracket distribution (<2h, 2-6h, 6-24h, >24h)")
    private Map<String, Long> resolutionTimeDistribution;

    public ResponseTimesResponse() {}

    public double getAvgFirstResponseMinutes() { return avgFirstResponseMinutes; }
    public void setAvgFirstResponseMinutes(double avgFirstResponseMinutes) { this.avgFirstResponseMinutes = avgFirstResponseMinutes; }

    public double getMedianFirstResponseMinutes() { return medianFirstResponseMinutes; }
    public void setMedianFirstResponseMinutes(double medianFirstResponseMinutes) { this.medianFirstResponseMinutes = medianFirstResponseMinutes; }

    public double getAvgResolutionHours() { return avgResolutionHours; }
    public void setAvgResolutionHours(double avgResolutionHours) { this.avgResolutionHours = avgResolutionHours; }

    public double getAvgResolutionMinutes() { return avgResolutionMinutes; }
    public void setAvgResolutionMinutes(double avgResolutionMinutes) { this.avgResolutionMinutes = avgResolutionMinutes; }

    public double getMedianResolutionMinutes() { return medianResolutionMinutes; }
    public void setMedianResolutionMinutes(double medianResolutionMinutes) { this.medianResolutionMinutes = medianResolutionMinutes; }

    public double getMinResolutionMinutes() { return minResolutionMinutes; }
    public void setMinResolutionMinutes(double minResolutionMinutes) { this.minResolutionMinutes = minResolutionMinutes; }

    public double getMaxResolutionMinutes() { return maxResolutionMinutes; }
    public void setMaxResolutionMinutes(double maxResolutionMinutes) { this.maxResolutionMinutes = maxResolutionMinutes; }

    public Map<String, Long> getResponseTimeDistribution() { return responseTimeDistribution; }
    public void setResponseTimeDistribution(Map<String, Long> responseTimeDistribution) { this.responseTimeDistribution = responseTimeDistribution; }

    public Map<String, Long> getResolutionTimeDistribution() { return resolutionTimeDistribution; }
    public void setResolutionTimeDistribution(Map<String, Long> resolutionTimeDistribution) { this.resolutionTimeDistribution = resolutionTimeDistribution; }
}

