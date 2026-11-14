package com.smartcity;

import com.smartcity.airquality.AirQualityReadingRepository;
import com.smartcity.common.AuditLog;
import com.smartcity.common.AuditLogRepository;
import com.smartcity.complaint.Complaint;
import com.smartcity.complaint.ComplaintRepository;
import com.smartcity.department.Department;
import com.smartcity.department.DepartmentRepository;
import com.smartcity.department.Team;
import com.smartcity.department.TeamRepository;
import com.smartcity.gis.EmergencyResource;
import com.smartcity.gis.EmergencyResourceRepository;
import com.smartcity.gis.Location;
import com.smartcity.gis.LocationRepository;
import com.smartcity.incident.*;
import com.smartcity.infrastructure.Infrastructure;
import com.smartcity.infrastructure.InfrastructureRepository;
import com.smartcity.iot.*;
import com.smartcity.notification.Notification;
import com.smartcity.notification.NotificationRepository;
import com.smartcity.priority.IncidentPriority;
import com.smartcity.priority.IncidentTier;
import com.smartcity.priority.PriorityRule;
import com.smartcity.priority.PriorityRuleRepository;
import com.smartcity.sla.SLARecord;
import com.smartcity.sla.SLARepository;
import com.smartcity.sla.SLAStatus;
import com.smartcity.traffic.TrafficReadingRepository;
import com.smartcity.traffic.TrafficSignal;
import com.smartcity.traffic.TrafficSignalRepository;
import com.smartcity.user.Role;
import com.smartcity.user.RoleRepository;
import com.smartcity.user.User;
import com.smartcity.user.UserRepository;
import com.smartcity.waste.WasteBin;
import com.smartcity.waste.WasteBinRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CivicPulseDatabaseFoundationTest {

    @Autowired private RoleRepository roleRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private PriorityRuleRepository priorityRuleRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private IncidentStatusHistoryRepository incidentStatusHistoryRepository;
    @Autowired private SLARepository slaRecordRepository;
    @Autowired private ComplaintRepository complaintRepository;
    @Autowired private InfrastructureRepository infrastructureRepository;
    @Autowired private TrafficSignalRepository trafficSignalRepository;
    @Autowired private WasteBinRepository wasteBinRepository;
    @Autowired private EmergencyResourceRepository emergencyResourceRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    @Autowired private SensorReadingRepository sensorReadingRepository;
    @Autowired private TrafficReadingRepository trafficReadingRepository;
    @Autowired private AirQualityReadingRepository airQualityReadingRepository;
    @Autowired private WeatherObservationRepository weatherObservationRepository;
    @Autowired private IotEventRepository iotEventRepository;

    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("Verify Spring context loads and all 16 PostgreSQL repositories are initialized")
    void testRepositoriesInitialized() {
        assertNotNull(roleRepository);
        assertNotNull(departmentRepository);
        assertNotNull(teamRepository);
        assertNotNull(userRepository);
        assertNotNull(locationRepository);
        assertNotNull(priorityRuleRepository);
        assertNotNull(incidentRepository);
        assertNotNull(incidentStatusHistoryRepository);
        assertNotNull(slaRecordRepository);
        assertNotNull(complaintRepository);
        assertNotNull(infrastructureRepository);
        assertNotNull(trafficSignalRepository);
        assertNotNull(wasteBinRepository);
        assertNotNull(emergencyResourceRepository);
        assertNotNull(notificationRepository);
        assertNotNull(auditLogRepository);
    }

    @Test
    @DisplayName("Verify MongoDB repositories are initialized")
    void testMongoRepositoriesInitialized() {
        assertNotNull(sensorReadingRepository);
        assertNotNull(trafficReadingRepository);
        assertNotNull(airQualityReadingRepository);
        assertNotNull(weatherObservationRepository);
        assertNotNull(iotEventRepository);
    }

    @Test
    @DisplayName("Verify Redis integration and ping connectivity")
    void testRedisIntegration() {
        assertNotNull(redisTemplate);
        assertNotNull(redisTemplate.getConnectionFactory());
        String ping = redisTemplate.getConnectionFactory().getConnection().ping();
        assertEquals("PONG", ping);
    }

    @Test
    @DisplayName("Verify PostgreSQL entities, UUID generation, foreign keys, and cascading relationships")
    void testEntityPersistenceAndRelationships() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // 1. Role
        Role role = roleRepository.saveAndFlush(new Role("COMMAND_OPERATOR_" + suffix, "Command Centre Watch Officer"));
        assertNotNull(role.getId());

        // 2. Department
        Department dept = departmentRepository.saveAndFlush(new Department("TRF_" + suffix, "Traffic Management", "Municipal Traffic & Mobility", "traffic_" + suffix + "@civicpulse.city", "112"));
        assertNotNull(dept.getId());

        // 3. Team
        Team team = teamRepository.saveAndFlush(new Team(dept, "TRF-ALPHA_" + suffix, "Traffic Rapid Alpha", "112"));
        assertNotNull(team.getId());
        assertEquals(dept.getId(), team.getDepartment().getId());

        // 4. User
        User user = userRepository.save(new User("officer_smith_" + suffix, "smith_" + suffix + "@civicpulse.city", "hash123", "Officer Smith", "9876543210", role, dept, team));
        assertNotNull(user.getId());
        assertEquals(role.getId(), user.getRole().getId());

        // 5. Location
        Location loc = locationRepository.saveAndFlush(new Location("LOC-SEC17-" + suffix, "Sector 17 Central Plaza", "SECTOR", 30.7380, 76.7840, "Ward 5", "Central"));
        assertNotNull(loc.getId());

        // 6. Priority Rule
        PriorityRule rule = priorityRuleRepository.saveAndFlush(new PriorityRule("PR-" + suffix, "traffic_signal_failure", IncidentTier.TIER_2, IncidentPriority.HIGH, 240, dept, "{}", "[]"));
        assertNotNull(rule.getId());

        // 7. Incident
        Incident incident = new Incident();
        incident.setIncidentNumber("INC-TEST-" + suffix);
        incident.setIncidentType("traffic_signal_failure");
        incident.setDescription("Signal grid blackout at Sector 17");
        incident.setSource(IncidentSource.OPERATOR);
        incident.setLatitude(30.7380);
        incident.setLongitude(76.7840);
        incident.setLocation(loc);
        incident.setAssignedTier(IncidentTier.TIER_2);
        incident.setPriority(IncidentPriority.HIGH);
        incident.setSlaMinutes(240);
        incident.setSlaDeadline(Instant.now().plusSeconds(14400));
        incident.setDepartment(dept);
        incident.setAssignedTeam(team);
        incident.setCurrentStatus(IncidentStatus.DETECTED);
        incident.setCreatedBy(user);
        Incident savedIncident = incidentRepository.saveAndFlush(incident);
        assertNotNull(savedIncident.getId());

        // 8. Incident Status History
        IncidentStatusHistory history = incidentStatusHistoryRepository.saveAndFlush(new IncidentStatusHistory(savedIncident, "DETECTED", "CLASSIFIED", user, user.getUsername(), "Auto-classified"));
        assertNotNull(history.getId());

        // 9. SLA Record
        SLARecord sla = slaRecordRepository.saveAndFlush(new SLARecord(savedIncident, 240, savedIncident.getSlaDeadline()));
        assertNotNull(sla.getId());
        assertEquals(savedIncident.getId(), sla.getIncident().getId());

        // 10. Complaint
        Complaint complaint = complaintRepository.saveAndFlush(new Complaint("CMP-TEST-" + suffix, user, "John Citizen", "9998887776", "traffic", "Signals not working", 30.7380, 76.7840, "Sector 17"));
        complaint.setIncident(savedIncident);
        assertNotNull(complaint.getId());

        // 11. Infrastructure
        Infrastructure infra = infrastructureRepository.saveAndFlush(new Infrastructure("INF-SIG-" + suffix, "Sector 17 Traffic Controller", "ROAD", 30.7380, 76.7840, "Sector 17", "PRIMARY", dept));
        assertNotNull(infra.getId());

        // 12. Traffic Signal
        TrafficSignal signal = trafficSignalRepository.saveAndFlush(new TrafficSignal("SIG-" + suffix, "Madhya Marg Intersection", "MAJOR", 30.7380, 76.7840));
        assertNotNull(signal.getId());

        // 13. Waste Bin
        WasteBin bin = wasteBinRepository.saveAndFlush(new WasteBin("BIN-" + suffix, "Sector 17 Plaza East", 30.7385, 76.7845, 1100));
        assertNotNull(bin.getId());

        // 14. Emergency Resource
        EmergencyResource er = emergencyResourceRepository.saveAndFlush(new EmergencyResource("General Hospital " + suffix, "HOSPITAL", 30.7410, 76.7820, "Sector 16", "0172-2752000", true, 500, "24/7"));
        assertNotNull(er.getId());

        // 15. Notification
        Notification notif = notificationRepository.save(new Notification(user, "New Incident Assignment", "Assigned to incident INC-TEST-001", "INCIDENT_ASSIGNMENT", "IN_APP"));
        assertNotNull(notif.getId());

        // 16. Audit Log
        AuditLog audit = auditLogRepository.save(new AuditLog("INCIDENT", savedIncident.getId().toString(), "CREATE", user.getUsername(), "Created incident", "127.0.0.1"));
        assertNotNull(audit.getId());
    }
}
