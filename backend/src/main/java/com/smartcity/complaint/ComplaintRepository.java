package com.smartcity.complaint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {
    Optional<Complaint> findByComplaintNumber(String complaintNumber);
    List<Complaint> findByCitizenId(UUID citizenId);
    List<Complaint> findByStatus(String status);
    long countByStatus(String status);
}
