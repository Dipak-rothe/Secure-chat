package com.dipak.server.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "conversation_members")
@IdClass(ConversationMember.ConversationMemberId.class)
public class ConversationMember {

    @Id
    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 10)
    private String role = "MEMBER";

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @PrePersist
    private void prePersist() {
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
    }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }

    public Long getLastReadMessageId() { return lastReadMessageId; }
    public void setLastReadMessageId(Long lastReadMessageId) { this.lastReadMessageId = lastReadMessageId; }

    public static class ConversationMemberId implements Serializable {
        private Long conversationId;
        private Long userId;

        public ConversationMemberId() {}
        public ConversationMemberId(Long conversationId, Long userId) {
            this.conversationId = conversationId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConversationMemberId that = (ConversationMemberId) o;
            return Objects.equals(conversationId, that.conversationId) && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(conversationId, userId);
        }
    }
}
