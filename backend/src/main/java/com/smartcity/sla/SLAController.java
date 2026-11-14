package com.smartcity.sla;

import com.smartcity.common.PagedResponse;
import com.smartcity.sla.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "SLA", description = "SLA policy management, target tracking, deadline monitoring, and automatic breach escalation APIs")
public class SLAController {

    private final SLAService slaService;

    public SLAController(SLAService slaService) {
        this.slaService = slaService;
    }

    // =========================================================================
    // 1. SLA POLICY MANAGEMENT ENDPOINTS (Prompt 4)
    // =========================================================================

    @GetMapping("/api/sla/policies")
    @Operation(summary = "List SLA policies", description = "Returns all configured SLA policies across incident categories and severity levels.")
    public ResponseEntity<List<SlaPolicyResponse>> getAllPolicies() {
        return ResponseEntity.ok(slaService.getAllPolicies());
    }

    @PostMapping("/api/sla/policies")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create SLA policy", description = "Creates a new SLA policy with category, severity, response time, and resolution time limits. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Policy created successfully",
                    content = @Content(schema = @Schema(implementation = SlaPolicyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Admin role required")
    })
    public ResponseEntity<SlaPolicyResponse> createPolicy(@Valid @RequestBody CreateSlaPolicyRequest request) {
        SlaPolicyResponse created = slaService.createPolicy(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/api/sla/policies/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update SLA policy", description = "Updates an existing SLA policy by ID. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Policy updated successfully",
                    content = @Content(schema = @Schema(implementation = SlaPolicyResponse.class))),
            @ApiResponse(responseCode = "404", description = "Policy not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Admin role required")
    })
    public ResponseEntity<SlaPolicyResponse> updatePolicy(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSlaPolicyRequest request
    ) {
        SlaPolicyResponse updated = slaService.updatePolicy(id, request);
        return ResponseEntity.ok(updated);
    }

    // =========================================================================
    // 2. DASHBOARD OVERVIEW & ESCALATION AUDIT LOGS (Prompt 4)
    // =========================================================================

    @GetMapping("/api/sla/overview")
    @Operation(summary = "SLA Dashboard Overview Metrics", description = "Returns active, on-track, warning, breached, and completed counts.")
    public ResponseEntity<SlaOverviewResponse> getOverviewMetrics() {
        return ResponseEntity.ok(slaService.getOverviewMetrics());
    }

    @GetMapping("/api/sla/escalations")
    @Operation(summary = "SLA Escalation History", description = "Returns the audit trail of automatic escalations with timestamps and level progression.")
    public ResponseEntity<List<SlaEscalationResponse>> getEscalationHistory(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(slaService.getEscalationHistory(limit));
    }

    // =========================================================================
    // 3. INCIDENT SLA DETAILS (Prompt 4 + backward compatible paths)
    // =========================================================================

    @GetMapping({"/api/sla/incidents/{incidentId}", "/api/sla/incident/{incidentId}"})
    @Operation(summary = "Get SLA details by incident ID", description = "Retrieves SLA tracking record by incident UUID or incident number.")
    public ResponseEntity<SLAResponse> getSlaByIncident(
            @Parameter(description = "Incident UUID or incident number")
            @PathVariable String incidentId
    ) {
        SLAResponse response = slaService.getSLAByIncidentId(incidentId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 4. LEGACY COMPATIBILITY ENDPOINTS
    // =========================================================================

    @PostMapping("/api/incidents/{id}/sla")
    @Operation(summary = "Calculate and create SLA for incident")
    public ResponseEntity<SLAResponse> createIncidentSLA(@PathVariable String id) {
        SLAResponse response = slaService.createSLAForIncident(id);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/api/incidents/{id}/sla")
    @Operation(summary = "Get SLA record for incident")
    public ResponseEntity<SLAResponse> getIncidentSLA(@PathVariable String id) {
        SLAResponse response = slaService.getSLAByIncidentId(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/sla/active")
    @Operation(summary = "List active SLA records")
    public ResponseEntity<Object> getActiveSLAs(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        List<SLAResponse> activeSLAs = slaService.getActiveSLAs();
        if (page != null || size != null) {
            int p = page != null ? Math.max(0, page) : 0;
            int s = size != null ? Math.max(1, size) : 20;
            return ResponseEntity.ok(PagedResponse.of(activeSLAs, p, s));
        }
        return ResponseEntity.ok(activeSLAs);
    }

    @GetMapping("/api/sla/at-risk")
    @Operation(summary = "List at-risk / warning SLA records")
    public ResponseEntity<Object> getAtRiskSLAs(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        List<SLAResponse> atRiskSLAs = slaService.getAtRiskSLAs();
        if (page != null || size != null) {
            int p = page != null ? Math.max(0, page) : 0;
            int s = size != null ? Math.max(1, size) : 20;
            return ResponseEntity.ok(PagedResponse.of(atRiskSLAs, p, s));
        }
        return ResponseEntity.ok(atRiskSLAs);
    }

    @GetMapping("/api/sla/breached")
    @Operation(summary = "List breached SLA records")
    public ResponseEntity<Object> getBreachedSLAs(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        List<SLAResponse> breachedSLAs = slaService.getBreachedSLAs();
        if (page != null || size != null) {
            int p = page != null ? Math.max(0, page) : 0;
            int s = size != null ? Math.max(1, size) : 20;
            return ResponseEntity.ok(PagedResponse.of(breachedSLAs, p, s));
        }
        return ResponseEntity.ok(breachedSLAs);
    }

    @GetMapping("/api/sla/{id}")
    @Operation(summary = "Get SLA record by ID")
    public ResponseEntity<SLAResponse> getSLAById(@PathVariable UUID id) {
        SLAResponse response = slaService.getSLAById(id);
        return ResponseEntity.ok(response);
    }
}
