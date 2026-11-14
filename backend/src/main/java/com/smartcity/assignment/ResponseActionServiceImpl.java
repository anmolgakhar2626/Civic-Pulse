package com.smartcity.assignment;

import com.smartcity.assignment.dto.CreateResponseActionRequest;
import com.smartcity.assignment.dto.ResponseActionResponse;
import com.smartcity.assignment.dto.UpdateResponseActionStatusRequest;
import com.smartcity.common.ResourceNotFoundException;
import com.smartcity.incident.Incident;
import com.smartcity.incident.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ResponseActionServiceImpl implements ResponseActionService {

    private static final Logger log = LoggerFactory.getLogger(ResponseActionServiceImpl.class);

    private final IncidentRepository incidentRepository;
    private final ResponseActionRepository responseActionRepository;
    private final ResponsePolicy responsePolicy;

    public ResponseActionServiceImpl(
            IncidentRepository incidentRepository,
            ResponseActionRepository responseActionRepository,
            ResponsePolicy responsePolicy
    ) {
        this.incidentRepository = incidentRepository;
        this.responseActionRepository = responseActionRepository;
        this.responsePolicy = responsePolicy;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseActionResponse> getActionsForIncident(String incidentIdentifier) {
        Incident incident = resolveIncident(incidentIdentifier);
        List<ResponseAction> actions = responseActionRepository.findByIncidentIdOrderByCreatedAtAsc(incident.getId());
        return actions.stream().map(ResponseActionResponse::fromEntity).collect(Collectors.toList());
    }

    @Override
    public ResponseActionResponse createAction(String incidentIdentifier, CreateResponseActionRequest request) {
        Incident incident = resolveIncident(incidentIdentifier);
        if (request == null || request.getActionType() == null) {
            throw new IllegalArgumentException("Action type cannot be null");
        }

        ResponseAction action = new ResponseAction(
                incident,
                request.getActionType(),
                request.getDescription() != null ? request.getDescription() : request.getActionType().name(),
                request.getStatus() != null ? request.getStatus() : ResponseActionStatus.RECOMMENDED,
                request.getCreatedBy() != null ? request.getCreatedBy() : "OPERATOR"
        );

        ResponseAction saved = responseActionRepository.save(action);
        log.info("Created response action '{}' for incident {}", saved.getActionType(), incident.getIncidentNumber());
        return ResponseActionResponse.fromEntity(saved);
    }

    @Override
    public ResponseActionResponse updateActionStatus(UUID actionId, UpdateResponseActionStatusRequest request) {
        ResponseAction action = responseActionRepository.findById(actionId)
                .orElseThrow(() -> new ResourceNotFoundException("ResponseAction", "id", actionId));

        if (request == null || request.getStatus() == null) {
            throw new IllegalArgumentException("Target status cannot be null");
        }

        action.setStatus(request.getStatus());
        if (request.getStatus() == ResponseActionStatus.COMPLETED && action.getCompletedAt() == null) {
            action.setCompletedAt(Instant.now());
        }

        ResponseAction updated = responseActionRepository.save(action);
        log.info("Updated response action {} to status {}", actionId, updated.getStatus());
        return ResponseActionResponse.fromEntity(updated);
    }

    @Override
    public List<ResponseActionResponse> generateResponsePlan(String incidentIdentifier) {
        Incident incident = resolveIncident(incidentIdentifier);
        List<ResponsePolicy.RecommendedAction> recommendations = responsePolicy.evaluate(incident);

        List<ResponseAction> savedActions = new ArrayList<>();
        for (ResponsePolicy.RecommendedAction rec : recommendations) {
            ResponseAction action = new ResponseAction(
                    incident,
                    rec.actionType(),
                    rec.description(),
                    ResponseActionStatus.RECOMMENDED,
                    "RESPONSE_POLICY_ENGINE"
            );
            savedActions.add(responseActionRepository.save(action));
        }

        log.info("Generated response plan with {} recommended action(s) for incident {}",
                savedActions.size(), incident.getIncidentNumber());

        return savedActions.stream().map(ResponseActionResponse::fromEntity).collect(Collectors.toList());
    }

    private Incident resolveIncident(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new ResourceNotFoundException("Incident identifier cannot be blank");
        }

        try {
            UUID uuid = UUID.fromString(identifier.trim());
            Optional<Incident> byId = incidentRepository.findById(uuid);
            if (byId.isPresent()) {
                return byId.get();
            }
        } catch (IllegalArgumentException ignored) {
            // Not a UUID, try by incident number
        }

        return incidentRepository.findByIncidentNumber(identifier.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with identifier: " + identifier));
    }
}
