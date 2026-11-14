package com.smartcity.iot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.iot.thresholds")
public class IoTThresholdProperties {

    // PM2.5 thresholds (µg/m³)
    private double pm25Critical = 300.0;
    private double pm25High = 200.0;
    private double pm25Medium = 100.0;

    // Water level / flood thresholds (%)
    private double waterLevelDanger = 80.0;
    private double waterLevelCritical = 95.0;

    // Waste bin fill level thresholds (%)
    private double wasteLevelOverflow = 90.0;
    private double wasteLevelCritical = 98.0;

    // Gas leak concentration thresholds (ppm)
    private double gasPpmDanger = 100.0;
    private double gasPpmCritical = 250.0;

    // Smoke concentration thresholds (ppm)
    private double smokePpmDanger = 50.0;
    private double smokePpmCritical = 100.0;

    // Electrical fault threshold (> 0 indicates electrical hazard)
    private double electricalHazardThreshold = 1.0;

    public double getPm25Critical() { return pm25Critical; }
    public void setPm25Critical(double pm25Critical) { this.pm25Critical = pm25Critical; }

    public double getPm25High() { return pm25High; }
    public void setPm25High(double pm25High) { this.pm25High = pm25High; }

    public double getPm25Medium() { return pm25Medium; }
    public void setPm25Medium(double pm25Medium) { this.pm25Medium = pm25Medium; }

    public double getWaterLevelDanger() { return waterLevelDanger; }
    public void setWaterLevelDanger(double waterLevelDanger) { this.waterLevelDanger = waterLevelDanger; }

    public double getWaterLevelCritical() { return waterLevelCritical; }
    public void setWaterLevelCritical(double waterLevelCritical) { this.waterLevelCritical = waterLevelCritical; }

    public double getWasteLevelOverflow() { return wasteLevelOverflow; }
    public void setWasteLevelOverflow(double wasteLevelOverflow) { this.wasteLevelOverflow = wasteLevelOverflow; }

    public double getWasteLevelCritical() { return wasteLevelCritical; }
    public void setWasteLevelCritical(double wasteLevelCritical) { this.wasteLevelCritical = wasteLevelCritical; }

    public double getGasPpmDanger() { return gasPpmDanger; }
    public void setGasPpmDanger(double gasPpmDanger) { this.gasPpmDanger = gasPpmDanger; }

    public double getGasPpmCritical() { return gasPpmCritical; }
    public void setGasPpmCritical(double gasPpmCritical) { this.gasPpmCritical = gasPpmCritical; }

    public double getSmokePpmDanger() { return smokePpmDanger; }
    public void setSmokePpmDanger(double smokePpmDanger) { this.smokePpmDanger = smokePpmDanger; }

    public double getSmokePpmCritical() { return smokePpmCritical; }
    public void setSmokePpmCritical(double smokePpmCritical) { this.smokePpmCritical = smokePpmCritical; }

    public double getElectricalHazardThreshold() { return electricalHazardThreshold; }
    public void setElectricalHazardThreshold(double electricalHazardThreshold) { this.electricalHazardThreshold = electricalHazardThreshold; }
}
