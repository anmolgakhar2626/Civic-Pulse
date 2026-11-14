package com.smartcity.priority;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcity.priority.dto.EvaluatePriorityRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PriorityEngineTest {

    private PriorityEngine priorityEngine;
    private List<PriorityRule> defaultRules;

    @BeforeEach
    void setUp() {
        priorityEngine = new PriorityEngine(new ObjectMapper());
        defaultRules = createDefaultRules();
    }

    private List<PriorityRule> createDefaultRules() {
        List<PriorityRule> rules = new ArrayList<>();

        // Tier 1: CRITICAL
        rules.add(new PriorityRule("LIVE_ELECTRICAL_WIRE", "Live Exposed Electrical Wire", "ELECTRICAL_HAZARD",
                PriorityTier.CRITICAL, 30, "{\"exposedLiveWire\": true}",
                "[\"Live exposed electrical wire hazard\", \"Immediate electrocution and public safety hazard\"]", true));

        rules.add(new PriorityRule("MAJOR_ROAD_ACCIDENT", "Major Road Collision", "TRAFFIC_COLLISION",
                PriorityTier.CRITICAL, 30, "{\"severity\": \"EXTREME\"}",
                "[\"Major vehicular collision on municipal roadway\", \"Critical emergency dispatch required\"]", true));

        rules.add(new PriorityRule("OPEN_MANHOLE_BUSY_ROAD", "Open Manhole on Busy Road", "OPEN_MANHOLE",
                PriorityTier.CRITICAL, 30, "{\"busyRoad\": true}",
                "[\"Open uncovered manhole on high-traffic road\", \"Severe collision and pedestrian danger\"]", true));

        rules.add(new PriorityRule("SUBMERGED_UNDERPASS", "Submerged Underpass", "FLASH_FLOOD",
                PriorityTier.CRITICAL, 30, "{\"submergedUnderpass\": true}",
                "[\"Sudden deep flash flood submerging roadway underpass\"]", true));

        rules.add(new PriorityRule("WASTE_OBSTRUCTION_EMERGENCY", "Waste Obstruction Blocking Emergency Route", "WASTE_OVERFLOW",
                PriorityTier.CRITICAL, 30, "{\"blockingEmergencyAccess\": true}",
                "[\"Waste obstruction blocking critical emergency vehicle access\"]", true));

        // Tier 2: HIGH
        rules.add(new PriorityRule("TRAFFIC_SIGNAL_MAJOR_JUNCTION", "Traffic Signal Failure at Major Junction", "TRAFFIC_SIGNAL_FAILURE",
                PriorityTier.HIGH, 240, "{\"majorJunction\": true}",
                "[\"Traffic signal non-functional at major arterial junction\", \"Major arterial gridlock hazard\"]", true));

        rules.add(new PriorityRule("MAIN_PIPELINE_BURST", "Main Water Pipeline Burst", "WATER_PIPE_BURST",
                PriorityTier.HIGH, 240, "{\"severity\": \"HIGH\"}",
                "[\"Primary municipal water transmission pipeline rupture\", \"Flooding and water loss risk\"]", true));

        rules.add(new PriorityRule("DRINKING_WATER_CONTAMINATION", "Drinking Water Contamination", "WATER_CONTAMINATION",
                PriorityTier.HIGH, 240, "{}",
                "[\"Detected contamination in municipal drinking water supply\"]", true));

        rules.add(new PriorityRule("ROAD_CAVE_IN", "Roadway Cave-In / Sinkhole", "ROAD_DAMAGE",
                PriorityTier.HIGH, 240, "{\"roadCaveIn\": true}",
                "[\"Structural roadway cave-in / sinkhole detected\"]", true));

        rules.add(new PriorityRule("GAS_LEAKAGE", "Hazardous Gas Pipeline Leak", "GAS_LEAK",
                PriorityTier.HIGH, 240, "{}",
                "[\"Flammable or toxic gas leakage detected\"]", true));

        rules.add(new PriorityRule("PM25_HAZARDOUS_SMOG", "Severe Smog Cluster (PM2.5 > 300)", "AIR_POLLUTION",
                PriorityTier.HIGH, 240, "{\"minPm25\": 300}",
                "[\"PM2.5 particulate level exceeds 300 µg/m³ (Hazardous)\"]", true));

        // Tier 3: MEDIUM
        rules.add(new PriorityRule("WASTE_BIN_OVERFLOW_NORMAL", "Smart Waste Bin >90% Capacity", "WASTE_OVERFLOW",
                PriorityTier.MEDIUM, 1440, "{\"minFillPercentage\": 90, \"blockingEmergencyAccess\": false}",
                "[\"Smart waste bin fill exceeds 90% in residential sector\"]", true));

        rules.add(new PriorityRule("TRAFFIC_SIGNAL_MINOR_JUNCTION", "Traffic Signal Failure at Minor Junction", "TRAFFIC_SIGNAL_FAILURE",
                PriorityTier.MEDIUM, 1440, "{\"majorJunction\": false}",
                "[\"Traffic signal non-functional at minor local intersection\"]", true));

        rules.add(new PriorityRule("DEAD_STREETLIGHTS", "Streetlight Cluster Outage", "STREETLIGHT_OUTAGE",
                PriorityTier.MEDIUM, 1440, "{}",
                "[\"Public lighting luminaire failure in ward\"]", true));

        rules.add(new PriorityRule("CLOGGED_STORM_DRAIN", "Clogged Roadside Storm Drain", "DRAINAGE_ISSUE",
                PriorityTier.MEDIUM, 1440, "{}",
                "[\"Roadside storm drain clogged with silt and debris\"]", true));

        rules.add(new PriorityRule("ILLEGAL_DUMPING", "Illegal Roadside Dumping", "ILLEGAL_DUMPING",
                PriorityTier.MEDIUM, 1440, "{}",
                "[\"Unauthorized debris dumped along public right-of-way\"]", true));

        // Tier 4: LOW
        rules.add(new PriorityRule("MINOR_ROAD_CRACKS", "Minor Road Surface Cracks", "ROAD_MAINTENANCE",
                PriorityTier.LOW, 2880, "{}",
                "[\"Superficial pavement fissures scheduled for maintenance\"]", true));

        rules.add(new PriorityRule("FADED_LANE_MARKINGS", "Faded Roadway Lane Markings", "ROAD_MARKINGS",
                PriorityTier.LOW, 2880, "{}",
                "[\"Faded thermal traffic lane markings\"]", true));

        rules.add(new PriorityRule("TREE_PRUNING", "Municipal Tree Pruning Required", "TREE_PRUNING",
                PriorityTier.LOW, 2880, "{}",
                "[\"Overgrown municipal tree branches\"]", true));

        rules.add(new PriorityRule("GRAFFITI_REMOVAL", "Civic Property Graffiti Removal", "VANDALISM",
                PriorityTier.LOW, 2880, "{}",
                "[\"Unauthorized graffiti on municipal asset\"]", true));

        return rules;
    }

    @Test
    @DisplayName("Test 1: Major road accident/collision → CRITICAL (<= 30 min SLA)")
    void test1_majorAccident_evaluatesToCritical() {
        EvaluatePriorityRequest request = new EvaluatePriorityRequest();
        request.setIncidentType("TRAFFIC_COLLISION");
        request.setSeverity(Severity.EXTREME);
        request.setDescription("Three vehicle collision on highway with entrapment");

        PriorityResult result = priorityEngine.evaluate(request, defaultRules);

        assertThat(result.getTier()).isEqualTo(PriorityTier.CRITICAL);
        assertThat(result.getPriorityLabel()).isEqualTo("CRITICAL");
        assertThat(result.getMatchedRule()).isEqualTo("MAJOR_ROAD_ACCIDENT");
        assertThat(result.getSlaMinutes()).isEqualTo(30);
        assertThat(result.getRecommendedSlaCategory()).isEqualTo("<= 30 minutes");
        assertThat(result.getReason()).isNotEmpty();
    }

    @Test
    @DisplayName("Test 2: Live electrical hazard → CRITICAL (<= 30 min SLA)")
    void test2_liveElectricalHazard_evaluatesToCritical() {
        EvaluatePriorityRequest request = new EvaluatePriorityRequest();
        request.setIncidentType("ELECTRICAL_HAZARD");
        request.setExposedLiveWire(true);
        request.setDescription("Live exposed power line snapped and lying across sidewalk");

        PriorityResult result = priorityEngine.evaluate(request, defaultRules);

        assertThat(result.getTier()).isEqualTo(PriorityTier.CRITICAL);
        assertThat(result.getMatchedRule()).isEqualTo("LIVE_ELECTRICAL_WIRE");
        assertThat(result.getReason()).anyMatch(r -> r.contains("Live exposed electrical wire"));
    }

    @Test
    @DisplayName("Test 3: Open manhole on busy road → CRITICAL (<= 30 min SLA)")
    void test3_openManholeOnBusyRoad_evaluatesToCritical() {
        EvaluatePriorityRequest request = new EvaluatePriorityRequest();
        request.setIncidentType("OPEN_MANHOLE");
        request.setBusyRoad(true);
        request.setPublicExposure(PublicExposure.VERY_HIGH);
        request.setDescription("Cover broken on 6-lane ring road");

        PriorityResult result = priorityEngine.evaluate(request, defaultRules);

        assertThat(result.getTier()).isEqualTo(PriorityTier.CRITICAL);
        assertThat(result.getMatchedRule()).isEqualTo("OPEN_MANHOLE_BUSY_ROAD");
        assertThat(result.getReason()).anyMatch(r -> r.contains("high-traffic road"));
    }

    @Test
    @DisplayName("Test 4: Major traffic signal failure → HIGH (2–4 hours SLA)")
    void test4_majorTrafficSignalFailure_evaluatesToHigh() {
        EvaluatePriorityRequest request = new EvaluatePriorityRequest();
        request.setIncidentType("TRAFFIC_SIGNAL_FAILURE");
        request.setMajorJunction(true);
        request.setPublicExposure(PublicExposure.HIGH);

        PriorityResult result = priorityEngine.evaluate(request, defaultRules);

        assertThat(result.getTier()).isEqualTo(PriorityTier.HIGH);
        assertThat(result.getMatchedRule()).isEqualTo("TRAFFIC_SIGNAL_MAJOR_JUNCTION");
        assertThat(result.getSlaMinutes()).isEqualTo(240);
        assertThat(result.getRecommendedSlaCategory()).isEqualTo("2–4 hours");
        assertThat(result.getReason()).anyMatch(r -> r.contains("major arterial junction"));
    }

    @Test
    @DisplayName("Test 5: Pipeline burst → HIGH (2–4 hours SLA)")
    void test5_pipelineBurst_evaluatesToHigh() {
        EvaluatePriorityRequest request = new EvaluatePriorityRequest();
        request.setIncidentType("WATER_PIPE_BURST");
        request.setSeverity(Severity.HIGH);
        request.setDescription("12-inch main transmission pipe ruptured");

        PriorityResult result = priorityEngine.evaluate(request, defaultRules);

        assertThat(result.getTier()).isEqualTo(PriorityTier.HIGH);
        assertThat(result.getMatchedRule()).isEqualTo("MAIN_PIPELINE_BURST");
    }

    @Test
    @DisplayName("Test 6: Gas leakage → HIGH (2–4 hours SLA)")
    void test6_gasLeakage_evaluatesToHigh() {
        EvaluatePriorityRequest request = new EvaluatePriorityRequest();
        request.setIncidentType("GAS_LEAK");
        request.setDescription("Strong odor of natural gas near commercial sector");

        PriorityResult result = priorityEngine.evaluate(request, defaultRules);

        assertThat(result.getTier()).isEqualTo(PriorityTier.HIGH);
        assertThat(result.getMatchedRule()).isEqualTo("GAS_LEAKAGE");
    }

    @Test
    @DisplayName("Test 7: PM2.5 > 300 → HIGH (2–4 hours SLA)")
    void test7_pm25Above300_evaluatesToHigh() {
        EvaluatePriorityRequest request = new EvaluatePriorityRequest();
        request.setIncidentType("AIR_POLLUTION");
        request.setPm25(342.5);

        PriorityResult result = priorityEngine.evaluate(request, defaultRules);

        assertThat(result.getTier()).isEqualTo(PriorityTier.HIGH);
        assertThat(result.getMatchedRule()).isEqualTo("PM25_HAZARDOUS_SMOG");
        assertThat(result.getReason()).anyMatch(r -> r.contains("342.5"));
    }

    @Test
    @DisplayName("Test 8: Waste bin >90% capacity in residential area → MEDIUM (12–24 hours SLA)")
    void test8_wasteBinAbove90Normal_evaluatesToMedium() {
        EvaluatePriorityRequest request = new EvaluatePriorityRequest();
        request.setIncidentType("WASTE_OVERFLOW");
        request.setWasteBinFillPercentage(94.0);
        request.setBlockingEmergencyAccess(false);

        PriorityResult result = priorityEngine.evaluate(request, defaultRules);

        assertThat(result.getTier()).isEqualTo(PriorityTier.MEDIUM);
        assertThat(result.getMatchedRule()).isEqualTo("WASTE_BIN_OVERFLOW_NORMAL");
        assertThat(result.getSlaMinutes()).isEqualTo(1440);
        assertThat(result.getRecommendedSlaCategory()).isEqualTo("12–24 hours");
    }

    @Test
    @DisplayName("Test 9: Dead streetlight → MEDIUM (12–24 hours SLA)")
    void test9_deadStreetlight_evaluatesToMedium() {
        EvaluatePriorityRequest request = new EvaluatePriorityRequest();
        request.setIncidentType("STREETLIGHT_OUTAGE");
        request.setDescription("Dark streetlights along residential sector 4");

        PriorityResult result = priorityEngine.evaluate(request, defaultRules);

        assertThat(result.getTier()).isEqualTo(PriorityTier.MEDIUM);
        assertThat(result.getMatchedRule()).isEqualTo("DEAD_STREETLIGHTS");
    }

    @Test
    @DisplayName("Test 10: Minor road crack → LOW (48–72 hours SLA)")
    void test10_minorRoadCrack_evaluatesToLow() {
        EvaluatePriorityRequest request = new EvaluatePriorityRequest();
        request.setIncidentType("ROAD_MAINTENANCE");
        request.setDescription("Minor hairline cracks on road shoulder");

        PriorityResult result = priorityEngine.evaluate(request, defaultRules);

        assertThat(result.getTier()).isEqualTo(PriorityTier.LOW);
        assertThat(result.getMatchedRule()).isEqualTo("MINOR_ROAD_CRACKS");
        assertThat(result.getSlaMinutes()).isEqualTo(2880);
        assertThat(result.getRecommendedSlaCategory()).isEqualTo("48–72 hours");
    }

    @Test
    @DisplayName("Test 11: Graffiti → LOW (48–72 hours SLA)")
    void test11_graffiti_evaluatesToLow() {
        EvaluatePriorityRequest request = new EvaluatePriorityRequest();
        request.setIncidentType("VANDALISM");
        request.setDescription("Graffiti tags on boundary wall");

        PriorityResult result = priorityEngine.evaluate(request, defaultRules);

        assertThat(result.getTier()).isEqualTo(PriorityTier.LOW);
        assertThat(result.getMatchedRule()).isEqualTo("GRAFFITI_REMOVAL");
    }

    @Test
    @DisplayName("Test 12: Context-aware waste obstruction blocking emergency route → Escalates to CRITICAL")
    void test12_contextAwareWasteObstruction_escalatesToCritical() {
        EvaluatePriorityRequest request = new EvaluatePriorityRequest();
        request.setIncidentType("WASTE_OVERFLOW");
        request.setWasteBinFillPercentage(95.0);
        request.setBlockingEmergencyAccess(true);
        request.setDescription("Dumpster overturned blocking emergency ambulance entrance");

        PriorityResult result = priorityEngine.evaluate(request, defaultRules);

        // Escalates to CRITICAL because emergency access is blocked
        assertThat(result.getTier()).isEqualTo(PriorityTier.CRITICAL);
        assertThat(result.getMatchedRule()).isEqualTo("WASTE_OBSTRUCTION_EMERGENCY");
        assertThat(result.getSlaMinutes()).isEqualTo(30);
        assertThat(result.getReason()).anyMatch(r -> r.contains("emergency"));
    }

    @Test
    @DisplayName("Test 13: Multiple matching rules → Highest safety-critical tier wins (CRITICAL wins over MEDIUM/LOW)")
    void test13_multipleMatchingRules_highestTierWins() {
        EvaluatePriorityRequest request = new EvaluatePriorityRequest();
        // Streetlight outage (normally MEDIUM) with an exposed live wire on the ground (CRITICAL safety hazard)
        request.setIncidentType("STREETLIGHT_OUTAGE");
        request.setExposedLiveWire(true);
        request.setDescription("Streetlight snapped off pole with live electrical wire sparking on pavement");

        PriorityResult result = priorityEngine.evaluate(request, defaultRules);

        assertThat(result.getTier()).isEqualTo(PriorityTier.CRITICAL);
        assertThat(result.getMatchedRule()).isEqualTo("LIVE_ELECTRICAL_WIRE");
        assertThat(result.getReason()).anyMatch(r -> r.contains("Tier escalated"));
    }

    @Test
    @DisplayName("Test 14: Disabled rule is skipped")
    void test14_disabledRule_isSkipped() {
        // Disable the gas leak rule
        for (PriorityRule rule : defaultRules) {
            if ("GAS_LEAKAGE".equals(rule.getRuleCode())) {
                rule.setEnabled(false);
            }
        }

        EvaluatePriorityRequest request = new EvaluatePriorityRequest();
        request.setIncidentType("GAS_LEAK");

        PriorityResult result = priorityEngine.evaluate(request, defaultRules);

        // When the only rule for gas leak is disabled, it must fall back to manual review
        assertThat(result.getTier()).isNull();
        assertThat(result.isManualClassificationRequired()).isTrue();
        assertThat(result.getPriorityLabel()).isEqualTo("MANUAL_REVIEW");
    }

    @Test
    @DisplayName("Test 15: No matching rule returns controlled result indicating manual classification required")
    void test15_noMatchingRule_returnsControlledManualReview() {
        EvaluatePriorityRequest request = new EvaluatePriorityRequest();
        request.setIncidentType("ALIEN_INVASION_ANOMALY");
        request.setDescription("Unusual atmospheric phenomena over city park");

        PriorityResult result = priorityEngine.evaluate(request, defaultRules);

        assertThat(result.getTier()).isNull();
        assertThat(result.getPriorityLabel()).isEqualTo("MANUAL_REVIEW");
        assertThat(result.isManualClassificationRequired()).isTrue();
        assertThat(result.getMatchedRule()).isNull();
        assertThat(result.getRecommendedSlaCategory()).isEqualTo("MANUAL_DISPATCH");
        assertThat(result.getReason()).anyMatch(r -> r.contains("Manual operator classification required"));
    }

    @Test
    @DisplayName("Test 16: Severe flash flood / submerged underpass → CRITICAL (<= 30 min SLA)")
    void test16_severeFlashFlood_evaluatesToCritical() {
        EvaluatePriorityRequest request = new EvaluatePriorityRequest();
        request.setIncidentType("FLASH_FLOOD");
        request.setSubmergedUnderpass(true);
        request.setDescription("Severe flash flood submerged railway underpass with 1.5m water");

        PriorityResult result = priorityEngine.evaluate(request, defaultRules);

        assertThat(result.getTier()).isEqualTo(PriorityTier.CRITICAL);
        assertThat(result.getMatchedRule()).isEqualTo("SUBMERGED_UNDERPASS");
        assertThat(result.getSlaMinutes()).isEqualTo(30);
    }

    @Test
    @DisplayName("Test 17: Illegal dumping → MEDIUM (12–24 hours SLA)")
    void test17_illegalDumping_evaluatesToMedium() {
        EvaluatePriorityRequest request = new EvaluatePriorityRequest();
        request.setIncidentType("ILLEGAL_DUMPING");
        request.setDescription("Construction debris dumped along road shoulder");

        PriorityResult result = priorityEngine.evaluate(request, defaultRules);

        assertThat(result.getTier()).isEqualTo(PriorityTier.MEDIUM);
        assertThat(result.getMatchedRule()).isEqualTo("ILLEGAL_DUMPING");
        assertThat(result.getSlaMinutes()).isEqualTo(1440);
    }

    @Test
    @DisplayName("Test 18: Blocked roadside storm drain → MEDIUM (12–24 hours SLA)")
    void test18_blockedStormDrain_evaluatesToMedium() {
        EvaluatePriorityRequest request = new EvaluatePriorityRequest();
        request.setIncidentType("DRAINAGE_ISSUE");
        request.setDescription("Storm drain blocked with mud and leaf litter");

        PriorityResult result = priorityEngine.evaluate(request, defaultRules);

        assertThat(result.getTier()).isEqualTo(PriorityTier.MEDIUM);
        assertThat(result.getMatchedRule()).isEqualTo("CLOGGED_STORM_DRAIN");
        assertThat(result.getSlaMinutes()).isEqualTo(1440);
    }

    @Test
    @DisplayName("Test 19: Faded roadway markings → LOW (48–72 hours SLA)")
    void test19_fadedRoadMarkings_evaluatesToLow() {
        EvaluatePriorityRequest request = new EvaluatePriorityRequest();
        request.setIncidentType("ROAD_MARKINGS");
        request.setDescription("Pedestrian crossing stripes faded on local avenue");

        PriorityResult result = priorityEngine.evaluate(request, defaultRules);

        assertThat(result.getTier()).isEqualTo(PriorityTier.LOW);
        assertThat(result.getMatchedRule()).isEqualTo("FADED_LANE_MARKINGS");
        assertThat(result.getSlaMinutes()).isEqualTo(2880);
    }

    @Test
    @DisplayName("Test 20: Tree pruning required → LOW (48–72 hours SLA)")
    void test20_treePruning_evaluatesToLow() {
        EvaluatePriorityRequest request = new EvaluatePriorityRequest();
        request.setIncidentType("TREE_PRUNING");
        request.setDescription("Overgrown bougainvillea branches protruding toward sidewalk");

        PriorityResult result = priorityEngine.evaluate(request, defaultRules);

        assertThat(result.getTier()).isEqualTo(PriorityTier.LOW);
        assertThat(result.getMatchedRule()).isEqualTo("TREE_PRUNING");
        assertThat(result.getSlaMinutes()).isEqualTo(2880);
    }
}
