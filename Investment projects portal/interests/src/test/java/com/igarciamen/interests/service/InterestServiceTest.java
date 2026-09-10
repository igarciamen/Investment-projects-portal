package com.igarciamen.interests.service;

import com.igarciamen.interests.model.ExpressionOfInterest;
import com.igarciamen.interests.payloads.request.CreateInterestRequest;
import com.igarciamen.interests.repository.ExpressionOfInterestRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterestServiceTest {

    @Mock private ExpressionOfInterestRepository interestRepo;
    @Mock private ProjectClient projectClient;

    @InjectMocks private InterestService interestService;

    @Test
    void create_savesTheInterestWhenTheProjectIsApproved() {
        doNothing().when(projectClient).verifyApprovedOrThrow(1L);
        when(interestRepo.existsByProjectIdAndInvestorId(1L, 20L)).thenReturn(false);
        when(interestRepo.save(any(ExpressionOfInterest.class))).thenAnswer(inv -> {
            ExpressionOfInterest e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        var result = interestService.create(20L, new CreateInterestRequest(1L, "Interested in this project"));

        assertEquals(1L, result.getProjectId());
        assertEquals(20L, result.getInvestorId());
        System.out.println("=== create: interest saved for an approved project ===");
    }

    @Test
    void create_throws404WhenTheProjectIsNotApproved() {
        doThrow(new EntityNotFoundException("not found")).when(projectClient).verifyApprovedOrThrow(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interestService.create(20L, new CreateInterestRequest(1L, "message")));

        assertEquals(404, ex.getStatusCode().value());
        verify(interestRepo, never()).save(any());
    }

    @Test
    void create_throws409OnADuplicateExpressionOfInterest() {
        doNothing().when(projectClient).verifyApprovedOrThrow(1L);
        when(interestRepo.existsByProjectIdAndInvestorId(1L, 20L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interestService.create(20L, new CreateInterestRequest(1L, "message")));

        assertEquals(409, ex.getStatusCode().value());
        verify(interestRepo, never()).save(any());
        System.out.println("=== create: 409 when the investor already expressed interest ===");
    }

    @Test
    void listByProject_verifiesAccessBeforeListing() {
        when(interestRepo.findByProjectIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(new ExpressionOfInterest(1L, 20L, "msg")));

        var result = interestService.listByProject(1L);

        assertEquals(1, result.size());
        verify(projectClient).verifyAccessOrThrow(1L);
    }

    @Test
    void listByInvestor_returnsOnlyTheirOwnExpressions() {
        when(interestRepo.findByInvestorIdOrderByCreatedAtDesc(20L))
                .thenReturn(List.of(new ExpressionOfInterest(1L, 20L, "msg")));

        var result = interestService.listByInvestor(20L);

        assertEquals(1, result.size());
        verifyNoInteractions(projectClient);
    }
}
