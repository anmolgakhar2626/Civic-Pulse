package com.smartcity.airquality;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AirQualityReadingRepository extends MongoRepository<AirQualityReading, String> {
    List<AirQualityReading> findTop50ByOrderByTimestampDesc();
    List<AirQualityReading> findByStationIdOrderByTimestampDesc(String stationId);
}
