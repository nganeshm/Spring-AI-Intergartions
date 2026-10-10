package com.example.springai.service;

import com.example.springai.dto.ChatRequest;
import com.example.springai.dto.ChatResponse;
import com.example.springai.dto.Message;
import com.example.springai.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ChatService: Core business logic for AI chatbot.
 * 
 * Annotations:
 * @Service: Spring stereotype annotation, marks as service layer bean
 * @RequiredArgsConstructor: Lombok generates constructor for all final fields (dependency injection)
 * @Slf4j: Lombok generates logger (prevents boilerplate logging setup)
 * @Transactional: Marks entire service as transactional, all methods participate in transactions
 * 
 * Spring AI Components:
 * - ChatClient: High-level API for interacting with AI models (replaces direct ChatModel usage)
 * - VectorStore: Abstraction for storing and searching embeddings
 * 
 * Dependency Injection:
 * All dependencies are final fields, injected via constructor by Spring.
 * No @Autowired needed with @RequiredArgsConstructor.
 * 
 * Note: org.springframework.ai.chat.model.ChatResponse is referenced by its
 * fully-qualified name below because this class also defines/imports the
 * project's own com.example.springai.dto.ChatResponse DTO, and both share
 * the simple name "ChatResponse".
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatService {

    private static final int TOP_K = 5;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final MessageRepository messageRepository;

    /**
     * Process a chat request: Query AI model with optional RAG context.
     * 
     * @param request ChatRequest containing user message and options
     * @return ChatResponse with AI response and metadata
     * 
     * Flow:
     * 1. Retrieve conversation history
     * 2. If RAG enabled: search vector store for relevant documents
     * 3. Build prompt with RAG context
     * 4. Call Claude API via ChatClient
     * 5. Save messages to database
     * 6. Return response with sources
     */
    public ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Processing chat request for conversation: {}", request.getConversationId());

            // Retrieve conversation history
            List<Message> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(
                    request.getConversationId()
            );

            // Build prompt with RAG context
            String augmentedPrompt = buildPrompt(request, history);

            // Call Claude API
            org.springframework.ai.chat.model.ChatResponse aiResponse = callChatModel(augmentedPrompt, request);

            // Extract assistant message
            String assistantMessage = aiResponse.getResult().getOutput().getText();

            // Save messages to database
            saveMessages(request, assistantMessage);

            // Retrieve relevant documents for response
            List<ChatResponse.RelevantDocument> sources = request.isUseRag() 
                ? searchRelevantDocuments(request.getUserMessage())
                : List.of();

            long processingTime = System.currentTimeMillis() - startTime;

            return ChatResponse.builder()
                    .conversationId(request.getConversationId())
                    .userMessage(request.getUserMessage())
                    .assistantMessage(assistantMessage)
                    .model("nomic-embed-text")
                    .relevantDocuments(sources)
                    .processingTimeMs((double) processingTime)
                    .timestamp(LocalDateTime.now())
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Error processing chat request", e);
            return ChatResponse.builder()
                    .conversationId(request.getConversationId())
                    .userMessage(request.getUserMessage())
                    .error(e.getMessage())
                    .success(false)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Build prompt with RAG context and conversation history.
     * 
     * Prompt engineering strategy:
     * 1. System prompt: Sets AI behavior/role
     * 2. RAG context: Relevant documents if enabled
     * 3. Conversation history: Previous messages for context
     * 4. User message: Current query
     */
    private String buildPrompt(ChatRequest request, List<Message> history) {
        StringBuilder prompt = new StringBuilder();

        // System prompt
        if (request.getSystemPrompt() != null) {
            prompt.append("System: ").append(request.getSystemPrompt()).append("\n\n");
        } else {
            prompt.append("You are a helpful AI assistant. Answer questions accurately and concisely.\n\n");
        }

        // RAG context
        if (request.isUseRag()) {
            List<Document> relevantDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(request.getUserMessage())
                            .topK(TOP_K)
                            .build()
            );

            if (!relevantDocs.isEmpty()) {
                prompt.append("Context from documents:\n");
                relevantDocs.forEach(doc -> 
                    prompt.append("- ").append(doc.getText()).append("\n")
                );
                prompt.append("\n");
            }
        }

        // Conversation history (last 5 exchanges)
        if (!history.isEmpty()) {
            prompt.append("Previous conversation:\n");
            history.stream()
                    .skip(Math.max(0, history.size() - 10))  // Last 10 messages
                    .forEach(msg -> 
                        prompt.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n")
                    );
            prompt.append("\n");
        }

        // Current user message
        prompt.append("User: ").append(request.getUserMessage());

        return prompt.toString();
    }

    /**
     * Call Claude API using Spring AI ChatClient.
     * 
     * ChatClient provides:
     * - Fluent API for building requests
     * - Automatic model configuration from application.yml
     * - Message handling (UserMessage, SystemMessage, AssistantMessage)
     * - Temperature, top-p, max-tokens overrides
     * 
     * @param prompt The complete prompt including context
     * @param request Original request (for overrides)
     * @return ChatResponse from AI model
     */
    private org.springframework.ai.chat.model.ChatResponse callChatModel(String prompt, ChatRequest request) {
        log.debug("Calling Claude API with prompt length: {}", prompt.length());

        // Apply temperature override if provided
        if (request.getTemperatureOverride() != null) {
            // Note: Temperature override would need ChatClient builder configuration
            // This is pseudocode showing the concept
            log.debug("Using custom temperature: {}", request.getTemperatureOverride());
        }

        return chatClient.prompt()
                .user(prompt)
                .call()
                .chatResponse();
    }

    /**
     * Save user and assistant messages to database.
     * Uses batch insert for performance.
     */
    private void saveMessages(ChatRequest request, String assistantMessage) {
        LocalDateTime now = LocalDateTime.now();

        Message userMsg = Message.builder()
                .conversationId(request.getConversationId())
                .role(Message.MessageRole.USER)
                .content(request.getUserMessage())
                .model("claude-opus-4-6")
                .createdAt(now)
                .build();

        Message assistantMsg = Message.builder()
                .conversationId(request.getConversationId())
                .role(Message.MessageRole.ASSISTANT)
                .content(assistantMessage)
                .model("claude-opus-4-6")
                .createdAt(now)
                .build();

        messageRepository.saveAll(List.of(userMsg, assistantMsg));
        log.info("Saved messages for conversation: {}", request.getConversationId());
    }

    /**
     * Search vector store for documents relevant to user message.
     * Returns top matches with similarity scores.
     */
    private List<ChatResponse.RelevantDocument> searchRelevantDocuments(String query) {
        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(TOP_K)
                            .build()
            );

            return docs.stream()
                    .map(doc -> ChatResponse.RelevantDocument.builder()
                            .documentId(doc.getId())
                            .content(doc.getText())
                            .source(doc.getMetadata().getOrDefault("source", "unknown").toString())
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Error searching vector store", e);
            return List.of();
        }
    }

    /**
     * Retrieve conversation history with pagination.
     * 
     * @param conversationId Conversation identifier
     * @return Conversation messages
     */
    @Transactional(readOnly = true)
    public List<Message> getConversationHistory(String conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    /**
     * Delete old messages for cleanup.
     * Useful for managing database size.
     */
    public long deleteOldMessages(int daysOld) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysOld);
        long deleted = messageRepository.deleteByCreatedAtBefore(cutoff);
        log.info("Deleted {} old messages", deleted);
        return deleted;
    }
}
