package com.smartcity.analytics;

import com.smartcity.analytics.dto.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface AnalyticsDao {

    OverviewAnalyticsResponse getOverview(Instant startDate, Instant endDate, String category, String severity);

    List<TrendMetricDTO> getIncidentTrends(Instant startDate, Instant endDate, String category, String severity, String interval);

    List<CategoryMetricDTO> getIncidentsByCategory(Instant startDate, Instant endDate, String severity, String status);

    List<SeverityMetricDTO> getIncidentsBySeverity(Instant startDate, Instant endDate, String category, String status);

    List<StatusMetricDTO> getIncidentsByStatus(Instant startDate, Instant endDate, String category, String severity);

    SlaPerformanceResponse getSlaPerformance(Instant startDate, Instant endDate, String category, String severity);

    InfrastructureAnalyticsResponse getInfrastructureAnalytics();

    ResponseTimesResponse getResponseTimes(Instant startDate, Instant endDate, String category, String severity);

    LocationAnalyticsResponse getLocationAnalytics(Instant startDate, Instant endDate, String category, String severity, int limit);

    List<Map<String, Object>> getIncidentsForExport(Instant startDate, Instant endDate, String category, String severity, String status, int limit);
}

