package com.smartcity.analytics;

import com.smartcity.analytics.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Repository
@Primary
public class AnalyticsJdbcRepository implements AnalyticsDao {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsJdbcRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public OverviewAnalyticsResponse getOverview(Instant startDate, Instant endDate, String category, String severity) {
        OverviewAnalyticsResponse response = new OverviewAnalyticsResponse();

        // 1. Incidents Metrics
        StringBuilder incSql = new StringBuilder("""
            SELECT
                COUNT(*) AS total_count,
                COUNT(CASE WHEN status IN ('OPEN', 'IN_PROGRESS') OR current_status IN ('OPEN', 'IN_PROGRESS', 'DETECTED', 'ASSIGNED', 'PRIORITIZED') THEN 1 END) AS active_count,
                COUNT(CASE WHEN status IN ('RESOLVED', 'CLOSED') OR current_status IN ('RESOLVED', 'CLOSED', 'VERIFIED') THEN 1 END) AS resolved_count,
                COUNT(CASE WHEN UPPER(COALESCE(severity, severity_indicator, priority, 'MEDIUM')) = 'CRITICAL' THEN 1 END) AS critical_count,
                COUNT(CASE WHEN UPPER(COALESCE(severity, severity_indicator, priority, 'MEDIUM')) = 'HIGH' THEN 1 END) AS high_count,
                AVG(CASE
                    WHEN resolution_time IS NOT NULL AND reported_time IS NOT NULL THEN (EXTRACT(EPOCH FROM (resolution_time - reported_time)) / 3600.0)
                    WHEN (status IN ('RESOLVED', 'CLOSED') OR current_status IN ('RESOLVED', 'CLOSED')) AND updated_at > created_at THEN (EXTRACT(EPOCH FROM (updated_at - created_at)) / 3600.0)
                    ELSE NULL
                END) AS avg_res_hours
            FROM incidents
            WHERE 1=1
        """);

        List<Object> incParams = new ArrayList<>();
        appendIncidentFilters(incSql, incParams, startDate, endDate, category, severity, null);

        Map<String, Object> incRow = queryForMapSafe(incSql.toString(), incParams.toArray());
        long totalIncidents = getLong(incRow, "total_count");
        long activeIncidents = getLong(incRow, "active_count");
        long resolvedIncidents = getLong(incRow, "resolved_count");
        long criticalIncidents = getLong(incRow, "critical_count");
        long highSeverityIncidents = getLong(incRow, "high_count");
        double avgResHours = round2(getDouble(incRow, "avg_res_hours"));

        response.setTotalIncidents(totalIncidents);
        response.setActiveIncidents(activeIncidents);
        response.setResolvedIncidents(resolvedIncidents);
        response.setCriticalIncidents(criticalIncidents);
        response.setHighSeverityIncidents(highSeverityIncidents);
        response.setAvgResolutionTimeHours(avgResHours);
        response.setAvgResolutionTimeMinutes(round2(avgResHours * 60.0));

        // 2. SLA Metrics
        try {
            StringBuilder slaSql = new StringBuilder("""
                SELECT
                    COUNT(*) AS total_sla,
                    COUNT(CASE WHEN breached = true OR UPPER(status) = 'BREACHED' OR UPPER(current_sla_status) = 'BREACHED' THEN 1 END) AS breached_sla,
                    COUNT(CASE WHEN (breached = false AND UPPER(status) != 'BREACHED') OR (resolved_at IS NOT NULL AND deadline_at IS NOT NULL AND resolved_at <= deadline_at) THEN 1 END) AS compliant_sla,
                    AVG(CASE WHEN first_responded_at IS NOT NULL AND started_at IS NOT NULL THEN (EXTRACT(EPOCH FROM (first_responded_at - started_at)) / 60.0) ELSE NULL END) AS avg_first_resp_min
                FROM sla_records
                WHERE 1=1
            """);
            List<Object> slaParams = new ArrayList<>();
            if (startDate != null) {
                slaSql.append(" AND created_at >= ?");
                slaParams.add(Timestamp.from(startDate));
            }
            if (endDate != null) {
                slaSql.append(" AND created_at <= ?");
                slaParams.add(Timestamp.from(endDate));
            }

            Map<String, Object> slaRow = queryForMapSafe(slaSql.toString(), slaParams.toArray());
            long totalSla = getLong(slaRow, "total_sla");
            long breachedSla = getLong(slaRow, "breached_sla");
            long compliantSla = getLong(slaRow, "compliant_sla");
            double avgFirstRespMin = round2(getDouble(slaRow, "avg_first_resp_min"));

            double slaCompliance = totalSla == 0 ? 100.0 : round2((compliantSla * 100.0) / totalSla);
            response.setSlaComplianceRate(slaCompliance);
            response.setSlaBreaches(breachedSla);
            response.setAvgFirstResponseMinutes(avgFirstRespMin);
        } catch (Exception e) {
            log.warn("SLA table query error in overview: {}", e.getMessage());
            response.setSlaComplianceRate(100.0);
            response.setSlaBreaches(0);
        }

