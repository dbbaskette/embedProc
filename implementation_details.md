# Implementation Details

## Architecture Overview

The application now uses a layered architecture with improved error handling and monitoring:

### Service Layer
- **EmbeddingService**: Centralized service for embedding operations with retry logic and metrics
- **MonitorService**: Provides metrics aggregation for web monitoring UI
- **VectorQueryProcessor**: Handles similarity search operations

### Controller Layer
- **MonitorController**: REST controller for web monitoring interface (active with local/cloud/scdf profiles)

### Processor Layer  
- **ScdfStreamProcessor**: Stream processing for SCDF deployment
- **StandaloneDirectoryProcessor**: Batch processing for standalone mode

## Monitoring UI System

### Web Interface Components
The application includes a built-in web monitoring interface that displays real-time processing metrics:

- **Frontend**: Single-page HTML application with responsive design
- **Backend**: REST API endpoints for metrics data
- **Auto-refresh**: Updates every 5 seconds with latest counters
- **Status Indicators**: Visual online/offline status

### Controller Architecture
```java
@Controller
@Profile({"local", "cloud", "scdf"})
public class MonitorController {
    @GetMapping("/")           // Serves the monitoring UI
    @GetMapping("/api/metrics") // JSON API for metrics data
}
```

### Metrics Tracking
The application tracks three key metrics through Micrometer counters:

1. **Chunks Received**: `chunks.received`
   - Incremented when text is split into processing chunks
   - Tracks total workload (SCDF: per chunk, Standalone: per file)

2. **Chunks Processed**: `embeddings.processed` 
   - Incremented on successful embedding storage
   - Indicates successful processing completion

3. **Chunks Errored**: `embeddings.errors`
   - Incremented on embedding processing failures
   - Tracks error rate for monitoring

### Profile-Based UI Availability

| Profile | Web UI | Port | Use Case |
|---------|--------|------|----------|
| `standalone` | ❌ | N/A | Headless batch processing |
| `local` | ✅ | 8080 | Development with monitoring |
| `scdf` | ✅ | Default | SCDF stream processing |
| `cloud` | ✅ | Default | Cloud deployment |

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

### Web Dashboard
- **Real-time Metrics Display**: Live counters with automatic refresh
- **Success Rate Calculation**: Processed/Received percentage
- **Visual Status Indicators**: Connection status to backend
- **Responsive Design**: Works on desktop and mobile devices

### Metrics Collection
- `chunks.received`: Counter of text chunks created for processing
- `embeddings.processed`: Counter of successfully processed embeddings
- `embeddings.errors`: Counter of embedding processing errors

### Health Checks
- Custom health indicators for vector store connectivity
- Actuator endpoints for monitoring application health

### Retry Logic
- Automatic retry on transient failures (3 attempts with 1-second backoff)
- Proper error propagation for non-recoverable errors

## Static Resource Management

### Web Assets Structure
```
src/main/resources/static/
├── index.html              # Main monitoring UI
└── images/
    └── embedProc.jpg       # Application logo
```

### Resource Serving
- Static resources served by Spring Boot's embedded web server
- Logo automatically copied from project images directory during build
- CSS and JavaScript embedded in HTML for simplicity

## Profile Configuration Strategy

### Local Development Profile
The `local` profile enables web functionality while maintaining standalone processing behavior:

```properties
# application-local.properties
app.processor.mode=standalone        # Use standalone processing
server.port=8080                    # Enable web server
# spring.main.web-application-type   # Default: servlet (web enabled)
```

### Standalone Profile Comparison
The `standalone` profile explicitly disables web functionality:

```properties
# application-standalone.properties
spring.main.web-application-type=NONE   # Disable web server
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

## Release Management

### release.sh Script
- **Purpose**: Automates version incrementing, tagging, and GitHub releases
- **Usage**: `./release.sh [--release VERSION] [--jar-only]`
- **Features**:
  - Automatic version detection and incrementing
  - Git tagging and pushing
  - GitHub release creation with JAR attachment
  - Supports custom version specification
  - JAR-only mode for building without version changes
  - Smart change detection (only commits if changes exist)
  - Duplicate tag protection

### Build Process Integration
- Maven version updates reflected in JAR naming
- Automatic resource copying (logo image to static directory)
- Profile-specific configuration validation
