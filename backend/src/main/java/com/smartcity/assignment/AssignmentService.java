package com.smartcity.assignment;

import com.smartcity.assignment.dto.AssignIncidentRequest;
import com.smartcity.assignment.dto.AssignmentHistoryResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentService {

    AssignmentResult autoAssign(String incidentIdentifier);

    AssignmentResult assign(String incidentIdentifier, AssignIncidentRequest request);

    List<AssignmentHistoryResponse> getAssignmentHistory(String incidentIdentifier);

    Optional<AssignmentRule> findMatchingRule(String incidentType);
}
