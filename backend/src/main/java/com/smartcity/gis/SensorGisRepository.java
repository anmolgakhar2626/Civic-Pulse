package com.smartcity.gis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SensorGisRepository extends JpaRepository<SensorGis, UUID> {

    Optional<SensorGis> findBySensorId(String sensorId);

    List<SensorGis> findByStatus(String status);

    List<SensorGis> findBySensorType(String sensorType);

    @Query(value =
        "SELECT * FROM sensors_gis " +
        "WHERE earth_box(ll_to_earth(:latitude, :longitude), :radiusMeters) @> ll_to_earth(latitude, longitude) " +
        "AND earth_distance(ll_to_earth(:latitude, :longitude), ll_to_earth(latitude, longitude)) <= :radiusMeters " +
        "ORDER BY earth_distance(ll_to_earth(:latitude, :longitude), ll_to_earth(latitude, longitude)) ASC",
        nativeQuery = true)
    List<SensorGis> findNearbySensors(
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude,
        @Param("radiusMeters") Double radiusMeters);
}
