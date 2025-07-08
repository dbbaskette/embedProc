package com.baskettecase.embedProc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ApplicationConfig {

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

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
} 