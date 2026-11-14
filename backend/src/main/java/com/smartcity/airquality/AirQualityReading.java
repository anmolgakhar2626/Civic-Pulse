package com.smartcity.airquality;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "air_quality_readings")
public class AirQualityReading {

    @Id
    private String id;
    private String stationId;
    private String locationName;
    private Double latitude;
    private Double longitude;
    private Double aqi;
    private Double pm25;
    private Double pm10;
    private Double no2;
    private Double co;
    private Double so2;
    private Double o3;
    private String status; // GOOD, MODERATE, UNHEALTHY, VERY_UNHEALTHY, HAZARDOUS
    private Boolean simulated = true;
    private Instant timestamp = Instant.now();

    public AirQualityReading() {}

    public AirQualityReading(String stationId, String locationName, Double latitude, Double longitude,
                             Double aqi, Double pm25, Double pm10, Double no2, Double co, Double so2, Double o3, Boolean simulated) {
        this.stationId = stationId;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.aqi = aqi;
        this.pm25 = pm25;
        this.pm10 = pm10;
        this.no2 = no2;
        this.co = co;
        this.so2 = so2;
        this.o3 = o3;
        this.simulated = simulated;
        this.status = calculateStatus(aqi != null ? aqi : (pm25 != null ? pm25 : 0));
        this.timestamp = Instant.now();
    }

    private String calculateStatus(double val) {
        if (val <= 50) return "GOOD";
        if (val <= 100) return "MODERATE";
        if (val <= 200) return "UNHEALTHY";
        if (val <= 300) return "VERY_UNHEALTHY";
        return "HAZARDOUS";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getAqi() { return aqi; }
    public void setAqi(Double aqi) {
        this.aqi = aqi;
        this.status = calculateStatus(aqi);
    }

    public Double getPm25() { return pm25; }
    public void setPm25(Double pm25) { this.pm25 = pm25; }

    public Double getPm10() { return pm10; }
    public void setPm10(Double pm10) { this.pm10 = pm10; }

    public Double getNo2() { return no2; }
    public void setNo2(Double no2) { this.no2 = no2; }

    public Double getCo() { return co; }
    public void setCo(Double co) { this.co = co; }

    public Double getSo2() { return so2; }
    public void setSo2(Double so2) { this.so2 = so2; }

    public Double getO3() { return o3; }
    public void setO3(Double o3) { this.o3 = o3; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getSimulated() { return simulated; }
    public void setSimulated(Boolean simulated) { this.simulated = simulated; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
