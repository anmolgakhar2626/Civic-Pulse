package com.smartcity.grievance.portal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GovernmentGrievanceDao {

    Optional<GovernmentPortal> findPortalByCode(String portalCode);

    List<GovernmentPortal> findAllActivePortals();

    Optional<ExternalGrievance> findByComplaintId(UUID complaintId);

    Optional<ExternalGrievance> findById(UUID id);

    Optional<ExternalGrievance> findByIdempotencyKey(String idempotencyKey);

    Optional<ExternalGrievance> findByAcknowledgementNumber(String acknowledgementNumber);

    ExternalGrievance saveExternalGrievance(ExternalGrievance grievance);

    int updateExternalGrievanceStatus(UUID grievanceId, String newStatus, String statusDescription, Instant lastSyncedAt);

    void saveStatusHistory(GrievanceStatusHistory history);

    List<GrievanceStatusHistory> findStatusHistory(UUID externalGrievanceId);

    void saveSyncLog(GrievanceSyncLog syncLog);

    List<GrievanceSyncLog> findSyncLogs(UUID externalGrievanceId);

    void saveDocument(AcknowledgementDocument document);

    List<AcknowledgementDocument> findDocuments(UUID externalGrievanceId);

    Optional<AcknowledgementDocument> findDocumentByReference(String reference);
}
