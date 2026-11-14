package com.smartcity.websocket.dto;

import java.time.Instant;

/**
 * Real-time Sensor telemetry event model.
 * Published to /topic/sensors.
 */
public class SensorEvent {

    private String sensorId;
    private String sensorType;
    private Double value;
    private String unit;
    private Double latitude;
    private Double longitude;
    private Instant timestamp;
    private String status; // NORMAL, ALERT, WARNING

    public SensorEvent() {
        this.timestamp = Instant.now();
    }

    public SensorEvent(String sensorId, String sensorType, Double value, String unit,
                       Double latitude, Double longitude, Instant timestamp, String status) {
        this.sensorId = sensorId;
        this.sensorType = sensorType;
        this.value = value;
        this.unit = unit;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.status = status;
    }

    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }

    public String getSensorType() { return sensorType; }
    public void setSensorType(String sensorType) { this.sensorType = sensorType; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp != null ? timestamp : Instant.now(); }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
