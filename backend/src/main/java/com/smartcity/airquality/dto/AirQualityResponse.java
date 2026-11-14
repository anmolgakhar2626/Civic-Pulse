package com.smartcity.airquality.dto;

import java.time.Instant;

public class AirQualityResponse {

    private Double pm25;
    private String pm25Unit = "µg/m³";
    private Double pm10;
    private String pm10Unit = "µg/m³";
    private Integer aqi;
    private String category; // GOOD, MODERATE, UNHEALTHY, VERY_UNHEALTHY, HAZARDOUS
    private Double latitude;
    private Double longitude;
    private Instant timestamp;
    private boolean thresholdCrossed;
    private String source = "OPEN_METEO_AQ";
    private boolean cached;

    public AirQualityResponse() {}

    public AirQualityResponse(Double pm25, Double pm10, Integer aqi, String category,
                              Double latitude, Double longitude, Instant timestamp,
                              boolean thresholdCrossed, boolean cached) {
        this.pm25 = pm25;
        this.pm10 = pm10;
        this.aqi = aqi;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.thresholdCrossed = thresholdCrossed;
        this.cached = cached;
    }

    public Double getPm25() { return pm25; }
    public void setPm25(Double pm25) { this.pm25 = pm25; }

    public String getPm25Unit() { return pm25Unit; }
    public void setPm25Unit(String pm25Unit) { this.pm25Unit = pm25Unit; }

    public Double getPm10() { return pm10; }
    public void setPm10(Double pm10) { this.pm10 = pm10; }

    public String getPm10Unit() { return pm10Unit; }
    public void setPm10Unit(String pm10Unit) { this.pm10Unit = pm10Unit; }

    public Integer getAqi() { return aqi; }
    public void setAqi(Integer aqi) { this.aqi = aqi; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public boolean isThresholdCrossed() { return thresholdCrossed; }
    public void setThresholdCrossed(boolean thresholdCrossed) { this.thresholdCrossed = thresholdCrossed; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public boolean isCached() { return cached; }
    public void setCached(boolean cached) { this.cached = cached; }
}
