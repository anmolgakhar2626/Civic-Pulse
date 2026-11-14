package com.smartcity.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenMeteoWeatherResponse {

    private Double latitude;
    private Double longitude;

    @JsonProperty("current_units")
    private Map<String, String> currentUnits;

    private CurrentWeather current;

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Map<String, String> getCurrentUnits() { return currentUnits; }
    public void setCurrentUnits(Map<String, String> currentUnits) { this.currentUnits = currentUnits; }

    public CurrentWeather getCurrent() { return current; }
    public void setCurrent(CurrentWeather current) { this.current = current; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CurrentWeather {
        private String time;

        @JsonProperty("temperature_2m")
        private Double temperature2m;

        @JsonProperty("relative_humidity_2m")
        private Double relativeHumidity2m;

        private Double precipitation;

        @JsonProperty("wind_speed_10m")
        private Double windSpeed10m;

        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }

        public Double getTemperature2m() { return temperature2m; }
        public void setTemperature2m(Double temperature2m) { this.temperature2m = temperature2m; }

        public Double getRelativeHumidity2m() { return relativeHumidity2m; }
        public void setRelativeHumidity2m(Double relativeHumidity2m) { this.relativeHumidity2m = relativeHumidity2m; }

        public Double getPrecipitation() { return precipitation; }
        public void setPrecipitation(Double precipitation) { this.precipitation = precipitation; }

        public Double getWindSpeed10m() { return windSpeed10m; }
        public void setWindSpeed10m(Double windSpeed10m) { this.windSpeed10m = windSpeed10m; }
    }
}
