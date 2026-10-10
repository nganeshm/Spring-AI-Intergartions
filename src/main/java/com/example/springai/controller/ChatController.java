package com.example.springai.controller;

import com.example.springai.dto.ChatRequest;
import com.example.springai.dto.ChatResponse;
import com.example.springai.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request for conversation: {}", request.getConversationId());

        try {
            ChatResponse response = chatService.chat(request);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(response);
            }

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(ChatResponse.builder()
                            .success(false)
                            .error("Invalid request: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<?> getConversation(@PathVariable String conversationId) {
        log.info("Retrieving conversation: {}", conversationId);

        var messages = chatService.getConversationHistory(conversationId);

        if (messages.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
                "conversationId", conversationId,
                "messages", messages,
                "messageCount", messages.size()
        ));
    }
}
