package com.baskettecase.embedProc.processor;
    
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Bean
    public Consumer<String> embedProc() {
        return text -> {
            Document doc = new Document(text);
            vectorStore.add(List.of(doc));
            // Optionally run query after embedding if queryText is set and hasn't run yet
            if (queryText != null && !queryText.isBlank() && queryRun.compareAndSet(false, true)) {
                vectorQueryProcessor.runQuery(queryText, 5);
            }
        };
    }
}
