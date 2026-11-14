package com.smartcity.traffic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrafficSignalRepository extends JpaRepository<TrafficSignal, UUID> {
    Optional<TrafficSignal> findBySignalCode(String signalCode);
    List<TrafficSignal> findByOperationalStatus(String operationalStatus);
    List<TrafficSignal> findByJunctionType(String junctionType);
}
