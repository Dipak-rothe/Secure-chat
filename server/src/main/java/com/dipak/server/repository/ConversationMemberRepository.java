package com.dipak.server.repository;

import com.dipak.server.domain.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMember.ConversationMemberId> {
    List<ConversationMember> findByUserId(Long userId);
    List<ConversationMember> findByConversationId(Long conversationId);
    Optional<ConversationMember> findByConversationIdAndUserId(Long conversationId, Long userId);
}
