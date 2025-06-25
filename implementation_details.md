# Implementation Details

## Architecture Overview

The application now uses a layered architecture with improved error handling and monitoring:

### Service Layer
- **EmbeddingService**: Centralized service for embedding operations with retry logic and metrics
- **VectorQueryProcessor**: Handles similarity search operations

### Processor Layer  
- **ScdfStreamProcessor**: Stream processing for SCDF deployment
- **StandaloneDirectoryProcessor**: Batch processing for standalone mode

## Embedding Storage with pgvector

Embeddings are stored in PostgreSQL with pgvector using the Spring AI `VectorStore` interface through the `EmbeddingService`.

- The `EmbeddingService` provides centralized embedding operations with:
  - Automatic retry on failures (up to 3 attempts)
  - Metrics collection (success/error counters)
  - Proper error handling and logging
  - Batch processing capabilities
- Both successful and failed storage attempts are logged with appropriate detail levels.

**Success log example:**
```
[EmbeddingService] Successfully stored embedding for text preview: 'This is a sample input...'
```
**Error log example:**
```
[EmbeddingService] Failed to store embedding for text preview: 'This is a sample input...'. Error: <error message>
```

## Monitoring and Metrics

The application now includes comprehensive monitoring:

### Metrics
- `embeddings.processed`: Counter of successfully processed embeddings
- `embeddings.errors`: Counter of embedding processing errors

### Health Checks
- Custom health indicators for vector store connectivity
- Actuator endpoints for monitoring application health

### Retry Logic
- Automatic retry on transient failures (3 attempts with 1-second backoff)
- Proper error propagation for non-recoverable errors

## Logging for Embedding Output

The `ScdfStreamProcessor` logs every embedding sent to the output queue. Each log entry includes a preview of the input text, the embedding size, and the first five values of the embedding. This allows you to verify that embeddings are being placed on the RabbitMQ queue.

**Example log message:**
```
[embedProc] Sending embedding to output queue. Text preview: 'This is a sample input...', Embedding size: 1536, Embedding (first 5): [0.012, 0.034, 0.056, 0.078, 0.091]
```

Use these logs to confirm that your processor is producing and sending embeddings as expected.


## Required Dependencies for Embedding

- The project requires `spring-boot-starter-web` as a dependency. This ensures that a `RestClient.Builder` bean is auto-configured, which is necessary for Spring AI's Ollama integration. Without this dependency, the application will fail to start with an UnsatisfiedDependencyException due to the missing bean.

- If you see an error similar to:

  > Parameter 1 of method ollamaApi in org.springframework.ai.autoconfigure.ollama.OllamaAutoConfiguration required a bean of type 'org.springframework.web.client.RestClient$Builder' that could not be found.

  Ensure that `spring-boot-starter-web` is included in your `pom.xml`.

## Dependency Location

The dependency is found in the `<dependencies>` section of `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

## Additional Notes
- This is the preferred solution over defining the bean manually, as it leverages Spring Boot's auto-configuration and is less error-prone.
