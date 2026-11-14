package com.smartcity.assignment;

import com.smartcity.incident.Incident;
import com.smartcity.incident.IncidentStatus;
import com.smartcity.priority.PriorityTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Response Policy & Action Engine Tests")
class ResponsePolicyTest {

    private ResponsePolicy responsePolicy;

    @BeforeEach
    void setUp() {
        responsePolicy = new ResponsePolicy();
    }

    @Test
    @DisplayName("11a. TIER 1 Major Accident response plan: EMERGENCY_RESPONSE, ASSIGN_FIELD_TEAM, REQUEST_OPERATOR_REVIEW")
    void testTier1MajorAccidentResponsePlan() {
        Incident incident = new Incident();
        incident.setIncidentType("MAJOR_ROAD_ACCIDENT");
        incident.setTier(PriorityTier.CRITICAL);
        incident.setEscalationState("NONE");

        List<ResponsePolicy.RecommendedAction> actions = responsePolicy.evaluate(incident);

        assertFalse(actions.isEmpty());
        assertTrue(actions.stream().anyMatch(a -> a.actionType() == ResponseActionType.EMERGENCY_RESPONSE));
        assertTrue(actions.stream().anyMatch(a -> a.actionType() == ResponseActionType.ASSIGN_FIELD_TEAM));
        assertTrue(actions.stream().anyMatch(a -> a.actionType() == ResponseActionType.REQUEST_OPERATOR_REVIEW));
        assertTrue(actions.stream().anyMatch(a -> a.actionType() == ResponseActionType.RECOMMEND_TRAFFIC_REROUTE));
    }

    @Test
    @DisplayName("11b. TIER 1 Critical incident with SLA breach triggers ESCALATE action")
    void testCriticalIncidentWithSlaBreachTriggersEscalate() {
        Incident incident = new Incident();
        incident.setIncidentType("ELECTRICAL_HAZARD");
        incident.setTier(PriorityTier.CRITICAL);
        incident.setEscalationState("LEVEL_2"); // SLA Breached

        List<ResponsePolicy.RecommendedAction> actions = responsePolicy.evaluate(incident);

        assertTrue(actions.stream().anyMatch(a -> a.actionType() == ResponseActionType.ESCALATE),
                "Should include ESCALATE action when SLA is breached");
    }

    @Test
    @DisplayName("11c. TIER 2 Traffic Signal Failure: CREATE_WORK_ORDER, ASSIGN_FIELD_TEAM, MARK_GIS_CAUTION_ZONE, RECOMMEND_TRAFFIC_REROUTE")
    void testTier2TrafficSignalResponsePlan() {
        Incident incident = new Incident();
        incident.setIncidentType("TRAFFIC_SIGNAL_FAILURE");
        incident.setTier(PriorityTier.HIGH);

        List<ResponsePolicy.RecommendedAction> actions = responsePolicy.evaluate(incident);

        assertTrue(actions.stream().anyMatch(a -> a.actionType() == ResponseActionType.CREATE_WORK_ORDER));
        assertTrue(actions.stream().anyMatch(a -> a.actionType() == ResponseActionType.ASSIGN_FIELD_TEAM));
        assertTrue(actions.stream().anyMatch(a -> a.actionType() == ResponseActionType.MARK_GIS_CAUTION_ZONE));
        assertTrue(actions.stream().anyMatch(a -> a.actionType() == ResponseActionType.RECOMMEND_TRAFFIC_REROUTE));
    }

    @Test
    @DisplayName("11d. TIER 3 Waste Overflow: CREATE_WORK_ORDER, ADD_TO_COLLECTION_ROUTE, ASSIGN_FIELD_TEAM")
    void testTier3WasteOverflowResponsePlan() {
        Incident incident = new Incident();
        incident.setIncidentType("WASTE_OVERFLOW");
        incident.setTier(PriorityTier.MEDIUM);

        List<ResponsePolicy.RecommendedAction> actions = responsePolicy.evaluate(incident);

        assertTrue(actions.stream().anyMatch(a -> a.actionType() == ResponseActionType.CREATE_WORK_ORDER));
        assertTrue(actions.stream().anyMatch(a -> a.actionType() == ResponseActionType.ADD_TO_COLLECTION_ROUTE));
        assertTrue(actions.stream().anyMatch(a -> a.actionType() == ResponseActionType.ASSIGN_FIELD_TEAM));
    }

    @Test
    @DisplayName("11e. TIER 4 Graffiti: CREATE_WORK_ORDER, ADD_TO_MAINTENANCE_ROUTE")
    void testTier4GraffitiResponsePlan() {
        Incident incident = new Incident();
        incident.setIncidentType("GRAFFITI");
        incident.setTier(PriorityTier.LOW);

        List<ResponsePolicy.RecommendedAction> actions = responsePolicy.evaluate(incident);

        assertTrue(actions.stream().anyMatch(a -> a.actionType() == ResponseActionType.CREATE_WORK_ORDER));
        assertTrue(actions.stream().anyMatch(a -> a.actionType() == ResponseActionType.ADD_TO_MAINTENANCE_ROUTE));
    }

    @Test
    @DisplayName("12. Response-action status transitions and completion timestamp")
    void testResponseActionStatusProgression() {
        ResponseAction action = new ResponseAction(
                new Incident(),
                ResponseActionType.CREATE_WORK_ORDER,
                "Generate work order",
                ResponseActionStatus.RECOMMENDED,
                "DISPATCHER"
        );

        assertEquals(ResponseActionStatus.RECOMMENDED, action.getStatus());
        assertNull(action.getCompletedAt());

        action.setStatus(ResponseActionStatus.APPROVED);
        assertEquals(ResponseActionStatus.APPROVED, action.getStatus());

        action.setStatus(ResponseActionStatus.IN_PROGRESS);
        assertEquals(ResponseActionStatus.IN_PROGRESS, action.getStatus());

        action.setStatus(ResponseActionStatus.COMPLETED);
        assertEquals(ResponseActionStatus.COMPLETED, action.getStatus());
        assertNotNull(action.getCompletedAt(), "completedAt must be populated when status is COMPLETED");
    }
}
