package com.smartcity.security;

import com.smartcity.security.dto.*;
import org.springframework.security.core.Authentication;

public interface AuthService {
    UserSummaryResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request, String ipAddress);
    com.smartcity.security.otp.dto.OtpResponse sendLoginOtp(LoginOtpSendRequest request);
    AuthResponse loginWithOtp(LoginOtpVerifyRequest request, String ipAddress);
    UserSummaryResponse getCurrentUser(Authentication authentication);
    void logout(Authentication authentication, String ipAddress);
    UserSummaryResponse updateProfile(Authentication authentication, UpdateProfileRequest request);
    void changePassword(Authentication authentication, ChangePasswordRequest request);
}
