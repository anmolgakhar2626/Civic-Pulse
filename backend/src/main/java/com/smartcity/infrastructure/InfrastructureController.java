package com.smartcity.infrastructure;

import com.smartcity.common.PagedResponse;
import com.smartcity.infrastructure.dto.CreateInfrastructureRequest;
import com.smartcity.infrastructure.dto.InfrastructureResponse;
import com.smartcity.infrastructure.dto.UpdateInfrastructureRequest;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/infrastructure")
@Tag(name = "Infrastructure", description = "Municipal infrastructure assets, operational monitoring, and CRUD endpoints")
public class InfrastructureController {

    private final InfrastructureService infrastructureService;

    public InfrastructureController(InfrastructureService infrastructureService) {
        this.infrastructureService = infrastructureService;
    }

    @GetMapping
    @Operation(summary = "List infrastructure assets with optional filters and bounding box")
    public ResponseEntity<PagedResponse<InfrastructureResponse>> getInfrastructure(
            @Parameter(description = "Category: ROAD, BRIDGE, HOSPITAL, WATER_PIPELINE, DRAINAGE, SUBSTATION, SHELTER")
            @RequestParam(required = false) String category,
            @Parameter(description = "Operational status: OPERATIONAL, DEGRADED, OFFLINE, UNDER_MAINTENANCE")
            @RequestParam(required = false) String status,
            @Parameter(description = "Search keyword in name, code, or address")
            @RequestParam(required = false) String search,
            @Parameter(description = "Bounding box minimum latitude")
            @RequestParam(required = false) Double minLat,
            @Parameter(description = "Bounding box maximum latitude")
            @RequestParam(required = false) Double maxLat,
            @Parameter(description = "Bounding box minimum longitude")
            @RequestParam(required = false) Double minLng,
            @Parameter(description = "Bounding box maximum longitude")
            @RequestParam(required = false) Double maxLng,
            @Parameter(description = "Zero-based page number")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 200)")
            @RequestParam(defaultValue = "20") int size
    ) {
        PagedResponse<InfrastructureResponse> response = infrastructureService.getInfrastructureList(
                category, status, search, minLat, maxLat, minLng, maxLng, page, size
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get infrastructure asset details by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Infrastructure asset found",
                    content = @Content(schema = @Schema(implementation = InfrastructureResponse.class))),
            @ApiResponse(responseCode = "404", description = "Infrastructure asset not found")
    })
    public ResponseEntity<InfrastructureResponse> getInfrastructureById(@PathVariable UUID id) {
        return ResponseEntity.ok(infrastructureService.getInfrastructureById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMAND_OPERATOR', 'OPERATOR')")
    @Operation(summary = "Create a new infrastructure asset", description = "Restricted to ADMIN or COMMAND_OPERATOR roles.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Asset registered successfully",
                    content = @Content(schema = @Schema(implementation = InfrastructureResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload or validation failure"),
            @ApiResponse(responseCode = "403", description = "Unauthorized role")
    })
    public ResponseEntity<InfrastructureResponse> createInfrastructure(@Valid @RequestBody CreateInfrastructureRequest request) {
        InfrastructureResponse created = infrastructureService.createInfrastructure(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMAND_OPERATOR', 'OPERATOR')")
    @Operation(summary = "Update an infrastructure asset", description = "Restricted to ADMIN or COMMAND_OPERATOR roles.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asset updated successfully",
                    content = @Content(schema = @Schema(implementation = InfrastructureResponse.class))),
            @ApiResponse(responseCode = "404", description = "Asset not found"),
            @ApiResponse(responseCode = "403", description = "Unauthorized role")
    })
    public ResponseEntity<InfrastructureResponse> updateInfrastructure(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInfrastructureRequest request
    ) {
        InfrastructureResponse updated = infrastructureService.updateInfrastructure(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an infrastructure asset", description = "Restricted to ADMIN role only.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Asset deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Asset not found"),
            @ApiResponse(responseCode = "403", description = "Unauthorized role")
    })
    public ResponseEntity<Void> deleteInfrastructure(@PathVariable UUID id) {
        infrastructureService.deleteInfrastructure(id);
        return ResponseEntity.noContent().build();
    }
}

