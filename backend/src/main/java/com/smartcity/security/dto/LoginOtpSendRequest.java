package com.smartcity.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Citizen Mobile OTP Login Request Payload to dispatch OTP")
public class LoginOtpSendRequest {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Mobile number must be 10 to 15 digits (optional leading +)")
    @Schema(description = "Registered citizen mobile number", example = "+919876543210")
    private String mobileNumber;

    public LoginOtpSendRequest() {}

    public LoginOtpSendRequest(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }
}

