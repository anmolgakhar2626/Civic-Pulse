package com.smartcity.security.otp;

import java.time.Instant;

public class SmsSendResult {
    private final boolean success;
    private final String messageId;
    private final String providerName;
    private final String message;
    private final Instant timestamp;

    public SmsSendResult(boolean success, String messageId, String providerName, String message) {
        this.success = success;
        this.messageId = messageId;
        this.providerName = providerName;
        this.message = message;
        this.timestamp = Instant.now();
    }

    public static SmsSendResult success(String messageId, String providerName, String message) {
        return new SmsSendResult(true, messageId, providerName, message);
    }

    public static SmsSendResult failure(String providerName, String errorMessage) {
        return new SmsSendResult(false, null, providerName, errorMessage);
    }

    public boolean isSuccess() { return success; }
    public String getMessageId() { return messageId; }
    public String getProviderName() { return providerName; }
    public String getMessage() { return message; }
    public Instant getTimestamp() { return timestamp; }
}
