package com.smartcity.weather;

import com.smartcity.weather.dto.WeatherResponse;

import java.util.List;

public interface WeatherService {

    WeatherResponse getCurrentWeather(Double latitude, Double longitude);

    List<WeatherObservation> getHistoricalWeather(int hours);

    void fetchAndRecordObservation();
}
