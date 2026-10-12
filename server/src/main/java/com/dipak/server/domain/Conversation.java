package com.dipak.server.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "conversations",
       indexes = @Index(name = "idx_conv_direct_key", columnList = "direct_key", unique = true))
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String type; // "DIRECT" or "GROUP"

    @Column(length = 200)
    private String title;

    @Column(name = "direct_key", unique = true, length = 64)
    private String directKey;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDirectKey() { return directKey; }
    public void setDirectKey(String directKey) { this.directKey = directKey; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
