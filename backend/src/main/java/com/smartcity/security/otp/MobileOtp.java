package com.smartcity.security.otp;

import java.time.Instant;
import java.util.UUID;

public class MobileOtp {

    private UUID id;
    private String mobileNumber;
    private UUID userId;
    private String otpHash;
    private String purpose; // REGISTRATION, VERIFICATION, LOGIN
    private int attemptCount;
    private int maxAttempts = 3;
    private Instant resendAfter;
    private Instant expiresAt;
    private boolean isUsed;
    private Instant createdAt;

    public MobileOtp() {}

    public MobileOtp(String mobileNumber, UUID userId, String otpHash, String purpose,
                     int maxAttempts, Instant resendAfter, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.mobileNumber = mobileNumber;
        this.userId = userId;
        this.otpHash = otpHash;
        this.purpose = purpose != null ? purpose : "REGISTRATION";
        this.attemptCount = 0;
        this.maxAttempts = maxAttempts > 0 ? maxAttempts : 3;
        this.resendAfter = resendAfter;
        this.expiresAt = expiresAt;
        this.isUsed = false;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public Instant getResendAfter() { return resendAfter; }
    public void setResendAfter(Instant resendAfter) { this.resendAfter = resendAfter; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isUsed() { return isUsed; }
    public void setUsed(boolean used) { isUsed = used; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isLocked() {
        return attemptCount >= maxAttempts;
    }
}
