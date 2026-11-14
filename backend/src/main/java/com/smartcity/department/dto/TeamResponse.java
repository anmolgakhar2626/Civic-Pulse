package com.smartcity.department.dto;

import com.smartcity.department.Team;

import java.time.Instant;
import java.util.UUID;

public class TeamResponse {

    private UUID id;
    private UUID departmentId;
    private String name;
    private String code;
    private String contactPhone;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public TeamResponse() {}

    public TeamResponse(UUID id, UUID departmentId, String name, String code, String contactPhone, boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.departmentId = departmentId;
        this.name = name;
        this.code = code;
        this.contactPhone = contactPhone;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TeamResponse fromEntity(Team team) {
        if (team == null) return null;
        UUID deptId = team.getDepartment() != null ? team.getDepartment().getId() : null;
        return new TeamResponse(
                team.getId(),
                deptId,
                team.getName(),
                team.getCode(),
                team.getContactNumber(),
                team.isActive(),
                team.getCreatedAt(),
                team.getUpdatedAt()
        );
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
