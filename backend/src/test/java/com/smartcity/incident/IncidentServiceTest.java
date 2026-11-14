package com.smartcity.incident;

import com.smartcity.common.ResourceNotFoundException;
import com.smartcity.department.Department;
import com.smartcity.department.DepartmentRepository;
import com.smartcity.department.Team;
import com.smartcity.department.TeamRepository;
import com.smartcity.incident.dto.CreateIncidentRequest;
import com.smartcity.incident.dto.IncidentResponse;
import com.smartcity.incident.dto.IncidentStatusHistoryResponse;
import com.smartcity.incident.dto.UpdateIncidentRequest;
import com.smartcity.incident.dto.UpdateIncidentStatusRequest;
import com.smartcity.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentStatusHistoryRepository statusHistoryRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private IncidentServiceImpl incidentService;

    private Incident sampleIncident;
    private UUID sampleId;

    @BeforeEach
    void setUp() {
        sampleId = UUID.randomUUID();
        sampleIncident = new Incident();
        sampleIncident.setId(sampleId);
        sampleIncident.setIncidentNumber("INC-20261003-TEST001");
        sampleIncident.setIncidentType("WATER_LEAK");
        sampleIncident.setDescription("Water main burst flooding avenue");
        sampleIncident.setSource(IncidentSource.CITIZEN);
        sampleIncident.setLatitude(12.9716);
        sampleIncident.setLongitude(77.5946);
        sampleIncident.setAddress("MG Road, Ward 110");
        sampleIncident.setStatus(IncidentStatus.DETECTED);
        sampleIncident.setCreatedAt(Instant.now());
        sampleIncident.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("createIncident creates entity with DETECTED status, null priority/tier/SLA and initial history entry")
    void createIncident_shouldSetDefaultsAndRecordInitialHistory() {
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setIncidentType("TRAFFIC_COLLISION");
        request.setDescription("Collision at central junction");
        request.setSource(IncidentSource.OPERATOR);
        request.setLatitude(12.975);
        request.setLongitude(77.601);
        request.setAddress("Brigade Road");
        request.setCreatedByUsername("operator_1");

        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> {
            Incident inc = invocation.getArgument(0);
            inc.setId(UUID.randomUUID());
            return inc;
        });

        IncidentResponse response = incidentService.createIncident(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getIncidentNumber()).startsWith("INC-");
        assertThat(response.getIncidentType()).isEqualTo("TRAFFIC_COLLISION");
        assertThat(response.getSource()).isEqualTo(IncidentSource.OPERATOR);
        assertThat(response.getStatus()).isEqualTo(IncidentStatus.DETECTED);
        assertThat(response.getPriority()).isNull();
        assertThat(response.getAssignedTier()).isNull();
        assertThat(response.getSlaMinutes()).isNull();
        assertThat(response.getSlaDeadline()).isNull();

        // Verify status history recorded
        ArgumentCaptor<IncidentStatusHistory> historyCaptor = ArgumentCaptor.forClass(IncidentStatusHistory.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        IncidentStatusHistory capturedHistory = historyCaptor.getValue();
        assertThat(capturedHistory.getPreviousStatus()).isNull();
        assertThat(capturedHistory.getNewStatus()).isEqualTo("DETECTED");
        assertThat(capturedHistory.getChangedBy()).isEqualTo("operator_1");
    }

    @Test
    @DisplayName("createIncident resolves department and team when provided")
    void createIncident_withDepartmentAndTeam_shouldResolveAssociations() {
        UUID deptId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        Department dept = new Department();
        dept.setId(deptId);
        dept.setCode("PWD");
        dept.setName("Public Works Department");

        Team team = new Team();
        team.setId(teamId);
        team.setName("Road Maintenance Alpha");

        when(departmentRepository.findById(deptId)).thenReturn(Optional.of(dept));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setIncidentType("POTHOLE");
        request.setDescription("Deep pothole in outer lane");
        request.setSource(IncidentSource.CITIZEN);
        request.setLatitude(12.91);
        request.setLongitude(77.58);
        request.setDepartmentId(deptId);
        request.setAssignedTeamId(teamId);

        IncidentResponse response = incidentService.createIncident(request);

        assertThat(response.getDepartmentId()).isEqualTo(deptId);
        assertThat(response.getDepartmentName()).isEqualTo("Public Works Department");
        assertThat(response.getAssignedTeamId()).isEqualTo(teamId);
        assertThat(response.getAssignedTeamName()).isEqualTo("Road Maintenance Alpha");
    }

    @Test
    @DisplayName("createIncident throws ResourceNotFoundException if department does not exist")
    void createIncident_departmentNotFound_shouldThrowException() {
        UUID invalidDeptId = UUID.randomUUID();
        when(departmentRepository.findById(invalidDeptId)).thenReturn(Optional.empty());

        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setIncidentType("POTHOLE");
        request.setDescription("Pothole");
        request.setSource(IncidentSource.CITIZEN);
        request.setLatitude(12.91);
        request.setLongitude(77.58);
        request.setDepartmentId(invalidDeptId);

        assertThatThrownBy(() -> incidentService.createIncident(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Department not found");
    }

    @Test
    @DisplayName("getIncidentById returns IncidentResponse when found")
    void getIncidentById_whenFound_shouldReturnResponse() {
        when(incidentRepository.findById(sampleId)).thenReturn(Optional.of(sampleIncident));

        IncidentResponse response = incidentService.getIncidentById(sampleId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(sampleId);
        assertThat(response.getIncidentNumber()).isEqualTo("INC-20261003-TEST001");
        assertThat(response.getStatus()).isEqualTo(IncidentStatus.DETECTED);
    }

    @Test
    @DisplayName("getIncidentById throws ResourceNotFoundException when not found")
    void getIncidentById_whenNotFound_shouldThrowResourceNotFoundException() {
        UUID nonExistentId = UUID.randomUUID();
        when(incidentRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.getIncidentById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Incident not found");
    }

    @Test
    @DisplayName("getAllIncidents returns paged and filtered results")
    void getAllIncidents_withFilters_shouldReturnPagedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Incident> list = List.of(sampleIncident);
        Page<Incident> page = new PageImpl<>(list, pageable, 1);

        when(incidentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<IncidentResponse> result = incidentService.getAllIncidents(
                IncidentStatus.DETECTED,
                "WATER_LEAK",
                IncidentSource.CITIZEN,
                null,
                pageable
        );

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getIncidentType()).isEqualTo("WATER_LEAK");
    }

    @Test
    @DisplayName("updateIncident updates mutable fields and returns response")
    void updateIncident_shouldUpdateFields() {
        when(incidentRepository.findById(sampleId)).thenReturn(Optional.of(sampleIncident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateIncidentRequest updateReq = new UpdateIncidentRequest();
        updateReq.setDescription("Updated water main description");
        updateReq.setSeverityIndicator("MODERATE");

        IncidentResponse response = incidentService.updateIncident(sampleId, updateReq);

        assertThat(response.getDescription()).isEqualTo("Updated water main description");
        assertThat(response.getSeverityIndicator()).isEqualTo("MODERATE");
    }

    @Test
    @DisplayName("updateIncidentStatus updates status, sets resolution time if RESOLVED, and logs history")
    void updateIncidentStatus_shouldUpdateStatusAndRecordHistory() {
        when(incidentRepository.findById(sampleId)).thenReturn(Optional.of(sampleIncident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateIncidentStatusRequest statusReq = new UpdateIncidentStatusRequest();
        statusReq.setStatus(IncidentStatus.IN_PROGRESS);
        statusReq.setChangedBy("field_officer_1");
        statusReq.setRemarks("Crew dispatched to site");

        IncidentResponse response = incidentService.updateIncidentStatus(sampleId, statusReq);

        assertThat(response.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);

        ArgumentCaptor<IncidentStatusHistory> historyCaptor = ArgumentCaptor.forClass(IncidentStatusHistory.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        IncidentStatusHistory capturedHistory = historyCaptor.getValue();
        assertThat(capturedHistory.getPreviousStatus()).isEqualTo("DETECTED");
        assertThat(capturedHistory.getNewStatus()).isEqualTo("IN_PROGRESS");
        assertThat(capturedHistory.getChangedBy()).isEqualTo("field_officer_1");
        assertThat(capturedHistory.getRemarks()).isEqualTo("Crew dispatched to site");
    }

    @Test
    @DisplayName("updateIncidentStatus sets resolution time when transition is RESOLVED")
    void updateIncidentStatus_whenResolved_shouldSetResolutionTime() {
        when(incidentRepository.findById(sampleId)).thenReturn(Optional.of(sampleIncident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateIncidentStatusRequest statusReq = new UpdateIncidentStatusRequest();
        statusReq.setStatus(IncidentStatus.RESOLVED);
        statusReq.setChangedBy("field_officer_1");
        statusReq.setRemarks("Pipe repaired successfully");

        IncidentResponse response = incidentService.updateIncidentStatus(sampleId, statusReq);

        assertThat(response.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(response.getResolutionTime()).isNotNull();
    }

    @Test
    @DisplayName("deleteIncident removes entity when found")
    void deleteIncident_whenFound_shouldDelete() {
        when(incidentRepository.findById(sampleId)).thenReturn(Optional.of(sampleIncident));

        incidentService.deleteIncident(sampleId);

        verify(incidentRepository).delete(sampleIncident);
    }

    @Test
    @DisplayName("getIncidentStatusHistory returns ordered history")
    void getIncidentStatusHistory_shouldReturnHistoryList() {
        when(incidentRepository.existsById(sampleId)).thenReturn(true);

        IncidentStatusHistory h1 = new IncidentStatusHistory(sampleIncident, "DETECTED", "ASSIGNED", "dispatcher", "Assigned to team");
        h1.setId(UUID.randomUUID());
        h1.setChangedAt(Instant.now());

        when(statusHistoryRepository.findByIncidentIdOrderByChangedAtDesc(sampleId)).thenReturn(List.of(h1));

        List<IncidentStatusHistoryResponse> history = incidentService.getIncidentStatusHistory(sampleId);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getNewStatus()).isEqualTo("ASSIGNED");
        assertThat(history.get(0).getRemarks()).isEqualTo("Assigned to team");
    }
}
