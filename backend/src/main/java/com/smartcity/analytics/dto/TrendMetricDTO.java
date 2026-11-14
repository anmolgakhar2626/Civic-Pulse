package com.smartcity.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Incident trend data point for time-series charts")
public class TrendMetricDTO {

    @Schema(description = "Time bucket formatted as YYYY-MM-DD or YYYY-MM", example = "2026-10-28")
    private String period;

    @Schema(description = "Number of incidents created during this bucket", example = "15")
    private long createdCount;

    @Schema(description = "Number of incidents resolved during this bucket", example = "12")
    private long resolvedCount;

    @Schema(description = "Number of SLA breaches recorded during this bucket", example = "2")
    private long breachedCount;

    public TrendMetricDTO() {}

    public TrendMetricDTO(String period, long createdCount, long resolvedCount, long breachedCount) {
        this.period = period;
        this.createdCount = createdCount;
        this.resolvedCount = resolvedCount;
        this.breachedCount = breachedCount;
    }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public long getCreatedCount() { return createdCount; }
    public void setCreatedCount(long createdCount) { this.createdCount = createdCount; }

    public long getResolvedCount() { return resolvedCount; }
    public void setResolvedCount(long resolvedCount) { this.resolvedCount = resolvedCount; }

    public long getBreachedCount() { return breachedCount; }
    public void setBreachedCount(long breachedCount) { this.breachedCount = breachedCount; }
}

