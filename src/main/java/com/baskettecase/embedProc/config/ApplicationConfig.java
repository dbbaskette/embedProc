package com.baskettecase.embedProc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;

@Configuration
public class ApplicationConfig {

    @Autowired(required = false)
    private VectorStore vectorStore;

    @Bean
    public Counter embeddingProcessedCounter(MeterRegistry meterRegistry) {
        return Counter.builder("embeddings.processed")
                .description("Number of embeddings processed")
                .register(meterRegistry);
    }

    @Bean
    public Counter embeddingErrorCounter(MeterRegistry meterRegistry) {
        return Counter.builder("embeddings.errors")
                .description("Number of embedding processing errors")
                .register(meterRegistry);
    }
} 