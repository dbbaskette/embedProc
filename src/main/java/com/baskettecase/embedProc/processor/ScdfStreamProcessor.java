package com.baskettecase.embedProc.processor;
    
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import java.util.List;
import java.util.function.Consumer;

@Configuration
@Profile("scdf")
public class ScdfStreamProcessor {
    private final VectorStore vectorStore;

    public ScdfStreamProcessor(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Bean
    public Consumer<String> embedProc() {
        return text -> {
            Document doc = new Document(text);
            vectorStore.add(List.of(doc));
        };
    }
}
