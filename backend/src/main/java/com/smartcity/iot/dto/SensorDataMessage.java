package com.smartcity.iot.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class SensorDataMessage {

    @NotBlank(message = "sensorId is required")
    private String sensorId;

    @NotBlank(message = "sensorType is required")
    private String sensorType;

    @NotNull(message = "value is required")
    @JsonProperty("value")
    @JsonAlias({"readingValue", "value"})
    private Double value;

    private String unit;
    private Double latitude;
    private Double longitude;

    @JsonProperty("location")
    @JsonAlias({"locationDescription", "location", "address"})
    private String location;

    private Instant timestamp;

    private Map<String, Object> metadata = new HashMap<>();

    public SensorDataMessage() {
        this.timestamp = Instant.now();
    }

    public SensorDataMessage(String sensorId, String sensorType, Double value, String unit,
                             Double latitude, Double longitude, String location) {
        this.sensorId = sensorId;
        this.sensorType = sensorType;
        this.value = value;
        this.unit = unit;
        this.latitude = latitude;
        this.longitude = longitude;
        this.location = location;
        this.timestamp = Instant.now();
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

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Instant getTimestamp() { return timestamp != null ? timestamp : Instant.now(); }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata != null ? metadata : new HashMap<>(); }
}
