package com.smartcity.grievance.portal;

import com.smartcity.grievance.portal.dto.ExternalGrievanceTrackingResponse;
import com.smartcity.grievance.portal.dto.ForwardGrievanceRequest;
import com.smartcity.user.User;

import java.util.List;
import java.util.UUID;

public interface GovernmentGrievanceService {

    ExternalGrievanceTrackingResponse forwardToGovernmentPortal(UUID complaintId, ForwardGrievanceRequest request, User requester);

    ExternalGrievanceTrackingResponse getExternalTracking(UUID complaintId);

    java.util.Optional<ExternalGrievanceTrackingResponse> findExternalTracking(UUID complaintId);

    ExternalGrievanceTrackingResponse syncExternalStatus(UUID complaintId, User requester);

    List<GovernmentPortal> getAvailablePortals();

    byte[] generateReceiptPdf(String documentReference);
}
