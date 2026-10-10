package com.example.springai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ChatResponse DTO for outgoing chat responses.
 * 
 * Annotations:
 * @JsonInclude: Controls serialization of null fields (NON_NULL excludes them)
 * @Data: Lombok generates getters, setters, equals, hashCode, toString
 * @AllArgsConstructor: Lombok generates all-args constructor
 * @NoArgsConstructor: Lombok generates no-args constructor
 * @Builder: Lombok generates builder pattern
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {

    private Long messageId;
    private String conversationId;
    private String userMessage;
    private String assistantMessage;
    private String model;
    private Integer tokensUsed;
    private LocalDateTime timestamp;

    @Builder.Default
    private List<RelevantDocument> relevantDocuments = List.of();  // RAG sources

    private Double processingTimeMs;
    private String error;
    private boolean success;

    /**
     * Nested DTO for RAG source documents.
     * Shows which documents were used to generate the response.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RelevantDocument {
        private String documentId;
        private String title;
        private String content;
        private Double relevanceScore;  // Similarity score
        private String source;
    }
}
