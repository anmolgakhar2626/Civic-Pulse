package com.smartcity.priority;

import com.smartcity.incident.Incident;
import com.smartcity.incident.IncidentRepository;
import com.smartcity.incident.IncidentSource;
import com.smartcity.incident.IncidentStatus;
import com.smartcity.incident.IncidentStatusHistory;
import com.smartcity.incident.IncidentStatusHistoryRepository;
import com.smartcity.priority.dto.EvaluatePriorityRequest;
import com.smartcity.priority.dto.PriorityEvaluationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PriorityIntegrationTest {

    @Autowired
    private PriorityService priorityService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private IncidentStatusHistoryRepository statusHistoryRepository;

    @Test
    @DisplayName("Evaluate Priority API standalone: verifies rule matching and explainability from database rules")
    void testEvaluatePriorityApi_standalone() {
        EvaluatePriorityRequest request = new EvaluatePriorityRequest();
        request.setIncidentType("TRAFFIC_SIGNAL_FAILURE");
        request.setMajorJunction(true);
        request.setPublicExposure(PublicExposure.HIGH);

        PriorityEvaluationResponse response = priorityService.evaluatePriority(request);

        assertThat(response).isNotNull();
        assertThat(response.getTier()).isEqualTo(PriorityTier.HIGH);
        assertThat(response.getPriorityLabel()).isEqualTo("HIGH");
        assertThat(response.getMatchedRule()).isEqualTo("TRAFFIC_SIGNAL_MAJOR_JUNCTION");
        assertThat(response.getRecommendedSlaCategory()).isEqualTo("2–4 hours");
        assertThat(response.getReason()).isNotEmpty();
        assertThat(response.getEvaluatedAt()).isNotNull();
        assertThat(response.isManualClassificationRequired()).isFalse();
    }

    @Test
    @DisplayName("Evaluate Incident Priority: updates Incident, persists priority and reasons, transitions to PRIORITIZED and audits history")
    void testEvaluateIncidentPriority_persistenceAndStatus() {
        // Create unprioritized incident in DB
        Incident incident = new Incident();
        incident.setIncidentNumber("INC-PRIORITY-TEST-01");
        incident.setIncidentType("ELECTRICAL_HAZARD");
        incident.setDescription("Live exposed power line hanging low near bus stop");
        incident.setSource(IncidentSource.CITIZEN);
        incident.setLatitude(12.98);
        incident.setLongitude(77.60);
        incident.setStatus(IncidentStatus.DETECTED);
        incident.setSeverity(Severity.EXTREME);
        incident.setSafetyImpact("EXPOSED_LIVE_WIRE");
        incident = incidentRepository.saveAndFlush(incident);

        // Evaluate priority via service
        PriorityEvaluationResponse response = priorityService.evaluateIncidentPriority(incident.getId());

        assertThat(response.getTier()).isEqualTo(PriorityTier.CRITICAL);
        assertThat(response.getMatchedRule()).isEqualTo("LIVE_ELECTRICAL_WIRE");
        assertThat(response.getSlaMinutes()).isEqualTo(30);

        // Verify incident in database is updated
        Incident updatedIncident = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(updatedIncident.getTier()).isEqualTo(PriorityTier.CRITICAL);
        assertThat(updatedIncident.getPriorityLabel()).isEqualTo("CRITICAL");
        assertThat(updatedIncident.getPriorityRuleId()).isEqualTo("LIVE_ELECTRICAL_WIRE");
        assertThat(updatedIncident.getPriorityReason()).contains("Live exposed electrical wire");
        assertThat(updatedIncident.getPriorityEvaluatedAt()).isNotNull();
        assertThat(updatedIncident.getSlaMinutes()).isEqualTo(30);
        assertThat(updatedIncident.getSlaDeadline()).isNotNull();
        assertThat(updatedIncident.getStatus()).isEqualTo(IncidentStatus.PRIORITIZED);

        // Verify status transition history audit record
        List<IncidentStatusHistory> history = statusHistoryRepository.findByIncidentIdOrderByChangedAtDesc(incident.getId());
        assertThat(history).isNotEmpty();
        assertThat(history.get(0).getToStatus()).isEqualTo("PRIORITIZED");
        assertThat(history.get(0).getChangedBy()).isEqualTo("PRIORITY_ENGINE");
        assertThat(history.get(0).getRemarks()).contains("CRITICAL");
    }
}
