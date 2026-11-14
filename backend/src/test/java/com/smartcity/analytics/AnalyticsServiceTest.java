package com.smartcity.analytics;

import com.smartcity.analytics.dto.*;
import com.smartcity.department.Department;
import com.smartcity.department.DepartmentRepository;
import com.smartcity.incident.Incident;
import com.smartcity.incident.IncidentRepository;
import com.smartcity.incident.IncidentStatus;
import com.smartcity.iot.IotEventRepository;
import com.smartcity.iot.SensorDeviceRepository;
import com.smartcity.iot.SensorReadingRepository;
import com.smartcity.priority.PriorityTier;
import com.smartcity.sla.SLARecord;
import com.smartcity.sla.SLARepository;
import com.smartcity.sla.SLAStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AnalyticsServiceTest {

    private IncidentRepository incidentRepository;
    private SLARepository slaRepository;
    private DepartmentRepository departmentRepository;
    private SensorDeviceRepository sensorDeviceRepository;
    private SensorReadingRepository sensorReadingRepository;
    private IotEventRepository iotEventRepository;
    private AnalyticsServiceImpl analyticsService;

    @BeforeEach
    void setUp() {
        incidentRepository = mock(IncidentRepository.class);
        slaRepository = mock(SLARepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        sensorDeviceRepository = mock(SensorDeviceRepository.class);
        sensorReadingRepository = mock(SensorReadingRepository.class);
        iotEventRepository = mock(IotEventRepository.class);

        analyticsService = new AnalyticsServiceImpl(
                incidentRepository,
                slaRepository,
                departmentRepository,
                sensorDeviceRepository,
                sensorReadingRepository,
                iotEventRepository
        );
    }

    @Test
    @DisplayName("Overview analytics computes accurate database aggregates without inventing metrics")
    void testOverviewAnalyticsComputation() {
        Department dept = new Department("TRAFFIC", "Traffic Police");
        dept.setId(UUID.randomUUID());

        Incident inc1 = new Incident();
        inc1.setId(UUID.randomUUID());
        inc1.setIncidentNumber("INC-001");
        inc1.setStatus(IncidentStatus.ASSIGNED);
        inc1.setPriorityTier(PriorityTier.CRITICAL);
        inc1.setAssignedDepartment(dept);
        inc1.setCreatedAt(Instant.now().minusSeconds(3600));

        Incident inc2 = new Incident();
        inc2.setId(UUID.randomUUID());
        inc2.setIncidentNumber("INC-002");
        inc2.setStatus(IncidentStatus.RESOLVED);
        inc2.setPriorityTier(PriorityTier.MEDIUM);
        inc2.setAssignedDepartment(dept);
        inc2.setCreatedAt(Instant.now().minusSeconds(7200));
        inc2.setResolvedAt(Instant.now().minusSeconds(3600)); // 1 hour resolution

        when(incidentRepository.findAll()).thenReturn(List.of(inc1, inc2));

        SLARecord sla1 = new SLARecord();
        sla1.setStatus(SLAStatus.ON_TRACK);
        SLARecord sla2 = new SLARecord();
        sla2.setStatus(SLAStatus.BREACHED);

        when(slaRepository.findAll()).thenReturn(List.of(sla1, sla2));
        when(departmentRepository.findAll()).thenReturn(List.of(dept));
        when(sensorDeviceRepository.count()).thenReturn(15L);
        when(iotEventRepository.count()).thenReturn(3L);

        OverviewAnalyticsResponse overview = analyticsService.getOverviewAnalytics(null, null);

        assertThat(overview.getTotalIncidents()).isEqualTo(2);
        assertThat(overview.getActiveIncidents()).isEqualTo(1);
        assertThat(overview.getResolvedIncidents()).isEqualTo(1);
        assertThat(overview.getCriticalIncidents()).isEqualTo(1);
        assertThat(overview.getSlaBreaches()).isEqualTo(1);
        assertThat(overview.getSlaComplianceRate()).isEqualTo(50.0); // 1 of 2 breached -> 50%
        assertThat(overview.getAvgResolutionTimeHours()).isEqualTo(1.0);
        assertThat(overview.getActiveSensors()).isEqualTo(15L);
        assertThat(overview.getSensorAlertsCount()).isEqualTo(3L);
        assertThat(overview.getPriorityDistribution().get("CRITICAL")).isEqualTo(1L);
        assertThat(overview.getPriorityDistribution().get("MEDIUM")).isEqualTo(1L);
    }

    @Test
    @DisplayName("SLA analytics computes compliance and escalation distributions accurately")
    void testSlaAnalyticsComputation() {
        SLARecord s1 = new SLARecord();
        s1.setStatus(SLAStatus.ON_TRACK);
        SLARecord s2 = new SLARecord();
        s2.setStatus(SLAStatus.AT_RISK);
        SLARecord s3 = new SLARecord();
        s3.setStatus(SLAStatus.BREACHED);

        when(slaRepository.findAll()).thenReturn(List.of(s1, s2, s3));

        SlaAnalyticsResponse res = analyticsService.getSlaAnalytics(null, null);

        assertThat(res.getTotalMonitored()).isEqualTo(3);
        assertThat(res.getOnTrack()).isEqualTo(1);
        assertThat(res.getAtRisk()).isEqualTo(1);
        assertThat(res.getBreached()).isEqualTo(1);
        assertThat(res.getComplianceRate()).isEqualTo(66.7);
    }
}
