package com.baskettecase.embedProc.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class EmbeddingModelConfig {

    /**
     * Primary embedding model for standalone profile - uses Ollama
     * This method marks the Ollama embedding model as primary when standalone profile is active
     */
    @Bean
    @Primary
    @Profile("standalone")
    public EmbeddingModel standaloneEmbeddingModel(@Qualifier("ollamaEmbeddingModel") EmbeddingModel ollamaEmbeddingModel) {
        return ollamaEmbeddingModel;
    }

    /**
     * Primary embedding model for cloud profile - uses OpenAI
     * This method marks the OpenAI embedding model as primary when cloud profile is active
     */
    @Bean
    @Primary
    @Profile("cloud")
    public EmbeddingModel cloudEmbeddingModel(@Qualifier("openAiEmbeddingModel") EmbeddingModel openAiEmbeddingModel) {
        return openAiEmbeddingModel;
    }
}