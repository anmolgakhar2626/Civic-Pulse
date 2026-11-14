package com.smartcity.sla.dto;

import com.smartcity.sla.SLAPolicy;
import java.time.Instant;
import java.util.UUID;

public class SlaPolicyResponse {

    private UUID id;
    private String category;
    private String severity;
    private Integer responseTimeMinutes;
    private Integer resolutionTimeMinutes;
    private Boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;

    public SlaPolicyResponse() {}

    public static SlaPolicyResponse fromEntity(SLAPolicy policy) {
        if (policy == null) return null;
        SlaPolicyResponse resp = new SlaPolicyResponse();
        resp.setId(policy.getId());
        resp.setCategory(policy.getCategory());
        resp.setSeverity(policy.getSeverity());
        resp.setResponseTimeMinutes(policy.getResponseTimeMinutes());
        resp.setResolutionTimeMinutes(policy.getResolutionTimeMinutes());
        resp.setEnabled(policy.getEnabled());
        resp.setCreatedAt(policy.getCreatedAt());
        resp.setUpdatedAt(policy.getUpdatedAt());
        return resp;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public Integer getResponseTimeMinutes() { return responseTimeMinutes; }
    public void setResponseTimeMinutes(Integer responseTimeMinutes) { this.responseTimeMinutes = responseTimeMinutes; }

    public Integer getResolutionTimeMinutes() { return resolutionTimeMinutes; }
    public void setResolutionTimeMinutes(Integer resolutionTimeMinutes) { this.resolutionTimeMinutes = resolutionTimeMinutes; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

