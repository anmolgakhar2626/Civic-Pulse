package com.smartcity.incident;

import com.smartcity.department.Department;
import com.smartcity.department.Team;
import com.smartcity.gis.Location;
import com.smartcity.priority.IncidentPriority;
import com.smartcity.priority.IncidentTier;
import com.smartcity.priority.PriorityTier;
import com.smartcity.priority.PublicExposure;
import com.smartcity.priority.Severity;
import com.smartcity.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "incident_number", nullable = false, unique = true, length = 50)
    private String incidentNumber;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "location_name", length = 255)
    private String locationName;

    @Column(name = "incident_type", length = 100)
    private String incidentType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IncidentSource source; // CITIZEN, IOT, EXTERNAL_API, OPERATOR, SYSTEM

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 255)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "reported_time", nullable = false)
    private Instant reportedAt = Instant.now();

    @Column(name = "detection_time", nullable = false)
    private Instant detectedAt = Instant.now();

    @Column(name = "severity_indicator", length = 50)
    private String severityIndicator;

    @Column(name = "population_exposure", length = 50)
    private String populationExposure;

    @Column(name = "infrastructure_importance", length = 50)
    private String infrastructureImportance;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_tier", length = 20)
    private IncidentTier assignedTier;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private IncidentPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_tier", length = 20)
    private PriorityTier tier;

    @Column(name = "priority_label", length = 50)
    private String priorityLabel;

    @Column(name = "priority_reason", columnDefinition = "TEXT")
    private String priorityReason;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "public_exposure", length = 50)
    private PublicExposure publicExposure;

    @Column(name = "safety_impact", length = 100)
    private String safetyImpact;

    @Column(name = "priority_evaluated_at")
    private Instant priorityEvaluatedAt;

    @Column(name = "priority_rule_id", length = 100)
    private String priorityRuleId;

    @Column(name = "sla_minutes")
    private Integer slaMinutes;

    @Column(name = "sla_deadline")
    private Instant slaDeadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department assignedDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_team_id")
    private Team assignedTeam;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "assigned_by", length = 100)
    private String assignedBy;

    @Column(name = "assignment_reason", columnDefinition = "TEXT")
    private String assignmentReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 50)
    private IncidentStatus status = IncidentStatus.DETECTED;

    @Column(name = "status", length = 50)
    private String legacyStatus = "OPEN";

    @Column(name = "escalation_state", nullable = false, length = 50)
    private String escalationState = "NONE";

    @Column(name = "resolution_time")
    private Instant resolutionTime;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "evidence_url", length = 500)
    private String evidenceUrl;

    @Column(name = "reason_notes", columnDefinition = "TEXT")
    private String reasonNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by")
    private User reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (this.title == null && this.incidentType != null) {
            this.title = this.incidentType;
        }
        if (this.category == null && this.incidentType != null) {
            this.category = this.incidentType;
        }
        if (this.incidentType == null && this.category != null) {
            this.incidentType = this.category;
        }
        if (this.locationName == null && this.address != null) {
            this.locationName = this.address;
        }
        if (this.address == null && this.locationName != null) {
            this.address = this.locationName;
        }
        if (this.reportedBy == null && this.createdBy != null) {
            this.reportedBy = this.createdBy;
        }
        if (this.createdBy == null && this.reportedBy != null) {
            this.createdBy = this.reportedBy;
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = Instant.now();
        }
        if (this.reportedAt == null) {
            this.reportedAt = Instant.now();
        }
        if (this.detectedAt == null) {
            this.detectedAt = Instant.now();
        }
        if (this.status == null) {
            this.status = IncidentStatus.DETECTED;
        }
        if (this.escalationState == null) {
            this.escalationState = "NONE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Incident() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getIncidentNumber() { return incidentNumber; }
    public void setIncidentNumber(String incidentNumber) { this.incidentNumber = incidentNumber; }

    public String getIncidentType() { return incidentType; }
    public void setIncidentType(String incidentType) { this.incidentType = incidentType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public IncidentSource getSource() { return source; }
    public void setSource(IncidentSource source) { this.source = source; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public Instant getReportedAt() { return reportedAt; }
    public void setReportedAt(Instant reportedAt) { this.reportedAt = reportedAt; }
    public Instant getReportedTime() { return reportedAt; }
    public void setReportedTime(Instant reportedTime) { this.reportedAt = reportedTime; }

    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }
    public Instant getDetectionTime() { return detectedAt; }
    public void setDetectionTime(Instant detectionTime) { this.detectedAt = detectionTime; }

    public String getSeverityIndicator() { return severityIndicator; }
    public void setSeverityIndicator(String severityIndicator) {
        this.severityIndicator = severityIndicator;
        if (severityIndicator != null) {
            try {
                this.severity = Severity.valueOf(severityIndicator.toUpperCase());
            } catch (Exception ignored) {}
        }
    }

    public String getPopulationExposure() { return populationExposure; }
    public void setPopulationExposure(String populationExposure) { this.populationExposure = populationExposure; }

    public String getInfrastructureImportance() { return infrastructureImportance; }
    public void setInfrastructureImportance(String infrastructureImportance) { this.infrastructureImportance = infrastructureImportance; }

    public IncidentTier getAssignedTier() { return assignedTier; }
    public void setAssignedTier(IncidentTier assignedTier) { this.assignedTier = assignedTier; }

    public IncidentPriority getPriority() { return priority; }
    public void setPriority(IncidentPriority priority) { this.priority = priority; }

    public PriorityTier getTier() { return tier; }
    public PriorityTier getPriorityTier() { return tier; }
    public void setTier(PriorityTier tier) {
        this.tier = tier;
        if (tier != null) {
            this.priorityLabel = tier.name();
            try {
                this.assignedTier = IncidentTier.valueOf("TIER_" + tier.getTierNumber());
                this.priority = IncidentPriority.valueOf(tier.name());
            } catch (Exception ignored) {}
        }
    }
    public void setPriorityTier(PriorityTier tier) { setTier(tier); }

    public String getPriorityLabel() { return priorityLabel; }
    public void setPriorityLabel(String priorityLabel) { this.priorityLabel = priorityLabel; }

    public String getPriorityReason() { return priorityReason; }
    public void setPriorityReason(String priorityReason) { this.priorityReason = priorityReason; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) {
        this.severity = severity;
        if (severity != null) {
            this.severityIndicator = severity.name();
        }
    }

    public PublicExposure getPublicExposure() { return publicExposure; }
    public void setPublicExposure(PublicExposure publicExposure) {
        this.publicExposure = publicExposure;
        if (publicExposure != null) {
            this.populationExposure = publicExposure.name();
        }
    }

    public String getSafetyImpact() { return safetyImpact; }
    public void setSafetyImpact(String safetyImpact) { this.safetyImpact = safetyImpact; }

    public Instant getPriorityEvaluatedAt() { return priorityEvaluatedAt; }
    public void setPriorityEvaluatedAt(Instant priorityEvaluatedAt) { this.priorityEvaluatedAt = priorityEvaluatedAt; }

    public String getPriorityRuleId() { return priorityRuleId; }
    public void setPriorityRuleId(String priorityRuleId) { this.priorityRuleId = priorityRuleId; }

    public Integer getSlaMinutes() { return slaMinutes; }
    public void setSlaMinutes(Integer slaMinutes) { this.slaMinutes = slaMinutes; }

    public Instant getSlaDeadline() { return slaDeadline; }
    public void setSlaDeadline(Instant slaDeadline) { this.slaDeadline = slaDeadline; }

    public Department getAssignedDepartment() { return assignedDepartment; }
    public void setAssignedDepartment(Department assignedDepartment) { this.assignedDepartment = assignedDepartment; }
    public Department getDepartment() { return assignedDepartment; }
    public void setDepartment(Department department) { this.assignedDepartment = department; }

    public Team getAssignedTeam() { return assignedTeam; }
    public void setAssignedTeam(Team assignedTeam) { this.assignedTeam = assignedTeam; }

    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) {
        this.status = status;
        this.legacyStatus = status != null ? status.name() : "OPEN";
    }

    public String getLegacyStatus() { return legacyStatus; }
    public void setLegacyStatus(String legacyStatus) { this.legacyStatus = legacyStatus; }
    public IncidentStatus getCurrentStatus() { return status; }
    public void setCurrentStatus(IncidentStatus currentStatus) { this.status = currentStatus; }

    public String getEscalationState() { return escalationState; }
    public void setEscalationState(String escalationState) { this.escalationState = escalationState; }

    public Instant getResolutionTime() { return resolutionTime; }
    public void setResolutionTime(Instant resolutionTime) { this.resolutionTime = resolutionTime; }

    public Instant getResolvedAt() { return resolutionTime; }
    public void setResolvedAt(Instant resolvedAt) { this.resolutionTime = resolvedAt; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public String getEvidenceUrl() { return evidenceUrl; }
    public void setEvidenceUrl(String evidenceUrl) { this.evidenceUrl = evidenceUrl; }

    public String getReasonNotes() { return reasonNotes; }
    public void setReasonNotes(String reasonNotes) { this.reasonNotes = reasonNotes; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public String getAssignmentReason() { return assignmentReason; }
    public void setAssignmentReason(String assignmentReason) { this.assignmentReason = assignmentReason; }

    public String getTitle() { return title != null ? title : incidentType; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category != null ? category : incidentType; }
    public void setCategory(String category) { this.category = category; }

    public String getLocationName() { return locationName != null ? locationName : address; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public User getReportedBy() { return reportedBy != null ? reportedBy : createdBy; }
    public void setReportedBy(User reportedBy) { this.reportedBy = reportedBy; }

    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }
}
