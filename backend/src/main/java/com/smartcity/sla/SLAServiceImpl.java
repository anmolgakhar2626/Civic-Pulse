package com.smartcity.sla;

import com.smartcity.common.ResourceNotFoundException;
import com.smartcity.incident.Incident;
import com.smartcity.incident.IncidentRepository;
import com.smartcity.incident.IncidentStatus;
import com.smartcity.priority.PriorityTier;
import com.smartcity.sla.dto.*;
import com.smartcity.websocket.WebSocketEventPublisher;
import com.smartcity.websocket.dto.AlertEvent;
import com.smartcity.websocket.dto.SLAEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class SLAServiceImpl implements SLAService {

    private static final Logger log = LoggerFactory.getLogger(SLAServiceImpl.class);

    private final SLARepository slaRepository;
    private final IncidentRepository incidentRepository;
    private final SLACalculator slaCalculator;
    private final SlaDao slaDao;
    private Clock clock = Clock.systemUTC();

    @Autowired(required = false)
    @Lazy
    private WebSocketEventPublisher webSocketEventPublisher;

    @Autowired(required = false)
    @Lazy
    private com.smartcity.notification.NotificationService notificationService;

    @Autowired(required = false)
    @Lazy
    private com.smartcity.common.AuditService auditService;

    @Autowired(required = false)
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Autowired
    public SLAServiceImpl(
            SLARepository slaRepository,
            IncidentRepository incidentRepository,
            SLACalculator slaCalculator,
            @Autowired(required = false) SlaDao slaDao
    ) {
        this.slaRepository = slaRepository;
        this.incidentRepository = incidentRepository;
        this.slaCalculator = slaCalculator;
        this.slaDao = slaDao;
    }

    public SLAServiceImpl(SLARepository slaRepository, IncidentRepository incidentRepository, SLACalculator slaCalculator) {
        this(slaRepository, incidentRepository, slaCalculator, null);
    }

    public void setNotificationService(com.smartcity.notification.NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void setWebSocketEventPublisher(WebSocketEventPublisher webSocketEventPublisher) {
        this.webSocketEventPublisher = webSocketEventPublisher;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public Clock getClock() {
        return clock;
    }

    private Instant now() {
        return Instant.now(clock);
    }

    // === POLICY MANAGEMENT ===

    @Override
    @Transactional(readOnly = true)
    public List<SlaPolicyResponse> getAllPolicies() {
        if (slaDao != null) {
            return slaDao.findAllPolicies().stream()
                    .map(SlaPolicyResponse::fromEntity)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public SlaPolicyResponse createPolicy(CreateSlaPolicyRequest request) {
        SLAPolicy policy = new SLAPolicy(
                request.getCategory(),
                request.getSeverity().toUpperCase(),
                request.getResponseTimeMinutes(),
                request.getResolutionTimeMinutes(),
                request.getEnabled() != null ? request.getEnabled() : true
        );

        if (slaDao != null) {
            SLAPolicy saved = slaDao.savePolicy(policy);
            return SlaPolicyResponse.fromEntity(saved);
        }
        return SlaPolicyResponse.fromEntity(policy);
    }

    @Override
    public SlaPolicyResponse updatePolicy(UUID id, UpdateSlaPolicyRequest request) {
        if (slaDao != null) {
            SLAPolicy existing = slaDao.findPolicyById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("SLA Policy not found with ID: " + id));

            if (request.getCategory() != null) existing.setCategory(request.getCategory());
            if (request.getSeverity() != null) existing.setSeverity(request.getSeverity().toUpperCase());
            if (request.getResponseTimeMinutes() != null) existing.setResponseTimeMinutes(request.getResponseTimeMinutes());
            if (request.getResolutionTimeMinutes() != null) existing.setResolutionTimeMinutes(request.getResolutionTimeMinutes());
            if (request.getEnabled() != null) existing.setEnabled(request.getEnabled());

            slaDao.updatePolicy(id, existing);
            return SlaPolicyResponse.fromEntity(existing);
        }
        throw new UnsupportedOperationException("SLA persistence DAO unavailable");
    }

    @Override
    @Transactional(readOnly = true)
    public SLAPolicy resolvePolicyForIncident(Incident incident) {
        String category = incident.getCategory() != null ? incident.getCategory() : incident.getIncidentType();
        String severity = incident.getSeverityIndicator();
        if (severity == null && incident.getPriority() != null) {
            severity = incident.getPriority().name();
        }
        if (severity == null && incident.getTier() != null) {
            severity = incident.getTier().name();
        }
        if (severity == null) {
            severity = "MEDIUM";
        }

        if (slaDao != null) {
            Optional<SLAPolicy> policyOpt = slaDao.findPolicyByCategoryAndSeverity(category, severity);
            if (policyOpt.isPresent()) {
                return policyOpt.get();
            }
        }

        // Default in-memory policy fallback
        int resTime = 720;
        int respTime = 60;
        if ("CRITICAL".equalsIgnoreCase(severity)) {
            respTime = 10;
            resTime = 30;
        } else if ("HIGH".equalsIgnoreCase(severity)) {
            respTime = 30;
            resTime = 120;
        } else if ("LOW".equalsIgnoreCase(severity)) {
            respTime = 120;
            resTime = 1440;
        }

        return new SLAPolicy(category != null ? category : "DEFAULT", severity.toUpperCase(), respTime, resTime, true);
    }

    // === INCIDENT SLA TRACKING ===

    @Override
    public SLAResponse assignPolicyAndCreateSLA(Incident incident) {
        if (incident == null) {
            throw new IllegalArgumentException("Incident cannot be null");
        }

        SLAPolicy policy = resolvePolicyForIncident(incident);
        PriorityTier tier = resolvePriorityTier(incident);
        if (tier == null) {
            throw new IllegalStateException("Incident must be prioritized before SLA creation.");
        }
        final PriorityTier effectiveTier = tier;

        Instant startedAt = incident.getReportedAt() != null ? incident.getReportedAt()
                : (incident.getCreatedAt() != null ? incident.getCreatedAt() : now());

        int resolutionMinutes = policy.getResolutionTimeMinutes() != null ? policy.getResolutionTimeMinutes() : 120;
        int responseMinutes = policy.getResponseTimeMinutes() != null ? policy.getResponseTimeMinutes() : 30;

        Instant resolutionDeadline = startedAt.plus(resolutionMinutes, ChronoUnit.MINUTES);
        Instant responseDeadline = startedAt.plus(responseMinutes, ChronoUnit.MINUTES);

        Instant currentNow = now();
        long remainingMinutes = slaCalculator.calculateRemainingMinutes(resolutionDeadline, currentNow);
        long remainingResponseMinutes = slaCalculator.calculateRemainingMinutes(responseDeadline, currentNow);

        SLAStatus status = slaCalculator.calculateStatus(startedAt, resolutionDeadline, incident.getResolutionTime(), currentNow);
        EscalationLevel escalationLevel = slaCalculator.calculateEscalationLevel(effectiveTier, status, EscalationLevel.NONE);

        SLARecord slaRecord;
        if (slaDao != null) {
            slaRecord = slaDao.findRecordByIncidentId(incident.getId())
                    .orElseGet(() -> new SLARecord(incident, effectiveTier, resolutionMinutes, startedAt, resolutionDeadline));
        } else {
            slaRecord = slaRepository.findByIncidentId(incident.getId())
                    .orElseGet(() -> new SLARecord(incident, effectiveTier, resolutionMinutes, startedAt, resolutionDeadline));
        }

        slaRecord.setIncident(incident);
        slaRecord.setPolicy(policy);
        slaRecord.setPriorityTier(effectiveTier);
        slaRecord.setTargetMinutes(resolutionMinutes);
        slaRecord.setTargetSlaMinutes(resolutionMinutes);
        slaRecord.setStartedAt(startedAt);
        slaRecord.setDeadlineAt(resolutionDeadline);
        slaRecord.setResolutionDeadline(resolutionDeadline);
        slaRecord.setResponseDeadline(responseDeadline);
        slaRecord.setStatus(status);
        slaRecord.setRemainingMinutes(remainingMinutes);
        slaRecord.setEscalationLevel(escalationLevel);

        if (status == SLAStatus.BREACHED) {
            slaRecord.setBreached(true);
            if (slaRecord.getBreachedAt() == null) {
                slaRecord.setBreachedAt(currentNow);
            }
        }

        incident.setSlaMinutes(resolutionMinutes);
        incident.setSlaDeadline(resolutionDeadline);
        if (slaDao != null && incident.getId() != null) {
            slaDao.updateIncidentSlaDeadline(incident.getId(), resolutionMinutes, resolutionDeadline);
        } else {
            try { incidentRepository.save(incident); } catch (Exception ignored) {}
        }

        SLARecord saved;
        if (slaDao != null) {
            saved = slaDao.saveRecord(slaRecord);
        } else {
            saved = slaRepository.save(slaRecord);
        }

        log.info("Assigned SLA policy [{}] to incident {}: responseLimit={}m, resolutionLimit={}m, deadline={}",
                policy.getCategory() + "/" + policy.getSeverity(), incident.getIncidentNumber(), responseMinutes, resolutionMinutes, resolutionDeadline);

        publishSlaEvents(saved, null, null);

        SLAResponse response = SLAResponse.fromEntity(saved != null ? saved : slaRecord);
        if (response != null) {
            response.setRemainingResponseMinutes(remainingResponseMinutes);
            response.setRemainingResolutionMinutes(remainingMinutes);
        }
        return response;
    }

    @Override
    public SLAResponse createSLAForIncident(String incidentIdentifier) {
        Incident incident = resolveIncident(incidentIdentifier);
        return assignPolicyAndCreateSLA(incident);
    }

    @Override
    public SLAResponse createSLAForIncident(UUID incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", incidentId));
        return assignPolicyAndCreateSLA(incident);
    }

    @Override
    @Transactional(readOnly = true)
    public SLAResponse getSLAByIncidentId(String incidentIdentifier) {
        Incident incident = resolveIncident(incidentIdentifier);
        return getSLAByIncidentId(incident.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public SLAResponse getSLAByIncidentId(UUID incidentId) {
        SLARecord record;
        if (slaDao != null) {
            record = slaDao.findRecordByIncidentId(incidentId)
                    .orElseThrow(() -> new ResourceNotFoundException("SLARecord", "incidentId", incidentId));
        } else {
            record = slaRepository.findByIncidentId(incidentId)
                    .orElseThrow(() -> new ResourceNotFoundException("SLARecord", "incidentId", incidentId));
        }

        enrichDynamicDeadlines(record);
        return SLAResponse.fromEntity(record);
    }

    @Override
    @Transactional(readOnly = true)
    public SLAResponse getSLAById(UUID id) {
        SLARecord record;
        if (slaDao != null) {
            record = slaDao.findRecordById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("SLARecord", "id", id));
        } else {
            record = slaRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("SLARecord", "id", id));
        }

        enrichDynamicDeadlines(record);
        return SLAResponse.fromEntity(record);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SLAResponse> getActiveSLAs() {
        List<SLARecord> list;
        if (slaDao != null) {
            list = slaDao.findAllActiveRecords();
        } else {
            list = slaRepository.findAllActiveSLAs();
        }

        return list.stream()
                .peek(this::enrichDynamicDeadlines)
                .map(SLAResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SLAResponse> getAtRiskSLAs() {
        List<SLARecord> list;
        if (slaDao != null) {
            list = slaDao.findRecordsByStatus(SLAStatus.WARNING);
        } else {
            list = slaRepository.findByStatus(SLAStatus.AT_RISK);
        }

        return list.stream()
                .peek(this::enrichDynamicDeadlines)
                .map(SLAResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SLAResponse> getBreachedSLAs() {
        List<SLARecord> list;
        if (slaDao != null) {
            list = slaDao.findRecordsByStatus(SLAStatus.BREACHED);
        } else {
            list = slaRepository.findByStatus(SLAStatus.BREACHED);
        }

        return list.stream()
                .peek(this::enrichDynamicDeadlines)
                .map(SLAResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private void enrichDynamicDeadlines(SLARecord record) {
        if (record == null) return;
        Instant currentNow = now();
        if (record.getStatus() != null && !record.getStatus().isTerminal()) {
            Instant targetDeadline = record.getResolutionDeadline() != null ? record.getResolutionDeadline() : record.getDeadlineAt();
            if (targetDeadline != null) {
                record.setRemainingMinutes(slaCalculator.calculateRemainingMinutes(targetDeadline, currentNow));
            }
        }
    }

    @Override
    public int evaluateAllActiveSLAs() {
        List<SLARecord> activeRecords;
        if (slaDao != null) {
            activeRecords = slaDao.findAllActiveRecords();
        } else {
            activeRecords = slaRepository.findAllActiveSLAs();
        }

        Instant currentNow = now();
        int evaluatedCount = 0;

        for (SLARecord record : activeRecords) {
            Incident incident = record.getIncident();
            if (incident == null && record.getIncidentId() != null) {
                incident = incidentRepository.findById(record.getIncidentId()).orElse(null);
                record.setIncident(incident);
            }

            // Terminal check: if incident is RESOLVED or CLOSED, mark SLA COMPLETED
            if (incident != null && (incident.getStatus() == IncidentStatus.RESOLVED || incident.getStatus() == IncidentStatus.CLOSED)) {
                Instant resolvedAt = incident.getResolutionTime() != null ? incident.getResolutionTime() : currentNow;
                markIncidentResolved(incident.getId(), resolvedAt);
                evaluatedCount++;
                continue;
            }

            // Skip already terminal records
            if (record.getStatus() != null && record.getStatus().isTerminal()) {
                continue;
            }

            Instant deadline = record.getResolutionDeadline() != null ? record.getResolutionDeadline() : record.getDeadlineAt();
            Instant respDeadline = record.getResponseDeadline();

            // 1. Calculate remaining resolution time
            long remainingMinutes = slaCalculator.calculateRemainingMinutes(deadline, currentNow);
            record.setRemainingMinutes(remainingMinutes);

            // 2. Evaluate SLA status
            SLAStatus previousStatus = record.getStatus();
            EscalationLevel previousEscalation = record.getEscalationLevel();

            SLAStatus newStatus = slaCalculator.calculateStatus(record.getStartedAt(), deadline, record.getResolvedAt(), currentNow);

            // Also check response deadline breach if first response hasn't occurred
            if (newStatus != SLAStatus.BREACHED && record.getFirstRespondedAt() == null && respDeadline != null && currentNow.isAfter(respDeadline)) {
                newStatus = SLAStatus.BREACHED;
            }

            record.setStatus(newStatus);

            // 3. Detect breaches
            boolean newlyBreached = false;
            if (newStatus == SLAStatus.BREACHED) {
                if (record.getBreached() == null || !record.getBreached()) {
                    record.setBreached(true);
                    record.setBreachedAt(currentNow);
                    newlyBreached = true;
                    log.warn("SLA breached for incident {}: deadline was {}",
                            incident != null ? incident.getIncidentNumber() : record.getIncidentId(), deadline);
                }
            }

            // 4. Calculate escalation level
            EscalationLevel newEscalation = slaCalculator.calculateEscalationLevel(
                    record.getPriorityTier(),
                    newStatus,
                    previousEscalation
            );
            record.setEscalationLevel(newEscalation);

            boolean escalationChanged = (previousEscalation == null || newEscalation != previousEscalation);

            // 5. Automatic Escalation Audit Record (prevent duplicates)
            if (escalationChanged && newEscalation != EscalationLevel.NONE) {
                record.setLastEscalatedAt(currentNow);
                if (slaDao != null && incident != null) {
                    boolean alreadyAudited = slaDao.hasRecentEscalation(incident.getId(), newEscalation.name());
                    if (!alreadyAudited) {
                        String reason = "Automatic escalation triggered due to SLA " + newStatus.name() + " (" + remainingMinutes + " mins remaining)";
                        slaDao.recordEscalation(
                                incident.getId(),
                                record.getId(),
                                previousEscalation != null ? previousEscalation.name() : "NONE",
                                newEscalation.name(),
                                previousStatus != null ? previousStatus.name() : "ON_TRACK",
                                newStatus.name(),
                                reason,
                                currentNow
                        );
                        log.info("Recorded SLA escalation audit: Incident {} -> {}", incident.getIncidentNumber(), newEscalation);
                    }
                }
            }

            if (incident != null && incident.getId() != null) {
                incident.setEscalationState(newEscalation.name());
                if (slaDao != null) {
                    slaDao.updateIncidentEscalationState(incident.getId(), newEscalation.name());
                } else {
                    try { incidentRepository.save(incident); } catch (Exception ignored) {}
                }
            }

            if (slaDao != null) {
                slaDao.updateRecord(record);
            } else {
                slaRepository.save(record);
            }

            publishSlaEvents(record, previousStatus, previousEscalation);
            evaluatedCount++;
        }

        return evaluatedCount;
    }

    @Override
    public SLAResponse recordFirstResponse(UUID incidentId, Instant respondedAt) {
        Instant ts = respondedAt != null ? respondedAt : now();
        if (slaDao != null) {
            slaDao.recordFirstResponse(incidentId, ts);
        }

        SLARecord record = null;
        if (slaDao != null) {
            record = slaDao.findRecordByIncidentId(incidentId).orElse(null);
        }
        if (record == null) {
            Optional<SLARecord> opt = slaRepository.findByIncidentId(incidentId);
            if (opt.isPresent()) {
                record = opt.get();
                record.setFirstRespondedAt(ts);
                record.setAcknowledgedAt(ts);
                slaRepository.save(record);
            }
        }

        if (record != null) {
            log.info("Recorded first response for incidentId {} at {}", incidentId, ts);
            return SLAResponse.fromEntity(record);
        }
        return null;
    }

    @Override
    public SLAResponse markIncidentResolved(UUID incidentId, Instant resolvedAt) {
        Instant ts = resolvedAt != null ? resolvedAt : now();
        if (slaDao != null) {
            slaDao.recordResolution(incidentId, ts);
        }

        Optional<SLARecord> slaOpt = slaRepository.findByIncidentId(incidentId);
        SLARecord record;
        if (slaOpt.isPresent()) {
            record = slaOpt.get();
        } else if (slaDao != null) {
            record = slaDao.findRecordByIncidentId(incidentId).orElse(null);
        } else {
            return null;
        }

        if (record == null) return null;

        SLAStatus previousStatus = record.getStatus();
        EscalationLevel previousEscalation = record.getEscalationLevel();

        record.setStatus(SLAStatus.RESOLVED);
        record.setResolvedAt(ts);
        record.setEscalationLevel(EscalationLevel.NONE);
        record.setRemainingMinutes(0L);

        SLARecord saved;
        if (slaDao != null) {
            saved = slaDao.saveRecord(record);
        } else {
            saved = slaRepository.save(record);
        }

        publishSlaEvents(saved, previousStatus, previousEscalation);
        log.info("SLA marked COMPLETED for incidentId: {}", incidentId);
        return SLAResponse.fromEntity(saved);
    }

    // === OVERVIEW METRICS & ESCALATION HISTORY ===

    @Override
    @Transactional(readOnly = true)
    public SlaOverviewResponse getOverviewMetrics() {
        if (slaDao != null) {
            return slaDao.getOverviewMetrics();
        }
        // Fallback calculation using repository
        long totalActive = slaRepository.findAllActiveSLAs().size();
        long onTrack = slaRepository.countByStatus(SLAStatus.ON_TRACK);
        long warning = slaRepository.countByStatus(SLAStatus.AT_RISK);
        long breached = slaRepository.countByStatus(SLAStatus.BREACHED);
        long completed = slaRepository.countByStatus(SLAStatus.RESOLVED);
        return new SlaOverviewResponse(totalActive, onTrack, warning, breached, completed);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlaEscalationResponse> getEscalationHistory(int limit) {
        if (slaDao != null) {
            return slaDao.getEscalationHistory(limit);
        }
        return List.of();
    }

    // === HELPER METHODS ===

    private void publishSlaEvents(SLARecord record, SLAStatus previousStatus, EscalationLevel previousEscalation) {
        if (record == null) return;

        SLAStatus currentStatus = record.getStatus();
        EscalationLevel currentEscalation = record.getEscalationLevel();

        boolean statusChanged = (previousStatus == null || currentStatus != previousStatus);
        boolean escalationChanged = (previousEscalation == null || currentEscalation != previousEscalation);

        boolean shouldPublishSla = statusChanged || escalationChanged
                || currentStatus == SLAStatus.WARNING
                || currentStatus == SLAStatus.AT_RISK
                || currentStatus == SLAStatus.BREACHED
                || currentStatus == SLAStatus.COMPLETED
                || currentStatus == SLAStatus.RESOLVED;

        if (shouldPublishSla && webSocketEventPublisher != null) {
            SLAEvent event = new SLAEvent(
                    record.getIncident() != null && record.getIncident().getId() != null ? record.getIncident().getId().toString() : null,
                    record.getId() != null ? record.getId().toString() : null,
                    record.getPriorityTier() != null ? record.getPriorityTier().name() : null,
                    record.getDeadlineAt(),
                    record.getRemainingMinutes(),
                    currentStatus != null ? currentStatus.name() : null,
                    currentEscalation != null ? currentEscalation.name() : null,
                    now()
            );
            webSocketEventPublisher.publishSLAEvent(event);
        }

        Incident inc = record.getIncident();
        String incId = (inc != null && inc.getId() != null) ? inc.getId().toString() : null;
        Double lat = inc != null ? inc.getLatitude() : null;
        Double lon = inc != null ? inc.getLongitude() : null;
        String deptName = (inc != null && inc.getAssignedDepartment() != null) ? inc.getAssignedDepartment().getName() : null;

        if (currentStatus != null && currentStatus.isWarning() && (previousStatus == null || !previousStatus.isWarning())) {
            if (notificationService != null) {
                notificationService.notifySlaAtRisk(record, deptName);
            }
            if (eventPublisher != null && inc != null) {
                UUID asgnId = inc.getAssignedTo() != null ? inc.getAssignedTo().getId() : null;
                eventPublisher.publishEvent(new com.smartcity.notification.event.SlaWarningNotificationEvent(
                        record.getId(),
                        inc.getId(),
                        inc.getIncidentNumber(),
                        record.getRemainingMinutes() != null ? record.getRemainingMinutes() : 0L,
                        record.getDeadlineAt(),
                        asgnId,
                        deptName
                ));
            }
            if (webSocketEventPublisher != null) {
                AlertEvent alert = new AlertEvent(
                        "SLA_WARNING",
                        "HIGH",
                        "SLA Deadline Approaching",
                        "Incident " + (inc != null ? inc.getIncidentNumber() : "") + " SLA deadline approaching (" + record.getRemainingMinutes() + " mins remaining)",
                        incId,
                        lat,
                        lon
                );
                webSocketEventPublisher.publishAlert(alert);
            }
        } else if (currentStatus == SLAStatus.BREACHED && (previousStatus == null || previousStatus != SLAStatus.BREACHED)) {
            if (notificationService != null) {
                notificationService.notifySlaBreached(record, deptName);
            }
            if (eventPublisher != null && inc != null) {
                UUID asgnId = inc.getAssignedTo() != null ? inc.getAssignedTo().getId() : null;
                eventPublisher.publishEvent(new com.smartcity.notification.event.SlaBreachedNotificationEvent(
                        record.getId(),
                        inc.getId(),
                        inc.getIncidentNumber(),
                        record.getBreachedAt() != null ? record.getBreachedAt() : now(),
                        asgnId,
                        deptName
                ));
            }
            if (webSocketEventPublisher != null) {
                AlertEvent alert = new AlertEvent(
                        "SLA_BREACHED",
                        "CRITICAL",
                        "SLA Breached",
                        "Incident " + (inc != null ? inc.getIncidentNumber() : "") + " SLA target has breached",
                        incId,
                        lat,
                        lon
                );
                webSocketEventPublisher.publishAlert(alert);
            }
        }

        if (escalationChanged && currentEscalation != null && currentEscalation != EscalationLevel.NONE) {
            if (currentEscalation == EscalationLevel.LEVEL_3 && notificationService != null) {
                notificationService.notifyLevel3Escalation(record, "Incident SLA reached LEVEL_3 critical escalation");
            }
            if (eventPublisher != null && inc != null) {
                UUID asgnId = inc.getAssignedTo() != null ? inc.getAssignedTo().getId() : null;
                eventPublisher.publishEvent(new com.smartcity.notification.event.IncidentEscalatedNotificationEvent(
                        inc.getId(),
                        inc.getIncidentNumber(),
                        currentEscalation.name(),
                        "SLA automatic escalation progression",
                        asgnId,
                        deptName
                ));
            }
            if (webSocketEventPublisher != null) {
                AlertEvent alert = new AlertEvent(
                        "ESCALATION",
                        currentEscalation == EscalationLevel.LEVEL_3 ? "CRITICAL" : "HIGH",
                        "SLA Escalation: " + currentEscalation.name(),
                        "Incident " + (inc != null ? inc.getIncidentNumber() : "") + " escalated to " + currentEscalation.name(),
                        incId,
                        lat,
                        lon
                );
                webSocketEventPublisher.publishAlert(alert);
            }

            if (auditService != null && record.getId() != null) {
                String details = String.format("{\"incidentNumber\":\"%s\",\"escalationLevel\":\"%s\"}",
                        inc != null ? inc.getIncidentNumber() : "N/A", currentEscalation.name());
                auditService.record("SLA", record.getId().toString(), "SLA_ESCALATION", "SYSTEM/SLA_SCHEDULER", details, "127.0.0.1");
            }
        }
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
            // Try by incident number
        }

        return incidentRepository.findByIncidentNumber(identifier.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with identifier: " + identifier));
    }

    private PriorityTier resolvePriorityTier(Incident incident) {
        if (incident.getTier() != null) {
            return incident.getTier();
        }
        if (incident.getPriority() != null) {
            return PriorityTier.fromString(incident.getPriority().name());
        }
        if (incident.getAssignedTier() != null) {
            return PriorityTier.fromString(incident.getAssignedTier().name());
        }
        if (incident.getSeverityIndicator() != null && !incident.getSeverityIndicator().isBlank()) {
            try {
                return PriorityTier.fromString(incident.getSeverityIndicator().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }
}
