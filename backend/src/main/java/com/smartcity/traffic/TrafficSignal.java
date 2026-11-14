package com.smartcity.traffic;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "traffic_signals")
public class TrafficSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "signal_code", nullable = false, unique = true, length = 50)
    private String signalCode;

    @Column(name = "junction_name", nullable = false, length = 150)
    private String junctionName;

    @Column(name = "junction_type", nullable = false, length = 50)
    private String junctionType; // MAJOR, MINOR, RESIDENTIAL

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "operational_status", nullable = false, length = 50)
    private String operationalStatus = "ACTIVE"; // ACTIVE, FAILING, MAINTENANCE, OFFLINE

    @Column(name = "current_cycle_seconds")
    private Integer currentCycleSeconds = 90;

    @Column(name = "last_inspected_at")
    private Instant lastInspectedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public TrafficSignal() {}

    public TrafficSignal(String signalCode, String junctionName, String junctionType, Double latitude, Double longitude) {
        this.signalCode = signalCode;
        this.junctionName = junctionName;
        this.junctionType = junctionType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.operationalStatus = "ACTIVE";
        this.currentCycleSeconds = 90;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getSignalCode() { return signalCode; }
    public void setSignalCode(String signalCode) { this.signalCode = signalCode; }

    public String getJunctionName() { return junctionName; }
    public void setJunctionName(String junctionName) { this.junctionName = junctionName; }

    public String getJunctionType() { return junctionType; }
    public void setJunctionType(String junctionType) { this.junctionType = junctionType; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getOperationalStatus() { return operationalStatus; }
    public void setOperationalStatus(String operationalStatus) { this.operationalStatus = operationalStatus; }

    public Integer getCurrentCycleSeconds() { return currentCycleSeconds; }
    public void setCurrentCycleSeconds(Integer currentCycleSeconds) { this.currentCycleSeconds = currentCycleSeconds; }

    public Instant getLastInspectedAt() { return lastInspectedAt; }
    public void setLastInspectedAt(Instant lastInspectedAt) { this.lastInspectedAt = lastInspectedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
