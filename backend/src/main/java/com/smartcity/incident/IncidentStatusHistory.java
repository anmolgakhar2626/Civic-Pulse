package com.smartcity.incident;

import com.smartcity.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident_status_history")
public class IncidentStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Column(name = "from_status", length = 50)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 50)
    private String toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id")
    private User changedByUser;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @Column(name = "changed_by_username", length = 100)
    private String changedByUsername;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.changedAt == null) {
            this.changedAt = this.createdAt;
        }
        if (this.changedBy == null && this.changedByUsername != null) {
            this.changedBy = this.changedByUsername;
        } else if (this.changedByUsername == null && this.changedBy != null) {
            this.changedByUsername = this.changedBy;
        }
        if (this.remarks == null && this.notes != null) {
            this.remarks = this.notes;
        } else if (this.notes == null && this.remarks != null) {
            this.notes = this.remarks;
        }
    }

    public IncidentStatusHistory() {}

    public IncidentStatusHistory(Incident incident, String fromStatus, String toStatus, String changedBy, String remarks) {
        this.incident = incident;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBy = changedBy != null ? changedBy : "SYSTEM";
        this.changedByUsername = this.changedBy;
        this.remarks = remarks;
        this.notes = remarks;
        this.changedAt = Instant.now();
        this.createdAt = this.changedAt;
    }

    public IncidentStatusHistory(Incident incident, String fromStatus, String toStatus, User changedByUser, String changedByUsername, String notes) {
        this.incident = incident;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedByUser = changedByUser;
        this.changedByUsername = changedByUsername != null ? changedByUsername : (changedByUser != null ? changedByUser.getUsername() : "SYSTEM");
        this.changedBy = this.changedByUsername;
        this.notes = notes;
        this.remarks = notes;
        this.changedAt = Instant.now();
        this.createdAt = this.changedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Incident getIncident() { return incident; }
    public void setIncident(Incident incident) { this.incident = incident; }

    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    public String getPreviousStatus() { return fromStatus; }
    public void setPreviousStatus(String previousStatus) { this.fromStatus = previousStatus; }

    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    public String getNewStatus() { return toStatus; }
    public void setNewStatus(String newStatus) { this.toStatus = newStatus; }

    public User getChangedByUser() { return changedByUser; }
    public void setChangedByUser(User changedByUser) { this.changedByUser = changedByUser; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
        this.changedByUsername = changedBy;
    }

    public String getChangedByUsername() { return changedByUsername; }
    public void setChangedByUsername(String changedByUsername) {
        this.changedByUsername = changedByUsername;
        if (this.changedBy == null) this.changedBy = changedByUsername;
    }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) {
        this.remarks = remarks;
        this.notes = remarks;
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) {
        this.notes = notes;
        if (this.remarks == null) this.remarks = notes;
    }

    public Instant getChangedAt() { return changedAt; }
    public void setChangedAt(Instant changedAt) { this.changedAt = changedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
