package com.smartcity.iot;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "sensors")
public class SensorDevice {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sensorId;

    @Indexed
    private SensorType sensorType;

    private String location;
    private Double latitude;
    private Double longitude;

    @Indexed
    private SensorStatus status = SensorStatus.ONLINE;

    private Double lastValue;
    private String unit;

    @Indexed
    private Instant lastSeenAt = Instant.now();

    private Instant registeredAt = Instant.now();

    private Map<String, Object> metadata = new HashMap<>();

    public SensorDevice() {}

    public SensorDevice(String sensorId, SensorType sensorType, String location, Double latitude, Double longitude) {
        this.sensorId = sensorId;
        this.sensorType = sensorType;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = SensorStatus.ONLINE;
        this.lastSeenAt = Instant.now();
        this.registeredAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }

    public SensorType getSensorType() { return sensorType; }
    public void setSensorType(SensorType sensorType) { this.sensorType = sensorType; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public SensorStatus getStatus() { return status; }
    public void setStatus(SensorStatus status) { this.status = status; }

    public Double getLastValue() { return lastValue; }
    public void setLastValue(Double lastValue) { this.lastValue = lastValue; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public Instant getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Instant registeredAt) { this.registeredAt = registeredAt; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata != null ? metadata : new HashMap<>(); }
}
