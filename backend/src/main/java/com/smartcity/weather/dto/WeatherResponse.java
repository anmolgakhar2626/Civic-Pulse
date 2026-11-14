package com.smartcity.weather.dto;

import java.time.Instant;

public class WeatherResponse {

    private Double temperature;
    private String temperatureUnit = "°C";
    private Double humidity;
    private String humidityUnit = "%";
    private Double precipitation;
    private String precipitationUnit = "mm";
    private Double windSpeed;
    private String windSpeedUnit = "km/h";
    private Double latitude;
    private Double longitude;
    private Instant timestamp;
    private String condition;
    private String source = "OPEN_METEO";
    private boolean cached;

    public WeatherResponse() {}

    public WeatherResponse(Double temperature, Double humidity, Double precipitation,
                           Double windSpeed, Double latitude, Double longitude,
                           Instant timestamp, String condition, boolean cached) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.precipitation = precipitation;
        this.windSpeed = windSpeed;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.condition = condition;
        this.cached = cached;
    }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public String getTemperatureUnit() { return temperatureUnit; }
    public void setTemperatureUnit(String temperatureUnit) { this.temperatureUnit = temperatureUnit; }

    public Double getHumidity() { return humidity; }
    public void setHumidity(Double humidity) { this.humidity = humidity; }

    public String getHumidityUnit() { return humidityUnit; }
    public void setHumidityUnit(String humidityUnit) { this.humidityUnit = humidityUnit; }

    public Double getPrecipitation() { return precipitation; }
    public void setPrecipitation(Double precipitation) { this.precipitation = precipitation; }

    public String getPrecipitationUnit() { return precipitationUnit; }
    public void setPrecipitationUnit(String precipitationUnit) { this.precipitationUnit = precipitationUnit; }

    public Double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(Double windSpeed) { this.windSpeed = windSpeed; }

    public String getWindSpeedUnit() { return windSpeedUnit; }
    public void setWindSpeedUnit(String windSpeedUnit) { this.windSpeedUnit = windSpeedUnit; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public boolean isCached() { return cached; }
    public void setCached(boolean cached) { this.cached = cached; }
}
