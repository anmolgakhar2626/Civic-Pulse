package com.smartcity.security.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Production-ready SMS Gateway Provider using Twilio REST API.
 * Dispatches real SMS messages to recipient mobile numbers via Twilio Messages API.
 */
@Component
public class TwilioSmsGatewayProvider implements SmsGatewayProvider {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsGatewayProvider.class);
    private static final Pattern SID_PATTERN = Pattern.compile("\"sid\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ERROR_MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");

    @Value("${smartcity.sms.twilio.account-sid:${TWILIO_ACCOUNT_SID:}}")
    private String accountSid;

    @Value("${smartcity.sms.twilio.auth-token:${TWILIO_AUTH_TOKEN:}}")
    private String authToken;

    @Value("${smartcity.sms.twilio.from-number:${TWILIO_FROM_NUMBER:}}")
    private String fromNumber;

    private final HttpClient httpClient;

    public TwilioSmsGatewayProvider() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public TwilioSmsGatewayProvider(HttpClient httpClient, String accountSid, String authToken, String fromNumber) {
        this.httpClient = httpClient;
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
    }

    @Override
    public String getProviderName() {
        return "TWILIO";
    }

    @Override
    public SmsSendResult sendSms(String mobileNumber, String message) {
        if (accountSid == null || accountSid.isBlank() ||
            authToken == null || authToken.isBlank() ||
            fromNumber == null || fromNumber.isBlank()) {
            log.error("[TWILIO] Cannot send SMS: Twilio credentials not configured (TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_FROM_NUMBER).");
            return SmsSendResult.failure(getProviderName(), "Twilio SMS Gateway credentials are not configured.");
        }

        try {
            // E.164 normalization for Twilio (+ country code)
            String targetPhone = normalizeToE164(mobileNumber);

            String url = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", accountSid.trim());

            String formData = "To=" + URLEncoder.encode(targetPhone, StandardCharsets.UTF_8)
                    + "&From=" + URLEncoder.encode(fromNumber.trim(), StandardCharsets.UTF_8)
                    + "&Body=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

            String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                    (accountSid.trim() + ":" + authToken.trim()).getBytes(StandardCharsets.UTF_8)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            String responseBody = response.body();

            if (statusCode >= 200 && statusCode < 300) {
                String sid = extractSid(responseBody);
                log.info("[TWILIO] Successfully dispatched SMS to {} (SID: {})", maskPhone(targetPhone), sid);
                return SmsSendResult.success(sid != null ? sid : "TWILIO-MSG", getProviderName(), "SMS successfully delivered via Twilio");
            } else {
                String errorMessage = extractErrorMessage(responseBody);
                if (errorMessage == null || errorMessage.isBlank()) {
                    errorMessage = "Twilio API returned HTTP " + statusCode;
                }
                log.error("[TWILIO] Delivery failed to {}: HTTP {} - {}", maskPhone(targetPhone), statusCode, errorMessage);
                return SmsSendResult.failure(getProviderName(), errorMessage);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[TWILIO] Request interrupted while sending SMS to {}", maskPhone(mobileNumber));
            return SmsSendResult.failure(getProviderName(), "SMS dispatch request interrupted: " + e.getMessage());
        } catch (Exception e) {
            log.error("[TWILIO] Exception sending SMS to {}: {}", maskPhone(mobileNumber), e.getMessage());
            return SmsSendResult.failure(getProviderName(), "Failed to reach Twilio SMS gateway: " + e.getMessage());
        }
    }

    @Override
    public boolean isSandbox() {
        return false;
    }

    private String extractSid(String body) {
        if (body == null) return null;
        Matcher matcher = SID_PATTERN.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractErrorMessage(String body) {
        if (body == null) return null;
        Matcher matcher = ERROR_MESSAGE_PATTERN.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static String normalizeToE164(String input) {
        if (input == null) return "";
        String clean = input.replaceAll("[\\s\\-\\(\\)]", "");
        if (clean.startsWith("+")) {
            return clean;
        }
        if (clean.startsWith("0") && clean.length() == 11) {
            clean = clean.substring(1);
        }
        if (clean.startsWith("91") && clean.length() == 12) {
            return "+" + clean;
        }
        if (clean.length() == 10) {
            return "+91" + clean;
        }
        return "+" + clean;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "***";
        return phone.substring(0, 3) + "******" + phone.substring(phone.length() - 2);
    }
}

