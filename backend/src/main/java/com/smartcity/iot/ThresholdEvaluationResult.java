package com.smartcity.iot;

import com.smartcity.priority.PriorityTier;
import com.smartcity.priority.Severity;

import java.util.HashMap;
import java.util.Map;

public class ThresholdEvaluationResult {

    private final boolean breachDetected;
    private final String ruleName;
    private final String incidentType;
    private final PriorityTier priorityTier;
    private final Severity severity;
    private final String title;
    private final String description;
    private final Map<String, Object> context;

    private ThresholdEvaluationResult(
            boolean breachDetected,
            String ruleName,
            String incidentType,
            PriorityTier priorityTier,
            Severity severity,
            String title,
            String description,
            Map<String, Object> context
    ) {
        this.breachDetected = breachDetected;
        this.ruleName = ruleName;
        this.incidentType = incidentType;
        this.priorityTier = priorityTier;
        this.severity = severity;
        this.title = title;
        this.description = description;
        this.context = context != null ? context : new HashMap<>();
    }

    public static ThresholdEvaluationResult noBreach() {
        return new ThresholdEvaluationResult(false, null, null, null, null, null, null, null);
    }

    public static ThresholdEvaluationResult breach(
            String ruleName,
            String incidentType,
            PriorityTier priorityTier,
            Severity severity,
            String title,
            String description,
            Map<String, Object> context
    ) {
        return new ThresholdEvaluationResult(true, ruleName, incidentType, priorityTier, severity, title, description, context);
    }

    public boolean isBreachDetected() { return breachDetected; }
    public String getRuleName() { return ruleName; }
    public String getIncidentType() { return incidentType; }
    public PriorityTier getPriorityTier() { return priorityTier; }
    public Severity getSeverity() { return severity; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Map<String, Object> getContext() { return context; }
}
