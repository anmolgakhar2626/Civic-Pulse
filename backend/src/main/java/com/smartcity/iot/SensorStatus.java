package com.smartcity.iot;

public enum SensorStatus {
    ONLINE,
    OFFLINE,
    FAULT,
    UNKNOWN;

    public static SensorStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return UNKNOWN;
        }
        try {
            return SensorStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
