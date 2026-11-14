package com.smartcity.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data Access Object interface for Infrastructure persistence operations.
 * Defines contract for high-performance parameterized JDBC operations.
 */
public interface InfrastructureDao {

    Infrastructure save(Infrastructure infrastructure);

    Optional<Infrastructure> findById(UUID id);

    Optional<Infrastructure> findByCode(String code);

    List<Infrastructure> findAll(
            String category,
            String status,
            String search,
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng,
            int limit,
            int offset
    );

    long count(
            String category,
            String status,
            String search,
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng
    );

    Infrastructure update(Infrastructure infrastructure);

    boolean deleteById(UUID id);

    boolean existsByCode(String code);
}

