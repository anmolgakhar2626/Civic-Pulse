package com.smartcity.assignment.dto;

import com.smartcity.assignment.IncidentAssignmentHistory;

import java.time.Instant;
import java.util.UUID;

public class AssignmentHistoryResponse {

    private UUID id;
    private UUID incidentId;
    private String incidentNumber;
    private String assignedBy;
    private UUID previousDepartmentId;
    private String previousDepartmentName;
    private UUID newDepartmentId;
    private String newDepartmentName;
    private UUID previousTeamId;
    private String previousTeamName;
    private UUID newTeamId;
    private String newTeamName;
    private String assignmentReason;
    private Instant assignedAt;

    public AssignmentHistoryResponse() {}

    public static AssignmentHistoryResponse fromEntity(IncidentAssignmentHistory entity) {
        if (entity == null) {
            return null;
        }

        AssignmentHistoryResponse dto = new AssignmentHistoryResponse();
        dto.setId(entity.getId());
        if (entity.getIncident() != null) {
            dto.setIncidentId(entity.getIncident().getId());
            dto.setIncidentNumber(entity.getIncident().getIncidentNumber());
        }
        dto.setAssignedBy(entity.getAssignedBy());

        if (entity.getPreviousDepartment() != null) {
            dto.setPreviousDepartmentId(entity.getPreviousDepartment().getId());
            dto.setPreviousDepartmentName(entity.getPreviousDepartment().getName());
        }
        if (entity.getNewDepartment() != null) {
            dto.setNewDepartmentId(entity.getNewDepartment().getId());
            dto.setNewDepartmentName(entity.getNewDepartment().getName());
        }
        if (entity.getPreviousTeam() != null) {
            dto.setPreviousTeamId(entity.getPreviousTeam().getId());
            dto.setPreviousTeamName(entity.getPreviousTeam().getName());
        }
        if (entity.getNewTeam() != null) {
            dto.setNewTeamId(entity.getNewTeam().getId());
            dto.setNewTeamName(entity.getNewTeam().getName());
        }

        dto.setAssignmentReason(entity.getAssignmentReason());
        dto.setAssignedAt(entity.getAssignedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getIncidentId() { return incidentId; }
    public void setIncidentId(UUID incidentId) { this.incidentId = incidentId; }

    public String getIncidentNumber() { return incidentNumber; }
    public void setIncidentNumber(String incidentNumber) { this.incidentNumber = incidentNumber; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public UUID getPreviousDepartmentId() { return previousDepartmentId; }
    public void setPreviousDepartmentId(UUID previousDepartmentId) { this.previousDepartmentId = previousDepartmentId; }

    public String getPreviousDepartmentName() { return previousDepartmentName; }
    public void setPreviousDepartmentName(String previousDepartmentName) { this.previousDepartmentName = previousDepartmentName; }

    public UUID getNewDepartmentId() { return newDepartmentId; }
    public void setNewDepartmentId(UUID newDepartmentId) { this.newDepartmentId = newDepartmentId; }

    public String getNewDepartmentName() { return newDepartmentName; }
    public void setNewDepartmentName(String newDepartmentName) { this.newDepartmentName = newDepartmentName; }

    public UUID getPreviousTeamId() { return previousTeamId; }
    public void setPreviousTeamId(UUID previousTeamId) { this.previousTeamId = previousTeamId; }

    public String getPreviousTeamName() { return previousTeamName; }
    public void setPreviousTeamName(String previousTeamName) { this.previousTeamName = previousTeamName; }

    public UUID getNewTeamId() { return newTeamId; }
    public void setNewTeamId(UUID newTeamId) { this.newTeamId = newTeamId; }

    public String getNewTeamName() { return newTeamName; }
    public void setNewTeamName(String newTeamName) { this.newTeamName = newTeamName; }

    public String getAssignmentReason() { return assignmentReason; }
    public void setAssignmentReason(String assignmentReason) { this.assignmentReason = assignmentReason; }

    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }
}
