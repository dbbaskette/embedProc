package com.baskettecase.embedProc.processor;
    
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.baskettecase.embedProc.service.EmbeddingService;
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

@Configuration
@Profile("cloud")
@EnableAsync
public class ScdfStreamProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(ScdfStreamProcessor.class);
    
    private final EmbeddingService embeddingService;
    private final VectorQueryProcessor vectorQueryProcessor;
    private final String queryText;
    private final AtomicBoolean queryRun = new AtomicBoolean(false);
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final int maxWordsPerChunk;
    private final int overlapWords;

    @Autowired
    public ScdfStreamProcessor(EmbeddingService embeddingService, VectorQueryProcessor vectorQueryProcessor, 
                             @Value("${app.query.text:}") String queryText,
                             @Value("${app.chunking.max-words-per-chunk:300}") int maxWordsPerChunk,
                             @Value("${app.chunking.overlap-words:30}") int overlapWords,
                             ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.embeddingService = embeddingService;
        this.vectorQueryProcessor = vectorQueryProcessor;
        this.queryText = queryText;
        this.maxWordsPerChunk = maxWordsPerChunk;
        this.overlapWords = overlapWords;
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
     * Enhanced chunking with semantic boundaries and configurable strategies
     * Supports smaller chunks (200-500 words) for precise matches with overlap
     */
    private List<String> chunkTextEnhanced(String text, int maxWordsPerChunk, int overlapWords) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        // Split text into paragraphs first for semantic chunking
        String[] paragraphs = text.split("\\n\\s*\\n");
        
        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                continue;
            }
            
            // If paragraph is smaller than max chunk size, add it as a single chunk
            String[] words = paragraph.trim().split("\\s+");
            if (words.length <= maxWordsPerChunk) {
                chunks.add(paragraph.trim());
                continue;
            }
            
            // Split large paragraphs into smaller chunks with overlap
            List<String> paragraphChunks = chunkParagraph(paragraph, maxWordsPerChunk, overlapWords);
            chunks.addAll(paragraphChunks);
        }
        
        return chunks;
    }
    
    /**
     * Chunk a single paragraph with word-based splitting and overlap
     */
    private List<String> chunkParagraph(String paragraph, int maxWordsPerChunk, int overlapWords) {
        List<String> chunks = new ArrayList<>();
        String[] words = paragraph.trim().split("\\s+");
        
        if (words.length <= maxWordsPerChunk) {
            chunks.add(paragraph.trim());
            return chunks;
        }
        
        int stepSize = maxWordsPerChunk - overlapWords;
        if (stepSize <= 0) {
            stepSize = maxWordsPerChunk / 2; // Fallback if overlap is too large
        }
        
        for (int startIndex = 0; startIndex < words.length; startIndex += stepSize) {
            int endIndex = Math.min(startIndex + maxWordsPerChunk, words.length);
            
            // Build the chunk
            StringBuilder chunkBuilder = new StringBuilder();
            for (int i = startIndex; i < endIndex; i++) {
                if (i > startIndex) {
                    chunkBuilder.append(' ');
                }
                chunkBuilder.append(words[i]);
            }
            
            chunks.add(chunkBuilder.toString());
            
            // If we've reached the end, break
            if (endIndex >= words.length) {
                break;
            }
        }
        
        return chunks;
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
            // Handle the specific format from textProc: "Processed file written to HDFS: http://..."
            if (message != null && message.trim().startsWith("Processed file written to HDFS:")) {
                String fileUrl = message.trim().substring("Processed file written to HDFS:".length()).trim();
                
                if (!fileUrl.isEmpty()) {
                    // Fix WebHDFS URL encoding and add operation parameter
                    fileUrl = fixWebHdfsUrl(fileUrl);
                    logger.info("Extracted file URL: {}", fileUrl);
                    return fileUrl;
                }
            }
            
            // Try to parse as JSON first (fallback for other formats)
            JsonNode jsonNode = objectMapper.readTree(message);
            
            // Look for common field names that might contain the file URL
            String fileUrl = null;
            if (jsonNode.has("fileUrl")) {
                fileUrl = jsonNode.get("fileUrl").asText();
            } else if (jsonNode.has("url")) {
                fileUrl = jsonNode.get("url").asText();
            } else if (jsonNode.has("file_url")) {
                fileUrl = jsonNode.get("file_url").asText();
            } else if (jsonNode.has("content")) {
                // If the message has a content field, use that directly
                return jsonNode.get("content").asText();
            } else {
                // If no recognized field, assume the entire message is the URL
                fileUrl = message.trim();
            }
            
            if (fileUrl == null || fileUrl.trim().isEmpty()) {
                logger.warn("No file URL found in message");
                return null;
            }
            
            // Fix WebHDFS URL encoding and add operation parameter
            fileUrl = fixWebHdfsUrl(fileUrl);
            
            return fileUrl;
            
        } catch (Exception e) {
            logger.warn("Failed to parse message as expected format: {}", e.getMessage());
            
            // If all parsing fails, assume the message is the content itself
            logger.warn("Message is not in expected format, treating as direct content");
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

    @Bean
    public Consumer<String> embedProc() {
        logger.info("Creating embedProc function bean");
        return message -> {
            try {
                if (message == null || message.trim().isEmpty()) {
                    logger.warn("Received empty message, skipping...");
                    return;
                }

                logger.info("embedProc function invoked with message: {}", message.substring(0, Math.min(50, message.length())) + "...");

                // Extract file URL from the message
                String fileUrl = extractFileUrl(message);
                if (fileUrl == null || fileUrl.isEmpty()) {
                    logger.warn("No valid file URL found in message: {}", message);
                    return;
                }

                logger.info("Extracted file URL: {}. Processing asynchronously to allow immediate acknowledgment.", fileUrl);
                
                // Process the entire file asynchronously - this allows immediate acknowledgment
                processFileAsync(fileUrl);
                
                logger.info("embedProc function completed successfully - file processing asynchronously");
                
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
            List<String> chunks = chunkTextEnhanced(fileContent, maxWordsPerChunk, overlapWords);
            logger.info("Created {} chunks from file: {}", chunks.size(), fileUrl);
            
            if (chunks.isEmpty()) {
                logger.warn("No chunks generated from the input text");
                return CompletableFuture.completedFuture(null);
            }

            // Store embeddings using the EmbeddingService
            embeddingService.storeEmbeddings(chunks);
            
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
}

