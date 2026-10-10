# Spring AI RAG Chatbot Application

A production-ready **Retrieval-Augmented Generation (RAG)** chatbot application built with Spring AI, Claude API, and vector databases. Demonstrates enterprise-grade patterns for integrating AI models into Spring Boot applications.

## 📋 Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture & Components](#architecture--components)
3. [Spring AI Ecosystem](#spring-ai-ecosystem)
4. [Dependencies Explained](#dependencies-explained)
5. [Annotations & Their Roles](#annotations--their-roles)
6. [Setup & Configuration](#setup--configuration)
7. [API Usage](#api-usage)
8. [RAG Pipeline](#rag-pipeline)
9. [Troubleshooting](#troubleshooting)

---

## Project Overview

**What is this?**
A chat application that combines Spring Boot with AI models to answer questions based on your document knowledge base. Uses **RAG** to retrieve relevant documents before generating responses, improving accuracy and reducing hallucinations.

**Key Features:**
- 🤖 Claude API integration via Spring AI
- 📚 RAG (Retrieval-Augmented Generation) with vector embeddings
- 💾 Persistent chat history (PostgreSQL)
- 🔍 Similarity search over documents
- 🚀 Production-ready error handling
- 📊 Extensible architecture

**Tech Stack:**
```
Spring Boot 3.3.0
├── Spring AI (ChatClient, VectorStore)
├── Claude API (via Anthropic integration)
├── PostgreSQL (chat history)
├── Chroma Vector DB (embeddings)
└── Ollama (local embeddings)
```

---

## Architecture & Components

### System Design

```
┌─────────────────┐
│   REST Client   │
└────────┬────────┘
         │ POST /api/chat
         ↓
┌─────────────────────────┐
│   ChatController        │ → Spring Web MVC
└────────┬────────────────┘
         │ calls
         ↓
┌─────────────────────────┐
│   ChatService           │ → Business Logic
├─────────────────────────┤
│ 1. Retrieve History     │
│ 2. Search Vector Store  │ → RAG Pipeline
│ 3. Build Prompt         │
│ 4. Call ChatClient      │ → Spring AI
│ 5. Save to Database     │
└────────┬─────┬──────────┘
         │     │
    ┌────▼─┐  │
    │  DB  │  │
    └──────┘  │
         ┌────▼──────────────┐
         │  Spring AI Stack  │
         ├───────────────────┤
         │ ChatClient        │
         │ ├─ ChatModel      │ → Claude API
         │ └─ Advisors       │
         │ VectorStore       │ → Chroma DB
         │ EmbeddingModel    │ → Ollama
         └───────────────────┘
```

### Class Hierarchy

| Layer | Classes | Responsibility |
|-------|---------|-----------------|
| **Controller** | ChatController | HTTP request/response handling |
| **Service** | ChatService | Business logic, orchestration |
| **Repository** | MessageRepository | Database access (Spring Data JPA) |
| **Domain** | Message | Entity, maps to DB table |
| **DTO** | ChatRequest, ChatResponse | JSON serialization |
| **Config** | SpringAiConfig | Spring AI bean configuration |

---

## Spring AI Ecosystem

### Core Components Explained

#### 1. **ChatClient** (Recommended)
High-level API for interacting with LLMs. Replaces direct `ChatModel` usage.

```java
// Instead of ChatModel:
chatModel.call(new Prompt(...));

// Use ChatClient (cleaner):
chatClient.prompt()
    .user("What is Spring AI?")
    .call()
    .content();
```

**Advantages:**
- Fluent, readable API
- Automatic message formatting
- Built-in streaming support
- Advisor pipeline for RAG
- Better error handling

**Key Methods:**
```java
chatClient.prompt()           // Start building request
    .user(String)             // Add user message
    .system(String)           // Add system prompt
    .call()                   // Execute request
    .content()                // Get response as string
    .chatResponse()           // Get full response object
```

#### 2. **ChatModel**
Abstraction for LLM providers (Claude, OpenAI, Gemini, etc.). Auto-configured from dependencies.

```java
@Bean
public ChatClient chatClient(ChatModel chatModel) {
    return ChatClient.builder(chatModel).build();
}
```

Implementations:
- `AnthropicChatModel` (Claude)
- `OpenAiChatModel` (GPT-4, GPT-3.5)
- `OllamaChatModel` (Local models)

#### 3. **VectorStore**
Manages embeddings for RAG. Supports multiple backends.

```java
// Search for similar documents
List<Document> results = vectorStore.similaritySearch("Spring AI", 5);

// Add documents with metadata
vectorStore.add(List.of(
    new Document("Spring AI enables...", Map.of("source", "docs.md"))
));
```

**Supported Implementations:**
| Database | Use Case | Setup |
|----------|----------|-------|
| **Chroma** | Dev, prototyping | Docker: `docker run -p 8000:8000 chromadb/chroma` |
| **PgVector** | Production + PostgreSQL | Extension: `CREATE EXTENSION vector;` |
| **Pinecone** | Managed cloud | API key required |
| **Weaviate** | Enterprise search | Docker or cloud |
| **Milvus** | High-scale | Docker with GPU |

#### 4. **EmbeddingModel**
Converts text to vector embeddings for semantic search.

```java
EmbeddingResponse response = embeddingModel.embedForResponse("Spring AI");
float[] embedding = response.getResult().getOutput();
```

Providers:
- **Ollama** (local, free, offline)
- **OpenAI** (cloud, $$$)
- **Hugging Face** (cloud, free tier)
- **Cohere** (cloud, specialized)

#### 5. **Document Readers**
Loads documents from various sources into VectorStore.

```java
// PDF loading
PdfDocumentReader pdfReader = new PdfDocumentReader(Resource);
List<Document> docs = pdfReader.get();
vectorStore.add(docs);

// Tika support (Word, Excel, PPT, etc.)
TikaDocumentReader tikaReader = new TikaDocumentReader(Resource);
vectorStore.add(tikaReader.get());
```

---

## Dependencies Explained

### `pom.xml` Breakdown

#### Spring AI Bill of Materials (BOM)
```xml
<dependencyManagement>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-bom</artifactId>
        <version>1.0.0-M3</version>
        <type>pom</type>
        <scope>import</scope>
    </dependency>
</dependencyManagement>
```

**What it does:** Centralizes Spring AI version management. Ensures all Spring AI libraries use compatible versions without specifying `<version>` tags individually.

#### Core Dependencies

| Dependency | Purpose | When to Use |
|------------|---------|------------|
| `spring-ai-anthropic` | Claude API support | If using Anthropic/Claude |
| `spring-ai-openai` | OpenAI (GPT-4) | If using OpenAI models |
| `spring-ai-ollama` | Local embeddings | For on-premise, offline |
| `spring-ai-spring-boot-autoconfigure` | Auto-configuration | Always required |
| `spring-ai-chroma` | Chroma vector DB | If using Chroma |
| `spring-ai-pgvector-store` | PostgreSQL pgvector | If using PgVector |
| `spring-ai-pdf-document-reader` | PDF parsing | If loading PDFs |
| `spring-ai-tika-document-reader` | Office documents | If loading Word/Excel/PPT |

#### Boot Starters

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

Brings in: Spring MVC, Tomcat, Jackson, Validation

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

Brings in: Hibernate ORM, Spring Data JPA, Transaction management

---

## Annotations & Their Roles

### Spring Boot Core Annotations

#### Class-Level Annotations

| Annotation | Location | Purpose | Example |
|-----------|----------|---------|---------|
| `@SpringBootApplication` | Main class | Enables auto-config, component scan | `SpringAiRagChatbotApplication` |
| `@Configuration` | Config class | Spring configuration (Java-based) | `SpringAiConfig` |
| `@RestController` | Controller class | REST endpoints, auto-JSON serialization | `ChatController` |
| `@Service` | Service class | Business logic bean | `ChatService` |
| `@Repository` | Repository interface | Data access, exception translation | `MessageRepository` |
| `@Entity` | Domain class | Maps to database table | `Message` |
| `@EnableScheduling` | Main class | Enables @Scheduled tasks | `SpringAiRagChatbotApplication` |

#### Method-Level Annotations

| Annotation | Location | Purpose | Scope |
|-----------|----------|---------|-------|
| `@Bean` | Config method | Registers object as Spring bean | Application |
| `@Transactional` | Service method | Manages database transactions | Method/Class |
| `@PostMapping` | Controller method | Maps POST HTTP requests | Endpoint |
| `@GetMapping` | Controller method | Maps GET HTTP requests | Endpoint |
| `@DeleteMapping` | Controller method | Maps DELETE HTTP requests | Endpoint |
| `@RequestMapping` | Controller class | Base URL path | Class |
| `@ResponseStatus` | Method | Sets HTTP response code | Endpoint |
| `@ExceptionHandler` | Method | Handles exceptions globally | Class |

#### Parameter Annotations

| Annotation | Purpose | Example |
|-----------|---------|---------|
| `@RequestBody` | Deserialize JSON to Java object | `ChatRequest request` |
| `@PathVariable` | Extract value from URL path | `@PathVariable String conversationId` |
| `@RequestParam` | Extract query parameter | `@RequestParam int page` |
| `@Valid` | Trigger validation on object | `@Valid @RequestBody ChatRequest` |

### Spring AI Specific Annotations

| Annotation | Purpose | Example |
|-----------|---------|---------|
| `@ConditionalOnBean` | Create bean only if dependency exists | `@ConditionalOnBean(EmbeddingModel.class)` |
| `@ConditionalOnMissingBean` | Create only if not already defined | Override defaults |
| `@ConditionalOnProperty` | Create if property is set | `spring.ai.enabled=true` |

### JPA/Hibernate Annotations

| Annotation | Location | Purpose | Example |
|-----------|----------|---------|---------|
| `@Table` | Entity class | Specifies database table name | `@Table(name = "messages")` |
| `@Column` | Entity field | Column name, constraints | `@Column(nullable = false)` |
| `@Id` | Entity field | Primary key | `@Id private Long id;` |
| `@GeneratedValue` | Primary key field | Auto-generation strategy | `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `@Enumerated` | Enum field | Enum storage strategy | `@Enumerated(EnumType.STRING)` |
| `@PrePersist` | Entity method | Called before INSERT | `@PrePersist protected void onCreate()` |
| `@PreUpdate` | Entity method | Called before UPDATE | `@PreUpdate protected void onUpdate()` |
| `@Index` | @Table | Database index for performance | `@Index(name = "idx_conversation_id", columnList = "conversation_id")` |

### Validation Annotations

```java
// From jakarta.validation.constraints (Jakarta Bean Validation API)
@NotBlank(message = "...")     // String must not be empty
@NotEmpty(message = "...")     // Collection must not be empty
@NotNull(message = "...")      // Value cannot be null
@Min(value = 1)                // Minimum value
@Max(value = 100)              // Maximum value
@Email                         // Email format
@Pattern(regexp = "...")       // Regex pattern
@Size(min = 1, max = 255)      // Collection/String size
@Positive                      // Number > 0
@Range(min = 1, max = 100)     // Value range
```

### Lombok Annotations (Reduces Boilerplate)

| Annotation | Generates |
|-----------|-----------|
| `@Data` | Getters, setters, equals, hashCode, toString |
| `@NoArgsConstructor` | No-argument constructor |
| `@AllArgsConstructor` | Constructor with all fields |
| `@Builder` | Builder pattern |
| `@RequiredArgsConstructor` | Constructor for final fields (dependency injection) |
| `@Slf4j` | Logger instance: `log.info()`, `log.error()` |
| `@Value` | Immutable class |
| `@Getter/@Setter` | Individual getters/setters |

---

## Setup & Configuration

### Prerequisites

```bash
# Required
- Java 17+
- Maven 3.8+
- Docker (for PostgreSQL, Chroma, Ollama)

# External Services
- Anthropic API Key (https://console.anthropic.com)
- Chroma running on localhost:8000
- Ollama running on localhost:11434 (for embeddings)
```

### 1. Install PostgreSQL (for chat history)

```bash
# Using Docker
docker run -d \
  --name postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=spring_ai_db \
  -p 5432:5432 \
  postgres:15-alpine

# Create pgvector extension (optional, if using PgVector)
docker exec -it postgres psql -U postgres -d spring_ai_db -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

### 2. Install Chroma (Vector DB)

```bash
docker run -d \
  --name chroma \
  -p 8000:8000 \
  chromadb/chroma

# Verify: curl http://localhost:8000/api/v1/heartbeat
```

### 3. Install Ollama (Local Embeddings)

```bash
# Download from https://ollama.ai
# Or Docker
docker run -d \
  --name ollama \
  -p 11434:11434 \
  ollama/ollama

# Pull embedding model
docker exec ollama ollama pull nomic-embed-text
```

### 4. Environment Setup

```bash
# Set API key
export ANTHROPIC_API_KEY="sk-ant-..."

# Optional: Database credentials
export DB_USERNAME="postgres"
export DB_PASSWORD="postgres"
```

### 5. Build & Run

```bash
# Clone/navigate to project
cd spring-ai-rag-chatbot

# Build
mvn clean package

# Run
java -jar target/spring-ai-rag-chatbot-1.0.0.jar

# Or using Maven directly
mvn spring-boot:run

# Application starts on http://localhost:8080
```

### 6. Verify Installation

```bash
# Health check
curl http://localhost:8080/api/chat/health

# Expected response:
# {"status":"UP","service":"ChatBot AI Service"}
```

---

## API Usage

### 1. Send a Chat Message (with RAG)

**Endpoint:** `POST /api/chat`

**Request:**
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "conversationId": "user-123-conv-1",
    "userMessage": "What is Spring AI and how does RAG work?",
    "useRag": true,
    "systemPrompt": "You are a Spring Framework expert. Answer concisely."
  }'
```

**Request Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `conversationId` | String | ✅ | Unique conversation identifier |
| `userMessage` | String | ✅ | User question/prompt |
| `useRag` | Boolean | ❌ | Enable RAG (default: true) |
| `systemPrompt` | String | ❌ | Custom system prompt |
| `temperatureOverride` | Double | ❌ | Override default temperature (0-1) |
| `documentIds` | String[] | ❌ | Filter RAG to specific documents |

**Response:**
```json
{
  "conversationId": "user-123-conv-1",
  "userMessage": "What is Spring AI and how does RAG work?",
  "assistantMessage": "Spring AI is a framework for building AI applications in Spring Boot...",
  "model": "claude-opus-4-6",
  "relevantDocuments": [
    {
      "documentId": "doc-1",
      "title": "Spring AI Guide",
      "content": "Spring AI enables seamless integration...",
      "relevanceScore": 0.92,
      "source": "docs/spring-ai.md"
    },
    {
      "documentId": "doc-2",
      "title": "RAG Patterns",
      "content": "RAG combines retrieval with generation...",
      "relevanceScore": 0.87,
      "source": "docs/rag-patterns.md"
    }
  ],
  "processingTimeMs": 2341.5,
  "timestamp": "2024-01-15T10:30:00",
  "success": true
}
```

### 2. Get Conversation History

**Endpoint:** `GET /api/chat/{conversationId}`

```bash
curl http://localhost:8080/api/chat/user-123-conv-1
```

**Response:**
```json
{
  "conversationId": "user-123-conv-1",
  "messages": [
    {
      "id": 1,
      "conversationId": "user-123-conv-1",
      "role": "USER",
      "content": "What is Spring AI?",
      "model": "claude-opus-4-6",
      "createdAt": "2024-01-15T10:30:00"
    },
    {
      "id": 2,
      "conversationId": "user-123-conv-1",
      "role": "ASSISTANT",
      "content": "Spring AI is a framework...",
      "model": "claude-opus-4-6",
      "createdAt": "2024-01-15T10:30:05"
    }
  ],
  "messageCount": 2
}
```

### 3. Error Responses

**Validation Error (400):**
```json
{
  "success": false,
  "error": "Validation failed",
  "details": [
    {
      "field": "userMessage",
      "message": "User message cannot be blank"
    }
  ]
}
```

**Server Error (500):**
```json
{
  "conversationId": "user-123-conv-1",
  "userMessage": "What is Spring AI?",
  "error": "API rate limit exceeded",
  "success": false,
  "timestamp": "2024-01-15T10:30:00"
}
```

---

## RAG Pipeline

### How RAG Works in This Application

**RAG = Retrieval-Augmented Generation**

```
1. User Message
     ↓
2. Retrieve similar documents from vector store
     ↓
3. Augment prompt with retrieved documents
     ↓
4. Send augmented prompt to Claude
     ↓
5. Claude generates response (more accurate!)
     ↓
6. Return response + source documents
```

### Document Ingestion Example

```java
// Add documents to vector store (DocumentService)
@Service
public class DocumentService {
    
    private final VectorStore vectorStore;
    
    public void ingestPdfDocuments(Resource pdfFile) {
        // Step 1: Read PDF
        PdfDocumentReader reader = new PdfDocumentReader(pdfFile);
        List<Document> documents = reader.get();
        
        // Step 2: Chunk (split into smaller pieces)
        // Done automatically by DocumentReader
        
        // Step 3: Generate embeddings & store
        vectorStore.add(documents);
    }
}
```

### Chunking Strategy

Documents are split into chunks for better retrieval:

```
Original Document (5000 chars)
     ↓
Chunk Size: 1000 chars
Overlap: 200 chars
     ↓
Chunk 1: [0-1000]      (Overlap: 800-1000)
Chunk 2: [800-1800]    (Overlap: 1600-1800)
Chunk 3: [1600-2600]   
...
```

**Why overlap?** Preserves context between chunks, prevents losing important information at chunk boundaries.

### Similarity Search

```java
// Search for top 5 similar documents
List<Document> results = vectorStore.similaritySearch(
    "Spring AI framework", 
    5  // top-k
);

// Each document has similarity score (0-1)
// 1.0 = perfect match
// 0.0 = completely unrelated
```

---

## Configuration Reference

### `application.yml` Properties

#### Anthropic/Claude Configuration
```yaml
spring.ai.anthropic:
  api-key: ${ANTHROPIC_API_KEY}  # Required
  chat.options:
    model: claude-opus-4-6      # Model selection
    temperature: 0.7             # 0=deterministic, 1=creative
    top-p: 0.95                 # Nucleus sampling
    top-k: 40                    # Token limit
    max-tokens: 2048            # Response length limit
```

**Parameters Explained:**

| Parameter | Range | Effect |
|-----------|-------|--------|
| `temperature` | 0.0-1.0 | Higher = more creative/random |
| `top-p` | 0.0-1.0 | Nucleus sampling (diversity) |
| `top-k` | 1-100 | Keep top-k probable tokens |
| `max-tokens` | 1-4000 | Max response length |

#### Vector Store Configuration (Chroma)
```yaml
spring.ai.vectorstore.chroma:
  client: localhost
  port: 8000
  collection-name: documents
  initialize-schema: true
```

#### Ollama Embeddings Configuration
```yaml
spring.ai.ollama:
  base-url: http://localhost:11434
  embed:
    model: nomic-embed-text  # Embedding model name
    options:
      num-predict: 384       # Embedding vector size
```

#### Database Configuration
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/spring_ai_db
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    hibernate.ddl-auto: validate  # validate|create|create-drop|update
    show-sql: false
```

---

## Troubleshooting

### Common Issues & Solutions

#### 1. "API Key not found" Error

```
Error: spring.ai.anthropic.api-key not configured
```

**Solution:**
```bash
# Set environment variable
export ANTHROPIC_API_KEY="sk-ant-..."

# Or add to application.yml
spring:
  ai:
    anthropic:
      api-key: sk-ant-...  # NOT recommended for production
```

#### 2. "Cannot connect to Chroma" Error

```
java.net.ConnectException: Connection refused
```

**Solution:**
```bash
# Verify Chroma is running
curl http://localhost:8000/api/v1/heartbeat

# If not running, start it
docker run -d -p 8000:8000 chromadb/chroma

# Check logs
docker logs chroma
```

#### 3. "Cannot find EmbeddingModel" Error

```
Error: No qualifying bean of type 'EmbeddingModel'
```

**Solution:** Add Ollama dependency and ensure Ollama is running:

```bash
# Start Ollama
docker run -d -p 11434:11434 ollama/ollama

# Pull model
docker exec ollama ollama pull nomic-embed-text
```

#### 4. "Validation failed" (400 Error)

```
"conversationId": "Field error: conversationId cannot be blank"
```

**Solution:** Ensure all required fields are provided:
```json
{
  "conversationId": "conv-123",    // Required
  "userMessage": "Hello"            // Required
}
```

#### 5. Rate Limit Error

```
Error: 429 Too Many Requests - API rate limit exceeded
```

**Solution:** Implement retry logic or backoff:

```java
@Service
public class ChatService {
    @Retry(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public ChatResponse chat(ChatRequest request) {
        // Will retry up to 3 times with 1s delay
    }
}
```

#### 6. Database Connection Error

```
org.postgresql.util.PSQLException: Connection refused
```

**Solution:**
```bash
# Start PostgreSQL
docker run -d -p 5432:5432 \
  -e POSTGRES_DB=spring_ai_db \
  postgres:15-alpine

# Verify connection
psql -h localhost -U postgres -d spring_ai_db
```

---

## Performance Optimization Tips

### 1. Database Indexing
Already included in `Message.java`:
```java
@Table(indexes = {
    @Index(name = "idx_conversation_id", columnList = "conversation_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
```

### 2. Connection Pooling (HikariCP)
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 20000
```

### 3. Caching Conversation History
```java
@Cacheable("conversations")
public List<Message> getConversationHistory(String conversationId) {
    return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
}
```

### 4. Vector Search Optimization
- Use chunking overlap to avoid missing context
- Limit similarity search to top-k (default: 5)
- Use metadata filtering to reduce search space

---

## Next Steps

1. **Add Document Upload:** Create endpoint to ingest PDFs/Word docs
2. **Implement Caching:** Cache conversation history with Redis
3. **Add Function Calling:** Let AI call Java methods (e.g., fetch real-time data)
4. **Deploy to Cloud:** Docker + Kubernetes on GKE/EKS
5. **Add UI:** React frontend to communicate with API

---

## References

- [Spring AI Documentation](https://docs.spring.io/spring-ai/docs/current/reference/html/)
- [Anthropic Claude API](https://docs.anthropic.com)
- [RAG Patterns](https://js.langchain.com/docs/modules/chains/popular/openai_functions)
- [Spring Boot Reference](https://spring.io/projects/spring-boot)

---

**Happy coding! 🚀**
