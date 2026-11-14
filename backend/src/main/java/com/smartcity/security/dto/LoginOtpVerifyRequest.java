package com.smartcity.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Citizen Mobile OTP Login Request Payload to verify OTP and obtain JWT")
public class LoginOtpVerifyRequest {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Mobile number must be 10 to 15 digits (optional leading +)")
    @Schema(description = "Registered citizen mobile number", example = "+919876543210")
    private String mobileNumber;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be exactly 6 digits")
    @Schema(description = "6-digit OTP code received via SMS", example = "123456")
    private String otp;

    public LoginOtpVerifyRequest() {}

    public LoginOtpVerifyRequest(String mobileNumber, String otp) {
        this.mobileNumber = mobileNumber;
        this.otp = otp;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}

