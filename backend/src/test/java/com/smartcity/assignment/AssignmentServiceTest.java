package com.smartcity.assignment;

import com.smartcity.assignment.dto.AssignIncidentRequest;
import com.smartcity.assignment.dto.AssignmentHistoryResponse;
import com.smartcity.common.ResourceNotFoundException;
import com.smartcity.department.Department;
import com.smartcity.department.DepartmentRepository;
import com.smartcity.department.Team;
import com.smartcity.department.TeamRepository;
import com.smartcity.incident.Incident;
import com.smartcity.incident.IncidentRepository;
import com.smartcity.incident.IncidentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Assignment Service Unit Tests")
class AssignmentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private AssignmentRuleRepository assignmentRuleRepository;

    @Mock
    private IncidentAssignmentHistoryRepository historyRepository;

    private AssignmentServiceImpl assignmentService;

    private Department emergencyDept;
    private Department pwdDept;
    private Department trafficDept;
    private Department waterDept;
    private Department envDept;
    private Department sanitationDept;

    private Team trafficTeam;
    private Team pwdTeam;
    private Team emergTeam;

    @BeforeEach
    void setUp() {
        assignmentService = new AssignmentServiceImpl(
                incidentRepository,
                departmentRepository,
                teamRepository,
                assignmentRuleRepository,
                historyRepository
        );

        emergencyDept = new Department("EMERGENCY", "Emergency Services", "Emergency", "em@city.gov", "112");
        emergencyDept.setId(UUID.randomUUID());

        pwdDept = new Department("PWD", "Public Works", "Public Works", "pwd@city.gov", "113");
        pwdDept.setId(UUID.randomUUID());

        trafficDept = new Department("TRAFFIC", "Traffic Department", "Traffic", "trf@city.gov", "114");
        trafficDept.setId(UUID.randomUUID());

        waterDept = new Department("WATER", "Water Department", "Water", "water@city.gov", "115");
        waterDept.setId(UUID.randomUUID());

        envDept = new Department("ENVIRONMENT", "Environment Department", "Environment", "env@city.gov", "116");
        envDept.setId(UUID.randomUUID());

        sanitationDept = new Department("SANITATION", "Sanitation Department", "Sanitation", "san@city.gov", "117");
        sanitationDept.setId(UUID.randomUUID());

        trafficTeam = new Team(trafficDept, "TRAFFIC_OPS", "Traffic Operations Response Team", "1095");
        trafficTeam.setId(UUID.randomUUID());

        pwdTeam = new Team(pwdDept, "PWD_ROAD", "Road & Infrastructure Repair Team", "1800-PWD");
        pwdTeam.setId(UUID.randomUUID());

        emergTeam = new Team(emergencyDept, "EMERG_RAPID", "Emergency Rapid Response Team", "112");
        emergTeam.setId(UUID.randomUUID());
    }

    private void mockActiveRules() {
        List<AssignmentRule> rules = List.of(
                new AssignmentRule("TRAFFIC_SIGNAL_FAILURE", "TRAFFIC", "Traffic Department", "TRAFFIC_OPS", "Traffic signal failure at junction"),
                new AssignmentRule("WASTE_OVERFLOW", "SANITATION", "Sanitation Department", "SANIT_SQUAD", "Waste bin overflow"),
                new AssignmentRule("PIPELINE_BURST", "WATER", "Water Department", "WATER_PIPE", "Main water pipeline burst"),
                new AssignmentRule("ROAD_DAMAGE", "PWD", "Public Works", "PWD_ROAD", "Road surface damage"),
                new AssignmentRule("ROAD_CRACK", "PWD", "Public Works", "PWD_ROAD", "Road surface damage"),
                new AssignmentRule("AIR_QUALITY_HAZARD", "ENVIRONMENT", "Environment Department", "ENV_HAZARD", "Hazardous smog"),
                new AssignmentRule("MAJOR_ROAD_ACCIDENT", "EMERGENCY", "Emergency Services", "EMERG_RAPID", "Major collision")
        );
        when(assignmentRuleRepository.findByActiveTrue()).thenReturn(rules);
    }

    @Test
    @DisplayName("1. Traffic signal failure maps to Traffic Department")
    void testTrafficSignalMapsToTrafficDepartment() {
        mockActiveRules();
        Optional<AssignmentRule> rule = assignmentService.findMatchingRule("TRAFFIC_SIGNAL_FAILURE");
        assertTrue(rule.isPresent());
        assertEquals("TRAFFIC", rule.get().getDepartmentCode());
        assertEquals("Traffic Department", rule.get().getDepartmentName());
    }

    @Test
    @DisplayName("2. Waste overflow maps to Sanitation Department")
    void testWasteOverflowMapsToSanitationDepartment() {
        mockActiveRules();
        Optional<AssignmentRule> rule = assignmentService.findMatchingRule("WASTE_OVERFLOW");
        assertTrue(rule.isPresent());
        assertEquals("SANITATION", rule.get().getDepartmentCode());
        assertEquals("Sanitation Department", rule.get().getDepartmentName());
    }

    @Test
    @DisplayName("3. Pipeline burst maps to Water Department")
    void testPipelineBurstMapsToWaterDepartment() {
        mockActiveRules();
        Optional<AssignmentRule> rule = assignmentService.findMatchingRule("PIPELINE_BURST");
        assertTrue(rule.isPresent());
        assertEquals("WATER", rule.get().getDepartmentCode());
        assertEquals("Water Department", rule.get().getDepartmentName());
    }

    @Test
    @DisplayName("4. Road damage maps to Public Works Department")
    void testRoadDamageMapsToPublicWorks() {
        mockActiveRules();
        Optional<AssignmentRule> rule = assignmentService.findMatchingRule("ROAD_DAMAGE");
        assertTrue(rule.isPresent());
        assertEquals("PWD", rule.get().getDepartmentCode());
        assertEquals("Public Works", rule.get().getDepartmentName());
    }

    @Test
    @DisplayName("5. Air-quality incident maps to Environment Department")
    void testAirQualityMapsToEnvironmentDepartment() {
        mockActiveRules();
        Optional<AssignmentRule> rule = assignmentService.findMatchingRule("AIR_QUALITY_HAZARD");
        assertTrue(rule.isPresent());
        assertEquals("ENVIRONMENT", rule.get().getDepartmentCode());
        assertEquals("Environment Department", rule.get().getDepartmentName());
    }

    @Test
    @DisplayName("6. Accident maps to Emergency Services")
    void testAccidentMapsToEmergencyServices() {
        mockActiveRules();
        Optional<AssignmentRule> rule = assignmentService.findMatchingRule("MAJOR_ROAD_ACCIDENT");
        assertTrue(rule.isPresent());
        assertEquals("EMERGENCY", rule.get().getDepartmentCode());
        assertEquals("Emergency Services", rule.get().getDepartmentName());
    }

    @Test
    @DisplayName("7. No matching department returns a controlled error")
    void testNoMatchingDepartmentThrowsControlledError() {
        when(assignmentRuleRepository.findByActiveTrue()).thenReturn(List.of());

        Incident incident = new Incident();
        incident.setId(UUID.randomUUID());
        incident.setIncidentType("UNKNOWN_SPACE_INVASION");
        when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                assignmentService.autoAssign(incident.getId().toString()));

        assertTrue(ex.getMessage().contains("No suitable department found"));
        verify(incidentRepository, never()).save(any());
    }

    @Test
    @DisplayName("8. Auto-assignment automatically assigns department, active team, and advances status")
    void testAutoAssignmentWorkflow() {
        mockActiveRules();

        Incident incident = new Incident();
        incident.setId(UUID.randomUUID());
        incident.setIncidentNumber("INC-TRF-001");
        incident.setIncidentType("TRAFFIC_SIGNAL_FAILURE");
        incident.setStatus(IncidentStatus.PRIORITIZED);

        when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));
        when(departmentRepository.findByCode("TRAFFIC")).thenReturn(Optional.of(trafficDept));
        when(teamRepository.findByCode("TRAFFIC_OPS")).thenReturn(Optional.of(trafficTeam));

        AssignmentResult result = assignmentService.autoAssign(incident.getId().toString());

        assertNotNull(result);
        assertEquals("Traffic Department", result.getDepartment());
        assertEquals("Traffic Operations Response Team", result.getTeam());
        assertEquals(trafficDept.getId(), incident.getAssignedDepartment().getId());
        assertEquals(trafficTeam.getId(), incident.getAssignedTeam().getId());
        assertEquals(IncidentStatus.ASSIGNED, incident.getStatus());
        assertEquals("AUTO_ASSIGNER", incident.getAssignedBy());
        assertNotNull(incident.getAssignedAt());

        verify(incidentRepository).save(incident);
        verify(historyRepository).save(any(IncidentAssignmentHistory.class));
    }

    @Test
    @DisplayName("9. Manual assignment assigns specified department and team")
    void testManualAssignment() {
        Incident incident = new Incident();
        incident.setId(UUID.randomUUID());
        incident.setIncidentNumber("INC-PWD-001");
        incident.setStatus(IncidentStatus.DETECTED);

        when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));
        when(departmentRepository.findById(pwdDept.getId())).thenReturn(Optional.of(pwdDept));
        when(teamRepository.findById(pwdTeam.getId())).thenReturn(Optional.of(pwdTeam));

        AssignIncidentRequest request = new AssignIncidentRequest(
                pwdDept.getId(), pwdTeam.getId(), "OPERATOR_ALICE", "Dispatched to fix roadway"
        );

        AssignmentResult result = assignmentService.assign(incident.getId().toString(), request);

        assertNotNull(result);
        assertEquals("Public Works", result.getDepartment());
        assertEquals("Road & Infrastructure Repair Team", result.getTeam());
        assertEquals("Dispatched to fix roadway", result.getReason());
        assertEquals("OPERATOR_ALICE", incident.getAssignedBy());
        assertEquals(IncidentStatus.ASSIGNED, incident.getStatus());

        verify(historyRepository).save(any(IncidentAssignmentHistory.class));
    }

    @Test
    @DisplayName("10. Assignment history is auditable and queryable")
    void testAssignmentHistoryTracking() {
        Incident incident = new Incident();
        incident.setId(UUID.randomUUID());
        incident.setIncidentNumber("INC-HIST-001");

        when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));

        IncidentAssignmentHistory h1 = new IncidentAssignmentHistory(
                incident, "OPERATOR_BOB", null, pwdDept, null, pwdTeam, "Initial assignment"
        );
        h1.setId(UUID.randomUUID());

        when(historyRepository.findByIncidentIdOrderByAssignedAtDesc(incident.getId()))
                .thenReturn(List.of(h1));

        List<AssignmentHistoryResponse> history = assignmentService.getAssignmentHistory(incident.getId().toString());

        assertEquals(1, history.size());
        assertEquals("OPERATOR_BOB", history.get(0).getAssignedBy());
        assertEquals("Public Works", history.get(0).getNewDepartmentName());
        assertEquals("Road & Infrastructure Repair Team", history.get(0).getNewTeamName());
    }

    @Test
    @DisplayName("13. Invalid department or team validation")
    void testInvalidDepartmentOrTeam() {
        Incident incident = new Incident();
        incident.setId(UUID.randomUUID());
        when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));

        UUID nonExistentDeptId = UUID.randomUUID();
        when(departmentRepository.findById(nonExistentDeptId)).thenReturn(Optional.empty());

        AssignIncidentRequest request1 = new AssignIncidentRequest(nonExistentDeptId, null, "ADMIN", "Invalid test");
        assertThrows(ResourceNotFoundException.class, () ->
                assignmentService.assign(incident.getId().toString(), request1));

        // Team belongs to another department
        when(departmentRepository.findById(pwdDept.getId())).thenReturn(Optional.of(pwdDept));
        when(teamRepository.findById(trafficTeam.getId())).thenReturn(Optional.of(trafficTeam));

        AssignIncidentRequest request2 = new AssignIncidentRequest(pwdDept.getId(), trafficTeam.getId(), "ADMIN", "Mismatched team");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                assignmentService.assign(incident.getId().toString(), request2));

        assertTrue(ex.getMessage().contains("does not belong to department"));
    }

    @Test
    @DisplayName("14. Inactive department or inactive team throws error")
    void testInactiveDepartmentOrTeam() {
        Incident incident = new Incident();
        incident.setId(UUID.randomUUID());
        when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));

        Department inactiveDept = new Department("INACTIVE_DEPT", "Inactive Dept", "Disabled", "x@x.com", "000");
        inactiveDept.setId(UUID.randomUUID());
        inactiveDept.setActive(false);

        when(departmentRepository.findById(inactiveDept.getId())).thenReturn(Optional.of(inactiveDept));

        AssignIncidentRequest request1 = new AssignIncidentRequest(inactiveDept.getId(), null, "ADMIN", "Inactive dept test");
        IllegalStateException ex1 = assertThrows(IllegalStateException.class, () ->
                assignmentService.assign(incident.getId().toString(), request1));
        assertTrue(ex1.getMessage().contains("Cannot assign to inactive department"));

        // Inactive team
        Team inactiveTeam = new Team(pwdDept, "INACTIVE_TEAM", "Inactive Team", "000", false);
        inactiveTeam.setId(UUID.randomUUID());

        when(departmentRepository.findById(pwdDept.getId())).thenReturn(Optional.of(pwdDept));
        when(teamRepository.findById(inactiveTeam.getId())).thenReturn(Optional.of(inactiveTeam));

        AssignIncidentRequest request2 = new AssignIncidentRequest(pwdDept.getId(), inactiveTeam.getId(), "ADMIN", "Inactive team test");
        IllegalStateException ex2 = assertThrows(IllegalStateException.class, () ->
                assignmentService.assign(incident.getId().toString(), request2));
        assertTrue(ex2.getMessage().contains("Cannot assign to inactive team"));
    }
}
