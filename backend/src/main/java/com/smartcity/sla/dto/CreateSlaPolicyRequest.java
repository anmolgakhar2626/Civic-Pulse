package com.smartcity.sla.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateSlaPolicyRequest {

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Severity is required (CRITICAL, HIGH, MEDIUM, LOW)")
    private String severity;

    @NotNull(message = "Response time limit in minutes is required")
    @Min(value = 1, message = "Response time must be at least 1 minute")
    private Integer responseTimeMinutes;

    @NotNull(message = "Resolution time limit in minutes is required")
    @Min(value = 1, message = "Resolution time must be at least 1 minute")
    private Integer resolutionTimeMinutes;

    private Boolean enabled = true;

    public CreateSlaPolicyRequest() {}

    public CreateSlaPolicyRequest(String category, String severity, Integer responseTimeMinutes, Integer resolutionTimeMinutes, Boolean enabled) {
        this.category = category;
        this.severity = severity;
        this.responseTimeMinutes = responseTimeMinutes;
        this.resolutionTimeMinutes = resolutionTimeMinutes;
        this.enabled = enabled != null ? enabled : true;
    }

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

