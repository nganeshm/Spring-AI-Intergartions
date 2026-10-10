package com.example.springai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for ingesting raw text into the vector store.
 *
 * Used by POST /api/documents/text - splits the content into chunks,
 * embeds them, and writes them to the VectorStore (pgvector).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentIngestRequest {

    @NotBlank(message = "Content cannot be blank")
    private String content;

    @Builder.Default
    private String source = "manual-entry";
}
