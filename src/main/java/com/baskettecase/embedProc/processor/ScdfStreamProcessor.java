package com.baskettecase.embedProc.processor;
    
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import java.util.List;
import java.util.function.Consumer;

@Configuration
@Profile("scdf")
public class ScdfStreamProcessor {
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    public ScdfStreamProcessor(EmbeddingModel embeddingModel, VectorStore vectorStore) {
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
    }

    @Bean
    public Consumer<String> processText() {
        return text -> {
            Document doc = new Document(text);
            vectorStore.add(List.of(doc));
        };
    }
}
