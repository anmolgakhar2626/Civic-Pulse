package com.smartcity.security.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * Configuration that selects and binds the active SmsGatewayProvider.
 * Enforces production security: mock provider cannot be accidentally used in production.
 */
@Configuration
public class SmsGatewayConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SmsGatewayConfiguration.class);

    @Value("${smartcity.sms.provider:${SMARTCITY_SMS_PROVIDER:mock}}")
    private String configuredProvider;

    @Value("${smartcity.sms.twilio.account-sid:${TWILIO_ACCOUNT_SID:}}")
    private String twilioAccountSid;

    @Value("${smartcity.sms.twilio.auth-token:${TWILIO_AUTH_TOKEN:}}")
    private String twilioAuthToken;

    @Value("${smartcity.sms.twilio.from-number:${TWILIO_FROM_NUMBER:}}")
    private String twilioFromNumber;

    @Value("${smartcity.sms.fast2sms.api-key:${FAST2SMS_API_KEY:}}")
    private String fast2SmsApiKey;

    @Value("${smartcity.sms.http.gateway-url:${SMS_GATEWAY_URL:}}")
    private String httpGatewayUrl;

    @Bean
    @Primary
    public SmsGatewayProvider activeSmsGatewayProvider(
            TwilioSmsGatewayProvider twilioProvider,
            Fast2SmsGatewayProvider fast2SmsProvider,
            HttpSmsGatewayProvider httpProvider,
            MockDevSmsGatewayProvider mockProvider,
            Environment environment) {

        boolean isProd = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p));

        String providerChoice;
        if (configuredProvider == null || configuredProvider.isBlank()) {
            if (twilioAccountSid != null && !twilioAccountSid.isBlank()) {
                providerChoice = "twilio";
            } else if (fast2SmsApiKey != null && !fast2SmsApiKey.isBlank()) {
                providerChoice = "fast2sms";
            } else if (httpGatewayUrl != null && !httpGatewayUrl.isBlank()) {
                providerChoice = "http";
            } else {
                providerChoice = "mock";
            }
        } else {
            providerChoice = configuredProvider.trim().toLowerCase();
        }

        if (isProd) {
            if ("mock".equals(providerChoice)) {
                throw new IllegalStateException(
                        "CRITICAL SECURITY CONFIGURATION ERROR: Production configuration cannot use MockDevSmsGatewayProvider. " +
                        "Real Twilio SMS gateway credentials must be configured via environment variables."
                );
            }
            if ("twilio".equals(providerChoice)) {
                if (twilioAccountSid == null || twilioAccountSid.isBlank() ||
                    twilioAuthToken == null || twilioAuthToken.isBlank() ||
                    twilioFromNumber == null || twilioFromNumber.isBlank()) {
                    throw new IllegalStateException(
                            "CRITICAL SECURITY CONFIGURATION ERROR: Production configuration requires complete Twilio credentials. " +
                            "Please set TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, and TWILIO_FROM_NUMBER in your environment."
                    );
                }
            }
        }

        switch (providerChoice) {
            case "twilio":
                if (twilioAccountSid == null || twilioAccountSid.isBlank() ||
                    twilioAuthToken == null || twilioAuthToken.isBlank() ||
                    twilioFromNumber == null || twilioFromNumber.isBlank()) {
                    if (!isProd) {
                        log.warn("[SMS GATEWAY CONFIG] Twilio configured but credentials (TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_FROM_NUMBER) are incomplete. Falling back to MOCK_DEV_SMS_GATEWAY in development.");
                        return mockProvider;
                    }
                }
                log.info("[SMS GATEWAY CONFIG] Active Provider: TWILIO (Real SMS Delivery)");
                return twilioProvider;
            case "fast2sms":
                log.info("[SMS GATEWAY CONFIG] Active Provider: FAST2SMS (Real SMS Delivery)");
                return fast2SmsProvider;
            case "http":
                log.info("[SMS GATEWAY CONFIG] Active Provider: GENERIC_HTTP_SMS (Real SMS Delivery)");
                return httpProvider;
            case "mock":
            default:
                log.warn("[SMS GATEWAY CONFIG] Active Provider: MOCK_DEV_SMS_GATEWAY (Development Only)");
                return mockProvider;
        }
    }
}

