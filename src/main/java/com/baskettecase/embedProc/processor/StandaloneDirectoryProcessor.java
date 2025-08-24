package com.baskettecase.embedProc.processor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import java.io.IOException;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.Value;
import com.baskettecase.embedProc.service.EmbeddingService;
import com.baskettecase.embedProc.service.DocumentType;
import com.baskettecase.embedProc.service.MonitorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;

@Component
@Profile("standalone")
public class StandaloneDirectoryProcessor implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(StandaloneDirectoryProcessor.class);
    
    private final EmbeddingService embeddingService;
    private final MonitorService monitorService;
    private final VectorQueryProcessor vectorQueryProcessor;
    private final JdbcTemplate jdbcTemplate;
    private final String queryText;
    private final boolean useReferenceNumbers;
    private final Integer defaultRefnum1;
    private final Integer defaultRefnum2;

    public StandaloneDirectoryProcessor(EmbeddingService embeddingService,
                                      MonitorService monitorService,
                                      VectorQueryProcessor vectorQueryProcessor,
                                      JdbcTemplate jdbcTemplate,
                                      @Value("${app.query.text:}") String queryText,
                                      @Value("${app.reference-numbers.enable-validation:false}") boolean useReferenceNumbers,
                                      @Value("${app.reference-numbers.default.refnum1:100001}") Integer defaultRefnum1,
                                      @Value("${app.reference-numbers.default.refnum2:200001}") Integer defaultRefnum2) {
        this.embeddingService = embeddingService;
        this.monitorService = monitorService;
        this.vectorQueryProcessor = vectorQueryProcessor;
        this.jdbcTemplate = jdbcTemplate;
        this.queryText = queryText;
        this.useReferenceNumbers = useReferenceNumbers;
        this.defaultRefnum1 = defaultRefnum1;
        this.defaultRefnum2 = defaultRefnum2;
        
        logger.info("StandaloneDirectoryProcessor initialized with reference numbers: {}", useReferenceNumbers);
        if (useReferenceNumbers) {
            logger.info("Default reference numbers - refnum1: {}, refnum2: {}", defaultRefnum1, defaultRefnum2);
        }
    }


    @Override
    public void run(String... args) throws Exception {
        Path inputDir = Paths.get("./data/input_files");
        if (!Files.exists(inputDir)) {
            System.err.println("Input directory does not exist: " + inputDir.toAbsolutePath());
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDir, "*.txt")) {
            for (Path entry : stream) {
                processFile(entry);
            }
        }
        // After processing all files, display stored embeddings with reference numbers
        displayStoredEmbeddings();
        
        // Run similarity query if query text is provided
        if (queryText != null && !queryText.trim().isEmpty()) {
            logger.info("\n=== Running Similarity Query ===");
            vectorQueryProcessor.runQuery(queryText, 5);
        } else {
            logger.info("No query text provided. Skipping similarity search.");
        }
        
        // Exit after processing all files and running the query
        System.exit(0);
    }

    private void processFile(Path file) {
        try {
            // Update current file being processed
            if (monitorService != null) {
                monitorService.setCurrentFile(file.getFileName().toString());
            }
            
            String content = Files.readString(file);
            logger.info("Processing file: {}", file.getFileName());
            
            if (useReferenceNumbers) {
                // Extract reference numbers from filename
                ReferenceNumbers refNumbers = extractReferenceNumbersFromFilename(file.getFileName().toString());
                if (refNumbers != null) {
                    logger.info("Storing embedding with reference numbers from filename - refnum1: {}, refnum2: {}", 
                               refNumbers.refnum1, refNumbers.refnum2);
                    // Determine document type from file path
                    DocumentType documentType = DocumentType.fromUrl(file.toString());
                    // For reference documents, don't include refnums
                    if (documentType == DocumentType.REFERENCE) {
                        embeddingService.storeEmbeddingWithMetadata(content, null, null, documentType, file.toString());
                    } else {
                        embeddingService.storeEmbeddingWithMetadata(content, refNumbers.refnum1, refNumbers.refnum2, documentType, file.toString());
                    }
                } else {
                    // Fall back to default reference numbers if filename parsing fails
                    logger.warn("Could not extract reference numbers from filename: {}, using defaults", file.getFileName());
                    // Determine document type from file path
                    DocumentType documentType = DocumentType.fromUrl(file.toString());
                    // For reference documents, don't include refnums
                    if (documentType == DocumentType.REFERENCE) {
                        embeddingService.storeEmbeddingWithMetadata(content, null, null, documentType, file.toString());
                    } else {
                        embeddingService.storeEmbeddingWithMetadata(content, defaultRefnum1, defaultRefnum2, documentType, file.toString());
                    }
                }
            } else {
                // Store embedding using the regular EmbeddingService
                embeddingService.storeEmbedding(content);
            }
            
            logger.info("Successfully stored embedding for file: {}", file.getFileName());
            
            // Update monitor service with processed file
            if (monitorService != null) {
                monitorService.incrementFilesProcessed();
                monitorService.publishEvent("FILE_COMPLETE", file.getFileName().toString());
            }
            
        } catch (IOException e) {
            logger.error("Failed to read file: {}, error: {}", file, e.getMessage());
            // Update monitor service with error
            if (monitorService != null) {
                monitorService.setLastError("Failed to read file " + file.getFileName() + ": " + e.getMessage());
                monitorService.setCurrentFile(null);
            }
        } catch (Exception e) {
            logger.error("Failed to process file: {}, error: {}", file, e.getMessage());
            // Update monitor service with error
            if (monitorService != null) {
                monitorService.setLastError("Failed to process file " + file.getFileName() + ": " + e.getMessage());
                monitorService.setCurrentFile(null);
            }
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
     * Display all stored embeddings with their reference number metadata by querying the database directly
     */
    private void displayStoredEmbeddings() {
        try {
            logger.info("\n=== Stored Embeddings with Reference Numbers ===");
            
            // Query the pgvector embeddings table directly
            String sql = "SELECT id, content, metadata FROM embeddings ORDER BY id LIMIT 100";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            
            if (rows.isEmpty()) {
                logger.info("No embeddings found in the database.");
                return;
            }
            
            logger.info("Found {} stored embeddings:", rows.size());
            
            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> row = rows.get(i);
                Object id = row.get("id");
                String content = (String) row.get("content");
                
                // Handle PostgreSQL JSON field which returns as PGobject
                String metadataJson = null;
                Object metadataObj = row.get("metadata");
                if (metadataObj != null) {
                    metadataJson = metadataObj.toString();
                }
                
                // Parse metadata JSON to extract reference numbers
                String refnum1 = "N/A";
                String refnum2 = "N/A";
                String timestamp = "N/A";
                
                if (metadataJson != null && !metadataJson.isEmpty()) {
                    // Simple JSON parsing for refnum1, refnum2, and timestamp
                    if (metadataJson.contains("refnum1")) {
                        refnum1 = extractJsonValue(metadataJson, "refnum1");
                    }
                    if (metadataJson.contains("refnum2")) {
                        refnum2 = extractJsonValue(metadataJson, "refnum2");
                    }
                    if (metadataJson.contains("timestamp")) {
                        timestamp = extractJsonValue(metadataJson, "timestamp");
                    }
                }
                
                String contentPreview = content != null && content.length() > 100 
                    ? content.substring(0, 100) + "..."
                    : (content != null ? content : "[No content]");
                
                logger.info("[{}] ID: {}, RefNum1: {}, RefNum2: {}, Timestamp: {}, Content: '{}'", 
                           i + 1, id, refnum1, refnum2, timestamp, contentPreview);
            }
            
            logger.info("=== End of Stored Embeddings ===");
            
        } catch (Exception e) {
            logger.error("Failed to display stored embeddings: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Simple helper to extract values from JSON metadata string
     */
    private String extractJsonValue(String json, String key) {
        try {
            String searchPattern = "\"" + key + "\":";
            int startIndex = json.indexOf(searchPattern);
            if (startIndex == -1) return "N/A";
            
            startIndex += searchPattern.length();
            // Skip whitespace and quotes
            while (startIndex < json.length() && (json.charAt(startIndex) == ' ' || json.charAt(startIndex) == '"')) {
                startIndex++;
            }
            
            int endIndex = startIndex;
            // Find end of value (comma, closing brace, or quote)
            while (endIndex < json.length() && json.charAt(endIndex) != ',' && json.charAt(endIndex) != '}' && json.charAt(endIndex) != '"') {
                endIndex++;
            }
            
            return json.substring(startIndex, endIndex).trim();
        } catch (Exception e) {
            return "N/A";
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
