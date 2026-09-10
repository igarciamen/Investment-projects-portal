package com.igarciamen.messages.repository;

import com.igarciamen.messages.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :cid AND m.senderId <> :requesterId AND m.readAt IS NULL")
    long countUnread(@Param("cid") Long conversationId, @Param("requesterId") Long requesterId);

    @Modifying
    @Query("UPDATE Message m SET m.readAt = :now WHERE m.conversation.id = :cid AND m.senderId <> :requesterId AND m.readAt IS NULL")
    int markAsRead(@Param("cid") Long conversationId, @Param("requesterId") Long requesterId, @Param("now") LocalDateTime now);
}
