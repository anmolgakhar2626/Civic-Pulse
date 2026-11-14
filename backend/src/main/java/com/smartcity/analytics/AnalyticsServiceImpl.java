package com.smartcity.analytics;

import com.smartcity.analytics.dto.*;
import com.smartcity.department.Department;
import com.smartcity.department.DepartmentRepository;
import com.smartcity.incident.Incident;
import com.smartcity.incident.IncidentRepository;
import com.smartcity.incident.IncidentStatus;
import com.smartcity.iot.IotEvent;
import com.smartcity.iot.IotEventRepository;
import com.smartcity.iot.SensorDevice;
import com.smartcity.iot.SensorDeviceRepository;
import com.smartcity.iot.SensorReadingRepository;
import com.smartcity.sla.SLARecord;
import com.smartcity.sla.SLARepository;
import com.smartcity.sla.SLAStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsServiceImpl.class);

    private final AnalyticsDao analyticsDao;
    private final IncidentRepository incidentRepository;
    private final SLARepository slaRepository;
    private final DepartmentRepository departmentRepository;
    private final SensorDeviceRepository sensorDeviceRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final IotEventRepository iotEventRepository;

    @Autowired
    public AnalyticsServiceImpl(
            @Autowired(required = false) AnalyticsDao analyticsDao,
            IncidentRepository incidentRepository,
            SLARepository slaRepository,
            DepartmentRepository departmentRepository,
            @Autowired(required = false) SensorDeviceRepository sensorDeviceRepository,
            @Autowired(required = false) SensorReadingRepository sensorReadingRepository,
            @Autowired(required = false) IotEventRepository iotEventRepository
    ) {
        this.analyticsDao = analyticsDao;
        this.incidentRepository = incidentRepository;
        this.slaRepository = slaRepository;
        this.departmentRepository = departmentRepository;
        this.sensorDeviceRepository = sensorDeviceRepository;
        this.sensorReadingRepository = sensorReadingRepository;
        this.iotEventRepository = iotEventRepository;
    }

    public AnalyticsServiceImpl(
            IncidentRepository incidentRepository,
            SLARepository slaRepository,
            DepartmentRepository departmentRepository,
            SensorDeviceRepository sensorDeviceRepository,
            SensorReadingRepository sensorReadingRepository,
            IotEventRepository iotEventRepository
    ) {
        this(null, incidentRepository, slaRepository, departmentRepository, sensorDeviceRepository, sensorReadingRepository, iotEventRepository);
    }

    @Override
    public OverviewAnalyticsResponse getOverview(Instant startDate, Instant endDate, String category, String severity) {
        if (analyticsDao != null) {
            OverviewAnalyticsResponse response = analyticsDao.getOverview(startDate, endDate, category, severity);
            // Enrich with department workload if available
            try {
                List<Incident> incidents = filterIncidents(startDate, endDate);
                List<SLARecord> slas = filterSlas(startDate, endDate);
                response.setDepartmentWorkload(buildDepartmentWorkload(incidents, slas));
            } catch (Exception e) {
                log.debug("Department workload query skipped in overview: {}", e.getMessage());
            }
            return response;
        }
        return getOverviewAnalytics(startDate, endDate);
    }

    @Override
    public List<TrendMetricDTO> getIncidentTrends(Instant startDate, Instant endDate, String category, String severity, String interval) {
        if (analyticsDao != null) {
            return analyticsDao.getIncidentTrends(startDate, endDate, category, severity, interval);
        }
        return Collections.emptyList();
    }

    @Override
    public List<CategoryMetricDTO> getIncidentsByCategory(Instant startDate, Instant endDate, String severity, String status) {
        if (analyticsDao != null) {
            return analyticsDao.getIncidentsByCategory(startDate, endDate, severity, status);
        }
        return Collections.emptyList();
    }

    @Override
    public List<SeverityMetricDTO> getIncidentsBySeverity(Instant startDate, Instant endDate, String category, String status) {
        if (analyticsDao != null) {
            return analyticsDao.getIncidentsBySeverity(startDate, endDate, category, status);
        }
        return Collections.emptyList();
    }

    @Override
    public List<StatusMetricDTO> getIncidentsByStatus(Instant startDate, Instant endDate, String category, String severity) {
        if (analyticsDao != null) {
            return analyticsDao.getIncidentsByStatus(startDate, endDate, category, severity);
        }
        return Collections.emptyList();
    }

    @Override
    public SlaPerformanceResponse getSlaPerformance(Instant startDate, Instant endDate, String category, String severity) {
        if (analyticsDao != null) {
            return analyticsDao.getSlaPerformance(startDate, endDate, category, severity);
        }
        return new SlaPerformanceResponse();
    }

    @Override
    public InfrastructureAnalyticsResponse getInfrastructureAnalytics() {
        if (analyticsDao != null) {
            return analyticsDao.getInfrastructureAnalytics();
        }
        return new InfrastructureAnalyticsResponse();
    }

    @Override
    public ResponseTimesResponse getResponseTimes(Instant startDate, Instant endDate, String category, String severity) {
        if (analyticsDao != null) {
            return analyticsDao.getResponseTimes(startDate, endDate, category, severity);
        }
        return new ResponseTimesResponse();
    }

    @Override
    public LocationAnalyticsResponse getLocationAnalytics(Instant startDate, Instant endDate, String category, String severity, int limit) {
        if (analyticsDao != null) {
            return analyticsDao.getLocationAnalytics(startDate, endDate, category, severity, limit);
        }
        return new LocationAnalyticsResponse(Collections.emptyList(), 0);
    }

    @Override
    public byte[] exportIncidentsCsv(Instant startDate, Instant endDate, String category, String severity, String status, int limit) {
        List<Map<String, Object>> rows = analyticsDao != null
                ? analyticsDao.getIncidentsForExport(startDate, endDate, category, severity, status, limit)
                : Collections.emptyList();

        StringBuilder csv = new StringBuilder();
        // UTF-8 BOM for spreadsheet encoding recognition
        csv.append("\uFEFF");
        // Header
        csv.append("Incident Number,Title,Category,Severity,Status,Location Name,Latitude,Longitude,Created At,Resolution Time\n");

        for (Map<String, Object> row : rows) {
            csv.append(sanitizeCsvField(row.get("incident_number"))).append(",");
            csv.append(sanitizeCsvField(row.get("title"))).append(",");
            csv.append(sanitizeCsvField(row.get("category"))).append(",");
            csv.append(sanitizeCsvField(row.get("severity"))).append(",");
            csv.append(sanitizeCsvField(row.get("status"))).append(",");
            csv.append(sanitizeCsvField(row.get("location_name"))).append(",");
            csv.append(sanitizeCsvField(row.get("latitude"))).append(",");
            csv.append(sanitizeCsvField(row.get("longitude"))).append(",");
            csv.append(sanitizeCsvField(row.get("created_at"))).append(",");
            csv.append(sanitizeCsvField(row.get("resolution_time"))).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Sanitizes values against CSV Formula Injection (CWE-1236)
     * Prepends a single quote if the value starts with =, +, -, @, \t, or \r.
     */
    public static String sanitizeCsvField(Object value) {
        if (value == null) {
            return "";
        }
        String str = value.toString();

        if (!str.isEmpty()) {
            char firstChar = str.charAt(0);
            if (firstChar == '=' || firstChar == '+' || firstChar == '-' || firstChar == '@' || firstChar == '\t' || firstChar == '\r') {
                str = "'" + str;
            }
        }

        boolean mustQuote = str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r");
        if (mustQuote) {
            str = "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }

    // ==========================================
    // Backward-compatible methods
    // ==========================================

    @Override
    public OverviewAnalyticsResponse getOverviewAnalytics(Instant startDate, Instant endDate) {
        List<Incident> incidents = filterIncidents(startDate, endDate);
        List<SLARecord> slas = filterSlas(startDate, endDate);

        OverviewAnalyticsResponse response = new OverviewAnalyticsResponse();
        response.setTotalIncidents(incidents.size());

        long active = incidents.stream()
                .filter(i -> i.getStatus() != IncidentStatus.RESOLVED && i.getStatus() != IncidentStatus.CLOSED)
                .count();
        long resolved = incidents.stream()
                .filter(i -> i.getStatus() == IncidentStatus.RESOLVED || i.getStatus() == IncidentStatus.CLOSED)
                .count();
        long critical = incidents.stream()
                .filter(i -> (i.getPriorityTier() != null && "CRITICAL".equalsIgnoreCase(i.getPriorityTier().name()))
                        || "CRITICAL".equalsIgnoreCase(i.getPriorityLabel()))
                .count();

        response.setActiveIncidents(active);
        response.setResolvedIncidents(resolved);
        response.setCriticalIncidents(critical);

        // Priority distribution
        Map<String, Long> prioDist = new LinkedHashMap<>();
        prioDist.put("CRITICAL", 0L);
        prioDist.put("HIGH", 0L);
        prioDist.put("MEDIUM", 0L);
        prioDist.put("LOW", 0L);
        for (Incident inc : incidents) {
            String tier = inc.getPriorityTier() != null ? inc.getPriorityTier().name()
                    : (inc.getPriorityLabel() != null ? inc.getPriorityLabel().toUpperCase() : "MEDIUM");
            prioDist.put(tier, prioDist.getOrDefault(tier, 0L) + 1);
        }
        response.setPriorityDistribution(prioDist);

        // SLA compliance
        long totalSlas = slas.size();
        long breachedSlas = slas.stream().filter(s -> s.getStatus() == SLAStatus.BREACHED).count();
        response.setSlaBreaches(breachedSlas);
        double compliance = totalSlas > 0 ? ((double) (totalSlas - breachedSlas) / totalSlas) * 100.0 : 100.0;
        response.setSlaComplianceRate(Math.round(compliance * 10.0) / 10.0);

        // Average resolution time
        double totalResHours = 0;
        long resCount = 0;
        for (Incident inc : incidents) {
            if (inc.getResolvedAt() != null && inc.getCreatedAt() != null) {
                long minutes = Duration.between(inc.getCreatedAt(), inc.getResolvedAt()).toMinutes();
                if (minutes >= 0) {
                    totalResHours += (minutes / 60.0);
                    resCount++;
                }
            }
        }
        response.setAvgResolutionTimeHours(resCount > 0 ? Math.round((totalResHours / resCount) * 10.0) / 10.0 : 0.0);

        // IoT metrics from MongoDB
        try {
            if (sensorDeviceRepository != null) {
                response.setActiveSensors(sensorDeviceRepository.count());
            }
            if (iotEventRepository != null) {
                response.setSensorAlertsCount(iotEventRepository.count());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch sensor metrics for overview: {}", e.getMessage());
        }

        // Department workload
        response.setDepartmentWorkload(buildDepartmentWorkload(incidents, slas));

        return response;
    }

    @Override
    public IncidentsAnalyticsResponse getIncidentsAnalytics(Instant startDate, Instant endDate) {
        List<Incident> incidents = filterIncidents(startDate, endDate);

        IncidentsAnalyticsResponse response = new IncidentsAnalyticsResponse();
        response.setTotalIncidents(incidents.size());

        long active = incidents.stream()
                .filter(i -> i.getStatus() != IncidentStatus.RESOLVED && i.getStatus() != IncidentStatus.CLOSED)
                .count();
        long resolved = incidents.stream()
                .filter(i -> i.getStatus() == IncidentStatus.RESOLVED || i.getStatus() == IncidentStatus.CLOSED)
                .count();

        response.setActiveIncidents(active);
        response.setResolvedIncidents(resolved);

        // Distributions
        Map<String, Long> prio = new LinkedHashMap<>();
        prio.put("CRITICAL", 0L);
        prio.put("HIGH", 0L);
        prio.put("MEDIUM", 0L);
        prio.put("LOW", 0L);

        Map<String, Long> statusDist = new LinkedHashMap<>();
        Map<String, Long> sourceDist = new LinkedHashMap<>();
        Map<String, Long> deptDist = new LinkedHashMap<>();

        double totalResHours = 0;
        long resCount = 0;

        for (Incident inc : incidents) {
            String p = inc.getPriorityTier() != null ? inc.getPriorityTier().name()
                    : (inc.getPriorityLabel() != null ? inc.getPriorityLabel().toUpperCase() : "MEDIUM");
            prio.put(p, prio.getOrDefault(p, 0L) + 1);

            String s = inc.getStatus() != null ? inc.getStatus().name() : "DETECTED";
            statusDist.put(s, statusDist.getOrDefault(s, 0L) + 1);

            String src = inc.getSource() != null ? inc.getSource().name() : "UNKNOWN";
            sourceDist.put(src, sourceDist.getOrDefault(src, 0L) + 1);

            String dept = inc.getAssignedDepartment() != null ? inc.getAssignedDepartment().getName() : "Unassigned";
            deptDist.put(dept, deptDist.getOrDefault(dept, 0L) + 1);

            if (inc.getResolvedAt() != null && inc.getCreatedAt() != null) {
                long minutes = Duration.between(inc.getCreatedAt(), inc.getResolvedAt()).toMinutes();
                if (minutes >= 0) {
                    totalResHours += (minutes / 60.0);
                    resCount++;
                }
            }
        }

        response.setPriorityDistribution(prio);
        response.setStatusDistribution(statusDist);
        response.setSourceDistribution(sourceDist);
        response.setDepartmentDistribution(deptDist);
        response.setAvgResolutionTimeHours(resCount > 0 ? Math.round((totalResHours / resCount) * 10.0) / 10.0 : 0.0);

        // Trends
        response.setIncidentTrends(calculateDailyTrends(incidents));

        return response;
    }

    @Override
    public SlaAnalyticsResponse getSlaAnalytics(Instant startDate, Instant endDate) {
        List<SLARecord> slas = filterSlas(startDate, endDate);

        SlaAnalyticsResponse response = new SlaAnalyticsResponse();
        response.setTotalMonitored(slas.size());

        long onTrack = slas.stream().filter(s -> s.getStatus() == SLAStatus.ON_TRACK).count();
        long atRisk = slas.stream().filter(s -> s.getStatus() == SLAStatus.AT_RISK).count();
        long breached = slas.stream().filter(s -> s.getStatus() == SLAStatus.BREACHED).count();
        long resolved = slas.stream().filter(s -> s.getStatus() == SLAStatus.RESOLVED).count();

        response.setOnTrack(onTrack);
        response.setAtRisk(atRisk);
        response.setBreached(breached);
        response.setResolved(resolved);

        double compliance = slas.size() > 0 ? ((double) (slas.size() - breached) / slas.size()) * 100.0 : 100.0;
        response.setComplianceRate(Math.round(compliance * 10.0) / 10.0);

        // Escalation distribution
        Map<String, Long> escDist = new LinkedHashMap<>();
        escDist.put("NONE", 0L);
        escDist.put("LEVEL_1", 0L);
        escDist.put("LEVEL_2", 0L);
        escDist.put("LEVEL_3", 0L);
        for (SLARecord sla : slas) {
            String esc = sla.getEscalationLevel() != null ? sla.getEscalationLevel().name() : "NONE";
            escDist.put(esc, escDist.getOrDefault(esc, 0L) + 1);
        }
        response.setEscalationDistribution(escDist);

        // Avg resolution minutes from SLAs
        double totalMinutes = 0;
        long count = 0;
        for (SLARecord sla : slas) {
            if (sla.getResolvedAt() != null && sla.getStartedAt() != null) {
                long mins = Duration.between(sla.getStartedAt(), sla.getResolvedAt()).toMinutes();
                if (mins >= 0) {
                    totalMinutes += mins;
                    count++;
                }
            }
        }
        response.setAvgResolutionMinutes(count > 0 ? Math.round((totalMinutes / count) * 10.0) / 10.0 : 0.0);

        return response;
    }

    @Override
    public DepartmentAnalyticsResponse getDepartmentAnalytics(Instant startDate, Instant endDate) {
        List<Incident> incidents = filterIncidents(startDate, endDate);
        List<SLARecord> slas = filterSlas(startDate, endDate);
        return new DepartmentAnalyticsResponse(buildDepartmentWorkload(incidents, slas));
    }

    @Override
    public SensorAnalyticsResponse getSensorAnalytics(Instant startDate, Instant endDate) {
        SensorAnalyticsResponse response = new SensorAnalyticsResponse();

        try {
            if (sensorDeviceRepository != null) {
                List<SensorDevice> devices = sensorDeviceRepository.findAll();
                response.setTotalSensors(devices.size());
                long active = devices.stream().filter(d -> d.getStatus() == com.smartcity.iot.SensorStatus.ONLINE).count();
                response.setActiveSensors(active);

                Map<String, Long> byType = devices.stream()
                        .filter(d -> d.getSensorType() != null)
                        .collect(Collectors.groupingBy(d -> d.getSensorType().name(), Collectors.counting()));
                response.setSensorsByType(byType);
            }

            if (sensorReadingRepository != null) {
                response.setTotalReadings(sensorReadingRepository.count());
            }

            if (iotEventRepository != null) {
                List<IotEvent> events = iotEventRepository.findAll();
                response.setTotalAlerts(events.size());

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"));
                Map<String, Long> countsByDate = new TreeMap<>();
                for (IotEvent e : events) {
                    if (e.getTimestamp() != null) {
                        String dateStr = formatter.format(e.getTimestamp());
                        countsByDate.put(dateStr, countsByDate.getOrDefault(dateStr, 0L) + 1);
                    }
                }
                List<TrendPoint> trendList = countsByDate.entrySet().stream()
                        .map(entry -> new TrendPoint(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList());
                response.setAlertTrends(trendList);
            }
        } catch (Exception e) {
            log.warn("Sensor analytics query error: {}", e.getMessage());
        }

        return response;
    }

    private List<Incident> filterIncidents(Instant startDate, Instant endDate) {
        if (startDate != null && endDate != null) {
            return incidentRepository.findAll((root, query, cb) -> cb.between(root.get("createdAt"), startDate, endDate));
        } else if (startDate != null) {
            return incidentRepository.findAll((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
        } else if (endDate != null) {
            return incidentRepository.findAll((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
        } else {
            return incidentRepository.findAll();
        }
    }

    private List<SLARecord> filterSlas(Instant startDate, Instant endDate) {
        List<SLARecord> all = slaRepository.findAll();
        if (startDate == null && endDate == null) {
            return all;
        }
        return all.stream()
                .filter(s -> {
                    Instant t = s.getCreatedAt();
                    if (t == null) return true;
                    if (startDate != null && t.isBefore(startDate)) return false;
                    return endDate == null || !t.isAfter(endDate);
                })
                .collect(Collectors.toList());
    }

    private List<DepartmentWorkloadDTO> buildDepartmentWorkload(List<Incident> incidents, List<SLARecord> slas) {
        List<Department> departments = departmentRepository.findAll();
        List<DepartmentWorkloadDTO> result = new ArrayList<>();

        for (Department dept : departments) {
            List<Incident> deptIncidents = incidents.stream()
                    .filter(i -> i.getAssignedDepartment() != null && dept.getId().equals(i.getAssignedDepartment().getId()))
                    .collect(Collectors.toList());

            long total = deptIncidents.size();
            long active = deptIncidents.stream()
                    .filter(i -> i.getStatus() != IncidentStatus.RESOLVED && i.getStatus() != IncidentStatus.CLOSED)
                    .count();
            long resolved = deptIncidents.stream()
                    .filter(i -> i.getStatus() == IncidentStatus.RESOLVED || i.getStatus() == IncidentStatus.CLOSED)
                    .count();

            double totalHours = 0;
            long resolvedWithTime = 0;
            for (Incident inc : deptIncidents) {
                if (inc.getResolvedAt() != null && inc.getCreatedAt() != null) {
                    long mins = Duration.between(inc.getCreatedAt(), inc.getResolvedAt()).toMinutes();
                    if (mins >= 0) {
                        totalHours += (mins / 60.0);
                        resolvedWithTime++;
                    }
                }
            }
            double avgHours = resolvedWithTime > 0 ? Math.round((totalHours / resolvedWithTime) * 10.0) / 10.0 : 0.0;

            List<SLARecord> deptSlas = slas.stream()
                    .filter(s -> s.getIncident() != null && s.getIncident().getAssignedDepartment() != null
                            && dept.getId().equals(s.getIncident().getAssignedDepartment().getId()))
                    .collect(Collectors.toList());
            long breached = deptSlas.stream().filter(s -> s.getStatus() == SLAStatus.BREACHED).count();
            double slaRate = deptSlas.size() > 0 ? ((double) (deptSlas.size() - breached) / deptSlas.size()) * 100.0 : 100.0;

            result.add(new DepartmentWorkloadDTO(
                    dept.getId(),
                    dept.getName(),
                    dept.getCode(),
                    total,
                    active,
                    resolved,
                    avgHours,
                    Math.round(slaRate * 10.0) / 10.0
            ));
        }

        return result;
    }

    private List<TrendPoint> calculateDailyTrends(List<Incident> incidents) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"));
        Map<String, Long> dailyCounts = new TreeMap<>();

        for (Incident inc : incidents) {
            if (inc.getCreatedAt() != null) {
                String d = formatter.format(inc.getCreatedAt());
                dailyCounts.put(d, dailyCounts.getOrDefault(d, 0L) + 1);
            }
        }

        return dailyCounts.entrySet().stream()
                .map(e -> new TrendPoint(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }
}
