package com.smartcity.incident;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentStatusHistoryRepository extends JpaRepository<IncidentStatusHistory, UUID> {
    List<IncidentStatusHistory> findByIncidentIdOrderByChangedAtDesc(UUID incidentId);
    List<IncidentStatusHistory> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId);
}
