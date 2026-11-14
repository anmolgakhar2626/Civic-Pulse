package com.smartcity.priority;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcity.priority.dto.EvaluatePriorityRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PriorityEngine {

    private static final Logger log = LoggerFactory.getLogger(PriorityEngine.class);
    private final ObjectMapper objectMapper;

    public PriorityEngine() {
        this.objectMapper = new ObjectMapper();
    }

    public PriorityEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public PriorityResult evaluate(EvaluatePriorityRequest request, List<PriorityRule> rules) {
        if (request == null) {
            return PriorityResult.manualReview(List.of("Null evaluation request received. Manual review required."));
        }

        if (rules == null || rules.isEmpty()) {
            return PriorityResult.manualReview(List.of(
                    "No configured priority rules available in system. Manual review required.",
                    "Incident type: " + (request.getIncidentType() != null ? request.getIncidentType() : "UNSPECIFIED")
            ));
        }

        List<MatchEvaluation> matches = new ArrayList<>();

        for (PriorityRule rule : rules) {
            if (rule == null || !Boolean.TRUE.equals(rule.getEnabled())) {
                continue; // Requirement 14: Disabled rules are skipped
            }

            MatchEvaluation evaluation = evaluateRule(rule, request);
            if (evaluation.isMatched()) {
                matches.add(evaluation);
            }
        }

        if (matches.isEmpty()) {
            // Requirement 15: No matching rule returns controlled manual classification result (not random)
            String incType = request.getIncidentType() != null ? request.getIncidentType() : "UNSPECIFIED";
            List<String> unclassifiedReasons = new ArrayList<>();
            unclassifiedReasons.add("No matching automated priority rule found for incident type: " + incType);
            if (request.getSeverity() != null) {
                unclassifiedReasons.add("Reported severity: " + request.getSeverity());
            }
            if (request.getPublicExposure() != null) {
                unclassifiedReasons.add("Reported exposure: " + request.getPublicExposure());
            }
            unclassifiedReasons.add("Automated priority assignment withheld. Manual operator classification required.");
            return PriorityResult.manualReview(unclassifiedReasons);
        }

        // Requirement 9 & 10: Sort matches with highest safety-critical tier first (CRITICAL > HIGH > MEDIUM > LOW)
        matches.sort((a, b) -> {
            int rankCompare = Integer.compare(
                    b.getRule().getTier().getPrecedenceRank(),
                    a.getRule().getTier().getPrecedenceRank()
            );
            if (rankCompare != 0) {
                return rankCompare;
            }
            // Tie-break 1: Safety-critical score
            int safetyCompare = Integer.compare(b.getSafetyScore(), a.getSafetyScore());
            if (safetyCompare != 0) {
                return safetyCompare;
            }
            // Tie-break 2: Number of matching conditions (more specific rule wins)
            return Integer.compare(b.getConditionSpecificity(), a.getConditionSpecificity());
        });

        MatchEvaluation winner = matches.get(0);
        PriorityRule winningRule = winner.getRule();

        // Compile comprehensive, explainable reasons
        List<String> combinedReasons = new ArrayList<>(winner.getReasons());

        // Document if other rules matched and why this tier won
        if (matches.size() > 1) {
            MatchEvaluation runnerUp = matches.get(1);
            if (winningRule.getTier().getPrecedenceRank() > runnerUp.getRule().getTier().getPrecedenceRank()) {
                combinedReasons.add(String.format("Tier escalated to %s due to safety precedence over %s rule (%s)",
                        winningRule.getTier(), runnerUp.getRule().getTier(), runnerUp.getRule().getRuleCode()));
            }
        }

        log.info("Priority evaluated: rule={}, tier={}, reasonsCount={}",
                winningRule.getRuleCode(), winningRule.getTier(), combinedReasons.size());

        return new PriorityResult(
                winningRule.getTier(),
                combinedReasons,
                winningRule.getRuleCode(),
                winningRule.getName(),
                winningRule.getSlaMinutes()
        );
    }

    private MatchEvaluation evaluateRule(PriorityRule rule, EvaluatePriorityRequest request) {
        String ruleType = rule.getIncidentType();
        String reqType = request.getIncidentType();

        Map<String, Object> conditions = parseConditions(rule.getConditionsJson());
        List<String> reasons = new ArrayList<>();
        int specificity = conditions.size();
        int safetyScore = 0;

        // Check if universal safety rule that applies regardless of incidentType
        boolean isUniversalSafetyRule = isUniversalSafetyRule(rule, conditions);

        if (!isUniversalSafetyRule) {
            if (ruleType != null && !ruleType.equalsIgnoreCase("*")) {
                if (reqType == null || !matchesType(ruleType, reqType)) {
                    return MatchEvaluation.notMatched();
                }
            }
        }

        // Evaluate specific conditions
        if (conditions.containsKey("exposedLiveWire")) {
            boolean required = Boolean.TRUE.equals(conditions.get("exposedLiveWire"));
            boolean actual = request.isExposedLiveWire() ||
                    "EXPOSED_LIVE_WIRE".equalsIgnoreCase(request.getSafetyImpact()) ||
                    containsIgnoreCase(request.getDescription(), "live wire", "exposed wire", "electrocution");
            if (required != actual) {
                return MatchEvaluation.notMatched();
            }
            if (actual) {
                safetyScore += 100;
                reasons.add("Live exposed electrical wire hazard confirmed");
            }
        }

        if (conditions.containsKey("transformerSpark")) {
            boolean required = Boolean.TRUE.equals(conditions.get("transformerSpark"));
            boolean actual = request.isTransformerSpark() ||
                    containsIgnoreCase(request.getDescription(), "transformer", "sparking", "electrical spark");
            if (required != actual) {
                return MatchEvaluation.notMatched();
            }
            if (actual) {
                safetyScore += 90;
                reasons.add("High-voltage transformer spark / fire hazard");
            }
        }

        if (conditions.containsKey("submergedUnderpass")) {
            boolean required = Boolean.TRUE.equals(conditions.get("submergedUnderpass"));
            boolean actual = request.isSubmergedUnderpass() ||
                    "SUBMERGED_UNDERPASS".equalsIgnoreCase(request.getSafetyImpact()) ||
                    containsIgnoreCase(request.getDescription(), "submerged", "underpass", "flash flood");
            if (required != actual) {
                return MatchEvaluation.notMatched();
            }
            if (actual) {
                safetyScore += 95;
                reasons.add("Sudden deep flash flood submerging underpass corridor");
            }
        }

        if (conditions.containsKey("busyRoad")) {
            boolean required = Boolean.TRUE.equals(conditions.get("busyRoad"));
            boolean actual = request.isBusyRoad() ||
                    request.getPublicExposure() == PublicExposure.VERY_HIGH ||
                    request.getPublicExposure() == PublicExposure.HIGH ||
                    "HIGH".equalsIgnoreCase(request.getInfrastructureImportance());
            if (required != actual) {
                return MatchEvaluation.notMatched();
            }
            if (actual) {
                safetyScore += 80;
                reasons.add("Hazard located on high-density arterial roadway");
            }
        }

        if (conditions.containsKey("blockingEmergencyAccess")) {
            boolean required = Boolean.TRUE.equals(conditions.get("blockingEmergencyAccess"));
            boolean actual = request.isBlockingEmergencyAccess() ||
                    "BLOCKING_EMERGENCY_ACCESS".equalsIgnoreCase(request.getSafetyImpact()) ||
                    containsIgnoreCase(request.getDescription(), "emergency access", "ambulance blocked", "hospital gate");
            if (required != actual) {
                return MatchEvaluation.notMatched();
            }
            if (actual) {
                safetyScore += 85;
                reasons.add("Obstruction blocking emergency access route");
            }
        }

        if (conditions.containsKey("majorJunction")) {
            boolean required = Boolean.TRUE.equals(conditions.get("majorJunction"));
            boolean actual = request.isMajorJunction() ||
                    "HIGH".equalsIgnoreCase(request.getInfrastructureImportance()) ||
                    "CRITICAL".equalsIgnoreCase(request.getInfrastructureImportance());
            if (required != actual) {
                return MatchEvaluation.notMatched();
            }
            if (actual) {
                reasons.add("Major high-traffic junction failure");
            } else {
                reasons.add("Minor intersection failure");
            }
        }

        if (conditions.containsKey("roadCaveIn")) {
            boolean required = Boolean.TRUE.equals(conditions.get("roadCaveIn"));
            boolean actual = request.isRoadCaveIn() ||
                    "ROAD_CAVE_IN".equalsIgnoreCase(request.getSafetyImpact()) ||
                    containsIgnoreCase(request.getDescription(), "cave in", "sinkhole");
            if (required != actual) {
                return MatchEvaluation.notMatched();
            }
            if (actual) {
                safetyScore += 70;
                reasons.add("Severe roadway structural cave-in / sinkhole detected");
            }
        }

        if (conditions.containsKey("severity")) {
            String requiredSev = String.valueOf(conditions.get("severity")).toUpperCase();
            if (request.getSeverity() == null) {
                return MatchEvaluation.notMatched();
            }
            if ("EXTREME".equals(requiredSev) && request.getSeverity() != Severity.EXTREME) {
                return MatchEvaluation.notMatched();
            }
            if ("HIGH".equals(requiredSev) && (request.getSeverity() != Severity.HIGH && request.getSeverity() != Severity.EXTREME)) {
                return MatchEvaluation.notMatched();
            }
            reasons.add("Assessed incident severity: " + request.getSeverity());
        }

        if (conditions.containsKey("minPm25")) {
            double threshold = Double.parseDouble(String.valueOf(conditions.get("minPm25")));
            if (request.getPm25() == null || request.getPm25() < threshold) {
                return MatchEvaluation.notMatched();
            }
            reasons.add(String.format("PM2.5 particulate level is %.1f µg/m³ (exceeds %.0f threshold)", request.getPm25(), threshold));
        }

        if (conditions.containsKey("minFillPercentage")) {
            double threshold = Double.parseDouble(String.valueOf(conditions.get("minFillPercentage")));
            if (request.getWasteBinFillPercentage() == null || request.getWasteBinFillPercentage() < threshold) {
                return MatchEvaluation.notMatched();
            }
            reasons.add(String.format("Smart waste bin fill level is %.0f%% (exceeds %.0f%% threshold)", request.getWasteBinFillPercentage(), threshold));
        }

        // Add base template reasons from rule
        List<String> templateReasons = parseTemplateReasons(rule.getReasonTemplate());
        if (!templateReasons.isEmpty()) {
            reasons.addAll(0, templateReasons);
        } else if (reasons.isEmpty()) {
            reasons.add(rule.getName() != null ? rule.getName() : rule.getRuleCode());
        }

        // Contextual exposure addition
        if (request.getPublicExposure() == PublicExposure.VERY_HIGH || request.getPublicExposure() == PublicExposure.HIGH) {
            reasons.add("High public exposure zone (" + request.getPublicExposure() + ")");
        }

        return MatchEvaluation.matched(rule, reasons, specificity, safetyScore);
    }

    private boolean isUniversalSafetyRule(PriorityRule rule, Map<String, Object> conditions) {
        String code = rule.getRuleCode();
        if (code == null) return false;
        return code.equals("LIVE_ELECTRICAL_WIRE") ||
                code.equals("WASTE_OBSTRUCTION_EMERGENCY") ||
                code.equals("SUBMERGED_UNDERPASS") ||
                Boolean.TRUE.equals(conditions.get("exposedLiveWire")) ||
                Boolean.TRUE.equals(conditions.get("blockingEmergencyAccess"));
    }

    private boolean matchesType(String ruleType, String reqType) {
        if (ruleType.equalsIgnoreCase(reqType)) {
            return true;
        }
        // Normalize underscores and synonyms
        String rNorm = ruleType.replace("_", "").toLowerCase();
        String qNorm = reqType.replace("_", "").toLowerCase();
        if (rNorm.contains(qNorm) || qNorm.contains(rNorm)) {
            return true;
        }
        // Specific semantic synonyms
        if ((rNorm.contains("pothole") || rNorm.contains("crack")) && qNorm.contains("road")) return true;
        if (rNorm.contains("collision") && qNorm.contains("accident")) return true;
        if (rNorm.contains("accident") && qNorm.contains("collision")) return true;
        if (rNorm.contains("pipe") && qNorm.contains("water")) return true;
        if (rNorm.contains("leak") && qNorm.contains("pipe")) return true;
        if ((rNorm.contains("air") || rNorm.contains("smog") || rNorm.contains("pollution")) &&
            (qNorm.contains("air") || qNorm.contains("smog") || qNorm.contains("pollution"))) return true;
        if (rNorm.contains("air") && qNorm.contains("smog")) return true;
        if (rNorm.contains("smog") && qNorm.contains("air")) return true;
        if (rNorm.contains("waste") && qNorm.contains("dumping")) return true;
        if (rNorm.contains("dumping") && qNorm.contains("waste")) return true;
        if (rNorm.contains("graffiti") && qNorm.contains("vandalism")) return true;
        if (rNorm.contains("vandalism") && qNorm.contains("graffiti")) return true;
        return false;
    }

    private Map<String, Object> parseConditions(String json) {
        if (json == null || json.trim().isEmpty() || json.trim().equals("{}")) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse conditions JSON: {}", json);
            return Collections.emptyMap();
        }
    }

    private List<String> parseTemplateReasons(String template) {
        if (template == null || template.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            if (template.trim().startsWith("[")) {
                return objectMapper.readValue(template, new TypeReference<List<String>>() {});
            } else {
                return List.of(template.trim());
            }
        } catch (Exception e) {
            return List.of(template.trim());
        }
    }

    private boolean containsIgnoreCase(String text, String... candidates) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        for (String c : candidates) {
            if (lower.contains(c.toLowerCase())) return true;
        }
        return false;
    }

    private static class MatchEvaluation {
        private final boolean matched;
        private final PriorityRule rule;
        private final List<String> reasons;
        private final int conditionSpecificity;
        private final int safetyScore;

        private MatchEvaluation(boolean matched, PriorityRule rule, List<String> reasons, int conditionSpecificity, int safetyScore) {
            this.matched = matched;
            this.rule = rule;
            this.reasons = reasons;
            this.conditionSpecificity = conditionSpecificity;
            this.safetyScore = safetyScore;
        }

        public static MatchEvaluation matched(PriorityRule rule, List<String> reasons, int conditionSpecificity, int safetyScore) {
            return new MatchEvaluation(true, rule, reasons, conditionSpecificity, safetyScore);
        }

        public static MatchEvaluation notMatched() {
            return new MatchEvaluation(false, null, Collections.emptyList(), 0, 0);
        }

        public boolean isMatched() { return matched; }
        public PriorityRule getRule() { return rule; }
        public List<String> getReasons() { return reasons; }
        public int getConditionSpecificity() { return conditionSpecificity; }
        public int getSafetyScore() { return safetyScore; }
    }
}
