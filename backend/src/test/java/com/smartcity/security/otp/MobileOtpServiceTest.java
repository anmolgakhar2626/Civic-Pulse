package com.smartcity.security.otp;

import com.smartcity.security.otp.dto.OtpResponse;
import com.smartcity.security.otp.dto.SendOtpRequest;
import com.smartcity.security.otp.dto.VerifyOtpRequest;
import com.smartcity.user.User;
import com.smartcity.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MobileOtpServiceTest {

    @Mock
    private MobileOtpRepository otpRepository;

    @Mock
    private UserRepository userRepository;

    private MockDevSmsGatewayProvider smsGatewayProvider;
    private OtpServiceImpl otpService;

    @BeforeEach
    void setUp() {
        smsGatewayProvider = new MockDevSmsGatewayProvider();
        otpService = new OtpServiceImpl(otpRepository, smsGatewayProvider, userRepository);
    }

    @Test
    @DisplayName("Send OTP: Generates secure OTP, saves hashed record, and dispatches via dev SMS gateway")
    void sendOtp_success() {
        SendOtpRequest request = new SendOtpRequest("+919876543210", "REGISTRATION");
        when(otpRepository.countRecentRequests(anyString(), any(Instant.class))).thenReturn(1);
        when(otpRepository.findLatestByMobile(anyString(), anyString())).thenReturn(Optional.empty());

        OtpResponse response = otpService.sendOtp(request, null);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMobileNumber()).isEqualTo("+919876543210");
        assertThat(response.getCooldownSeconds()).isEqualTo(60);
        assertThat(response.getExpiresInSeconds()).isEqualTo(300);
        String deliveredOtp = smsGatewayProvider.getLastSentOtp("+919876543210");
        assertThat(deliveredOtp).isNotNull();

        // Verify plain OTP is NOT saved into DB, only salted hash
        verify(otpRepository).save(argThat(otp -> {
            assertThat(otp.getOtpHash()).isNotEmpty();
            assertThat(otp.getOtpHash()).isNotEqualTo(deliveredOtp);
            return true;
        }));
    }

    @Test
    @DisplayName("Send OTP: Enforces 60-second resend cooldown")
    void sendOtp_resendCooldownEnforced() {
        SendOtpRequest request = new SendOtpRequest("+919876543210", "REGISTRATION");
        when(otpRepository.countRecentRequests(anyString(), any(Instant.class))).thenReturn(1);

        MobileOtp existingOtp = new MobileOtp("+919876543210", null, "HASHED_123456", "REGISTRATION", 3,
                Instant.now().plusSeconds(45), Instant.now().plusSeconds(280));
        when(otpRepository.findLatestByMobile(anyString(), anyString())).thenReturn(Optional.of(existingOtp));

        OtpResponse response = otpService.sendOtp(request, null);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Please wait");
        verify(otpRepository, never()).save(any(MobileOtp.class));
    }

    @Test
    @DisplayName("Send OTP: Enforces hourly rate limit (max 5 requests/hour)")
    void sendOtp_rateLimitExceeded() {
        SendOtpRequest request = new SendOtpRequest("+919876543210", "REGISTRATION");
        when(otpRepository.countRecentRequests(anyString(), any(Instant.class))).thenReturn(5);

        OtpResponse response = otpService.sendOtp(request, null);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("OTP request limit reached");
        verify(otpRepository, never()).save(any(MobileOtp.class));
    }

    @Test
    @DisplayName("Verify OTP: Valid OTP verifies successfully and updates user status")
    void verifyOtp_success() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setMobileNumber("+919876543210");
        user.setIsMobileVerified(false);

        // Create an OTP through sendOtp so hash is matched correctly
        when(otpRepository.countRecentRequests(anyString(), any(Instant.class))).thenReturn(0);
        when(otpRepository.findLatestByMobile(anyString(), anyString())).thenReturn(Optional.empty());

        SendOtpRequest sendReq = new SendOtpRequest("+919876543210", "REGISTRATION");
        OtpResponse sentResp = otpService.sendOtp(sendReq, userId);
        String generatedOtp = smsGatewayProvider.getLastSentOtp("+919876543210");

        // Capture what was saved
        verify(otpRepository).save(argThat(savedOtp -> {
            when(otpRepository.findLatestActiveOtp("+919876543210", "REGISTRATION")).thenReturn(Optional.of(savedOtp));
            return true;
        }));

        VerifyOtpRequest request = new VerifyOtpRequest("+919876543210", generatedOtp, "REGISTRATION");
        OtpResponse verifyResp = otpService.verifyOtp(request, userId);

        assertThat(verifyResp.isSuccess()).isTrue();
        assertThat(verifyResp.getMessage()).contains("successfully verified");
        verify(otpRepository).markUsed(any());
        verify(userRepository).updateMobileVerification(eq(userId), eq("+919876543210"), eq(true));
    }

    @Test
    @DisplayName("Verify OTP: Invalid OTP increments attempt count")
    void verifyOtp_invalidCode_incrementsAttempt() {
        UUID otpId = UUID.randomUUID();
        MobileOtp activeOtp = new MobileOtp("+919876543210", null, "HASHED_654321", "REGISTRATION", 3,
                Instant.now().minusSeconds(10), Instant.now().plusSeconds(200));
        activeOtp.setId(otpId);
        activeOtp.setAttemptCount(1);

        when(otpRepository.findLatestActiveOtp("+919876543210", "REGISTRATION")).thenReturn(Optional.of(activeOtp));

        VerifyOtpRequest request = new VerifyOtpRequest("+919876543210", "000000", "REGISTRATION");
        OtpResponse verifyResp = otpService.verifyOtp(request, null);

        assertThat(verifyResp.isSuccess()).isFalse();
        assertThat(verifyResp.getMessage()).contains("Invalid verification code");
        verify(otpRepository).incrementAttempts(otpId);
    }

    @Test
    @DisplayName("Validate and consume OTP during registration: atomically consumes active OTP")
    void validateAndConsumeOtp_success() {
        when(otpRepository.countRecentRequests(anyString(), any(Instant.class))).thenReturn(0);
        when(otpRepository.findLatestByMobile(anyString(), anyString())).thenReturn(Optional.empty());

        SendOtpRequest sendReq = new SendOtpRequest("+919876543210", "REGISTRATION");
        OtpResponse sentResp = otpService.sendOtp(sendReq, null);
        String generatedOtp = smsGatewayProvider.getLastSentOtp("+919876543210");

        verify(otpRepository).save(argThat(savedOtp -> {
            when(otpRepository.findLatestActiveOtp("+919876543210", "REGISTRATION")).thenReturn(Optional.of(savedOtp));
            return true;
        }));

        boolean consumed = otpService.validateAndConsumeOtp("+919876543210", generatedOtp, "REGISTRATION");
        assertThat(consumed).isTrue();
        verify(otpRepository).markUsed(any());
    }
}
