package com.smartcity.iot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "sensor_readings")
@CompoundIndex(name = "idx_sensor_timestamp", def = "{'sensorId': 1, 'timestamp': -1}")
public class SensorReading {

    @Id
    private String id;

    @Indexed
    private String sensorId;

    @Indexed
    private String sensorType; // AIR_QUALITY, PM25, PM10, TEMPERATURE, HUMIDITY, WATER_LEVEL, etc.

    private Double value;
    private String unit;
    private Double latitude;
    private Double longitude;
    private String location;
    private String status; // NORMAL, WARNING, ALERT
    private Map<String, Object> metadata = new HashMap<>();

    @Indexed
    private Instant timestamp = Instant.now();

    public SensorReading() {}

    public SensorReading(String sensorId, String sensorType, Double value, String unit,
                         Double latitude, Double longitude, String location, String status) {
        this.sensorId = sensorId;
        this.sensorType = sensorType;
        this.value = value;
        this.unit = unit;
        this.latitude = latitude;
        this.longitude = longitude;
        this.location = location;
        this.status = status;
        this.timestamp = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }

    public String getSensorType() { return sensorType; }
    public void setSensorType(String sensorType) { this.sensorType = sensorType; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    @JsonIgnore
    public Double getReadingValue() { return value; }
    @JsonIgnore
    public void setReadingValue(Double readingValue) { this.value = readingValue; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    @JsonIgnore
    public String getLocationDescription() { return location; }
    @JsonIgnore
    public void setLocationDescription(String locationDescription) { this.location = locationDescription; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata != null ? metadata : new HashMap<>(); }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp != null ? timestamp : Instant.now(); }
}
