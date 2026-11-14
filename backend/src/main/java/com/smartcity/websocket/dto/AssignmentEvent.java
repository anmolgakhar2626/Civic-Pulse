package com.smartcity.websocket.dto;

import java.time.Instant;

/**
 * Real-time Incident Department & Team assignment event model.
 * Published to /topic/assignments.
 */
public class AssignmentEvent {

    private String incidentId;
    private String department;
    private String team;
    private String assignedBy;
    private Instant assignedAt;
    private String assignmentReason;

    public AssignmentEvent() {
        this.assignedAt = Instant.now();
    }

    public AssignmentEvent(String incidentId, String department, String team,
                           String assignedBy, Instant assignedAt, String assignmentReason) {
        this.incidentId = incidentId;
        this.department = department;
        this.team = team;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt != null ? assignedAt : Instant.now();
        this.assignmentReason = assignmentReason;
    }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt != null ? assignedAt : Instant.now(); }

    public String getAssignmentReason() { return assignmentReason; }
    public void setAssignmentReason(String assignmentReason) { this.assignmentReason = assignmentReason; }
}
