package com.smartcity.airquality;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AirQualityObservationRepository extends MongoRepository<AirQualityObservation, String> {

    Optional<AirQualityObservation> findTopByOrderByTimestampDesc();

    List<AirQualityObservation> findByTimestampBetweenOrderByTimestampAsc(Instant start, Instant end);
}
