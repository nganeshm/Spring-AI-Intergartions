package com.example.springai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DocumentService: Handles ingestion of documents into the RAG vector store.
 *
 * This is the missing half of the RAG pipeline: ChatService only ever
 * *reads* from the VectorStore via similaritySearch(). Nothing gets into
 * the vector table (e.g. pgvector's "vector_store" table) unless something
 * calls VectorStore.add(...) - that's exactly what this service does.
 *
 * Flow:
 * 1. Read raw content (plain text, or a file via PDF/Tika readers)
 * 2. Split into token-sized chunks (TokenTextSplitter)
 * 3. Each chunk is embedded (EmbeddingModel, e.g. Ollama) and written to
 *    the VectorStore (PgVectorStore, auto-configured from application.yml)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final VectorStore vectorStore;

    @Value("${app.rag.chunk-size:1000}")
    private int chunkSize;

    /**
     * Ingest raw text directly into the vector store.
     *
     * @param content the raw text to embed and store
     * @param source  metadata tag identifying where this text came from
     * @return number of chunks written to the vector store
     */
    public int ingestText(String content, String source) {
        Document document = new Document(content, Map.of("source", source));

        List<Document> chunks = splitter().split(document);
        vectorStore.add(chunks);

        log.info("Ingested {} chunk(s) from text source '{}'", chunks.size(), source);
        return chunks.size();
    }

    /**
     * Ingest an uploaded file into the vector store.
     * PDF files are parsed with PagePdfDocumentReader; everything else
     * (docx, pptx, txt, html, ...) is parsed via Apache Tika.
     *
     * @param file the uploaded multipart file
     * @return number of chunks written to the vector store
     */
    public int ingestFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : UUID.randomUUID().toString();

        Resource resource = asNamedResource(file, filename);

        List<Document> rawDocuments = filename.toLowerCase().endsWith(".pdf")
                ? new PagePdfDocumentReader(resource).get()
                : new TikaDocumentReader(resource).get();

        rawDocuments.forEach(doc -> doc.getMetadata().putIfAbsent("source", filename));

        List<Document> chunks = splitter().split(rawDocuments);
        vectorStore.add(chunks);

        log.info("Ingested {} chunk(s) from file '{}'", chunks.size(), filename);
        return chunks.size();
    }

    private TokenTextSplitter splitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .build();
    }

    /**
     * Wraps the uploaded bytes in a Resource that reports the original
     * filename, since document readers use it to detect content and
     * populate metadata.
     */
    private Resource asNamedResource(MultipartFile file, String filename) throws IOException {
        return new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }
}
