package com.smartcity.incident;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID>, JpaSpecificationExecutor<Incident> {
    Optional<Incident> findByIncidentNumber(String incidentNumber);
    boolean existsByIncidentNumber(String incidentNumber);
    List<Incident> findByStatus(IncidentStatus status);
    List<Incident> findByAssignedDepartmentId(UUID departmentId);
    List<Incident> findByIncidentTypeAndStatusIn(String incidentType, java.util.Collection<IncidentStatus> statuses);

    @org.springframework.data.jpa.repository.Query(value =
        "SELECT * FROM incidents " +
        "WHERE earth_box(ll_to_earth(:latitude, :longitude), :radiusMeters) @> ll_to_earth(latitude, longitude) " +
        "AND earth_distance(ll_to_earth(:latitude, :longitude), ll_to_earth(latitude, longitude)) <= :radiusMeters " +
        "ORDER BY earth_distance(ll_to_earth(:latitude, :longitude), ll_to_earth(latitude, longitude)) ASC",
        nativeQuery = true)
    List<Incident> findNearbyIncidents(
        @org.springframework.data.repository.query.Param("latitude") Double latitude,
        @org.springframework.data.repository.query.Param("longitude") Double longitude,
        @org.springframework.data.repository.query.Param("radiusMeters") Double radiusMeters);
}
