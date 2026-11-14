package com.smartcity.security.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Production-ready SMS Gateway Provider for Indian mobile numbers using Fast2SMS API.
 */
@Component
public class Fast2SmsGatewayProvider implements SmsGatewayProvider {

    private static final Logger log = LoggerFactory.getLogger(Fast2SmsGatewayProvider.class);
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("\"request_id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\\[?\"([^\"]+)\"");

    @Value("${smartcity.sms.fast2sms.api-key:${FAST2SMS_API_KEY:}}")
    private String apiKey;

    private final HttpClient httpClient;

    public Fast2SmsGatewayProvider() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public Fast2SmsGatewayProvider(HttpClient httpClient, String apiKey) {
        this.httpClient = httpClient;
        this.apiKey = apiKey;
    }

    @Override
    public String getProviderName() {
        return "FAST2SMS";
    }

    @Override
    public SmsSendResult sendSms(String mobileNumber, String message) {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("[FAST2SMS] Cannot send SMS: API key not configured (FAST2SMS_API_KEY).");
            return SmsSendResult.failure(getProviderName(), "Fast2SMS API key is not configured.");
        }

        try {
            // Strip +91 for Fast2SMS national number
            String digits = mobileNumber.replaceAll("[^0-9]", "");
            if (digits.startsWith("91") && digits.length() == 12) {
                digits = digits.substring(2);
            }

            // Extract 6-digit OTP from message for quick route
            String otpCode = "";
            Matcher otpMatcher = Pattern.compile("\\b(\\d{6})\\b").matcher(message);
            if (otpMatcher.find()) {
                otpCode = otpMatcher.group(1);
            }

            String jsonPayload;
            if (!otpCode.isEmpty()) {
                jsonPayload = String.format(
                        "{\"route\":\"otp\",\"variables_values\":\"%s\",\"numbers\":\"%s\"}",
                        otpCode, digits
                );
            } else {
                jsonPayload = String.format(
                        "{\"route\":\"q\",\"message\":\"%s\",\"language\":\"english\",\"flash\":0,\"numbers\":\"%s\"}",
                        message.replace("\"", "\\\""), digits
                );
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.fast2sms.com/dev/bulkV2"))
                    .timeout(Duration.ofSeconds(10))
                    .header("authorization", apiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            String responseBody = response.body();

            if (statusCode == 200 && responseBody.contains("\"return\":true")) {
                String reqId = extractRequestId(responseBody);
                log.info("[FAST2SMS] Successfully dispatched SMS to {} (ReqId: {})", maskPhone(mobileNumber), reqId);
                return SmsSendResult.success(reqId != null ? reqId : "FAST2SMS-OK", getProviderName(), "SMS successfully dispatched via Fast2SMS");
            } else {
                String errorMsg = extractMessage(responseBody);
                log.error("[FAST2SMS] Delivery failed to {}: HTTP {} - {}", maskPhone(mobileNumber), statusCode, errorMsg);
                return SmsSendResult.failure(getProviderName(), errorMsg != null ? errorMsg : "Fast2SMS returned error code " + statusCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[FAST2SMS] Dispatch interrupted for {}", maskPhone(mobileNumber));
            return SmsSendResult.failure(getProviderName(), "SMS dispatch interrupted: " + e.getMessage());
        } catch (Exception e) {
            log.error("[FAST2SMS] Error dispatching SMS to {}: {}", maskPhone(mobileNumber), e.getMessage());
            return SmsSendResult.failure(getProviderName(), "Fast2SMS error: " + e.getMessage());
        }
    }

    @Override
    public boolean isSandbox() {
        return false;
    }

    private String extractRequestId(String body) {
        if (body == null) return null;
        Matcher m = REQUEST_ID_PATTERN.matcher(body);
        return m.find() ? m.group(1) : null;
    }

    private String extractMessage(String body) {
        if (body == null) return null;
        Matcher m = MESSAGE_PATTERN.matcher(body);
        return m.find() ? m.group(1) : body;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "***";
        return phone.substring(0, 3) + "******" + phone.substring(phone.length() - 2);
    }
}

