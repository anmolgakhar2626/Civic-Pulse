package com.smartcity.analytics;

import com.smartcity.analytics.dto.*;
import com.smartcity.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Municipal database intelligence, trends, SLA compliance, response times, and CSV export")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get high-level command centre operational metrics",
            description = "Computes real-time totals, active counts, SLA compliance rate, comparisons, and workload from PostgreSQL.")
    public ResponseEntity<ApiResponse<OverviewAnalyticsResponse>> getOverview(
            @Parameter(description = "Start timestamp in ISO-8601 UTC format")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "End timestamp in ISO-8601 UTC format")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @Parameter(description = "Optional category filter")
            @RequestParam(required = false) String category,
            @Parameter(description = "Optional severity filter")
            @RequestParam(required = false) String severity
    ) {
        OverviewAnalyticsResponse response = analyticsService.getOverview(startDate, endDate, category, severity);
        return ResponseEntity.ok(ApiResponse.ok(response, "Overview analytics computed successfully"));
    }

    @GetMapping("/incidents/trends")
    @Operation(summary = "Get incident creation, resolution, and breach trends over time",
            description = "Aggregates incident velocity grouped by day/period.")
    public ResponseEntity<ApiResponse<List<TrendMetricDTO>>> getIncidentTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "day") String interval
    ) {
        List<TrendMetricDTO> response = analyticsService.getIncidentTrends(startDate, endDate, category, severity, interval);
        return ResponseEntity.ok(ApiResponse.ok(response, "Incident trends computed successfully"));
    }

    @GetMapping("/incidents/by-category")
    @Operation(summary = "Get incident breakdown by category",
            description = "Computes total volume, percentage of total, critical volume, and average resolution hours per category.")
    public ResponseEntity<ApiResponse<List<CategoryMetricDTO>>> getIncidentsByCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status
    ) {
        List<CategoryMetricDTO> response = analyticsService.getIncidentsByCategory(startDate, endDate, severity, status);
        return ResponseEntity.ok(ApiResponse.ok(response, "Category metrics computed successfully"));
    }

    @GetMapping("/incidents/by-severity")
    @Operation(summary = "Get incident breakdown by severity level",
            description = "Computes distribution across CRITICAL, HIGH, MEDIUM, and LOW severity tiers.")
    public ResponseEntity<ApiResponse<List<SeverityMetricDTO>>> getIncidentsBySeverity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status
    ) {
        List<SeverityMetricDTO> response = analyticsService.getIncidentsBySeverity(startDate, endDate, category, status);
        return ResponseEntity.ok(ApiResponse.ok(response, "Severity metrics computed successfully"));
    }

    @GetMapping("/incidents/by-status")
    @Operation(summary = "Get incident breakdown by lifecycle status",
            description = "Computes distribution across OPEN, IN_PROGRESS, RESOLVED, and CLOSED statuses.")
    public ResponseEntity<ApiResponse<List<StatusMetricDTO>>> getIncidentsByStatus(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String severity
    ) {
        List<StatusMetricDTO> response = analyticsService.getIncidentsByStatus(startDate, endDate, category, severity);
        return ResponseEntity.ok(ApiResponse.ok(response, "Status metrics computed successfully"));
    }

    @GetMapping("/sla-performance")
    @Operation(summary = "Get comprehensive SLA performance intelligence",
            description = "Computes compliance rate, target vs actuals, escalation levels, and historical compliance trends.")
    public ResponseEntity<ApiResponse<SlaPerformanceResponse>> getSlaPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String severity
    ) {
        SlaPerformanceResponse response = analyticsService.getSlaPerformance(startDate, endDate, category, severity);
        return ResponseEntity.ok(ApiResponse.ok(response, "SLA performance intelligence computed successfully"));
    }

    @GetMapping("/infrastructure")
    @Operation(summary = "Get infrastructure asset health and operational breakdown",
            description = "Aggregates total assets, operational rates, status breakdown, and maintenance priorities.")
    public ResponseEntity<ApiResponse<InfrastructureAnalyticsResponse>> getInfrastructureAnalytics() {
        InfrastructureAnalyticsResponse response = analyticsService.getInfrastructureAnalytics();
        return ResponseEntity.ok(ApiResponse.ok(response, "Infrastructure analytics computed successfully"));
    }

    @GetMapping("/response-times")
    @Operation(summary = "Get first response and resolution time intelligence",
            description = "Computes average, median, min, max, and bucket distributions for operational response and resolution.")
    public ResponseEntity<ApiResponse<ResponseTimesResponse>> getResponseTimes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String severity
    ) {
        ResponseTimesResponse response = analyticsService.getResponseTimes(startDate, endDate, category, severity);
        return ResponseEntity.ok(ApiResponse.ok(response, "Response times intelligence computed successfully"));
    }

    @GetMapping("/locations")
    @Operation(summary = "Get geographic incident concentrations by location / ward",
            description = "Returns top incident clusters by location name, count, critical incidents, and geographic coordinates.")
    public ResponseEntity<ApiResponse<LocationAnalyticsResponse>> getLocationAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "10") int limit
    ) {
        LocationAnalyticsResponse response = analyticsService.getLocationAnalytics(startDate, endDate, category, severity, limit);
        return ResponseEntity.ok(ApiResponse.ok(response, "Location analytics computed successfully"));
    }

    @GetMapping(value = "/export/csv", produces = "text/csv")
    @Operation(summary = "Export incidents analytics data as sanitized CSV",
            description = "Exports filtered incidents in CSV format with formula injection protection (CWE-1236).")
    public ResponseEntity<byte[]> exportIncidentsCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1000") int limit
    ) {
        byte[] csvData = analyticsService.exportIncidentsCsv(startDate, endDate, category, severity, status, limit);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"civicpulse_analytics_" + System.currentTimeMillis() + ".csv\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csvData);
    }

    // ==========================================
    // Legacy endpoints for backward compatibility
    // ==========================================

    @GetMapping("/incidents")
    @Operation(summary = "Legacy incident trends and distribution analytics")
    public ResponseEntity<ApiResponse<IncidentsAnalyticsResponse>> getIncidentsAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        IncidentsAnalyticsResponse response = analyticsService.getIncidentsAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(response, "Incidents analytics computed successfully"));
    }

    @GetMapping("/sla")
    @Operation(summary = "Legacy SLA compliance analytics")
    public ResponseEntity<ApiResponse<SlaAnalyticsResponse>> getSlaAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        SlaAnalyticsResponse response = analyticsService.getSlaAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(response, "SLA analytics computed successfully"));
    }

    @GetMapping("/departments")
    @Operation(summary = "Legacy department workload analytics")
    public ResponseEntity<ApiResponse<DepartmentAnalyticsResponse>> getDepartmentAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        DepartmentAnalyticsResponse response = analyticsService.getDepartmentAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(response, "Department workload analytics computed successfully"));
    }

    @GetMapping("/sensors")
    @Operation(summary = "Legacy sensor analytics")
    public ResponseEntity<ApiResponse<SensorAnalyticsResponse>> getSensorAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        SensorAnalyticsResponse response = analyticsService.getSensorAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(response, "Sensor analytics computed successfully"));
    }
}
