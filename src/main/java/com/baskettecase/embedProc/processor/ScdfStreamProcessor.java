package com.baskettecase.embedProc.processor;

import com.baskettecase.embedProc.service.EmbeddingService;
import com.baskettecase.embedProc.service.VectorStoreService;

import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

/**
 * Spring Cloud Data Flow (SCDF) stream processor for text embeddings.
 * <p>
 * When the 'scdf' Spring profile is active, this component:
 * <ul>
 *   <li>Receives text messages from an input queue.</li>
 *   <li>Uses the Ollama Nomic model (via Spring AI) to generate embedding vectors for the text.</li>
 *   <li>Stores the resulting embedding in a PostgreSQL database using pgvector via Spring AI's VectorStore abstraction.</li>
 *   <li>Sends the embedding vector as a message to an output queue.</li>
 * </ul>
 * <b>Key Features:</b>
 * <ul>
 *   <li>Integration with Spring AI EmbeddingModel and VectorStore</li>
 *   <li>Logs outgoing embeddings and storage results for observability</li>
 * </ul>
 * <b>Dependencies:</b> Requires Spring AI, pgvector, and a running PostgreSQL instance.
 */
@Component
@Profile("scdf")
public class ScdfStreamProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ScdfStreamProcessor.class);
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    /**
     * Constructs the processor with required services.
     *
     * @param embeddingService Service for generating text embeddings
     * @param vectorStoreService Service for persisting embeddings in pgvector
     */
    public ScdfStreamProcessor(EmbeddingService embeddingService, VectorStoreService vectorStoreService) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
    }
    
    /**
     * Defines the Spring Cloud Stream processor function bean.
     * <p>
     * Flow:
     * <ol>
     *   <li>Receives a message with a String payload (text).</li>
     *   <li>Generates an embedding vector using the configured EmbeddingService (Ollama Nomic model).</li>
     *   <li>Persists the embedding and original text in PostgreSQL/pgvector via VectorStoreService.</li>
     *   <li>Returns a message containing the embedding as a List&lt;Double&gt; for downstream processing.</li>
     * </ol>
     * <b>Logging:</b> Logs outgoing embedding and storage events for traceability.
     *
     * @return a function mapping input messages to output embedding messages
     */
    @Profile("scdf")
    @Bean
    public Function<Message<String>, Message<List<Double>>> embedProc() {
        return message -> {
            String text = message.getPayload();
            // Generate embedding for input text
            List<Double> embedding = embeddingService.generateEmbedding(text);
            // Persist embedding and text in pgvector
            vectorStoreService.saveEmbedding(text, embedding);
            // Build outgoing message for downstream
            Message<List<Double>> outgoing = MessageBuilder.withPayload(embedding).build();
            logger.info("[embedProc] Sending embedding to output queue. Text preview: '{}', Embedding size: {}, Embedding (first 5): {}", 
                text.length() > 50 ? text.substring(0, 50) + "..." : text, 
                embedding.size(), 
                embedding.size() > 5 ? embedding.subList(0, 5) : embedding);
            return outgoing;
        };
    }
}
