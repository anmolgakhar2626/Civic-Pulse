package com.smartcity.airquality.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenMeteoAirQualityResponse {

    private Double latitude;
    private Double longitude;

    @JsonProperty("current_units")
    private Map<String, String> currentUnits;

    private CurrentAirQuality current;

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Map<String, String> getCurrentUnits() { return currentUnits; }
    public void setCurrentUnits(Map<String, String> currentUnits) { this.currentUnits = currentUnits; }

    public CurrentAirQuality getCurrent() { return current; }
    public void setCurrent(CurrentAirQuality current) { this.current = current; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CurrentAirQuality {
        private String time;

        private Double pm10;

        @JsonProperty("pm2_5")
        private Double pm25;

        @JsonProperty("european_aqi")
        private Double europeanAqi;

        @JsonProperty("us_aqi")
        private Double usAqi;

        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }

        public Double getPm10() { return pm10; }
        public void setPm10(Double pm10) { this.pm10 = pm10; }

        public Double getPm25() { return pm25; }
        public void setPm25(Double pm25) { this.pm25 = pm25; }

        public Double getEuropeanAqi() { return europeanAqi; }
        public void setEuropeanAqi(Double europeanAqi) { this.europeanAqi = europeanAqi; }

        public Double getUsAqi() { return usAqi; }
        public void setUsAqi(Double usAqi) { this.usAqi = usAqi; }
    }
}
