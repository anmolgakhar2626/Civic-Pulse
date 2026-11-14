package com.smartcity.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Audit Logs", description = "Municipal governance, change audit trail, and compliance logging APIs")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMAND_OPERATOR')")
    @Operation(summary = "Get recent system audit logs",
            description = "Retrieves recent municipal actions, state transitions, logins, and operational dispatch records.")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getRecentLogs(
            @RequestParam(required = false, defaultValue = "50") int limit
    ) {
        List<AuditLog> logs = auditService.getRecentLogs(limit);
        return ResponseEntity.ok(ApiResponse.ok(logs, "Audit logs retrieved successfully"));
    }

    @GetMapping("/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER')")
    @Operation(summary = "Get audit logs for specific entity",
            description = "Retrieves full historical audit trail for a specific entity ID.")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getLogsByEntity(
            @PathVariable String entityType,
            @PathVariable String entityId
    ) {
        List<AuditLog> logs = auditService.getLogsByEntity(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.ok(logs, "Entity audit trail retrieved successfully"));
    }
}
