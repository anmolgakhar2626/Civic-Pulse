package com.smartcity.assignment;

import com.smartcity.assignment.dto.AssignIncidentRequest;
import com.smartcity.assignment.dto.AssignmentHistoryResponse;
import com.smartcity.common.ResourceNotFoundException;
import com.smartcity.department.Department;
import com.smartcity.department.DepartmentRepository;
import com.smartcity.department.Team;
import com.smartcity.department.TeamRepository;
import com.smartcity.incident.Incident;
import com.smartcity.incident.IncidentRepository;
import com.smartcity.incident.IncidentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssignmentServiceImpl implements AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentServiceImpl.class);

    private final IncidentRepository incidentRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final AssignmentRuleRepository assignmentRuleRepository;
    private final IncidentAssignmentHistoryRepository historyRepository;

    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.smartcity.websocket.WebSocketEventPublisher webSocketEventPublisher;

    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.smartcity.common.AuditService auditService;

    public AssignmentServiceImpl(
            IncidentRepository incidentRepository,
            DepartmentRepository departmentRepository,
            TeamRepository teamRepository,
            AssignmentRuleRepository assignmentRuleRepository,
            IncidentAssignmentHistoryRepository historyRepository
    ) {
        this.incidentRepository = incidentRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.assignmentRuleRepository = assignmentRuleRepository;
        this.historyRepository = historyRepository;
    }

    @Override
    public AssignmentResult autoAssign(String incidentIdentifier) {
        Incident incident = resolveIncident(incidentIdentifier);
        AssignmentRule rule = findMatchingRule(incident.getIncidentType())
                .orElseThrow(() -> new IllegalStateException("No suitable department found for incident type: " + incident.getIncidentType()));

        Department department = departmentRepository.findByCode(rule.getDepartmentCode())
                .orElseGet(() -> departmentRepository.findByNameIgnoreCase(rule.getDepartmentName())
                        .orElseThrow(() -> new IllegalStateException("Configured department '" + rule.getDepartmentName() + "' not found in database")));

        if (!department.isActive()) {
            throw new IllegalStateException("Configured department '" + department.getName() + "' is inactive");
        }

        Team team = resolveActiveTeam(department, rule.getDefaultTeamCode());

        Department prevDept = incident.getAssignedDepartment();
        Team prevTeam = incident.getAssignedTeam();

        String assignedBy = "AUTO_ASSIGNER";
        String reason = rule.getDescription() != null && !rule.getDescription().isBlank()
                ? rule.getDescription()
                : "Auto-assigned based on rule for " + rule.getIncidentType();

        incident.setAssignedDepartment(department);
        incident.setAssignedTeam(team);
        incident.setAssignedAt(Instant.now());
        incident.setAssignedBy(assignedBy);
        incident.setAssignmentReason(reason);

        // Advance status to ASSIGNED if previously DETECTED, CLASSIFIED, or PRIORITIZED
        if (incident.getStatus() == IncidentStatus.DETECTED ||
            incident.getStatus() == IncidentStatus.CLASSIFIED ||
            incident.getStatus() == IncidentStatus.PRIORITIZED) {
            incident.setStatus(IncidentStatus.ASSIGNED);
        }

        incidentRepository.save(incident);

        // Record auditable history
        IncidentAssignmentHistory history = new IncidentAssignmentHistory(
                incident, assignedBy, prevDept, department, prevTeam, team, reason
        );
        historyRepository.save(history);

        log.info("Auto-assigned incident {} to department '{}' and team '{}' [reason: {}]",
                incident.getIncidentNumber(), department.getName(), team != null ? team.getName() : "None", reason);

        AssignmentResult result = new AssignmentResult(
                incident.getIncidentNumber() != null ? incident.getIncidentNumber() : incident.getId().toString(),
                department.getName(),
                team != null ? team.getName() : null,
                reason
        );
        result.setIncidentUuid(incident.getId());
        result.setDepartmentId(department.getId());
        result.setDepartmentCode(department.getCode());
        if (team != null) {
            result.setTeamId(team.getId());
            result.setTeamCode(team.getCode());
        }
        result.setAssignedBy(assignedBy);
        result.setAssignedAt(incident.getAssignedAt());

        publishAssignmentEvents(incident, department, team, assignedBy, incident.getAssignedAt(), reason);

        return result;
    }

    @Override
    public AssignmentResult assign(String incidentIdentifier, AssignIncidentRequest request) {
        if (request == null || request.getDepartmentId() == null) {
            // If no specific department requested in manual assign payload, delegate to automatic rule workflow
            return autoAssign(incidentIdentifier);
        }

        Incident incident = resolveIncident(incidentIdentifier);

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));

        if (!department.isActive()) {
            throw new IllegalStateException("Cannot assign to inactive department: " + department.getName());
        }

        Team team = null;
        if (request.getTeamId() != null) {
            team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", request.getTeamId()));

            if (!team.isActive()) {
                throw new IllegalStateException("Cannot assign to inactive team: " + team.getName());
            }

            if (team.getDepartment() == null || !team.getDepartment().getId().equals(department.getId())) {
                throw new IllegalArgumentException("Team '" + team.getName() + "' does not belong to department '" + department.getName() + "'");
            }
        }

        Department prevDept = incident.getAssignedDepartment();
        Team prevTeam = incident.getAssignedTeam();

        String assignedBy = request.getAssignedBy() != null && !request.getAssignedBy().isBlank()
                ? request.getAssignedBy().trim()
                : "OPERATOR";
        String reason = request.getReason() != null && !request.getReason().isBlank()
                ? request.getReason().trim()
                : "Manually assigned to " + department.getName();

        incident.setAssignedDepartment(department);
        incident.setAssignedTeam(team);
        incident.setAssignedAt(Instant.now());
        incident.setAssignedBy(assignedBy);
        incident.setAssignmentReason(reason);

        if (incident.getStatus() == IncidentStatus.DETECTED ||
            incident.getStatus() == IncidentStatus.CLASSIFIED ||
            incident.getStatus() == IncidentStatus.PRIORITIZED) {
            incident.setStatus(IncidentStatus.ASSIGNED);
        }

        incidentRepository.save(incident);

        // Record history
        IncidentAssignmentHistory history = new IncidentAssignmentHistory(
                incident, assignedBy, prevDept, department, prevTeam, team, reason
        );
        historyRepository.save(history);

        log.info("Manually assigned incident {} to department '{}' by '{}' [reason: {}]",
                incident.getIncidentNumber(), department.getName(), assignedBy, reason);

        AssignmentResult result = new AssignmentResult(
                incident.getIncidentNumber() != null ? incident.getIncidentNumber() : incident.getId().toString(),
                department.getName(),
                team != null ? team.getName() : null,
                reason
        );
        result.setIncidentUuid(incident.getId());
        result.setDepartmentId(department.getId());
        result.setDepartmentCode(department.getCode());
        if (team != null) {
            result.setTeamId(team.getId());
            result.setTeamCode(team.getCode());
        }
        result.setAssignedBy(assignedBy);
        result.setAssignedAt(incident.getAssignedAt());

        publishAssignmentEvents(incident, department, team, assignedBy, incident.getAssignedAt(), reason);

        return result;
    }

    private void publishAssignmentEvents(Incident incident, Department department, Team team, String assignedBy, Instant assignedAt, String reason) {
        if (webSocketEventPublisher != null && incident != null) {
            webSocketEventPublisher.publishAssignmentEvent(new com.smartcity.websocket.dto.AssignmentEvent(
                    incident.getId() != null ? incident.getId().toString() : null,
                    department != null ? department.getName() : null,
                    team != null ? team.getName() : null,
                    assignedBy,
                    assignedAt != null ? assignedAt : Instant.now(),
                    reason
            ));

            webSocketEventPublisher.publishIncidentEvent(new com.smartcity.websocket.dto.IncidentEvent(
                    incident.getId() != null ? incident.getId().toString() : null,
                    incident.getIncidentNumber(),
                    incident.getIncidentType(),
                    incident.getDescription(),
                    incident.getPriorityTier() != null ? incident.getPriorityTier().name() : null,
                    incident.getPriorityLabel(),
                    incident.getStatus() != null ? incident.getStatus().name() : null,
                    incident.getLatitude(),
                    incident.getLongitude(),
                    department != null ? department.getName() : null,
                    Instant.now(),
                    "ASSIGNED"
            ));
        }

        if (auditService != null && incident.getId() != null) {
            String details = String.format("{\"department\":\"%s\",\"departmentCode\":\"%s\",\"team\":\"%s\",\"reason\":\"%s\"}",
                    department != null ? department.getName() : "None",
                    department != null ? department.getCode() : "None",
                    team != null ? team.getName() : "None",
                    reason != null ? reason : "Assigned");
            auditService.record("INCIDENT", incident.getId().toString(), "INCIDENT_ASSIGNED", assignedBy != null ? assignedBy : "SYSTEM", details, "127.0.0.1");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentHistoryResponse> getAssignmentHistory(String incidentIdentifier) {
        Incident incident = resolveIncident(incidentIdentifier);
        List<IncidentAssignmentHistory> history = historyRepository.findByIncidentIdOrderByAssignedAtDesc(incident.getId());
        return history.stream().map(AssignmentHistoryResponse::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssignmentRule> findMatchingRule(String incidentType) {
        if (incidentType == null || incidentType.isBlank()) {
            return Optional.empty();
        }

        String raw = incidentType.trim().toUpperCase();

        // 1. Direct exact match
        Optional<AssignmentRule> directMatch = assignmentRuleRepository.findByIncidentTypeIgnoreCaseAndActiveTrue(raw);
        if (directMatch.isPresent()) {
            return directMatch;
        }

        List<AssignmentRule> activeRules = assignmentRuleRepository.findByActiveTrue();

        // 2. Exact token equality after normalizing non-alphanumeric chars
        String normalized = raw.replaceAll("[^A-Z0-9]", "_");
        for (AssignmentRule rule : activeRules) {
            String ruleNorm = rule.getIncidentType().trim().toUpperCase().replaceAll("[^A-Z0-9]", "_");
            if (ruleNorm.equals(normalized)) {
                return Optional.of(rule);
            }
        }

        // 3. Keyword / semantic fallback match against canonical rules
        for (AssignmentRule rule : activeRules) {
            String rType = rule.getIncidentType().toUpperCase();

            if (raw.contains("ACCIDENT") || raw.contains("COLLISION")) {
                if (rType.equals("MAJOR_ROAD_ACCIDENT")) return Optional.of(rule);
            } else if (raw.contains("ELECTRICAL") || raw.contains("WIRE") || raw.contains("TRANSFORMER")) {
                if (rType.equals("ELECTRICAL_HAZARD")) return Optional.of(rule);
            } else if (raw.contains("MANHOLE")) {
                if (rType.equals("OPEN_MANHOLE")) return Optional.of(rule);
            } else if (raw.contains("FLOOD") || raw.contains("SUBMERGED")) {
                if (rType.equals("FLASH_FLOOD")) return Optional.of(rule);
            } else if (raw.contains("SIGNAL") || raw.contains("TRAFFIC")) {
                if (rType.equals("TRAFFIC_SIGNAL_FAILURE")) return Optional.of(rule);
            } else if (raw.contains("PIPELINE") || raw.contains("PIPE_BURST")) {
                if (rType.equals("PIPELINE_BURST")) return Optional.of(rule);
            } else if (raw.contains("CONTAMINATION") || raw.contains("POISONED_WATER")) {
                if (rType.equals("WATER_CONTAMINATION")) return Optional.of(rule);
            } else if (raw.contains("CAVE_IN") || raw.contains("SINKHOLE")) {
                if (rType.equals("ROAD_CAVE_IN")) return Optional.of(rule);
            } else if (raw.contains("GAS") || raw.contains("LEAK")) {
                if (rType.equals("GAS_LEAK")) return Optional.of(rule);
            } else if (raw.contains("SMOG") || raw.contains("AIR_QUALITY") || raw.contains("PM25") || raw.contains("POLLUTION") || raw.contains("AIR")) {
                if (rType.equals("AIR_QUALITY_HAZARD")) return Optional.of(rule);
            } else if (raw.contains("WASTE") || raw.contains("BIN") || raw.contains("OVERFLOW")) {
                if (rType.equals("WASTE_OVERFLOW")) return Optional.of(rule);
            } else if (raw.contains("DUMP")) {
                if (rType.equals("ILLEGAL_DUMPING")) return Optional.of(rule);
            } else if (raw.contains("STREETLIGHT") || raw.contains("DARK_STREET")) {
                if (rType.equals("STREETLIGHT_FAILURE")) return Optional.of(rule);
            } else if (raw.contains("DRAIN")) {
                if (rType.equals("BLOCKED_STORM_DRAIN")) return Optional.of(rule);
            } else if (raw.contains("CRACK") || raw.contains("POTHOLE") || raw.contains("ROAD_DAMAGE")) {
                if (rType.equals("ROAD_CRACK")) return Optional.of(rule);
            } else if (raw.contains("SIDEWALK") || raw.contains("FOOTPATH")) {
                if (rType.equals("SIDEWALK_DAMAGE")) return Optional.of(rule);
            } else if (raw.contains("TREE") || raw.contains("PRUNING") || raw.contains("BRANCH")) {
                if (rType.equals("TREE_PRUNING")) return Optional.of(rule);
            } else if (raw.contains("GRAFFITI") || raw.contains("VANDALISM")) {
                if (rType.equals("GRAFFITI")) return Optional.of(rule);
            }
        }

        return Optional.empty();
    }

    private Team resolveActiveTeam(Department department, String defaultTeamCode) {
        if (defaultTeamCode != null && !defaultTeamCode.isBlank()) {
            Optional<Team> byCode = teamRepository.findByCode(defaultTeamCode.trim());
            if (byCode.isPresent() && byCode.get().isActive()) {
                return byCode.get();
            }
        }

        List<Team> activeTeams = teamRepository.findByDepartmentIdAndActiveTrue(department.getId());
        if (!activeTeams.isEmpty()) {
            return activeTeams.get(0);
        }

        return null;
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
