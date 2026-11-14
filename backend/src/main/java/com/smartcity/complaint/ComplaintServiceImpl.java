package com.smartcity.complaint;

import com.smartcity.common.ForbiddenException;
import com.smartcity.common.ResourceNotFoundException;
import com.smartcity.complaint.dto.*;
import com.smartcity.department.Department;
import com.smartcity.department.DepartmentRepository;
import com.smartcity.grievance.portal.GovernmentGrievanceService;
import com.smartcity.grievance.portal.dto.ExternalGrievanceTrackingResponse;
import com.smartcity.notification.NotificationChannel;
import com.smartcity.notification.NotificationService;
import com.smartcity.user.Role;
import com.smartcity.user.User;
import com.smartcity.user.UserDao;
import com.smartcity.websocket.WebSocketEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ComplaintServiceImpl implements ComplaintService {

    private static final Logger log = LoggerFactory.getLogger(ComplaintServiceImpl.class);

    public static final int VERIFIED_CITIZEN_THRESHOLD = 5; // >= 5 genuine complaints
    private static final DateTimeFormatter RECEIPT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.of("UTC"));

    private final ComplaintDao complaintDao;
    private final UserDao userDao;
    private final DepartmentRepository departmentRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final WebSocketEventPublisher webSocketPublisher;
    private final GovernmentGrievanceService grievanceService;

    @Autowired
    public ComplaintServiceImpl(
            ComplaintDao complaintDao,
            UserDao userDao,
            DepartmentRepository departmentRepository,
            FileStorageService fileStorageService,
            NotificationService notificationService,
            @Autowired(required = false) WebSocketEventPublisher webSocketPublisher,
            @Autowired(required = false) GovernmentGrievanceService grievanceService
    ) {
        this.complaintDao = complaintDao;
        this.userDao = userDao;
        this.departmentRepository = departmentRepository;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
        this.webSocketPublisher = webSocketPublisher;
        this.grievanceService = grievanceService;
    }

    public ComplaintServiceImpl(
            ComplaintDao complaintDao,
            UserDao userDao,
            GovernmentGrievanceService grievanceService
    ) {
        this(complaintDao, userDao, null, null, null, null, grievanceService);
    }

    public static String mapCategoryToDepartmentCode(String category) {
        if (category == null) return "PWD";
        String upper = category.trim().toUpperCase();
        if (upper.contains("ROAD") || upper.contains("POTHOLE") || upper.contains("PWD") || upper.contains("INFRA") || upper.contains("BRIDGE") || upper.contains("FOOTPATH")) {
            return "PWD";
        } else if (upper.contains("WATER") || upper.contains("DRAIN") || upper.contains("LEAK")) {
            return "WATER";
        } else if (upper.contains("ELECT") || upper.contains("POWER") || upper.contains("LIGHT")) {
            return "ELECTRICAL";
        } else if (upper.contains("SOLID") || upper.contains("WASTE") || upper.contains("GARBAGE") || upper.contains("CLEAN") || upper.contains("SANIT")) {
            return "SANITATION";
        } else if (upper.contains("HEALTH") || upper.contains("DISEASE") || upper.contains("HOSPITAL") || upper.contains("EPIDEMIC")) {
            return "HEALTH";
        } else if (upper.contains("TRAFFIC") || upper.contains("SIGNAL") || upper.contains("CONGESTION") || upper.contains("PARKING")) {
            return "TRAFFIC";
        } else if (upper.contains("ENVIRON") || upper.contains("POLLUTION") || upper.contains("NOISE") || upper.contains("TREE")) {
            return "ENVIRONMENT";
        } else if (upper.contains("EMERGENCY") || upper.contains("FIRE") || upper.contains("DISASTER") || upper.contains("ACCIDENT")) {
            return "EMERGENCY";
        } else {
            return "PWD";
        }
    }

    public static int getSlaMinutesForPriority(String priority) {
        if (priority == null) return 1440; // Default MEDIUM: 24h
        return switch (priority.trim().toUpperCase()) {
            case "CRITICAL" -> 240;  // 4 hours
            case "HIGH" -> 720;      // 12 hours
            case "LOW" -> 2880;     // 48 hours
            case "MEDIUM" -> 1440;  // 24 hours
            default -> 1440;
        };
    }

    @Override
    @Transactional
    public ComplaintResponse createComplaint(CreateComplaintRequest request, List<MultipartFile> files, User citizen) {
        if (citizen == null) {
            throw new ForbiddenException("Authentication is required to file a complaint.");
        }

        User freshUser = userDao.findById(citizen.getId()).orElse(citizen);
        boolean isCitizen = (freshUser.getRoles() != null && freshUser.getRoles().stream()
                .anyMatch(r -> "ROLE_CITIZEN".equalsIgnoreCase(r.getName()) || "CITIZEN".equalsIgnoreCase(r.getName())))
                || "CITIZEN".equalsIgnoreCase(freshUser.getRoleName());

        boolean isStaffOrAdmin = freshUser.getRoles() != null && freshUser.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getName()) || "ADMIN".equalsIgnoreCase(r.getName())
                        || "ROLE_COMMAND_OPERATOR".equalsIgnoreCase(r.getName()) || "COMMAND_OPERATOR".equalsIgnoreCase(r.getName())
                        || "ROLE_DEPARTMENT_OFFICER".equalsIgnoreCase(r.getName()) || "DEPARTMENT_OFFICER".equalsIgnoreCase(r.getName())
                        || "ROLE_FIELD_WORKER".equalsIgnoreCase(r.getName()) || "FIELD_WORKER".equalsIgnoreCase(r.getName()));

        if (!isCitizen || isStaffOrAdmin) {
            throw new ForbiddenException("Access denied: Grievance intake and submission is strictly reserved for citizen accounts.");
        }

        // If citizen provided mobile number with complaint, persist to user profile if missing
        if (request.getCitizenPhone() != null && !request.getCitizenPhone().isBlank()) {
            if (freshUser.getMobileNumber() == null || freshUser.getMobileNumber().isBlank()) {
                freshUser.setMobileNumber(request.getCitizenPhone().trim());
                try {
                    userDao.save(freshUser);
                } catch (Exception ignored) {}
            }
        }

        // 1. Generate unique sequence record number: CP-YYYY-XXXXXX
        String recordNumber = null;
        try {
            recordNumber = complaintDao.generateNextComplaintRecordNumber();
        } catch (Exception ignored) {}
        if (recordNumber == null || recordNumber.isBlank()) {
            recordNumber = "CP-" + java.time.Year.now().getValue() + "-" + String.format("%06d", System.currentTimeMillis() % 1000000);
        }

        // 2. Resolve department
        String category = request.getCategory() != null ? request.getCategory().trim() : "GENERAL";
        String deptCode = mapCategoryToDepartmentCode(category);
        Department dept = null;
        if (departmentRepository != null) {
            try {
                if (request.getDepartmentId() != null) {
                    dept = departmentRepository.findById(request.getDepartmentId()).orElse(null);
                }
                if (dept == null) {
                    dept = departmentRepository.findByCode(deptCode).orElse(null);
                }
            } catch (Exception ignored) {}
        }

        // 3. Resolve Priority & SLA
        String priority = (request.getPriority() != null && !request.getPriority().isBlank())
                ? request.getPriority().trim().toUpperCase()
                : "MEDIUM";
        int slaMinutes = getSlaMinutesForPriority(priority);
        Instant now = Instant.now();
        Instant slaDeadline = now.plus(Duration.ofMinutes(slaMinutes));

        // 4. Construct Complaint
        Complaint complaint = new Complaint();
        complaint.setId(UUID.randomUUID());
        complaint.setComplaintNumber(recordNumber);
        complaint.setCitizen(freshUser);
        complaint.setCitizenName(freshUser.getFullName());
        complaint.setCitizenPhone(request.getCitizenPhone() != null && !request.getCitizenPhone().isBlank()
                ? request.getCitizenPhone().trim()
                : (freshUser.getMobileNumber() != null ? freshUser.getMobileNumber() : freshUser.getPhone()));
        complaint.setTitle(request.getTitle() != null && !request.getTitle().isBlank()
                ? request.getTitle().trim()
                : category + " Grievance Report");
        complaint.setCategory(category);
        complaint.setSubcategory(request.getSubcategory() != null ? request.getSubcategory().trim() : null);
        complaint.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");
        complaint.setDepartment(dept);
        complaint.setPriority(priority);
        complaint.setSlaMinutes(slaMinutes);
        complaint.setSlaDeadline(slaDeadline);
        complaint.setSlaStatus("ON_TRACK");
        complaint.setLatitude(request.getLatitude());
        complaint.setLongitude(request.getLongitude());
        complaint.setAddress(request.getAddress() != null ? request.getAddress().trim() : "Smart City Command Zone");
        complaint.setStatus("SUBMITTED");
        complaint.setComplaintVerificationStatus("UNVERIFIED");
        complaint.setEvidenceUrl(request.getEvidenceUrl());
        complaint.setCreatedAt(now);
        complaint.setUpdatedAt(now);

        // Save Complaint first so foreign key constraint in complaint_attachments is satisfied
        Complaint saved = complaintDao.save(complaint);
        if (saved == null) saved = complaint;

        // 5. Store multipart file attachments if uploaded
        List<ComplaintAttachment> storedAttachments = new ArrayList<>();
        if (fileStorageService != null && files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    try {
                        FileStorageService.StoredFileResult result = fileStorageService.storeFile(file);
                        ComplaintAttachment attachment = new ComplaintAttachment();
                        attachment.setId(UUID.randomUUID());
                        attachment.setComplaintId(saved.getId());
                        attachment.setFileName(result.getFileName());
                        attachment.setOriginalFileName(result.getOriginalFileName());
                        attachment.setFileType(result.getContentType());
                        attachment.setFileSize(result.getFileSize());
                        attachment.setFilePath(result.getFilePath());
                        attachment.setFileUrl("/api/complaints/attachments/" + attachment.getId());
                        attachment.setUploadedBy(freshUser);
                        attachment.setCreatedAt(now);
                        complaintDao.saveAttachment(attachment);
                        storedAttachments.add(attachment);
                    } catch (Exception attachEx) {
                        log.warn("Could not attach file '{}' to complaint {}: {}",
                                file.getOriginalFilename(), saved.getComplaintNumber(), attachEx.getMessage());
                    }
                }
            }
        }

        // Set primary evidence URL if available
        if (!storedAttachments.isEmpty()) {
            saved.setEvidenceUrl(storedAttachments.get(0).getFileUrl());
            try {
                complaintDao.save(saved);
            } catch (Exception ignored) {}
        }

        log.info("Saved unified complaint {} for citizen {} with department {}",
                saved.getComplaintNumber(), freshUser.getUsername(), dept != null ? dept.getCode() : "NONE");

        // 6. Generate and save persistent digital acknowledgement receipt
        String receiptNumber = recordNumber.replace("CP-", "RCP-");
        ComplaintReceipt receipt = new ComplaintReceipt();
        receipt.setId(UUID.randomUUID());
        receipt.setReceiptNumber(receiptNumber);
        receipt.setComplaintId(saved.getId());
        receipt.setComplaintNumber(recordNumber);
        receipt.setCitizenId(freshUser.getId());
        receipt.setCitizenName(freshUser.getFullName());
        receipt.setCitizenPhone(saved.getCitizenPhone());
        receipt.setTitle(saved.getTitle());
        receipt.setCategory(category);
        receipt.setDepartmentCode(dept != null ? dept.getCode() : "PWD");
        receipt.setDepartmentName(dept != null ? dept.getName() : "Public Works Department");
        receipt.setPriority(priority);
        receipt.setSlaMinutes(slaMinutes);
        receipt.setSlaDeadline(slaDeadline);
        receipt.setSubmittedAt(now);
        receipt.setQrData(recordNumber + "|" + receiptNumber + "|" + (dept != null ? dept.getCode() : "PWD") + "|" + priority);
        receipt.setIssuedAt(now);
        try {
            complaintDao.saveReceipt(receipt);
        } catch (Exception ignored) {}

        // 7. Audit initial status history
        ComplaintStatusHistory history = new ComplaintStatusHistory();
        history.setId(UUID.randomUUID());
        history.setComplaintId(saved.getId());
        history.setFromStatus(null);
        history.setToStatus("SUBMITTED");
        history.setChangedBy(freshUser.getId());
        history.setChangedByName(freshUser.getFullName());
        history.setChangedByRole(freshUser.getRoleName() != null ? freshUser.getRoleName() : "CITIZEN");
        history.setRemarks("Citizen grievance submitted successfully. Official acknowledgement issued: " + receiptNumber);
        history.setCreatedAt(now);
        try {
            complaintDao.saveStatusHistory(history);
        } catch (Exception ignored) {}

        // 8. Dispatch Notifications
        if (notificationService != null) {
            try {
                notificationService.createComplaintNotification(
                        freshUser,
                        "CITIZEN",
                        "Grievance Lodged: " + recordNumber,
                        "Your grievance '" + saved.getTitle() + "' has been logged under "
                                + (dept != null ? dept.getName() : "Municipal Department")
                                + ". Receipt: " + receiptNumber,
                        "COMPLAINT_REGISTERED",
                        "INFO",
                        saved.getId(),
                        recordNumber,
                        NotificationChannel.IN_APP
                );

                // Notify department officers
                notificationService.createComplaintNotification(
                        null,
                        "DEPARTMENT_OFFICER",
                        "New " + priority + " Complaint: " + recordNumber,
                        "Grievance '" + saved.getTitle() + "' in " + (dept != null ? dept.getName() : "Department") + " requires review.",
                        "NEW_COMPLAINT",
                        "CRITICAL".equalsIgnoreCase(priority) ? "CRITICAL" : "MEDIUM",
                        saved.getId(),
                        recordNumber,
                        NotificationChannel.IN_APP
                );
            } catch (Exception notifEx) {
                log.warn("Failed to dispatch notifications for complaint {}: {}", recordNumber, notifEx.getMessage());
            }
        }

        // 9. Push real-time WebSocket update
        if (webSocketPublisher != null) {
            try {
                Map<String, Object> event = new HashMap<>();
                event.put("eventType", "NEW_COMPLAINT");
                event.put("complaintId", saved.getId());
                event.put("complaintNumber", recordNumber);
                event.put("priority", priority);
                event.put("departmentCode", dept != null ? dept.getCode() : "PWD");
                event.put("status", "SUBMITTED");
                event.put("timestamp", now.toString());
                webSocketPublisher.publishNotification(event);
            } catch (Exception wsEx) {
                log.warn("WebSocket publish warning for complaint {}: {}", recordNumber, wsEx.getMessage());
            }
        }

        return buildEnrichedResponse(saved, freshUser);
    }

    @Override
    @Transactional
    public ComplaintResponse verifyComplaint(UUID complaintId, VerifyComplaintRequest request, User verifier) {
        if (verifier == null) {
            throw new ForbiddenException("Authentication required for complaint verification.");
        }

        // RBAC validation: Only DEPARTMENT_OFFICER and ADMIN can verify complaints
        boolean isAuthorized = verifier.getRoles() != null && verifier.getRoles().stream()
                .map(Role::getName)
                .anyMatch(roleName -> "ROLE_ADMIN".equalsIgnoreCase(roleName)
                        || "ADMIN".equalsIgnoreCase(roleName)
                        || "ROLE_DEPARTMENT_OFFICER".equalsIgnoreCase(roleName)
                        || "DEPARTMENT_OFFICER".equalsIgnoreCase(roleName));

        if (!isAuthorized) {
            throw new ForbiddenException("Access denied: Only authorized Department Officers and Admins can verify citizen complaints.");
        }

        Complaint complaint = complaintDao.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        // Prevent self-verification: Citizens cannot verify their own complaints
        if (complaint.getCitizen() != null && complaint.getCitizen().getId().equals(verifier.getId())) {
            throw new ForbiddenException("Conflict of interest: You cannot verify your own civic complaints.");
        }

        String previousStatus = complaint.getComplaintVerificationStatus();
        String newStatus = request.getVerificationStatus().trim().toUpperCase();

        complaintDao.updateVerificationStatus(complaintId, newStatus, verifier.getId(), request.getNotes());

        // Audit the verification decision
        ComplaintVerificationHistory history = new ComplaintVerificationHistory();
        history.setId(UUID.randomUUID());
        history.setComplaintId(complaintId);
        history.setCitizenId(complaint.getCitizen() != null ? complaint.getCitizen().getId() : null);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setDecisionReason(request.getNotes() != null && !request.getNotes().isBlank() ? request.getNotes() : "Verification updated to " + newStatus);
        history.setVerifiedBy(verifier.getId());
        history.setVerifiedByName(verifier.getFullName() != null ? verifier.getFullName() : verifier.getUsername());
        history.setCreatedAt(Instant.now());
        complaintDao.saveVerificationHistory(history);

        // Audit in lifecycle status history as well
        ComplaintStatusHistory statusHistory = new ComplaintStatusHistory();
        statusHistory.setId(UUID.randomUUID());
        statusHistory.setComplaintId(complaintId);
        statusHistory.setFromStatus(complaint.getStatus());
        statusHistory.setToStatus(complaint.getStatus());
        statusHistory.setChangedBy(verifier.getId());
        statusHistory.setChangedByName(verifier.getFullName());
        statusHistory.setChangedByRole(verifier.getRoleName() != null ? verifier.getRoleName() : "DEPARTMENT_OFFICER");
        statusHistory.setRemarks("Complaint marked as " + newStatus + ". Notes: " + request.getNotes());
        statusHistory.setCreatedAt(Instant.now());
        complaintDao.saveStatusHistory(statusHistory);

        // Recalculate citizen genuine complaint reputation
        User citizen = null;
        if (complaint.getCitizen() != null && complaint.getCitizen().getId() != null) {
            UUID citizenId = complaint.getCitizen().getId();
            int genuineCount = complaintDao.countGenuineComplaintsByCitizen(citizenId);
            boolean isVerifiedCitizen = genuineCount >= VERIFIED_CITIZEN_THRESHOLD;

            userDao.updateReputation(citizenId, genuineCount, isVerifiedCitizen);
            citizen = userDao.findById(citizenId).orElse(null);

            log.info("Recalculated reputation for citizen {}: genuineComplaints={}, isVerifiedCitizen={}",
                    citizenId, genuineCount, isVerifiedCitizen);

            // Notify citizen of verification decision
            if (notificationService != null) {
                try {
                    String badgeMsg = isVerifiedCitizen ? " Congratulations! You have unlocked the Verified Citizen badge." : "";
                    notificationService.createComplaintNotification(
                            citizen,
                            "CITIZEN",
                            "Complaint Verification: " + newStatus,
                            "Your complaint " + complaint.getComplaintNumber() + " has been reviewed and verified as " + newStatus + "." + badgeMsg,
                            "COMPLAINT_VERIFIED",
                            "GENUINE".equalsIgnoreCase(newStatus) ? "INFO" : "MEDIUM",
                            complaintId,
                            complaint.getComplaintNumber(),
                            NotificationChannel.IN_APP
                    );
                } catch (Exception notifEx) {
                    log.warn("Failed to notify citizen on verification: {}", notifEx.getMessage());
                }
            }
        }

        Complaint updated = complaintDao.findById(complaintId).orElse(complaint);
        return buildEnrichedResponse(updated, citizen);
    }

    @Override
    @Transactional
    public ComplaintResponse assignComplaint(UUID complaintId, AssignComplaintRequest request, User assigner) {
        if (assigner == null) {
            throw new ForbiddenException("Authentication required.");
        }

        Complaint complaint = complaintDao.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found: " + complaintId));

        User assignee = userDao.findById(request.getAssignedToUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Assignee user not found: " + request.getAssignedToUserId()));

        Department dept = null;
        if (request.getDepartmentId() != null) {
            dept = departmentRepository.findById(request.getDepartmentId()).orElse(complaint.getDepartment());
            complaint.setDepartment(dept);
        }

        String fromStatus = complaint.getStatus();
        complaint.setAssignedTo(assignee);
        if ("SUBMITTED".equalsIgnoreCase(complaint.getStatus())) {
            complaint.setStatus("ASSIGNED");
        }
        complaint.setUpdatedAt(Instant.now());
        Complaint saved = complaintDao.save(complaint);

        // Status history audit
        ComplaintStatusHistory history = new ComplaintStatusHistory();
        history.setId(UUID.randomUUID());
        history.setComplaintId(complaintId);
        history.setFromStatus(fromStatus);
        history.setToStatus(saved.getStatus());
        history.setChangedBy(assigner.getId());
        history.setChangedByName(assigner.getFullName());
        history.setChangedByRole(assigner.getRoleName() != null ? assigner.getRoleName() : "DEPARTMENT_OFFICER");
        history.setRemarks("Assigned to " + assignee.getFullName() + (request.getNotes() != null ? ". Notes: " + request.getNotes() : ""));
        history.setCreatedAt(Instant.now());
        complaintDao.saveStatusHistory(history);

        // Notify field worker
        try {
            notificationService.createComplaintNotification(
                    assignee,
                    "FIELD_WORKER",
                    "New Task Assigned: " + complaint.getComplaintNumber(),
                    "You have been assigned to resolve complaint '" + complaint.getTitle() + "'.",
                    "TASK_ASSIGNED",
                    "HIGH",
                    complaintId,
                    complaint.getComplaintNumber(),
                    NotificationChannel.IN_APP
            );

            // Notify citizen
            if (complaint.getCitizen() != null) {
                User citizen = userDao.findById(complaint.getCitizen().getId()).orElse(null);
                if (citizen != null) {
                    notificationService.createComplaintNotification(
                            citizen,
                            "CITIZEN",
                            "Complaint Assigned: " + complaint.getComplaintNumber(),
                            "Your complaint has been assigned to " + assignee.getFullName() + " for field action.",
                            "COMPLAINT_ASSIGNED",
                            "INFO",
                            complaintId,
                            complaint.getComplaintNumber(),
                            NotificationChannel.IN_APP
                    );
                }
            }
        } catch (Exception ex) {
            log.warn("Notification dispatch failed for assignment: {}", ex.getMessage());
        }

        return buildEnrichedResponse(saved, complaint.getCitizen());
    }

    @Override
    @Transactional
    public ComplaintResponse updateProgress(UUID complaintId, UpdateComplaintProgressRequest request, List<MultipartFile> evidenceFiles, User updater) {
        if (updater == null) {
            throw new ForbiddenException("Authentication required.");
        }

        Complaint complaint = complaintDao.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found: " + complaintId));

        String fromStatus = complaint.getStatus();
        String toStatus = request.getStatus() != null && !request.getStatus().isBlank()
                ? request.getStatus().trim().toUpperCase()
                : ("SUBMITTED".equalsIgnoreCase(complaint.getStatus()) ? "IN_PROGRESS" : complaint.getStatus());

        complaint.setStatus(toStatus);
        complaint.setUpdatedAt(Instant.now());

        // Store progress attachments if any
        if (evidenceFiles != null && !evidenceFiles.isEmpty()) {
            for (MultipartFile file : evidenceFiles) {
                if (file != null && !file.isEmpty()) {
                    FileStorageService.StoredFileResult result = fileStorageService.storeFile(file);
                    ComplaintAttachment att = new ComplaintAttachment();
                    att.setId(UUID.randomUUID());
                    att.setComplaintId(complaintId);
                    att.setFileName(result.getFileName());
                    att.setOriginalFileName(result.getOriginalFileName());
                    att.setFileType(result.getContentType());
                    att.setFileSize(result.getFileSize());
                    att.setFilePath(result.getFilePath());
                    att.setFileUrl("/api/complaints/attachments/" + att.getId());
                    att.setUploadedBy(updater);
                    att.setCreatedAt(Instant.now());
                    complaintDao.saveAttachment(att);
                }
            }
        }

        Complaint saved = complaintDao.save(complaint);

        // Status history audit
        ComplaintStatusHistory history = new ComplaintStatusHistory();
        history.setId(UUID.randomUUID());
        history.setComplaintId(complaintId);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setChangedBy(updater.getId());
        history.setChangedByName(updater.getFullName());
        history.setChangedByRole(updater.getRoleName() != null ? updater.getRoleName() : "FIELD_WORKER");
        history.setRemarks(request.getProgressNotes() != null && !request.getProgressNotes().isBlank()
                ? request.getProgressNotes().trim()
                : "Work progress updated by field crew.");
        history.setCreatedAt(Instant.now());
        complaintDao.saveStatusHistory(history);

        // Notify citizen
        try {
            if (complaint.getCitizen() != null) {
                User citizen = userDao.findById(complaint.getCitizen().getId()).orElse(null);
                if (citizen != null) {
                    notificationService.createComplaintNotification(
                            citizen,
                            "CITIZEN",
                            "Work Update on " + complaint.getComplaintNumber(),
                            "Status updated to " + toStatus + ": " + history.getRemarks(),
                            "PROGRESS_UPDATE",
                            "INFO",
                            complaintId,
                            complaint.getComplaintNumber(),
                            NotificationChannel.IN_APP
                    );
                }
            }
        } catch (Exception ex) {
            log.warn("Notification dispatch failed for progress update: {}", ex.getMessage());
        }

        return buildEnrichedResponse(saved, complaint.getCitizen());
    }

    @Override
    @Transactional
    public ComplaintResponse submitResolution(UUID complaintId, String resolutionNotes, List<MultipartFile> evidenceFiles, User resolver) {
        if (resolver == null) {
            throw new ForbiddenException("Authentication required.");
        }

        Complaint complaint = complaintDao.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found: " + complaintId));

        String fromStatus = complaint.getStatus();
        complaint.setStatus("RESOLVED");
        complaint.setResolutionNotes(resolutionNotes != null ? resolutionNotes.trim() : "Resolution completed.");
        complaint.setResolvedAt(Instant.now());
        complaint.setSlaStatus("RESOLVED");
        complaint.setUpdatedAt(Instant.now());

        // Upload resolution evidence photos
        if (evidenceFiles != null && !evidenceFiles.isEmpty()) {
            for (MultipartFile file : evidenceFiles) {
                if (file != null && !file.isEmpty()) {
                    FileStorageService.StoredFileResult result = fileStorageService.storeFile(file);
                    ComplaintAttachment att = new ComplaintAttachment();
                    att.setId(UUID.randomUUID());
                    att.setComplaintId(complaintId);
                    att.setFileName(result.getFileName());
                    att.setOriginalFileName(result.getOriginalFileName());
                    att.setFileType(result.getContentType());
                    att.setFileSize(result.getFileSize());
                    att.setFilePath(result.getFilePath());
                    att.setFileUrl("/api/complaints/attachments/" + att.getId());
                    att.setUploadedBy(resolver);
                    att.setCreatedAt(Instant.now());
                    complaintDao.saveAttachment(att);
                    if (complaint.getResolutionEvidenceUrl() == null) {
                        complaint.setResolutionEvidenceUrl(att.getFileUrl());
                    }
                }
            }
        }

        Complaint saved = complaintDao.save(complaint);

        // Status history audit
        ComplaintStatusHistory history = new ComplaintStatusHistory();
        history.setId(UUID.randomUUID());
        history.setComplaintId(complaintId);
        history.setFromStatus(fromStatus);
        history.setToStatus("RESOLVED");
        history.setChangedBy(resolver.getId());
        history.setChangedByName(resolver.getFullName());
        history.setChangedByRole(resolver.getRoleName() != null ? resolver.getRoleName() : "FIELD_WORKER");
        history.setRemarks("Resolution submitted: " + complaint.getResolutionNotes());
        history.setCreatedAt(Instant.now());
        complaintDao.saveStatusHistory(history);

        // Notify citizen & officers
        try {
            if (complaint.getCitizen() != null) {
                User citizen = userDao.findById(complaint.getCitizen().getId()).orElse(null);
                if (citizen != null) {
                    notificationService.createComplaintNotification(
                            citizen,
                            "CITIZEN",
                            "Grievance Resolved: " + complaint.getComplaintNumber(),
                            "Resolution submitted: " + complaint.getResolutionNotes() + ". Please review.",
                            "COMPLAINT_RESOLVED",
                            "INFO",
                            complaintId,
                            complaint.getComplaintNumber(),
                            NotificationChannel.IN_APP
                    );
                }
            }
        } catch (Exception ex) {
            log.warn("Notification dispatch failed for resolution submit: {}", ex.getMessage());
        }

        return buildEnrichedResponse(saved, complaint.getCitizen());
    }

    @Override
    @Transactional
    public ComplaintResponse verifyResolution(UUID complaintId, String decision, String notes, User verifier) {
        if (verifier == null) {
            throw new ForbiddenException("Authentication required.");
        }

        Complaint complaint = complaintDao.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found: " + complaintId));

        String fromStatus = complaint.getStatus();
        String toStatus = "ACCEPTED".equalsIgnoreCase(decision) ? "CLOSED" : "IN_PROGRESS";

        complaint.setStatus(toStatus);
        if ("CLOSED".equalsIgnoreCase(toStatus)) {
            complaint.setClosedAt(Instant.now());
        }
        complaint.setUpdatedAt(Instant.now());
        Complaint saved = complaintDao.save(complaint);

        // Status history audit
        ComplaintStatusHistory history = new ComplaintStatusHistory();
        history.setId(UUID.randomUUID());
        history.setComplaintId(complaintId);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setChangedBy(verifier.getId());
        history.setChangedByName(verifier.getFullName());
        history.setChangedByRole(verifier.getRoleName() != null ? verifier.getRoleName() : "DEPARTMENT_OFFICER");
        history.setRemarks("Resolution verification decision: " + decision + (notes != null ? ". Notes: " + notes : ""));
        history.setCreatedAt(Instant.now());
        complaintDao.saveStatusHistory(history);

        // Notify citizen
        try {
            if (complaint.getCitizen() != null) {
                User citizen = userDao.findById(complaint.getCitizen().getId()).orElse(null);
                if (citizen != null) {
                    notificationService.createComplaintNotification(
                            citizen,
                            "CITIZEN",
                            "Complaint Status Update: " + toStatus,
                            "Officer verified resolution as " + decision + ". Grievance is now " + toStatus + ".",
                            "RESOLUTION_VERIFIED",
                            "INFO",
                            complaintId,
                            complaint.getComplaintNumber(),
                            NotificationChannel.IN_APP
                    );
                }
            }
        } catch (Exception ex) {
            log.warn("Notification dispatch failed for resolution verification: {}", ex.getMessage());
        }

        return buildEnrichedResponse(saved, complaint.getCitizen());
    }

    @Override
    @Transactional
    public ComplaintResponse changePriority(UUID complaintId, ChangePriorityRequest request, User modifier) {
        if (modifier == null) {
            throw new ForbiddenException("Authentication required.");
        }

        Complaint complaint = complaintDao.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found: " + complaintId));

        String oldPriority = complaint.getPriority();
        String newPriority = request.getPriority().trim().toUpperCase();
        int newSlaMinutes = getSlaMinutesForPriority(newPriority);

        complaint.setPriority(newPriority);
        complaint.setSlaMinutes(newSlaMinutes);
        if (complaint.getCreatedAt() != null) {
            complaint.setSlaDeadline(complaint.getCreatedAt().plus(Duration.ofMinutes(newSlaMinutes)));
        }
        complaint.setUpdatedAt(Instant.now());
        Complaint saved = complaintDao.save(complaint);

        // Status history audit
        ComplaintStatusHistory history = new ComplaintStatusHistory();
        history.setId(UUID.randomUUID());
        history.setComplaintId(complaintId);
        history.setFromStatus(complaint.getStatus());
        history.setToStatus(complaint.getStatus());
        history.setChangedBy(modifier.getId());
        history.setChangedByName(modifier.getFullName());
        history.setChangedByRole(modifier.getRoleName() != null ? modifier.getRoleName() : "ADMIN");
        history.setRemarks("Priority adjusted from " + oldPriority + " to " + newPriority + ". Reason: " + request.getReason());
        history.setCreatedAt(Instant.now());
        complaintDao.saveStatusHistory(history);

        return buildEnrichedResponse(saved, complaint.getCitizen());
    }

    @Override
    @Transactional
    public ComplaintResponse closeOrReopen(UUID complaintId, String action, String reason, User user) {
        if (user == null) {
            throw new ForbiddenException("Authentication required.");
        }

        Complaint complaint = complaintDao.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found: " + complaintId));

        String fromStatus = complaint.getStatus();
        String toStatus;
        if ("REOPEN".equalsIgnoreCase(action)) {
            toStatus = "IN_PROGRESS";
            complaint.setClosedAt(null);
        } else {
            toStatus = "CLOSED";
            complaint.setClosedAt(Instant.now());
        }

        complaint.setStatus(toStatus);
        complaint.setUpdatedAt(Instant.now());
        Complaint saved = complaintDao.save(complaint);

        ComplaintStatusHistory history = new ComplaintStatusHistory();
        history.setId(UUID.randomUUID());
        history.setComplaintId(complaintId);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setChangedBy(user.getId());
        history.setChangedByName(user.getFullName());
        history.setChangedByRole(user.getRoleName() != null ? user.getRoleName() : "ADMIN");
        history.setRemarks("Grievance " + action.toLowerCase() + "ed. Reason: " + (reason != null ? reason : "Administrative action"));
        history.setCreatedAt(Instant.now());
        complaintDao.saveStatusHistory(history);

        return buildEnrichedResponse(saved, complaint.getCitizen());
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintById(UUID complaintId) {
        Complaint complaint = complaintDao.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        User citizen = null;
        if (complaint.getCitizen() != null && complaint.getCitizen().getId() != null) {
            citizen = userDao.findById(complaint.getCitizen().getId()).orElse(null);
        }

        return buildEnrichedResponse(complaint, citizen);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getComplaints(String status, String category, String verificationStatus,
                                                UUID citizenId, UUID departmentId, UUID assignedToUserId,
                                                String priority, String search, String slaStatus, Boolean unassigned,
                                                int page, int size) {
        int limit = size > 0 ? size : 50;
        int offset = Math.max(0, page) * limit;

        List<Complaint> complaints = complaintDao.findAll(status, category, verificationStatus,
                citizenId, departmentId, assignedToUserId, priority, search, slaStatus, unassigned, limit, offset);

        return complaints.stream().map(c -> {
            User citizen = null;
            if (c.getCitizen() != null && c.getCitizen().getId() != null) {
                citizen = userDao.findById(c.getCitizen().getId()).orElse(null);
            }
            return buildEnrichedResponse(c, citizen);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getComplaints(String status, String category, String verificationStatus,
                                                UUID citizenId, UUID departmentId, UUID assignedToUserId,
                                                String priority, String search, int page, int size) {
        return getComplaints(status, category, verificationStatus, citizenId, departmentId, assignedToUserId, priority, search, null, null, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public int countComplaints(String status, String category, String verificationStatus,
                               UUID citizenId, UUID departmentId, UUID assignedToUserId,
                               String priority, String search, String slaStatus, Boolean unassigned) {
        return complaintDao.countAll(status, category, verificationStatus, citizenId, departmentId, assignedToUserId, priority, search, slaStatus, unassigned);
    }

    @Override
    @Transactional(readOnly = true)
    public int countComplaints(String status, String category, String verificationStatus,
                               UUID citizenId, UUID departmentId, UUID assignedToUserId,
                               String priority, String search) {
        return countComplaints(status, category, verificationStatus, citizenId, departmentId, assignedToUserId, priority, search, null, null);
    }

    @Override
    @Transactional
    public int evaluateComplaintSlaDeadlines() {
        List<Complaint> breached = complaintDao.findAll(null, null, null, null, null, null, null, null, "BREACHED", null, 100, 0);
        int updated = 0;
        Instant now = Instant.now();
        for (Complaint c : breached) {
            if (!"BREACHED".equalsIgnoreCase(c.getSlaStatus())) {
                c.setSlaStatus("BREACHED");
                c.setUpdatedAt(now);
                complaintDao.save(c);

                ComplaintStatusHistory history = new ComplaintStatusHistory();
                history.setId(UUID.randomUUID());
                history.setComplaintId(c.getId());
                history.setFromStatus(c.getStatus());
                history.setToStatus(c.getStatus());
                history.setChangedByName("SLA Monitor Daemon");
                history.setChangedByRole("SYSTEM");
                history.setRemarks("Resolution deadline passed (" + c.getSlaDeadline() + "). Marked SLA BREACHED.");
                history.setCreatedAt(now);
                try { complaintDao.saveStatusHistory(history); } catch (Exception ignored) {}

                if (notificationService != null) {
                    try {
                        notificationService.createComplaintNotification(
                                null,
                                "DEPARTMENT_OFFICER",
                                "SLA BREACH ALERT: " + c.getComplaintNumber(),
                                "Complaint '" + c.getTitle() + "' has exceeded guaranteed resolution time.",
                                "SLA_BREACH",
                                "CRITICAL",
                                c.getId(),
                                c.getComplaintNumber(),
                                NotificationChannel.IN_APP
                        );
                    } catch (Exception ignored) {}
                }
                updated++;
            }
        }
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public CitizenReputationResponse getCitizenReputation(UUID citizenId) {
        User citizen = userDao.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen not found with id: " + citizenId));

        int genuineCount = complaintDao.countGenuineComplaintsByCitizen(citizenId);
        boolean isVerified = genuineCount >= VERIFIED_CITIZEN_THRESHOLD;

        return new CitizenReputationResponse(
                citizen.getId(),
                citizen.getUsername(),
                citizen.getFullName(),
                genuineCount,
                isVerified
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintVerificationHistory> getVerificationHistory(UUID complaintId) {
        return complaintDao.findVerificationHistory(complaintId);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintReceiptDTO getReceipt(UUID complaintId) {
        return complaintDao.findReceiptByComplaintId(complaintId)
                .map(ComplaintReceiptDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("No receipt found for complaint: " + complaintId));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getReceiptPdf(UUID complaintId) {
        ComplaintReceipt r = complaintDao.findReceiptByComplaintId(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found for complaint: " + complaintId));

        String submittedDate = r.getSubmittedAt() != null ? RECEIPT_DATE_FORMAT.format(r.getSubmittedAt()) : "N/A";
        String slaDeadline = r.getSlaDeadline() != null ? RECEIPT_DATE_FORMAT.format(r.getSlaDeadline()) : "N/A";

        String content = """
                ================================================================================
                CIVICPULSE SMART CITY COMMAND CENTRE
                OFFICIAL CITIZEN GRIEVANCE ACKNOWLEDGEMENT RECEIPT
                ================================================================================

                Receipt Number     : %s
                Complaint Number   : %s
                Filing Date & Time : %s
                SLA Resolution Due : %s (%s Priority - %d Minutes)

                --- CITIZEN PARTICULARS ---
                Citizen Name       : %s
                Registered Mobile  : %s
                Filing Status      : OFFICIALLY REGISTERED

                --- GRIEVANCE PARTICULARS ---
                Title              : %s
                Category           : %s
                Assigned Department: %s (%s)

                --- OFFICIAL SLA GUARANTEE & TRACKING ---
                Target SLA Window  : %d Minutes
                Tracking Reference : %s
                Digital QR Payload : %s

                Notice:
                This document serves as legal proof of submission into the CivicPulse
                Command Centre. You can track real-time field progress on your Citizen Portal.
                ================================================================================
                """.formatted(
                r.getReceiptNumber(),
                r.getComplaintNumber(),
                submittedDate,
                slaDeadline,
                r.getPriority(),
                r.getSlaMinutes(),
                r.getCitizenName(),
                r.getCitizenPhone(),
                r.getTitle(),
                r.getCategory(),
                r.getDepartmentName(),
                r.getDepartmentCode(),
                r.getSlaMinutes(),
                r.getComplaintNumber(),
                r.getQrData()
        );

        return content.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintAttachment getAttachment(UUID attachmentId) {
        return complaintDao.findAttachmentById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found: " + attachmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminComplaintStatsDTO getAdminStats() {
        return complaintDao.getAdminComplaintStats();
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentComplaintStatsDTO getDepartmentStats(UUID departmentId, User officer) {
        UUID effectiveDeptId = departmentId;
        if (effectiveDeptId == null && officer != null && officer.getDepartment() != null) {
            effectiveDeptId = officer.getDepartment().getId();
        }
        if (effectiveDeptId == null) {
            throw new IllegalArgumentException("Department ID must be provided or linked to the officer profile.");
        }
        return complaintDao.getDepartmentComplaintStats(effectiveDeptId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentComplaintStatsDTO> getAllDepartmentStats() {
        return complaintDao.getAllDepartmentStats();
    }

    private ComplaintResponse buildEnrichedResponse(Complaint complaint, User citizen) {
        ExternalGrievanceTrackingResponse externalTracking = getSafeExternalTracking(complaint.getId());
        ComplaintResponse resp = new ComplaintResponse(complaint, citizen, externalTracking);

        // Populate attachments
        try {
            List<ComplaintAttachment> atts = complaintDao.findAttachmentsByComplaintId(complaint.getId());
            if (atts != null) {
                resp.setAttachments(atts.stream().map(ComplaintAttachmentDTO::new).collect(Collectors.toList()));
            }
        } catch (Exception ignored) {}

        // Populate receipt
        try {
            complaintDao.findReceiptByComplaintId(complaint.getId())
                    .ifPresent(r -> resp.setReceipt(new ComplaintReceiptDTO(r)));
        } catch (Exception ignored) {}

        // Populate status history
        try {
            List<ComplaintStatusHistory> history = complaintDao.findStatusHistoryByComplaintId(complaint.getId());
            if (history != null) {
                resp.setStatusHistory(history.stream().map(ComplaintStatusHistoryDTO::new).collect(Collectors.toList()));
            }
        } catch (Exception ignored) {}

        return resp;
    }

    private ExternalGrievanceTrackingResponse getSafeExternalTracking(UUID complaintId) {
        if (grievanceService == null) return null;
        try {
            return grievanceService.findExternalTracking(complaintId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
