package com.smartcity.incident;

import com.smartcity.incident.dto.CreateIncidentRequest;
import com.smartcity.incident.dto.IncidentResponse;
import com.smartcity.incident.dto.IncidentStatusHistoryResponse;
import com.smartcity.incident.dto.UpdateIncidentRequest;
import com.smartcity.incident.dto.UpdateIncidentStatusRequest;
import com.smartcity.priority.PriorityTier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface IncidentService {
    IncidentResponse createIncident(CreateIncidentRequest request);
    IncidentResponse getIncidentById(UUID id);

    Page<IncidentResponse> getFilteredIncidents(
            String search,
            String category,
            String severity,
            String status,
            Pageable pageable
    );

    Page<IncidentResponse> getAllIncidents(
            IncidentStatus status,
            String incidentType,
            IncidentSource source,
            UUID departmentId,
            Pageable pageable
    );

    Page<IncidentResponse> getAllIncidents(
            IncidentStatus status,
            String incidentType,
            IncidentSource source,
            PriorityTier tier,
            String department,
            String assignedTeam,
            Pageable pageable
    );

    IncidentResponse updateIncident(UUID id, UpdateIncidentRequest request);
    IncidentResponse updateIncidentStatus(UUID id, UpdateIncidentStatusRequest request);
    void deleteIncident(UUID id);
    List<IncidentStatusHistoryResponse> getIncidentStatusHistory(UUID incidentId);
}
