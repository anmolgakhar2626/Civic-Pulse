package com.smartcity.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status distribution metric")
public class StatusMetricDTO {

    @Schema(description = "Incident status: OPEN, IN_PROGRESS, RESOLVED, CLOSED", example = "OPEN")
    private String status;

    @Schema(description = "Count of incidents", example = "25")
    private long count;

    @Schema(description = "Percentage of total incidents", example = "35.5")
    private double percentage;

    public StatusMetricDTO() {}

    public StatusMetricDTO(String status, long count, double percentage) {
        this.status = status;
        this.count = count;
        this.percentage = percentage;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }

    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }
}

