package com.smartcity.sla.dto;

import jakarta.validation.constraints.Min;

public class UpdateSlaPolicyRequest {

    private String category;
    private String severity;

    @Min(value = 1, message = "Response time must be at least 1 minute")
    private Integer responseTimeMinutes;

    @Min(value = 1, message = "Resolution time must be at least 1 minute")
    private Integer resolutionTimeMinutes;

    private Boolean enabled;

    public UpdateSlaPolicyRequest() {}

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
}

