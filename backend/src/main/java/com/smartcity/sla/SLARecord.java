package com.smartcity.sla;

import com.smartcity.incident.Incident;
import com.smartcity.priority.PriorityTier;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sla_records")
public class SLARecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false, unique = true)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private SLAPolicy policy;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_tier", length = 20)
    private PriorityTier priorityTier;

    @Column(name = "target_minutes", nullable = false)
    private Integer targetMinutes;

    @Column(name = "target_sla_minutes")
    private Integer targetSlaMinutes;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "deadline_at", nullable = false)
    private Instant deadlineAt;

    @Column(name = "response_deadline")
    private Instant responseDeadline;

    @Column(name = "resolution_deadline")
    private Instant resolutionDeadline;

    @Column(name = "first_responded_at")
    private Instant firstRespondedAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SLAStatus status = SLAStatus.ON_TRACK;

    @Column(name = "current_sla_status")
    private String currentSlaStatus;

    @Column(name = "remaining_minutes")
    private Long remainingMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "escalation_level", nullable = false, length = 50)
    private EscalationLevel escalationLevel = EscalationLevel.NONE;

    @Column(name = "last_escalated_at")
    private Instant lastEscalatedAt;

    @Column(nullable = false)
    private Boolean breached = false;

    @Column(name = "breached_at")
    private Instant breachedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = Instant.now();
        }
        if (this.startedAt == null) {
            this.startedAt = this.createdAt;
        }
        if (this.status == null) {
            this.status = SLAStatus.ON_TRACK;
        }
        if (this.currentSlaStatus == null && this.status != null) {
            this.currentSlaStatus = this.status.name();
        }
        if (this.escalationLevel == null) {
            this.escalationLevel = EscalationLevel.NONE;
        }
        if (this.targetSlaMinutes == null && this.targetMinutes != null) {
            this.targetSlaMinutes = this.targetMinutes;
        } else if (this.targetMinutes == null && this.targetSlaMinutes != null) {
            this.targetMinutes = this.targetSlaMinutes;
        }
        if (this.resolutionDeadline == null && this.deadlineAt != null) {
            this.resolutionDeadline = this.deadlineAt;
        } else if (this.deadlineAt == null && this.resolutionDeadline != null) {
            this.deadlineAt = this.resolutionDeadline;
        }
        if (this.firstRespondedAt == null && this.acknowledgedAt != null) {
            this.firstRespondedAt = this.acknowledgedAt;
        } else if (this.acknowledgedAt == null && this.firstRespondedAt != null) {
            this.acknowledgedAt = this.firstRespondedAt;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
        if (this.targetSlaMinutes == null && this.targetMinutes != null) {
            this.targetSlaMinutes = this.targetMinutes;
        }
        if (this.status != null) {
            this.currentSlaStatus = this.status.name();
        }
        if (this.resolutionDeadline == null && this.deadlineAt != null) {
            this.resolutionDeadline = this.deadlineAt;
        } else if (this.deadlineAt == null && this.resolutionDeadline != null) {
            this.deadlineAt = this.resolutionDeadline;
        }
        if (this.firstRespondedAt == null && this.acknowledgedAt != null) {
            this.firstRespondedAt = this.acknowledgedAt;
        } else if (this.acknowledgedAt == null && this.firstRespondedAt != null) {
            this.acknowledgedAt = this.firstRespondedAt;
        }
    }

    public SLARecord() {}

    public SLARecord(Incident incident, PriorityTier priorityTier, Integer targetMinutes, Instant startedAt, Instant deadlineAt) {
        this.incident = incident;
        this.priorityTier = priorityTier;
        this.targetMinutes = targetMinutes;
        this.targetSlaMinutes = targetMinutes;
        this.startedAt = startedAt != null ? startedAt : Instant.now();
        this.deadlineAt = deadlineAt;
        this.resolutionDeadline = deadlineAt;
        this.status = SLAStatus.ON_TRACK;
        this.currentSlaStatus = SLAStatus.ON_TRACK.name();
        this.escalationLevel = EscalationLevel.NONE;
        this.breached = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public SLARecord(Incident incident, Integer targetMinutes, Instant deadlineAt) {
        this(incident, incident != null ? incident.getTier() : null, targetMinutes, Instant.now(), deadlineAt);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Incident getIncident() { return incident; }
    public void setIncident(Incident incident) { this.incident = incident; }

    public SLAPolicy getPolicy() { return policy; }
    public void setPolicy(SLAPolicy policy) { this.policy = policy; }

    public PriorityTier getPriorityTier() { return priorityTier; }
    public void setPriorityTier(PriorityTier priorityTier) { this.priorityTier = priorityTier; }

    public Integer getTargetMinutes() { return targetMinutes; }
    public void setTargetMinutes(Integer targetMinutes) {
        this.targetMinutes = targetMinutes;
        this.targetSlaMinutes = targetMinutes;
    }

    public Integer getTargetSlaMinutes() { return targetMinutes; }
    public void setTargetSlaMinutes(Integer targetSlaMinutes) {
        this.targetMinutes = targetSlaMinutes;
        this.targetSlaMinutes = targetSlaMinutes;
    }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(Instant deadlineAt) {
        this.deadlineAt = deadlineAt;
        if (this.resolutionDeadline == null) {
            this.resolutionDeadline = deadlineAt;
        }
    }

    public Instant getResponseDeadline() { return responseDeadline; }
    public void setResponseDeadline(Instant responseDeadline) { this.responseDeadline = responseDeadline; }

    public Instant getResolutionDeadline() { return resolutionDeadline; }
    public void setResolutionDeadline(Instant resolutionDeadline) {
        this.resolutionDeadline = resolutionDeadline;
        this.deadlineAt = resolutionDeadline;
    }

    public Instant getFirstRespondedAt() { return firstRespondedAt; }
    public void setFirstRespondedAt(Instant firstRespondedAt) {
        this.firstRespondedAt = firstRespondedAt;
        this.acknowledgedAt = firstRespondedAt;
    }

    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
        this.firstRespondedAt = acknowledgedAt;
    }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public UUID getIncidentId() {
        return incident != null ? incident.getId() : null;
    }

    public SLAStatus getStatus() { return status; }
    public void setStatus(SLAStatus status) {
        this.status = status;
        if (status != null) {
            this.currentSlaStatus = status.name();
        }
    }

    public String getCurrentSlaStatus() { return currentSlaStatus; }
    public void setCurrentSlaStatus(String currentSlaStatus) { this.currentSlaStatus = currentSlaStatus; }

    public Long getRemainingMinutes() { return remainingMinutes; }
    public void setRemainingMinutes(Long remainingMinutes) { this.remainingMinutes = remainingMinutes; }

    public EscalationLevel getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(EscalationLevel escalationLevel) { this.escalationLevel = escalationLevel; }

    public Instant getLastEscalatedAt() { return lastEscalatedAt; }
    public void setLastEscalatedAt(Instant lastEscalatedAt) { this.lastEscalatedAt = lastEscalatedAt; }

    public Boolean getBreached() { return breached; }
    public void setBreached(Boolean breached) { this.breached = breached; }

    public Instant getBreachedAt() { return breachedAt; }
    public void setBreachedAt(Instant breachedAt) { this.breachedAt = breachedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
