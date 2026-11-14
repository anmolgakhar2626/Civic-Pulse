package com.smartcity.security.otp;

/**
 * Configurable SMS Gateway provider interface for mobile verification and OTP dispatch.
 */
public interface SmsGatewayProvider {

    /**
     * Provider identifier (e.g., MOCK_DEV, GOV_NIC_SMS, TWILIO).
     */
    String getProviderName();

    /**
     * Dispatches an SMS message containing the OTP to the specified mobile number.
     *
     * @param mobileNumber Recipient mobile number in E.164 or national format
     * @param message Complete SMS body text
     * @return SmsSendResult indicating delivery status and message reference
     */
    SmsSendResult sendSms(String mobileNumber, String message);

    /**
     * Dispatches an SMS message containing the OTP with an explicit purpose (LOGIN or REGISTRATION).
     * Defaults to sendSms(mobileNumber, message) for backward compatibility.
     *
     * @param mobileNumber Recipient mobile number in E.164 or national format
     * @param message Complete SMS body text
     * @param purpose OTP purpose (e.g., LOGIN, REGISTRATION)
     * @return SmsSendResult indicating delivery status and message reference
     */
    default SmsSendResult sendSms(String mobileNumber, String message, String purpose) {
        return sendSms(mobileNumber, message);
    }

    /**
     * Whether this provider is a development/mock provider.
     */
    boolean isSandbox();
}
