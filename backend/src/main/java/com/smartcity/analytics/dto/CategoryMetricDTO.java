package com.smartcity.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Category-wise incident aggregate metric")
public class CategoryMetricDTO {

    @Schema(description = "Incident category name", example = "Water Supply")
    private String category;

    @Schema(description = "Total number of incidents in this category", example = "42")
    private long count;

    @Schema(description = "Percentage of total incidents", example = "28.5")
    private double percentage;

    @Schema(description = "Count of critical severity incidents in this category", example = "5")
    private long criticalCount;

    @Schema(description = "Average resolution time in hours", example = "3.2")
    private double avgResolutionHours;

    public CategoryMetricDTO() {}

    public CategoryMetricDTO(String category, long count, double percentage, long criticalCount, double avgResolutionHours) {
        this.category = category;
        this.count = count;
        this.percentage = percentage;
        this.criticalCount = criticalCount;
        this.avgResolutionHours = avgResolutionHours;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }

    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }

    public long getCriticalCount() { return criticalCount; }
    public void setCriticalCount(long criticalCount) { this.criticalCount = criticalCount; }

    public double getAvgResolutionHours() { return avgResolutionHours; }
    public void setAvgResolutionHours(double avgResolutionHours) { this.avgResolutionHours = avgResolutionHours; }
}

