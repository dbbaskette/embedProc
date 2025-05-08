package com.baskettecase.embedProc.processor;

import org.springframework.ai.ollama.OllamaEmbeddingClient;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.function.Function;

/**
 * Spring Cloud Data Flow (SCDF) Stream Processor.
 * <p>
 * This component is activated under the 'scdf' Spring profile and is designed to work as a message-driven processor for SCDF pipelines.
 * It listens for incoming messages (typically from RabbitMQ), expects a JSON payload with S3/MinIO object references,
 * downloads the referenced file, extracts its text using Apache Tika, and outputs the result.
 * <p>
 * Environment Variables:
 * <ul>
 *   <li><b>S3_ENDPOINT</b>: URL for MinIO/S3 (default: http://localhost:9000)</li>
 *   <li><b>S3_ACCESS_KEY</b>: Access key for MinIO/S3 (required)</li>
 *   <li><b>S3_SECRET_KEY</b>: Secret key for MinIO/S3 (required)</li>
 * </ul>
 *
 * Message Structure:
 * <ul>
 *   <li>Input: JSON with fields <code>bucketName</code> and <code>key</code> (object name)</li>
 *   <li>Output: Extracted text as message payload</li>
 * </ul>
 *
 * Error Handling:
 * <ul>
 *   <li>Logs and returns empty payload on JSON parse, download, or extraction errors</li>
 *   <li>Deletes temporary files after processing</li>
 * </ul>
 *
 * See {@link ExtractionService} for extraction logic.
 */
@Component
@Profile("scdf")
public class ScdfStreamProcessor {
    private final EmbeddingClient embeddingClient;

    @Autowired
    public ScdfStreamProcessor(OllamaEmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    /**
     * Spring Cloud Stream Function: receives text as input, outputs embedding vector.
     * Input message payload: String (text to embed)
     * Output message payload: List<Double> (embedding vector)
     */
    @Bean
    public Function<Message<String>, Message<List<Double>>> embedText() {
        return message -> {
            String text = message.getPayload();
            List<Double> embedding = embeddingClient.embed(text);
            return MessageBuilder.withPayload(embedding).build();
        };
    }
}                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * Spring Cloud Stream Function: receives S3 reference as input, outputs extracted text.
     * Input message payload should be 'bucket/filename' (e.g., mybucket/myfile.pdf)
     */
    @Profile("scdf")
    @Bean
    /**
     * Spring Cloud Stream Function bean.
     * <p>
     * Listens for messages containing JSON payloads with S3/MinIO object references, downloads the file, extracts text, and returns the result.
     *
     * @return Function that processes messages and emits extracted text.
     *
     * <b>Expected input message:</b>
     * <pre>{ "bucketName": "mybucket", "key": "myfile.pdf" }</pre>
     *
     * <b>Error handling:</b>
     * <ul>
     *   <li>Returns empty payload on JSON parse, download, or extraction errors</li>
     *   <li>All errors are logged</li>
     * </ul>
     */
    public Function<Message<String>, Message<String>> textProc() {
        logger.atDebug().log("SCDF mode activated in FUNCTION");
        return inputMsg -> {
            String payload = inputMsg.getPayload();
            String bucket;
            String key;
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            try {
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(payload);
                bucket = root.get("bucketName").asText();
                key = root.get("key").asText();
            } catch (Exception e) {
                logger.error("Failed to parse bucketName/key from JSON message: {}", payload, e);
                return MessageBuilder.withPayload("").build();
            }
            logger.info("Processing S3 object: {}/{}", bucket, key);

            // Download object from MinIO to /tmp/<bucketName>/<key>
            Path bucketDir = Path.of("/tmp", bucket);
            Path tempFile;
            try {
                Files.createDirectories(bucketDir);
                tempFile = bucketDir.resolve(key);
                try (var stream = minioClient.getObject(
                        GetObjectArgs.builder().bucket(bucket).object(key).build())) {
                    Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (io.minio.errors.ErrorResponseException |
                     io.minio.errors.InsufficientDataException |
                     io.minio.errors.InternalException |
                     io.minio.errors.InvalidResponseException |
                     io.minio.errors.ServerException |
                     io.minio.errors.XmlParserException |
                     java.security.InvalidKeyException |
                     java.security.NoSuchAlgorithmException |
                     IOException e) {
                logger.error("Error downloading MinIO object: {}/{}", bucket, key, e);
                return MessageBuilder.withPayload("").build();
            }

            // Extract text
            String extractedText;
            try {
                extractedText = extractionService.extractTextFromFile(tempFile);
            } catch (IOException e) {
                logger.error("Error extracting text from file {}: {}", tempFile, e.getMessage(), e);
                return MessageBuilder.withPayload("").build();
            } finally {
                try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
            }

            logger.info("Extraction complete for {}/{} ({} chars)", bucket, key, extractedText.length());
            return MessageBuilder.withPayload(extractedText).build();
        };
    }
}
    