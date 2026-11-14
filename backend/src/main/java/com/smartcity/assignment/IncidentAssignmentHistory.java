package com.smartcity.assignment;

import com.smartcity.department.Department;
import com.smartcity.department.Team;
import com.smartcity.incident.Incident;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident_assignment_history")
public class IncidentAssignmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Column(name = "assigned_by", nullable = false, length = 100)
    private String assignedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_department_id")
    private Department previousDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_department_id")
    private Department newDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_team_id")
    private Team previousTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_team_id")
    private Team newTeam;

    @Column(name = "assignment_reason", columnDefinition = "TEXT")
    private String assignmentReason;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt = Instant.now();

    public IncidentAssignmentHistory() {}

    public IncidentAssignmentHistory(Incident incident, String assignedBy, Department previousDepartment,
                                     Department newDepartment, Team previousTeam, Team newTeam,
                                     String assignmentReason) {
        this.incident = incident;
        this.assignedBy = assignedBy != null ? assignedBy : "SYSTEM";
        this.previousDepartment = previousDepartment;
        this.newDepartment = newDepartment;
        this.previousTeam = previousTeam;
        this.newTeam = newTeam;
        this.assignmentReason = assignmentReason;
        this.assignedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Incident getIncident() { return incident; }
    public void setIncident(Incident incident) { this.incident = incident; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public Department getPreviousDepartment() { return previousDepartment; }
    public void setPreviousDepartment(Department previousDepartment) { this.previousDepartment = previousDepartment; }

    public Department getNewDepartment() { return newDepartment; }
    public void setNewDepartment(Department newDepartment) { this.newDepartment = newDepartment; }

    public Team getPreviousTeam() { return previousTeam; }
    public void setPreviousTeam(Team previousTeam) { this.previousTeam = previousTeam; }

    public Team getNewTeam() { return newTeam; }
    public void setNewTeam(Team newTeam) { this.newTeam = newTeam; }

    public String getAssignmentReason() { return assignmentReason; }
    public void setAssignmentReason(String assignmentReason) { this.assignmentReason = assignmentReason; }

    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }
}
