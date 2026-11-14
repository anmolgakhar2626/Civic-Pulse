package com.smartcity.weather;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository("cityWeatherObservationRepository")
public interface CityWeatherObservationRepository extends MongoRepository<WeatherObservation, String> {

    Optional<WeatherObservation> findTopByOrderByTimestampDesc();

    List<WeatherObservation> findByTimestampBetweenOrderByTimestampAsc(Instant start, Instant end);
}
