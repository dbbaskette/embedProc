package com.baskettecase.embedProc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for persisting text embeddings in PostgreSQL/pgvector using Spring AI's VectorStore.
 * <p>
 * Handles creation of Document objects and storage, with logging for both success and error cases.
 */
@Service
public class VectorStoreService {
    private static final Logger logger = LoggerFactory.getLogger(VectorStoreService.class);
    private final VectorStore vectorStore;

    public VectorStoreService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Stores the embedding and associated text in the pgvector database.
     * Logs both success and error.
     *
     * @param text      The original input text
     * @param embedding The embedding vector to store
     */
    public void saveEmbedding(String text, List<Double> embedding) {
        try {
            Document doc = new Document(text);
            doc.setEmbedding(embedding);
            vectorStore.add(List.of(doc));
            logger.info("[VectorStoreService] Successfully stored embedding for text preview: '{}', size: {}", text.length() > 50 ? text.substring(0, 50) + "..." : text, embedding.size());
        } catch (Exception e) {
            logger.error("[VectorStoreService] Failed to store embedding for text preview: '{}'. Error: {}", text.length() > 50 ? text.substring(0, 50) + "..." : text, e.getMessage(), e);
        }
    }
}
