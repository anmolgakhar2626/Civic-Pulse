package com.smartcity.airquality;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "air_quality_observations")
@CompoundIndex(name = "idx_aq_lat_lon_timestamp", def = "{'latitude': 1, 'longitude': 1, 'timestamp': -1}")
public class AirQualityObservation {

    @Id
    private String id;

    private Double latitude;
    private Double longitude;
    private Double pm25;
    private Double pm10;
    private Integer aqi;
    private String category;
    private boolean thresholdCrossed;

    @Indexed
    private Instant timestamp = Instant.now();

    public AirQualityObservation() {}

    public AirQualityObservation(Double latitude, Double longitude, Double pm25, Double pm10,
                                 Integer aqi, String category, boolean thresholdCrossed, Instant timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.pm25 = pm25;
        this.pm10 = pm10;
        this.aqi = aqi;
        this.category = category;
        this.thresholdCrossed = thresholdCrossed;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getPm25() { return pm25; }
    public void setPm25(Double pm25) { this.pm25 = pm25; }

    public Double getPm10() { return pm10; }
    public void setPm10(Double pm10) { this.pm10 = pm10; }

    public Integer getAqi() { return aqi; }
    public void setAqi(Integer aqi) { this.aqi = aqi; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isThresholdCrossed() { return thresholdCrossed; }
    public void setThresholdCrossed(boolean thresholdCrossed) { this.thresholdCrossed = thresholdCrossed; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
