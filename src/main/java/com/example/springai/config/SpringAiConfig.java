package com.example.springai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring AI Configuration: Defines beans for ChatClient and other AI components.
 * 
 * Annotations:
 * @Configuration: Marks as Spring configuration class (alternative to XML config)
 * @Bean: Registers methods as bean definitions in Spring context
 * 
 * Spring AI Components:
 * 1. ChatClient: High-level API for chat interactions
 *    - Replaces direct ChatModel usage
 *    - Supports fluent API and method chaining
 *    - Auto-configured from application.yml
 *    - Supports advisors for RAG pipeline
 *    - Handles message formatting and response parsing
 * 
 * 2. VectorStore: Abstraction for vector database operations
 *    - Stores embeddings for RAG
 *    - Supports similarity search
 *    - Auto-configured by spring-ai-starter-vector-store-pgvector
 *      (backed by the PostgreSQL "vector" extension) based on
 *      spring.ai.vectorstore.pgvector.* properties in application.yml.
 *      No manual @Bean definition is required here.
 * 
 * 3. EmbeddingModel: Converts text to vectors
 *    - Auto-configured by spring-ai-starter-model-ollama
 *    - Used by VectorStore for similarity search
 * 
 * Configuration Strategy:
 * - Auto-configuration: Handled by spring-ai-*-starter-* dependencies
 * - Manual beans: Override defaults or add custom logic
 */
@Configuration
@Slf4j
public class SpringAiConfig {

    /**
     * ChatClient Bean: High-level API for chat interactions.
     * 
     * ChatClient Advantages over direct ChatModel:
     * - Fluent builder API (.prompt().user().call().content())
     * - Automatic response parsing and formatting
     * - Built-in advisor support for RAG pipelines
     * - Message history management
     * - Streaming response support
     * - Error handling and retries
     * 
     * Configuration:
     * - ChatModel is auto-created from application.yml settings
     * - spring.ai.anthropic.api-key must be set in environment
     * - Model, temperature, max-tokens configured in application.yml
     * 
     * @param chatModel The underlying ChatModel (auto-configured)
     * @return ChatClient instance
     * 
     * Example Usage in Service:
     * chatClient.prompt()
     *    .user("What is Spring AI?")
     *    .call()
     *    .content();
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        log.info("Initializing ChatClient with model: {}", chatModel.getClass().getSimpleName());

        return ChatClient.builder(chatModel).build();
    }
}
