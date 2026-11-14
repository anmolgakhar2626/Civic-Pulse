package com.smartcity.iot;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IotEventRepository extends MongoRepository<IotEvent, String> {
    Optional<IotEvent> findByEventId(String eventId);
    List<IotEvent> findByDeviceIdOrderByTimestampDesc(String deviceId);
    Page<IotEvent> findByDeviceIdOrderByTimestampDesc(String deviceId, Pageable pageable);
    List<IotEvent> findByProcessedFalseOrderByTimestampAsc();
    List<IotEvent> findBySeverityOrderByTimestampDesc(String severity);
    Page<IotEvent> findBySeverityOrderByTimestampDesc(String severity, Pageable pageable);
    Page<IotEvent> findAllByOrderByTimestampDesc(Pageable pageable);
}
