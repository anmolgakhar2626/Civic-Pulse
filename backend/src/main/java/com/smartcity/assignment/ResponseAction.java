package com.smartcity.assignment;

import com.smartcity.incident.Incident;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "response_actions")
public class ResponseAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ResponseActionType actionType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ResponseActionStatus status = ResponseActionStatus.RECOMMENDED;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    public ResponseAction() {}

    public ResponseAction(Incident incident, ResponseActionType actionType, String description,
                          ResponseActionStatus status, String createdBy) {
        this.incident = incident;
        this.actionType = actionType;
        this.description = description;
        this.status = status != null ? status : ResponseActionStatus.RECOMMENDED;
        this.createdBy = createdBy != null ? createdBy : "SYSTEM";
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Incident getIncident() { return incident; }
    public void setIncident(Incident incident) { this.incident = incident; }

    public UUID getIncidentId() {
        return incident != null ? incident.getId() : null;
    }

    public ResponseActionType getActionType() { return actionType; }
    public void setActionType(ResponseActionType actionType) { this.actionType = actionType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ResponseActionStatus getStatus() { return status; }
    public void setStatus(ResponseActionStatus status) {
        this.status = status;
        if (status == ResponseActionStatus.COMPLETED && this.completedAt == null) {
            this.completedAt = Instant.now();
        }
    }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
