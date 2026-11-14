package com.smartcity.priority;

import com.smartcity.priority.dto.EvaluatePriorityRequest;
import com.smartcity.priority.dto.PriorityEvaluationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/priority")
@Tag(name = "Priority", description = "Multi-factor Priority Rule Engine evaluation and tier scoring APIs")
public class PriorityController {

    private final PriorityService priorityService;

    public PriorityController(PriorityService priorityService) {
        this.priorityService = priorityService;
    }

    @PostMapping("/evaluate")
    @Operation(summary = "Evaluate priority for raw incident parameters",
            description = "Applies deterministic, multi-factor municipal priority rules to evaluate priority tier, reasons, and target SLA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Priority evaluated successfully",
                    content = @Content(schema = @Schema(implementation = PriorityEvaluationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    public ResponseEntity<PriorityEvaluationResponse> evaluatePriority(
            @Valid @RequestBody EvaluatePriorityRequest request
    ) {
        PriorityEvaluationResponse response = priorityService.evaluatePriority(request);
        return ResponseEntity.ok(response);
    }
}
