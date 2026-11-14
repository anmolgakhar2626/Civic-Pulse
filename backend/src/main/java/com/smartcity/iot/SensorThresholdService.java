package com.smartcity.iot;

import com.smartcity.priority.PriorityTier;
import com.smartcity.priority.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SensorThresholdService {

    private static final Logger log = LoggerFactory.getLogger(SensorThresholdService.class);

    private final IoTThresholdProperties properties;

    public SensorThresholdService(IoTThresholdProperties properties) {
        this.properties = properties;
    }

    public ThresholdEvaluationResult evaluate(SensorReading reading) {
        if (reading == null || reading.getValue() == null) {
            return ThresholdEvaluationResult.noBreach();
        }

        SensorType sensorType = SensorType.fromString(reading.getSensorType());
        double value = reading.getValue();
        String sensorId = reading.getSensorId() != null ? reading.getSensorId() : "UNKNOWN_SENSOR";
        String locationStr = reading.getLocation() != null ? reading.getLocation() : "City Zone";

        Map<String, Object> context = new HashMap<>();
        context.put("sensorId", sensorId);
        context.put("sensorType", sensorType.name());
        context.put("value", value);
        context.put("unit", reading.getUnit());
        context.put("location", locationStr);

        switch (sensorType) {
            case PM25, AIR_QUALITY, PM10 -> {
                context.put("pm25", value);
                if (value > properties.getPm25Critical()) {
                    return ThresholdEvaluationResult.breach(
                            "PM25_CRITICAL_ALERT",
                            "AIR_QUALITY_HAZARD",
                            PriorityTier.CRITICAL,
                            Severity.EXTREME,
                            "Severe Toxic Smog Emergency (PM2.5 > 300)",
                            String.format("[Sensor: %s] Critical PM2.5 level detected at %.1f %s at %s. Severe toxic smog cluster posing immediate public health hazard. (PM2.5: %.1f)",
                                    sensorId, value, reading.getUnit() != null ? reading.getUnit() : "µg/m³", locationStr, value),
                            context
                    );
                } else if (value >= properties.getPm25High()) {
                    return ThresholdEvaluationResult.breach(
                            "PM25_HIGH_ALERT",
                            "AIR_QUALITY_HAZARD",
                            PriorityTier.HIGH,
                            Severity.HIGH,
                            "High Air Quality Hazard (PM2.5 201-300)",
                            String.format("[Sensor: %s] High PM2.5 level detected at %.1f %s at %s. Unhealthy air quality cluster detected. (PM2.5: %.1f)",
                                    sensorId, value, reading.getUnit() != null ? reading.getUnit() : "µg/m³", locationStr, value),
                            context
                    );
                } else if (value >= properties.getPm25Medium()) {
                    return ThresholdEvaluationResult.breach(
                            "PM25_MEDIUM_ALERT",
                            "AIR_QUALITY_HAZARD",
                            PriorityTier.MEDIUM,
                            Severity.MEDIUM,
                            "Moderate Air Quality Warning (PM2.5 101-200)",
                            String.format("[Sensor: %s] Elevated PM2.5 level detected at %.1f %s at %s. (PM2.5: %.1f)",
                                    sensorId, value, reading.getUnit() != null ? reading.getUnit() : "µg/m³", locationStr, value),
                            context
                    );
                }
            }

            case WATER_LEVEL, FLOOD -> {
                context.put("waterLevel", value);
                if (value >= properties.getWaterLevelCritical()) {
                    return ThresholdEvaluationResult.breach(
                            "WATER_LEVEL_CRITICAL",
                            "FLASH_FLOOD",
                            PriorityTier.CRITICAL,
                            Severity.EXTREME,
                            "Critical Flash Flood / Submerged Underpass",
                            String.format("[Sensor: %s] Critical water level reached %.1f%s at %s. Submerged underpass and severe flooding entrapment risk.",
                                    sensorId, value, reading.getUnit() != null ? reading.getUnit() : "%", locationStr),
                            context
                    );
                } else if (value >= properties.getWaterLevelDanger()) {
                    return ThresholdEvaluationResult.breach(
                            "WATER_LEVEL_DANGER",
                            "FLASH_FLOOD",
                            PriorityTier.HIGH,
                            Severity.HIGH,
                            "High Water Level Danger Threshold Exceeded",
                            String.format("[Sensor: %s] Dangerous water level detected at %.1f%s at %s. Flood warning and drain blockage check required.",
                                    sensorId, value, reading.getUnit() != null ? reading.getUnit() : "%", locationStr),
                            context
                    );
                }
            }

            case WASTE_LEVEL -> {
                context.put("wasteBinFillPercentage", value);
                if (value >= properties.getWasteLevelCritical()) {
                    return ThresholdEvaluationResult.breach(
                            "WASTE_BIN_CRITICAL",
                            "WASTE_OVERFLOW",
                            PriorityTier.HIGH,
                            Severity.HIGH,
                            "Smart Waste Bin Overflowing Critical Route",
                            String.format("[Sensor: %s] Smart waste bin fill reached %.1f%s at %s. Overflow spilling onto roadway.",
                                    sensorId, value, reading.getUnit() != null ? reading.getUnit() : "%", locationStr),
                            context
                    );
                } else if (value >= properties.getWasteLevelOverflow()) {
                    return ThresholdEvaluationResult.breach(
                            "WASTE_BIN_OVERFLOW",
                            "WASTE_OVERFLOW",
                            PriorityTier.MEDIUM,
                            Severity.MEDIUM,
                            "Smart Waste Bin Exceeded 90% Capacity",
                            String.format("[Sensor: %s] Smart waste bin capacity reached %.1f%s at %s (fill: %.0f%%). Route dispatch scheduled.",
                                    sensorId, value, reading.getUnit() != null ? reading.getUnit() : "%", locationStr, value),
                            context
                    );
                }
            }

            case GAS -> {
                context.put("gasPpm", value);
                if (value >= properties.getGasPpmCritical()) {
                    return ThresholdEvaluationResult.breach(
                            "GAS_LEAK_CRITICAL",
                            "GAS_LEAK",
                            PriorityTier.CRITICAL,
                            Severity.EXTREME,
                            "Severe Combustible/Toxic Gas Leak Emergency",
                            String.format("[Sensor: %s] Hazardous combustible gas detected at %.1f ppm at %s. Immediate explosion risk, evacuation and emergency dispatch required.",
                                    sensorId, value, locationStr),
                            context
                    );
                } else if (value >= properties.getGasPpmDanger()) {
                    return ThresholdEvaluationResult.breach(
                            "GAS_LEAK_HIGH",
                            "GAS_LEAK",
                            PriorityTier.HIGH,
                            Severity.HIGH,
                            "Hazardous Gas Pipeline Leak Detected",
                            String.format("[Sensor: %s] Gas leakage detected at %.1f ppm at %s. Containment crew required.",
                                    sensorId, value, locationStr),
                            context
                    );
                }
            }

            case SMOKE -> {
                context.put("smokePpm", value);
                if (value >= properties.getSmokePpmCritical()) {
                    return ThresholdEvaluationResult.breach(
                            "SMOKE_CRITICAL",
                            "FIRE_HAZARD",
                            PriorityTier.CRITICAL,
                            Severity.EXTREME,
                            "Critical Dense Smoke / Active Fire Outbreak",
                            String.format("[Sensor: %s] Dense smoke concentration detected at %.1f ppm at %s. Potential structural fire outbreak.",
                                    sensorId, value, locationStr),
                            context
                    );
                } else if (value >= properties.getSmokePpmDanger()) {
                    return ThresholdEvaluationResult.breach(
                            "SMOKE_HIGH",
                            "FIRE_HAZARD",
                            PriorityTier.HIGH,
                            Severity.HIGH,
                            "Smoke Detection Warning",
                            String.format("[Sensor: %s] Elevated smoke levels detected at %.1f ppm at %s.",
                                    sensorId, value, locationStr),
                            context
                    );
                }
            }

            case ELECTRICAL -> {
                context.put("electricalSpike", value);
                if (value >= properties.getElectricalHazardThreshold()) {
                    return ThresholdEvaluationResult.breach(
                            "ELECTRICAL_HAZARD_CRITICAL",
                            "ELECTRICAL_HAZARD",
                            PriorityTier.CRITICAL,
                            Severity.EXTREME,
                            "Dangerous Live Exposed Electrical Hazard Detected",
                            String.format("[Sensor: %s] Dangerous electrical discharge / live wire fault indicator (%.1f) detected at %s. Immediate electrocution risk.",
                                    sensorId, value, locationStr),
                            context
                    );
                }
            }

            default -> {
                // Other sensor types (TEMPERATURE, HUMIDITY, TRAFFIC) - default checks
                if (sensorType == SensorType.TEMPERATURE && value >= 50.0) {
                    return ThresholdEvaluationResult.breach(
                            "EXTREME_HEAT_ALERT",
                            "WEATHER_EXTREME",
                            PriorityTier.HIGH,
                            Severity.HIGH,
                            "Extreme Ambient Temperature Hazard",
                            String.format("[Sensor: %s] Severe temperature spike detected at %.1f°C at %s.", sensorId, value, locationStr),
                            context
                    );
                }
            }
        }

        return ThresholdEvaluationResult.noBreach();
    }
}
