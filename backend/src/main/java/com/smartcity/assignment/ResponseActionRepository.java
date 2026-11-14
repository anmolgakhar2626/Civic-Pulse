package com.smartcity.assignment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResponseActionRepository extends JpaRepository<ResponseAction, UUID> {

    @Query("SELECT r FROM ResponseAction r WHERE r.incident.id = :incidentId ORDER BY r.createdAt ASC")
    List<ResponseAction> findByIncidentIdOrderByCreatedAtAsc(@Param("incidentId") UUID incidentId);
}
