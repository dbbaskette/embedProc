package com.baskettecase.embedProc.processor;
    
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.baskettecase.embedProc.service.EmbeddingService;
import com.baskettecase.embedProc.service.MonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Semaphore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Configuration
@Profile("cloud")
@EnableAsync
public class ScdfStreamProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(ScdfStreamProcessor.class);
    
    private final EmbeddingService embeddingService;
    private final VectorQueryProcessor vectorQueryProcessor;
    private final MonitorService monitorService;
    private final String queryText;
    private final AtomicBoolean queryRun = new AtomicBoolean(false);
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final int maxWordsPerChunk;
    private final int overlapWords;
    private final int minMeaningfulWords;
    
    // Work limiting to prevent single instance from taking too much work
    private final AtomicInteger activeProcessingCount = new AtomicInteger(0);
    private final Semaphore processingSemaphore;
    private final int maxConcurrentFiles;

    @Autowired
    public ScdfStreamProcessor(EmbeddingService embeddingService, VectorQueryProcessor vectorQueryProcessor, 
                             MonitorService monitorService,
                             @Value("${app.query.text:}") String queryText,
                             @Value("${app.chunking.max-words-per-chunk:1000}") int maxWordsPerChunk,
                             @Value("${app.chunking.overlap-words:150}") int overlapWords,
                             @Value("${app.chunking.min-meaningful-words:100}") int minMeaningfulWords,
                             @Value("${app.processing.max-concurrent-files:2}") int maxConcurrentFiles,
                             ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.embeddingService = embeddingService;
        this.vectorQueryProcessor = vectorQueryProcessor;
        this.monitorService = monitorService;
        this.queryText = queryText;
        this.maxWordsPerChunk = maxWordsPerChunk;
        this.overlapWords = overlapWords;
        this.minMeaningfulWords = minMeaningfulWords;
        this.maxConcurrentFiles = maxConcurrentFiles;
        this.processingSemaphore = new Semaphore(maxConcurrentFiles);
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void logBeanCreation() {
        logger.info("ScdfStreamProcessor bean created for SCDF deployment");
    }

    private List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        // Use a scanner to process the text more efficiently
        java.util.Scanner scanner = new java.util.Scanner(text);
        int currentChunkSize = 0;
        
        // Buffer for the current chunk
        StringBuilder chunkBuilder = new StringBuilder();
        
        while (scanner.hasNext()) {
            String word = scanner.next();
            
            if (currentChunkSize > 0) {
                chunkBuilder.append(' ');
            }
            chunkBuilder.append(word);
            currentChunkSize++;
            
            // If we've reached the chunk size, add it to the results
            if (currentChunkSize >= chunkSize) {
                chunks.add(chunkBuilder.toString());
                
                // Handle overlap by moving back 'overlap' number of words
                if (overlap > 0 && overlap < chunkSize) {
                    // Reset the builder and add the overlap words
                    String currentChunk = chunkBuilder.toString();
                    String[] words = currentChunk.split("\\s+");
                    chunkBuilder.setLength(0);
                    
                    // Start the next chunk with the overlapping words
                    int overlapStart = Math.max(0, words.length - overlap);
                    for (int i = overlapStart; i < words.length; i++) {
                        if (i > overlapStart) {
                            chunkBuilder.append(' ');
                        }
                        chunkBuilder.append(words[i]);
                    }
                    currentChunkSize = words.length - overlapStart;
                } else {
                    chunkBuilder.setLength(0);
                    currentChunkSize = 0;
                }
            }
        }
        
        // Add the last chunk if it's not empty
        if (currentChunkSize > 0) {
            chunks.add(chunkBuilder.toString());
        }
        
        scanner.close();
        return chunks;
    }

    /**
     * Enhanced chunking with semantic boundaries for better Q&A context
     * Creates larger chunks (800-1200 words) with meaningful overlap
     */
    private List<String> chunkTextEnhanced(String text, int maxWordsPerChunk, int overlapWords) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        // Split text into paragraphs first for semantic chunking
        String[] paragraphs = text.split("\\n\\s*\\n");
        
        // Combine short paragraphs to create more meaningful chunks
        StringBuilder currentChunkBuilder = new StringBuilder();
        int currentWordCount = 0;
        
        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                continue;
            }
            
            // Count meaningful words (ignore excessive whitespace)
            int paragraphWordCount = countMeaningfulWords(paragraph);
            
            // If adding this paragraph would exceed max chunk size, finalize current chunk
            if (currentWordCount + paragraphWordCount > maxWordsPerChunk && currentWordCount > 0) {
                String chunk = currentChunkBuilder.toString().trim();
                if (countMeaningfulWords(chunk) >= minMeaningfulWords) { // Use configurable minimum
                    chunks.add(chunk);
                }
                currentChunkBuilder.setLength(0);
                currentWordCount = 0;
            }
            
            // Add paragraph to current chunk
            if (currentWordCount > 0) {
                currentChunkBuilder.append("\n\n");
            }
            currentChunkBuilder.append(paragraph.trim());
            currentWordCount += paragraphWordCount;
            
            // If current chunk is large enough, finalize it
            if (currentWordCount >= maxWordsPerChunk) {
                String chunk = currentChunkBuilder.toString().trim();
                if (countMeaningfulWords(chunk) >= minMeaningfulWords) { // Use configurable minimum
                    chunks.add(chunk);
                }
                currentChunkBuilder.setLength(0);
                currentWordCount = 0;
            }
        }
        
        // Add remaining content as final chunk if it's substantial
        if (currentWordCount > 0) {
            String chunk = currentChunkBuilder.toString().trim();
            if (countMeaningfulWords(chunk) >= minMeaningfulWords) { // Use configurable minimum
                chunks.add(chunk);
            }
        }
        
        return chunks;
    }
    
    /**
     * Count meaningful words, ignoring excessive whitespace and empty lines
     */
    private int countMeaningfulWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        
        // Split into words and filter out empty strings and whitespace-only strings
        String[] words = text.trim().split("\\s+");
        int meaningfulWordCount = 0;
        
        for (String word : words) {
            // Only count words that have actual content (not just spaces, punctuation, etc.)
            if (word != null && !word.trim().isEmpty() && word.matches(".*[a-zA-Z0-9].*")) {
                meaningfulWordCount++;
            }
        }
        
        return meaningfulWordCount;
    }
    


    private String fetchFileContent(String fileUrl) {
        try {
            // Reduced logging to prevent log rate limits
            
            // Check if this is a WebHDFS URL
            boolean isWebHdfs = fileUrl.contains("/webhdfs/");
            
            if (isWebHdfs) {
                // Reduced logging to prevent log rate limits
                return fetchWebHdfsContent(fileUrl);
            } else {
                // Regular HTTP URL handling
                ResponseEntity<String> response = restTemplate.getForEntity(fileUrl, String.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    String content = response.getBody();
                    // Reduced logging to prevent log rate limits
                    return content;
                } else {
                    logger.error("Failed to fetch file content from URL: {}. Status: {}", fileUrl, response.getStatusCode());
                    return null; // Return null instead of throwing exception
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching file content from URL: {}. Error: {}", fileUrl, e.getMessage());
            return null; // Return null instead of throwing exception
        }
    }

    private String fetchWebHdfsContent(String webHdfsUrl) {
        try {
            // Reduced logging to prevent log rate limits
            
            // WebHDFS requires specific headers and may need to follow redirects
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "embedProc/1.0");
            
            // Create HTTP entity with headers
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            
            // Create URI to prevent RestTemplate from re-encoding the URL
            java.net.URI uri = new java.net.URI(webHdfsUrl);
            
            // Make the request using URI instead of String to prevent re-encoding
            ResponseEntity<String> response = restTemplate.exchange(uri, org.springframework.http.HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                String content = response.getBody();
                // Reduced logging to prevent log rate limits
                return content;
            } else {
                logger.error("Failed to fetch WebHDFS content. Status: {}, Response: {}", response.getStatusCode(), response.getBody());
                return null; // Return null instead of throwing exception
            }
        } catch (Exception e) {
            logger.error("Error fetching WebHDFS content from URL: {}. Error: {}", webHdfsUrl, e.getMessage());
            return null; // Return null instead of throwing exception
        }
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
            
            // Fix WebHDFS URL encoding and add operation parameter
            fileUrl = fixWebHdfsUrl(fileUrl);
            
            return fileUrl;
            
        } catch (Exception e) {
            logger.warn("Failed to parse message as JSON: {}", e.getMessage());
            return null;
        }
    }

    private String fixWebHdfsUrl(String url) {
        try {
            // Check if this is a WebHDFS URL
            if (url.contains("/webhdfs/")) {
                // Reduced logging to prevent log rate limits
                
                // Handle double-encoding but preserve URL encoding for WebHDFS
                String processedUrl = url;
                
                // Check for double-encoding (e.g., %2520 instead of %20)
                if (url.contains("%25")) {
                    // Decode only the double-encoded parts
                    processedUrl = url.replace("%25", "%");
                    // Reduced logging to prevent log rate limits
                }
                
                // Remove any existing query parameters
                String baseUrl = processedUrl;
                if (processedUrl.contains("?")) {
                    baseUrl = processedUrl.substring(0, processedUrl.indexOf("?"));
                }
                
                // Add the required WebHDFS operation parameter
                String fixedUrl = baseUrl + "?op=OPEN";
                // Reduced logging to prevent log rate limits
                
                return fixedUrl;
            } else {
                // Not a WebHDFS URL, return as-is
                return url;
            }
        } catch (Exception e) {
            logger.error("Error fixing WebHDFS URL: {}. Error: {}", url, e.getMessage());
            // Return original URL if fixing fails
            return url;
        }
    }

    /**
     * Download file to temp storage and process in streaming fashion
     * This reduces memory usage for large files
     */
    private File downloadFileToTemp(String fileUrl) {
        try {
            // Create temp file
            File tempFile = File.createTempFile("embedproc_", ".txt");
            tempFile.deleteOnExit(); // Clean up on JVM exit
            
            logger.info("Downloading file to temp: {} -> {}", fileUrl, tempFile.getAbsolutePath());
            
            // Check if this is a WebHDFS URL
            boolean isWebHdfs = fileUrl.contains("/webhdfs/");
            
            if (isWebHdfs) {
                // WebHDFS download
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("User-Agent", "embedProc/1.0");
                org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
                java.net.URI uri = new java.net.URI(fileUrl);
                
                ResponseEntity<byte[]> response = restTemplate.exchange(uri, org.springframework.http.HttpMethod.GET, entity, byte[].class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    byte[] content = response.getBody();
                    Files.write(tempFile.toPath(), content);
                    logger.info("Downloaded {} bytes to temp file", content.length);
                    return tempFile;
                } else {
                    logger.error("Failed to download WebHDFS file. Status: {}", response.getStatusCode());
                    return null;
                }
            } else {
                // Regular HTTP download
                ResponseEntity<byte[]> response = restTemplate.getForEntity(fileUrl, byte[].class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    byte[] content = response.getBody();
                    Files.write(tempFile.toPath(), content);
                    logger.info("Downloaded {} bytes to temp file", content.length);
                    return tempFile;
                } else {
                    logger.error("Failed to download file. Status: {}", response.getStatusCode());
                    return null;
                }
            }
            
        } catch (Exception e) {
            logger.error("Error downloading file to temp: {}", e.getMessage());
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
            
            // Download file to temp storage
            tempFile = downloadFileToTemp(fileUrl);
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
            List<String> allChunks = chunkTextEnhanced(fileContent, maxWordsPerChunk, overlapWords);
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
                embeddingService.storeEmbeddingsParallel(batch);
                
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
            if (monitorService != null) {
                logger.info("File processing completed: {} chunks processed for file: {}", 
                           allChunks.size(), fileUrl);
            }
            
            logger.info("Streaming temp file processing completed successfully for file: {} ({} chunks)", 
                       fileUrl, allChunks.size());
            
        } catch (Exception e) {
            logger.error("Error in streaming temp file processing for file {}: {}", fileUrl, e.getMessage(), e);
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
            
            // Fetch file content
            String fileContent = fetchFileContent(fileUrl);
            if (fileContent == null || fileContent.isEmpty()) {
                logger.warn("No content found in file: {}", fileUrl);
                return CompletableFuture.completedFuture(null);
            }

            logger.info("Processing document of length: {} characters from file: {}", fileContent.length(), fileUrl);
            
            // Enhanced chunking with semantic boundaries
            List<String> allChunks = chunkTextEnhanced(fileContent, maxWordsPerChunk, overlapWords);
            logger.info("Created {} chunks from file: {} (avg {} meaningful words per chunk)", 
                       allChunks.size(), fileUrl, 
                       allChunks.isEmpty() ? 0 : allChunks.stream()
                           .mapToInt(chunk -> countMeaningfulWords(chunk))
                           .average().orElse(0.0));
            
            // Log chunk quality statistics
            if (!allChunks.isEmpty()) {
                int minWords = allChunks.stream().mapToInt(chunk -> countMeaningfulWords(chunk)).min().orElse(0);
                int maxWords = allChunks.stream().mapToInt(chunk -> countMeaningfulWords(chunk)).max().orElse(0);
                logger.info("Chunk quality - Min: {} meaningful words, Max: {} meaningful words, Target: {} words", 
                           minWords, maxWords, maxWordsPerChunk);
            }
            
            if (allChunks.isEmpty()) {
                logger.warn("No chunks generated from the input text");
                return CompletableFuture.completedFuture(null);
            }

            // Store embeddings using the EmbeddingService with parallel processing
            embeddingService.storeEmbeddingsParallel(allChunks);
            
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
            
            // Fetch file content
            String fileContent = fetchFileContent(fileUrl);
            if (fileContent == null || fileContent.isEmpty()) {
                logger.warn("No content found in file: {}", fileUrl);
                return CompletableFuture.completedFuture(null);
            }

            logger.info("Processing document of length: {} characters from file: {}", fileContent.length(), fileUrl);
            
            // Process in smaller chunks for better responsiveness
            int streamingChunkSize = 500; // Process 500 chunks at a time
            List<String> allChunks = chunkTextEnhanced(fileContent, maxWordsPerChunk, overlapWords);
            logger.info("Created {} total chunks from file: {} (avg {} meaningful words per chunk)", 
                       allChunks.size(), fileUrl,
                       allChunks.isEmpty() ? 0 : allChunks.stream()
                           .mapToInt(chunk -> countMeaningfulWords(chunk))
                           .average().orElse(0.0));
            
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
                embeddingService.storeEmbeddingsParallel(batch);
                
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
            
            // Fetch file content
            String fileContent = fetchFileContent(fileUrl);
            if (fileContent == null || fileContent.isEmpty()) {
                logger.warn("No content found in file: {}", fileUrl);
                return CompletableFuture.completedFuture(null);
            }

            logger.info("Processing document of length: {} characters from file: {}", fileContent.length(), fileUrl);
            
            // Enhanced chunking with semantic boundaries
            List<String> allChunks = chunkTextEnhanced(fileContent, maxWordsPerChunk, overlapWords);
            logger.info("Created {} chunks from file: {} (avg {} meaningful words per chunk)", 
                       allChunks.size(), fileUrl,
                       allChunks.isEmpty() ? 0 : allChunks.stream()
                           .mapToInt(chunk -> countMeaningfulWords(chunk))
                           .average().orElse(0.0));
            
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
                embeddingService.storeEmbeddingsParallel(batch);
                
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
}

