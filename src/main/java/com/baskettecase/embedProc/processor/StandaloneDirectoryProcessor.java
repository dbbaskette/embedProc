package com.baskettecase.embedProc.processor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Profile;
import java.io.IOException;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import com.baskettecase.embedProc.service.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.Counter;

@Component
@Profile("standalone")
public class StandaloneDirectoryProcessor implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(StandaloneDirectoryProcessor.class);
    
    private final EmbeddingModel embeddingModel;
    private final EmbeddingService embeddingService;
    private final VectorQueryProcessor vectorQueryProcessor;
    private final String queryText;
    private final Counter chunksReceivedCounter;

    @Autowired
    public StandaloneDirectoryProcessor(EmbeddingModel embeddingModel, EmbeddingService embeddingService, 
                                      VectorQueryProcessor vectorQueryProcessor, @Value("${app.query.text:}") String queryText,
                                      Counter chunksReceivedCounter) {
        this.embeddingModel = embeddingModel;
        this.embeddingService = embeddingService;
        this.vectorQueryProcessor = vectorQueryProcessor;
        this.queryText = queryText;
        this.chunksReceivedCounter = chunksReceivedCounter;
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
        // After processing all files and writing to the database, run the query if defined
        vectorQueryProcessor.runQuery(queryText, 5);
        // Exit after processing all files and running the query
        System.exit(0);
    }

    private void processFile(Path file) {
        try {
            String content = Files.readString(file);
            float[] embedding = embeddingModel.embed(content);
            logger.info("Processing file: {}", file.getFileName());
            logger.debug("Embedding dimensions: {}", embedding.length);
            
            // Increment chunks received counter (1 file = 1 chunk in standalone mode)
            chunksReceivedCounter.increment();
            
            // Store embedding using the EmbeddingService
            embeddingService.storeEmbedding(content);
            logger.info("Successfully stored embedding for file: {}", file.getFileName());
            
        } catch (IOException e) {
            logger.error("Failed to read file: {}, error: {}", file, e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to process file: {}, error: {}", file, e.getMessage());
        }
    }
}
