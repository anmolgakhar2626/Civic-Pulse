package com.smartcity.priority;

import com.smartcity.priority.dto.EvaluatePriorityRequest;
import com.smartcity.priority.dto.PriorityEvaluationResponse;

import java.util.List;
import java.util.UUID;

public interface PriorityService {
    PriorityEvaluationResponse evaluatePriority(EvaluatePriorityRequest request);
    PriorityEvaluationResponse evaluateIncidentPriority(UUID incidentId);
    List<PriorityRule> getActiveRules();
}
