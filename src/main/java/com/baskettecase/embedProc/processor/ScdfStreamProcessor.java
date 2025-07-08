package com.baskettecase.embedProc.processor;
    
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.baskettecase.embedProc.service.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicBoolean;

@Configuration
@Profile("cloud")
public class ScdfStreamProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(ScdfStreamProcessor.class);
    
    private final EmbeddingService embeddingService;
    private final VectorQueryProcessor vectorQueryProcessor;
    private final String queryText;
    private final AtomicBoolean queryRun = new AtomicBoolean(false);
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Autowired
    public ScdfStreamProcessor(EmbeddingService embeddingService, VectorQueryProcessor vectorQueryProcessor, 
                             @Value("${app.query.text:}") String queryText,
                             ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.embeddingService = embeddingService;
        this.vectorQueryProcessor = vectorQueryProcessor;
        this.queryText = queryText;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
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

    private String fetchFileContent(String fileUrl) {
        try {
            logger.info("Fetching file content from URL: {}", fileUrl);
            
            // Check if this is a WebHDFS URL
            boolean isWebHdfs = fileUrl.contains("/webhdfs/");
            
            if (isWebHdfs) {
                logger.info("Detected WebHDFS URL, using specialized handling");
                return fetchWebHdfsContent(fileUrl);
            } else {
                // Regular HTTP URL handling
                ResponseEntity<String> response = restTemplate.getForEntity(fileUrl, String.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    String content = response.getBody();
                    logger.info("Successfully fetched file content, length: {} characters", content != null ? content.length() : 0);
                    return content;
                } else {
                    logger.error("Failed to fetch file content from URL: {}. Status: {}", fileUrl, response.getStatusCode());
                    throw new RuntimeException("Failed to fetch file content, HTTP status: " + response.getStatusCode());
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching file content from URL: {}. Error: {}", fileUrl, e.getMessage());
            throw new RuntimeException("Failed to fetch file content from URL: " + fileUrl, e);
        }
    }

    private String fetchWebHdfsContent(String webHdfsUrl) {
        try {
            logger.info("Fetching content from WebHDFS URL: {}", webHdfsUrl);
            
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
                logger.info("Successfully fetched WebHDFS content, length: {} characters", content != null ? content.length() : 0);
                return content;
            } else {
                logger.error("Failed to fetch WebHDFS content. Status: {}, Response: {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to fetch WebHDFS content, HTTP status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error fetching WebHDFS content from URL: {}. Error: {}", webHdfsUrl, e.getMessage());
            throw new RuntimeException("Failed to fetch WebHDFS content from URL: " + webHdfsUrl, e);
        }
    }

    private String extractFileUrl(String message) {
        try {
            // Try to parse as JSON first
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
                throw new RuntimeException("No file URL found in message");
            }
            
            // Fix WebHDFS URL encoding and add operation parameter
            fileUrl = fixWebHdfsUrl(fileUrl);
            
            logger.info("Extracted and fixed file URL from message: {}", fileUrl);
            return fileUrl;
            
        } catch (Exception e) {
            logger.warn("Failed to parse message as JSON, treating as direct content: {}", e.getMessage());
            // If JSON parsing fails, assume the message is the content itself
            return null;
        }
    }

    private String fixWebHdfsUrl(String url) {
        try {
            // Check if this is a WebHDFS URL
            if (url.contains("/webhdfs/")) {
                logger.info("Fixing WebHDFS URL: {}", url);
                
                // Handle double-encoding but preserve URL encoding for WebHDFS
                String processedUrl = url;
                
                // Check for double-encoding (e.g., %2520 instead of %20)
                if (url.contains("%25")) {
                    // Decode only the double-encoded parts
                    processedUrl = url.replace("%25", "%");
                    logger.info("Fixed double-encoding: {}", processedUrl);
                }
                
                // Remove any existing query parameters
                String baseUrl = processedUrl;
                if (processedUrl.contains("?")) {
                    baseUrl = processedUrl.substring(0, processedUrl.indexOf("?"));
                }
                
                // Add the required WebHDFS operation parameter
                String fixedUrl = baseUrl + "?op=OPEN";
                logger.info("Fixed WebHDFS URL: {}", fixedUrl);
                
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
        return message -> {
            try {
                if (message == null || message.trim().isEmpty()) {
                    logger.warn("Received empty message, skipping...");
                    return;
                }

                logger.info("Processing message of length: {} characters", message.length());
                
                // Extract file URL from the message
                String fileUrl = extractFileUrl(message);
                String fileContent;
                
                if (fileUrl != null) {
                    // Fetch the file content from the URL
                    fileContent = fetchFileContent(fileUrl);
                } else {
                    // If no URL was extracted, treat the message as direct content
                    fileContent = message;
                    logger.info("Treating message as direct file content");
                }
                
                if (fileContent == null || fileContent.trim().isEmpty()) {
                    logger.warn("No file content to process, skipping...");
                    return;
                }

                logger.info("Processing file content of length: {} characters", fileContent.length());
                
                // Split the text into chunks (2000 characters per chunk, 200 characters overlap)
                // Optimized for performance: larger chunks = fewer API calls
                List<String> chunks = chunkText(fileContent, 2000, 200);
                
                if (chunks.isEmpty()) {
                    logger.warn("No chunks generated from the file content");
                    return;
                }

                logger.info("Generated {} chunks from file content", chunks.size());
                
                // Store embeddings using batch processing for better performance
                embeddingService.storeEmbeddings(chunks);
                
                // Optionally run query after embedding if queryText is set and hasn't run yet
                if (queryText != null && !queryText.trim().isEmpty() && queryRun.compareAndSet(false, true)) {
                    logger.info("Running vector query: {}", queryText);
                    vectorQueryProcessor.runQuery(queryText, 5);
                }
                
            } catch (Exception e) {
                logger.error("Error processing message: {}", e.getMessage(), e);
                throw e;
            }
        };
    }
}
