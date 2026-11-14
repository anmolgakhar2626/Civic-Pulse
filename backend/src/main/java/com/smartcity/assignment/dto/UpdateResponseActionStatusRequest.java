package com.smartcity.assignment.dto;

import com.smartcity.assignment.ResponseActionStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateResponseActionStatusRequest {

    @NotNull(message = "Status is required")
    private ResponseActionStatus status;

    public UpdateResponseActionStatusRequest() {}

    public UpdateResponseActionStatusRequest(ResponseActionStatus status) {
        this.status = status;
    }

    public ResponseActionStatus getStatus() { return status; }
    public void setStatus(ResponseActionStatus status) { this.status = status; }
}
