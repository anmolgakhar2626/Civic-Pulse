package com.smartcity.traffic;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "traffic_readings")
public class TrafficReading {

    @Id
    private String id;
    private String sensorId;
    private String junctionName;
    private Double latitude;
    private Double longitude;
    private Integer vehicleCount;
    private Double averageSpeed;
    private String congestionLevel; // LOW, MODERATE, HEAVY, SEVERE
    private Boolean simulated = true;
    private Instant timestamp = Instant.now();

    public TrafficReading() {}

    public TrafficReading(String sensorId, String junctionName, Double latitude, Double longitude,
                          Integer vehicleCount, Double averageSpeed, String congestionLevel, Boolean simulated) {
        this.sensorId = sensorId;
        this.junctionName = junctionName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.vehicleCount = vehicleCount;
        this.averageSpeed = averageSpeed;
        this.congestionLevel = congestionLevel;
        this.simulated = simulated;
        this.timestamp = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }

    public String getJunctionName() { return junctionName; }
    public void setJunctionName(String junctionName) { this.junctionName = junctionName; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Integer getVehicleCount() { return vehicleCount; }
    public void setVehicleCount(Integer vehicleCount) { this.vehicleCount = vehicleCount; }

    public Double getAverageSpeed() { return averageSpeed; }
    public void setAverageSpeed(Double averageSpeed) { this.averageSpeed = averageSpeed; }

    public String getCongestionLevel() { return congestionLevel; }
    public void setCongestionLevel(String congestionLevel) { this.congestionLevel = congestionLevel; }

    public Boolean getSimulated() { return simulated; }
    public void setSimulated(Boolean simulated) { this.simulated = simulated; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
