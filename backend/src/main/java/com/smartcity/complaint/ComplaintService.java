package com.smartcity.complaint;

import com.smartcity.complaint.dto.*;
import com.smartcity.user.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ComplaintService {

    ComplaintResponse createComplaint(CreateComplaintRequest request, List<MultipartFile> files, User citizen);

    default ComplaintResponse createComplaint(CreateComplaintRequest request, User citizen) {
        return createComplaint(request, List.of(), citizen);
    }

    ComplaintResponse verifyComplaint(UUID complaintId, VerifyComplaintRequest request, User verifier);

    ComplaintResponse getComplaintById(UUID complaintId);

    List<ComplaintResponse> getComplaints(String status, String category, String verificationStatus,
                                          UUID citizenId, UUID departmentId, UUID assignedToUserId,
                                          String priority, String search, String slaStatus, Boolean unassigned,
                                          int page, int size);

    default List<ComplaintResponse> getComplaints(String status, String category, String verificationStatus,
                                                  UUID citizenId, UUID departmentId, UUID assignedToUserId,
                                                  String priority, String search, int page, int size) {
        return getComplaints(status, category, verificationStatus, citizenId, departmentId, assignedToUserId, priority, search, null, null, page, size);
    }

    default List<ComplaintResponse> getComplaints(String status, String category, String verificationStatus,
                                                  UUID citizenId, int page, int size) {
        return getComplaints(status, category, verificationStatus, citizenId, null, null, null, null, null, null, page, size);
    }

    int countComplaints(String status, String category, String verificationStatus,
                        UUID citizenId, UUID departmentId, UUID assignedToUserId,
                        String priority, String search, String slaStatus, Boolean unassigned);

    default int countComplaints(String status, String category, String verificationStatus,
                        UUID citizenId, UUID departmentId, UUID assignedToUserId,
                        String priority, String search) {
        return countComplaints(status, category, verificationStatus, citizenId, departmentId, assignedToUserId, priority, search, null, null);
    }

    default int countComplaints(String status, String category, String verificationStatus) {
        return countComplaints(status, category, verificationStatus, null, null, null, null, null, null, null);
    }

    int evaluateComplaintSlaDeadlines();

    CitizenReputationResponse getCitizenReputation(UUID citizenId);

    List<ComplaintVerificationHistory> getVerificationHistory(UUID complaintId);

    ComplaintResponse assignComplaint(UUID complaintId, AssignComplaintRequest request, User assigner);

    ComplaintResponse updateProgress(UUID complaintId, UpdateComplaintProgressRequest request, List<MultipartFile> evidenceFiles, User updater);

    ComplaintResponse submitResolution(UUID complaintId, String resolutionNotes, List<MultipartFile> evidenceFiles, User resolver);

    ComplaintResponse verifyResolution(UUID complaintId, String decision, String notes, User verifier);

    ComplaintResponse changePriority(UUID complaintId, ChangePriorityRequest request, User modifier);

    ComplaintResponse closeOrReopen(UUID complaintId, String action, String reason, User user);

    ComplaintReceiptDTO getReceipt(UUID complaintId);

    byte[] getReceiptPdf(UUID complaintId);

    ComplaintAttachment getAttachment(UUID attachmentId);

    AdminComplaintStatsDTO getAdminStats();

    DepartmentComplaintStatsDTO getDepartmentStats(UUID departmentId, User officer);

    List<DepartmentComplaintStatsDTO> getAllDepartmentStats();
}
