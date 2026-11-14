package com.smartcity.complaint;

import com.smartcity.complaint.dto.AdminComplaintStatsDTO;
import com.smartcity.complaint.dto.DepartmentComplaintStatsDTO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComplaintDao {

    Complaint save(Complaint complaint);

    Optional<Complaint> findById(UUID id);

    Optional<Complaint> findByComplaintNumber(String complaintNumber);

    List<Complaint> findByCitizenId(UUID citizenId);

    List<Complaint> findAll(String status, String category, String verificationStatus,
                            UUID citizenId, UUID departmentId, UUID assignedToUserId,
                            String priority, String search, String slaStatus, Boolean unassigned,
                            int limit, int offset);

    int countAll(String status, String category, String verificationStatus,
                 UUID citizenId, UUID departmentId, UUID assignedToUserId,
                 String priority, String search, String slaStatus, Boolean unassigned);

    // Backward compatible findAll & countAll
    default List<Complaint> findAll(String status, String category, String verificationStatus,
                            UUID citizenId, UUID departmentId, UUID assignedToUserId,
                            String priority, String search, int limit, int offset) {
        return findAll(status, category, verificationStatus, citizenId, departmentId, assignedToUserId, priority, search, null, null, limit, offset);
    }

    default int countAll(String status, String category, String verificationStatus,
                 UUID citizenId, UUID departmentId, UUID assignedToUserId,
                 String priority, String search) {
        return countAll(status, category, verificationStatus, citizenId, departmentId, assignedToUserId, priority, search, null, null);
    }

    default List<Complaint> findAll(String status, String category, String verificationStatus, int limit, int offset) {
        return findAll(status, category, verificationStatus, null, null, null, null, null, null, null, limit, offset);
    }

    default int countAll(String status, String category, String verificationStatus) {
        return countAll(status, category, verificationStatus, null, null, null, null, null, null, null);
    }

    int updateVerificationStatus(UUID complaintId, String newStatus, UUID verifiedBy, String notes);

    int countGenuineComplaintsByCitizen(UUID citizenId);

    void saveVerificationHistory(ComplaintVerificationHistory history);

    List<ComplaintVerificationHistory> findVerificationHistory(UUID complaintId);

    // Sequence generator for record number: CP-YYYY-XXXXXX
    String generateNextComplaintRecordNumber();

    // Attachments
    ComplaintAttachment saveAttachment(ComplaintAttachment attachment);

    List<ComplaintAttachment> findAttachmentsByComplaintId(UUID complaintId);

    Optional<ComplaintAttachment> findAttachmentById(UUID attachmentId);

    // Digital Acknowledgement Receipts
    ComplaintReceipt saveReceipt(ComplaintReceipt receipt);

    Optional<ComplaintReceipt> findReceiptByComplaintId(UUID complaintId);

    Optional<ComplaintReceipt> findReceiptByNumber(String receiptNumber);

    // Status History
    ComplaintStatusHistory saveStatusHistory(ComplaintStatusHistory history);

    List<ComplaintStatusHistory> findStatusHistoryByComplaintId(UUID complaintId);

    // Statistics
    AdminComplaintStatsDTO getAdminComplaintStats();

    DepartmentComplaintStatsDTO getDepartmentComplaintStats(UUID departmentId);

    List<DepartmentComplaintStatsDTO> getAllDepartmentStats();
}
