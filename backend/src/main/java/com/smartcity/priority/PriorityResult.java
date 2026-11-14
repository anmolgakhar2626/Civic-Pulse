package com.smartcity.priority;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PriorityResult {

    private PriorityTier tier;
    private String priorityLabel;
    private List<String> reason;
    private String matchedRule;
    private String matchedRuleName;
    private String recommendedSlaCategory;
    private Integer slaMinutes;
    private Instant evaluatedAt;
    private boolean manualClassificationRequired;

    public PriorityResult() {
        this.reason = new ArrayList<>();
        this.evaluatedAt = Instant.now();
    }

    public PriorityResult(PriorityTier tier, List<String> reason, String matchedRule, String matchedRuleName, Integer slaMinutes) {
        this.tier = tier;
        this.priorityLabel = tier != null ? tier.name() : "MANUAL_REVIEW";
        this.reason = reason != null ? new ArrayList<>(reason) : new ArrayList<>();
        this.matchedRule = matchedRule;
        this.matchedRuleName = matchedRuleName;
        this.slaMinutes = slaMinutes != null ? slaMinutes : (tier != null ? tier.getTargetSlaMinutes() : null);
        this.recommendedSlaCategory = tier != null ? tier.getTargetSlaCategory() : "MANUAL_DISPATCH";
        this.evaluatedAt = Instant.now();
        this.manualClassificationRequired = false;
    }

    public static PriorityResult manualReview(List<String> reason) {
        PriorityResult result = new PriorityResult();
        result.setTier(null);
        result.setPriorityLabel("MANUAL_REVIEW");
        result.setReason(reason != null ? reason : List.of("No automated priority rule matched. Manual operator classification required."));
        result.setMatchedRule(null);
        result.setMatchedRuleName("NO_MATCHING_RULE");
        result.setRecommendedSlaCategory("MANUAL_DISPATCH");
        result.setSlaMinutes(null);
        result.setEvaluatedAt(Instant.now());
        result.setManualClassificationRequired(true);
        return result;
    }

    public PriorityTier getTier() { return tier; }
    public void setTier(PriorityTier tier) {
        this.tier = tier;
        if (tier != null) {
            this.priorityLabel = tier.name();
            this.recommendedSlaCategory = tier.getTargetSlaCategory();
            if (this.slaMinutes == null) {
                this.slaMinutes = tier.getTargetSlaMinutes();
            }
        }
    }

    public String getPriorityLabel() { return priorityLabel; }
    public void setPriorityLabel(String priorityLabel) { this.priorityLabel = priorityLabel; }

    public List<String> getReason() { return reason; }
    public void setReason(List<String> reason) { this.reason = reason; }

    public String getMatchedRule() { return matchedRule; }
    public void setMatchedRule(String matchedRule) { this.matchedRule = matchedRule; }

    public String getMatchedRuleName() { return matchedRuleName; }
    public void setMatchedRuleName(String matchedRuleName) { this.matchedRuleName = matchedRuleName; }

    public String getRecommendedSlaCategory() { return recommendedSlaCategory; }
    public void setRecommendedSlaCategory(String recommendedSlaCategory) { this.recommendedSlaCategory = recommendedSlaCategory; }

    public Integer getSlaMinutes() { return slaMinutes; }
    public void setSlaMinutes(Integer slaMinutes) { this.slaMinutes = slaMinutes; }

    public Instant getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }

    public boolean isManualClassificationRequired() { return manualClassificationRequired; }
    public void setManualClassificationRequired(boolean manualClassificationRequired) { this.manualClassificationRequired = manualClassificationRequired; }
}
