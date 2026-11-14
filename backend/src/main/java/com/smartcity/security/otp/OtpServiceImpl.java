package com.smartcity.security.otp;

import com.smartcity.security.otp.dto.OtpResponse;
import com.smartcity.security.otp.dto.SendOtpRequest;
import com.smartcity.security.otp.dto.VerifyOtpRequest;
import com.smartcity.user.User;
import com.smartcity.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class OtpServiceImpl implements OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);

    private static final int COOLDOWN_SECONDS = 60;
    private static final int EXPIRY_SECONDS = 300; // 5 minutes
    private static final int MAX_HOURLY_REQUESTS = 5;
    private static final int MAX_ATTEMPTS = 3;
    private static final String HASH_SALT = "CivicPulse_SmartCity_OTP_Salt_v1";

    private final MobileOtpRepository mobileOtpRepository;
    private final SmsGatewayProvider smsGatewayProvider;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpServiceImpl(
            MobileOtpRepository mobileOtpRepository,
            SmsGatewayProvider smsGatewayProvider,
            UserRepository userRepository
    ) {
        this.mobileOtpRepository = mobileOtpRepository;
        this.smsGatewayProvider = smsGatewayProvider;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OtpResponse sendOtp(SendOtpRequest request, UUID currentUserId) {
        String mobile = normalizeMobile(request.getMobileNumber());
        String purpose = request.getPurpose() != null ? request.getPurpose() : "REGISTRATION";

        Instant now = Instant.now();

        // 1. Rate limiting check (max 5 per hour)
        int hourlyCount = mobileOtpRepository.countRecentRequests(mobile, now.minus(Duration.ofHours(1)));
        if (hourlyCount >= MAX_HOURLY_REQUESTS) {
            log.warn("Rate limit exceeded for mobile: {} (requests: {})", mobile, hourlyCount);
            return OtpResponse.failure("OTP request limit reached. Maximum " + MAX_HOURLY_REQUESTS + " requests per hour.", 0);
        }

        // 2. Cooldown check (60s cooldown from latest request)
        Optional<MobileOtp> latestOtp = mobileOtpRepository.findLatestByMobile(mobile, purpose);
        if (latestOtp.isPresent()) {
            MobileOtp prev = latestOtp.get();
            if (now.isBefore(prev.getResendAfter())) {
                long remainingCooldown = Duration.between(now, prev.getResendAfter()).getSeconds();
                return OtpResponse.failure("Please wait " + remainingCooldown + " seconds before requesting a new OTP.", null);
            }
        }

        // 3. Generate secure 6-digit numeric OTP
        int rawOtpNum = 100000 + secureRandom.nextInt(900000);
        String rawOtp = String.valueOf(rawOtpNum);
        String otpHash = hashOtp(rawOtp, mobile);

        // 4. Save MobileOtp record
        Instant expiresAt = now.plus(Duration.ofSeconds(EXPIRY_SECONDS));
        Instant resendAfter = now.plus(Duration.ofSeconds(COOLDOWN_SECONDS));

        UUID targetUserId = currentUserId;
        if (targetUserId == null) {
            Optional<User> existingUser = userRepository.findByMobileNumber(mobile);
            if (existingUser.isPresent()) {
                targetUserId = existingUser.get().getId();
            }
        }

        // 5. Dispatch via SMS Provider
        String smsMessage = "Your CivicPulse verification code is: " + rawOtp + ". Valid for 5 minutes. Do not share this OTP.";
        SmsSendResult sendResult = smsGatewayProvider.sendSms(mobile, smsMessage, purpose);

        if (!sendResult.isSuccess()) {
            log.error("Failed to dispatch SMS to {}: {}", maskPhone(mobile), sendResult.getMessage());
            return OtpResponse.failure("SMS delivery failed: " + sendResult.getMessage(), null);
        }

        // 6. Save MobileOtp record only AFTER SMS is confirmed dispatched
        MobileOtp newOtp = new MobileOtp(mobile, targetUserId, otpHash, purpose, MAX_ATTEMPTS, resendAfter, expiresAt);
        mobileOtpRepository.save(newOtp);

        log.info("Dispatched OTP via {} to {} for purpose {}. Expires in {}s",
                sendResult.getProviderName(), maskPhone(mobile), purpose, EXPIRY_SECONDS);

        String successMessage = smsGatewayProvider.isSandbox()
                ? "Demo OTP generated successfully. Check the local backend terminal."
                : "OTP sent to your registered mobile number.";

        return OtpResponse.sent(mobile, COOLDOWN_SECONDS, EXPIRY_SECONDS, successMessage);
    }

    @Override
    @Transactional
    public OtpResponse verifyOtp(VerifyOtpRequest request, UUID currentUserId) {
        String mobile = normalizeMobile(request.getMobileNumber());
        String candidateOtp = request.getOtp() != null ? request.getOtp().trim() : "";
        String purpose = request.getPurpose() != null ? request.getPurpose() : "REGISTRATION";

        Optional<MobileOtp> otpOpt = mobileOtpRepository.findLatestActiveOtp(mobile, purpose);
        if (otpOpt.isEmpty()) {
            return OtpResponse.failure("No active OTP found. Please request a new verification code.", null);
        }

        MobileOtp otp = otpOpt.get();

        // 1. Check expiration
        if (otp.isExpired()) {
            return OtpResponse.failure("Verification code has expired. Please request a new OTP.", null);
        }

        // 2. Check maximum attempts
        if (otp.isLocked()) {
            return OtpResponse.failure("Maximum verification attempts exceeded. Please request a new OTP.", 0);
        }

        // 3. Constant-time hash verification
        String expectedHash = otp.getOtpHash();
        String candidateHash = hashOtp(candidateOtp, mobile);

        if (!MessageDigest.isEqual(expectedHash.getBytes(StandardCharsets.UTF_8), candidateHash.getBytes(StandardCharsets.UTF_8))) {
            mobileOtpRepository.incrementAttempts(otp.getId());
            int remaining = otp.getMaxAttempts() - (otp.getAttemptCount() + 1);
            if (remaining <= 0) {
                return OtpResponse.failure("Invalid verification code. Maximum attempts reached. Please request a new OTP.", 0);
            }
            return OtpResponse.failure("Invalid verification code. " + remaining + " attempt(s) remaining.", remaining);
        }

        // 4. Mark OTP as used
        mobileOtpRepository.markUsed(otp.getId());

        // 5. Mark user mobile as verified if user is linked or exists
        UUID userIdToVerify = currentUserId != null ? currentUserId : otp.getUserId();
        if (userIdToVerify == null) {
            Optional<User> uOpt = userRepository.findByMobileNumber(mobile);
            if (uOpt.isPresent()) {
                userIdToVerify = uOpt.get().getId();
            }
        }

        if (userIdToVerify != null) {
            userRepository.updateMobileVerification(userIdToVerify, mobile, true);
            log.info("User {} mobile number {} successfully verified", userIdToVerify, mobile);
        }

        return OtpResponse.verified(mobile, "Mobile number successfully verified!");
    }

    @Override
    @Transactional
    public OtpResponse verifyLoginOtp(String mobileNumber, String candidateOtp) {
        String mobile = normalizeMobile(mobileNumber);
        String otpCode = candidateOtp != null ? candidateOtp.trim() : "";

        if (otpCode.length() != 6) {
            return OtpResponse.failure("OTP must be exactly 6 digits.", null);
        }

        Optional<MobileOtp> otpOpt = mobileOtpRepository.findLatestActiveOtp(mobile, "LOGIN");
        if (otpOpt.isEmpty()) {
            return OtpResponse.failure("No active OTP found. Please request a new verification code.", null);
        }

        MobileOtp otp = otpOpt.get();

        // 1. Check expiration
        if (otp.isExpired()) {
            return OtpResponse.failure("Verification code has expired. Please request a new OTP.", null);
        }

        // 2. Check maximum attempts
        if (otp.isLocked()) {
            return OtpResponse.failure("Maximum verification attempts exceeded. Please request a new OTP.", 0);
        }

        // 3. Constant-time hash verification
        String expectedHash = otp.getOtpHash();
        String candidateHash = hashOtp(otpCode, mobile);

        if (!MessageDigest.isEqual(expectedHash.getBytes(StandardCharsets.UTF_8), candidateHash.getBytes(StandardCharsets.UTF_8))) {
            mobileOtpRepository.incrementAttemptsAtomic(otp.getId());
            int remaining = otp.getMaxAttempts() - (otp.getAttemptCount() + 1);
            if (remaining <= 0) {
                return OtpResponse.failure("Invalid verification code. Maximum attempts reached. Please request a new OTP.", 0);
            }
            return OtpResponse.failure("Invalid verification code. " + remaining + " attempt(s) remaining.", remaining);
        }

        // 4. Mark OTP as used atomically (Replay attack & concurrency protection)
        boolean marked = mobileOtpRepository.markUsedAtomic(otp.getId());
        if (!marked) {
            log.warn("Concurrent duplicate attempt to consume login OTP {} for mobile {}", otp.getId(), maskPhone(mobile));
            return OtpResponse.failure("Verification code has already been used. Please request a new OTP.", null);
        }

        log.info("Successfully verified login OTP for mobile {}", maskPhone(mobile));
        return OtpResponse.verified(mobile, "OTP verified successfully!");
    }

    @Override
    @Transactional
    public boolean validateAndConsumeOtp(String mobileNumber, String candidateOtp, String purpose) {
        if (candidateOtp == null || candidateOtp.isBlank()) {
            return false;
        }
        String mobile = normalizeMobile(mobileNumber);
        Optional<MobileOtp> otpOpt = mobileOtpRepository.findLatestActiveOtp(mobile, purpose);
        if (otpOpt.isEmpty()) {
            // Check if the OTP was already verified via inline verification within valid expiry window
            Optional<MobileOtp> latestOpt = mobileOtpRepository.findLatestByMobile(mobile, purpose);
            if (latestOpt.isPresent()) {
                MobileOtp usedOtp = latestOpt.get();
                if (usedOtp.isUsed() && !usedOtp.isExpired()) {
                    String expectedHash = usedOtp.getOtpHash();
                    String candidateHash = hashOtp(candidateOtp.trim(), mobile);
                    if (MessageDigest.isEqual(expectedHash.getBytes(StandardCharsets.UTF_8), candidateHash.getBytes(StandardCharsets.UTF_8))) {
                        return true;
                    }
                }
            }
            return false;
        }

        MobileOtp otp = otpOpt.get();
        if (otp.isExpired() || otp.isLocked()) {
            return false;
        }

        String expectedHash = otp.getOtpHash();
        String candidateHash = hashOtp(candidateOtp.trim(), mobile);

        if (MessageDigest.isEqual(expectedHash.getBytes(StandardCharsets.UTF_8), candidateHash.getBytes(StandardCharsets.UTF_8))) {
            mobileOtpRepository.markUsed(otp.getId());
            return true;
        } else {
            mobileOtpRepository.incrementAttempts(otp.getId());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMobileVerified(UUID userId) {
        if (userId == null) return false;
        return userRepository.findById(userId)
                .map(u -> Boolean.TRUE.equals(u.getIsMobileVerified()))
                .orElse(false);
    }

    private String hashOtp(String rawOtp, String mobile) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = HASH_SALT + ":" + mobile + ":" + rawOtp;
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm missing", e);
        }
    }

    private String normalizeMobile(String mobile) {
        if (mobile == null) return "";
        return mobile.replaceAll("\\s+", "").trim();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "***";
        return phone.substring(0, 3) + "******" + phone.substring(phone.length() - 2);
    }
}
