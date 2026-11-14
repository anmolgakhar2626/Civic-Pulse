package com.smartcity.complaint.dto;

import com.smartcity.complaint.Complaint;
import com.smartcity.grievance.portal.dto.ExternalGrievanceTrackingResponse;
import com.smartcity.user.User;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ComplaintResponse {

    private UUID id;
    private String complaintNumber;
    private UUID citizenId;
    private String citizenName;
    private String citizenPhone;
    private boolean citizenIsVerifiedBadge;
    private int citizenGenuineCount;
    private String title;
    private String category;
    private String subcategory;
    private String description;
    private Double latitude;
    private Double longitude;
    private String address;
    private String status;
    private String complaintVerificationStatus;
    private String verifiedByName;
    private Instant verifiedAt;
    private String verificationNotes;
    private String evidenceUrl;

    // Department & Assignee
    private UUID departmentId;
    private String departmentCode;
    private String departmentName;
    private UUID assignedToId;
    private String assignedToName;
    private String assignedToRole;

    // SLA & Priority
    private String priority;
    private Integer slaMinutes;
    private Instant slaDeadline;
    private String slaStatus;
    private Long remainingMinutes;

    // Resolution & Closure
    private String resolutionNotes;
    private String resolutionEvidenceUrl;
    private Instant resolvedAt;
    private Instant closedAt;

    private Instant createdAt;
    private Instant updatedAt;

    // Rich embedded data
    private List<ComplaintAttachmentDTO> attachments = new ArrayList<>();
    private ComplaintReceiptDTO receipt;
    private List<ComplaintStatusHistoryDTO> statusHistory = new ArrayList<>();
    private ExternalGrievanceTrackingResponse externalTracking;

    public ComplaintResponse() {}

    public ComplaintResponse(Complaint c, User citizen, ExternalGrievanceTrackingResponse externalTracking) {
        if (c != null) {
            this.id = c.getId();
            this.complaintNumber = c.getComplaintNumber();
            this.citizenId = c.getCitizen() != null ? c.getCitizen().getId() : (citizen != null ? citizen.getId() : null);
            this.citizenName = c.getCitizenName();
            this.citizenPhone = c.getCitizenPhone();
            this.title = c.getTitle();
            this.category = c.getCategory();
            this.subcategory = c.getSubcategory();
            this.description = c.getDescription();
            this.latitude = c.getLatitude();
            this.longitude = c.getLongitude();
            this.address = c.getAddress();
            this.status = c.getStatus();
            this.complaintVerificationStatus = c.getComplaintVerificationStatus();
            this.verifiedByName = c.getVerifiedBy() != null ? c.getVerifiedBy().getFullName() : null;
            this.verifiedAt = c.getVerifiedAt();
            this.verificationNotes = c.getVerificationNotes();
            this.evidenceUrl = c.getEvidenceUrl();

            if (c.getDepartment() != null) {
                this.departmentId = c.getDepartment().getId();
                this.departmentCode = c.getDepartment().getCode();
                this.departmentName = c.getDepartment().getName();
            }

            if (c.getAssignedTo() != null) {
                this.assignedToId = c.getAssignedTo().getId();
                this.assignedToName = c.getAssignedTo().getFullName();
                this.assignedToRole = c.getAssignedTo().getRoleName() != null ? c.getAssignedTo().getRoleName() : (c.getAssignedTo().getRole() != null ? c.getAssignedTo().getRole().getName() : null);
            }

            this.priority = c.getPriority();
            this.slaMinutes = c.getSlaMinutes();
            this.slaDeadline = c.getSlaDeadline();
            this.slaStatus = c.getSlaStatus();

            if (this.slaDeadline != null) {
                if ("RESOLVED".equalsIgnoreCase(this.status) || "CLOSED".equalsIgnoreCase(this.status)) {
                    this.remainingMinutes = 0L;
                    this.slaStatus = "RESOLVED";
                } else {
                    long diff = Duration.between(Instant.now(), this.slaDeadline).toMinutes();
                    this.remainingMinutes = diff;
                    if (diff < 0) {
                        this.slaStatus = "BREACHED";
                    } else if (diff <= 120) {
                        this.slaStatus = "AT_RISK";
                    } else {
                        this.slaStatus = "ON_TRACK";
                    }
                }
            }

            this.resolutionNotes = c.getResolutionNotes();
            this.resolutionEvidenceUrl = c.getResolutionEvidenceUrl();
            this.resolvedAt = c.getResolvedAt();
            this.closedAt = c.getClosedAt();

            this.createdAt = c.getCreatedAt();
            this.updatedAt = c.getUpdatedAt();
        }

        if (citizen != null) {
            this.citizenIsVerifiedBadge = citizen.isVerifiedCitizen();
            this.citizenGenuineCount = citizen.getGenuineComplaintCount();
            if (this.citizenName == null) this.citizenName = citizen.getFullName();
            if (this.citizenPhone == null) this.citizenPhone = citizen.getMobileNumber();
        }

        this.externalTracking = externalTracking;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getComplaintNumber() { return complaintNumber; }
    public void setComplaintNumber(String complaintNumber) { this.complaintNumber = complaintNumber; }

    public UUID getCitizenId() { return citizenId; }
    public void setCitizenId(UUID citizenId) { this.citizenId = citizenId; }

    public String getCitizenName() { return citizenName; }
    public void setCitizenName(String citizenName) { this.citizenName = citizenName; }

    public String getCitizenPhone() { return citizenPhone; }
    public void setCitizenPhone(String citizenPhone) { this.citizenPhone = citizenPhone; }

    public boolean isCitizenIsVerifiedBadge() { return citizenIsVerifiedBadge; }
    public void setCitizenIsVerifiedBadge(boolean citizenIsVerifiedBadge) { this.citizenIsVerifiedBadge = citizenIsVerifiedBadge; }

    public int getCitizenGenuineCount() { return citizenGenuineCount; }
    public void setCitizenGenuineCount(int citizenGenuineCount) { this.citizenGenuineCount = citizenGenuineCount; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubcategory() { return subcategory; }
    public void setSubcategory(String subcategory) { this.subcategory = subcategory; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getComplaintVerificationStatus() { return complaintVerificationStatus; }
    public void setComplaintVerificationStatus(String complaintVerificationStatus) { this.complaintVerificationStatus = complaintVerificationStatus; }

    public String getVerifiedByName() { return verifiedByName; }
    public void setVerifiedByName(String verifiedByName) { this.verifiedByName = verifiedByName; }

    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getVerificationNotes() { return verificationNotes; }
    public void setVerificationNotes(String verificationNotes) { this.verificationNotes = verificationNotes; }

    public String getEvidenceUrl() { return evidenceUrl; }
    public void setEvidenceUrl(String evidenceUrl) { this.evidenceUrl = evidenceUrl; }

    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public UUID getAssignedToId() { return assignedToId; }
    public void setAssignedToId(UUID assignedToId) { this.assignedToId = assignedToId; }

    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }

    public String getAssignedToRole() { return assignedToRole; }
    public void setAssignedToRole(String assignedToRole) { this.assignedToRole = assignedToRole; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public Integer getSlaMinutes() { return slaMinutes; }
    public void setSlaMinutes(Integer slaMinutes) { this.slaMinutes = slaMinutes; }

    public Instant getSlaDeadline() { return slaDeadline; }
    public void setSlaDeadline(Instant slaDeadline) { this.slaDeadline = slaDeadline; }

    public String getSlaStatus() { return slaStatus; }
    public void setSlaStatus(String slaStatus) { this.slaStatus = slaStatus; }

    public Long getRemainingMinutes() { return remainingMinutes; }
    public void setRemainingMinutes(Long remainingMinutes) { this.remainingMinutes = remainingMinutes; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public String getResolutionEvidenceUrl() { return resolutionEvidenceUrl; }
    public void setResolutionEvidenceUrl(String resolutionEvidenceUrl) { this.resolutionEvidenceUrl = resolutionEvidenceUrl; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<ComplaintAttachmentDTO> getAttachments() { return attachments; }
    public void setAttachments(List<ComplaintAttachmentDTO> attachments) { this.attachments = attachments; }

    public ComplaintReceiptDTO getReceipt() { return receipt; }
    public void setReceipt(ComplaintReceiptDTO receipt) { this.receipt = receipt; }

    public List<ComplaintStatusHistoryDTO> getStatusHistory() { return statusHistory; }
    public void setStatusHistory(List<ComplaintStatusHistoryDTO> statusHistory) { this.statusHistory = statusHistory; }

    public ExternalGrievanceTrackingResponse getExternalTracking() { return externalTracking; }
    public void setExternalTracking(ExternalGrievanceTrackingResponse externalTracking) { this.externalTracking = externalTracking; }
}
