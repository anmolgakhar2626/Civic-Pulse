package com.smartcity.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Severity-wise incident aggregate metric")
public class SeverityMetricDTO {

    @Schema(description = "Severity level: CRITICAL, HIGH, MEDIUM, LOW", example = "CRITICAL")
    private String severity;

    @Schema(description = "Count of incidents", example = "12")
    private long count;

    @Schema(description = "Percentage of total incidents", example = "15.0")
    private double percentage;

    public SeverityMetricDTO() {}

    public SeverityMetricDTO(String severity, long count, double percentage) {
        this.severity = severity;
        this.count = count;
        this.percentage = percentage;
    }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }

    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }
}

