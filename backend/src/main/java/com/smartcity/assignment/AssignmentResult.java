package com.smartcity.assignment;

import java.time.Instant;
import java.util.UUID;

public class AssignmentResult {

    private String incidentId;
    private UUID incidentUuid;
    private String department;
    private UUID departmentId;
    private String departmentCode;
    private String team;
    private UUID teamId;
    private String teamCode;
    private String reason;
    private String assignedBy;
    private Instant assignedAt;

    public AssignmentResult() {}

    public AssignmentResult(String incidentId, String department, String team, String reason) {
        this.incidentId = incidentId;
        this.department = department;
        this.team = team;
        this.reason = reason;
        this.assignedAt = Instant.now();
    }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public UUID getIncidentUuid() { return incidentUuid; }
    public void setIncidentUuid(UUID incidentUuid) { this.incidentUuid = incidentUuid; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }

    public UUID getTeamId() { return teamId; }
    public void setTeamId(UUID teamId) { this.teamId = teamId; }

    public String getTeamCode() { return teamCode; }
    public void setTeamCode(String teamCode) { this.teamCode = teamCode; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }
}
