package com.smartcity.iot;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "weather_observations")
@CompoundIndex(name = "idx_weather_station_timestamp", def = "{'stationId': 1, 'timestamp': -1}")
public class WeatherObservation {

    @Id
    private String id;

    @Indexed
    private String stationId;

    private Double latitude;
    private Double longitude;
    private Double temperature;
    private Double humidity;
    private Double precipitation;
    private Double windSpeed;
    private Double windDirection;
    private Double surfacePressure;
    private Integer weatherCode;
    private String condition;
    private String floodRiskIndex; // LOW, MODERATE, HIGH

    @Indexed
    private Instant timestamp = Instant.now();

    public WeatherObservation() {}

    public WeatherObservation(String stationId, Double latitude, Double longitude, Double temperature,
                              Double humidity, Double precipitation, Double windSpeed, Integer weatherCode,
                              String condition, String floodRiskIndex) {
        this.stationId = stationId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.temperature = temperature;
        this.humidity = humidity;
        this.precipitation = precipitation;
        this.windSpeed = windSpeed;
        this.weatherCode = weatherCode;
        this.condition = condition;
        this.floodRiskIndex = floodRiskIndex;
        this.timestamp = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Double getHumidity() { return humidity; }
    public void setHumidity(Double humidity) { this.humidity = humidity; }

    public Double getPrecipitation() { return precipitation; }
    public void setPrecipitation(Double precipitation) { this.precipitation = precipitation; }

    public Double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(Double windSpeed) { this.windSpeed = windSpeed; }

    public Double getWindDirection() { return windDirection; }
    public void setWindDirection(Double windDirection) { this.windDirection = windDirection; }

    public Double getSurfacePressure() { return surfacePressure; }
    public void setSurfacePressure(Double surfacePressure) { this.surfacePressure = surfacePressure; }

    public Integer getWeatherCode() { return weatherCode; }
    public void setWeatherCode(Integer weatherCode) { this.weatherCode = weatherCode; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getFloodRiskIndex() { return floodRiskIndex; }
    public void setFloodRiskIndex(String floodRiskIndex) { this.floodRiskIndex = floodRiskIndex; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
