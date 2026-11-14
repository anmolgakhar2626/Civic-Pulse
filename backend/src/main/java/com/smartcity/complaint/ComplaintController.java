package com.smartcity.complaint;

import com.smartcity.common.ForbiddenException;
import com.smartcity.common.PagedResponse;
import com.smartcity.complaint.dto.*;
import com.smartcity.grievance.portal.GovernmentGrievanceService;
import com.smartcity.grievance.portal.dto.ExternalGrievanceTrackingResponse;
import com.smartcity.grievance.portal.dto.ForwardGrievanceRequest;
import com.smartcity.security.dto.UserSummaryResponse;
import com.smartcity.user.User;
import com.smartcity.user.UserDao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/complaints")
@Tag(name = "Complaints & Grievances", description = "Citizen complaint reporting, reputation management, officer verification, and government grievance portal integration")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final FileStorageService fileStorageService;
    private final GovernmentGrievanceService grievanceService;
    private final UserDao userDao;

    public ComplaintController(ComplaintService complaintService,
                               FileStorageService fileStorageService,
                               GovernmentGrievanceService grievanceService,
                               UserDao userDao) {
        this.complaintService = complaintService;
        this.fileStorageService = fileStorageService;
        this.grievanceService = grievanceService;
        this.userDao = userDao;
    }

    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return userDao.findByUsernameOrEmail(auth.getName()).orElse(null);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "File a new citizen complaint with file attachments", description = "Submits a civic grievance with optional photos. Requires verified mobile number.")
    public ResponseEntity<ComplaintResponse> createComplaintMultipart(
            @ModelAttribute CreateComplaintRequest request,
            @RequestParam(value = "files", required = false) List<MultipartFile> files
    ) {
        User currentUser = resolveCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("Authentication is required to file a complaint.");
        }
        boolean isCitizen = currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(r -> "ROLE_CITIZEN".equalsIgnoreCase(r.getName()) || "CITIZEN".equalsIgnoreCase(r.getName()));
        boolean isStaffOrAdmin = currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getName()) || "ADMIN".equalsIgnoreCase(r.getName())
                        || "ROLE_COMMAND_OPERATOR".equalsIgnoreCase(r.getName()) || "COMMAND_OPERATOR".equalsIgnoreCase(r.getName())
                        || "ROLE_DEPARTMENT_OFFICER".equalsIgnoreCase(r.getName()) || "DEPARTMENT_OFFICER".equalsIgnoreCase(r.getName())
                        || "ROLE_FIELD_WORKER".equalsIgnoreCase(r.getName()) || "FIELD_WORKER".equalsIgnoreCase(r.getName()));

        if (!isCitizen || isStaffOrAdmin) {
            throw new ForbiddenException("Access denied: Grievance intake and submission is strictly reserved for citizen accounts.");
        }
        ComplaintResponse response = complaintService.createComplaint(request, files != null ? files : List.of(), currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "File a new citizen complaint via JSON", description = "Submits a civic grievance without attachments.")
    public ResponseEntity<ComplaintResponse> createComplaintJson(@Valid @RequestBody CreateComplaintRequest request) {
        User currentUser = resolveCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("Authentication is required to file a complaint.");
        }
        boolean isCitizen = currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(r -> "ROLE_CITIZEN".equalsIgnoreCase(r.getName()) || "CITIZEN".equalsIgnoreCase(r.getName()));
        boolean isStaffOrAdmin = currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getName()) || "ADMIN".equalsIgnoreCase(r.getName())
                        || "ROLE_COMMAND_OPERATOR".equalsIgnoreCase(r.getName()) || "COMMAND_OPERATOR".equalsIgnoreCase(r.getName())
                        || "ROLE_DEPARTMENT_OFFICER".equalsIgnoreCase(r.getName()) || "DEPARTMENT_OFFICER".equalsIgnoreCase(r.getName())
                        || "ROLE_FIELD_WORKER".equalsIgnoreCase(r.getName()) || "FIELD_WORKER".equalsIgnoreCase(r.getName()));

        if (!isCitizen || isStaffOrAdmin) {
            throw new ForbiddenException("Access denied: Grievance intake and submission is strictly reserved for citizen accounts.");
        }
        ComplaintResponse response = complaintService.createComplaint(request, List.of(), currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List complaints", description = "Query complaints with filtering by status, category, verification, department, priority, and citizen.")
    public ResponseEntity<PagedResponse<ComplaintResponse>> getComplaints(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String verificationStatus,
            @RequestParam(required = false) UUID citizenId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID assignedToUserId,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String slaStatus,
            @RequestParam(required = false) Boolean unassigned,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        User currentUser = resolveCurrentUser();
        boolean isAdminOrOp = currentUser != null && currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getName())
                        || "ADMIN".equalsIgnoreCase(r.getName())
                        || "ROLE_COMMAND_OPERATOR".equalsIgnoreCase(r.getName())
                        || "COMMAND_OPERATOR".equalsIgnoreCase(r.getName()));

        boolean isOfficer = currentUser != null && currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(r -> "ROLE_DEPARTMENT_OFFICER".equalsIgnoreCase(r.getName())
                        || "DEPARTMENT_OFFICER".equalsIgnoreCase(r.getName()));

        boolean isFieldWorker = currentUser != null && currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(r -> "ROLE_FIELD_WORKER".equalsIgnoreCase(r.getName())
                        || "FIELD_WORKER".equalsIgnoreCase(r.getName()));

        UUID effCitizenId = citizenId;
        UUID effDeptId = departmentId;
        UUID effAssignedTo = assignedToUserId;

        if (!isAdminOrOp) {
            if (isOfficer) {
                if (effDeptId == null && currentUser.getDepartment() != null) {
                    effDeptId = currentUser.getDepartment().getId();
                }
            } else if (isFieldWorker) {
                if (effAssignedTo == null) {
                    effAssignedTo = currentUser.getId();
                }
            } else {
                effCitizenId = currentUser.getId();
            }
        }

        List<ComplaintResponse> content = complaintService.getComplaints(
                status, category, verificationStatus, effCitizenId, effDeptId, effAssignedTo, priority, search, slaStatus, unassigned, page, size);
        int total = complaintService.countComplaints(
                status, category, verificationStatus, effCitizenId, effDeptId, effAssignedTo, priority, search, slaStatus, unassigned);
        int pageSize = size > 0 ? size : 50;
        int totalPages = (int) Math.ceil((double) total / pageSize);
        boolean first = (page == 0);
        boolean last = ((long) (page + 1) * pageSize >= total);

        PagedResponse<ComplaintResponse> response = new PagedResponse<>(content, page, pageSize, total, totalPages, first, last);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get complaint details by ID")
    public ResponseEntity<ComplaintResponse> getComplaintById(@PathVariable UUID id) {
        User currentUser = resolveCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("Authentication required.");
        }
        ComplaintResponse response = complaintService.getComplaintById(id);

        boolean isAdminOrOp = currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getName())
                        || "ADMIN".equalsIgnoreCase(r.getName())
                        || "ROLE_COMMAND_OPERATOR".equalsIgnoreCase(r.getName())
                        || "COMMAND_OPERATOR".equalsIgnoreCase(r.getName()));

        boolean isOfficer = currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(r -> "ROLE_DEPARTMENT_OFFICER".equalsIgnoreCase(r.getName())
                        || "DEPARTMENT_OFFICER".equalsIgnoreCase(r.getName()));

        boolean isFieldWorker = currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(r -> "ROLE_FIELD_WORKER".equalsIgnoreCase(r.getName())
                        || "FIELD_WORKER".equalsIgnoreCase(r.getName()));

        if (!isAdminOrOp) {
            if (isOfficer) {
                // Department isolation: officer must belong to same department or complaint is unassigned
                if (currentUser.getDepartment() != null && response.getDepartmentId() != null
                        && !currentUser.getDepartment().getId().equals(response.getDepartmentId())) {
                    throw new ForbiddenException("Access denied: Department isolation prevents viewing complaints belonging to other departments.");
                }
            } else if (isFieldWorker) {
                // Field worker can view if assigned to them or in their department
                boolean isAssigned = response.getAssignedToId() != null && response.getAssignedToId().equals(currentUser.getId());
                boolean isSameDept = currentUser.getDepartment() != null && response.getDepartmentId() != null
                        && currentUser.getDepartment().getId().equals(response.getDepartmentId());
                if (!isAssigned && !isSameDept) {
                    throw new ForbiddenException("Access denied: Field worker not assigned to this grievance.");
                }
            } else {
                // Citizen role
                boolean isOwner = response.getCitizenId() != null && response.getCitizenId().equals(currentUser.getId());
                if (!isOwner) {
                    // Check if resolved / closed: can view resolved grievances per visibility rules, but mask private data
                    boolean isResolvedOrClosed = "RESOLVED".equalsIgnoreCase(response.getStatus()) || "CLOSED".equalsIgnoreCase(response.getStatus());
                    if (!isResolvedOrClosed) {
                        throw new ForbiddenException("Access denied: Citizens cannot access other citizens' private grievance data.");
                    }
                    // Sanitize private citizen data for non-owner viewing resolved public complaints
                    response.setCitizenPhone(null);
                }
            }
        }

        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/{id}/verify", method = {RequestMethod.PATCH, RequestMethod.POST})
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT_OFFICER')")
    @Operation(summary = "Verify complaint", description = "Marks complaint as GENUINE, REJECTED_SPAM, DUPLICATE, or UNVERIFIED, triggering reputation recalculation.")
    public ResponseEntity<ComplaintResponse> verifyComplaint(
            @PathVariable UUID id,
            @Valid @RequestBody VerifyComplaintRequest request
    ) {
        User verifier = resolveCurrentUser();
        if (verifier == null) {
            throw new ForbiddenException("Authentication required.");
        }
        ComplaintResponse response = complaintService.verifyComplaint(id, request, verifier);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER')")
    @Operation(summary = "Assign complaint to department or field worker")
    public ResponseEntity<ComplaintResponse> assignComplaint(
            @PathVariable UUID id,
            @Valid @RequestBody AssignComplaintRequest request
    ) {
        User assigner = resolveCurrentUser();
        return ResponseEntity.ok(complaintService.assignComplaint(id, request, assigner));
    }

    @PatchMapping(value = "/{id}/progress", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER', 'FIELD_WORKER')")
    @Operation(summary = "Update progress with photo evidence")
    public ResponseEntity<ComplaintResponse> updateProgressMultipart(
            @PathVariable UUID id,
            @ModelAttribute UpdateComplaintProgressRequest request,
            @RequestParam(value = "files", required = false) List<MultipartFile> files
    ) {
        User updater = resolveCurrentUser();
        return ResponseEntity.ok(complaintService.updateProgress(id, request, files != null ? files : List.of(), updater));
    }

    @PatchMapping(value = "/{id}/progress", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER', 'FIELD_WORKER')")
    @Operation(summary = "Update progress via JSON")
    public ResponseEntity<ComplaintResponse> updateProgressJson(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateComplaintProgressRequest request
    ) {
        User updater = resolveCurrentUser();
        return ResponseEntity.ok(complaintService.updateProgress(id, request, List.of(), updater));
    }

    @PostMapping(value = "/{id}/resolution", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER', 'FIELD_WORKER')")
    @Operation(summary = "Submit resolution with proof photos")
    public ResponseEntity<ComplaintResponse> submitResolutionMultipart(
            @PathVariable UUID id,
            @RequestParam("resolutionNotes") String resolutionNotes,
            @RequestParam(value = "files", required = false) List<MultipartFile> files
    ) {
        User resolver = resolveCurrentUser();
        return ResponseEntity.ok(complaintService.submitResolution(id, resolutionNotes, files != null ? files : List.of(), resolver));
    }

    @PostMapping(value = "/{id}/resolution", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER', 'FIELD_WORKER')")
    @Operation(summary = "Submit resolution via JSON")
    public ResponseEntity<ComplaintResponse> submitResolutionJson(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body
    ) {
        User resolver = resolveCurrentUser();
        String notes = body != null ? body.get("resolutionNotes") : "Resolved";
        return ResponseEntity.ok(complaintService.submitResolution(id, notes, List.of(), resolver));
    }

    @PatchMapping("/{id}/verify-resolution")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER')")
    @Operation(summary = "Verify submitted resolution (ACCEPTED -> CLOSED, REJECTED -> IN_PROGRESS)")
    public ResponseEntity<ComplaintResponse> verifyResolution(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body
    ) {
        User verifier = resolveCurrentUser();
        String decision = body != null && body.containsKey("decision") ? body.get("decision") : "ACCEPTED";
        String notes = body != null ? body.get("notes") : "";
        return ResponseEntity.ok(complaintService.verifyResolution(id, decision, notes, verifier));
    }

    @PatchMapping("/{id}/priority")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMAND_OPERATOR')")
    @Operation(summary = "Change priority and adjust SLA deadline")
    public ResponseEntity<ComplaintResponse> changePriority(
            @PathVariable UUID id,
            @Valid @RequestBody ChangePriorityRequest request
    ) {
        User modifier = resolveCurrentUser();
        return ResponseEntity.ok(complaintService.changePriority(id, request, modifier));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMAND_OPERATOR', 'CITIZEN')")
    @Operation(summary = "Close or reopen a complaint")
    public ResponseEntity<ComplaintResponse> closeOrReopen(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        User user = resolveCurrentUser();
        String action = body != null && body.containsKey("action") ? body.get("action") : "CLOSE";
        String reason = body != null ? body.get("reason") : "Administrative decision";
        return ResponseEntity.ok(complaintService.closeOrReopen(id, action, reason, user));
    }

    @GetMapping("/assignees")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER')")
    @Operation(summary = "Get eligible field workers and staff for complaint assignment")
    public ResponseEntity<List<UserSummaryResponse>> getEligibleAssignees(
            @RequestParam(required = false) UUID departmentId
    ) {
        User current = resolveCurrentUser();
        UUID effDeptId = departmentId;
        if (effDeptId == null && current != null && current.getDepartment() != null) {
            effDeptId = current.getDepartment().getId();
        }

        final UUID targetDeptId = effDeptId;
        List<UserSummaryResponse> assignees = userDao.findAll().stream()
                .filter(u -> u.getActive() != null && u.getActive() && (u.getEnabled() == null || u.getEnabled()))
                .filter(u -> {
                    boolean isWorkerOrOfficer = u.getRoles() != null && u.getRoles().stream().anyMatch(r ->
                            "ROLE_FIELD_WORKER".equalsIgnoreCase(r.getName())
                                    || "FIELD_WORKER".equalsIgnoreCase(r.getName())
                                    || "ROLE_DEPARTMENT_OFFICER".equalsIgnoreCase(r.getName())
                                    || "DEPARTMENT_OFFICER".equalsIgnoreCase(r.getName()));
                    if (!isWorkerOrOfficer) return false;
                    if (targetDeptId != null && u.getDepartment() != null) {
                        return targetDeptId.equals(u.getDepartment().getId());
                    }
                    return true;
                })
                .map(UserSummaryResponse::fromEntity)
                .toList();

        if (assignees.isEmpty()) {
            assignees = userDao.findAll().stream()
                    .filter(u -> u.getActive() != null && u.getActive() && (u.getEnabled() == null || u.getEnabled()))
                    .filter(u -> u.getRoles() != null && u.getRoles().stream().anyMatch(r ->
                            "ROLE_FIELD_WORKER".equalsIgnoreCase(r.getName())
                                    || "FIELD_WORKER".equalsIgnoreCase(r.getName())
                                    || "ROLE_DEPARTMENT_OFFICER".equalsIgnoreCase(r.getName())
                                    || "DEPARTMENT_OFFICER".equalsIgnoreCase(r.getName())))
                    .map(UserSummaryResponse::fromEntity)
                    .toList();
        }

        return ResponseEntity.ok(assignees);
    }

    @GetMapping("/{id}/receipt")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get official digital acknowledgement receipt metadata")
    public ResponseEntity<ComplaintReceiptDTO> getComplaintReceipt(@PathVariable UUID id) {
        return ResponseEntity.ok(complaintService.getReceipt(id));
    }

    @GetMapping("/{id}/receipt/pdf")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download official digital acknowledgement receipt text/PDF")
    public ResponseEntity<byte[]> downloadReceiptPdf(@PathVariable UUID id) {
        ComplaintReceiptDTO receipt = complaintService.getReceipt(id);
        byte[] data = complaintService.getReceiptPdf(id);
        String fileName = "Receipt-" + receipt.getComplaintNumber() + ".txt";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(data);
    }

    @GetMapping("/attachments/{attachmentId}")
    @Operation(summary = "Stream complaint attachment file")
    public ResponseEntity<Resource> getAttachment(@PathVariable UUID attachmentId) {
        ComplaintAttachment attachment = complaintService.getAttachment(attachmentId);
        Resource resource = fileStorageService.loadFileAsResource(attachment.getFileName());
        String contentType = attachment.getFileType() != null ? attachment.getFileType() : "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getOriginalFileName() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(resource);
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMAND_OPERATOR')")
    @Operation(summary = "Get administrative KPIs and complaint metrics")
    public ResponseEntity<AdminComplaintStatsDTO> getAdminStats() {
        return ResponseEntity.ok(complaintService.getAdminStats());
    }

    @GetMapping("/department/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER')")
    @Operation(summary = "Get department-specific complaint statistics")
    public ResponseEntity<DepartmentComplaintStatsDTO> getDepartmentStats(
            @RequestParam(required = false) UUID departmentId
    ) {
        User officer = resolveCurrentUser();
        return ResponseEntity.ok(complaintService.getDepartmentStats(departmentId, officer));
    }

    @GetMapping("/department/stats/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMAND_OPERATOR')")
    @Operation(summary = "Get complaint statistics for all departments")
    public ResponseEntity<List<DepartmentComplaintStatsDTO>> getAllDepartmentStats() {
        return ResponseEntity.ok(complaintService.getAllDepartmentStats());
    }

    @GetMapping("/{id}/verification-history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get complaint verification history audit trail")
    public ResponseEntity<List<ComplaintVerificationHistory>> getVerificationHistory(@PathVariable UUID id) {
        List<ComplaintVerificationHistory> history = complaintService.getVerificationHistory(id);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/reputation/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current citizen reputation and Verified Citizen badge status")
    public ResponseEntity<CitizenReputationResponse> getMyReputation() {
        User currentUser = resolveCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("Authentication required.");
        }
        CitizenReputationResponse response = complaintService.getCitizenReputation(currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reputation/{citizenId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get reputation and Verified Citizen badge status for a citizen")
    public ResponseEntity<CitizenReputationResponse> getCitizenReputation(@PathVariable UUID citizenId) {
        CitizenReputationResponse response = complaintService.getCitizenReputation(citizenId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/external-submission")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER', 'CITIZEN')")
    @Operation(summary = "Forward complaint to official government portal adapter", description = "Submits complaint to designated sandbox or external portal adapter (e.g. CPGRAMS_SANDBOX). Idempotent.")
    public ResponseEntity<ExternalGrievanceTrackingResponse> forwardToGovernmentPortal(
            @PathVariable UUID id,
            @RequestBody(required = false) ForwardGrievanceRequest request
    ) {
        User requester = resolveCurrentUser();
        ExternalGrievanceTrackingResponse response = grievanceService.forwardToGovernmentPortal(id, request, requester);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/external-tracking")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get government grievance tracking and progress timeline")
    public ResponseEntity<ExternalGrievanceTrackingResponse> getExternalTracking(@PathVariable UUID id) {
        ExternalGrievanceTrackingResponse response = grievanceService.getExternalTracking(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/external-sync")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER', 'CITIZEN')")
    @Operation(summary = "Poll and sync grievance status progress from government portal")
    public ResponseEntity<ExternalGrievanceTrackingResponse> syncExternalStatus(@PathVariable UUID id) {
        User requester = resolveCurrentUser();
        ExternalGrievanceTrackingResponse response = grievanceService.syncExternalStatus(id, requester);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/documents/{documentReference}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download official acknowledgement receipt document")
    public ResponseEntity<byte[]> downloadAcknowledgementDocument(@PathVariable String documentReference) {
        byte[] data = grievanceService.generateReceiptPdf(documentReference);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + documentReference + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
