package com.smartcity.department.dto;

import com.smartcity.department.Department;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class DepartmentResponse {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private String contactEmail;
    private String contactPhone;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private List<TeamResponse> teams;

    public DepartmentResponse() {
        this.teams = Collections.emptyList();
    }

    public DepartmentResponse(UUID id, String code, String name, String description, String contactEmail, String contactPhone, boolean active, Instant createdAt, Instant updatedAt, List<TeamResponse> teams) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.teams = teams != null ? teams : Collections.emptyList();
    }

    public static DepartmentResponse fromEntity(Department dept, List<TeamResponse> teams) {
        if (dept == null) return null;
        return new DepartmentResponse(
                dept.getId(),
                dept.getCode(),
                dept.getName(),
                dept.getDescription(),
                dept.getContactEmail(),
                dept.getContactPhone(),
                dept.isActive(),
                dept.getCreatedAt(),
                dept.getUpdatedAt(),
                teams
        );
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<TeamResponse> getTeams() { return teams; }
    public void setTeams(List<TeamResponse> teams) { this.teams = teams; }
}
