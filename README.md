<div align="center">
  <img src="images/embedProc.jpg" alt="embedProc Logo" width="200"/>
  <h1>embedProc</h1>
  <h3>Advanced Text Embedding Processor</h3>
  
  [![Java Version](https://img.shields.io/badge/java-21-brightgreen)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
  [![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen)](https://spring.io/projects/spring-boot)
  [![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0-blue)](https://spring.io/projects/spring-ai)
  [![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
</div>

A high-performance Spring Cloud Data Flow (SCDF) processor that generates and stores text embeddings using state-of-the-art AI models with PostgreSQL and pgvector for efficient vector operations.

## ✨ Features

- 🚀 **High-Performance Processing**: Optimized for handling high-throughput text processing
- 🔍 **Multiple Model Support**: Works with various embedding models including Ollama's Nomic and OpenAI
- 💾 **Efficient Storage**: Utilizes PostgreSQL with pgvector for fast similarity search
- 📊 **Document Chunking**: Automatically processes large documents with configurable chunking
- 🔄 **Dual Modes**: Supports both standalone and SCDF deployment models
- 📝 **Comprehensive Logging**: Detailed logging for monitoring and debugging
- 🔒 **Secure**: Supports secure credential management with Spring Cloud Config
- 🧩 **Extensible**: Modular design for easy integration with other systems

## Prerequisites

- Java 21+
- Maven
- PostgreSQL with pgvector extension
- Ollama server running with Nomic model

## 🚀 Quick Start

### Prerequisites
- Java 21 or later
- Maven 3.6.3 or later
- PostgreSQL 14+ with pgvector extension
- Ollama server running (for local development)

### Building the Application

1. **Clone the repository**:
   ```bash
   git clone https://github.com/dbbaskette/embedProc.git
   cd embedProc
   ```

2. **Build the application**:
   ```bash
   ./mvnw clean package
   ```

### Running in Standalone Mode

```bash
java -jar target/embedProc-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=standalone \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/yourdb \
  --spring.datasource.username=youruser \
  --spring.datasource.password=yourpassword \
  --spring.ai.ollama.embedding.model=nomic-embed-text \
  --spring.ai.ollama.base-url=http://localhost:11434
```

### Deploying with Spring Cloud Data Flow

1. **Register the application in SCDF**:
   ```bash
   app register --name embed-proc --type processor \
     --uri maven://com.baskettecase:embedProc:0.0.1-SNAPSHOT
   ```

2. **Create and deploy the stream**:
   ```bash
   stream create --name my-embedding-stream \
     --definition "http | embed-proc | log" \
     --deploy
   ```

### Configuration

Configuration can be provided via:
- `application.properties`/`application.yml`
- Environment variables
- Spring Cloud Config Server
- Command line arguments

## Configuration

### Required Properties

| Property | Description | Example |
|----------|-------------|---------|
| `spring.datasource.url` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/yourdb` |
| `spring.datasource.username` | Database username | `youruser` |
| `spring.datasource.password` | Database password | `yourpassword` |
| `spring.ai.ollama.embedding.model` | Ollama model name | `nomic-embed-text` |
| `spring.ai.ollama.base-url` | Ollama server URL | `http://localhost:11434` |

### Optional Properties

```properties
# PgVector Configuration
spring.ai.vectorstore.pgvector.enabled=true
spring.ai.vectorstore.pgvector.dimensions=768
spring.ai.vectorstore.pgvector.distance-type=COSINE_DISTANCE

# Logging
logging.level.com.baskettecase.embedProc=INFO
logging.level.org.springframework.ai=WARN
```

## Document Processing

- Large documents are automatically split into 1000-word chunks
- Chunks have a 100-word overlap to maintain context
- Each chunk is processed and stored separately
- Progress is logged during processing

## Logging

Logs include:
- Document processing status
- Chunk generation details
- Storage operations
- Error messages

## Monitoring

Monitor the `embedding.log` queue for operation details:

```json
{
  "textPreview": "Sample text...",
  "embeddingSize": 768,
  "timestamp": "2025-05-21T12:00:00Z",
  "status": "SUCCESS",
  "id": "550e8400-e29b-41d4-a716-446655440000"
}
```

## License

MIT
