package com.smartcity.complaint.dto;

import jakarta.validation.constraints.NotBlank;

public class ChangePriorityRequest {
    @NotBlank(message = "Priority is required")
    private String priority;

    private String reason;

    public ChangePriorityRequest() {}

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