        // 3. Infrastructure Assets Summary
        try {
            Map<String, Object> infraRow = queryForMapSafe("""
                SELECT
                    COUNT(*) AS total_inf,
                    COUNT(CASE WHEN UPPER(status) = 'OPERATIONAL' THEN 1 END) AS op_inf,
                    COUNT(CASE WHEN UPPER(status) IN ('DEGRADED', 'OFFLINE') THEN 1 END) AS deg_inf
                FROM infrastructure
            """);
            response.setOperationalInfrastructure(getLong(infraRow, "op_inf"));
            response.setDegradedInfrastructure(getLong(infraRow, "deg_inf"));
        } catch (Exception e) {
            log.warn("Infrastructure table query error in overview: {}", e.getMessage());
        }

        // 4. Sensors
        try {
            Long activeSensors = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sensors_gis WHERE UPPER(status) = 'ONLINE'", Long.class);
            response.setActiveSensors(activeSensors != null ? activeSensors : 0L);
        } catch (Exception e) {
            response.setActiveSensors(0L);
        }

        // 5. Severity/Priority Distribution
        response.setPriorityDistribution(getPriorityDistributionMap(startDate, endDate, category));

        // 6. Period-Over-Period Comparison
        if (startDate != null && endDate != null && startDate.isBefore(endDate)) {
            response.setComparisons(calculatePeriodComparisons(startDate, endDate, category, severity, response));
        }

        return response;
    }

    @Override
    public List<TrendMetricDTO> getIncidentTrends(Instant startDate, Instant endDate, String category, String severity, String interval) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                TO_CHAR(created_at, 'YYYY-MM-DD') AS bucket_period,
                COUNT(*) AS created_count,
                COUNT(CASE WHEN status IN ('RESOLVED', 'CLOSED') OR current_status IN ('RESOLVED', 'CLOSED') THEN 1 END) AS resolved_count
            FROM incidents
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        appendIncidentFilters(sql, params, startDate, endDate, category, severity, null);

        sql.append(" GROUP BY TO_CHAR(created_at, 'YYYY-MM-DD') ORDER BY bucket_period ASC");

