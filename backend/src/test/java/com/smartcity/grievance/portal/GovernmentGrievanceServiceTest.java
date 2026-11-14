package com.smartcity.grievance.portal;

import com.smartcity.complaint.Complaint;
import com.smartcity.complaint.ComplaintDao;
import com.smartcity.grievance.portal.dto.ExternalGrievanceTrackingResponse;
import com.smartcity.grievance.portal.dto.ForwardGrievanceRequest;
import com.smartcity.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GovernmentGrievanceServiceTest {

    @Mock
    private GovernmentGrievanceDao grievanceDao;

    @Mock
    private ComplaintDao complaintDao;

    private SandboxGovernmentPortalAdapter sandboxAdapter;
    private GovernmentPortalRegistry portalRegistry;
    private GovernmentGrievanceServiceImpl grievanceService;

    private Complaint complaint;
    private GovernmentPortal portal;
    private User requester;

    @BeforeEach
    void setUp() {
        sandboxAdapter = new SandboxGovernmentPortalAdapter();
        portalRegistry = new GovernmentPortalRegistry(List.of(sandboxAdapter));
        grievanceService = new GovernmentGrievanceServiceImpl(grievanceDao, portalRegistry, complaintDao);

        UUID complaintId = UUID.randomUUID();
        complaint = new Complaint();
        complaint.setId(complaintId);
        complaint.setComplaintNumber("CMP-102938");
        complaint.setCategory("DRAINAGE");
        complaint.setDescription("Sewage overflow near public school");

        portal = new GovernmentPortal();
        portal.setId(UUID.randomUUID());
        portal.setPortalCode("CPGRAMS_SANDBOX");
        portal.setPortalName("Central Public Grievance Redress & Monitoring System (Sandbox)");
        portal.setActive(true);
        portal.setSandbox(true);

        requester = new User();
        requester.setId(UUID.randomUUID());
        requester.setUsername("officer_traffic");
    }

    @Test
    @DisplayName("Government Grievance: Submits complaint to CPGRAMS Sandbox with official Ack format and saves receipt")
    void forwardToGovernmentPortal_success() {
        when(complaintDao.findById(complaint.getId())).thenReturn(Optional.of(complaint));
        when(grievanceDao.findPortalByCode("CPGRAMS_SANDBOX")).thenReturn(Optional.of(portal));
        when(grievanceDao.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(grievanceDao.saveExternalGrievance(any(ExternalGrievance.class))).thenAnswer(inv -> inv.getArgument(0));

        ForwardGrievanceRequest request = new ForwardGrievanceRequest("CPGRAMS_SANDBOX");
        ExternalGrievanceTrackingResponse response = grievanceService.forwardToGovernmentPortal(complaint.getId(), request, requester);

        assertThat(response).isNotNull();
        assertThat(response.getPortalCode()).isEqualTo("CPGRAMS_SANDBOX");
        assertThat(response.getAcknowledgementNumber()).matches("^GOV-CPGRAMS-2026-\\d{5}$");
        assertThat(response.getExternalStatus()).isEqualTo("ACKNOWLEDGED");
        assertThat(response.isSandbox()).isTrue();

        // Verify database persistence
        verify(grievanceDao).saveExternalGrievance(any(ExternalGrievance.class));
        verify(grievanceDao).saveStatusHistory(argThat(h -> "ACKNOWLEDGED".equals(h.getStatus())));
        verify(grievanceDao).saveDocument(argThat(d -> "ACKNOWLEDGEMENT_RECEIPT".equals(d.getDocumentType())));
        verify(grievanceDao).saveSyncLog(argThat(l -> "SUCCESS".equals(l.getStatus())));
    }

    @Test
    @DisplayName("Government Grievance: Idempotency check prevents duplicate submissions to government portal")
    void forwardToGovernmentPortal_idempotencyPreventDuplicate() {
        when(complaintDao.findById(complaint.getId())).thenReturn(Optional.of(complaint));
        when(grievanceDao.findPortalByCode("CPGRAMS_SANDBOX")).thenReturn(Optional.of(portal));

        ExternalGrievance existing = new ExternalGrievance(complaint.getId(), portal.getId(), "CPGRAMS_SANDBOX",
                "GOV-CPGRAMS-2026-99999", "IDEMP-CPGRAMS_SANDBOX-" + complaint.getId());
        when(grievanceDao.findByIdempotencyKey(anyString())).thenReturn(Optional.of(existing));

        ForwardGrievanceRequest request = new ForwardGrievanceRequest("CPGRAMS_SANDBOX");
        ExternalGrievanceTrackingResponse response = grievanceService.forwardToGovernmentPortal(complaint.getId(), request, requester);

        assertThat(response).isNotNull();
        assertThat(response.getAcknowledgementNumber()).isEqualTo("GOV-CPGRAMS-2026-99999");

        // Verify adapter was NOT called again to create a duplicate
        verify(grievanceDao, never()).saveExternalGrievance(argThat(g -> !g.equals(existing)));
    }

    @Test
    @DisplayName("Government Grievance: Handles API timeout gracefully and records sync failure log")
    void forwardToGovernmentPortal_timeoutHandling() {
        when(complaintDao.findById(complaint.getId())).thenReturn(Optional.of(complaint));
        when(grievanceDao.findPortalByCode("CPGRAMS_SANDBOX")).thenReturn(Optional.of(portal));
        when(grievanceDao.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());

        ForwardGrievanceRequest request = new ForwardGrievanceRequest("CPGRAMS_SANDBOX");
        request.setSimulateFailure("TIMEOUT");

        assertThatThrownBy(() -> grievanceService.forwardToGovernmentPortal(complaint.getId(), request, requester))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("connection timed out");

        verify(grievanceDao).saveSyncLog(argThat(l -> "TIMEOUT".equals(l.getStatus())));
    }

    @Test
    @DisplayName("Government Grievance: Status polling syncs progression and appends timeline history")
    void syncExternalStatus_progression() {
        UUID grvId = UUID.randomUUID();
        ExternalGrievance eg = new ExternalGrievance(complaint.getId(), portal.getId(), "CPGRAMS_SANDBOX",
                "GOV-CPGRAMS-2026-12345", "IDEMP-KEY-1");
        eg.setId(grvId);
        eg.setExternalStatus("ACKNOWLEDGED");

        when(grievanceDao.findByComplaintId(complaint.getId())).thenReturn(Optional.of(eg));
        when(grievanceDao.findPortalByCode("CPGRAMS_SANDBOX")).thenReturn(Optional.of(portal));

        ExternalGrievanceTrackingResponse response = grievanceService.syncExternalStatus(complaint.getId(), requester);

        assertThat(response).isNotNull();
        // Progressed from ACKNOWLEDGED to UNDER_PROCESS
        assertThat(response.getExternalStatus()).isEqualTo("UNDER_PROCESS");

        verify(grievanceDao).updateExternalGrievanceStatus(eq(grvId), eq("UNDER_PROCESS"), anyString(), any());
        verify(grievanceDao).saveStatusHistory(argThat(h -> "UNDER_PROCESS".equals(h.getStatus())));
        verify(grievanceDao).saveSyncLog(argThat(l -> "STATUS_POLL".equals(l.getSyncType()) && "SUCCESS".equals(l.getStatus())));
    }
}
