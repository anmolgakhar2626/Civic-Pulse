package com.smartcity.complaint;

import com.smartcity.common.ForbiddenException;
import com.smartcity.complaint.dto.*;
import com.smartcity.department.Department;
import com.smartcity.department.DepartmentRepository;
import com.smartcity.grievance.portal.GovernmentGrievanceService;
import com.smartcity.notification.NotificationDTO;
import com.smartcity.notification.NotificationService;
import com.smartcity.notification.NotificationController;
import com.smartcity.sla.dto.SlaOverviewResponse;
import com.smartcity.sla.SlaDao;
import com.smartcity.sla.SLAService;
import com.smartcity.sla.SLAController;
import com.smartcity.user.Role;
import com.smartcity.user.User;
import com.smartcity.user.UserDao;
import com.smartcity.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GrievanceSlaNotificationFunctionalTest {

    @Mock
    private ComplaintService complaintService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private GovernmentGrievanceService grievanceService;

    @Mock
    private UserDao userDao;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SLAService slaService;

    private ComplaintController complaintController;
    private NotificationController notificationController;
    private SLAController slaController;

    private User citizenUser;
    private User otherCitizenUser;
    private User adminUser;
    private User officerUser;
    private User fieldWorkerUser;

    private Department waterDept;
    private Department pwdDept;

    @BeforeEach
    void setUp() {
        complaintController = new ComplaintController(complaintService, fileStorageService, grievanceService, userDao);
        notificationController = new NotificationController(notificationService, userRepository);
        slaController = new SLAController(slaService);

        waterDept = new Department();
        waterDept.setId(UUID.randomUUID());
        waterDept.setCode("WATER");
        waterDept.setName("Water Supply");

        pwdDept = new Department();
        pwdDept.setId(UUID.randomUUID());
        pwdDept.setCode("PWD");
        pwdDept.setName("Public Works Department");

        citizenUser = new User();
        citizenUser.setId(UUID.randomUUID());
        citizenUser.setUsername("citizen_raj");
        citizenUser.setFullName("Raj Kumar");
        citizenUser.setPhone("+919876543210");
        citizenUser.setMobileNumber("+919876543210");
        citizenUser.setIsMobileVerified(true);
        citizenUser.setRoles(Set.of(new Role("ROLE_CITIZEN", "Citizen")));

        otherCitizenUser = new User();
        otherCitizenUser.setId(UUID.randomUUID());
        otherCitizenUser.setUsername("citizen_priya");
        otherCitizenUser.setFullName("Priya Sharma");
        otherCitizenUser.setPhone("+919876543211");
        otherCitizenUser.setRoles(Set.of(new Role("ROLE_CITIZEN", "Citizen")));

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setUsername("admin_user");
        adminUser.setRoles(Set.of(new Role("ROLE_ADMIN", "Administrator")));

        officerUser = new User();
        officerUser.setId(UUID.randomUUID());
        officerUser.setUsername("officer_water");
        officerUser.setDepartment(waterDept);
        officerUser.setRoles(Set.of(new Role("ROLE_DEPARTMENT_OFFICER", "Department Officer")));

        fieldWorkerUser = new User();
        fieldWorkerUser.setId(UUID.randomUUID());
        fieldWorkerUser.setUsername("worker_pwd");
        fieldWorkerUser.setDepartment(pwdDept);
        fieldWorkerUser.setRoles(Set.of(new Role("ROLE_FIELD_WORKER", "Field Worker")));
    }

    // =========================================================================
    // ISSUE 1: Citizen-Only Submission & Role-Based Isolation
    // =========================================================================

    @Test
    @DisplayName("Issue 1A: Citizen successfully submits grievance")
    void testCitizenCanSubmitGrievance() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(citizenUser.getUsername(), "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_CITIZEN")))
        );
        when(userDao.findByUsernameOrEmail(citizenUser.getUsername())).thenReturn(Optional.of(citizenUser));

        CreateComplaintRequest request = new CreateComplaintRequest();
        request.setTitle("Water Leakage");
        request.setCategory("WATER_SUPPLY");
        request.setDescription("Broken water pipe leaking clean water on Main Street.");

        ComplaintResponse expected = new ComplaintResponse();
        expected.setId(UUID.randomUUID());
        expected.setComplaintNumber("CP-2026-000001");
        expected.setTitle("Water Leakage");

        when(complaintService.createComplaint(any(), any(), eq(citizenUser))).thenReturn(expected);

        ResponseEntity<ComplaintResponse> res = complaintController.createComplaintMultipart(request, List.of());
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody().getComplaintNumber()).isEqualTo("CP-2026-000001");
    }

    @Test
    @DisplayName("Issue 1B: Non-citizen operational role (Admin) blocked with 403 Forbidden on grievance submission")
    void testAdminCannotSubmitGrievance() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser.getUsername(), "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
        when(userDao.findByUsernameOrEmail(adminUser.getUsername())).thenReturn(Optional.of(adminUser));

        CreateComplaintRequest request = new CreateComplaintRequest();
        request.setTitle("Pothole Fix");

        assertThatThrownBy(() -> complaintController.createComplaintMultipart(request, List.of()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("strictly reserved for citizen accounts");
    }

    @Test
    @DisplayName("Issue 1C: Department Officer blocked with 403 Forbidden on citizen grievance submission")
    void testOfficerCannotSubmitGrievance() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(officerUser.getUsername(), "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_DEPARTMENT_OFFICER")))
        );
        when(userDao.findByUsernameOrEmail(officerUser.getUsername())).thenReturn(Optional.of(officerUser));

        CreateComplaintRequest request = new CreateComplaintRequest();

        assertThatThrownBy(() -> complaintController.createComplaintMultipart(request, List.of()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("strictly reserved for citizen accounts");
    }

    @Test
    @DisplayName("Issue 1D: Citizen can view their own grievance details")
    void testCitizenCanViewOwnGrievance() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(citizenUser.getUsername(), "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_CITIZEN")))
        );
        when(userDao.findByUsernameOrEmail(citizenUser.getUsername())).thenReturn(Optional.of(citizenUser));

        UUID complaintId = UUID.randomUUID();
        ComplaintResponse comp = new ComplaintResponse();
        comp.setId(complaintId);
        comp.setCitizenId(citizenUser.getId());
        comp.setCitizenPhone("+919876543210");
        comp.setStatus("IN_PROGRESS");

        when(complaintService.getComplaintById(complaintId)).thenReturn(comp);

        ResponseEntity<ComplaintResponse> res = complaintController.getComplaintById(complaintId);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getCitizenPhone()).isEqualTo("+919876543210");
    }

    @Test
    @DisplayName("Issue 1E: Citizen blocked from accessing another citizen's active grievance data")
    void testCitizenBlockedFromOtherCitizenPrivateGrievance() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(citizenUser.getUsername(), "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_CITIZEN")))
        );
        when(userDao.findByUsernameOrEmail(citizenUser.getUsername())).thenReturn(Optional.of(citizenUser));

        UUID otherComplaintId = UUID.randomUUID();
        ComplaintResponse comp = new ComplaintResponse();
        comp.setId(otherComplaintId);
        comp.setCitizenId(otherCitizenUser.getId());
        comp.setStatus("IN_PROGRESS");

        when(complaintService.getComplaintById(otherComplaintId)).thenReturn(comp);

        assertThatThrownBy(() -> complaintController.getComplaintById(otherComplaintId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("cannot access other citizens' private grievance data");
    }

    @Test
    @DisplayName("Issue 1F: Citizen can view resolved grievance but private contact phone is masked")
    void testCitizenCanViewResolvedPublicGrievanceWithSanitizedData() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(citizenUser.getUsername(), "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_CITIZEN")))
        );
        when(userDao.findByUsernameOrEmail(citizenUser.getUsername())).thenReturn(Optional.of(citizenUser));

        UUID resolvedComplaintId = UUID.randomUUID();
        ComplaintResponse comp = new ComplaintResponse();
        comp.setId(resolvedComplaintId);
        comp.setCitizenId(otherCitizenUser.getId());
        comp.setCitizenPhone("+919876543211");
        comp.setStatus("RESOLVED");

        when(complaintService.getComplaintById(resolvedComplaintId)).thenReturn(comp);

        ResponseEntity<ComplaintResponse> res = complaintController.getComplaintById(resolvedComplaintId);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Private citizen phone masked
        assertThat(res.getBody().getCitizenPhone()).isNull();
    }

    @Test
    @DisplayName("Issue 1G: Department Officer blocked by department isolation from viewing other department grievance")
    void testOfficerBlockedFromOtherDepartmentGrievance() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(officerUser.getUsername(), "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_DEPARTMENT_OFFICER")))
        );
        when(userDao.findByUsernameOrEmail(officerUser.getUsername())).thenReturn(Optional.of(officerUser));

        UUID pwdComplaintId = UUID.randomUUID();
        ComplaintResponse comp = new ComplaintResponse();
        comp.setId(pwdComplaintId);
        comp.setDepartmentId(pwdDept.getId()); // PWD, while officer is WATER
        comp.setStatus("IN_PROGRESS");

        when(complaintService.getComplaintById(pwdComplaintId)).thenReturn(comp);

        assertThatThrownBy(() -> complaintController.getComplaintById(pwdComplaintId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Department isolation prevents viewing");
    }

    // =========================================================================
    // ISSUE 2: Grievance Photo Upload & Streaming Endpoint
    // =========================================================================

    @Test
    @DisplayName("Issue 2A: Attachment streaming endpoint returns resource with inline headers and cache control")
    void testAttachmentStreamingEndpoint() {
        UUID attachmentId = UUID.randomUUID();
        ComplaintAttachment att = new ComplaintAttachment();
        att.setId(attachmentId);
        att.setFileName("leakage_evidence.jpg");
        att.setOriginalFileName("photo.jpg");
        att.setContentType("image/jpeg");

        when(complaintService.getAttachment(attachmentId)).thenReturn(att);
        Resource mockResource = new ByteArrayResource(new byte[]{1, 2, 3});
        when(fileStorageService.loadFileAsResource("leakage_evidence.jpg")).thenReturn(mockResource);

        ResponseEntity<Resource> res = complaintController.getAttachment(attachmentId);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getHeaders().getContentType().toString()).isEqualTo("image/jpeg");
        assertThat(res.getHeaders().getCacheControl()).contains("max-age=86400");
    }

    // =========================================================================
    // ISSUE 3: SLA Breached Count Mismatch
    // =========================================================================

    @Test
    @DisplayName("Issue 3A: SLA Overview Metrics correctly returns authoritative breached counts")
    void testSlaOverviewMetrics() {
        SlaOverviewResponse overview = new SlaOverviewResponse(15, 8, 4, 3, 25);
        when(slaService.getOverviewMetrics()).thenReturn(overview);

        ResponseEntity<SlaOverviewResponse> res = slaController.getOverviewMetrics();
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getBreachedCount()).isEqualTo(3L);
        assertThat(res.getBody().getTotalActive()).isEqualTo(15L);
        assertThat(res.getBody().getCompletedCount()).isEqualTo(25L);
    }

    @Test
    @DisplayName("Issue 3B: DepartmentComplaintStatsDTO exposes overdueComplaints alias matching breachedDeadline")
    void testDepartmentStatsOverdueAlias() {
        DepartmentComplaintStatsDTO deptStats = new DepartmentComplaintStatsDTO();
        deptStats.setBreachedDeadline(7L);
        assertThat(deptStats.getOverdueComplaints()).isEqualTo(7L);

        deptStats.setOverdueComplaints(9L);
        assertThat(deptStats.getBreachedDeadline()).isEqualTo(9L);
    }

    // =========================================================================
    // ISSUE 4: Notification Portal – Individual Notification Issue
    // =========================================================================

    @Test
    @DisplayName("Issue 4A: GET /api/notifications with scope=INDIVIDUAL filters direct notifications")
    void testGetNotificationsIndividualScope() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                citizenUser.getUsername(), "N/A", List.of(new SimpleGrantedAuthority("ROLE_CITIZEN"))
        );
        when(userRepository.findByUsername(citizenUser.getUsername())).thenReturn(Optional.of(citizenUser));

        NotificationDTO directDto = new NotificationDTO();
        directDto.setId(UUID.randomUUID());
        directDto.setTitle("Your Grievance was Verified");
        directDto.setMessage("Field crew assigned.");
        directDto.setRecipientUserId(citizenUser.getId());

        when(notificationService.getNotificationsForUser(eq(citizenUser.getId()), any(), eq(false), any(), any(), eq(true), eq(0), eq(20)))
                .thenReturn(List.of(directDto));

        ResponseEntity<?> response = notificationController.getNotifications(
                false, null, null, true, "INDIVIDUAL", 0, 20, auth
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationService).getNotificationsForUser(eq(citizenUser.getId()), any(), eq(false), any(), any(), eq(true), eq(0), eq(20));
    }

    @Test
    @DisplayName("Issue 4B: GET /api/notifications/{id} retrieves full individual notification details")
    void testGetNotificationDetailsById() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                citizenUser.getUsername(), "N/A", List.of(new SimpleGrantedAuthority("ROLE_CITIZEN"))
        );
        when(userRepository.findByUsername(citizenUser.getUsername())).thenReturn(Optional.of(citizenUser));

        UUID notifId = UUID.randomUUID();
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notifId);
        dto.setTitle("SLA Resolution Complete");
        dto.setMessage("Your issue has been resolved.");
        dto.setRecipientUserId(citizenUser.getId());

        when(notificationService.getNotificationById(eq(notifId), eq(citizenUser.getId()), any()))
                .thenReturn(dto);

        ResponseEntity<?> response = notificationController.getNotificationById(notifId, auth);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationService).getNotificationById(eq(notifId), eq(citizenUser.getId()), any());
    }

    @Test
    @DisplayName("Issue 4C: Unauthorized cross-user notification detail access blocked")
    void testCrossUserNotificationAccessBlocked() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                citizenUser.getUsername(), "N/A", List.of(new SimpleGrantedAuthority("ROLE_CITIZEN"))
        );
        when(userRepository.findByUsername(citizenUser.getUsername())).thenReturn(Optional.of(citizenUser));

        UUID notifId = UUID.randomUUID();
        when(notificationService.getNotificationById(eq(notifId), eq(citizenUser.getId()), any()))
                .thenThrow(new AccessDeniedException("User not authorized for this notification"));

        assertThatThrownBy(() -> notificationController.getNotificationById(notifId, auth))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User not authorized");
    }

    @Test
    @DisplayName("Issue 4D: Notification mark as unread persists status update")
    void testMarkNotificationAsUnread() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                citizenUser.getUsername(), "N/A", List.of(new SimpleGrantedAuthority("ROLE_CITIZEN"))
        );
        when(userRepository.findByUsername(citizenUser.getUsername())).thenReturn(Optional.of(citizenUser));

        UUID notifId = UUID.randomUUID();
        NotificationDTO unreadDto = new NotificationDTO();
        unreadDto.setId(notifId);
        unreadDto.setStatus("UNREAD");
        unreadDto.setRead(false);

        when(notificationService.markAsUnread(eq(notifId), eq(citizenUser.getId()), any()))
                .thenReturn(unreadDto);

        ResponseEntity<?> response = notificationController.markAsUnread(notifId, auth);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationService).markAsUnread(eq(notifId), eq(citizenUser.getId()), any());
    }
}

