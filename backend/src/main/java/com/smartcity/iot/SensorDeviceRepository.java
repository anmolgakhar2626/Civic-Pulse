package com.smartcity.iot;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensorDeviceRepository extends MongoRepository<SensorDevice, String> {
    Optional<SensorDevice> findBySensorId(String sensorId);
    boolean existsBySensorId(String sensorId);
    Page<SensorDevice> findBySensorType(SensorType sensorType, Pageable pageable);
    Page<SensorDevice> findByStatus(SensorStatus status, Pageable pageable);
    List<SensorDevice> findByStatus(SensorStatus status);
}
