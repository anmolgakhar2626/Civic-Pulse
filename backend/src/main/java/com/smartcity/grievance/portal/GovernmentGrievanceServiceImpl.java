package com.smartcity.grievance.portal;

import com.smartcity.common.ResourceNotFoundException;
import com.smartcity.complaint.Complaint;
import com.smartcity.complaint.ComplaintDao;
import com.smartcity.grievance.portal.dto.ExternalGrievanceTrackingResponse;
import com.smartcity.grievance.portal.dto.ForwardGrievanceRequest;
import com.smartcity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GovernmentGrievanceServiceImpl implements GovernmentGrievanceService {

    private static final Logger log = LoggerFactory.getLogger(GovernmentGrievanceServiceImpl.class);

    private final GovernmentGrievanceDao grievanceDao;
    private final GovernmentPortalRegistry portalRegistry;
    private final ComplaintDao complaintDao;

    public GovernmentGrievanceServiceImpl(GovernmentGrievanceDao grievanceDao,
                                         GovernmentPortalRegistry portalRegistry,
                                         ComplaintDao complaintDao) {
        this.grievanceDao = grievanceDao;
        this.portalRegistry = portalRegistry;
        this.complaintDao = complaintDao;
    }

    @Override
    @Transactional
    public ExternalGrievanceTrackingResponse forwardToGovernmentPortal(UUID complaintId, ForwardGrievanceRequest request, User requester) {
        Complaint complaint = complaintDao.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        String portalCode = request != null && request.getPortalCode() != null ? request.getPortalCode().trim().toUpperCase() : "CPGRAMS_SANDBOX";

        GovernmentPortal portal = grievanceDao.findPortalByCode(portalCode)
                .orElseThrow(() -> new ResourceNotFoundException("Government portal not configured: " + portalCode));

        GovernmentPortalAdapter adapter = portalRegistry.getAdapter(portalCode)
                .orElseThrow(() -> new IllegalStateException("Adapter not registered for portal code: " + portalCode));

        // Idempotency Key calculation: complaintId + portalCode
        String idempotencyKey = "IDEMP-" + portalCode + "-" + complaintId;

        // Check if already submitted (idempotency protection)
        Optional<ExternalGrievance> existing = grievanceDao.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("[GOV PORTAL] Complaint {} already submitted to portal {}. Returning existing record (Idempotent replay).",
                    complaint.getComplaintNumber(), portalCode);
            ExternalGrievance eg = existing.get();
            return buildTrackingResponse(eg, portal, adapter.isSandbox());
        }

        // Call portal adapter
        String simulateFailure = request != null ? request.getSimulateFailure() : null;
        GrievanceSubmissionResult submissionResult;
        try {
            submissionResult = adapter.submitGrievance(complaint, idempotencyKey, simulateFailure);
        } catch (Exception e) {
            log.error("[GOV PORTAL] Error communicating with portal {}: {}", portalCode, e.getMessage(), e);
            grievanceDao.saveSyncLog(new GrievanceSyncLog(
                    null, portalCode, "SUBMISSION", "FAILED", idempotencyKey,
                    "ComplaintId: " + complaintId, null, e.getMessage(), 1
            ));
            throw new RuntimeException("Government Portal Gateway Communication Failure: " + e.getMessage());
        }

        // Handle submission result
        if (submissionResult.isTimeout()) {
            grievanceDao.saveSyncLog(new GrievanceSyncLog(
                    null, portalCode, "SUBMISSION", "TIMEOUT", idempotencyKey,
                    "ComplaintId: " + complaintId, null, submissionResult.getErrorMessage(), 1
            ));
            throw new RuntimeException("Government Portal connection timed out: " + submissionResult.getErrorMessage());
        }

        if (!submissionResult.isSuccess()) {
            grievanceDao.saveSyncLog(new GrievanceSyncLog(
                    null, portalCode, "SUBMISSION", "FAILED", idempotencyKey,
                    "ComplaintId: " + complaintId, null, submissionResult.getErrorMessage(), 1
            ));
            throw new RuntimeException("Government Portal submission rejected: " + submissionResult.getErrorMessage());
        }

        // Create ExternalGrievance record
        ExternalGrievance eg = new ExternalGrievance();
        eg.setId(UUID.randomUUID());
        eg.setComplaintId(complaint.getId());
        eg.setPortalId(portal.getId());
        eg.setPortalCode(portalCode);
        eg.setAcknowledgementNumber(submissionResult.getAcknowledgementNumber());
        eg.setIdempotencyKey(idempotencyKey);
        eg.setExternalStatus(submissionResult.getExternalStatus());
        eg.setStatusDescription(submissionResult.getStatusDescription());
        eg.setOfficerInCharge(submissionResult.getOfficerInCharge());
        eg.setTargetResolutionDate(submissionResult.getTargetResolutionDate());
        eg.setSubmittedAt(Instant.now());
        eg.setLastSyncedAt(Instant.now());
        eg.setCreatedAt(Instant.now());
        eg.setUpdatedAt(Instant.now());

        ExternalGrievance saved = grievanceDao.saveExternalGrievance(eg);

        // Record initial status history
        GrievanceStatusHistory initialHistory = new GrievanceStatusHistory(
                saved.getId(),
                submissionResult.getExternalStatus(),
                submissionResult.getStatusDescription(),
                submissionResult.getOfficerInCharge(),
                "{\"portal\":\"" + portalCode + "\",\"action\":\"INITIAL_REGISTRATION\"}"
        );
        grievanceDao.saveStatusHistory(initialHistory);

        // Record acknowledgement document
        if (submissionResult.getDocumentReference() != null) {
            AcknowledgementDocument doc = new AcknowledgementDocument(
                    saved.getId(),
                    submissionResult.getDocumentReference(),
                    "ACKNOWLEDGEMENT_RECEIPT",
                    "Official Portal Acknowledgement Receipt (" + submissionResult.getAcknowledgementNumber() + ")",
                    submissionResult.getDocumentUrl(),
                    1024L * 15 // ~15 KB receipt size
            );
            grievanceDao.saveDocument(doc);
        }

        // Record successful sync log
        grievanceDao.saveSyncLog(new GrievanceSyncLog(
                saved.getId(), portalCode, "SUBMISSION", "SUCCESS", idempotencyKey,
                "ComplaintId: " + complaintId,
                "AckNumber: " + submissionResult.getAcknowledgementNumber() + ", Status: " + submissionResult.getExternalStatus(),
                null, 0
        ));

        log.info("[GOV PORTAL] Successfully registered complaint {} on portal {}. Ack: {}",
                complaint.getComplaintNumber(), portalCode, submissionResult.getAcknowledgementNumber());

        return buildTrackingResponse(saved, portal, adapter.isSandbox());
    }

    @Override
    @Transactional(readOnly = true)
    public ExternalGrievanceTrackingResponse getExternalTracking(UUID complaintId) {
        ExternalGrievance eg = grievanceDao.findByComplaintId(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("No external government grievance tracking found for complaint id: " + complaintId));

        GovernmentPortal portal = grievanceDao.findPortalByCode(eg.getPortalCode()).orElse(null);
        boolean isSandbox = portal != null ? portal.isSandbox() : true;

        return buildTrackingResponse(eg, portal, isSandbox);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExternalGrievanceTrackingResponse> findExternalTracking(UUID complaintId) {
        return grievanceDao.findByComplaintId(complaintId)
                .map(eg -> {
                    GovernmentPortal portal = grievanceDao.findPortalByCode(eg.getPortalCode()).orElse(null);
                    boolean isSandbox = portal != null ? portal.isSandbox() : true;
                    return buildTrackingResponse(eg, portal, isSandbox);
                });
    }

    @Override
    @Transactional
    public ExternalGrievanceTrackingResponse syncExternalStatus(UUID complaintId, User requester) {
        ExternalGrievance eg = grievanceDao.findByComplaintId(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("No external grievance linked to complaint: " + complaintId));

        GovernmentPortalAdapter adapter = portalRegistry.getAdapter(eg.getPortalCode())
                .orElseThrow(() -> new IllegalStateException("Adapter not registered for portal code: " + eg.getPortalCode()));

        GovernmentPortal portal = grievanceDao.findPortalByCode(eg.getPortalCode()).orElse(null);

        GrievanceStatusResult statusResult = adapter.checkStatus(eg.getAcknowledgementNumber(), eg.getExternalStatus());

        Instant now = Instant.now();
        if (statusResult.isSuccess()) {
            boolean statusChanged = !statusResult.getExternalStatus().equalsIgnoreCase(eg.getExternalStatus());
            eg.setExternalStatus(statusResult.getExternalStatus());
            eg.setStatusDescription(statusResult.getRemarks());
            eg.setLastSyncedAt(now);

            grievanceDao.updateExternalGrievanceStatus(eg.getId(), statusResult.getExternalStatus(), statusResult.getRemarks(), now);

            if (statusChanged) {
                GrievanceStatusHistory history = new GrievanceStatusHistory(
                        eg.getId(),
                        statusResult.getExternalStatus(),
                        statusResult.getRemarks(),
                        statusResult.getActionTakenBy(),
                        "{\"polled_by\":\"" + (requester != null ? requester.getUsername() : "SYSTEM") + "\"}"
                );
                grievanceDao.saveStatusHistory(history);
            }

            grievanceDao.saveSyncLog(new GrievanceSyncLog(
                    eg.getId(), eg.getPortalCode(), "STATUS_POLL", "SUCCESS", eg.getIdempotencyKey(),
                    "AckNumber: " + eg.getAcknowledgementNumber(),
                    "Status: " + statusResult.getExternalStatus(), null, 0
            ));
        } else {
            grievanceDao.saveSyncLog(new GrievanceSyncLog(
                    eg.getId(), eg.getPortalCode(), "STATUS_POLL", "FAILED", eg.getIdempotencyKey(),
                    "AckNumber: " + eg.getAcknowledgementNumber(), null,
                    statusResult.getRemarks(), 1
            ));
        }

        return buildTrackingResponse(eg, portal, adapter.isSandbox());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GovernmentPortal> getAvailablePortals() {
        return grievanceDao.findAllActivePortals();
    }

    @Override
    public byte[] generateReceiptPdf(String documentReference) {
        AcknowledgementDocument doc = grievanceDao.findDocumentByReference(documentReference)
                .orElseThrow(() -> new ResourceNotFoundException("Document reference not found: " + documentReference));

        String content = """
            ========================================================================
            GOVERNMENT OF INDIA / STATE MUNICIPAL GRIEVANCE CELL
            CENTRAL PUBLIC GRIEVANCE REDRESS AND MONITORING SYSTEM (SANDBOX)
            OFFICIAL ACKNOWLEDGEMENT RECEIPT
            ========================================================================

            Document Reference : %s
            Issued At          : %s
            Document Title     : %s

            Status             : OFFICIALLY REGISTERED & VERIFIED
            Jurisdiction       : Municipal Smart City Command Centre Integration

            Note: This document confirms the official electronic intake of your civic
            grievance into the designated Central/State Government Grievance Portal.
            You can monitor real-time progression using your acknowledgement tracking.
            ========================================================================
            """.formatted(doc.getDocumentReference(), doc.getIssuedAt(), doc.getDocumentTitle());

        return content.getBytes(StandardCharsets.UTF_8);
    }

    private ExternalGrievanceTrackingResponse buildTrackingResponse(ExternalGrievance eg, GovernmentPortal portal, boolean isSandbox) {
        List<GrievanceStatusHistory> timeline = grievanceDao.findStatusHistory(eg.getId());
        List<AcknowledgementDocument> documents = grievanceDao.findDocuments(eg.getId());
        String portalName = portal != null ? portal.getPortalName() : eg.getPortalCode();
        return new ExternalGrievanceTrackingResponse(eg, portalName, isSandbox, timeline, documents);
    }
}
