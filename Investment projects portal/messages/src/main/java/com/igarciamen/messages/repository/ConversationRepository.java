package com.igarciamen.messages.repository;

import com.igarciamen.messages.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByProjectIdAndInvestorId(Long projectId, Long investorId);

    // For the promoter/admin: every investor thread open on a given project.
    List<Conversation> findByProjectIdOrderByLastMessageAtDesc(Long projectId);
}
