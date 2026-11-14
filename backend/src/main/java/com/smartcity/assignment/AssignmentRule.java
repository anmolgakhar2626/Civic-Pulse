package com.smartcity.assignment;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assignment_rules")
public class AssignmentRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "incident_type", nullable = false, length = 100)
    private String incidentType;

    @Column(name = "department_code", nullable = false, length = 50)
    private String departmentCode;

    @Column(name = "department_name", nullable = false, length = 150)
    private String departmentName;

    @Column(name = "default_team_code", length = 50)
    private String defaultTeamCode;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "priority_override", length = 50)
    private String priorityOverride;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public AssignmentRule() {}

    public AssignmentRule(String incidentType, String departmentCode, String departmentName, String defaultTeamCode, String description) {
        this.incidentType = incidentType;
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.defaultTeamCode = defaultTeamCode;
        this.description = description;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getIncidentType() { return incidentType; }
    public void setIncidentType(String incidentType) { this.incidentType = incidentType; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getDefaultTeamCode() { return defaultTeamCode; }
    public void setDefaultTeamCode(String defaultTeamCode) { this.defaultTeamCode = defaultTeamCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPriorityOverride() { return priorityOverride; }
    public void setPriorityOverride(String priorityOverride) { this.priorityOverride = priorityOverride; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public boolean isActive() { return Boolean.TRUE.equals(this.active); }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
