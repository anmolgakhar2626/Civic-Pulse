package com.smartcity.waste;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "waste_bins")
public class WasteBin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bin_code", nullable = false, unique = true, length = 50)
    private String binCode;

    @Column(name = "location_name", nullable = false, length = 150)
    private String locationName;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "capacity_liters", nullable = false)
    private Integer capacityLiters = 1100;

    @Column(name = "current_fill_percentage", nullable = false)
    private Double currentFillPercentage = 0.0;

    @Column(name = "last_emptied_at")
    private Instant lastEmptiedAt;

    @Column(name = "battery_percentage")
    private Double batteryPercentage = 100.0;

    @Column(name = "sensor_status", nullable = false, length = 50)
    private String sensorStatus = "ACTIVE"; // ACTIVE, FAULTY, OFFLINE

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public WasteBin() {}

    public WasteBin(String binCode, String locationName, Double latitude, Double longitude, Integer capacityLiters) {
        this.binCode = binCode;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.capacityLiters = capacityLiters != null ? capacityLiters : 1100;
        this.currentFillPercentage = 0.0;
        this.sensorStatus = "ACTIVE";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getBinCode() { return binCode; }
    public void setBinCode(String binCode) { this.binCode = binCode; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Integer getCapacityLiters() { return capacityLiters; }
    public void setCapacityLiters(Integer capacityLiters) { this.capacityLiters = capacityLiters; }

    public Double getCurrentFillPercentage() { return currentFillPercentage; }
    public void setCurrentFillPercentage(Double currentFillPercentage) { this.currentFillPercentage = currentFillPercentage; }

    public Instant getLastEmptiedAt() { return lastEmptiedAt; }
    public void setLastEmptiedAt(Instant lastEmptiedAt) { this.lastEmptiedAt = lastEmptiedAt; }

    public Double getBatteryPercentage() { return batteryPercentage; }
    public void setBatteryPercentage(Double batteryPercentage) { this.batteryPercentage = batteryPercentage; }

    public String getSensorStatus() { return sensorStatus; }
    public void setSensorStatus(String sensorStatus) { this.sensorStatus = sensorStatus; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
