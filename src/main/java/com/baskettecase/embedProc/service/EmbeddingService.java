package com.baskettecase.embedProc.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.ollama.api.OllamaOptions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * EmbeddingService provides methods to generate embedding vectors from input text using a configured embedding model.
 * <p>
 * This service uses Spring AI's EmbeddingModel abstraction, configured for Ollama models via application properties.
 * The service is intended to be used as a Spring-managed bean and provides a single method, {@code generateEmbedding},
 * which takes a string of text and returns a vector embedding as a list of doubles.
 * <p>
 * <b>Note:</b> As of Spring AI 1.0.0-M8, the EmbeddingModel API sends payloads in OpenAI-compatible format.
 * Ollama's /api/embeddings endpoint may require a different payload structure ("prompt" as a string, not "input" as an array),
 * so compatibility depends on the versions of Spring AI and Ollama in use. If you encounter 404 errors, consider using
 * a direct HTTP call to the Ollama API with the correct payload.
 *
 * Configuration:
 * <ul>
 *   <li>spring.ai.ollama.base-url</li>
 *   <li>spring.ai.ollama.embedding.model</li>
 * </ul>
 */
@Service
public class EmbeddingService {
    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    @org.springframework.beans.factory.annotation.Value("${spring.ai.ollama.base-url}")
    private String baseUrl;

    private final EmbeddingModel embeddingModel;
    private final String modelName;

    public EmbeddingService(EmbeddingModel embeddingModel, @Value("${spring.ai.ollama.embedding.model}") String modelName) {
        this.embeddingModel = embeddingModel;
        this.modelName = modelName;
    }

    /**
     * Generates an embedding vector from the given text using the configured embedding model.
     *
     * <p>This method logs debug information about the model, base URL, and a preview of the text.
     * It constructs an EmbeddingRequest and calls the embedding model. If successful, it returns
     * the embedding as a list of doubles. If embedding generation fails, a RuntimeException is thrown.
     *
     * @param text The input text to embed.
     * @return List<Double> embedding vector.
     * @throws RuntimeException if embedding generation fails (e.g., due to API incompatibility or server errors).
     */
  
    public List<Double> generateEmbedding(String text) {
        System.out.println("DEBUG: [EmbeddingService] Model name: " + modelName);
        System.out.println("DEBUG: [EmbeddingService] Base URL: " + baseUrl);
        System.out.println("DEBUG: [EmbeddingService] Text: " + (text.length() > 100 ? text.substring(0, 100) + "..." : text));
        log.trace("This is a TRACE level log!");

        try {
            OllamaOptions options = OllamaOptions.builder()
                    .model(modelName)
                    .truncate(true)
                    .build();
            text = "This is a much smaller test";

            
            EmbeddingRequest request = new EmbeddingRequest(List.of(text), options);
            EmbeddingResponse response = embeddingModel.call(request);
    
            float[] embeddingArray = response.getResults().get(0).getOutput();
            List<Double> embedding = new ArrayList<>(embeddingArray.length);
            for (float f : embeddingArray) {
                embedding.add((double) f);
            }
            return embedding;
        } catch (Exception e) {
            System.err.println("Failed to generate embedding: " + e.getMessage());
            throw new RuntimeException("Embedding generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Preferred version: Uses embeddingModel.embed(text) and returns the result directly.
     */
    // public List<Double> generateEmbedding(String text) {
    //     System.out.println("DEBUG: [EmbeddingService] Model name: " + modelName);
    //     System.out.println("DEBUG: [EmbeddingService] Base URL: " + baseUrl);
    //     System.out.println("DEBUG: [EmbeddingService] Text: " + (text.length() > 100 ? text.substring(0, 100) + "..." : text));
    //     try {
    //         float[] embeddingArray = embeddingModel.embed(text);
    //         List<Double> embedding = new ArrayList<>(embeddingArray.length);
    //         for (float f : embeddingArray) {
    //             embedding.add((double) f);
    //         }
    //         return embedding;
    //     } catch (Exception e) {
    //         System.err.println("Failed to generate embedding: " + e.getMessage());
    //         throw new RuntimeException("Embedding generation failed: " + e.getMessage(), e);
    //     }
    // }
}