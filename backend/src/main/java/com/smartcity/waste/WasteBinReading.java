package com.smartcity.waste;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "waste_bin_readings")
public class WasteBinReading {

    @Id
    private String id;
    private String binId;
    private String locationName;
    private Double latitude;
    private Double longitude;
    private Double fillPercentage;
    private Double batteryLevel;
    private String status; // NORMAL, MONITOR, CRITICAL
    private Boolean simulated = true;
    private Instant timestamp = Instant.now();

    public WasteBinReading() {}

    public WasteBinReading(String binId, String locationName, Double latitude, Double longitude,
                           Double fillPercentage, Double batteryLevel, Boolean simulated) {
        this.binId = binId;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.fillPercentage = fillPercentage;
        this.batteryLevel = batteryLevel;
        this.simulated = simulated;
        this.status = fillPercentage >= 90.0 ? "CRITICAL" : (fillPercentage >= 70.0 ? "MONITOR" : "NORMAL");
        this.timestamp = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBinId() { return binId; }
    public void setBinId(String binId) { this.binId = binId; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getFillPercentage() { return fillPercentage; }
    public void setFillPercentage(Double fillPercentage) {
        this.fillPercentage = fillPercentage;
        this.status = fillPercentage >= 90.0 ? "CRITICAL" : (fillPercentage >= 70.0 ? "MONITOR" : "NORMAL");
    }

    public Double getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(Double batteryLevel) { this.batteryLevel = batteryLevel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getSimulated() { return simulated; }
    public void setSimulated(Boolean simulated) { this.simulated = simulated; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
