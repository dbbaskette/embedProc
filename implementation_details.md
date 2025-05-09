# Implementation Details

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
