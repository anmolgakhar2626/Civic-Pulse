package com.smartcity.grievance.portal.dto;

import jakarta.validation.constraints.NotBlank;

public class ForwardGrievanceRequest {

    private String portalCode = "CPGRAMS_SANDBOX";
    private String simulateFailure; // For testing: "TIMEOUT", "ERROR"

    public ForwardGrievanceRequest() {}

    public ForwardGrievanceRequest(String portalCode) {
        this.portalCode = portalCode;
    }

    public String getPortalCode() {
        return portalCode != null && !portalCode.isBlank() ? portalCode : "CPGRAMS_SANDBOX";
    }

    public void setPortalCode(String portalCode) {
        this.portalCode = portalCode;
    }

    public String getSimulateFailure() {
        return simulateFailure;
    }

    public void setSimulateFailure(String simulateFailure) {
        this.simulateFailure = simulateFailure;
    }
}
