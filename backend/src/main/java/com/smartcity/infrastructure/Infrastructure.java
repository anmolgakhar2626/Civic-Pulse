package com.smartcity.infrastructure;

import com.smartcity.department.Department;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "infrastructure")
public class Infrastructure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "infrastructure_type", nullable = false, length = 100)
    private String infrastructureType; // ROAD, BRIDGE, SUBSTATION, WATER_PIPELINE, DRAINAGE, SHELTER

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 255)
    private String address;

    @Column(nullable = false, length = 50)
    private String status = "OPERATIONAL";

    @Column(name = "importance_level", nullable = false, length = 50)
    private String importanceLevel = "PRIMARY"; // CRITICAL, PRIMARY, SECONDARY

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "last_inspected_at")
    private Instant lastInspectedAt;

    @Column(name = "maintenance_priority", length = 50)
    private String maintenancePriority = "MEDIUM";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Infrastructure() {}

    public Infrastructure(String code, String name, String infrastructureType, Double latitude, Double longitude,
                          String address, String importanceLevel, Department department) {
        this.code = code;
        this.name = name;
        this.infrastructureType = infrastructureType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.status = "OPERATIONAL";
        this.importanceLevel = importanceLevel;
        this.department = department;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getInfrastructureType() { return infrastructureType; }
    public void setInfrastructureType(String infrastructureType) { this.infrastructureType = infrastructureType; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getImportanceLevel() { return importanceLevel; }
    public void setImportanceLevel(String importanceLevel) { this.importanceLevel = importanceLevel; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }

    public Instant getLastInspectedAt() { return lastInspectedAt; }
    public void setLastInspectedAt(Instant lastInspectedAt) { this.lastInspectedAt = lastInspectedAt; }

    public String getMaintenancePriority() { return maintenancePriority; }
    public void setMaintenancePriority(String maintenancePriority) { this.maintenancePriority = maintenancePriority; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
