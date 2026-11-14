package com.smartcity.security.otp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwilioSmsGatewayProviderTest {

    @Mock
    private HttpClient httpClient;

    @Test
    @DisplayName("Twilio: Returns failure when credentials are missing")
    void sendSms_unconfiguredCredentials_returnsFailure() {
        TwilioSmsGatewayProvider provider = new TwilioSmsGatewayProvider(httpClient, "", "", "");
        SmsSendResult result = provider.sendSms("+919876543210", "Your OTP is 123456");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("credentials are not configured");
    }

    @Test
    @DisplayName("Twilio: Successfully dispatches SMS and extracts SID on HTTP 201")
    @SuppressWarnings("unchecked")
    void sendSms_success() throws Exception {
        TwilioSmsGatewayProvider provider = new TwilioSmsGatewayProvider(
                httpClient,
                "ACtestaccountsid000000000000000000",
                "testauthtoken000000000000000000",
                "+15551234567"
        );

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(201);
        when(mockResponse.body()).thenReturn("{\"sid\": \"SM8a4c1029384756\", \"status\": \"queued\"}");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        SmsSendResult result = provider.sendSms("+919876543210", "Your CivicPulse verification code is: 654321");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessageId()).isEqualTo("SM8a4c1029384756");
        assertThat(result.getProviderName()).isEqualTo("TWILIO");
    }

    @Test
    @DisplayName("Twilio: Parses error message when Twilio returns HTTP 400")
    @SuppressWarnings("unchecked")
    void sendSms_apiError_returnsFailureWithReason() throws Exception {
        TwilioSmsGatewayProvider provider = new TwilioSmsGatewayProvider(
                httpClient,
                "ACtestaccountsid000000000000000000",
                "testauthtoken000000000000000000",
                "+15551234567"
        );

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(400);
        when(mockResponse.body()).thenReturn("{\"code\": 21211, \"message\": \"The 'To' number is not a valid phone number.\"}");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        SmsSendResult result = provider.sendSms("invalid_phone", "Your CivicPulse verification code is: 654321");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("The 'To' number is not a valid phone number");
    }
}

