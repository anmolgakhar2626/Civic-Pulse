package com.smartcity.iot;

import com.smartcity.priority.PriorityTier;
import com.smartcity.priority.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SensorThresholdService Unit Tests")
class SensorThresholdServiceTest {

    private SensorThresholdService thresholdService;
    private IoTThresholdProperties properties;

    @BeforeEach
    void setUp() {
        properties = new IoTThresholdProperties();
        properties.setPm25Critical(300.0);
        properties.setPm25High(200.0);
        properties.setPm25Medium(100.0);
        properties.setWaterLevelDanger(80.0);
        properties.setWaterLevelCritical(95.0);
        properties.setWasteLevelOverflow(90.0);
        properties.setWasteLevelCritical(98.0);
        properties.setGasPpmDanger(100.0);
        properties.setGasPpmCritical(250.0);
        properties.setSmokePpmDanger(50.0);
        properties.setSmokePpmCritical(100.0);
        properties.setElectricalHazardThreshold(1.0);

        thresholdService = new SensorThresholdService(properties);
    }

    @Test
    @DisplayName("Normal reading generates no breach")
    void testNormalReadingNoBreach() {
        SensorReading reading = new SensorReading("PM-01", "PM25", 28.0, "µg/m³", 30.7, 76.7, "Sector 1", "NORMAL");
        ThresholdEvaluationResult result = thresholdService.evaluate(reading);

        assertFalse(result.isBreachDetected());
        assertNull(result.getIncidentType());
    }

    @Test
    @DisplayName("PM2.5 > 300 generates CRITICAL air-quality hazard")
    void testPm25CriticalBreach() {
        SensorReading reading = new SensorReading("PM-01", "PM25", 345.0, "µg/m³", 30.7, 76.7, "Sector 1", "NORMAL");
        ThresholdEvaluationResult result = thresholdService.evaluate(reading);

        assertTrue(result.isBreachDetected());
        assertEquals("AIR_QUALITY_HAZARD", result.getIncidentType());
        assertEquals(PriorityTier.CRITICAL, result.getPriorityTier());
        assertEquals(Severity.EXTREME, result.getSeverity());
        assertTrue(result.getDescription().contains("345.0"));
    }

    @Test
    @DisplayName("PM2.5 between 201-300 generates HIGH air-quality hazard")
    void testPm25HighBreach() {
        SensorReading reading = new SensorReading("PM-01", "PM25", 250.0, "µg/m³", 30.7, 76.7, "Sector 1", "NORMAL");
        ThresholdEvaluationResult result = thresholdService.evaluate(reading);

        assertTrue(result.isBreachDetected());
        assertEquals("AIR_QUALITY_HAZARD", result.getIncidentType());
        assertEquals(PriorityTier.HIGH, result.getPriorityTier());
    }

    @Test
    @DisplayName("PM2.5 between 101-200 generates MEDIUM air-quality hazard")
    void testPm25MediumBreach() {
        SensorReading reading = new SensorReading("PM-01", "PM25", 150.0, "µg/m³", 30.7, 76.7, "Sector 1", "NORMAL");
        ThresholdEvaluationResult result = thresholdService.evaluate(reading);

        assertTrue(result.isBreachDetected());
        assertEquals("AIR_QUALITY_HAZARD", result.getIncidentType());
        assertEquals(PriorityTier.MEDIUM, result.getPriorityTier());
    }

    @Test
    @DisplayName("Water level >= 95% generates CRITICAL flash flood hazard")
    void testWaterLevelCriticalBreach() {
        SensorReading reading = new SensorReading("WTR-01", "WATER_LEVEL", 96.0, "%", 30.72, 76.74, "Railway Underpass", "NORMAL");
        ThresholdEvaluationResult result = thresholdService.evaluate(reading);

        assertTrue(result.isBreachDetected());
        assertEquals("FLASH_FLOOD", result.getIncidentType());
        assertEquals(PriorityTier.CRITICAL, result.getPriorityTier());
        assertEquals(Severity.EXTREME, result.getSeverity());
    }

    @Test
    @DisplayName("Water level between 80-95% generates HIGH water level danger")
    void testWaterLevelDangerBreach() {
        SensorReading reading = new SensorReading("WTR-01", "WATER_LEVEL", 88.0, "%", 30.72, 76.74, "Drain Channel", "NORMAL");
        ThresholdEvaluationResult result = thresholdService.evaluate(reading);

        assertTrue(result.isBreachDetected());
        assertEquals("FLASH_FLOOD", result.getIncidentType());
        assertEquals(PriorityTier.HIGH, result.getPriorityTier());
    }

    @Test
    @DisplayName("Waste bin fill >= 90% generates MEDIUM waste overflow incident")
    void testWasteBinOverflowBreach() {
        SensorReading reading = new SensorReading("BIN-01", "WASTE_LEVEL", 94.0, "%", 30.71, 76.72, "Commercial Market", "NORMAL");
        ThresholdEvaluationResult result = thresholdService.evaluate(reading);

        assertTrue(result.isBreachDetected());
        assertEquals("WASTE_OVERFLOW", result.getIncidentType());
        assertEquals(PriorityTier.MEDIUM, result.getPriorityTier());
    }

    @Test
    @DisplayName("Gas concentration >= 100 ppm generates HIGH gas leak incident")
    void testGasLeakHighBreach() {
        SensorReading reading = new SensorReading("GAS-01", "GAS", 185.0, "ppm", 30.71, 76.72, "Industrial District", "NORMAL");
        ThresholdEvaluationResult result = thresholdService.evaluate(reading);

        assertTrue(result.isBreachDetected());
        assertEquals("GAS_LEAK", result.getIncidentType());
        assertEquals(PriorityTier.HIGH, result.getPriorityTier());
    }

    @Test
    @DisplayName("Gas concentration >= 250 ppm generates CRITICAL gas leak emergency")
    void testGasLeakCriticalBreach() {
        SensorReading reading = new SensorReading("GAS-01", "GAS", 280.0, "ppm", 30.71, 76.72, "Industrial District", "NORMAL");
        ThresholdEvaluationResult result = thresholdService.evaluate(reading);

        assertTrue(result.isBreachDetected());
        assertEquals("GAS_LEAK", result.getIncidentType());
        assertEquals(PriorityTier.CRITICAL, result.getPriorityTier());
    }

    @Test
    @DisplayName("Electrical hazard indicator >= 1.0 generates CRITICAL electrical hazard")
    void testElectricalHazardBreach() {
        SensorReading reading = new SensorReading("ELEC-01", "ELECTRICAL", 1.0, "fault", 30.70, 76.71, "Substation 4", "NORMAL");
        ThresholdEvaluationResult result = thresholdService.evaluate(reading);

        assertTrue(result.isBreachDetected());
        assertEquals("ELECTRICAL_HAZARD", result.getIncidentType());
        assertEquals(PriorityTier.CRITICAL, result.getPriorityTier());
        assertEquals(Severity.EXTREME, result.getSeverity());
    }

    @Test
    @DisplayName("Smoke concentration >= 100 ppm generates CRITICAL fire hazard")
    void testSmokeCriticalBreach() {
        SensorReading reading = new SensorReading("SMK-01", "SMOKE", 120.0, "ppm", 30.70, 76.71, "Commercial Hub", "NORMAL");
        ThresholdEvaluationResult result = thresholdService.evaluate(reading);

        assertTrue(result.isBreachDetected());
        assertEquals("FIRE_HAZARD", result.getIncidentType());
        assertEquals(PriorityTier.CRITICAL, result.getPriorityTier());
    }
}
