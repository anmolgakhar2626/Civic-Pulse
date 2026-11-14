package com.smartcity.iot;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeatherObservationRepository extends MongoRepository<WeatherObservation, String> {
    List<WeatherObservation> findByStationIdOrderByTimestampDesc(String stationId);
    List<WeatherObservation> findTop50ByOrderByTimestampDesc();
}
