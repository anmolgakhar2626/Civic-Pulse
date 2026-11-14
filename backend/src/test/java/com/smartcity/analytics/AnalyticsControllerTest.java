package com.smartcity.analytics;

import com.smartcity.analytics.dto.*;
import com.smartcity.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsController REST Endpoints & CSV Export Tests")
class AnalyticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        AnalyticsController controller = new AnalyticsController(analyticsService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/analytics/overview returns 200 with comprehensive operational KPIs")
    void testGetOverview() throws Exception {
        OverviewAnalyticsResponse response = new OverviewAnalyticsResponse();
        response.setTotalIncidents(120);
        response.setActiveIncidents(45);
        response.setResolvedIncidents(75);
        response.setCriticalIncidents(8);
        response.setHighSeverityIncidents(22);
        response.setSlaComplianceRate(92.5);
        response.setSlaBreaches(9);
        response.setAvgResolutionTimeHours(3.5);
        response.setAvgFirstResponseMinutes(14.2);
        response.setOperationalInfrastructure(42);
        response.setDegradedInfrastructure(3);

        when(analyticsService.getOverview(any(), any(), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/analytics/overview")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalIncidents").value(120))
                .andExpect(jsonPath("$.data.activeIncidents").value(45))
                .andExpect(jsonPath("$.data.resolvedIncidents").value(75))
                .andExpect(jsonPath("$.data.criticalIncidents").value(8))
                .andExpect(jsonPath("$.data.slaComplianceRate").value(92.5))
                .andExpect(jsonPath("$.data.operationalInfrastructure").value(42));
    }

    @Test
    @DisplayName("GET /api/analytics/incidents/trends returns 200 with time series velocity")
    void testGetIncidentTrends() throws Exception {
        TrendMetricDTO trend1 = new TrendMetricDTO("2026-10-25", 14L, 10L, 1L);
        TrendMetricDTO trend2 = new TrendMetricDTO("2026-10-26", 18L, 15L, 0L);

        when(analyticsService.getIncidentTrends(any(), any(), any(), any(), eq("day")))
                .thenReturn(List.of(trend1, trend2));

        mockMvc.perform(get("/api/analytics/incidents/trends")
                        .param("interval", "day")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].period").value("2026-10-25"))
                .andExpect(jsonPath("$.data[0].createdCount").value(14))
                .andExpect(jsonPath("$.data[0].resolvedCount").value(10))
                .andExpect(jsonPath("$.data[1].createdCount").value(18));
    }

    @Test
    @DisplayName("GET /api/analytics/incidents/by-category returns 200 with category breakdowns")
    void testGetIncidentsByCategory() throws Exception {
        CategoryMetricDTO cat1 = new CategoryMetricDTO("Water Supply", 35L, 35.0, 4L, 2.8);
        CategoryMetricDTO cat2 = new CategoryMetricDTO("Power Outage", 25L, 25.0, 6L, 1.9);

        when(analyticsService.getIncidentsByCategory(any(), any(), any(), any()))
                .thenReturn(List.of(cat1, cat2));

        mockMvc.perform(get("/api/analytics/incidents/by-category")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].category").value("Water Supply"))
                .andExpect(jsonPath("$.data[0].count").value(35))
                .andExpect(jsonPath("$.data[0].criticalCount").value(4));
    }

    @Test
    @DisplayName("GET /api/analytics/incidents/by-severity returns 200 with severity tiers")
    void testGetIncidentsBySeverity() throws Exception {
        SeverityMetricDTO s1 = new SeverityMetricDTO("CRITICAL", 10L, 10.0);
        SeverityMetricDTO s2 = new SeverityMetricDTO("HIGH", 30L, 30.0);
        SeverityMetricDTO s3 = new SeverityMetricDTO("MEDIUM", 45L, 45.0);
        SeverityMetricDTO s4 = new SeverityMetricDTO("LOW", 15L, 15.0);

        when(analyticsService.getIncidentsBySeverity(any(), any(), any(), any()))
                .thenReturn(List.of(s1, s2, s3, s4));

        mockMvc.perform(get("/api/analytics/incidents/by-severity")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$.data[0].count").value(10));
    }

    @Test
    @DisplayName("GET /api/analytics/incidents/by-status returns 200 with lifecycle statuses")
    void testGetIncidentsByStatus() throws Exception {
        StatusMetricDTO st1 = new StatusMetricDTO("OPEN", 20L, 20.0);
        StatusMetricDTO st2 = new StatusMetricDTO("IN_PROGRESS", 25L, 25.0);
        StatusMetricDTO st3 = new StatusMetricDTO("RESOLVED", 55L, 55.0);

        when(analyticsService.getIncidentsByStatus(any(), any(), any(), any()))
                .thenReturn(List.of(st1, st2, st3));

        mockMvc.perform(get("/api/analytics/incidents/by-status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].status").value("OPEN"))
                .andExpect(jsonPath("$.data[2].status").value("RESOLVED"));
    }

    @Test
    @DisplayName("GET /api/analytics/sla-performance returns 200 with SLA metrics")
    void testGetSlaPerformance() throws Exception {
        SlaPerformanceResponse sla = new SlaPerformanceResponse();
        sla.setTotalTracked(80);
        sla.setCompliantCount(74);
        sla.setBreachedCount(6);
        sla.setComplianceRate(92.5);
        sla.setAvgResponseTimeMinutes(11.4);
        sla.setAvgResolutionTimeMinutes(125.0);
        sla.setEscalationDistribution(Map.of("NONE", 65L, "LEVEL_1", 10L, "LEVEL_2", 5L));

        when(analyticsService.getSlaPerformance(any(), any(), any(), any())).thenReturn(sla);

        mockMvc.perform(get("/api/analytics/sla-performance")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.complianceRate").value(92.5))
                .andExpect(jsonPath("$.data.totalTracked").value(80))
                .andExpect(jsonPath("$.data.breachedCount").value(6));
    }

    @Test
    @DisplayName("GET /api/analytics/infrastructure returns 200 with health metrics")
    void testGetInfrastructureAnalytics() throws Exception {
        InfrastructureAnalyticsResponse infra = new InfrastructureAnalyticsResponse();
        infra.setTotalAssets(50);
        infra.setOperationalCount(44);
        infra.setDegradedCount(4);
        infra.setOfflineCount(2);
        infra.setOperationalRate(88.0);

        when(analyticsService.getInfrastructureAnalytics()).thenReturn(infra);

        mockMvc.perform(get("/api/analytics/infrastructure")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalAssets").value(50))
                .andExpect(jsonPath("$.data.operationalRate").value(88.0));
    }

    @Test
    @DisplayName("GET /api/analytics/response-times returns 200 with duration distributions")
    void testGetResponseTimes() throws Exception {
        ResponseTimesResponse rt = new ResponseTimesResponse();
        rt.setAvgFirstResponseMinutes(12.3);
        rt.setAvgResolutionMinutes(95.0);
        rt.setAvgResolutionHours(1.58);
        rt.setResponseTimeDistribution(Map.of("<15m", 30L, "15-30m", 15L, "30-60m", 5L, ">60m", 2L));

        when(analyticsService.getResponseTimes(any(), any(), any(), any())).thenReturn(rt);

        mockMvc.perform(get("/api/analytics/response-times")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.avgFirstResponseMinutes").value(12.3))
                .andExpect(jsonPath("$.data.avgResolutionHours").value(1.58));
    }

    @Test
    @DisplayName("GET /api/analytics/locations returns 200 with ward clusters")
    void testGetLocationAnalytics() throws Exception {
        LocationAnalyticsDTO loc1 = new LocationAnalyticsDTO("Sector 17 Plaza", 25L, 3L, 30.7398, 76.7827);
        LocationAnalyticsResponse locResp = new LocationAnalyticsResponse(List.of(loc1), 1);

        when(analyticsService.getLocationAnalytics(any(), any(), any(), any(), eq(10)))
                .thenReturn(locResp);

        mockMvc.perform(get("/api/analytics/locations")
                        .param("limit", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.locations", hasSize(1)))
                .andExpect(jsonPath("$.data.locations[0].locationName").value("Sector 17 Plaza"))
                .andExpect(jsonPath("$.data.locations[0].count").value(25));
    }

    @Test
    @DisplayName("GET /api/analytics/export/csv returns 200 with text/csv and Content-Disposition")
    void testExportIncidentsCsv() throws Exception {
        String csvContent = "\uFEFFIncident Number,Title,Category,Severity,Status,Location Name,Latitude,Longitude,Created At,Resolution Time\n"
                + "INC-20261028-01,Water Pipe Break,WATER,HIGH,RESOLVED,Sector 17,30.7398,76.7827,2026-10-28 10:00:00,2026-10-28 12:30:00\n";
        byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

        when(analyticsService.exportIncidentsCsv(any(), any(), any(), any(), any(), eq(1000)))
                .thenReturn(csvBytes);

        mockMvc.perform(get("/api/analytics/export/csv")
                        .param("limit", "1000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"civicpulse_analytics_")))
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(content().bytes(csvBytes));
    }
}
