package com.igarciamen.interests.repository;

import com.igarciamen.interests.model.ExpressionOfInterest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpressionOfInterestRepository extends JpaRepository<ExpressionOfInterest, Long> {

    // For the promoter/admin: "who is interested in this project".
    List<ExpressionOfInterest> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    // For the investor: "my expressions of interest".
    List<ExpressionOfInterest> findByInvestorIdOrderByCreatedAtDesc(Long investorId);

    boolean existsByProjectIdAndInvestorId(Long projectId, Long investorId);

    Optional<ExpressionOfInterest> findByProjectIdAndInvestorId(Long projectId, Long investorId);
}
