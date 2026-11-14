package com.smartcity.assignment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentRuleRepository extends JpaRepository<AssignmentRule, UUID> {

    Optional<AssignmentRule> findByIncidentTypeIgnoreCaseAndActiveTrue(String incidentType);

    List<AssignmentRule> findByActiveTrue();

    List<AssignmentRule> findByDepartmentCode(String departmentCode);
}
