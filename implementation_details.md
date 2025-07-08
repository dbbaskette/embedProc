# Implementation Details

## Architecture Overview

The application now uses a layered architecture with improved error handling and monitoring:

### Service Layer
- **EmbeddingService**: Centralized service for embedding operations with retry logic and metrics
- **VectorQueryProcessor**: Handles similarity search operations

### Processor Layer  
- **ScdfStreamProcessor**: Stream processing for cloud deployment
- **StandaloneDirectoryProcessor**: Batch processing for standalone mode

## Profile Configuration

The application supports multiple deployment profiles:

### Available Profiles
- **standalone**: Local processing with Ollama and file system I/O
- **cloud**: Cloud/SCDF deployment with OpenAI and streaming
- **local**: Development mode with monitoring UI

### Profile Consolidation
- **application-cloud.properties**: Combined configuration for both cloud and SCDF deployments
- **application-scdf.properties**: Removed (consolidated into cloud profile)
- All Java components updated to use "cloud" profile instead of "scdf"

### Spring Cloud Stream Configuration
- **Function Name**: `embedProc` (matches the `@Bean` method in `ScdfStreamProcessor`)
- **Input Binding**: `embedProc-in-0.destination=textInput`
- **Function Definition**: `spring.cloud.function.definition=embedProc`
- **Binder**: RabbitMQ (default)

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

### Instance Startup Reporting
- **InstanceStartupReporter**: Automatically reports instance startup to metrics queue
- **Event-driven**: Listens for `ApplicationReadyEvent` to ensure app is fully started
- **Instance identification**: Uses app name + instance index for unique identification
- **Startup message**: Publishes initial metrics with status "STARTED" and zero counters
- **Error resilience**: Startup reporting failures don't affect application startup

### Health Checks
- Custom health indicators for vector store connectivity
- Actuator endpoints for monitoring application health

### Retry Logic
- Automatic retry on transient failures (3 attempts with 1-second backoff)
- Proper error propagation for non-recoverable errors

## SCDF Processing Flow

When running in SCDF mode, the application:

1. **Receives Message**: Gets JSON message containing file URL from the input queue (`textproc-embedproc`)
2. **Extracts File URL**: Parses JSON message to extract the file URL (supports `fileUrl`, `url`, `file_url` fields)
3. **Fetches File Content**: Downloads the file content from the URL using RestTemplate
4. **Chunks Text**: Splits large documents into 1000-word chunks with 100-word overlap
5. **Generates Embeddings**: Creates embeddings for each chunk using the configured AI model
6. **Stores Embeddings**: Saves embeddings to PostgreSQL with pgvector
7. **Logs Progress**: Records processing metrics and sends to metrics queue

The `ScdfStreamProcessor.embedProc()` function is the entry point that processes each message from the queue.

### Message Format Support
The processor supports multiple message formats:
- **JSON with fileUrl**: `{"fileUrl": "http://example.com/file.txt"}`
- **JSON with url**: `{"url": "http://example.com/file.txt"}`
- **JSON with file_url**: `{"file_url": "http://example.com/file.txt"}`
- **JSON with content**: `{"content": "direct file content here"}`
- **Plain URL**: Direct URL string
- **Direct Content**: Raw file content (fallback)

### URL Type Support
The processor handles different types of URLs:
- **Regular HTTP URLs**: Standard web URLs for file downloads
- **WebHDFS URLs**: Specialized handling for Hadoop Distributed File System web interface
  - Detects URLs containing `/webhdfs/`
  - Fixes double-encoding issues (`%2520` → `%20`) while preserving valid URL encoding
  - Adds required `?op=OPEN` parameter for WebHDFS operations
  - Uses appropriate headers and redirect handling
  - Supports WebHDFS authentication and operations

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

## Release Management

### release.sh Script
- **Purpose**: Automates version incrementing, tagging, and GitHub releases
- **Usage**: `./release.sh [--release VERSION] [--jar-only]`
- **Features**:
  - Auto-increments patch version or sets specific version
  - Updates pom.xml version
  - Commits and pushes changes (only if there are changes)
  - Creates Git tags (only if tag doesn't exist)
  - **JAR-only mode**: `--jar-only` skips version increment, just builds and pushes JAR
  - Handles "up to date" repositories by still creating/updating GitHub releases
  - Optionally creates GitHub releases with JAR uploads
  - Improved error handling and user feedback

### Usage Examples
```bash
# Auto-increment patch version and create full release
./release.sh

# Set specific version
./release.sh --release 1.2.3

# Just build and push JAR for current version (useful when up to date)
./release.sh --jar-only
```

### Key Improvements
- **Smart change detection**: Only commits if there are actual changes
- **Duplicate tag protection**: Won't create tags that already exist
- **JAR-only mode**: Perfect for when you're up to date but want to push a JAR
- **Better GitHub CLI integration**: Checks for `gh` availability and provides helpful messages
