package com.smartcity.priority;

import com.smartcity.department.Department;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "priority_rules")
public class PriorityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rule_id", nullable = false, unique = true, length = 50)
    private String ruleId;

    @Column(length = 150)
    private String name;

    @Column(name = "incident_type", nullable = false, length = 100)
    private String incidentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PriorityTier tier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PriorityTier priority;

    @Column(name = "sla_minutes", nullable = false)
    private Integer slaMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_department_id")
    private Department defaultDepartment;

    @Column(name = "conditions_json", columnDefinition = "TEXT")
    private String conditionsJson;

    @Column(name = "reason_template", columnDefinition = "TEXT")
    private String reasonTemplate;

    @Column(name = "recommended_actions_json", columnDefinition = "TEXT")
    private String recommendedActionsJson;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public PriorityRule() {}

    public PriorityRule(String ruleId, String name, String incidentType, PriorityTier tier,
                        Integer slaMinutes, String conditionsJson, String reasonTemplate, Boolean enabled) {
        this.ruleId = ruleId;
        this.name = name;
        this.incidentType = incidentType;
        this.tier = tier;
        this.priority = tier;
        this.slaMinutes = slaMinutes != null ? slaMinutes : (tier != null ? tier.getTargetSlaMinutes() : 240);
        this.conditionsJson = conditionsJson;
        this.reasonTemplate = reasonTemplate;
        this.enabled = enabled != null ? enabled : true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public PriorityRule(String ruleId, String incidentType, IncidentTier tier, IncidentPriority priority,
                        Integer slaMinutes, Department defaultDepartment, String conditionsJson, String recommendedActionsJson) {
        this.ruleId = ruleId;
        this.name = incidentType;
        this.incidentType = incidentType;
        this.tier = tier != null ? PriorityTier.fromString(tier.name()) : PriorityTier.HIGH;
        this.priority = this.tier;
        this.slaMinutes = slaMinutes != null ? slaMinutes : 240;
        this.defaultDepartment = defaultDepartment;
        this.conditionsJson = conditionsJson;
        this.recommendedActionsJson = recommendedActionsJson;
        this.reasonTemplate = recommendedActionsJson;
        this.enabled = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getRuleCode() { return ruleId; }
    public void setRuleCode(String ruleCode) { this.ruleId = ruleCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIncidentType() { return incidentType; }
    public void setIncidentType(String incidentType) { this.incidentType = incidentType; }

    public PriorityTier getTier() { return tier; }
    public void setTier(PriorityTier tier) {
        this.tier = tier;
        if (this.priority == null) {
            this.priority = tier;
        }
    }

    public PriorityTier getPriority() { return priority; }
    public void setPriority(PriorityTier priority) { this.priority = priority; }

    public Integer getSlaMinutes() { return slaMinutes; }
    public void setSlaMinutes(Integer slaMinutes) { this.slaMinutes = slaMinutes; }

    public Department getDefaultDepartment() { return defaultDepartment; }
    public void setDefaultDepartment(Department defaultDepartment) { this.defaultDepartment = defaultDepartment; }

    public String getConditionsJson() { return conditionsJson; }
    public void setConditionsJson(String conditionsJson) { this.conditionsJson = conditionsJson; }

    public String getReasonTemplate() { return reasonTemplate; }
    public void setReasonTemplate(String reasonTemplate) { this.reasonTemplate = reasonTemplate; }

    public String getRecommendedActionsJson() { return recommendedActionsJson; }
    public void setRecommendedActionsJson(String recommendedActionsJson) { this.recommendedActionsJson = recommendedActionsJson; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
