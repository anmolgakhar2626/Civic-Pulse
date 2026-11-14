package com.smartcity.sla;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SLARepository extends JpaRepository<SLARecord, UUID> {

    @Query("SELECT s FROM SLARecord s WHERE s.incident.id = :incidentId")
    Optional<SLARecord> findByIncidentId(@Param("incidentId") UUID incidentId);

    @Query("SELECT s FROM SLARecord s WHERE s.incident.incidentNumber = :incidentNumber")
    Optional<SLARecord> findByIncidentNumber(@Param("incidentNumber") String incidentNumber);

    List<SLARecord> findByStatus(SLAStatus status);

    List<SLARecord> findByStatusIn(Collection<SLAStatus> statuses);

    List<SLARecord> findByStatusNot(SLAStatus status);

    long countByStatus(SLAStatus status);

    @Query("SELECT s FROM SLARecord s WHERE s.status <> com.smartcity.sla.SLAStatus.RESOLVED")
    List<SLARecord> findAllActiveSLAs();
}
