package com.smartcity.complaint.dto;

import java.util.List;
import java.util.Map;

public class AdminComplaintStatsDTO {
    private long totalRegisteredCitizens;
    private long totalComplaints;
    private long pendingComplaints;
    private long inProgressComplaints;
    private long resolvedComplaints;
    private long closedComplaints;
    private long overdueSlaComplaints;
    private List<DepartmentComplaintStatsDTO> departmentStats;
    private Map<String, Long> priorityDistribution;
    private Map<String, Long> statusDistribution;
    private List<ComplaintStatusHistoryDTO> recentActivities;

    public AdminComplaintStatsDTO() {}

    public long getTotalRegisteredCitizens() { return totalRegisteredCitizens; }
    public void setTotalRegisteredCitizens(long totalRegisteredCitizens) { this.totalRegisteredCitizens = totalRegisteredCitizens; }

    public long getTotalComplaints() { return totalComplaints; }
    public void setTotalComplaints(long totalComplaints) { this.totalComplaints = totalComplaints; }

    public long getPendingComplaints() { return pendingComplaints; }
    public void setPendingComplaints(long pendingComplaints) { this.pendingComplaints = pendingComplaints; }

    public long getInProgressComplaints() { return inProgressComplaints; }
    public void setInProgressComplaints(long inProgressComplaints) { this.inProgressComplaints = inProgressComplaints; }

    public long getResolvedComplaints() { return resolvedComplaints; }
    public void setResolvedComplaints(long resolvedComplaints) { this.resolvedComplaints = resolvedComplaints; }

    public long getClosedComplaints() { return closedComplaints; }
    public void setClosedComplaints(long closedComplaints) { this.closedComplaints = closedComplaints; }

    public long getOverdueSlaComplaints() { return overdueSlaComplaints; }
    public void setOverdueSlaComplaints(long overdueSlaComplaints) { this.overdueSlaComplaints = overdueSlaComplaints; }

    public long getOverdueComplaints() { return overdueSlaComplaints; }
    public void setOverdueComplaints(long overdueComplaints) { this.overdueSlaComplaints = overdueComplaints; }

    public List<DepartmentComplaintStatsDTO> getDepartmentStats() { return departmentStats; }
    public void setDepartmentStats(List<DepartmentComplaintStatsDTO> departmentStats) { this.departmentStats = departmentStats; }

    public List<DepartmentComplaintStatsDTO> getByDepartment() { return departmentStats; }
    public void setByDepartment(List<DepartmentComplaintStatsDTO> byDepartment) { this.departmentStats = byDepartment; }

    public Map<String, Long> getPriorityDistribution() { return priorityDistribution; }
    public void setPriorityDistribution(Map<String, Long> priorityDistribution) { this.priorityDistribution = priorityDistribution; }

    public Map<String, Long> getStatusDistribution() { return statusDistribution; }
    public void setStatusDistribution(Map<String, Long> statusDistribution) { this.statusDistribution = statusDistribution; }

    public List<ComplaintStatusHistoryDTO> getRecentActivities() { return recentActivities; }
    public void setRecentActivities(List<ComplaintStatusHistoryDTO> recentActivities) { this.recentActivities = recentActivities; }
}
