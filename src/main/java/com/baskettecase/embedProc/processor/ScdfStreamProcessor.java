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
            ResponseEntity<String> response = restTemplate.getForEntity(fileUrl, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                String content = response.getBody();
                logger.info("Successfully fetched file content, length: {} characters", content != null ? content.length() : 0);
                return content;
            } else {
                logger.error("Failed to fetch file content from URL: {}. Status: {}", fileUrl, response.getStatusCode());
                throw new RuntimeException("Failed to fetch file content, HTTP status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error fetching file content from URL: {}. Error: {}", fileUrl, e.getMessage());
            throw new RuntimeException("Failed to fetch file content from URL: " + fileUrl, e);
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
            
            logger.info("Extracted file URL from message: {}", fileUrl);
            return fileUrl;
            
        } catch (Exception e) {
            logger.warn("Failed to parse message as JSON, treating as direct content: {}", e.getMessage());
            // If JSON parsing fails, assume the message is the content itself
            return null;
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
                
                // Split the text into chunks (1000 words per chunk, 100 words overlap)
                List<String> chunks = chunkText(fileContent, 1000, 100);
                
                if (chunks.isEmpty()) {
                    logger.warn("No chunks generated from the file content");
                    return;
                }

                logger.info("Generated {} chunks from file content", chunks.size());
                
                // Store embeddings using the EmbeddingService
                embeddingService.storeEmbeddings(chunks);
                
                // Optionally run query after embedding if queryText is set and hasn't run yet
                if (queryText != null && !queryText.isBlank() && queryRun.compareAndSet(false, true)) {
                    vectorQueryProcessor.runQuery(queryText, 5);
                }
            } catch (Exception e) {
                logger.error("Error processing message: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to process message", e);
            }
        };
    }
}
