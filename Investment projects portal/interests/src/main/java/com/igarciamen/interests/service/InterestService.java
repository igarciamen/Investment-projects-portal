package com.igarciamen.interests.service;

import com.igarciamen.interests.model.ExpressionOfInterest;
import com.igarciamen.interests.payloads.request.CreateInterestRequest;
import com.igarciamen.interests.repository.ExpressionOfInterestRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class InterestService {

    private final ExpressionOfInterestRepository interestRepo;
    private final ProjectClient projectClient;

    public InterestService(ExpressionOfInterestRepository interestRepo, ProjectClient projectClient) {
        this.interestRepo = interestRepo;
        this.projectClient = projectClient;
    }

    // One investor can express interest in a given project only once -- a
    // second attempt is rejected with 409 rather than creating a duplicate
    // row (the unique constraint on the entity is the last line of defense,
    // this check gives a clean error message instead of a raw SQL exception).
    public ExpressionOfInterest create(Long investorId, CreateInterestRequest req) {
        try {
            projectClient.verifyApprovedOrThrow(req.getProjectId());
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Approved project not found: " + req.getProjectId());
        }

        if (interestRepo.existsByProjectIdAndInvestorId(req.getProjectId(), investorId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You have already expressed interest in this project");
        }

        ExpressionOfInterest interest = new ExpressionOfInterest(req.getProjectId(), investorId, req.getMessage());
        return interestRepo.save(interest);
    }

    // For the promoter/admin: who is interested in a given project. Access is
    // checked by delegating to projects' own GET /api/projects/{id} (owner or
    // admin), the same principle used throughout this platform (documents,
    // messages): the source of truth for "who owns this project" is projects,
    // never duplicated here.
    public List<ExpressionOfInterest> listByProject(Long projectId) {
        projectClient.verifyAccessOrThrow(projectId);
        return interestRepo.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    // For the investor: "my expressions of interest".
    public List<ExpressionOfInterest> listByInvestor(Long investorId) {
        return interestRepo.findByInvestorIdOrderByCreatedAtDesc(investorId);
    }
}
