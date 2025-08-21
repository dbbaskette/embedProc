package com.baskettecase.embedProc.processor;
    
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.beans.factory.annotation.Value;
import com.baskettecase.embedProc.service.FileDownloaderService;
import com.baskettecase.embedProc.service.TextChunkingService;
import com.baskettecase.embedProc.service.EmbeddingService;
import com.baskettecase.embedProc.service.DocumentType;
import com.baskettecase.embedProc.service.ProcessingStateService;

import com.baskettecase.embedProc.service.MonitorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;


import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;



import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Semaphore;
import java.io.File;

import java.nio.file.Files;


@Configuration
@Profile("cloud")
@EnableAsync
public class ScdfStreamProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(ScdfStreamProcessor.class);
    
    private final FileDownloaderService fileDownloaderService;
    private final TextChunkingService textChunkingService;
    private final EmbeddingService embeddingService;
    private final ProcessingStateService processingStateService;

    private final VectorQueryProcessor vectorQueryProcessor;
    private final MonitorService monitorService;
    private final String queryText;
    private final boolean useReferenceNumbers;
    private final Integer defaultRefnum1;
    private final Integer defaultRefnum2;
    private final AtomicBoolean queryRun = new AtomicBoolean(false);
    private final ObjectMapper objectMapper;

    

    
    // Work limiting to prevent single instance from taking too much work
    private final AtomicInteger activeProcessingCount = new AtomicInteger(0);
    private final Semaphore processingSemaphore;
    private final int maxConcurrentFiles;

    public ScdfStreamProcessor(FileDownloaderService fileDownloaderService,
                             TextChunkingService textChunkingService,
                             EmbeddingService embeddingService,
                             ProcessingStateService processingStateService,

                             VectorQueryProcessor vectorQueryProcessor, 
                             MonitorService monitorService,
                             @Value("${app.query.text:}") String queryText,
                             @Value("${app.reference-numbers.enabled:false}") boolean useReferenceNumbers,
                             @Value("${app.reference-numbers.default.refnum1:100001}") Integer defaultRefnum1,
                             @Value("${app.reference-numbers.default.refnum2:200001}") Integer defaultRefnum2,
                             @Value("${app.processing.max-concurrent-files:2}") int maxConcurrentFiles,
                             ObjectMapper objectMapper) {
        this.fileDownloaderService = fileDownloaderService;
        this.textChunkingService = textChunkingService;
        this.embeddingService = embeddingService;
        this.processingStateService = processingStateService;

        this.vectorQueryProcessor = vectorQueryProcessor;
        this.monitorService = monitorService;
        this.queryText = queryText;
        this.useReferenceNumbers = useReferenceNumbers;
        this.defaultRefnum1 = defaultRefnum1;
        this.defaultRefnum2 = defaultRefnum2;
        this.maxConcurrentFiles = maxConcurrentFiles;
        this.processingSemaphore = new Semaphore(maxConcurrentFiles);
        this.objectMapper = objectMapper;
        
        logger.info("ScdfStreamProcessor initialized with reference numbers: {}", useReferenceNumbers);
        if (useReferenceNumbers) {
            logger.info("Default reference numbers - refnum1: {}, refnum2: {}", defaultRefnum1, defaultRefnum2);
        }
    }

    @PostConstruct
    public void logBeanCreation() {
        logger.info("ScdfStreamProcessor bean created for SCDF deployment");
    }




    




    private String extractFileUrl(String message) {
        try {
            // Parse message as JSON
            JsonNode jsonNode = objectMapper.readTree(message);
            
            // Extract URL from the standardized JSON format
            String fileUrl = null;
            if (jsonNode.has("url")) {
                fileUrl = jsonNode.get("url").asText();
            } else {
                logger.warn("No 'url' field found in message JSON");
                return null;
            }
            
            if (fileUrl == null || fileUrl.trim().isEmpty()) {
                logger.warn("URL field is empty in message");
                return null;
            }
            
            // Fix WebHDFS URL encoding and add operation parameter using FileDownloaderService
            fileUrl = fileDownloaderService.fixWebHdfsUrl(fileUrl);
            
            return fileUrl;
            
        } catch (Exception e) {
            logger.warn("Failed to parse message as JSON: {}", e.getMessage());
            return null;
        }
    }


    
    /**
     * Process file in streaming fashion from temp storage with real-time metrics
     */
    @Async
    public CompletableFuture<Void> processFileStreamingFromTemp(String fileUrl) {
        File tempFile = null;
        try {
            logger.info("Starting streaming temp file processing for: {}", fileUrl);
            
            // Update current file being processed
            if (monitorService != null) {
                String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
                if (filename.contains("?")) {
                    filename = filename.substring(0, filename.indexOf("?"));
                }
                monitorService.setCurrentFile(filename);
            }
            
            // Download file to temp storage using FileDownloaderService
            tempFile = fileDownloaderService.downloadFileToTemp(fileUrl);
            if (tempFile == null || !tempFile.exists()) {
                logger.warn("Failed to download file to temp storage: {}", fileUrl);
                return CompletableFuture.completedFuture(null);
            }
            
            logger.info("Processing temp file: {} ({} bytes)", tempFile.getAbsolutePath(), tempFile.length());
            
            // Read file content from temp storage
            String fileContent = new String(Files.readAllBytes(tempFile.toPath()));
            logger.info("Processing document of length: {} characters from temp file", fileContent.length());
            
            // Process in streaming batches with real-time metrics
            int streamingChunkSize = 200; // Smaller batches for better responsiveness
            List<String> allChunks = textChunkingService.chunkTextEnhanced(fileContent);
            logger.info("Created {} total chunks from temp file", allChunks.size());
            
            if (allChunks.isEmpty()) {
                logger.warn("No chunks generated from the temp file");
                return CompletableFuture.completedFuture(null);
            }

            // Update total chunks count for this file
            if (monitorService != null) {
                monitorService.incrementTotalChunks(allChunks.size());
                logger.info("Updated total chunks count: {} for file: {}", allChunks.size(), fileUrl);
            }

            // Process chunks in streaming batches with real-time progress updates
            int totalBatches = (allChunks.size() + streamingChunkSize - 1) / streamingChunkSize;
            int processedChunks = 0;
            
            for (int i = 0; i < allChunks.size(); i += streamingChunkSize) {
                int endIndex = Math.min(i + streamingChunkSize, allChunks.size());
                List<String> batch = allChunks.subList(i, endIndex);
                int batchNumber = (i / streamingChunkSize) + 1;
                
                logger.info("Processing batch {}/{} (chunks {}-{}) for file: {}", 
                    batchNumber, totalBatches, i + 1, endIndex, fileUrl);
                
                // Store embeddings for this batch using parallel processing
                if (useReferenceNumbers) {
                    // Extract reference numbers from file URL
                    ReferenceNumbers refNumbers = extractReferenceNumbersFromFileUrl(fileUrl);
                    Integer refnum1 = refNumbers != null ? refNumbers.refnum1 : defaultRefnum1;
                    Integer refnum2 = refNumbers != null ? refNumbers.refnum2 : defaultRefnum2;
                    
                    if (refNumbers != null) {
                        logger.info("Using reference numbers from filename - refnum1: {}, refnum2: {}", refnum1, refnum2);
                    } else {
                        logger.info("Using default reference numbers - refnum1: {}, refnum2: {}", refnum1, refnum2);
                    }
                    
                    // Convert to TextWithMetadata and use embedding service with metadata
                    DocumentType documentType = DocumentType.fromUrl(fileUrl);
                    List<EmbeddingService.TextWithMetadata> metadataBatch = batch.stream()
                        .map(text -> {
                            // For reference documents, don't include refnums
                            if (documentType == DocumentType.REFERENCE) {
                                return new EmbeddingService.TextWithMetadata(text, null, null, documentType, fileUrl);
                            } else {
                                return new EmbeddingService.TextWithMetadata(text, refnum1, refnum2, documentType, fileUrl);
                            }
                        })
                        .collect(java.util.stream.Collectors.toList());
                    embeddingService.storeEmbeddingsWithMetadataParallel(metadataBatch);
                } else {
                    embeddingService.storeEmbeddingsParallel(batch);
                }
                
                // Update progress metrics
                processedChunks += batch.size();
                if (monitorService != null) {
                    // Real-time progress update
                    logger.info("Progress: {}/{} chunks processed ({:.1f}%) for file: {}", 
                        processedChunks, allChunks.size(), 
                        (processedChunks * 100.0) / allChunks.size(), fileUrl);
                }
                
                // Small delay to prevent overwhelming the system
                Thread.sleep(25);
            }
            
            // Optionally run query after embedding if queryText is set and hasn't run yet
            if (queryText != null && !queryText.isBlank() && queryRun.compareAndSet(false, true)) {
                vectorQueryProcessor.runQuery(queryText, 5);
            }
            
            // Final completion metric
            String completedFilename = null;
            if (monitorService != null) {
                logger.info("File processing completed: {} chunks processed for file: {}", 
                           allChunks.size(), fileUrl);
                try {
                    completedFilename = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
                    if (completedFilename.contains("?")) {
                        completedFilename = completedFilename.substring(0, completedFilename.indexOf('?'));
                    }
                } catch (Exception ignored) {}
            }
            
            logger.info("Streaming temp file processing completed successfully for file: {} ({} chunks)", 
                       fileUrl, allChunks.size());
            
            // Mark file as completed and emit FILE_PROCESSED event
            if (monitorService != null) {
                monitorService.incrementFilesProcessed();
                if (completedFilename != null) {
                    monitorService.publishEvent("FILE_COMPLETE", completedFilename);
                }
                monitorService.setCurrentFile(null); // Clear current file
            }
            
        } catch (Exception e) {
            logger.error("Error in streaming temp file processing for file {}: {}", fileUrl, e.getMessage(), e);
            
            // Track error in monitoring
            if (monitorService != null) {
                monitorService.setLastError("Processing file " + fileUrl + ": " + e.getMessage());
                monitorService.setCurrentFile(null); // Clear current file on error
            }
        } finally {
            // Clean up temp file
            if (tempFile != null && tempFile.exists()) {
                try {
                    tempFile.delete();
                    logger.info("Cleaned up temp file: {}", tempFile.getAbsolutePath());
                } catch (Exception e) {
                    logger.warn("Failed to clean up temp file: {}", e.getMessage());
                }
            }
        }
        
        return CompletableFuture.completedFuture(null);
    }

    @Bean
    public Consumer<String> embedProc() {
        logger.info("Creating embedProc function bean with work limiting: max {} concurrent files", 
                   maxConcurrentFiles);
        return message -> {
            try {
                if (message == null || message.trim().isEmpty()) {
                    logger.warn("Received empty message, skipping...");
                    return;
                }

                // Check if processing is enabled
                if (!processingStateService.isProcessingEnabled()) {
                    logger.info("Processing is disabled, leaving message in queue for later processing: {}", 
                               message.substring(0, Math.min(50, message.length())) + "...");
                    // Don't acknowledge the message - let it stay in queue
                    return;
                }

                // Check if we're already processing too much work
                int currentProcessing = activeProcessingCount.get();
                if (currentProcessing >= maxConcurrentFiles) {
                    logger.warn("Instance at capacity ({} active), rejecting message to allow other instances to process", currentProcessing);
                    // Don't acknowledge the message - let it go to another instance
                    return;
                }

                logger.info("embedProc function invoked with message: {} (active processing: {}/{})", 
                           message.substring(0, Math.min(50, message.length())) + "...", 
                           currentProcessing, maxConcurrentFiles);

                // Extract file URL from the message
                String fileUrl = extractFileUrl(message);
                if (fileUrl == null || fileUrl.isEmpty()) {
                    logger.warn("No valid file URL found in message: {}", message);
                    return;
                }

                // Try to acquire processing permit
                if (!processingSemaphore.tryAcquire()) {
                    logger.warn("No processing permits available, rejecting message to allow other instances to process");
                    return;
                }

                try {
                    activeProcessingCount.incrementAndGet();
                    logger.info("Extracted file URL: {}. Processing asynchronously (active: {}/{})", 
                               fileUrl, activeProcessingCount.get(), maxConcurrentFiles);
                    
                    // Process the file asynchronously with work limiting
                    processFileStreamingFromTemp(fileUrl);
                    
                    logger.info("embedProc function completed successfully - file processing asynchronously");
                    
                } finally {
                    activeProcessingCount.decrementAndGet();
                    processingSemaphore.release();
                }
                
            } catch (Exception e) {
                logger.error("Error processing document: {}", e.getMessage(), e);
                // Don't throw exception to allow message acknowledgment
            }
        };
    }

    @Async
    public CompletableFuture<Void> processFileAsync(String fileUrl) {
        try {
            logger.info("Starting async file processing for: {}", fileUrl);
            
            // Fetch file content using FileDownloaderService
            String fileContent = fileDownloaderService.fetchFileContent(fileUrl);
            if (fileContent == null || fileContent.isEmpty()) {
                logger.warn("No content found in file: {}", fileUrl);
                return CompletableFuture.completedFuture(null);
            }

            logger.info("Processing document of length: {} characters from file: {}", fileContent.length(), fileUrl);
            
            // Enhanced chunking with semantic boundaries
            List<String> allChunks = textChunkingService.chunkTextEnhanced(fileContent);
            logger.info("Created {} chunks from file: {} using TextChunkingService", allChunks.size(), fileUrl);
            
            if (allChunks.isEmpty()) {
                logger.warn("No chunks generated from the input text");
                return CompletableFuture.completedFuture(null);
            }

            // Store embeddings using the EmbeddingService with parallel processing
            if (useReferenceNumbers) {
                // Extract reference numbers from file URL
                ReferenceNumbers refNumbers = extractReferenceNumbersFromFileUrl(fileUrl);
                Integer refnum1 = refNumbers != null ? refNumbers.refnum1 : defaultRefnum1;
                Integer refnum2 = refNumbers != null ? refNumbers.refnum2 : defaultRefnum2;
                
                if (refNumbers != null) {
                    logger.info("Using reference numbers from filename - refnum1: {}, refnum2: {}", refnum1, refnum2);
                } else {
                    logger.info("Using default reference numbers - refnum1: {}, refnum2: {}", refnum1, refnum2);
                }
                
                // Convert to TextWithMetadata and use embedding service with metadata
                DocumentType documentType = DocumentType.fromUrl(fileUrl);
                List<EmbeddingService.TextWithMetadata> metadataChunks = allChunks.stream()
                    .map(text -> {
                        // For reference documents, don't include refnums
                        if (documentType == DocumentType.REFERENCE) {
                            return new EmbeddingService.TextWithMetadata(text, null, null, documentType, fileUrl);
                        } else {
                            return new EmbeddingService.TextWithMetadata(text, refnum1, refnum2, documentType, fileUrl);
                        }
                    })
                    .collect(java.util.stream.Collectors.toList());
                embeddingService.storeEmbeddingsWithMetadataParallel(metadataChunks);
            } else {
                embeddingService.storeEmbeddingsParallel(allChunks);
            }
            
            // Optionally run query after embedding if queryText is set and hasn't run yet
            if (queryText != null && !queryText.isBlank() && queryRun.compareAndSet(false, true)) {
                vectorQueryProcessor.runQuery(queryText, 5);
            }
            
            logger.info("Async file processing completed successfully for file: {}", fileUrl);
            
        } catch (Exception e) {
            logger.error("Error in async file processing for file {}: {}", fileUrl, e.getMessage(), e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Process file in streaming fashion to reduce memory usage and improve responsiveness
     */
    @Async
    public CompletableFuture<Void> processFileStreaming(String fileUrl) {
        try {
            logger.info("Starting streaming file processing for: {}", fileUrl);
            
            // Fetch file content using FileDownloaderService
            String fileContent = fileDownloaderService.fetchFileContent(fileUrl);
            if (fileContent == null || fileContent.isEmpty()) {
                logger.warn("No content found in file: {}", fileUrl);
                return CompletableFuture.completedFuture(null);
            }

            logger.info("Processing document of length: {} characters from file: {}", fileContent.length(), fileUrl);
            
            // Process in smaller chunks for better responsiveness
            int streamingChunkSize = 500; // Process 500 chunks at a time
            List<String> allChunks = textChunkingService.chunkTextEnhanced(fileContent);
            logger.info("Created {} total chunks from file: {} using TextChunkingService", allChunks.size(), fileUrl);
            
            if (allChunks.isEmpty()) {
                logger.warn("No chunks generated from the input text");
                return CompletableFuture.completedFuture(null);
            }

            // Process chunks in streaming batches
            for (int i = 0; i < allChunks.size(); i += streamingChunkSize) {
                int endIndex = Math.min(i + streamingChunkSize, allChunks.size());
                List<String> batch = allChunks.subList(i, endIndex);
                
                logger.info("Processing batch {}/{} (chunks {}-{})", 
                    (i / streamingChunkSize) + 1, 
                    (allChunks.size() + streamingChunkSize - 1) / streamingChunkSize,
                    i + 1, endIndex);
                
                // Store embeddings for this batch
                if (useReferenceNumbers) {
                    // Extract reference numbers from file URL
                    ReferenceNumbers refNumbers = extractReferenceNumbersFromFileUrl(fileUrl);
                    Integer refnum1 = refNumbers != null ? refNumbers.refnum1 : defaultRefnum1;
                    Integer refnum2 = refNumbers != null ? refNumbers.refnum2 : defaultRefnum2;
                    
                    // Convert to TextWithMetadata and use embedding service with metadata
                    DocumentType documentType = DocumentType.fromUrl(fileUrl);
                    List<EmbeddingService.TextWithMetadata> metadataBatch = batch.stream()
                        .map(text -> {
                            // For reference documents, don't include refnums
                            if (documentType == DocumentType.REFERENCE) {
                                return new EmbeddingService.TextWithMetadata(text, null, null, documentType, fileUrl);
                            } else {
                                return new EmbeddingService.TextWithMetadata(text, refnum1, refnum2, documentType, fileUrl);
                            }
                        })
                        .collect(java.util.stream.Collectors.toList());
                    embeddingService.storeEmbeddingsWithMetadataParallel(metadataBatch);
                } else {
                    embeddingService.storeEmbeddingsParallel(batch);
                }
                
                // Small delay to prevent overwhelming the system
                Thread.sleep(100);
            }
            
            // Optionally run query after embedding if queryText is set and hasn't run yet
            if (queryText != null && !queryText.isBlank() && queryRun.compareAndSet(false, true)) {
                vectorQueryProcessor.runQuery(queryText, 5);
            }
            
            logger.info("Streaming file processing completed successfully for file: {}", fileUrl);
            
        } catch (Exception e) {
            logger.error("Error in streaming file processing for file {}: {}", fileUrl, e.getMessage(), e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Process file with work limiting to prevent single instance from taking too much work
     */
    @Async
    public CompletableFuture<Void> processFileWithLimiting(String fileUrl) {
        try {
            logger.info("Starting work-limited file processing for: {}", fileUrl);
            
            // Fetch file content using FileDownloaderService
            String fileContent = fileDownloaderService.fetchFileContent(fileUrl);
            if (fileContent == null || fileContent.isEmpty()) {
                logger.warn("No content found in file: {}", fileUrl);
                return CompletableFuture.completedFuture(null);
            }

            logger.info("Processing document of length: {} characters from file: {}", fileContent.length(), fileUrl);
            
            // Enhanced chunking with semantic boundaries
            List<String> allChunks = textChunkingService.chunkTextEnhanced(fileContent);
            logger.info("Created {} chunks from file: {} using TextChunkingService", allChunks.size(), fileUrl);
            
            if (allChunks.isEmpty()) {
                logger.warn("No chunks generated from the input text");
                return CompletableFuture.completedFuture(null);
            }

            // Process chunks in smaller batches to be more responsive
            int batchSize = 100; // Fixed batch size for consistency
            int totalBatches = (allChunks.size() + batchSize - 1) / batchSize;
            
            logger.info("Processing {} chunks in {} batches of size {}", allChunks.size(), totalBatches, batchSize);
            
            for (int i = 0; i < allChunks.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, allChunks.size());
                List<String> batch = allChunks.subList(i, endIndex);
                
                logger.info("Processing batch {}/{} (chunks {}-{})", 
                    (i / batchSize) + 1, totalBatches, i + 1, endIndex);
                
                // Store embeddings for this batch using parallel processing
                if (useReferenceNumbers) {
                    // Convert to TextWithMetadata and use embedding service with metadata
                    DocumentType documentType = DocumentType.fromUrl(fileUrl);
                    List<EmbeddingService.TextWithMetadata> metadataBatch = batch.stream()
                        .map(text -> {
                            // For reference documents, don't include refnums
                            if (documentType == DocumentType.REFERENCE) {
                                return new EmbeddingService.TextWithMetadata(text, null, null, documentType, fileUrl);
                            } else {
                                return new EmbeddingService.TextWithMetadata(text, defaultRefnum1, defaultRefnum2, documentType, fileUrl);
                            }
                        })
                        .collect(java.util.stream.Collectors.toList());
                    embeddingService.storeEmbeddingsWithMetadataParallel(metadataBatch);
                } else {
                    embeddingService.storeEmbeddingsParallel(batch);
                }
                
                // Small delay to prevent overwhelming the system
                Thread.sleep(50);
            }
            
            // Optionally run query after embedding if queryText is set and hasn't run yet
            if (queryText != null && !queryText.isBlank() && queryRun.compareAndSet(false, true)) {
                vectorQueryProcessor.runQuery(queryText, 5);
            }
            
            logger.info("Work-limited file processing completed successfully for file: {}", fileUrl);
            
        } catch (Exception e) {
            logger.error("Error in work-limited file processing for file {}: {}", fileUrl, e.getMessage(), e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Extract reference numbers from file URL by getting the filename
     * @param fileUrl The file URL to extract filename from
     * @return ReferenceNumbers object or null if parsing fails
     */
    private ReferenceNumbers extractReferenceNumbersFromFileUrl(String fileUrl) {
        try {
            // Extract filename from URL
            String filename;
            if (fileUrl.contains("/")) {
                filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            } else {
                filename = fileUrl;
            }
            
            // Remove URL parameters if any
            if (filename.contains("?")) {
                filename = filename.substring(0, filename.indexOf("?"));
            }
            
            return extractReferenceNumbersFromFilename(filename);
            
        } catch (Exception e) {
            logger.error("Unexpected error extracting filename from URL: {}. Error: {}", fileUrl, e.getMessage());
            return null;
        }
    }

    /**
     * Extract reference numbers from filename pattern: <refnum1>-<refnum2>.txt
     * @param filename The filename to parse
     * @return ReferenceNumbers object or null if parsing fails
     */
    private ReferenceNumbers extractReferenceNumbersFromFilename(String filename) {
        try {
            // Remove .txt extension
            if (!filename.toLowerCase().endsWith(".txt")) {
                logger.warn("File does not have .txt extension: {}", filename);
                return null;
            }
            
            String nameWithoutTxt = filename.substring(0, filename.length() - 4);
            
            // Handle double extensions like .pdf.txt - remove any remaining extension
            String nameWithoutExtensions = nameWithoutTxt;
            int lastDotIndex = nameWithoutTxt.lastIndexOf('.');
            if (lastDotIndex > 0) {
                nameWithoutExtensions = nameWithoutTxt.substring(0, lastDotIndex);
            }
            
            // Split by dash to get refnum1 and refnum2
            String[] parts = nameWithoutExtensions.split("-");
            if (parts.length != 2) {
                logger.warn("Filename does not match pattern <refnum1>-<refnum2>.[ext].txt: {}", filename);
                return null;
            }
            
            // Parse the reference numbers
            Integer refnum1 = Integer.parseInt(parts[0].trim());
            Integer refnum2 = Integer.parseInt(parts[1].trim());
            
            // Validate that they are 6-digit numbers
            if (refnum1 < 100000 || refnum1 > 999999 || refnum2 < 100000 || refnum2 > 999999) {
                logger.warn("Reference numbers must be 6-digit integers (100000-999999). Got refnum1: {}, refnum2: {} from filename: {}", 
                           refnum1, refnum2, filename);
                return null;
            }
            
            logger.debug("Extracted reference numbers from filename {}: refnum1={}, refnum2={}", filename, refnum1, refnum2);
            logger.info("Successfully parsed reference numbers from filename '{}': refnum1={}, refnum2={}", filename, refnum1, refnum2);
            return new ReferenceNumbers(refnum1, refnum2);
            
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse reference numbers from filename: {}. Error: {}", filename, e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error parsing filename: {}. Error: {}", filename, e.getMessage());
            return null;
        }
    }

    /**
     * Simple data class to hold reference numbers
     */
    private static class ReferenceNumbers {
        final Integer refnum1;
        final Integer refnum2;
        
        ReferenceNumbers(Integer refnum1, Integer refnum2) {
            this.refnum1 = refnum1;
            this.refnum2 = refnum2;
        }
    }
}
