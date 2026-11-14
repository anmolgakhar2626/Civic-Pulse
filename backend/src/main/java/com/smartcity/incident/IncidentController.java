package com.smartcity.incident;

import com.smartcity.common.PagedResponse;
import com.smartcity.incident.dto.CreateIncidentRequest;
import com.smartcity.incident.dto.IncidentResponse;
import com.smartcity.incident.dto.IncidentStatusHistoryResponse;
import com.smartcity.incident.dto.UpdateIncidentRequest;
import com.smartcity.incident.dto.UpdateIncidentStatusRequest;
import com.smartcity.priority.PriorityService;
import com.smartcity.priority.PriorityTier;
import com.smartcity.priority.dto.PriorityEvaluationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents")
@Tag(name = "Incidents", description = "Incident lifecycle management, reporting, updates, and query APIs")
public class IncidentController {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "createdAt", "updatedAt", "reportedAt", "detectedAt",
            "priority", "tier", "status", "source", "incidentType", "incidentNumber", "category", "severity", "title"
    );

    private final IncidentService incidentService;
    private final PriorityService priorityService;

    public IncidentController(IncidentService incidentService, PriorityService priorityService) {
        this.incidentService = incidentService;
        this.priorityService = priorityService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMAND_OPERATOR', 'CITIZEN')")
    @Operation(summary = "Create a new incident", description = "Report and ingest a new city incident from Citizen, IoT sensor, External API, or Operator.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Incident created successfully",
                    content = @Content(schema = @Schema(implementation = IncidentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure or invalid payload")
    })
    public ResponseEntity<IncidentResponse> createIncident(@Valid @RequestBody CreateIncidentRequest request) {
        IncidentResponse response = incidentService.createIncident(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List and filter incidents", description = "Retrieve a paginated, filterable, and sorted list of municipal incidents.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incidents retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter or unsafe sort parameter")
    })
    public ResponseEntity<PagedResponse<IncidentResponse>> getAllIncidents(
            @Parameter(description = "Text search across title, description, incident number, or location")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter by incident category (e.g. Flood, Fire, Traffic, Pollution, Infrastructure)")
            @RequestParam(required = false) String category,
            @Parameter(description = "Filter by severity (LOW, MEDIUM, HIGH, CRITICAL)")
            @RequestParam(required = false) String severity,
            @Parameter(description = "Filter by incident status (e.g. OPEN, IN_PROGRESS, RESOLVED, CLOSED, DETECTED)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by incident type (e.g. TRAFFIC_SIGNAL_FAILURE, FLASH_FLOOD)")
            @RequestParam(required = false) String incidentType,
            @Parameter(description = "Filter by ingestion source (CITIZEN, IOT, EXTERNAL_API, OPERATOR, SYSTEM)")
            @RequestParam(required = false) IncidentSource source,
            @Parameter(description = "Filter by priority tier (CRITICAL, HIGH, MEDIUM, LOW)")
            @RequestParam(required = false) PriorityTier tier,
            @Parameter(description = "Filter by department UUID, code, or name (e.g. TRAFFIC, PWD)")
            @RequestParam(required = false) String department,
            @Parameter(description = "Filter by assigned team UUID, code, or name")
            @RequestParam(required = false) String assignedTeam,
            @Parameter(description = "Legacy department UUID filter")
            @RequestParam(required = false) UUID departmentId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        validateSort(pageable);

        Page<IncidentResponse> page;
        if (search != null || category != null || severity != null || (status != null && !isEnumStatus(status))) {
            String cat = category != null ? category : incidentType;
            page = incidentService.getFilteredIncidents(search, cat, severity, status, pageable);
        } else if (tier != null || assignedTeam != null || (department != null && !isUuid(department))) {
            IncidentStatus statEnum = status != null ? parseStatusEnum(status) : null;
            String deptVal = department != null ? department : (departmentId != null ? departmentId.toString() : null);
            page = incidentService.getAllIncidents(statEnum, incidentType, source, tier, deptVal, assignedTeam, pageable);
        } else {
            IncidentStatus statEnum = status != null ? parseStatusEnum(status) : null;
            UUID resolvedDeptId = departmentId != null ? departmentId : (department != null ? UUID.fromString(department) : null);
            page = incidentService.getAllIncidents(statEnum, incidentType, source, resolvedDeptId, pageable);
        }

        return ResponseEntity.ok(PagedResponse.of(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get incident by ID", description = "Retrieve incident details by incident UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident found"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<IncidentResponse> getIncidentById(@PathVariable UUID id) {
        IncidentResponse response = incidentService.getIncidentById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMAND_OPERATOR')")
    @Operation(summary = "Update an incident", description = "Full or partial update of mutable incident attributes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failure"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<IncidentResponse> updateIncident(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateIncidentRequest request
    ) {
        IncidentResponse response = incidentService.updateIncident(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER', 'FIELD_WORKER')")
    @Operation(summary = "Update incident status", description = "Execute an audited lifecycle status transition for an incident.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<IncidentResponse> updateIncidentStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateIncidentStatusRequest request
    ) {
        IncidentResponse response = incidentService.updateIncidentStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/evaluate-priority")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMAND_OPERATOR')")
    @Operation(summary = "Evaluate incident priority", description = "Trigger the Priority Rule Engine to evaluate and assign a priority tier and reason.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Priority evaluated successfully"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<PriorityEvaluationResponse> evaluateIncidentPriority(@PathVariable UUID id) {
        PriorityEvaluationResponse response = priorityService.evaluateIncidentPriority(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an incident", description = "Remove an incident by its UUID. Restricted to ADMIN users.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Incident deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<Void> deleteIncident(@PathVariable UUID id) {
        incidentService.deleteIncident(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get incident status audit history", description = "Retrieve immutable audit history of all status changes for this incident.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "History retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<List<IncidentStatusHistoryResponse>> getIncidentHistory(@PathVariable UUID id) {
        List<IncidentStatusHistoryResponse> history = incidentService.getIncidentStatusHistory(id);
        return ResponseEntity.ok(history);
    }

    private void validateSort(Pageable pageable) {
        if (pageable != null && pageable.getSort().isSorted()) {
            for (Sort.Order order : pageable.getSort()) {
                if (!ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                    throw new IllegalArgumentException(
                            "Invalid sort field '" + order.getProperty() + "'. Allowed fields are: " + ALLOWED_SORT_PROPERTIES
                    );
                }
            }
        }
    }

    private boolean isUuid(String value) {
        if (value == null) return false;
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isEnumStatus(String value) {
        if (value == null) return false;
        try {
            IncidentStatus.valueOf(value.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private IncidentStatus parseStatusEnum(String value) {
        if (value == null) return null;
        try {
            return IncidentStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
