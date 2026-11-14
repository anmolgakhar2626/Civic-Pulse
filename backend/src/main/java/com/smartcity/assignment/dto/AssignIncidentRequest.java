package com.smartcity.assignment.dto;

import java.util.UUID;

public class AssignIncidentRequest {

    private UUID departmentId;
    private UUID teamId;
    private String assignedBy;
    private String reason;

    public AssignIncidentRequest() {}

    public AssignIncidentRequest(UUID departmentId, UUID teamId, String assignedBy, String reason) {
        this.departmentId = departmentId;
        this.teamId = teamId;
        this.assignedBy = assignedBy;
        this.reason = reason;
    }

    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }

    public UUID getTeamId() { return teamId; }
    public void setTeamId(UUID teamId) { this.teamId = teamId; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
