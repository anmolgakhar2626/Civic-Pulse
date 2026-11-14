package com.smartcity.grievance.portal;

import com.smartcity.complaint.Complaint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

/**
 * Clearly labelled Development and Testing Sandbox Adapter for Government Grievance Integration.
 * Demonstrates official API workflow (idempotency, acknowledgements, progress history, timeouts, retries)
 * without pretending to communicate with live production government services.
 */
@Component
public class SandboxGovernmentPortalAdapter implements GovernmentPortalAdapter {

    private static final Logger log = LoggerFactory.getLogger(SandboxGovernmentPortalAdapter.class);
    private final SecureRandom random = new SecureRandom();

    @Override
    public String getPortalCode() {
        return "CPGRAMS_SANDBOX";
    }

    @Override
    public String getPortalName() {
        return "Central Public Grievance Redress & Monitoring System (Sandbox Demo)";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean isSandbox() {
        return true;
    }

    @Override
    public GrievanceSubmissionResult submitGrievance(Complaint complaint, String idempotencyKey, String simulateFailure) {
        log.info("[GOV PORTAL SANDBOX] Submitting complaint {} (category: {}) to {}",
                complaint.getComplaintNumber(), complaint.getCategory(), getPortalName());

        if ("TIMEOUT".equalsIgnoreCase(simulateFailure)) {
            log.warn("[GOV PORTAL SANDBOX] Simulated API connection timeout for complaint {}", complaint.getComplaintNumber());
            return GrievanceSubmissionResult.timeout("Gateway connection timed out while contacting Government Portal Sandbox (HTTP 504)");
        }

        if ("ERROR".equalsIgnoreCase(simulateFailure)) {
            log.warn("[GOV PORTAL SANDBOX] Simulated upstream API rejection for complaint {}", complaint.getComplaintNumber());
            return GrievanceSubmissionResult.failure("Government Portal Sandbox rejected submission: Schema validation error (HTTP 422)");
        }

        // Generate official-format acknowledgement number
        int serial = 10000 + random.nextInt(90000);
        String ackNumber = "GOV-CPGRAMS-2026-" + serial;
        String status = "ACKNOWLEDGED";
        String desc = "Grievance received and registered by Central Public Grievance Redressal Nodal Cell (Sandbox).";
        String officer = "Shri R. K. Sharma (Nodal Grievance Officer, Sector 17)";
        Instant targetDate = Instant.now().plus(Duration.ofDays(15));

        String docRef = "DOC-CPGRAMS-ACK-" + serial + ".pdf";
        String docUrl = "/api/external-grievances/documents/" + docRef;

        log.info("[GOV PORTAL SANDBOX] Successfully registered. Ack Number: {}, Target: {}", ackNumber, targetDate);
        return GrievanceSubmissionResult.success(ackNumber, status, desc, officer, targetDate, docRef, docUrl);
    }

    @Override
    public GrievanceStatusResult checkStatus(String acknowledgementNumber, String currentStatus) {
        log.info("[GOV PORTAL SANDBOX] Polling progress for acknowledgement number: {}, current: {}",
                acknowledgementNumber, currentStatus);

        // Realistic progression simulator
        String nextStatus;
        String remarks;
        String actionBy = "Public Grievance Monitoring Officer";

        if ("ACKNOWLEDGED".equalsIgnoreCase(currentStatus)) {
            nextStatus = "UNDER_PROCESS";
            remarks = "Grievance assigned to Municipal Field Engineering Division. Site inspection scheduled.";
        } else if ("UNDER_PROCESS".equalsIgnoreCase(currentStatus)) {
            nextStatus = "ACTION_TAKEN";
            remarks = "Field response team deployed. Corrective maintenance work initiated on location.";
        } else if ("ACTION_TAKEN".equalsIgnoreCase(currentStatus)) {
            nextStatus = "RESOLVED";
            remarks = "Municipal team verified remediation. Grievance closed with resolution certificate.";
        } else {
            nextStatus = currentStatus;
            remarks = "No status change. Current status: " + currentStatus;
        }

        return GrievanceStatusResult.success(acknowledgementNumber, nextStatus, remarks, actionBy);
    }
}
