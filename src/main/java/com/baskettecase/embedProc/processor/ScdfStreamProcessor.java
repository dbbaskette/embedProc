package com.baskettecase.embedProc.processor;

import com.baskettecase.embedProc.service.EmbeddingService;

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
    private final EmbeddingService embeddingService;

    public ScdfStreamProcessor(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
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
    public Function<Message<String>, Message<List<Double>>> embedText() {
        return message -> {
            String text = message.getPayload();
            List<Double> embedding = embeddingService.generateEmbedding(text);
            return MessageBuilder.withPayload(embedding).build();
        };
    }
}
