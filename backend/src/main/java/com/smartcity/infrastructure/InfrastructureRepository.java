package com.smartcity.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InfrastructureRepository extends JpaRepository<Infrastructure, UUID> {
    Optional<Infrastructure> findByCode(String code);
    List<Infrastructure> findByInfrastructureType(String infrastructureType);
    List<Infrastructure> findByImportanceLevel(String importanceLevel);
    List<Infrastructure> findByDepartmentId(UUID departmentId);

    @org.springframework.data.jpa.repository.Query(value =
        "SELECT * FROM infrastructure " +
        "WHERE earth_box(ll_to_earth(:latitude, :longitude), :radiusMeters) @> ll_to_earth(latitude, longitude) " +
        "AND earth_distance(ll_to_earth(:latitude, :longitude), ll_to_earth(latitude, longitude)) <= :radiusMeters " +
        "ORDER BY earth_distance(ll_to_earth(:latitude, :longitude), ll_to_earth(latitude, longitude)) ASC",
        nativeQuery = true)
    List<Infrastructure> findNearbyInfrastructure(
        @org.springframework.data.repository.query.Param("latitude") Double latitude,
        @org.springframework.data.repository.query.Param("longitude") Double longitude,
        @org.springframework.data.repository.query.Param("radiusMeters") Double radiusMeters);
}
