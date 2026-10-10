package com.example.springai.dto;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Message entity for storing chat history.
 * 
 * Annotations:
 * @Entity: Marks this class as a JPA entity mapped to database table
 * @Table: Specifies the table name in the database
 * @Id: Marks the primary key field
 * @GeneratedValue: Auto-generates unique IDs (IDENTITY strategy for PostgreSQL)
 * @Column: Customizes column properties (nullable, length, unique)
 * @Data: Lombok annotation generating getters, setters, equals, hashCode, toString
 * @NoArgsConstructor: Lombok generates no-args constructor (required by JPA)
 * @AllArgsConstructor: Lombok generates all-args constructor
 * @Builder: Lombok generates builder pattern
 */
@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_conversation_id", columnList = "conversation_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private String conversationId;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageRole role;  // USER or ASSISTANT

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "tokens_used")
    private Integer tokensUsed;
//   Spring AI stores embeddings in its own vector table.
//    @Column(name = "embedding", columnDefinition = "vector(768)")
//    private String embedding;  // For vector search

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum MessageRole {
        USER, ASSISTANT, SYSTEM
    }
}
