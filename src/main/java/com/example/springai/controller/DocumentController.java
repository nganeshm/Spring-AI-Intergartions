package com.example.springai.controller;

import com.example.springai.dto.DocumentIngestRequest;
import com.example.springai.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * DocumentController: REST endpoints for ingesting content into the RAG vector store.
 *
 * Without calling one of these endpoints first, ChatService's RAG lookups
 * (vectorStore.similaritySearch) have nothing to retrieve, since the
 * vector store table stays empty until documents are explicitly added.
 *
 * POST /api/documents/text   - ingest raw text
 * POST /api/documents/upload - ingest an uploaded file (PDF, DOCX, TXT, HTML, ...)
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/text")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Map<String, Object>> ingestText(@Valid @RequestBody DocumentIngestRequest request) {
        log.info("Ingesting text document from source: {}", request.getSource());

        int chunks = documentService.ingestText(request.getContent(), request.getSource());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "chunksIngested", chunks,
                "source", request.getSource()
        ));
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> uploadDocument(@RequestParam("file") MultipartFile file) throws IOException {
        log.info("Ingesting uploaded file: {}", file.getOriginalFilename());

        int chunks = documentService.ingestFile(file);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "chunksIngested", chunks,
                "filename", file.getOriginalFilename()
        ));
    }
}
