# Implementation Details

## Embedding Storage with pgvector

Embeddings are now stored in PostgreSQL with pgvector using the Spring AI `VectorStore` interface. This is handled by the `VectorStoreService` class in the `service` package.

- After an embedding is generated, `VectorStoreService.saveEmbedding(text, embedding)` is called.
- The service creates a `Document`, sets its content and embedding, and stores it using `vectorStore.add(...)`.
- Both successful and failed storage attempts are logged.

**Success log example:**
```
[VectorStoreService] Successfully stored embedding for text preview: 'This is a sample input...', size: 1536
```
**Error log example:**
```
[VectorStoreService] Failed to store embedding for text preview: 'This is a sample input...'. Error: <error message>
```

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
