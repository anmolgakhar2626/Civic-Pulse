package com.smartcity.waste;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WasteReadingRepository extends MongoRepository<WasteBinReading, String> {
    List<WasteBinReading> findTop50ByOrderByTimestampDesc();
    List<WasteBinReading> findByBinIdOrderByTimestampDesc(String binId);
}
