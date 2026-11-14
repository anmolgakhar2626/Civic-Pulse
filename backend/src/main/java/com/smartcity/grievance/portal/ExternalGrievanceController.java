package com.smartcity.grievance.portal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/external-grievances")
@Tag(name = "External Grievances", description = "Government grievance portal information and document downloads")
public class ExternalGrievanceController {

    private final GovernmentGrievanceService grievanceService;

    public ExternalGrievanceController(GovernmentGrievanceService grievanceService) {
        this.grievanceService = grievanceService;
    }

    @GetMapping("/portals")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List available government grievance portals")
    public ResponseEntity<List<GovernmentPortal>> getPortals() {
        return ResponseEntity.ok(grievanceService.getAvailablePortals());
    }

    @GetMapping("/documents/{documentReference}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download official acknowledgement receipt")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable String documentReference) {
        byte[] data = grievanceService.generateReceiptPdf(documentReference);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + documentReference + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
