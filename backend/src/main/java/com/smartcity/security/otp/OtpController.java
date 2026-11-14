package com.smartcity.security.otp;

import com.smartcity.security.otp.dto.OtpResponse;
import com.smartcity.security.otp.dto.SendOtpRequest;
import com.smartcity.security.otp.dto.VerifyOtpRequest;
import com.smartcity.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth/otp")
@Tag(name = "Mobile OTP Authentication", description = "Endpoints for mobile OTP generation, SMS dispatch, and verification")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/send")
    @Operation(summary = "Send OTP to mobile number", description = "Generates and dispatches a 6-digit verification OTP with rate limits and cooldown.")
    public ResponseEntity<OtpResponse> sendOtp(
            @Valid @RequestBody SendOtpRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserId(authentication);
        OtpResponse response = otpService.sendOtp(request, currentUserId);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend")
    @Operation(summary = "Resend OTP", description = "Requests another OTP respecting the 60-second cooldown period.")
    public ResponseEntity<OtpResponse> resendOtp(
            @Valid @RequestBody SendOtpRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserId(authentication);
        OtpResponse response = otpService.sendOtp(request, currentUserId);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify OTP code", description = "Verifies the 6-digit OTP code against the salted hash, checking expiry and attempt limits.")
    public ResponseEntity<OtpResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserId(authentication);
        OtpResponse response = otpService.verifyOtp(request, currentUserId);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }
}
