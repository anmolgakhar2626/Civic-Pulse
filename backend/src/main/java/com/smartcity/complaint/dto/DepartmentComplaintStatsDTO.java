package com.smartcity.complaint.dto;

import java.util.UUID;

public class DepartmentComplaintStatsDTO {
    private UUID departmentId;
    private String departmentCode;
    private String departmentName;
    private long totalAssigned;
    private long newlyReceived;
    private long pending;
    private long inProgress;
    private long resolved;
    private long approachingDeadline;
    private long breachedDeadline;
    private double avgResolutionTimeHours;

    public DepartmentComplaintStatsDTO() {}

    public DepartmentComplaintStatsDTO(UUID departmentId, String departmentCode, String departmentName,
                                       long totalAssigned, long newlyReceived, long pending,
                                       long inProgress, long resolved, long approachingDeadline,
                                       long breachedDeadline, double avgResolutionTimeHours) {
        this.departmentId = departmentId;
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.totalAssigned = totalAssigned;
        this.newlyReceived = newlyReceived;
        this.pending = pending;
        this.inProgress = inProgress;
        this.resolved = resolved;
        this.approachingDeadline = approachingDeadline;
        this.breachedDeadline = breachedDeadline;
        this.avgResolutionTimeHours = avgResolutionTimeHours;
    }

    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public long getTotalAssigned() { return totalAssigned; }
    public void setTotalAssigned(long totalAssigned) { this.totalAssigned = totalAssigned; }

    public long getNewlyReceived() { return newlyReceived; }
    public void setNewlyReceived(long newlyReceived) { this.newlyReceived = newlyReceived; }

    public long getPending() { return pending; }
    public void setPending(long pending) { this.pending = pending; }

    public long getInProgress() { return inProgress; }
    public void setInProgress(long inProgress) { this.inProgress = inProgress; }

    public long getResolved() { return resolved; }
    public void setResolved(long resolved) { this.resolved = resolved; }

    public long getApproachingDeadline() { return approachingDeadline; }
    public void setApproachingDeadline(long approachingDeadline) { this.approachingDeadline = approachingDeadline; }

    public long getBreachedDeadline() { return breachedDeadline; }
    public void setBreachedDeadline(long breachedDeadline) { this.breachedDeadline = breachedDeadline; }

    public long getOverdueComplaints() { return breachedDeadline; }
    public void setOverdueComplaints(long overdueComplaints) { this.breachedDeadline = overdueComplaints; }

    public double getAvgResolutionTimeHours() { return avgResolutionTimeHours; }
    public void setAvgResolutionTimeHours(double avgResolutionTimeHours) { this.avgResolutionTimeHours = avgResolutionTimeHours; }
}

