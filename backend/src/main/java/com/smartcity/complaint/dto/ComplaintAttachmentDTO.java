package com.smartcity.complaint.dto;

import com.smartcity.complaint.ComplaintAttachment;
import java.time.Instant;
import java.util.UUID;

public class ComplaintAttachmentDTO {
    private UUID id;
    private String fileName;
    private String originalFileName;
    private Long fileSize;
    private String contentType;
    private String attachmentType;
    private String downloadUrl;
    private Instant createdAt;

    public ComplaintAttachmentDTO() {}

    public ComplaintAttachmentDTO(ComplaintAttachment att) {
        if (att != null) {
            this.id = att.getId();
            this.fileName = att.getFileName();
            this.originalFileName = att.getOriginalFileName();
            this.fileSize = att.getFileSize();
            this.contentType = att.getContentType();
            this.attachmentType = att.getAttachmentType();
            this.downloadUrl = "/api/complaints/attachments/" + att.getId();
            this.createdAt = att.getCreatedAt();
        }
    }

    public static ComplaintAttachmentDTO fromEntity(ComplaintAttachment att) {
        return att != null ? new ComplaintAttachmentDTO(att) : null;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getAttachmentType() { return attachmentType; }
    public void setAttachmentType(String attachmentType) { this.attachmentType = attachmentType; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
