package com.smartcity.incident;

import com.smartcity.department.Department;
import com.smartcity.department.DepartmentRepository;
import com.smartcity.incident.dto.CreateIncidentRequest;
import com.smartcity.incident.dto.IncidentResponse;
import com.smartcity.incident.dto.IncidentStatusHistoryResponse;
import com.smartcity.incident.dto.UpdateIncidentRequest;
import com.smartcity.incident.dto.UpdateIncidentStatusRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class IncidentIntegrationTest {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    @DisplayName("Complete Incident Lifecycle: Create, Status History Audit, Filter, Update, Transition and Delete")
    void testIncidentLifecycleAndStatusHistory() {
        Department dept = new Department();
        dept.setCode("EMERGENCY_OPS_" + UUID.randomUUID().toString().substring(0, 6));
        dept.setName("Emergency Operations Centre");
        dept = departmentRepository.saveAndFlush(dept);

        // 1. Create Incident
        CreateIncidentRequest createReq = new CreateIncidentRequest();
        createReq.setIncidentType("FLASH_FLOOD");
        createReq.setDescription("Underpass submerged with 1.5m water");
        createReq.setSource(IncidentSource.IOT);
        createReq.setLatitude(12.9352);
        createReq.setLongitude(77.6245);
        createReq.setAddress("Koramangala 80ft Road");
        createReq.setDepartmentId(dept.getId());
        createReq.setCreatedByUsername("iot_water_sensor_11");

        IncidentResponse created = incidentService.createIncident(createReq);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getIncidentNumber()).startsWith("INC-");
        assertThat(created.getStatus()).isEqualTo(IncidentStatus.DETECTED);
        assertThat(created.getDepartmentCode()).isEqualTo(dept.getCode());
        // Requirement 13: priority/tier unset initially, SLA assigned by SLA service
        assertThat(created.getPriority()).isNull();
        assertThat(created.getAssignedTier()).isNull();
        assertThat(created.getSlaMinutes()).isNotNull();
        assertThat(created.getSlaDeadline()).isNotNull();

        // 2. Verify Initial Status History (Requirement 11)
        List<IncidentStatusHistoryResponse> history1 = incidentService.getIncidentStatusHistory(created.getId());
        assertThat(history1).hasSize(1);
        assertThat(history1.get(0).getPreviousStatus()).isNull();
        assertThat(history1.get(0).getNewStatus()).isEqualTo("DETECTED");
        assertThat(history1.get(0).getChangedBy()).isEqualTo("iot_water_sensor_11");

        // 3. Filter Incidents (Requirement 7)
        Page<IncidentResponse> filtered = incidentService.getAllIncidents(
                IncidentStatus.DETECTED,
                "FLASH_FLOOD",
                IncidentSource.IOT,
                dept.getId(),
                PageRequest.of(0, 10)
        );
        assertThat(filtered.getTotalElements()).isGreaterThanOrEqualTo(1);

        // 4. Update Incident
        UpdateIncidentRequest updateReq = new UpdateIncidentRequest();
        updateReq.setSeverityIndicator("EXTREME");
        updateReq.setAddress("Koramangala 80ft Road Underpass #2");
        IncidentResponse updated = incidentService.updateIncident(created.getId(), updateReq);
        assertThat(updated.getSeverityIndicator()).isEqualTo("EXTREME");
        assertThat(updated.getAddress()).isEqualTo("Koramangala 80ft Road Underpass #2");

        // 5. Update Incident Status Transition (Requirement 11)
        UpdateIncidentStatusRequest statusReq1 = new UpdateIncidentStatusRequest();
        statusReq1.setStatus(IncidentStatus.ASSIGNED);
        statusReq1.setChangedBy("dispatcher_hq");
        statusReq1.setRemarks("Assigned to Quick Reaction Team Alpha");

        IncidentResponse statusUpdated1 = incidentService.updateIncidentStatus(created.getId(), statusReq1);
        assertThat(statusUpdated1.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);

        UpdateIncidentStatusRequest statusReq2 = new UpdateIncidentStatusRequest();
        statusReq2.setStatus(IncidentStatus.RESOLVED);
        statusReq2.setChangedBy("captain_alpha");
        statusReq2.setRemarks("Pumps drained water; underpass cleared and reopened");

        IncidentResponse statusUpdated2 = incidentService.updateIncidentStatus(created.getId(), statusReq2);
        assertThat(statusUpdated2.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(statusUpdated2.getResolutionTime()).isNotNull();

        // 6. Check Status History Audit Trail
        List<IncidentStatusHistoryResponse> historyAll = incidentService.getIncidentStatusHistory(created.getId());
        assertThat(historyAll).hasSize(3);
        // Ordered by changedAt desc
        assertThat(historyAll.get(0).getNewStatus()).isEqualTo("RESOLVED");
        assertThat(historyAll.get(0).getPreviousStatus()).isEqualTo("ASSIGNED");
        assertThat(historyAll.get(0).getChangedBy()).isEqualTo("captain_alpha");

        assertThat(historyAll.get(1).getNewStatus()).isEqualTo("ASSIGNED");
        assertThat(historyAll.get(1).getPreviousStatus()).isEqualTo("DETECTED");
        assertThat(historyAll.get(1).getChangedBy()).isEqualTo("dispatcher_hq");

        assertThat(historyAll.get(2).getNewStatus()).isEqualTo("DETECTED");

        // 7. Delete Incident
        incidentService.deleteIncident(created.getId());
    }
}
