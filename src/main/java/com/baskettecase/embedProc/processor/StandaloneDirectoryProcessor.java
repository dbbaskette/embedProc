package com.baskettecase.embedProc.processor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Profile;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;

@Component
@Profile("standalone")
public class StandaloneDirectoryProcessor implements CommandLineRunner {
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    public StandaloneDirectoryProcessor(EmbeddingModel embeddingModel, VectorStore vectorStore) {
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
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
        // Exit after processing all files and writing to the database
        System.exit(0);
    }

    private void processFile(Path file) {
        try {
            String content = Files.readString(file);
            float[] embedding = embeddingModel.embed(content);
            System.out.println("File: " + file.getFileName());
            System.out.println("Embedding: " + java.util.Arrays.toString(embedding));
            // Store embedding in vector store
            Document doc = new Document(content);
            vectorStore.add(List.of(doc));
            System.out.println("Stored embedding in vector store for file: " + file.getFileName());
            System.out.println("---");
        } catch (IOException e) {
            System.err.println("Failed to read file: " + file + ", error: " + e.getMessage());
        }
    }
}
