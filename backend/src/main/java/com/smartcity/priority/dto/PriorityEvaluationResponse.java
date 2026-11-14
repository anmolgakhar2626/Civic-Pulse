package com.smartcity.priority.dto;

import com.smartcity.priority.PriorityResult;
import com.smartcity.priority.PriorityTier;

import java.time.Instant;
import java.util.List;

public class PriorityEvaluationResponse {

    private PriorityTier tier;
    private String priorityLabel;
    private List<String> reason;
    private String matchedRule;
    private String recommendedSlaCategory;
    private Integer slaMinutes;
    private Instant evaluatedAt;
    private boolean manualClassificationRequired;

    public PriorityEvaluationResponse() {}

    public static PriorityEvaluationResponse fromResult(PriorityResult result) {
        if (result == null) {
            return null;
        }
        PriorityEvaluationResponse response = new PriorityEvaluationResponse();
        response.setTier(result.getTier());
        response.setPriorityLabel(result.getPriorityLabel());
        response.setReason(result.getReason());
        response.setMatchedRule(result.getMatchedRule());
        response.setRecommendedSlaCategory(result.getRecommendedSlaCategory());
        response.setSlaMinutes(result.getSlaMinutes());
        response.setEvaluatedAt(result.getEvaluatedAt());
        response.setManualClassificationRequired(result.isManualClassificationRequired());
        return response;
    }

    public PriorityTier getTier() { return tier; }
    public void setTier(PriorityTier tier) { this.tier = tier; }

    @com.fasterxml.jackson.annotation.JsonProperty("priorityTier")
    public PriorityTier getPriorityTier() { return tier; }

    public String getPriorityLabel() { return priorityLabel; }
    public void setPriorityLabel(String priorityLabel) { this.priorityLabel = priorityLabel; }

    public List<String> getReason() { return reason; }
    public void setReason(List<String> reason) { this.reason = reason; }

    public String getMatchedRule() { return matchedRule; }
    public void setMatchedRule(String matchedRule) { this.matchedRule = matchedRule; }

    public String getRecommendedSlaCategory() { return recommendedSlaCategory; }
    public void setRecommendedSlaCategory(String recommendedSlaCategory) { this.recommendedSlaCategory = recommendedSlaCategory; }

    public Integer getSlaMinutes() { return slaMinutes; }
    public void setSlaMinutes(Integer slaMinutes) { this.slaMinutes = slaMinutes; }

    public Instant getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }

    public boolean isManualClassificationRequired() { return manualClassificationRequired; }
    public void setManualClassificationRequired(boolean manualClassificationRequired) { this.manualClassificationRequired = manualClassificationRequired; }
}
