package com.smartcity.iot.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public class SensorStatusMessage {

    @NotBlank(message = "sensorId is required")
    private String sensorId;

    @NotBlank(message = "status is required")
    private String status; // ONLINE, OFFLINE, FAULT

    private Instant timestamp;

    public SensorStatusMessage() {
        this.timestamp = Instant.now();
    }

    public SensorStatusMessage(String sensorId, String status) {
        this.sensorId = sensorId;
        this.status = status;
        this.timestamp = Instant.now();
    }

    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getTimestamp() { return timestamp != null ? timestamp : Instant.now(); }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
