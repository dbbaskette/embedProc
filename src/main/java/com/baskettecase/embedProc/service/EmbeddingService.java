package com.baskettecase.embedProc.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.Counter;

import java.util.List;

@Service
public class EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    
    private final VectorStore vectorStore;
    private final Counter embeddingProcessedCounter;
    private final Counter embeddingErrorCounter;

    @Autowired
    public EmbeddingService(VectorStore vectorStore, 
                           Counter embeddingProcessedCounter,
                           Counter embeddingErrorCounter) {
        this.vectorStore = vectorStore;
        this.embeddingProcessedCounter = embeddingProcessedCounter;
        this.embeddingErrorCounter = embeddingErrorCounter;
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void storeEmbedding(String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                logger.warn("Attempted to store empty text, skipping");
                return;
            }

            Document doc = new Document(text);
            vectorStore.add(List.of(doc));
            
            embeddingProcessedCounter.increment();
            logger.debug("Successfully stored embedding for text preview: '{}'", 
                    text.substring(0, Math.min(text.length(), 50)) + "...");
            
        } catch (Exception e) {
            embeddingErrorCounter.increment();
            logger.error("Failed to store embedding for text preview: '{}'. Error: {}", 
                    text != null ? text.substring(0, Math.min(text.length(), 50)) + "..." : "null", 
                    e.getMessage());
            throw e;
        }
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void storeEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            logger.warn("Attempted to store empty text list, skipping");
            return;
        }

        int successCount = 0;
        int errorCount = 0;

        for (String text : texts) {
            try {
                storeEmbedding(text);
                successCount++;
            } catch (Exception e) {
                errorCount++;
                logger.error("Failed to store embedding in batch processing: {}", e.getMessage());
                // Continue processing other texts
            }
        }

        logger.info("Batch processing completed. Success: {}, Errors: {}", successCount, errorCount);
    }
} 