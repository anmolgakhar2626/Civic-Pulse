package com.smartcity.assignment;

import com.smartcity.incident.Incident;
import com.smartcity.priority.PriorityTier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ResponsePolicy {

    public record RecommendedAction(ResponseActionType actionType, String description) {}

    /**
     * Evaluates incident characteristics and produces a list of recommended actions.
     * Section 7 Examples:
     * - TIER 1 major accident: EMERGENCY_RESPONSE, ASSIGN_FIELD_TEAM, REQUEST_OPERATOR_REVIEW, ESCALATE if SLA breached
     * - TIER 2 traffic signal failure: CREATE_WORK_ORDER, ASSIGN_FIELD_TEAM, MARK_GIS_CAUTION_ZONE, RECOMMEND_TRAFFIC_REROUTE
     * - TIER 3 waste overflow: CREATE_WORK_ORDER, ADD_TO_COLLECTION_ROUTE, ASSIGN_FIELD_TEAM
     * - TIER 4 graffiti: CREATE_WORK_ORDER, ADD_TO_MAINTENANCE_ROUTE
     */
    public List<RecommendedAction> evaluate(Incident incident) {
        List<RecommendedAction> actions = new ArrayList<>();
        if (incident == null) {
            return actions;
        }

        PriorityTier tier = incident.getTier();
        String type = incident.getIncidentType() != null ? incident.getIncidentType().trim().toUpperCase() : "";

        // Normalize checks
        boolean isAccident = type.contains("ACCIDENT") || type.contains("COLLISION");
        boolean isElectricalOrGas = type.contains("ELECTRICAL") || type.contains("GAS") || type.contains("FIRE");
        boolean isTraffic = type.contains("TRAFFIC") || type.contains("SIGNAL") || isAccident;
        boolean isWaste = type.contains("WASTE") || type.contains("DUMP") || type.contains("BIN");
        boolean isMaintenance = type.contains("GRAFFITI") || type.contains("CRACK") || type.contains("SIDEWALK") || type.contains("TREE");
        boolean isWaterOrFlood = type.contains("FLOOD") || type.contains("WATER") || type.contains("PIPELINE") || type.contains("DRAIN");

        // 1. TIER 1 (CRITICAL) Policies
        if (tier == PriorityTier.CRITICAL || isAccident || (isElectricalOrGas && tier != PriorityTier.LOW)) {
            actions.add(new RecommendedAction(
                    ResponseActionType.EMERGENCY_RESPONSE,
                    "Initiate immediate emergency response protocol for critical public safety incident"
            ));
            actions.add(new RecommendedAction(
                    ResponseActionType.ASSIGN_FIELD_TEAM,
                    "Dispatch emergency rapid response field team immediately"
            ));
            actions.add(new RecommendedAction(
                    ResponseActionType.REQUEST_OPERATOR_REVIEW,
                    "Request Command Centre Watch Officer immediate operational review and oversight"
            ));

            if (isTraffic) {
                actions.add(new RecommendedAction(
                        ResponseActionType.MARK_GIS_CAUTION_ZONE,
                        "Establish GIS safety caution perimeter around incident coordinates"
                ));
                actions.add(new RecommendedAction(
                        ResponseActionType.RECOMMEND_TRAFFIC_REROUTE,
                        "Broadcast traffic advisory and recommend detour reroutes around incident zone"
                ));
            }

            // Check if SLA is breached or escalated
            if ("LEVEL_2".equalsIgnoreCase(incident.getEscalationState()) ||
                "LEVEL_3".equalsIgnoreCase(incident.getEscalationState()) ||
                "ESCALATED".equalsIgnoreCase(incident.getStatus().name())) {
                actions.add(new RecommendedAction(
                        ResponseActionType.ESCALATE,
                        "Escalate incident to Municipal Emergency Director due to active breach/escalation"
                ));
            }
            return actions;
        }

        // 2. TIER 2 (HIGH) Policies
        if (tier == PriorityTier.HIGH || isTraffic) {
            actions.add(new RecommendedAction(
                    ResponseActionType.CREATE_WORK_ORDER,
                    "Generate high-priority work order for immediate municipal response"
            ));
            actions.add(new RecommendedAction(
                    ResponseActionType.ASSIGN_FIELD_TEAM,
                    "Assign specialized field technical team for on-site assessment"
            ));
            actions.add(new RecommendedAction(
                    ResponseActionType.MARK_GIS_CAUTION_ZONE,
                    "Mark caution zone on GIS spatial layer to alert operations"
            ));
            if (isTraffic || isWaterOrFlood) {
                actions.add(new RecommendedAction(
                        ResponseActionType.RECOMMEND_TRAFFIC_REROUTE,
                        "Recommend smart transit rerouting to prevent localized congestion"
                ));
            }
            return actions;
        }

        // 3. TIER 3 (MEDIUM) Policies
        if (tier == PriorityTier.MEDIUM || isWaste) {
            actions.add(new RecommendedAction(
                    ResponseActionType.CREATE_WORK_ORDER,
                    "Generate municipal service work order for operational team"
            ));
            if (isWaste) {
                actions.add(new RecommendedAction(
                        ResponseActionType.ADD_TO_COLLECTION_ROUTE,
                        "Add affected location to prioritized sanitation collection route"
                ));
            }
            actions.add(new RecommendedAction(
                    ResponseActionType.ASSIGN_FIELD_TEAM,
                    "Assign departmental field unit for scheduled dispatch"
            ));
            return actions;
        }

        // 4. TIER 4 (LOW) Policies
        actions.add(new RecommendedAction(
                ResponseActionType.CREATE_WORK_ORDER,
                "Generate standard municipal maintenance ticket"
        ));
        actions.add(new RecommendedAction(
                ResponseActionType.ADD_TO_MAINTENANCE_ROUTE,
                "Add incident to scheduled routine municipal maintenance cycle"
        ));
        return actions;
    }
}
