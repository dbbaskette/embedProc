package com.baskettecase.embedProc.processor;
    
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicBoolean;

@Configuration
@Profile("scdf")

public class ScdfStreamProcessor {
    private final VectorStore vectorStore;
    private final VectorQueryProcessor vectorQueryProcessor;
    private final String queryText;
    private final AtomicBoolean queryRun = new AtomicBoolean(false);

    @Autowired
    public ScdfStreamProcessor(VectorStore vectorStore, VectorQueryProcessor vectorQueryProcessor, @Value("${app.query.text:}") String queryText) {
        this.vectorStore = vectorStore;
        this.vectorQueryProcessor = vectorQueryProcessor;
        this.queryText = queryText;
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

    @Bean
    public Consumer<String> embedProc() {
        return text -> {
            try {
                if (text == null || text.trim().isEmpty()) {
                    System.err.println("Received empty text, skipping...");
                    return;
                }

                System.out.println("Processing document of length: " + text.length() + " characters");
                
                // Split the text into chunks (1000 words per chunk, 100 words overlap)
                List<String> chunks = chunkText(text, 1000, 100);
                
                if (chunks.isEmpty()) {
                    System.err.println("No chunks generated from the input text");
                    return;
                }

                System.out.println("Generated " + chunks.size() + " chunks");
                
                // Store each chunk in the vector store
                int chunkCount = 0;
                for (String chunk : chunks) {
                    if (chunk != null && !chunk.trim().isEmpty()) {
                        try {
                            Document doc = new Document(chunk);
                            vectorStore.add(List.of(doc));
                            chunkCount++;
                            // Add some logging for every 10 chunks to monitor progress
                            if (chunkCount % 10 == 0) {
                                System.out.println("Processed " + chunkCount + " chunks so far...");
                            }
                        } catch (Exception e) {
                            System.err.println("Error processing chunk: " + e.getMessage());
                            // Continue with next chunk even if one fails
                        }
                    }
                }
                
                // Optionally run query after embedding if queryText is set and hasn't run yet
                if (queryText != null && !queryText.isBlank() && queryRun.compareAndSet(false, true)) {
                    vectorQueryProcessor.runQuery(queryText, 5);
                }
            } catch (Exception e) {
                System.err.println("Error processing document: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to process document", e);
            }
        };
    }
}
