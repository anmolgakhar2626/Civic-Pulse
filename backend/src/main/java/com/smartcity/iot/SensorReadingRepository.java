package com.smartcity.iot;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorReadingRepository extends MongoRepository<SensorReading, String> {
    List<SensorReading> findBySensorIdOrderByTimestampDesc(String sensorId);
    Page<SensorReading> findBySensorIdOrderByTimestampDesc(String sensorId, Pageable pageable);
    List<SensorReading> findBySensorTypeOrderByTimestampDesc(String sensorType);
    Page<SensorReading> findBySensorTypeOrderByTimestampDesc(String sensorType, Pageable pageable);
    List<SensorReading> findTop50ByOrderByTimestampDesc();
    Page<SensorReading> findAllByOrderByTimestampDesc(Pageable pageable);
}
