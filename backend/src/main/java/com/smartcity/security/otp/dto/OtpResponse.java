package com.smartcity.security.otp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpResponse {

    private boolean success;
    private String message;
    private String mobileNumber;
    private Integer cooldownSeconds;
    private Integer expiresInSeconds;
    private Integer remainingAttempts;

    public OtpResponse() {}

    public OtpResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static OtpResponse sent(String mobileNumber, int cooldownSeconds, int expiresInSeconds) {
        return sent(mobileNumber, cooldownSeconds, expiresInSeconds, "OTP sent to your registered mobile number.");
    }

    public static OtpResponse sent(String mobileNumber, int cooldownSeconds, int expiresInSeconds, String message) {
        OtpResponse resp = new OtpResponse(true, message != null ? message : "OTP sent to your registered mobile number.");
        resp.mobileNumber = mobileNumber;
        resp.cooldownSeconds = cooldownSeconds;
        resp.expiresInSeconds = expiresInSeconds;
        return resp;
    }

    public static OtpResponse verified(String mobileNumber, String message) {
        OtpResponse resp = new OtpResponse(true, message);
        resp.mobileNumber = mobileNumber;
        return resp;
    }

    public static OtpResponse failure(String message, Integer remainingAttempts) {
        OtpResponse resp = new OtpResponse(false, message);
        resp.remainingAttempts = remainingAttempts;
        return resp;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public Integer getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(Integer cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }

    public Integer getExpiresInSeconds() { return expiresInSeconds; }
    public void setExpiresInSeconds(Integer expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }

    public Integer getRemainingAttempts() { return remainingAttempts; }
    public void setRemainingAttempts(Integer remainingAttempts) { this.remainingAttempts = remainingAttempts; }
}
