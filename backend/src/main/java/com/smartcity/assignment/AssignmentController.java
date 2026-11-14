package com.smartcity.assignment;

import com.smartcity.assignment.dto.*;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final ResponseActionService responseActionService;

    public AssignmentController(AssignmentService assignmentService, ResponseActionService responseActionService) {
        this.assignmentService = assignmentService;
        this.responseActionService = responseActionService;
    }

    /**
     * Assign incident to a department/team manually.
     */
    @PostMapping("/api/incidents/{id}/assign")
    @Tag(name = "Assignments", description = "Department routing, team dispatch, and auto-assignment APIs")
    @Operation(summary = "Assign incident to department/team",
            description = "Manually assigns the specified incident to a department and team with explicit operator attribution and audit reason.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident assigned successfully",
                    content = @Content(schema = @Schema(implementation = AssignmentResult.class))),
            @ApiResponse(responseCode = "400", description = "Invalid assignment request or inactive entity"),
            @ApiResponse(responseCode = "404", description = "Incident, department, or team not found")
    })
    public ResponseEntity<AssignmentResult> assignIncident(
            @Parameter(description = "Incident UUID or incident number")
            @PathVariable String id,
            @Valid @RequestBody(required = false) AssignIncidentRequest request
    ) {
        AssignmentResult result = assignmentService.assign(id, request);
        return ResponseEntity.ok(result);
    }

    /**
     * Automatically determine and assign the recommended department and active team based on configured rules.
     */
    @PostMapping("/api/incidents/{id}/auto-assign")
    @Tag(name = "Assignments", description = "Department routing, team dispatch, and auto-assignment APIs")
    @Operation(summary = "Auto-assign incident",
            description = "Evaluates configured department assignment rules to automatically assign the recommended department and available active team.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident auto-assigned successfully",
                    content = @Content(schema = @Schema(implementation = AssignmentResult.class))),
            @ApiResponse(responseCode = "400", description = "No matching department mapping found"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<AssignmentResult> autoAssignIncident(
            @Parameter(description = "Incident UUID or incident number")
            @PathVariable String id
    ) {
        AssignmentResult result = assignmentService.autoAssign(id);
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieve full audit history of incident department and team assignments.
     */
    @GetMapping("/api/incidents/{id}/assignment-history")
    @Tag(name = "Assignments", description = "Department routing, team dispatch, and auto-assignment APIs")
    @Operation(summary = "Get assignment audit history",
            description = "Retrieves an immutable chronological record of all assignment changes for the specified incident.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assignment history retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<List<AssignmentHistoryResponse>> getAssignmentHistory(
            @Parameter(description = "Incident UUID or incident number")
            @PathVariable String id
    ) {
        List<AssignmentHistoryResponse> history = assignmentService.getAssignmentHistory(id);
        return ResponseEntity.ok(history);
    }

    /**
     * Get all response actions associated with an incident.
     */
    @GetMapping("/api/incidents/{id}/actions")
    @Tag(name = "Response Actions", description = "Response action generation, action approval, and status progression APIs")
    @Operation(summary = "List response actions for incident",
            description = "Retrieves all response actions (e.g. work orders, dispatch, caution zones) associated with the incident.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Response actions retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<List<ResponseActionResponse>> getActionsForIncident(
            @Parameter(description = "Incident UUID or incident number")
            @PathVariable String id
    ) {
        List<ResponseActionResponse> actions = responseActionService.getActionsForIncident(id);
        return ResponseEntity.ok(actions);
    }

    /**
     * Create a new response action for an incident.
     */
    @PostMapping("/api/incidents/{id}/actions")
    @Tag(name = "Response Actions", description = "Response action generation, action approval, and status progression APIs")
    @Operation(summary = "Create custom response action",
            description = "Creates an operator-specified response action for the incident.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Response action created successfully",
                    content = @Content(schema = @Schema(implementation = ResponseActionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<ResponseActionResponse> createResponseAction(
            @Parameter(description = "Incident UUID or incident number")
            @PathVariable String id,
            @Valid @RequestBody CreateResponseActionRequest request
    ) {
        ResponseActionResponse response = responseActionService.createAction(id, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Update the status of a specific response action.
     */
    @PatchMapping("/api/actions/{id}/status")
    @Tag(name = "Response Actions", description = "Response action generation, action approval, and status progression APIs")
    @Operation(summary = "Update response action status",
            description = "Transitions action status (RECOMMENDED -> APPROVED -> IN_PROGRESS -> COMPLETED / CANCELLED).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Action status updated successfully",
                    content = @Content(schema = @Schema(implementation = ResponseActionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure or invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Response action not found")
    })
    public ResponseEntity<ResponseActionResponse> updateActionStatus(
            @Parameter(description = "Response action UUID")
            @PathVariable UUID id,
            @Valid @RequestBody UpdateResponseActionStatusRequest request
    ) {
        ResponseActionResponse response = responseActionService.updateActionStatus(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Generate recommended response actions for an incident based on incident type, priority tier, and operational context.
     */
    @PostMapping("/api/incidents/{id}/generate-response-plan")
    @Tag(name = "Response Actions", description = "Response action generation, action approval, and status progression APIs")
    @Operation(summary = "Generate automated response plan",
            description = "Applies Response Policy Engine to determine recommended actions for the incident based on tier and operational state.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Response plan generated successfully"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<List<ResponseActionResponse>> generateResponsePlan(
            @Parameter(description = "Incident UUID or incident number")
            @PathVariable String id
    ) {
        List<ResponseActionResponse> plan = responseActionService.generateResponsePlan(id);
        return ResponseEntity.ok(plan);
    }
}
