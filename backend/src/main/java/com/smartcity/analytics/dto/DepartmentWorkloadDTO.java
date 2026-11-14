package com.smartcity.analytics.dto;

import java.util.UUID;

public class DepartmentWorkloadDTO {
    private UUID departmentId;
    private String departmentName;
    private String departmentCode;
    private long totalIncidents;
    private long activeIncidents;
    private long resolvedIncidents;
    private double avgResolutionHours;
    private double slaComplianceRate;

    public DepartmentWorkloadDTO() {}

    public DepartmentWorkloadDTO(UUID departmentId, String departmentName, String departmentCode,
                                 long totalIncidents, long activeIncidents, long resolvedIncidents,
                                 double avgResolutionHours, double slaComplianceRate) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.departmentCode = departmentCode;
        this.totalIncidents = totalIncidents;
        this.activeIncidents = activeIncidents;
        this.resolvedIncidents = resolvedIncidents;
        this.avgResolutionHours = avgResolutionHours;
        this.slaComplianceRate = slaComplianceRate;
    }

    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public long getTotalIncidents() { return totalIncidents; }
    public void setTotalIncidents(long totalIncidents) { this.totalIncidents = totalIncidents; }

    public long getActiveIncidents() { return activeIncidents; }
    public void setActiveIncidents(long activeIncidents) { this.activeIncidents = activeIncidents; }

    public long getResolvedIncidents() { return resolvedIncidents; }
    public void setResolvedIncidents(long resolvedIncidents) { this.resolvedIncidents = resolvedIncidents; }

    public double getAvgResolutionHours() { return avgResolutionHours; }
    public void setAvgResolutionHours(double avgResolutionHours) { this.avgResolutionHours = avgResolutionHours; }

    public double getSlaComplianceRate() { return slaComplianceRate; }
    public void setSlaComplianceRate(double slaComplianceRate) { this.slaComplianceRate = slaComplianceRate; }
}
