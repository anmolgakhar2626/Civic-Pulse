package com.smartcity.priority;

import com.smartcity.common.ResourceNotFoundException;
import com.smartcity.incident.*;
import com.smartcity.priority.dto.EvaluatePriorityRequest;
import com.smartcity.priority.dto.PriorityEvaluationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PriorityServiceImpl implements PriorityService {

    private static final Logger log = LoggerFactory.getLogger(PriorityServiceImpl.class);

    private final PriorityEngine priorityEngine;
    private final PriorityRuleRepository priorityRuleRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentStatusHistoryRepository statusHistoryRepository;

    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.smartcity.websocket.WebSocketEventPublisher webSocketEventPublisher;

    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.smartcity.notification.NotificationService notificationService;

    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.smartcity.common.AuditService auditService;

    public PriorityServiceImpl(
            PriorityEngine priorityEngine,
            PriorityRuleRepository priorityRuleRepository,
            IncidentRepository incidentRepository,
            IncidentStatusHistoryRepository statusHistoryRepository
    ) {
        this.priorityEngine = priorityEngine;
        this.priorityRuleRepository = priorityRuleRepository;
        this.incidentRepository = incidentRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PriorityEvaluationResponse evaluatePriority(EvaluatePriorityRequest request) {
        List<PriorityRule> rules = priorityRuleRepository.findByEnabledTrue();
        PriorityResult result = priorityEngine.evaluate(request, rules);
        return PriorityEvaluationResponse.fromResult(result);
    }

    @Override
    public PriorityEvaluationResponse evaluateIncidentPriority(UUID incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", incidentId));

        EvaluatePriorityRequest request = mapIncidentToRequest(incident);
        List<PriorityRule> rules = priorityRuleRepository.findByEnabledTrue();
        PriorityResult result = priorityEngine.evaluate(request, rules);

        // Store result on incident (Requirement 8)
        if (!result.isManualClassificationRequired() && result.getTier() != null) {
            incident.setTier(result.getTier());
            incident.setPriorityLabel(result.getPriorityLabel());
            incident.setPriorityReason(String.join(" | ", result.getReason()));
            incident.setPriorityRuleId(result.getMatchedRule());
            incident.setPriorityEvaluatedAt(result.getEvaluatedAt());

            if (result.getSlaMinutes() != null) {
                incident.setSlaMinutes(result.getSlaMinutes());
                incident.setSlaDeadline(Instant.now().plus(Duration.ofMinutes(result.getSlaMinutes())));
            }

            // Transition incident status to PRIORITIZED if currently DETECTED
            if (incident.getStatus() == IncidentStatus.DETECTED) {
                incident.setStatus(IncidentStatus.PRIORITIZED);
                IncidentStatusHistory history = new IncidentStatusHistory(
                        incident,
                        IncidentStatus.DETECTED.name(),
                        IncidentStatus.PRIORITIZED.name(),
                        "PRIORITY_ENGINE",
                        "Priority evaluated as " + result.getTier() + " via rule: " + result.getMatchedRule()
                );
                statusHistoryRepository.save(history);
            }
        } else {
            // Manual classification required
            incident.setPriorityLabel("MANUAL_REVIEW");
            incident.setPriorityReason(String.join(" | ", result.getReason()));
            incident.setPriorityEvaluatedAt(result.getEvaluatedAt());
        }

        incident.setUpdatedAt(Instant.now());
        incidentRepository.save(incident);

        if (webSocketEventPublisher != null) {
            com.smartcity.websocket.dto.IncidentEvent event = new com.smartcity.websocket.dto.IncidentEvent(
                    incident.getId() != null ? incident.getId().toString() : null,
                    incident.getIncidentNumber(),
                    incident.getIncidentType(),
                    incident.getDescription(),
                    incident.getPriorityTier() != null ? incident.getPriorityTier().name() : null,
                    incident.getPriorityLabel(),
                    incident.getStatus() != null ? incident.getStatus().name() : null,
                    incident.getLatitude(),
                    incident.getLongitude(),
                    incident.getAssignedDepartment() != null ? incident.getAssignedDepartment().getName() : null,
                    Instant.now(),
                    "PRIORITIZED"
            );
            webSocketEventPublisher.publishIncidentEvent(event);

            if (incident.getPriorityTier() == PriorityTier.CRITICAL) {
                if (notificationService != null) {
                    String dept = incident.getAssignedDepartment() != null ? incident.getAssignedDepartment().getName() : null;
                    notificationService.notifyCriticalIncident(incident, dept);
                }
                webSocketEventPublisher.publishAlert(new com.smartcity.websocket.dto.AlertEvent(
                        "CRITICAL_INCIDENT",
                        "CRITICAL",
                        "Critical Priority Assigned: " + incident.getIncidentNumber(),
                        "Incident prioritized as CRITICAL via rule: " + incident.getPriorityRuleId(),
                        incident.getId() != null ? incident.getId().toString() : null,
                        incident.getLatitude(),
                        incident.getLongitude()
                ));
            } else if (incident.getPriorityTier() == PriorityTier.HIGH) {
                webSocketEventPublisher.publishAlert(new com.smartcity.websocket.dto.AlertEvent(
                        "HIGH_PRIORITY_INCIDENT",
                        "HIGH",
                        "High Priority Assigned: " + incident.getIncidentNumber(),
                        "Incident prioritized as HIGH via rule: " + incident.getPriorityRuleId(),
                        incident.getId() != null ? incident.getId().toString() : null,
                        incident.getLatitude(),
                        incident.getLongitude()
                ));
            }
        }

        log.info("Incident {} priority evaluated: tier={}, rule={}",
                incident.getIncidentNumber(), incident.getPriorityLabel(), incident.getPriorityRuleId());

        if (auditService != null && incident.getId() != null) {
            String details = String.format("{\"incidentNumber\":\"%s\",\"tier\":\"%s\",\"label\":\"%s\",\"rule\":\"%s\"}",
                    incident.getIncidentNumber(), incident.getPriorityTier(), incident.getPriorityLabel(), incident.getPriorityRuleId());
            auditService.record("INCIDENT", incident.getId().toString(), "PRIORITY_EVALUATION", "SYSTEM/PRIORITY_ENGINE", details, "127.0.0.1");
        }

        return PriorityEvaluationResponse.fromResult(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriorityRule> getActiveRules() {
        return priorityRuleRepository.findByEnabledTrue();
    }

    private EvaluatePriorityRequest mapIncidentToRequest(Incident incident) {
        EvaluatePriorityRequest req = new EvaluatePriorityRequest();
        req.setIncidentType(incident.getIncidentType());
        req.setDescription(incident.getDescription());
        req.setAddress(incident.getAddress());

        if (incident.getSeverity() != null) {
            req.setSeverity(incident.getSeverity());
        } else if (incident.getSeverityIndicator() != null) {
            req.setSeverity(Severity.fromString(incident.getSeverityIndicator()));
        }

        if (incident.getPublicExposure() != null) {
            req.setPublicExposure(incident.getPublicExposure());
        } else if (incident.getPopulationExposure() != null) {
            req.setPublicExposure(PublicExposure.fromString(incident.getPopulationExposure()));
        }

        req.setInfrastructureImportance(incident.getInfrastructureImportance());
        req.setSafetyImpact(incident.getSafetyImpact());

        // Contextual signals from text / attributes
        String text = (incident.getDescription() != null ? incident.getDescription().toLowerCase() : "") + " " +
                (incident.getAddress() != null ? incident.getAddress().toLowerCase() : "");

        if (text.contains("major junction") || text.contains("arterial") || text.contains("high traffic")) {
            req.setMajorJunction(true);
            req.setBusyRoad(true);
        }
        if (text.contains("emergency access") || text.contains("hospital entrance") || text.contains("fire exit")) {
            req.setBlockingEmergencyAccess(true);
        }
        if (text.contains("live wire") || text.contains("exposed wire")) {
            req.setExposedLiveWire(true);
        }
        if (text.contains("submerged") || text.contains("underpass")) {
            req.setSubmergedUnderpass(true);
        }
        if (text.contains("cave in") || text.contains("sinkhole")) {
            req.setRoadCaveIn(true);
        }
        if (text.contains("spark") || text.contains("transformer")) {
            req.setTransformerSpark(true);
        }

        // Extract PM2.5 value if present in description/text
        java.util.regex.Matcher pmMatcher = java.util.regex.Pattern.compile("(?i)pm2[.]?5[:\\s=]+([0-9]+(?:\\.[0-9]+)?)").matcher(text);
        if (pmMatcher.find()) {
            try {
                req.setPm25(Double.parseDouble(pmMatcher.group(1)));
            } catch (NumberFormatException ignored) {}
        } else {
            java.util.regex.Matcher pmAlt = java.util.regex.Pattern.compile("(?i)pm2[.]?5[^0-9\n\r]{0,30}?([0-9]+(?:\\.[0-9]+)?)").matcher(text);
            while (pmAlt.find()) {
                try {
                    double val = Double.parseDouble(pmAlt.group(1));
                    if (val > 10.0) { // filter out small numbers like sensor numbers
                        req.setPm25(val);
                        break;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // Extract waste bin fill percentage if present in description/text
        java.util.regex.Matcher wasteMatcher = java.util.regex.Pattern.compile("(?i)(?:fill|capacity)[:\\s=]+([0-9]+(?:\\.[0-9]+)?)\\s*%?").matcher(text);
        if (wasteMatcher.find()) {
            try {
                req.setWasteBinFillPercentage(Double.parseDouble(wasteMatcher.group(1)));
            } catch (NumberFormatException ignored) {}
        } else {
            java.util.regex.Matcher wasteAlt = java.util.regex.Pattern.compile("(?i)(?:fill|capacity)[^0-9\n\r]{0,30}?([0-9]+(?:\\.[0-9]+)?)\\s*%?").matcher(text);
            if (wasteAlt.find()) {
                try {
                    req.setWasteBinFillPercentage(Double.parseDouble(wasteAlt.group(1)));
                } catch (NumberFormatException ignored) {}
            }
        }

        return req;
    }
}
