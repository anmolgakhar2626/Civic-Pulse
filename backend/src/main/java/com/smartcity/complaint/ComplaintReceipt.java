package com.smartcity.complaint;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "complaint_receipts")
public class ComplaintReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "complaint_id", nullable = false, unique = true)
    private UUID complaintId;

    @Column(name = "receipt_number", nullable = false, unique = true, length = 100)
    private String receiptNumber;

    @Column(name = "complaint_record_number", nullable = false, length = 50)
    private String complaintRecordNumber;

    @Column(name = "citizen_name", nullable = false, length = 150)
    private String citizenName;

    @Column(name = "citizen_phone", length = 50)
    private String citizenPhone;

    @Column(length = 200)
    private String title;

    @Column(length = 100)
    private String category;

    @Column(name = "department_name", length = 150)
    private String departmentName;

    @Column(name = "location_address", length = 255)
    private String locationAddress;

    @Column(length = 20)
    private String priority;

    @Column(length = 50)
    private String status;

    @Column(name = "sla_deadline")
    private Instant slaDeadline;

    @Column(name = "expected_resolution_hours")
    private Integer expectedResolutionHours;

    @Column(name = "acknowledgement_message", columnDefinition = "TEXT")
    private String acknowledgementMessage;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt = Instant.now();

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    public ComplaintReceipt() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getComplaintId() { return complaintId; }
    public void setComplaintId(UUID complaintId) { this.complaintId = complaintId; }

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    public String getComplaintRecordNumber() { return complaintRecordNumber; }
    public void setComplaintRecordNumber(String complaintRecordNumber) { this.complaintRecordNumber = complaintRecordNumber; }

    public String getComplaintNumber() { return complaintRecordNumber; }
    public void setComplaintNumber(String num) { this.complaintRecordNumber = num; }

    public String getCitizenName() { return citizenName; }
    public void setCitizenName(String citizenName) { this.citizenName = citizenName; }

    public String getCitizenPhone() { return citizenPhone; }
    public void setCitizenPhone(String citizenPhone) { this.citizenPhone = citizenPhone; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getLocationAddress() { return locationAddress; }
    public void setLocationAddress(String locationAddress) { this.locationAddress = locationAddress; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getSlaDeadline() { return slaDeadline; }
    public void setSlaDeadline(Instant slaDeadline) { this.slaDeadline = slaDeadline; }

    public Integer getExpectedResolutionHours() { return expectedResolutionHours; }
    public void setExpectedResolutionHours(Integer expectedResolutionHours) { this.expectedResolutionHours = expectedResolutionHours; }

    public String getAcknowledgementMessage() { return acknowledgementMessage; }
    public void setAcknowledgementMessage(String acknowledgementMessage) { this.acknowledgementMessage = acknowledgementMessage; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }

    public Instant getSubmittedAt() { return generatedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.generatedAt = submittedAt; }

    public Instant getIssuedAt() { return generatedAt; }
    public void setIssuedAt(Instant issuedAt) { this.generatedAt = issuedAt; }

    public Integer getSlaMinutes() { return expectedResolutionHours != null ? expectedResolutionHours * 60 : 1440; }
    public void setSlaMinutes(Integer slaMinutes) { this.expectedResolutionHours = slaMinutes != null ? slaMinutes / 60 : 24; }

    public String getDepartmentCode() { return "DEPT"; }
    public void setDepartmentCode(String departmentCode) {}

    public String getQrData() { return receiptNumber + "|" + complaintRecordNumber; }
    public void setQrData(String qrData) {}

    public UUID getCitizenId() { return null; }
    public void setCitizenId(UUID citizenId) {}

    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
}
