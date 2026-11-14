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
import java.util.UUID;

/**
 * Generic HTTP REST SMS Gateway Provider.
 * Allows integration with custom or municipal SMS gateways (e.g. NIC, CDAC, Msg91, Textlocal).
 */
@Component
public class HttpSmsGatewayProvider implements SmsGatewayProvider {

    private static final Logger log = LoggerFactory.getLogger(HttpSmsGatewayProvider.class);

    @Value("${smartcity.sms.http.gateway-url:${SMS_GATEWAY_URL:}}")
    private String gatewayUrl;

    @Value("${smartcity.sms.http.api-key:${SMS_GATEWAY_API_KEY:}}")
    private String apiKey;

    @Value("${smartcity.sms.http.sender-id:${SMS_GATEWAY_SENDER_ID:CIVICPULSE}}")
    private String senderId;

    private final HttpClient httpClient;

    public HttpSmsGatewayProvider() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public HttpSmsGatewayProvider(HttpClient httpClient, String gatewayUrl, String apiKey, String senderId) {
        this.httpClient = httpClient;
        this.gatewayUrl = gatewayUrl;
        this.apiKey = apiKey;
        this.senderId = senderId;
    }

    @Override
    public String getProviderName() {
        return "GENERIC_HTTP_SMS";
    }

    @Override
    public SmsSendResult sendSms(String mobileNumber, String message) {
        if (gatewayUrl == null || gatewayUrl.isBlank()) {
            log.error("[HTTP SMS] Cannot send SMS: Gateway URL not configured (SMS_GATEWAY_URL).");
            return SmsSendResult.failure(getProviderName(), "SMS Gateway URL is not configured.");
        }

        try {
            String jsonPayload = String.format(
                    "{\"to\":\"%s\",\"message\":\"%s\",\"senderId\":\"%s\"}",
                    mobileNumber, message.replace("\"", "\\\""), senderId
            );

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(gatewayUrl.trim()))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload));

            if (apiKey != null && !apiKey.isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + apiKey.trim());
                reqBuilder.header("X-API-Key", apiKey.trim());
            }

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                String msgId = "HTTP-" + UUID.randomUUID().toString().substring(0, 8);
                log.info("[HTTP SMS] Successfully dispatched SMS via {} to {}", gatewayUrl, maskPhone(mobileNumber));
                return SmsSendResult.success(msgId, getProviderName(), "SMS dispatched successfully via REST gateway");
            } else {
                log.error("[HTTP SMS] Delivery failed to {}: HTTP {} - {}", maskPhone(mobileNumber), statusCode, response.body());
                return SmsSendResult.failure(getProviderName(), "Gateway returned HTTP " + statusCode + ": " + response.body());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[HTTP SMS] Interrupted sending SMS to {}", maskPhone(mobileNumber));
            return SmsSendResult.failure(getProviderName(), "SMS dispatch interrupted: " + e.getMessage());
        } catch (Exception e) {
            log.error("[HTTP SMS] Exception dispatching SMS to {}: {}", maskPhone(mobileNumber), e.getMessage());
            return SmsSendResult.failure(getProviderName(), "Gateway error: " + e.getMessage());
        }
    }

    @Override
    public boolean isSandbox() {
        return false;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "***";
        return phone.substring(0, 3) + "******" + phone.substring(phone.length() - 2);
    }
}

