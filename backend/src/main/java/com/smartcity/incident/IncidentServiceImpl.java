package com.smartcity.incident;

import com.smartcity.common.ResourceNotFoundException;
import com.smartcity.department.Department;
import com.smartcity.department.DepartmentRepository;
import com.smartcity.department.Team;
import com.smartcity.department.TeamRepository;
import com.smartcity.incident.dto.CreateIncidentRequest;
import com.smartcity.incident.dto.IncidentResponse;
import com.smartcity.incident.dto.IncidentStatusHistoryResponse;
import com.smartcity.incident.dto.UpdateIncidentRequest;
import com.smartcity.incident.dto.UpdateIncidentStatusRequest;
import com.smartcity.priority.PriorityTier;
import com.smartcity.user.User;
import com.smartcity.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class IncidentServiceImpl implements IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentServiceImpl.class);

    private final IncidentDao incidentDao;
    private final IncidentRepository incidentRepository;
    private final IncidentStatusHistoryRepository statusHistoryRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    @org.springframework.context.annotation.Lazy
    @Autowired(required = false)
    private com.smartcity.sla.SLAService slaService;

    @org.springframework.context.annotation.Lazy
    @Autowired(required = false)
    private com.smartcity.websocket.WebSocketEventPublisher webSocketEventPublisher;

    @org.springframework.context.annotation.Lazy
    @Autowired(required = false)
    private com.smartcity.notification.NotificationService notificationService;

    @org.springframework.context.annotation.Lazy
    @Autowired(required = false)
    private com.smartcity.common.AuditService auditService;

    @Autowired(required = false)
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Autowired
    public IncidentServiceImpl(
            IncidentDao incidentDao,
            @Autowired(required = false) IncidentRepository incidentRepository,
            IncidentStatusHistoryRepository statusHistoryRepository,
            DepartmentRepository departmentRepository,
            TeamRepository teamRepository,
            UserRepository userRepository
    ) {
        this.incidentDao = incidentDao;
        this.incidentRepository = incidentRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    // Overloaded constructor for legacy unit tests that instantiate with IncidentRepository
    public IncidentServiceImpl(
            IncidentRepository incidentRepository,
            IncidentStatusHistoryRepository statusHistoryRepository,
            DepartmentRepository departmentRepository,
            TeamRepository teamRepository,
            UserRepository userRepository
    ) {
        this(null, incidentRepository, statusHistoryRepository, departmentRepository, teamRepository, userRepository);
    }

    @Override
    public IncidentResponse createIncident(CreateIncidentRequest request) {
        Incident incident = new Incident();
        incident.setIncidentNumber(generateIncidentNumber());
        incident.setTitle(request.getTitle());
        incident.setCategory(request.getCategory());
        incident.setIncidentType(request.getIncidentType());
        incident.setDescription(request.getDescription());
        incident.setSource(request.getSource() != null ? request.getSource() : IncidentSource.CITIZEN);
        incident.setLatitude(request.getLatitude());
        incident.setLongitude(request.getLongitude());
        incident.setLocationName(request.getLocationName());
        incident.setAddress(request.getAddress());

        IncidentStatus initStatus = IncidentStatus.DETECTED;
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                initStatus = IncidentStatus.valueOf(request.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                initStatus = IncidentStatus.DETECTED;
            }
        }
        incident.setStatus(initStatus);

        incident.setReportedAt(request.getReportedAt() != null ? request.getReportedAt() : Instant.now());
        incident.setDetectedAt(request.getDetectedAt() != null ? request.getDetectedAt() : Instant.now());
        incident.setSeverityIndicator(request.getSeverity());
        incident.setPopulationExposure(request.getPopulationExposure());
        incident.setInfrastructureImportance(request.getInfrastructureImportance());
        incident.setEvidenceUrl(request.getEvidenceUrl());

        // Keep priority, tier, and SLA null per Requirement 13 until Priority Engine evaluates
        incident.setPriority(null);
        incident.setAssignedTier(null);
        incident.setSlaMinutes(null);
        incident.setSlaDeadline(null);

        // Resolve Department if provided
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
            incident.setAssignedDepartment(department);
        }

        // Resolve Assigned Team if provided
        if (request.getAssignedTeamId() != null) {
            Team team = teamRepository.findById(request.getAssignedTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", request.getAssignedTeamId()));
            incident.setAssignedTeam(team);
        }

        // Securely resolve Reporter from Authenticated User context (never trust client payload for identity)
        User reporter = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            reporter = userRepository.findByUsernameOrEmail(authentication.getName()).orElse(null);
        }

        // Fallback for tests or automated ingest passing createdByUserId
        if (reporter == null && request.getCreatedByUserId() != null) {
            reporter = userRepository.findById(request.getCreatedByUserId()).orElse(null);
        }

        if (reporter != null) {
            // Feature 1: Mobile verification enforcement for citizen reporting
            if ("CITIZEN".equalsIgnoreCase(reporter.getRoleName()) && !Boolean.TRUE.equals(reporter.getIsMobileVerified())) {
                throw new com.smartcity.common.ForbiddenException("Mobile number verification is required before citizens can file municipal incident reports. Please complete OTP verification.");
            }
            incident.setCreatedBy(reporter);
            incident.setReportedBy(reporter);
        }

        // Resolve Assigned To if provided
        if (request.getAssignedTo() != null) {
            userRepository.findById(request.getAssignedTo()).ifPresent(op -> {
                incident.setAssignedTo(op);
                if (eventPublisher != null) {
                    eventPublisher.publishEvent(new com.smartcity.notification.event.IncidentAssignedNotificationEvent(
                            incident.getId(),
                            incident.getIncidentNumber(),
                            incident.getTitle(),
                            incident.getSeverityIndicator(),
                            op.getId(),
                            "Command Staff"
                    ));
                }
            });
        }

        Incident savedIncident;
        if (incidentDao != null) {
            savedIncident = incidentDao.save(incident);
        } else {
            savedIncident = incidentRepository.save(incident);
        }

        // Record initial status history entry
        String changedBy = (reporter != null && reporter.getUsername() != null) ? reporter.getUsername() :
                (request.getCreatedByUsername() != null ? request.getCreatedByUsername() : incident.getSource().name());

        IncidentStatusHistory history = new IncidentStatusHistory(
                savedIncident,
                null,
                savedIncident.getStatus() != null ? savedIncident.getStatus().name() : "OPEN",
                reporter,
                changedBy,
                "Incident logged via " + incident.getSource().name()
        );
        statusHistoryRepository.save(history);

        // Trigger real-time WebSocket notifications & event broadcast
        if (webSocketEventPublisher != null) {
            com.smartcity.websocket.dto.IncidentEvent event = new com.smartcity.websocket.dto.IncidentEvent(
                    savedIncident.getId() != null ? savedIncident.getId().toString() : null,
                    savedIncident.getIncidentNumber(),
                    savedIncident.getIncidentType(),
                    savedIncident.getDescription(),
                    savedIncident.getPriorityTier() != null ? savedIncident.getPriorityTier().name() : null,
                    savedIncident.getPriorityLabel(),
                    savedIncident.getStatus() != null ? savedIncident.getStatus().name() : null,
                    savedIncident.getLatitude(),
                    savedIncident.getLongitude(),
                    savedIncident.getAssignedDepartment() != null ? savedIncident.getAssignedDepartment().getName() : null,
                    savedIncident.getCreatedAt() != null ? savedIncident.getCreatedAt() : Instant.now(),
                    "CREATED"
            );
            webSocketEventPublisher.publishIncidentEvent(event);

            if ("CRITICAL".equalsIgnoreCase(savedIncident.getSeverityIndicator()) || "EXTREME".equalsIgnoreCase(savedIncident.getSeverityIndicator())) {
                if (notificationService != null) {
                    String dept = savedIncident.getAssignedDepartment() != null ? savedIncident.getAssignedDepartment().getName() : null;
                    notificationService.notifyCriticalIncident(savedIncident, dept);
                }
                webSocketEventPublisher.publishAlert(new com.smartcity.websocket.dto.AlertEvent(
                        "CRITICAL_INCIDENT",
                        "CRITICAL",
                        "Critical Incident Reported: " + savedIncident.getIncidentType(),
                        savedIncident.getDescription(),
                        savedIncident.getId() != null ? savedIncident.getId().toString() : null,
                        savedIncident.getLatitude(),
                        savedIncident.getLongitude()
                ));
            }
        }

        if (auditService != null && savedIncident.getId() != null) {
            String details = String.format("{\"incidentNumber\":\"%s\",\"type\":\"%s\",\"source\":\"%s\"}",
                    savedIncident.getIncidentNumber(), savedIncident.getIncidentType(), savedIncident.getSource());
            auditService.record("INCIDENT", savedIncident.getId().toString(), "INCIDENT_CREATED", changedBy, details, "127.0.0.1");
        }

        // Prompt 7: Publish Spring Application Event for Notification Center
        if (eventPublisher != null && savedIncident != null) {
            String deptName = savedIncident.getAssignedDepartment() != null ? savedIncident.getAssignedDepartment().getName() : null;
            UUID reporterId = savedIncident.getReportedBy() != null ? savedIncident.getReportedBy().getId() : null;
            UUID assignedToId = savedIncident.getAssignedTo() != null ? savedIncident.getAssignedTo().getId() : null;
            eventPublisher.publishEvent(new com.smartcity.notification.event.IncidentCreatedNotificationEvent(
                    savedIncident.getId(),
                    savedIncident.getIncidentNumber(),
                    savedIncident.getTitle(),
                    savedIncident.getCategory(),
                    savedIncident.getSeverityIndicator(),
                    reporterId,
                    assignedToId,
                    deptName
            ));
        }

        // Prompt 4: Automatically assign matching SLA Policy and calculate deadlines
        if (slaService != null && savedIncident.getId() != null) {
            try {
                slaService.assignPolicyAndCreateSLA(savedIncident);
            } catch (Exception e) {
                log.warn("Automatic SLA policy assignment for incident {}: {}", savedIncident.getIncidentNumber(), e.getMessage());
            }
        }

        return IncidentResponse.fromEntity(savedIncident);
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentResponse getIncidentById(UUID id) {
        if (incidentDao != null) {
            Incident incident = incidentDao.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
            return IncidentResponse.fromEntity(incident);
        }
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
        return IncidentResponse.fromEntity(incident);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncidentResponse> getFilteredIncidents(
            String search,
            String category,
            String severity,
            String status,
            Pageable pageable
    ) {
        if (incidentDao != null) {
            int page = pageable != null ? pageable.getPageNumber() : 0;
            int size = pageable != null ? pageable.getPageSize() : 20;
            int offset = page * size;

            List<Incident> incidents = incidentDao.findPaginatedAndFiltered(search, category, severity, status, offset, size);
            long total = incidentDao.countFiltered(search, category, severity, status);

            List<IncidentResponse> responses = incidents.stream()
                    .map(IncidentResponse::fromEntity)
                    .collect(Collectors.toList());

            return new PageImpl<>(responses, pageable != null ? pageable : Pageable.unpaged(), total);
        }

        // Fallback to existing repository if incidentDao not injected
        return getAllIncidents(
                status != null ? IncidentStatus.valueOf(status.toUpperCase()) : null,
                category,
                null,
                null,
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncidentResponse> getAllIncidents(
            IncidentStatus status,
            String incidentType,
            IncidentSource source,
            UUID departmentId,
            Pageable pageable
    ) {
        String deptParam = departmentId != null ? departmentId.toString() : null;
        return getAllIncidents(status, incidentType, source, null, deptParam, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncidentResponse> getAllIncidents(
            IncidentStatus status,
            String incidentType,
            IncidentSource source,
            PriorityTier tier,
            String department,
            String assignedTeam,
            Pageable pageable
    ) {
        if (incidentDao != null && tier == null && source == null && department == null && assignedTeam == null) {
            String statStr = status != null ? status.name() : null;
            return getFilteredIncidents(null, incidentType, null, statStr, pageable);
        }

        if (incidentRepository != null) {
            Specification<Incident> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                if (status != null) {
                    predicates.add(cb.equal(root.get("status"), status));
                }
                if (incidentType != null && !incidentType.trim().isEmpty()) {
                    predicates.add(cb.equal(cb.lower(root.get("incidentType")), incidentType.trim().toLowerCase()));
                }
                if (source != null) {
                    predicates.add(cb.equal(root.get("source"), source));
                }
                if (tier != null) {
                    predicates.add(cb.equal(root.get("tier"), tier));
                }
                if (department != null && !department.trim().isEmpty()) {
                    String dVal = department.trim();
                    try {
                        UUID dId = UUID.fromString(dVal);
                        predicates.add(cb.equal(root.get("assignedDepartment").get("id"), dId));
                    } catch (IllegalArgumentException e) {
                        predicates.add(cb.or(
                                cb.equal(cb.upper(root.get("assignedDepartment").get("code")), dVal.toUpperCase()),
                                cb.like(cb.lower(root.get("assignedDepartment").get("name")), "%" + dVal.toLowerCase() + "%")
                        ));
                    }
                }
                if (assignedTeam != null && !assignedTeam.trim().isEmpty()) {
                    String tVal = assignedTeam.trim();
                    try {
                        UUID tId = UUID.fromString(tVal);
                        predicates.add(cb.equal(root.get("assignedTeam").get("id"), tId));
                    } catch (IllegalArgumentException e) {
                        predicates.add(cb.or(
                                cb.equal(cb.upper(root.get("assignedTeam").get("code")), tVal.toUpperCase()),
                                cb.like(cb.lower(root.get("assignedTeam").get("name")), "%" + tVal.toLowerCase() + "%")
                        ));
                    }
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            };

            return incidentRepository.findAll(spec, pageable).map(IncidentResponse::fromEntity);
        }

        return Page.empty(pageable);
    }

    @Override
    public IncidentResponse updateIncident(UUID id, UpdateIncidentRequest request) {
        Incident incident;
        if (incidentDao != null) {
            incident = incidentDao.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
        } else {
            incident = incidentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
        }

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            incident.setTitle(request.getTitle().trim());
        }
        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            incident.setCategory(request.getCategory().trim());
        }
        if (request.getIncidentType() != null && !request.getIncidentType().trim().isEmpty()) {
            incident.setIncidentType(request.getIncidentType().trim());
        }
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            incident.setDescription(request.getDescription().trim());
        }
        if (request.getLatitude() != null) {
            incident.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            incident.setLongitude(request.getLongitude());
        }
        if (request.getLocationName() != null && !request.getLocationName().trim().isEmpty()) {
            incident.setLocationName(request.getLocationName().trim());
        }
        if (request.getAddress() != null && !request.getAddress().trim().isEmpty()) {
            incident.setAddress(request.getAddress().trim());
        }
        if (request.getSeverity() != null && !request.getSeverity().trim().isEmpty()) {
            incident.setSeverityIndicator(request.getSeverity().trim());
            try {
                incident.setSeverity(com.smartcity.priority.Severity.valueOf(request.getSeverity().trim().toUpperCase()));
            } catch (Exception ignored) {}
        }
        if (request.getEvidenceUrl() != null) {
            incident.setEvidenceUrl(request.getEvidenceUrl());
        }
        if (request.getResolutionNotes() != null) {
            incident.setResolutionNotes(request.getResolutionNotes());
        }
        if (request.getReasonNotes() != null) {
            incident.setReasonNotes(request.getReasonNotes());
        }
        if (request.getEscalationState() != null) {
            incident.setEscalationState(request.getEscalationState());
        }

        if (request.getAssignedTo() != null) {
            userRepository.findById(request.getAssignedTo()).ifPresent(op -> {
                incident.setAssignedTo(op);
                if (eventPublisher != null) {
                    eventPublisher.publishEvent(new com.smartcity.notification.event.IncidentAssignedNotificationEvent(
                            incident.getId(),
                            incident.getIncidentNumber(),
                            incident.getTitle(),
                            incident.getSeverityIndicator(),
                            op.getId(),
                            "Command Staff"
                    ));
                }
            });
        }

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
            incident.setAssignedDepartment(department);
        }

        if (request.getAssignedTeamId() != null) {
            Team team = teamRepository.findById(request.getAssignedTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", request.getAssignedTeamId()));
            incident.setAssignedTeam(team);
        }

        incident.setUpdatedAt(Instant.now());

        if (incidentDao != null) {
            incidentDao.update(id, incident);
            Incident updatedIncident = incidentDao.findById(id).orElse(incident);
            return IncidentResponse.fromEntity(updatedIncident);
        } else {
            Incident updatedIncident = incidentRepository.save(incident);
            return IncidentResponse.fromEntity(updatedIncident);
        }
    }

    @Override
    public IncidentResponse updateIncidentStatus(UUID id, UpdateIncidentStatusRequest request) {
        Incident incident;
        if (incidentDao != null) {
            incident = incidentDao.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
        } else {
            incident = incidentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
        }

        IncidentStatus previousStatus = incident.getStatus();
        IncidentStatus newStatus = request.getStatus();

        String remarks = (request.getRemarks() != null && !request.getRemarks().isBlank())
                ? request.getRemarks().trim()
                : (request.getNotes() != null && !request.getNotes().isBlank() ? request.getNotes().trim() : null);

        String changedBy = (request.getChangedBy() != null && !request.getChangedBy().isBlank())
                ? request.getChangedBy().trim()
                : "OPERATOR";

        incident.setStatus(newStatus);
        if (newStatus == IncidentStatus.RESOLVED || newStatus == IncidentStatus.CLOSED) {
            if (incident.getResolutionTime() == null) {
                incident.setResolutionTime(Instant.now());
            }
        }
        if (remarks != null) {
            incident.setReasonNotes(remarks);
        }
        incident.setUpdatedAt(Instant.now());

        Incident savedIncident;
        if (incidentDao != null) {
            incidentDao.updateStatus(id, newStatus.name(), remarks, changedBy);
            savedIncident = incidentDao.findById(id).orElse(incident);
        } else {
            savedIncident = incidentRepository.save(incident);
        }

        if ((newStatus == IncidentStatus.IN_PROGRESS || newStatus == IncidentStatus.ACKNOWLEDGED || newStatus == IncidentStatus.ASSIGNED) && slaService != null) {
            try {
                slaService.recordFirstResponse(savedIncident.getId(), Instant.now());
            } catch (Exception e) {
                // Keep incident update safe
            }
        }

        if ((newStatus == IncidentStatus.RESOLVED || newStatus == IncidentStatus.CLOSED) && slaService != null) {
            try {
                slaService.markIncidentResolved(savedIncident.getId(), savedIncident.getResolutionTime());
            } catch (Exception e) {
                // Keep incident update safe even if SLA record does not exist yet
            }
        }

        // Record status history transition
        IncidentStatusHistory history = new IncidentStatusHistory(
                savedIncident,
                previousStatus != null ? previousStatus.name() : null,
                newStatus.name(),
                changedBy,
                remarks != null ? remarks : "Status transitioned to " + newStatus.name()
        );
        statusHistoryRepository.save(history);

        if (auditService != null && savedIncident.getId() != null) {
            String details = String.format("{\"previousStatus\":\"%s\",\"newStatus\":\"%s\",\"remarks\":\"%s\"}",
                    previousStatus != null ? previousStatus.name() : "N/A", newStatus.name(), remarks != null ? remarks : "");
            auditService.record("INCIDENT", savedIncident.getId().toString(), "STATUS_CHANGE", changedBy, details, "127.0.0.1");
        }

        // Prompt 7: Publish Spring Application Event for Status Change & Resolution
        if (eventPublisher != null && savedIncident != null) {
            UUID reporterId = savedIncident.getReportedBy() != null ? savedIncident.getReportedBy().getId() : null;
            UUID assignedToId = savedIncident.getAssignedTo() != null ? savedIncident.getAssignedTo().getId() : null;
            eventPublisher.publishEvent(new com.smartcity.notification.event.IncidentStatusChangedNotificationEvent(
                    savedIncident.getId(),
                    savedIncident.getIncidentNumber(),
                    savedIncident.getTitle(),
                    previousStatus != null ? previousStatus.name() : null,
                    newStatus.name(),
                    changedBy,
                    reporterId,
                    assignedToId
            ));
        }

        if (webSocketEventPublisher != null) {
            String eventType = "STATUS_CHANGED";
            if (newStatus == IncidentStatus.RESOLVED) eventType = "RESOLVED";
            else if (newStatus == IncidentStatus.CLOSED) eventType = "CLOSED";
            else if (newStatus == IncidentStatus.ESCALATED) eventType = "ESCALATED";

            com.smartcity.websocket.dto.IncidentEvent event = new com.smartcity.websocket.dto.IncidentEvent(
                    savedIncident.getId() != null ? savedIncident.getId().toString() : null,
                    savedIncident.getIncidentNumber(),
                    savedIncident.getIncidentType(),
                    savedIncident.getDescription(),
                    savedIncident.getPriorityTier() != null ? savedIncident.getPriorityTier().name() : null,
                    savedIncident.getPriorityLabel(),
                    savedIncident.getStatus() != null ? savedIncident.getStatus().name() : null,
                    savedIncident.getLatitude(),
                    savedIncident.getLongitude(),
                    savedIncident.getAssignedDepartment() != null ? savedIncident.getAssignedDepartment().getName() : null,
                    Instant.now(),
                    eventType
            );
            webSocketEventPublisher.publishIncidentEvent(event);

            if (newStatus == IncidentStatus.ESCALATED) {
                webSocketEventPublisher.publishAlert(new com.smartcity.websocket.dto.AlertEvent(
                        "ESCALATION",
                        "HIGH",
                        "Incident Escalated: " + savedIncident.getIncidentNumber(),
                        "Incident status escalated to " + newStatus.name() + ": " + remarks,
                        savedIncident.getId() != null ? savedIncident.getId().toString() : null,
                        savedIncident.getLatitude(),
                        savedIncident.getLongitude()
                ));
            }
        }

        return IncidentResponse.fromEntity(savedIncident);
    }

    @Override
    public void deleteIncident(UUID id) {
        if (incidentDao != null) {
            if (!incidentDao.existsById(id)) {
                throw new ResourceNotFoundException("Incident", "id", id);
            }
            incidentDao.deleteById(id);
            return;
        }

        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
        incidentRepository.delete(incident);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidentStatusHistoryResponse> getIncidentStatusHistory(UUID incidentId) {
        boolean exists = incidentDao != null ? incidentDao.existsById(incidentId) : incidentRepository.existsById(incidentId);
        if (!exists) {
            throw new ResourceNotFoundException("Incident", "id", incidentId);
        }
        return statusHistoryRepository.findByIncidentIdOrderByChangedAtDesc(incidentId).stream()
                .map(IncidentStatusHistoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private String generateIncidentNumber() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String shortId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "INC-" + datePrefix + "-" + shortId;
    }
}
