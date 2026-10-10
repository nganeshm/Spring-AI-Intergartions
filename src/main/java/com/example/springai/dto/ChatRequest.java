package com.example.springai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.List;

/**
 * ChatRequest DTO for incoming chat messages.
 * 
 * Annotations:
 * @NotBlank: Validates that string is not null or empty (trims whitespace)
 * @NotEmpty: Validates that collection is not empty
 * @Valid: Cascades validation to nested objects
 * @Data: Lombok generates getters, setters, equals, hashCode, toString
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {

    @NotBlank(message = "Conversation ID cannot be blank")
    private String conversationId;

    @NotBlank(message = "User message cannot be blank")
    private String userMessage;

    @Builder.Default
    private boolean useRag = true;  // Enable/disable RAG for this request

    @Builder.Default
    private Double temperatureOverride = null;  // Optional: override default temperature

    @Builder.Default
    private List<String> documentIds = List.of();  // Filter RAG to specific documents

    private String systemPrompt;  // Custom system prompt for this conversation
}
