package com.example.springai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Boot Application Entry Point for Spring AI RAG Chatbot.
 * 
 * Annotations:
 * @SpringBootApplication: Combines three annotations:
 *    1. @Configuration: Marks as Spring configuration class
 *    2. @ComponentScan: Scans for @Component, @Service, @Repository, @Controller classes
 *    3. @EnableAutoConfiguration: Enables Spring Boot auto-configuration
 *       - Auto-detects and configures Spring AI components
 *       - Registers ChatModel, EmbeddingModel, VectorStore beans
 *       - Loads configuration from application.yml
 * 
 * @EnableScheduling: Enables scheduled tasks (e.g., cleanup jobs)
 * 
 * Spring Boot Auto-Configuration:
 * When you add spring-ai-*-spring-boot-autoconfigure dependencies:
 * - ChatModel beans are auto-created (e.g., AnthropicChatModel)
 * - VectorStore can be auto-configured (e.g., PgVectorStore)
 * - Properties are loaded from application.yml with spring.ai.* prefixes
 * - Conditional beans (@ConditionalOnProperty) enable/disable features
 * 
 * Key Dependencies:
 * - spring-ai-starter-model-anthropic: Provides ChatModel for Claude
 * - spring-ai-starter-model-ollama: Provides EmbeddingModel
 * - spring-ai-starter-vector-store-pgvector: PgVector VectorStore implementation
 * - spring-boot-starter-web: REST endpoints
 * - spring-boot-starter-data-jpa: Database persistence
 * 
 * Application Flow:
 * 1. Spring container starts, loads application.yml
 * 2. Auto-configuration creates ChatModel from API key
 * 3. ChatClient bean is created in SpringAiConfig
 * 4. VectorStore bean is auto-configured (PgVector)
 * 5. Repository beans are created
 * 6. Service beans are created (ChatService)
 * 7. Controller beans are created (ChatController)
 * 8. Application server starts (default: Tomcat on port 8080)
 * 9. REST endpoints become available
 */
@SpringBootApplication
@EnableScheduling
@Slf4j
public class SpringAiRagChatbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiRagChatbotApplication.class, args);
        log.info("========================================");
        log.info("Spring AI RAG Chatbot Application Started");
        log.info("Available endpoints:");
        log.info("  POST /api/chat - Send message");
        log.info("  GET /api/chat/{conversationId} - Get history");
        log.info("========================================");
    }
}
