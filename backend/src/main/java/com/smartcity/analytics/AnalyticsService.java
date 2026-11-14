package com.smartcity.analytics;

import com.smartcity.analytics.dto.*;

import java.time.Instant;
import java.util.List;

public interface AnalyticsService {

    OverviewAnalyticsResponse getOverview(Instant startDate, Instant endDate, String category, String severity);

    List<TrendMetricDTO> getIncidentTrends(Instant startDate, Instant endDate, String category, String severity, String interval);

    List<CategoryMetricDTO> getIncidentsByCategory(Instant startDate, Instant endDate, String severity, String status);

    List<SeverityMetricDTO> getIncidentsBySeverity(Instant startDate, Instant endDate, String category, String status);

    List<StatusMetricDTO> getIncidentsByStatus(Instant startDate, Instant endDate, String category, String severity);

    SlaPerformanceResponse getSlaPerformance(Instant startDate, Instant endDate, String category, String severity);

    InfrastructureAnalyticsResponse getInfrastructureAnalytics();

    ResponseTimesResponse getResponseTimes(Instant startDate, Instant endDate, String category, String severity);

    LocationAnalyticsResponse getLocationAnalytics(Instant startDate, Instant endDate, String category, String severity, int limit);

    byte[] exportIncidentsCsv(Instant startDate, Instant endDate, String category, String severity, String status, int limit);

    // Existing methods for backward compatibility
    OverviewAnalyticsResponse getOverviewAnalytics(Instant startDate, Instant endDate);

    IncidentsAnalyticsResponse getIncidentsAnalytics(Instant startDate, Instant endDate);

    SlaAnalyticsResponse getSlaAnalytics(Instant startDate, Instant endDate);

    DepartmentAnalyticsResponse getDepartmentAnalytics(Instant startDate, Instant endDate);

    SensorAnalyticsResponse getSensorAnalytics(Instant startDate, Instant endDate);
}
