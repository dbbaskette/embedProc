# embedProc: Text Embedding Processor

A Spring Cloud Data Flow (SCDF) processor that generates and stores text embeddings using Ollama's Nomic model and PostgreSQL with pgvector.

## Features

- 🚀 Stream processing of text messages
- 🔍 Generates embeddings using Ollama's Nomic model
- 💾 Stores embeddings in PostgreSQL with pgvector
- 📊 Handles large documents with automatic chunking
- 📝 Logs all operations for monitoring

## Prerequisites

- Java 21+
- Maven
- PostgreSQL with pgvector extension
- Ollama server running with Nomic model

## Quick Start

1. **Build the application**:
   ```bash
   mvn clean package
   ```

2. **Run with SCDF profile**:
   ```bash
   java -jar target/embedProc-0.0.1-SNAPSHOT.jar \
     --spring.profiles.active=scdf \
     --spring.datasource.url=jdbc:postgresql://localhost:5432/yourdb \
     --spring.datasource.username=youruser \
     --spring.datasource.password=yourpassword \
     --spring.ai.ollama.embedding.model=nomic-embed-text \
     --spring.ai.ollama.base-url=http://localhost:11434
   ```

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
