package com.smartcity.complaint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VerifyComplaintRequest {

    @NotBlank(message = "Verification status is required")
    @Pattern(regexp = "GENUINE|REJECTED_SPAM|DUPLICATE|UNVERIFIED",
            message = "Verification status must be GENUINE, REJECTED_SPAM, DUPLICATE, or UNVERIFIED")
    private String verificationStatus;

    private String notes;

    public VerifyComplaintRequest() {}

    public VerifyComplaintRequest(String verificationStatus, String notes) {
        this.verificationStatus = verificationStatus;
        this.notes = notes;
    }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
