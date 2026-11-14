package com.smartcity.weather;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "weather_observations")
@CompoundIndex(name = "idx_weather_lat_lon_timestamp", def = "{'latitude': 1, 'longitude': 1, 'timestamp': -1}")
public class WeatherObservation {

    @Id
    private String id;

    private Double latitude;
    private Double longitude;
    private Double temperature;
    private Double humidity;
    private Double precipitation;
    private Double windSpeed;
    private String condition;

    @Indexed
    private Instant timestamp = Instant.now();

    public WeatherObservation() {}

    public WeatherObservation(Double latitude, Double longitude, Double temperature,
                              Double humidity, Double precipitation, Double windSpeed,
                              String condition, Instant timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.temperature = temperature;
        this.humidity = humidity;
        this.precipitation = precipitation;
        this.windSpeed = windSpeed;
        this.condition = condition;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
