package com.smartcity.airquality;

import com.smartcity.airquality.dto.AirQualityResponse;

import java.util.List;

public interface AirQualityService {

    AirQualityResponse getCurrentAirQuality(Double latitude, Double longitude);

    List<AirQualityObservation> getHistoricalAirQuality(int hours);

    void fetchAndRecordObservation();
}
