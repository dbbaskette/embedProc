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
 * ScdfStreamProcessor is a Spring Cloud Data Flow (SCDF) stream processor.
 * <p>
 * When the 'scdf' Spring profile is active, this component:
 * <ul>
 *   <li>Receives text messages from an input queue.</li>
 *   <li>Uses the Ollama Nomic model (via Spring AI) to generate embedding vectors for the text.</li>
 *   <li>Converts the resulting float[] embedding to a List<Double>.</li>
 *   <li>Sends the embedding vector as a message to an output queue.</li>
 * </ul>
 * This enables downstream services to consume embedding vectors for further processing or storage.
 */
@Component
@Profile("scdf")
public class ScdfStreamProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ScdfStreamProcessor.class);
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    public ScdfStreamProcessor(EmbeddingService embeddingService, VectorStoreService vectorStoreService) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
    }
    
    /**
     * Spring Cloud Stream processor function bean.
     *
     * Receives a message with a String payload (text), generates its embedding vector using
     * the Ollama Nomic model, and returns a message containing the embedding as a List<Double>.
     *
     * @return a function that maps input messages to output embedding messages
     */
    @Profile("scdf")
    @Bean
    public Function<Message<String>, Message<List<Double>>> embedProc() {
        return message -> {
            String text = message.getPayload();
            List<Double> embedding = embeddingService.generateEmbedding(text);
            // Store in pgvector via VectorStoreService
            vectorStoreService.saveEmbedding(text, embedding);
            Message<List<Double>> outgoing = MessageBuilder.withPayload(embedding).build();
            logger.info("[embedProc] Sending embedding to output queue. Text preview: '{}', Embedding size: {}, Embedding (first 5): {}", 
                text.length() > 50 ? text.substring(0, 50) + "..." : text, 
                embedding.size(), 
                embedding.size() > 5 ? embedding.subList(0, 5) : embedding);
            return outgoing;
        };

    }
}
