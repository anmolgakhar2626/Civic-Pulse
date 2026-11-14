package com.smartcity.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Period-over-period comparison metric with explainable improvement indicator")
public class PeriodComparisonDTO {

    @Schema(description = "Metric identifier or title", example = "Total Incidents")
    private String metricName;

    @Schema(description = "Value in current period", example = "45.0")
    private double currentValue;

    @Schema(description = "Value in previous period of identical duration", example = "50.0")
    private double previousValue;

    @Schema(description = "Percentage difference: ((current - previous) / previous) * 100", example = "-10.0")
    private double percentageChange;

    @Schema(description = "True if the change represents an operational improvement", example = "true")
    private boolean improvement;

    @Schema(description = "Human-readable explanation of the comparison", example = "Decreased by 10.0% compared to previous 7 days (Improvement)")
    private String explanation;

    public PeriodComparisonDTO() {}

    public PeriodComparisonDTO(String metricName, double currentValue, double previousValue, boolean lowerIsBetter, String unit) {
        this.metricName = metricName;
        this.currentValue = round2(currentValue);
        this.previousValue = round2(previousValue);

        if (previousValue == 0.0) {
            this.percentageChange = currentValue == 0.0 ? 0.0 : 100.0;
        } else {
            this.percentageChange = round2(((currentValue - previousValue) / previousValue) * 100.0);
        }

        if (lowerIsBetter) {
            this.improvement = this.percentageChange < 0;
        } else {
            this.improvement = this.percentageChange > 0;
        }

        String direction = this.percentageChange >= 0 ? "Increased" : "Decreased";
        String tag = this.improvement ? " (Improvement)" : this.percentageChange == 0 ? " (Unchanged)" : " (Attention Needed)";
        this.explanation = String.format("%s by %.1f%% compared to prior period%s", direction, Math.abs(this.percentageChange), tag);
    }

    private static double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    public String getMetricName() { return metricName; }
    public void setMetricName(String metricName) { this.metricName = metricName; }

    public double getCurrentValue() { return currentValue; }
    public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }

    public double getPreviousValue() { return previousValue; }
    public void setPreviousValue(double previousValue) { this.previousValue = previousValue; }

    public double getPercentageChange() { return percentageChange; }
    public void setPercentageChange(double percentageChange) { this.percentageChange = percentageChange; }

    public boolean isImprovement() { return improvement; }
    public void setImprovement(boolean improvement) { this.improvement = improvement; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}

