package com.smartcity.complaint;

import com.smartcity.complaint.dto.*;
import com.smartcity.department.Department;
import com.smartcity.department.DepartmentRepository;
import com.smartcity.notification.NotificationService;
import com.smartcity.user.Role;
import com.smartcity.user.User;
import com.smartcity.user.UserDao;
import com.smartcity.websocket.WebSocketEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintLifecycleUnitTest {

    @Mock
    private ComplaintDao complaintDao;

    @Mock
    private UserDao userDao;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private WebSocketEventPublisher webSocketPublisher;

    private ComplaintServiceImpl complaintService;

    private User citizen;
    private User officer;
    private User fieldWorker;
    private User admin;
    private Department waterDept;

    @BeforeEach
    void setUp() {
        complaintService = new ComplaintServiceImpl(
                complaintDao,
                userDao,
                departmentRepository,
                fileStorageService,
                notificationService,
                webSocketPublisher,
                null
        );

        Role citizenRole = new Role();
        citizenRole.setName("ROLE_CITIZEN");

        Role officerRole = new Role();
        officerRole.setName("ROLE_DEPARTMENT_OFFICER");

        Role workerRole = new Role();
        workerRole.setName("ROLE_FIELD_WORKER");

        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");

        citizen = new User();
        citizen.setId(UUID.randomUUID());
        citizen.setUsername("citizen_test");
        citizen.setFullName("Aarav Mehta");
        citizen.setMobileNumber("+919876543210");
        citizen.setIsMobileVerified(true);
        citizen.setRoles(Set.of(citizenRole));

        officer = new User();
        officer.setId(UUID.randomUUID());
        officer.setUsername("officer_water");
        officer.setFullName("Officer Ramesh");
        officer.setRoles(Set.of(officerRole));

        fieldWorker = new User();
        fieldWorker.setId(UUID.randomUUID());
        fieldWorker.setUsername("worker_ram");
        fieldWorker.setFullName("Ram Singh");
        fieldWorker.setRoles(Set.of(workerRole));

        admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setUsername("admin");
        admin.setFullName("System Admin");
        admin.setRoles(Set.of(adminRole));

        waterDept = new Department("WATER", "Water Supply & Sewerage Board");
        waterDept.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Category to Department Routing: Correct municipal code resolved")
    void testCategoryDepartmentMapping() {
        assertThat(ComplaintServiceImpl.mapCategoryToDepartmentCode("WATER_SUPPLY")).isEqualTo("WATER");
        assertThat(ComplaintServiceImpl.mapCategoryToDepartmentCode("ROADS_AND_TRAFFIC")).isEqualTo("PWD");
        assertThat(ComplaintServiceImpl.mapCategoryToDepartmentCode("ELECTRICITY")).isEqualTo("ELECTRICAL");
        assertThat(ComplaintServiceImpl.mapCategoryToDepartmentCode("SOLID_WASTE")).isEqualTo("SANITATION");
        assertThat(ComplaintServiceImpl.mapCategoryToDepartmentCode("PUBLIC_HEALTH")).isEqualTo("HEALTH");
        assertThat(ComplaintServiceImpl.mapCategoryToDepartmentCode("TRAFFIC_MANAGEMENT")).isEqualTo("TRAFFIC");
        assertThat(ComplaintServiceImpl.mapCategoryToDepartmentCode("ENVIRONMENT")).isEqualTo("ENVIRONMENT");
        assertThat(ComplaintServiceImpl.mapCategoryToDepartmentCode("EMERGENCY_SERVICES")).isEqualTo("EMERGENCY");
    }

    @Test
    @DisplayName("Priority to SLA Minutes: Correct SLA windows assigned")
    void testPrioritySlaMinutes() {
        assertThat(ComplaintServiceImpl.getSlaMinutesForPriority("CRITICAL")).isEqualTo(240);  // 4h
        assertThat(ComplaintServiceImpl.getSlaMinutesForPriority("HIGH")).isEqualTo(720);      // 12h
        assertThat(ComplaintServiceImpl.getSlaMinutesForPriority("MEDIUM")).isEqualTo(1440);   // 24h
        assertThat(ComplaintServiceImpl.getSlaMinutesForPriority("LOW")).isEqualTo(2880);     // 48h
    }

    @Test
    @DisplayName("Create Complaint: Sequence CP-2026-000001, Auto Department, SLA & Receipt generated")
    void testCreateComplaint_success() {
        when(userDao.findById(citizen.getId())).thenReturn(Optional.of(citizen));
        when(complaintDao.generateNextComplaintRecordNumber()).thenReturn("CP-2026-000001");
        when(departmentRepository.findByCode("WATER")).thenReturn(Optional.of(waterDept));
        when(complaintDao.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateComplaintRequest request = new CreateComplaintRequest();
        request.setTitle("Severe pipeline rupture");
        request.setCategory("WATER_SUPPLY");
        request.setSubcategory("Pipeline Leakage");
        request.setPriority("HIGH");
        request.setDescription("Water flooding the main street");
        request.setAddress("Sector 17, Ward 4");

        ComplaintResponse response = complaintService.createComplaint(request, List.of(), citizen);

        assertThat(response).isNotNull();
        assertThat(response.getComplaintNumber()).isEqualTo("CP-2026-000001");
        assertThat(response.getDepartmentCode()).isEqualTo("WATER");
        assertThat(response.getPriority()).isEqualTo("HIGH");
        assertThat(response.getSlaMinutes()).isEqualTo(720);
        assertThat(response.getStatus()).isEqualTo("SUBMITTED");

        // Verify receipt saved
        ArgumentCaptor<ComplaintReceipt> receiptCaptor = ArgumentCaptor.forClass(ComplaintReceipt.class);
        verify(complaintDao).saveReceipt(receiptCaptor.capture());
        ComplaintReceipt savedReceipt = receiptCaptor.getValue();
        assertThat(savedReceipt.getReceiptNumber()).isEqualTo("RCP-2026-000001");
        assertThat(savedReceipt.getComplaintRecordNumber()).isEqualTo("CP-2026-000001");

        // Verify status history saved
        verify(complaintDao).saveStatusHistory(any(ComplaintStatusHistory.class));
    }

    @Test
    @DisplayName("Assign Complaint: Status transitions to ASSIGNED and field worker notified")
    void testAssignComplaint() {
        Complaint complaint = new Complaint();
        complaint.setId(UUID.randomUUID());
        complaint.setComplaintNumber("CP-2026-000001");
        complaint.setStatus("SUBMITTED");
        complaint.setCitizen(citizen);

        when(complaintDao.findById(complaint.getId())).thenReturn(Optional.of(complaint));
        when(userDao.findById(fieldWorker.getId())).thenReturn(Optional.of(fieldWorker));
        when(complaintDao.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));

        AssignComplaintRequest request = new AssignComplaintRequest();
        request.setAssignedToUserId(fieldWorker.getId());
        request.setNotes("Dispatch immediately");

        ComplaintResponse response = complaintService.assignComplaint(complaint.getId(), request, officer);

        assertThat(response.getStatus()).isEqualTo("ASSIGNED");
        assertThat(response.getAssignedToId()).isEqualTo(fieldWorker.getId());
        verify(complaintDao).saveStatusHistory(any(ComplaintStatusHistory.class));
    }

    @Test
    @DisplayName("Submit Resolution: Transitions status to RESOLVED and SLA status to RESOLVED")
    void testSubmitResolution() {
        Complaint complaint = new Complaint();
        complaint.setId(UUID.randomUUID());
        complaint.setComplaintNumber("CP-2026-000001");
        complaint.setStatus("IN_PROGRESS");
        complaint.setCitizen(citizen);
        complaint.setSlaDeadline(Instant.now().plusSeconds(3600));

        when(complaintDao.findById(complaint.getId())).thenReturn(Optional.of(complaint));
        when(complaintDao.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));

        ComplaintResponse response = complaintService.submitResolution(
                complaint.getId(),
                "Replaced burst valve and restored water pressure.",
                List.of(),
                fieldWorker
        );

        assertThat(response.getStatus()).isEqualTo("RESOLVED");
        assertThat(response.getResolutionNotes()).contains("Replaced burst valve");
        assertThat(response.getSlaStatus()).isEqualTo("RESOLVED");
        verify(complaintDao).saveStatusHistory(any(ComplaintStatusHistory.class));
    }

    @Test
    @DisplayName("Verify Resolution: ACCEPT closes complaint, REJECT moves back to IN_PROGRESS")
    void testVerifyResolution() {
        Complaint complaint = new Complaint();
        complaint.setId(UUID.randomUUID());
        complaint.setComplaintNumber("CP-2026-000001");
        complaint.setStatus("RESOLVED");
        complaint.setCitizen(citizen);

        when(complaintDao.findById(complaint.getId())).thenReturn(Optional.of(complaint));
        when(complaintDao.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));

        // Accept
        ComplaintResponse accepted = complaintService.verifyResolution(complaint.getId(), "ACCEPTED", "Work confirmed", officer);
        assertThat(accepted.getStatus()).isEqualTo("CLOSED");

        // Reject
        complaint.setStatus("RESOLVED");
        ComplaintResponse rejected = complaintService.verifyResolution(complaint.getId(), "REJECTED", "Leak still detected", officer);
        assertThat(rejected.getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("Change Priority: Updates priority, recalculates SLA, records audit trail")
    void testChangePriority() {
        Complaint complaint = new Complaint();
        complaint.setId(UUID.randomUUID());
        complaint.setComplaintNumber("CP-2026-000001");
        complaint.setPriority("LOW");
        complaint.setSlaMinutes(2880);
        complaint.setCreatedAt(Instant.now());

        when(complaintDao.findById(complaint.getId())).thenReturn(Optional.of(complaint));
        when(complaintDao.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));

        ChangePriorityRequest request = new ChangePriorityRequest();
        request.setPriority("CRITICAL");
        request.setReason("Escalated due to public hazard");

        ComplaintResponse response = complaintService.changePriority(complaint.getId(), request, admin);

        assertThat(response.getPriority()).isEqualTo("CRITICAL");
        assertThat(response.getSlaMinutes()).isEqualTo(240); // 4 hours
        verify(complaintDao).saveStatusHistory(any(ComplaintStatusHistory.class));
    }

    @Test
    @DisplayName("Download Receipt PDF/Text: Generates formatted receipt")
    void testReceiptGeneration() {
        ComplaintReceipt receipt = new ComplaintReceipt();
        receipt.setReceiptNumber("RCP-2026-000001");
        receipt.setComplaintRecordNumber("CP-2026-000001");
        receipt.setCitizenName("Aarav Mehta");
        receipt.setCitizenPhone("+919876543210");
        receipt.setTitle("Burst Pipeline");
        receipt.setCategory("WATER_SUPPLY");
        receipt.setDepartmentName("Water Supply Board");
        receipt.setPriority("HIGH");
        receipt.setExpectedResolutionHours(12);
        receipt.setGeneratedAt(Instant.now());

        UUID cid = UUID.randomUUID();
        when(complaintDao.findReceiptByComplaintId(cid)).thenReturn(Optional.of(receipt));

        byte[] pdfBytes = complaintService.getReceiptPdf(cid);
        assertThat(pdfBytes).isNotEmpty();
        String text = new String(pdfBytes);
        assertThat(text).contains("OFFICIAL CITIZEN GRIEVANCE ACKNOWLEDGEMENT RECEIPT");
        assertThat(text).contains("RCP-2026-000001");
        assertThat(text).contains("CP-2026-000001");
        assertThat(text).contains("Aarav Mehta");
    }
}

