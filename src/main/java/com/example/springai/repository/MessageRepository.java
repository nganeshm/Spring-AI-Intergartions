package com.example.springai.repository;

import com.example.springai.dto.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MessageRepository interface using Spring Data JPA.
 * 
 * Annotations:
 * @Repository: Marks as Spring Data repository, enables exception translation
 * @Query: Custom JPQL/native SQL queries
 * @Param: Binds method parameters to query placeholders
 * 
 * Spring Data JPA automatically implements CRUD operations:
 * - save(), saveAll()
 * - findById(), findAll()
 * - delete(), deleteById()
 * - count(), exists()
 * 
 * Custom query methods follow naming conventions:
 * - findBy[FieldName](): finds by single field
 * - findBy[Field1]And[Field2](): finds by multiple fields (AND logic)
 * - OrderBy[FieldName](Asc|Desc): sorts results
 * - Page/List return types for pagination
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Find all messages in a conversation, sorted by creation time.
     * Method naming convention allows Spring Data to generate the query automatically.
     */
    List<Message> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    /**
     * Find paginated messages for a conversation.
     * Pageable parameter enables pagination/sorting.
     */
    Page<Message> findByConversationId(String conversationId, Pageable pageable);

    /**
     * Find messages by role (USER or ASSISTANT) in a conversation.
     */
    List<Message> findByConversationIdAndRoleOrderByCreatedAtAsc(
            String conversationId, 
            Message.MessageRole role
    );

    /**
     * Custom JPQL query: Find messages in a conversation within a date range.
     * @Query: Specifies custom JPQL query
     * @Param: Binds named parameters
     */
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
           "AND m.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY m.createdAt ASC")
    List<Message> findMessagesByDateRange(
            @Param("conversationId") String conversationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Custom native SQL query: Find latest message in conversation.
     * nativeQuery = true: Executes as native SQL instead of JPQL
     */
    @Query(value = "SELECT * FROM messages WHERE conversation_id = :conversationId " +
                   "ORDER BY created_at DESC LIMIT 1", nativeQuery = true)
    Message findLatestMessageInConversation(@Param("conversationId") String conversationId);

    /**
     * Find count of messages by role in a conversation.
     * Useful for conversation statistics.
     */
    long countByConversationIdAndRole(String conversationId, Message.MessageRole role);

    /**
     * Find all unique conversation IDs.
     * Returns paginated results.
     */
    @Query(value = "SELECT DISTINCT m.conversation_id FROM messages m " +
                   "ORDER BY MAX(m.created_at) DESC", nativeQuery = true)
    Page<String> findDistinctConversationIds(Pageable pageable);

    /**
     * Delete messages older than specified date.
     * Useful for cleanup operations.
     */
    long deleteByCreatedAtBefore(LocalDateTime date);
}
