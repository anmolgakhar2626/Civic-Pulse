package com.smartcity.gis;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "emergency_resources")
public class EmergencyResource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "resource_type", nullable = false, length = 100)
    private String resourceType; // HOSPITAL, POLICE_STATION, FIRE_STATION, EMERGENCY_SHELTER

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 255)
    private String address;

    @Column(name = "contact_number", length = 50)
    private String contactNumber;

    @Column(nullable = false)
    private Boolean available = true;

    private Integer capacity = 0;

    @Column(name = "operational_hours", length = 100)
    private String operationalHours = "24/7";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public EmergencyResource() {}

    public EmergencyResource(String name, String resourceType, Double latitude, Double longitude,
                             String address, String contactNumber, Boolean available, Integer capacity, String operationalHours) {
        this.name = name;
        this.resourceType = resourceType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.contactNumber = contactNumber;
        this.available = available != null ? available : true;
        this.capacity = capacity != null ? capacity : 0;
        this.operationalHours = operationalHours != null ? operationalHours : "24/7";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getOperationalHours() { return operationalHours; }
    public void setOperationalHours(String operationalHours) { this.operationalHours = operationalHours; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
