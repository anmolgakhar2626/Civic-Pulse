package com.smartcity.grievance.portal;

import java.time.Instant;
import java.util.UUID;

public class AcknowledgementDocument {

    private UUID id;
    private UUID externalGrievanceId;
    private String documentReference;
    private String documentType = "ACKNOWLEDGEMENT_RECEIPT";
    private String documentTitle;
    private String documentUrl;
    private Instant issuedAt = Instant.now();
    private long fileSizeBytes;

    public AcknowledgementDocument() {}

    public AcknowledgementDocument(UUID externalGrievanceId, String documentReference,
                                   String documentType, String documentTitle, String documentUrl,
                                   long fileSizeBytes) {
        this.id = UUID.randomUUID();
        this.externalGrievanceId = externalGrievanceId;
        this.documentReference = documentReference;
        this.documentType = documentType;
        this.documentTitle = documentTitle;
        this.documentUrl = documentUrl;
        this.fileSizeBytes = fileSizeBytes;
        this.issuedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getExternalGrievanceId() { return externalGrievanceId; }
    public void setExternalGrievanceId(UUID externalGrievanceId) { this.externalGrievanceId = externalGrievanceId; }

    public String getDocumentReference() { return documentReference; }
    public void setDocumentReference(String documentReference) { this.documentReference = documentReference; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }

    public String getDocumentUrl() { return documentUrl; }
    public void setDocumentUrl(String documentUrl) { this.documentUrl = documentUrl; }

    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
}
