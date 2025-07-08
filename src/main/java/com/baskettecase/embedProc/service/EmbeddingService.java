package com.baskettecase.embedProc.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.Counter;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@Profile({"standalone", "cloud"})  // Exclude from local profile
public class EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    
    private final VectorStore vectorStore;
    private final Counter embeddingProcessedCounter;
    private final Counter embeddingErrorCounter;
    private final MonitorService monitorService;
    
    // Thread pool for parallel processing
    private final Executor embeddingExecutor = Executors.newFixedThreadPool(5);

    @Autowired
    public EmbeddingService(VectorStore vectorStore, 
                           Counter embeddingProcessedCounter,
                           Counter embeddingErrorCounter,
                           @Autowired(required = false) MonitorService monitorService) {
        this.vectorStore = vectorStore;
        this.embeddingProcessedCounter = embeddingProcessedCounter;
        this.embeddingErrorCounter = embeddingErrorCounter;
        this.monitorService = monitorService;
    }

    public void storeEmbedding(String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                logger.warn("Attempted to store empty text, skipping");
                return;
            }

            Document doc = new Document(text);
            vectorStore.add(List.of(doc));
            
            embeddingProcessedCounter.increment();
            
            // Update monitor service if available
            if (monitorService != null) {
                monitorService.incrementProcessedChunks(1);
            }
            
            // Removed debug logging to reduce log rate
            
        } catch (Exception e) {
            embeddingErrorCounter.increment();
            
            // Update monitor service if available
            if (monitorService != null) {
                monitorService.incrementErrors(1);
            }
            
            logger.error("Failed to store embedding for text preview: '{}'. Error: {}", 
                    text != null ? text.substring(0, Math.min(text.length(), 50)) + "..." : "null", 
                    e.getMessage());
            // Don't throw exception - just log the error
        }
    }

    public void storeEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            logger.warn("Attempted to store empty text list, skipping");
            return;
        }

        // Update total chunks count if monitor service is available
        if (monitorService != null) {
            monitorService.incrementTotalChunks(texts.size());
        }

        int successCount = 0;
        int errorCount = 0;

        // Process in batches for better performance
        int batchSize = 10; // Process 10 embeddings at a time
        for (int i = 0; i < texts.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, endIndex);
            
            try {
                // Convert batch to documents
                List<Document> documents = batch.stream()
                    .map(text -> new Document(text))
                    .toList();
                
                // Store batch in vector store
                vectorStore.add(documents);
                
                successCount += batch.size();
                embeddingProcessedCounter.increment(batch.size());
                
                // Removed debug logging to reduce log rate
                
            } catch (Exception e) {
                errorCount += batch.size();
                embeddingErrorCounter.increment(batch.size());
                logger.error("Failed to store batch of embeddings: {}", e.getMessage());
                // Continue processing other batches - don't throw exception
            }
        }

        // Update monitor service
        if (monitorService != null) {
            monitorService.incrementProcessedChunks(successCount);
            monitorService.incrementErrors(errorCount);
        }

        logger.info("Batch processing completed. Success: {}, Errors: {}", successCount, errorCount);
    }
    
    /**
     * Process embeddings in parallel for faster processing with real-time metrics
     * Uses CompletableFuture to process batches concurrently
     */
    public void storeEmbeddingsParallel(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            logger.warn("Attempted to store empty text list, skipping");
            return;
        }

        // Process in parallel batches
        int batchSize = 20; // Larger batches for parallel processing
        List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();
        
        for (int i = 0; i < texts.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, endIndex);
            final int batchIndex = i / batchSize + 1;
            final int totalBatches = (texts.size() + batchSize - 1) / batchSize;
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // Convert batch to documents
                    List<Document> documents = batch.stream()
                        .map(text -> new Document(text))
                        .toList();
                    
                    // Store batch in vector store
                    vectorStore.add(documents);
                    
                    embeddingProcessedCounter.increment(batch.size());
                    
                    // Update monitor service if available with real-time progress
                    if (monitorService != null) {
                        monitorService.incrementProcessedChunks(batch.size());
                        logger.debug("Processed batch {}/{} ({} chunks) - real-time metrics updated", 
                                   batchIndex, totalBatches, batch.size());
                    }
                    
                } catch (Exception e) {
                    embeddingErrorCounter.increment(batch.size());
                    
                    // Update monitor service if available
                    if (monitorService != null) {
                        monitorService.incrementErrors(batch.size());
                        logger.error("Failed to store parallel batch {}/{} of embeddings: {}", 
                                   batchIndex, totalBatches, e.getMessage());
                    }
                }
            }, embeddingExecutor);
            
            futures.add(future);
        }

        // Wait for all parallel operations to complete
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            logger.info("Parallel batch processing completed for {} chunks in {} batches", texts.size(), futures.size());
        } catch (Exception e) {
            logger.error("Error during parallel processing: {}", e.getMessage());
        }
    }
} 