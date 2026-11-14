package com.smartcity.complaint.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class AssignComplaintRequest {
    private UUID departmentId;
    private UUID assignedToUserId;
    private String reason;

    public AssignComplaintRequest() {}

    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }

    public UUID getAssignedToUserId() { return assignedToUserId; }
    public void setAssignedToUserId(UUID assignedToUserId) { this.assignedToUserId = assignedToUserId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getNotes() { return reason; }
    public void setNotes(String notes) { this.reason = notes; }
}
