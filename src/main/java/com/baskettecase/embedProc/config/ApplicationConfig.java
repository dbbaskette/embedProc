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
        RestTemplate restTemplate = new RestTemplate();
        
        // Configure to follow redirects (important for WebHDFS)
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory = 
            new org.springframework.http.client.SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(java.net.HttpURLConnection connection, String httpMethod) throws java.io.IOException {
                    super.prepareConnection(connection, httpMethod);
                    connection.setInstanceFollowRedirects(true);
                }
            };
        
        // Performance optimizations
        requestFactory.setConnectTimeout(10000); // 10 seconds
        requestFactory.setReadTimeout(30000);    // 30 seconds
        
        restTemplate.setRequestFactory(requestFactory);
        
        return restTemplate;
    }
} 