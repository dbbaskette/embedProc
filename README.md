<div align="center">
  <img src="images/embedProc.jpg" alt="embedProc Logo" width="200"/>
  <h1>embedProc</h1>
  <h3>Advanced Text Embedding Processor with Dual Deployment Support</h3>
  
  [![Java Version](https://img.shields.io/badge/java-21-brightgreen)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
  [![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen)](https://spring.io/projects/spring-boot)
  [![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0-blue)](https://spring.io/projects/spring-ai)
  [![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
</div>

---

A high-performance text embedding processor built with **Spring AI** that supports both **standalone** and **cloud** deployment modes. Generate and store text embeddings using multiple AI providers with PostgreSQL and pgvector for efficient vector operations.

## 🚀 Key Features

- **🔄 Dual Deployment Modes**: Standalone directory processing or cloud stream processing
- **🤖 Multiple AI Providers**: 
  - **Ollama** (local inference) for standalone mode
  - **OpenAI** for cloud deployments
  - **Smart Profile-Based Selection**: Automatic embedding model selection based on deployment environment
- **💾 Vector Storage**: PostgreSQL with pgvector extension for similarity search
- **📊 Smart Document Processing**: Automatic chunking with configurable overlap
- **🔁 Resilient Operations**: Built-in retry mechanisms and error handling
- **📈 Observability**: Metrics collection and comprehensive logging
- **🔒 Cloud-Ready**: Cloud Foundry and Kubernetes deployment support

## 📋 Prerequisites

- **Java 21+**
- **Maven 3.6.3+** (with `-parameters` flag support)
- **PostgreSQL 14+** with pgvector extension
- **Ollama server** (for standalone mode)
- **OpenAI API key** (for cloud mode)
- **Docker** (for local testing with Testcontainers)

## 🛠️ Quick Start

### 1. Clone and Build

```bash
git clone <your-repo-url>
cd embedProc
./mvnw clean package
```

### 2. Database Setup

```sql
-- Create database and enable pgvector
CREATE DATABASE embeddings_db;
\c embeddings_db;
CREATE EXTENSION IF NOT EXISTS vector;
```

## 🎯 Deployment Modes

### 📁 Standalone Mode (Directory Processing)

**Use Case**: Process files from local directories using Ollama for inference.

#### Setup Ollama
```bash
# Install and start Ollama
curl -fsSL https://ollama.ai/install.sh | sh
ollama serve

# Pull the embedding model
ollama pull nomic-embed-text
```

#### Configuration
Create `application-local.properties`:
```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/embeddings_db
spring.datasource.username=your_username
spring.datasource.password=your_password

# Ollama
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.embedding.model=nomic-embed-text

# Directories
app.processor.standalone.input-directory=./data/input_files
app.processor.standalone.processed-directory=./data/processed_files
app.processor.standalone.error-directory=./data/error_files
```

#### Run Standalone
```bash
# Using the convenience script
./standalone.sh

# Or directly with Java
java -jar target/embedProc-0.0.5.jar \
  --spring.profiles.active=standalone,local
```

### ☁️ Cloud Mode (Stream Processing)

**Use Case**: Process streaming data in Spring Cloud Data Flow using OpenAI.

#### Deploy to Cloud/SCDF
```bash
# Register the application
app register --name embed-proc --type processor \
  --uri maven://com.baskettecase:embedProc:0.0.5

# Create stream
stream create --name embedding-pipeline \
  --definition "http --port=9000 | embed-proc | log" \
  --deploy
```

#### Configuration Properties
```properties
# OpenAI API Key (set via environment or cloud deployment properties)
spring.ai.openai.api-key=your-openai-api-key
spring.ai.openai.embedding.options.model=text-embedding-3-small

# Database (auto-configured in Cloud Foundry)
spring.datasource.url=jdbc:postgresql://your-db-host:5432/your-db
spring.datasource.username=your-username
spring.datasource.password=your-password
```

#### Send Data to Stream
```bash
# Send text for processing
curl -X POST http://localhost:9000 \
  -H "Content-Type: text/plain" \
  -d "Your text to embed and store"
```

## ⚙️ Configuration Reference

### Core Properties

| Property | Description | Default | Required |
|----------|-------------|---------|----------|
| `app.processor.mode` | Deployment mode | `standalone` | No |
| `spring.datasource.url` | PostgreSQL connection URL | - | Yes |
| `spring.datasource.username` | Database username | - | Yes |
| `spring.datasource.password` | Database password | - | Yes |

### Embedding Model Configuration

The application automatically selects the appropriate embedding model based on the active Spring profile:

- **Standalone Profile**: Uses Ollama embedding model (local inference)
- **Cloud Profile**: Uses OpenAI embedding model (API-based)

This profile-based selection resolves Spring bean conflicts and ensures optimal performance for each deployment environment.

### Ollama Configuration (Standalone)

| Property | Description | Default |
|----------|-------------|---------|
| `spring.ai.ollama.base-url` | Ollama server URL | `http://localhost:11434` |
| `spring.ai.ollama.embedding.model` | Model name | `nomic-embed-text` |

### OpenAI Configuration (Cloud)

| Property | Description | Default |
|----------|-------------|---------|
| `spring.ai.openai.api-key` | OpenAI API key | - |
| `spring.ai.openai.embedding.options.model` | Model name | `text-embedding-3-small` |

### Vector Store Configuration

| Property | Description | Default |
|----------|-------------|---------|
| `spring.ai.vectorstore.pgvector.dimensions` | Vector dimensions | `768` (Ollama) / `1536` (OpenAI) |
| `spring.ai.vectorstore.pgvector.distance-type` | Distance metric | `COSINE_DISTANCE` |
| `spring.ai.vectorstore.pgvector.table-name` | Table name | `embeddings` |
| `spring.ai.vectorstore.pgvector.initialize-schema` | Auto-create schema | `true` |

### Directory Configuration (Standalone)

| Property | Description | Default |
|----------|-------------|---------|
| `app.processor.standalone.input-directory` | Input files location | `./data/input_files` |
| `app.processor.standalone.processed-directory` | Processed files location | `./data/processed_files` |
| `app.processor.standalone.error-directory` | Error files location | `./data/error_files` |

### Monitoring Configuration

| Property | Description | Default |
|----------|-------------|---------|
| `app.monitoring.rabbitmq.enabled` | Enable RabbitMQ metrics publishing | `false` |
| `app.monitoring.rabbitmq.queue-name` | RabbitMQ queue name for metrics | `embedproc.metrics` |

### Chunking Configuration

| Property | Description | Default |
|----------|-------------|---------|
| `app.chunking.max-words-per-chunk` | Maximum words per chunk (200-500 recommended) | `300` |
| `app.chunking.overlap-words` | Overlap words between chunks for context continuity | `30` |

## 📊 Document Processing

### Text Chunking
- **Chunk Size**: 300 words (configurable, optimized for precise matches)
- **Overlap**: 30 words (configurable, maintains context continuity)
- **Semantic Boundaries**: Paragraph-based splitting preserves document structure
- **Metadata**: Preserved for each chunk

### Supported File Types
- Plain text (`.txt`)
- Easily extensible for other formats

### Processing Flow
1. **File Detection**: Monitor input directory
2. **Text Extraction**: Read and validate content
3. **Semantic Chunking**: Split on paragraph boundaries with word-based overlap
4. **Embedding**: Generate vectors using AI model
5. **Storage**: Store in PostgreSQL with pgvector
6. **Cleanup**: Move processed files

### Cloud Mode Features
- **JSON Message Processing**: Handles multiple message formats (`fileUrl`, `url`, `file_url`, `content`)
- **WebHDFS Support**: Specialized handling for Hadoop Distributed File System URLs
- **URL Content Fetching**: Downloads file content from URLs using RestTemplate
- **Byte Array Support**: Handles messages received as byte arrays
- **Error Resilience**: Comprehensive retry logic and fallback mechanisms

## 📈 Monitoring & Observability

### Metrics
- `embedding.processed.count` - Successfully processed embeddings
- `embedding.error.count` - Failed embedding attempts

### Instance Startup Reporting
- **Automatic Reporting**: Each instance reports itself to metrics queue on startup
- **Event-Driven**: Uses `ApplicationReadyEvent` to ensure app is fully initialized
- **Instance Identification**: Unique instance IDs using `{appName}-{instanceIndex}` format
- **Startup Message**: Publishes initial metrics with status "STARTED" and zero counters
- **Error Resilience**: Startup reporting failures don't affect application startup

### Distributed Monitoring
- **RabbitMQ Integration**: Metrics published to `embedproc.metrics` queue
- **Circuit Breaker**: Automatic recovery from RabbitMQ failures
- **Cloud Profile**: Enabled by default in cloud deployments
- **Local Development**: Can be enabled for testing with local RabbitMQ

### Logging
```properties
# Optimized logging for Cloud Foundry (prevents rate limits)
logging.level.com.baskettecase.embedProc=INFO
logging.level.org.springframework.ai=WARN
logging.level.org.springframework.cloud.stream=WARN
```

### Health Checks
- Actuator endpoints available at `/actuator/health`
- Database connectivity checks
- AI service availability checks

## 🔧 Development

### Running Tests
```bash
./mvnw test
```

### Building Docker Image
```bash
./dockerbuild.sh
```

### Project Structure
```
src/
├── main/java/com/baskettecase/embedProc/
│   ├── EmbedProcApplication.java          # Main application
│   ├── config/ApplicationConfig.java     # Configuration beans
│   ├── service/EmbeddingService.java     # Core embedding logic
│   └── processor/
│       ├── StandaloneDirectoryProcessor.java  # Standalone mode
│       ├── ScdfStreamProcessor.java           # Cloud mode
│       └── VectorQueryProcessor.java          # Query operations
└── main/resources/
    ├── application.properties                 # Base configuration
    ├── application-standalone.properties      # Standalone config
    └── application-cloud.properties          # Cloud config
```

## 🚀 Deployment Examples

### Cloud Foundry
```bash
# Deploy with manifest
cf push -f manifest.yml

# Or with inline configuration
cf push embedproc -p target/embedProc-0.0.5.jar \
  --health-check-http-endpoint /actuator/health
```

### Kubernetes
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: embedproc
spec:
  replicas: 1
  selector:
    matchLabels:
      app: embedproc
  template:
    metadata:
      labels:
        app: embedproc
    spec:
      containers:
      - name: embedproc
        image: embedproc:0.0.5
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "cloud"
        - name: SPRING_AI_OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: openai-secret
              key: api-key
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

### Documentation Hierarchy
- **README.md**: Overview, quick start, and basic configuration
- **implementation_details.md**: Technical architecture and implementation details
- **gotchas.md**: Common problems, troubleshooting, and solutions
- **quick_reference.md**: Configuration parameters and command references
- **DISTRIBUTED_MONITORING_IMPLEMENTATION.md**: Monitoring system details
- **RELEASE_NOTES.md**: Version history and feature changes

### Getting Help
- **New Users**: Start with README.md for overview and quick start
- **Configuration Issues**: Check quick_reference.md for parameter details
- **Troubleshooting**: Consult gotchas.md for common problems
- **Technical Details**: See implementation_details.md for architecture
- **Monitoring Setup**: Use DISTRIBUTED_MONITORING_IMPLEMENTATION.md

---

<div align="center">
  <p>Built with ❤️ using Spring AI and Spring Boot</p>
</div>
