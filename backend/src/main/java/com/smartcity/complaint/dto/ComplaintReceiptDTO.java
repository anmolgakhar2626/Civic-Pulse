package com.smartcity.complaint.dto;

import com.smartcity.complaint.ComplaintReceipt;
import java.time.Instant;
import java.util.UUID;

public class ComplaintReceiptDTO {
    private UUID id;
    private String receiptNumber;
    private String complaintRecordNumber;
    private String citizenName;
    private String citizenPhone;
    private String title;
    private String category;
    private String departmentName;
    private String locationAddress;
    private String priority;
    private String status;
    private Instant slaDeadline;
    private Integer expectedResolutionHours;
    private String acknowledgementMessage;
    private Instant generatedAt;
    private String pdfDownloadUrl;

    public ComplaintReceiptDTO() {}

    public ComplaintReceiptDTO(ComplaintReceipt r) {
        if (r != null) {
            this.id = r.getId();
            this.receiptNumber = r.getReceiptNumber();
            this.complaintRecordNumber = r.getComplaintRecordNumber();
            this.citizenName = r.getCitizenName();
            this.citizenPhone = r.getCitizenPhone();
            this.title = r.getTitle();
            this.category = r.getCategory();
            this.departmentName = r.getDepartmentName();
            this.locationAddress = r.getLocationAddress();
            this.priority = r.getPriority();
            this.status = r.getStatus();
            this.slaDeadline = r.getSlaDeadline();
            this.expectedResolutionHours = r.getExpectedResolutionHours();
            this.acknowledgementMessage = r.getAcknowledgementMessage();
            this.generatedAt = r.getGeneratedAt();
            this.pdfDownloadUrl = "/api/complaints/" + r.getComplaintId() + "/receipt/pdf";
        }
    }

    public static ComplaintReceiptDTO fromEntity(ComplaintReceipt r) {
        return r != null ? new ComplaintReceiptDTO(r) : null;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    public String getComplaintRecordNumber() { return complaintRecordNumber; }
    public void setComplaintRecordNumber(String complaintRecordNumber) { this.complaintRecordNumber = complaintRecordNumber; }

    public String getComplaintNumber() { return complaintRecordNumber; }
    public void setComplaintNumber(String complaintNumber) { this.complaintRecordNumber = complaintNumber; }

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

    public String getPdfDownloadUrl() { return pdfDownloadUrl; }
    public void setPdfDownloadUrl(String pdfDownloadUrl) { this.pdfDownloadUrl = pdfDownloadUrl; }
}
