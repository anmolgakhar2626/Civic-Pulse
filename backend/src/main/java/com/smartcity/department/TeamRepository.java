package com.smartcity.department;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    Optional<Team> findByCode(String code);

    @Query("SELECT t FROM Team t WHERE t.department.id = :departmentId")
    List<Team> findByDepartmentId(@Param("departmentId") UUID departmentId);

    @Query("SELECT t FROM Team t WHERE t.department.id = :departmentId AND t.active = true")
    List<Team> findByDepartmentIdAndActiveTrue(@Param("departmentId") UUID departmentId);

    @Query("SELECT t FROM Team t WHERE t.department.id = :departmentId AND t.status = :status")
    List<Team> findByDepartmentIdAndStatus(@Param("departmentId") UUID departmentId, @Param("status") String status);
}