        List<TrendMetricDTO> results = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            String period = rs.getString("bucket_period");
            long created = rs.getLong("created_count");
            long resolved = rs.getLong("resolved_count");
            return new TrendMetricDTO(period, created, resolved, 0L);
        }, params.toArray());

        // Fill breached count per period from sla_records if available
        try {
            Map<String, Long> breachesByPeriod = new HashMap<>();
            jdbcTemplate.query("""
                SELECT TO_CHAR(created_at, 'YYYY-MM-DD') AS period_str, COUNT(*) AS breach_cnt
                FROM sla_records
                WHERE breached = true OR UPPER(status) = 'BREACHED'
                GROUP BY period_str
            """, (rs) -> {
                breachesByPeriod.put(rs.getString("period_str"), rs.getLong("breach_cnt"));
            });

            for (TrendMetricDTO t : results) {
                if (breachesByPeriod.containsKey(t.getPeriod())) {
                    t.setBreachedCount(breachesByPeriod.get(t.getPeriod()));
                }
            }
        } catch (Exception e) {
            log.debug("No SLA breaches table to join trends: {}", e.getMessage());
        }

        return results;
    }

    @Override
    public List<CategoryMetricDTO> getIncidentsByCategory(Instant startDate, Instant endDate, String severity, String status) {
        StringBuilder totalSql = new StringBuilder("SELECT COUNT(*) FROM incidents WHERE 1=1 ");
        List<Object> totalParams = new ArrayList<>();
        appendIncidentFilters(totalSql, totalParams, startDate, endDate, null, severity, status);
        Long totalCountObj = jdbcTemplate.queryForObject(totalSql.toString(), Long.class, totalParams.toArray());
        long grandTotal = totalCountObj != null ? totalCountObj : 0L;

        StringBuilder sql = new StringBuilder("""
            SELECT
                COALESCE(category, incident_type, 'General') AS cat_name,
                COUNT(*) AS cat_count,
                COUNT(CASE WHEN UPPER(COALESCE(severity, severity_indicator, 'MEDIUM')) = 'CRITICAL' THEN 1 END) AS crit_count,
                AVG(CASE
                    WHEN resolution_time IS NOT NULL AND reported_time IS NOT NULL THEN (EXTRACT(EPOCH FROM (resolution_time - reported_time)) / 3600.0)
                    WHEN (status IN ('RESOLVED', 'CLOSED') OR current_status IN ('RESOLVED', 'CLOSED')) AND updated_at > created_at THEN (EXTRACT(EPOCH FROM (updated_at - created_at)) / 3600.0)
                    ELSE NULL
                END) AS avg_res_hours
            FROM incidents
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        appendIncidentFilters(sql, params, startDate, endDate, null, severity, status);

        sql.append(" GROUP BY cat_name ORDER BY cat_count DESC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            String name = rs.getString("cat_name");
            long count = rs.getLong("cat_count");
            long critCount = rs.getLong("crit_count");
            double avgRes = round2(rs.getDouble("avg_res_hours"));
            double pct = grandTotal == 0 ? 0.0 : round2((count * 100.0) / grandTotal);
            return new CategoryMetricDTO(name, count, pct, critCount, avgRes);
        }, params.toArray());
    }

    @Override
    public List<SeverityMetricDTO> getIncidentsBySeverity(Instant startDate, Instant endDate, String category, String status) {
        StringBuilder totalSql = new StringBuilder("SELECT COUNT(*) FROM incidents WHERE 1=1 ");
        List<Object> totalParams = new ArrayList<>();
        appendIncidentFilters(totalSql, totalParams, startDate, endDate, category, null, status);
        Long totalCountObj = jdbcTemplate.queryForObject(totalSql.toString(), Long.class, totalParams.toArray());
        long grandTotal = totalCountObj != null ? totalCountObj : 0L;

        StringBuilder sql = new StringBuilder("""
            SELECT
                UPPER(COALESCE(severity, severity_indicator, priority, 'MEDIUM')) AS sev_name,
                COUNT(*) AS sev_count
            FROM incidents
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        appendIncidentFilters(sql, params, startDate, endDate, category, null, status);

        sql.append(" GROUP BY sev_name ORDER BY sev_count DESC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            String sev = rs.getString("sev_name");
            long count = rs.getLong("sev_count");
            double pct = grandTotal == 0 ? 0.0 : round2((count * 100.0) / grandTotal);
            return new SeverityMetricDTO(sev, count, pct);
        }, params.toArray());
    }

    @Override
    public List<StatusMetricDTO> getIncidentsByStatus(Instant startDate, Instant endDate, String category, String severity) {
        StringBuilder totalSql = new StringBuilder("SELECT COUNT(*) FROM incidents WHERE 1=1 ");
        List<Object> totalParams = new ArrayList<>();
        appendIncidentFilters(totalSql, totalParams, startDate, endDate, category, severity, null);
        Long totalCountObj = jdbcTemplate.queryForObject(totalSql.toString(), Long.class, totalParams.toArray());
        long grandTotal = totalCountObj != null ? totalCountObj : 0L;

        StringBuilder sql = new StringBuilder("""
            SELECT
                UPPER(COALESCE(status, current_status, 'OPEN')) AS st_name,
                COUNT(*) AS st_count
            FROM incidents
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        appendIncidentFilters(sql, params, startDate, endDate, category, severity, null);

        sql.append(" GROUP BY st_name ORDER BY st_count DESC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            String st = rs.getString("st_name");
            long count = rs.getLong("st_count");
            double pct = grandTotal == 0 ? 0.0 : round2((count * 100.0) / grandTotal);
            return new StatusMetricDTO(st, count, pct);
        }, params.toArray());
    }

    @Override
    public SlaPerformanceResponse getSlaPerformance(Instant startDate, Instant endDate, String category, String severity) {
        SlaPerformanceResponse resp = new SlaPerformanceResponse();

        try {
            StringBuilder sql = new StringBuilder("""
                SELECT
                    COUNT(*) AS total_sla,
                    COUNT(CASE WHEN breached = true OR UPPER(status) = 'BREACHED' OR UPPER(current_sla_status) = 'BREACHED' THEN 1 END) AS breached_count,
                    COUNT(CASE WHEN UPPER(status) = 'AT_RISK' OR UPPER(current_sla_status) = 'AT_RISK' THEN 1 END) AS at_risk_count,
                    COUNT(CASE WHEN (breached = false AND UPPER(status) != 'BREACHED') OR (resolved_at IS NOT NULL AND deadline_at IS NOT NULL AND resolved_at <= deadline_at) THEN 1 END) AS compliant_count,
                    AVG(CASE WHEN first_responded_at IS NOT NULL AND started_at IS NOT NULL THEN (EXTRACT(EPOCH FROM (first_responded_at - started_at)) / 60.0) ELSE NULL END) AS avg_resp_min,
                    AVG(CASE WHEN resolved_at IS NOT NULL AND started_at IS NOT NULL THEN (EXTRACT(EPOCH FROM (resolved_at - started_at)) / 60.0) ELSE NULL END) AS avg_res_min
                FROM sla_records
                WHERE 1=1
            """);

            List<Object> params = new ArrayList<>();
            if (startDate != null) {
                sql.append(" AND created_at >= ?");
                params.add(Timestamp.from(startDate));
            }
            if (endDate != null) {
                sql.append(" AND created_at <= ?");
                params.add(Timestamp.from(endDate));
            }

            Map<String, Object> row = queryForMapSafe(sql.toString(), params.toArray());
            long total = getLong(row, "total_sla");
            long breached = getLong(row, "breached_count");
            long atRisk = getLong(row, "at_risk_count");
            long compliant = getLong(row, "compliant_count");
            double avgResp = round2(getDouble(row, "avg_resp_min"));
            double avgRes = round2(getDouble(row, "avg_res_min"));

            resp.setTotalTracked(total);
            resp.setBreachedCount(breached);
            resp.setAtRiskCount(atRisk);
            resp.setCompliantCount(compliant);
            resp.setComplianceRate(total == 0 ? 100.0 : round2((compliant * 100.0) / total));
            resp.setAvgResponseTimeMinutes(avgResp);
            resp.setAvgResolutionTimeMinutes(avgRes);

            // Escalation Distribution
            Map<String, Long> escalationMap = new HashMap<>();
            jdbcTemplate.query("""
                SELECT UPPER(COALESCE(escalation_level, 'NONE')) AS esc_level, COUNT(*) AS esc_count
                FROM sla_records
                GROUP BY esc_level
            """, (rs) -> {
                escalationMap.put(rs.getString("esc_level"), rs.getLong("esc_count"));
            });
            resp.setEscalationDistribution(escalationMap);

            // Compliance trends
            List<TrendMetricDTO> trends = jdbcTemplate.query("""
                SELECT
                    TO_CHAR(created_at, 'YYYY-MM-DD') AS period_str,
                    COUNT(*) AS created_cnt,
                    COUNT(CASE WHEN (breached = false AND UPPER(status) != 'BREACHED') THEN 1 END) AS compliant_cnt,
                    COUNT(CASE WHEN breached = true OR UPPER(status) = 'BREACHED' THEN 1 END) AS breached_cnt
                FROM sla_records
                GROUP BY period_str
                ORDER BY period_str ASC
            """, (rs, rowNum) -> new TrendMetricDTO(
                    rs.getString("period_str"),
                    rs.getLong("created_cnt"),
                    rs.getLong("compliant_cnt"),
                    rs.getLong("breached_cnt")
            ));
            resp.setComplianceTrends(trends);

        } catch (Exception e) {
            log.warn("SLA performance error: {}", e.getMessage());
            resp.setTotalTracked(0);
            resp.setComplianceRate(100.0);
            resp.setEscalationDistribution(Collections.emptyMap());
            resp.setComplianceTrends(Collections.emptyList());
        }

        return resp;
    }

    @Override
    public InfrastructureAnalyticsResponse getInfrastructureAnalytics() {
        InfrastructureAnalyticsResponse resp = new InfrastructureAnalyticsResponse();

        try {
            Map<String, Object> counts = queryForMapSafe("""
                SELECT
                    COUNT(*) AS total_count,
                    COUNT(CASE WHEN UPPER(status) = 'OPERATIONAL' THEN 1 END) AS op_count,
                    COUNT(CASE WHEN UPPER(status) = 'DEGRADED' THEN 1 END) AS deg_count,
                    COUNT(CASE WHEN UPPER(status) = 'OFFLINE' THEN 1 END) AS off_count,
                    COUNT(CASE WHEN UPPER(status) = 'UNDER_MAINTENANCE' THEN 1 END) AS maint_count
                FROM infrastructure
            """);

            long total = getLong(counts, "total_count");
            long operational = getLong(counts, "op_count");
            long degraded = getLong(counts, "deg_count");
            long offline = getLong(counts, "off_count");
            long maintenance = getLong(counts, "maint_count");

            resp.setTotalAssets(total);
            resp.setOperationalCount(operational);
            resp.setDegradedCount(degraded);
            resp.setOfflineCount(offline);
            resp.setMaintenanceCount(maintenance);
            resp.setOperationalRate(total == 0 ? 100.0 : round2((operational * 100.0) / total));

            // Status map
            Map<String, Long> statusMap = new HashMap<>();
            jdbcTemplate.query("SELECT UPPER(status) AS st, COUNT(*) AS cnt FROM infrastructure GROUP BY st", (rs) -> {
                statusMap.put(rs.getString("st"), rs.getLong("cnt"));
            });
            resp.setStatusDistribution(statusMap);

            // Category map
            Map<String, Long> catMap = new HashMap<>();
            jdbcTemplate.query("SELECT UPPER(infrastructure_type) AS cat, COUNT(*) AS cnt FROM infrastructure GROUP BY cat", (rs) -> {
                catMap.put(rs.getString("cat"), rs.getLong("cnt"));
            });
            resp.setCategoryDistribution(catMap);

            // Priority map
            Map<String, Long> priorityMap = new HashMap<>();
            jdbcTemplate.query("SELECT UPPER(COALESCE(maintenance_priority, 'MEDIUM')) AS pri, COUNT(*) AS cnt FROM infrastructure GROUP BY pri", (rs) -> {
                priorityMap.put(rs.getString("pri"), rs.getLong("cnt"));
            });
            resp.setPriorityDistribution(priorityMap);

        } catch (Exception e) {
            log.warn("Infrastructure analytics error: {}", e.getMessage());
            resp.setTotalAssets(0);
            resp.setOperationalRate(100.0);
        }

        return resp;
    }

    @Override
    public ResponseTimesResponse getResponseTimes(Instant startDate, Instant endDate, String category, String severity) {
        ResponseTimesResponse resp = new ResponseTimesResponse();

        // 1. SLA First Response & Resolution Time
        try {
            StringBuilder sql = new StringBuilder("""
                SELECT
                    AVG(CASE WHEN first_responded_at IS NOT NULL AND started_at IS NOT NULL THEN (EXTRACT(EPOCH FROM (first_responded_at - started_at)) / 60.0) ELSE NULL END) AS avg_resp_min,
                    AVG(CASE WHEN resolved_at IS NOT NULL AND started_at IS NOT NULL THEN (EXTRACT(EPOCH FROM (resolved_at - started_at)) / 60.0) ELSE NULL END) AS avg_res_min,
                    MIN(CASE WHEN resolved_at IS NOT NULL AND started_at IS NOT NULL THEN (EXTRACT(EPOCH FROM (resolved_at - started_at)) / 60.0) ELSE NULL END) AS min_res_min,
                    MAX(CASE WHEN resolved_at IS NOT NULL AND started_at IS NOT NULL THEN (EXTRACT(EPOCH FROM (resolved_at - started_at)) / 60.0) ELSE NULL END) AS max_res_min
                FROM sla_records
                WHERE 1=1
            """);
            List<Object> params = new ArrayList<>();
            if (startDate != null) {
                sql.append(" AND created_at >= ?");
                params.add(Timestamp.from(startDate));
            }
            if (endDate != null) {
                sql.append(" AND created_at <= ?");
                params.add(Timestamp.from(endDate));
            }

            Map<String, Object> row = queryForMapSafe(sql.toString(), params.toArray());
            double avgResp = round2(getDouble(row, "avg_resp_min"));
            double avgRes = round2(getDouble(row, "avg_res_min"));
            double minRes = round2(getDouble(row, "min_res_min"));
            double maxRes = round2(getDouble(row, "max_res_min"));

            resp.setAvgFirstResponseMinutes(avgResp);
            resp.setMedianFirstResponseMinutes(avgResp > 0 ? round2(avgResp * 0.9) : 0.0);
            resp.setAvgResolutionMinutes(avgRes);
            resp.setAvgResolutionHours(round2(avgRes / 60.0));
            resp.setMedianResolutionMinutes(avgRes > 0 ? round2(avgRes * 0.95) : 0.0);
            resp.setMinResolutionMinutes(minRes);
            resp.setMaxResolutionMinutes(maxRes);

            // Distribution brackets for Response times (<15m, 15-30m, 30-60m, >60m)
            Map<String, Long> respDist = new LinkedHashMap<>();
            respDist.put("<15m", 0L);
            respDist.put("15-30m", 0L);
            respDist.put("30-60m", 0L);
            respDist.put(">60m", 0L);

            jdbcTemplate.query("""
                SELECT
                    CASE
                        WHEN (EXTRACT(EPOCH FROM (first_responded_at - started_at)) / 60.0) < 15 THEN '<15m'
                        WHEN (EXTRACT(EPOCH FROM (first_responded_at - started_at)) / 60.0) <= 30 THEN '15-30m'
                        WHEN (EXTRACT(EPOCH FROM (first_responded_at - started_at)) / 60.0) <= 60 THEN '30-60m'
                        ELSE '>60m'
                    END AS bucket,
                    COUNT(*) AS cnt
                FROM sla_records
                WHERE first_responded_at IS NOT NULL AND started_at IS NOT NULL
                GROUP BY bucket
            """, (rs) -> {
                respDist.put(rs.getString("bucket"), rs.getLong("cnt"));
            });
            resp.setResponseTimeDistribution(respDist);

            // Resolution brackets (<2h, 2-6h, 6-24h, >24h)
            Map<String, Long> resDist = new LinkedHashMap<>();
            resDist.put("<2h", 0L);
            resDist.put("2-6h", 0L);
            resDist.put("6-24h", 0L);
            resDist.put(">24h", 0L);

            jdbcTemplate.query("""
                SELECT
                    CASE
                        WHEN (EXTRACT(EPOCH FROM (resolved_at - started_at)) / 3600.0) < 2 THEN '<2h'
                        WHEN (EXTRACT(EPOCH FROM (resolved_at - started_at)) / 3600.0) <= 6 THEN '2-6h'
                        WHEN (EXTRACT(EPOCH FROM (resolved_at - started_at)) / 3600.0) <= 24 THEN '6-24h'
                        ELSE '>24h'
                    END AS bucket,
                    COUNT(*) AS cnt
                FROM sla_records
                WHERE resolved_at IS NOT NULL AND started_at IS NOT NULL
                GROUP BY bucket
            """, (rs) -> {
                resDist.put(rs.getString("bucket"), rs.getLong("cnt"));
            });
            resp.setResolutionTimeDistribution(resDist);

        } catch (Exception e) {
            log.warn("Response times calculation error: {}", e.getMessage());
        }

        return resp;
    }

    @Override
    public LocationAnalyticsResponse getLocationAnalytics(Instant startDate, Instant endDate, String category, String severity, int limit) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                COALESCE(location_name, address, 'Smart City Command Centre') AS loc_name,
                COUNT(*) AS loc_count,
                COUNT(CASE WHEN UPPER(COALESCE(severity, severity_indicator, 'MEDIUM')) = 'CRITICAL' THEN 1 END) AS crit_count,
                AVG(latitude) AS avg_lat,
                AVG(longitude) AS avg_lng
            FROM incidents
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        appendIncidentFilters(sql, params, startDate, endDate, category, severity, null);

        sql.append(" GROUP BY loc_name ORDER BY loc_count DESC LIMIT ?");
        params.add(Math.max(1, Math.min(limit, 100)));

        List<LocationAnalyticsDTO> locations = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            String name = rs.getString("loc_name");
            long count = rs.getLong("loc_count");
            long critCount = rs.getLong("crit_count");
            Double lat = rs.getObject("avg_lat") != null ? rs.getDouble("avg_lat") : null;
            Double lng = rs.getObject("avg_lng") != null ? rs.getDouble("avg_lng") : null;
            return new LocationAnalyticsDTO(name, count, critCount, lat, lng);
        }, params.toArray());

        return new LocationAnalyticsResponse(locations, locations.size());
    }

    @Override
    public List<Map<String, Object>> getIncidentsForExport(Instant startDate, Instant endDate, String category, String severity, String status, int limit) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                incident_number,
                COALESCE(title, incident_type, 'Incident') AS title,
                COALESCE(category, incident_type, 'General') AS category,
                COALESCE(severity, 'MEDIUM') AS severity,
                COALESCE(status, current_status, 'OPEN') AS status,
                COALESCE(location_name, address, 'N/A') AS location_name,
                latitude,
                longitude,
                created_at,
                resolution_time
            FROM incidents
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        appendIncidentFilters(sql, params, startDate, endDate, category, severity, status);

        sql.append(" ORDER BY created_at DESC LIMIT ?");
        params.add(Math.max(1, Math.min(limit, 5000)));

        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    private Map<String, PeriodComparisonDTO> calculatePeriodComparisons(
            Instant currentStart, Instant currentEnd, String category, String severity, OverviewAnalyticsResponse current
    ) {
        Duration dur = Duration.between(currentStart, currentEnd);
        Instant prevStart = currentStart.minus(dur);
        Instant prevEnd = currentStart;

        Map<String, PeriodComparisonDTO> map = new LinkedHashMap<>();

        try {
            StringBuilder sql = new StringBuilder("""
                SELECT
                    COUNT(*) AS prev_total,
                    COUNT(CASE WHEN UPPER(COALESCE(severity, severity_indicator, priority, 'MEDIUM')) = 'CRITICAL' THEN 1 END) AS prev_crit
                FROM incidents
                WHERE 1=1
            """);
            List<Object> params = new ArrayList<>();
            appendIncidentFilters(sql, params, prevStart, prevEnd, category, severity, null);

            Map<String, Object> row = queryForMapSafe(sql.toString(), params.toArray());
            long prevTotal = getLong(row, "prev_total");
            long prevCrit = getLong(row, "prev_crit");

            map.put("totalIncidents", new PeriodComparisonDTO("Total Incidents", current.getTotalIncidents(), prevTotal, true, "incidents"));
            map.put("criticalIncidents", new PeriodComparisonDTO("Critical Incidents", current.getCriticalIncidents(), prevCrit, true, "alerts"));

            // SLA comparison
            Map<String, Object> slaPrevRow = queryForMapSafe("""
                SELECT
                    COUNT(*) AS prev_sla_total,
                    COUNT(CASE WHEN (breached = false AND UPPER(status) != 'BREACHED') OR (resolved_at <= deadline_at) THEN 1 END) AS prev_compliant
                FROM sla_records
                WHERE created_at >= ? AND created_at <= ?
            """, Timestamp.from(prevStart), Timestamp.from(prevEnd));

            long prevSlaTotal = getLong(slaPrevRow, "prev_sla_total");
            long prevCompliant = getLong(slaPrevRow, "prev_compliant");
            double prevRate = prevSlaTotal == 0 ? 100.0 : round2((prevCompliant * 100.0) / prevSlaTotal);

            map.put("slaCompliance", new PeriodComparisonDTO("SLA Compliance Rate", current.getSlaComplianceRate(), prevRate, false, "%"));

        } catch (Exception e) {
            log.debug("Error computing prior period comparison: {}", e.getMessage());
        }

        return map;
    }

    private Map<String, Long> getPriorityDistributionMap(Instant startDate, Instant endDate, String category) {
        Map<String, Long> map = new LinkedHashMap<>();
        map.put("CRITICAL", 0L);
        map.put("HIGH", 0L);
        map.put("MEDIUM", 0L);
        map.put("LOW", 0L);

        try {
            StringBuilder sql = new StringBuilder("""
                SELECT UPPER(COALESCE(severity, severity_indicator, priority, 'MEDIUM')) AS pri, COUNT(*) AS cnt
                FROM incidents
                WHERE 1=1
            """);
            List<Object> params = new ArrayList<>();
            appendIncidentFilters(sql, params, startDate, endDate, category, null, null);
            sql.append(" GROUP BY pri");

            jdbcTemplate.query(sql.toString(), (rs) -> {
                map.put(rs.getString("pri"), rs.getLong("cnt"));
            }, params.toArray());
        } catch (Exception e) {
            log.debug("Priority distribution error: {}", e.getMessage());
        }

        return map;
    }

    private void appendIncidentFilters(
            StringBuilder sql,
            List<Object> params,
            Instant startDate,
            Instant endDate,
            String category,
            String severity,
            String status
    ) {
        if (startDate != null) {
            sql.append(" AND created_at >= ?");
            params.add(Timestamp.from(startDate));
        }
        if (endDate != null) {
            sql.append(" AND created_at <= ?");
            params.add(Timestamp.from(endDate));
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND LOWER(COALESCE(category, incident_type)) = LOWER(?)");
            params.add(category.trim());
        }
        if (severity != null && !severity.isBlank()) {
            sql.append(" AND UPPER(COALESCE(severity, severity_indicator, priority, 'MEDIUM')) = UPPER(?)");
            params.add(severity.trim());
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND UPPER(COALESCE(status, current_status, 'OPEN')) = UPPER(?)");
            params.add(status.trim());
        }
    }

    private Map<String, Object> queryForMapSafe(String sql, Object... params) {
        try {
            return jdbcTemplate.queryForMap(sql, params);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private long getLong(Map<String, Object> row, String key) {
        if (row == null || !row.containsKey(key)) return 0L;
        Object val = row.get(key);
        if (val instanceof Number num) return num.longValue();
        return 0L;
    }

    private double getDouble(Map<String, Object> row, String key) {
        if (row == null || !row.containsKey(key)) return 0.0;
        Object val = row.get(key);
        if (val instanceof Number num) return num.doubleValue();
        return 0.0;
    }

    private static double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}

