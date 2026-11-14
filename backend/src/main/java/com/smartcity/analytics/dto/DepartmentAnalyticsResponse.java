package com.smartcity.analytics.dto;

import java.util.List;

public class DepartmentAnalyticsResponse {
    private List<DepartmentWorkloadDTO> departments;

    public DepartmentAnalyticsResponse() {}

    public DepartmentAnalyticsResponse(List<DepartmentWorkloadDTO> departments) {
        this.departments = departments;
    }

    public List<DepartmentWorkloadDTO> getDepartments() { return departments; }
    public void setDepartments(List<DepartmentWorkloadDTO> departments) { this.departments = departments; }
}
