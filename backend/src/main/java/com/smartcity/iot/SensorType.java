package com.smartcity.iot;

public enum SensorType {
    AIR_QUALITY,
    PM25,
    PM10,
    TEMPERATURE,
    HUMIDITY,
    WATER_LEVEL,
    TRAFFIC,
    WASTE_LEVEL,
    FLOOD,
    SMOKE,
    GAS,
    ELECTRICAL;

    public static SensorType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return AIR_QUALITY;
        }
        String clean = value.trim().toUpperCase().replace("-", "_").replace(".", "");
        for (SensorType type : values()) {
            if (type.name().equalsIgnoreCase(clean) || type.name().replace("_", "").equalsIgnoreCase(clean)) {
                return type;
            }
        }
        // Fallback matching common variants
        if (clean.contains("PM25") || clean.contains("PM2_5")) return PM25;
        if (clean.contains("PM10")) return PM10;
        if (clean.contains("AIR")) return AIR_QUALITY;
        if (clean.contains("WATER") || clean.contains("LEVEL")) return WATER_LEVEL;
        if (clean.contains("WASTE") || clean.contains("BIN")) return WASTE_LEVEL;
        if (clean.contains("FLOOD")) return FLOOD;
        if (clean.contains("GAS")) return GAS;
        if (clean.contains("SMOKE")) return SMOKE;
        if (clean.contains("ELECTRIC") || clean.contains("POWER")) return ELECTRICAL;
        if (clean.contains("TEMP")) return TEMPERATURE;
        if (clean.contains("HUMID")) return HUMIDITY;
        if (clean.contains("TRAFFIC")) return TRAFFIC;

        return AIR_QUALITY;
    }
}
