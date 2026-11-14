package com.smartcity.assignment.dto;

import com.smartcity.assignment.ResponseActionStatus;
import com.smartcity.assignment.ResponseActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateResponseActionRequest {

    @NotNull(message = "Action type is required")
    private ResponseActionType actionType;

    @NotBlank(message = "Description is required")
    private String description;

    private ResponseActionStatus status;
    private String createdBy;

    public CreateResponseActionRequest() {}

    public CreateResponseActionRequest(ResponseActionType actionType, String description, ResponseActionStatus status, String createdBy) {
        this.actionType = actionType;
        this.description = description;
        this.status = status;
        this.createdBy = createdBy;
    }

    public ResponseActionType getActionType() { return actionType; }
    public void setActionType(ResponseActionType actionType) { this.actionType = actionType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ResponseActionStatus getStatus() { return status; }
    public void setStatus(ResponseActionStatus status) { this.status = status; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
