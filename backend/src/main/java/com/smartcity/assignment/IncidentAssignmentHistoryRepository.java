package com.smartcity.assignment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentAssignmentHistoryRepository extends JpaRepository<IncidentAssignmentHistory, UUID> {

    @Query("SELECT h FROM IncidentAssignmentHistory h WHERE h.incident.id = :incidentId ORDER BY h.assignedAt DESC")
    List<IncidentAssignmentHistory> findByIncidentIdOrderByAssignedAtDesc(@Param("incidentId") UUID incidentId);
}
