package com.smartcity.grievance.portal;

import com.smartcity.complaint.Complaint;

/**
 * Adapter interface for integrating with external Government Grievance Portals
 * (e.g., CPGRAMS, State Public Grievance Portals, or Municipal Grievance API).
 */
public interface GovernmentPortalAdapter {

    /**
     * Unique portal code matching database records (e.g., CPGRAMS_SANDBOX, STATE_PGRS_SANDBOX).
     */
    String getPortalCode();

    /**
     * Human-readable official portal name.
     */
    String getPortalName();

    /**
     * Whether this adapter is currently operational and online.
     */
    boolean isAvailable();

    /**
     * Submits an eligible municipal complaint to the external government grievance portal.
     *
     * @param complaint Complaint entity to forward
     * @param idempotencyKey Key to ensure duplicate submissions are prevented
     * @param simulateFailure Flag for integration testing of failures/timeouts
     * @return GrievanceSubmissionResult containing official acknowledgement reference
     */
    GrievanceSubmissionResult submitGrievance(Complaint complaint, String idempotencyKey, String simulateFailure);

    /**
     * Queries the external government portal for progress and status updates.
     *
     * @param acknowledgementNumber Official acknowledgement number
     * @param currentStatus Current recorded status
     * @return GrievanceStatusResult with latest external status and remarks
     */
    GrievanceStatusResult checkStatus(String acknowledgementNumber, String currentStatus);

    /**
     * Whether this adapter connects to a mock/sandbox endpoint.
     */
    boolean isSandbox();
}
