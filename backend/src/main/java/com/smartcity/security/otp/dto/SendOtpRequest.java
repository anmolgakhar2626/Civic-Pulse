package com.smartcity.security.otp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SendOtpRequest {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Mobile number must be 10 to 15 digits (optional leading +)")
    private String mobileNumber;

    private String purpose = "REGISTRATION";

    public SendOtpRequest() {}

    public SendOtpRequest(String mobileNumber, String purpose) {
        this.mobileNumber = mobileNumber;
        this.purpose = purpose != null ? purpose : "REGISTRATION";
    }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
}
