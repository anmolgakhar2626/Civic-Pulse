package com.smartcity.gis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmergencyResourceRepository extends JpaRepository<EmergencyResource, UUID> {
    List<EmergencyResource> findByResourceType(String resourceType);
    List<EmergencyResource> findByAvailableTrue();

    @org.springframework.data.jpa.repository.Query(value =
        "SELECT * FROM emergency_resources " +
        "WHERE earth_box(ll_to_earth(:latitude, :longitude), :radiusMeters) @> ll_to_earth(latitude, longitude) " +
        "AND earth_distance(ll_to_earth(:latitude, :longitude), ll_to_earth(latitude, longitude)) <= :radiusMeters " +
        "ORDER BY earth_distance(ll_to_earth(:latitude, :longitude), ll_to_earth(latitude, longitude)) ASC",
        nativeQuery = true)
    List<EmergencyResource> findNearbyResources(
        @org.springframework.data.repository.query.Param("latitude") Double latitude,
        @org.springframework.data.repository.query.Param("longitude") Double longitude,
        @org.springframework.data.repository.query.Param("radiusMeters") Double radiusMeters);
}
