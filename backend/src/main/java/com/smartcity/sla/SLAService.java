package com.smartcity.sla;

import com.smartcity.incident.Incident;
import com.smartcity.sla.dto.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SLAService {

    // === Policy Management ===
    List<SlaPolicyResponse> getAllPolicies();

    SlaPolicyResponse createPolicy(CreateSlaPolicyRequest request);

    SlaPolicyResponse updatePolicy(UUID id, UpdateSlaPolicyRequest request);

    SLAPolicy resolvePolicyForIncident(Incident incident);

    // === Incident SLA Tracking ===
    SLAResponse assignPolicyAndCreateSLA(Incident incident);

    SLAResponse createSLAForIncident(String incidentIdentifier);

    SLAResponse createSLAForIncident(UUID incidentId);

    SLAResponse getSLAByIncidentId(String incidentIdentifier);

    SLAResponse getSLAByIncidentId(UUID incidentId);

    SLAResponse getSLAById(UUID id);

    List<SLAResponse> getActiveSLAs();

    List<SLAResponse> getAtRiskSLAs();

    List<SLAResponse> getBreachedSLAs();

    int evaluateAllActiveSLAs();

    SLAResponse recordFirstResponse(UUID incidentId, Instant respondedAt);

    SLAResponse markIncidentResolved(UUID incidentId, Instant resolvedAt);

    // === Escalation & Overview ===
    SlaOverviewResponse getOverviewMetrics();

    List<SlaEscalationResponse> getEscalationHistory(int limit);
}
