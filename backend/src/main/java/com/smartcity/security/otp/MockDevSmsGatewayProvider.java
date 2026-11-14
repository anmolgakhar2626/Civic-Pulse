package com.smartcity.security.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Development-only and test mock SMS Gateway Provider.
 * Dispatches simulated SMS messages to logger and stores the latest OTP
 * in a thread-safe registry so integration tests can assert delivery.
 * In local mock/demo profile, writes generated OTPs to backend/demo-otp.log.
 */
@Component("mockDevSmsGatewayProvider")
public class MockDevSmsGatewayProvider implements SmsGatewayProvider {

    private static final Logger log = LoggerFactory.getLogger(MockDevSmsGatewayProvider.class);

    private final Map<String, String> lastSentMessages = new ConcurrentHashMap<>();
    private final Map<String, String> lastSentOtps = new ConcurrentHashMap<>();

    @Override
    public String getProviderName() {
        return "MOCK_DEV_SMS_GATEWAY";
    }

    @Override
    public SmsSendResult sendSms(String mobileNumber, String message) {
        return sendSms(mobileNumber, message, "REGISTRATION");
    }

    @Override
    public SmsSendResult sendSms(String mobileNumber, String message, String purpose) {
        String msgId = "SMS-" + UUID.randomUUID().toString().substring(0, 8);
        lastSentMessages.put(mobileNumber, message);

        String effectivePurpose = (purpose != null && !purpose.isBlank()) ? purpose.toUpperCase() : "REGISTRATION";

        // Extract 6-digit OTP if present
        String rawOtp = "";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\b(\\d{6})\\b").matcher(message);
        if (matcher.find()) {
            rawOtp = matcher.group(1);
            lastSentOtps.put(mobileNumber, rawOtp);
            writeDemoOtpLog(mobileNumber, rawOtp, effectivePurpose);
        }

        // Prominently print generated OTP in local development terminal for college demo
        System.out.println("\n" +
                "================================================================================\n" +
                "                     [CIVICPULSE LOCAL DEMO SMS GATEWAY]                       \n" +
                "--------------------------------------------------------------------------------\n" +
                " Recipient Mobile : " + mobileNumber + "\n" +
                " Generated OTP    : >>> " + rawOtp + " <<<\n" +
                " Purpose          : " + effectivePurpose + "\n" +
                " Security Policy  : Valid for 5 minutes | Single-Use | Salted SHA-256 in DB     \n" +
                " Mentor Notice    : Displayed strictly in local terminal & backend/demo-otp.log \n" +
                "================================================================================\n");

        log.info("[LOCAL DEMO OTP] Generated OTP [{}] for mobile {} (Purpose: {})", rawOtp, mobileNumber, effectivePurpose);
        return SmsSendResult.success(msgId, getProviderName(), "Demo OTP generated successfully in local environment");
    }

    private synchronized void writeDemoOtpLog(String mobileNumber, String otp, String purpose) {
        if (otp == null || otp.isBlank()) {
            return;
        }
        try {
            Path targetPath = resolveDemoOtpLogPath();
            if (targetPath.getParent() != null && !Files.exists(targetPath.getParent())) {
                Files.createDirectories(targetPath.getParent());
            }

            String timestamp = OffsetDateTime.now().toString();
            String logLine = String.format("[%s] PURPOSE: %s | MOBILE: %s | OTP: %s%n",
                    timestamp, purpose, mobileNumber, otp);

            Files.writeString(
                    targetPath,
                    logLine,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
            log.debug("Wrote demo OTP to {}", targetPath.toAbsolutePath());
        } catch (Exception e) {
            log.warn("Could not write demo-otp.log: {}", e.getMessage());
        }
    }

    private Path resolveDemoOtpLogPath() {
        Path cwd = Paths.get("").toAbsolutePath();
        if (cwd.endsWith("backend")) {
            return cwd.resolve("demo-otp.log");
        } else if (Files.isDirectory(cwd.resolve("backend"))) {
            return cwd.resolve("backend").resolve("demo-otp.log");
        } else {
            return cwd.resolve("demo-otp.log");
        }
    }

    @Override
    public boolean isSandbox() {
        return true;
    }

    public String getLastSentMessage(String mobileNumber) {
        return lastSentMessages.get(mobileNumber);
    }

    public String getLastSentOtp(String mobileNumber) {
        return lastSentOtps.get(mobileNumber);
    }

    public void clear() {
        lastSentMessages.clear();
        lastSentOtps.clear();
    }
}
