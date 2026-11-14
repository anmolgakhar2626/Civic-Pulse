package com.smartcity.traffic;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrafficReadingRepository extends MongoRepository<TrafficReading, String> {
    List<TrafficReading> findTop50ByOrderByTimestampDesc();
    List<TrafficReading> findBySensorIdOrderByTimestampDesc(String sensorId);
}
