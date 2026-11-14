package com.smartcity.sla;

import com.smartcity.sla.dto.SlaEscalationResponse;
import com.smartcity.sla.dto.SlaOverviewResponse;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data Access Object interface for SLA policies, incident tracking,
 * and escalation history persistence using Spring JdbcTemplate.
 */
public interface SlaDao {

    // === SLA Policy Management ===
    SLAPolicy savePolicy(SLAPolicy policy);

    int updatePolicy(UUID id, SLAPolicy policy);

    Optional<SLAPolicy> findPolicyById(UUID id);

    Optional<SLAPolicy> findPolicyByCategoryAndSeverity(String category, String severity);

    List<SLAPolicy> findAllPolicies();

    // === SLA Record Tracking ===
    SLARecord saveRecord(SLARecord record);

    int updateRecord(SLARecord record);

    Optional<SLARecord> findRecordById(UUID id);

    Optional<SLARecord> findRecordByIncidentId(UUID incidentId);

    List<SLARecord> findAllActiveRecords();

    List<SLARecord> findRecordsByStatus(SLAStatus status);

    int recordFirstResponse(UUID incidentId, Instant respondedAt);

    int recordResolution(UUID incidentId, Instant resolvedAt);

    int updateRecordStatusAndEscalation(UUID id, SLAStatus status, Long remainingMinutes, EscalationLevel level, Boolean breached, Instant breachedAt, Instant lastEscalatedAt);

    // === Escalation History & Auditing ===
    void recordEscalation(UUID incidentId, UUID slaRecordId, String prevEscalation, String newEscalation, String prevStatus, String newStatus, String reason, Instant escalatedAt);

    List<SlaEscalationResponse> getEscalationHistory(int limit);

    boolean hasRecentEscalation(UUID incidentId, String newEscalationLevel);

    // === Incident Metadata Sync ===
    int updateIncidentEscalationState(UUID incidentId, String escalationState);

    int updateIncidentSlaDeadline(UUID incidentId, Integer slaMinutes, Instant slaDeadline);

    // === Dashboard Overview Metrics ===
    SlaOverviewResponse getOverviewMetrics();
}
