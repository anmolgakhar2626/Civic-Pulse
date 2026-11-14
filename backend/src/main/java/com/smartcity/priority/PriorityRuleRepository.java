package com.smartcity.priority;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PriorityRuleRepository extends JpaRepository<PriorityRule, UUID> {
    Optional<PriorityRule> findByRuleId(String ruleId);
    List<PriorityRule> findByEnabledTrue();
    List<PriorityRule> findByIncidentTypeAndEnabledTrue(String incidentType);
    List<PriorityRule> findByTier(PriorityTier tier);
}
