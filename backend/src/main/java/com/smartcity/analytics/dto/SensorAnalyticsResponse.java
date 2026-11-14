package com.smartcity.analytics.dto;

import java.util.List;
import java.util.Map;

public class SensorAnalyticsResponse {
    private long totalSensors;
    private long activeSensors;
    private long totalReadings;
    private long totalAlerts;
    private Map<String, Long> sensorsByType;
    private List<TrendPoint> alertTrends;

    public SensorAnalyticsResponse() {}

    public long getTotalSensors() { return totalSensors; }
    public void setTotalSensors(long totalSensors) { this.totalSensors = totalSensors; }

    public long getActiveSensors() { return activeSensors; }
    public void setActiveSensors(long activeSensors) { this.activeSensors = activeSensors; }

    public long getTotalReadings() { return totalReadings; }
    public void setTotalReadings(long totalReadings) { this.totalReadings = totalReadings; }

    public long getTotalAlerts() { return totalAlerts; }
    public void setTotalAlerts(long totalAlerts) { this.totalAlerts = totalAlerts; }

    public Map<String, Long> getSensorsByType() { return sensorsByType; }
    public void setSensorsByType(Map<String, Long> sensorsByType) { this.sensorsByType = sensorsByType; }

    public List<TrendPoint> getAlertTrends() { return alertTrends; }
    public void setAlertTrends(List<TrendPoint> alertTrends) { this.alertTrends = alertTrends; }
}
