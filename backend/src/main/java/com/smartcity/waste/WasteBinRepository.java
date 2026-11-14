package com.smartcity.waste;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WasteBinRepository extends JpaRepository<WasteBin, UUID> {
    Optional<WasteBin> findByBinCode(String binCode);
    List<WasteBin> findByCurrentFillPercentageGreaterThanEqual(Double threshold);
    List<WasteBin> findBySensorStatus(String sensorStatus);
}
