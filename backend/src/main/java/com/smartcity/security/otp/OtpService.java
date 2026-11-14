package com.smartcity.security.otp;

import com.smartcity.security.otp.dto.OtpResponse;
import com.smartcity.security.otp.dto.SendOtpRequest;
import com.smartcity.security.otp.dto.VerifyOtpRequest;

import java.util.UUID;

public interface OtpService {

    /**
     * Sends a 6-digit verification OTP to the target mobile number, enforcing rate limits and cooldowns.
     */
    OtpResponse sendOtp(SendOtpRequest request, UUID currentUserId);

    /**
     * Verifies the provided OTP, checking expiry, attempt limits, and hash match.
     */
    OtpResponse verifyOtp(VerifyOtpRequest request, UUID currentUserId);

    /**
     * Verifies a login OTP with purpose 'LOGIN' atomically with replay protection.
     */
    OtpResponse verifyLoginOtp(String mobileNumber, String candidateOtp);

    /**
     * Validates whether an OTP is correct for a mobile number without consuming it,
     * or consumes it for atomic operations like registration.
     */
    boolean validateAndConsumeOtp(String mobileNumber, String otp, String purpose);

    /**
     * Returns true if the user with the given ID has a verified mobile number.
     */
    boolean isMobileVerified(UUID userId);
}
