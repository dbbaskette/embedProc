# Quick Reference

## Configuration Properties

### Core Properties
- `app.processor.mode`: Deployment mode (`standalone`, `cloud`)
- `app.query.text`: Default query for vector operations

### Database Configuration
- `spring.datasource.url`: PostgreSQL connection URL
- `spring.datasource.username`: Database username
- `spring.datasource.password`: Database password
- `spring.datasource.driver-class-name`: PostgreSQL driver

### AI Model Configuration
- `spring.ai.ollama.base-url`: Ollama server URL (standalone)
- `spring.ai.ollama.embedding.model`: Ollama model name
- `spring.ai.openai.api-key`: OpenAI API key (cloud)
- `spring.ai.openai.embedding.options.model`: OpenAI model name

### Vector Store Configuration
- `spring.ai.vectorstore.pgvector.enabled`: Enable pgvector
- `spring.ai.vectorstore.pgvector.dimensions`: Vector dimensions (768 for Ollama, 1536 for OpenAI)
- `spring.ai.vectorstore.pgvector.distance-type`: Distance metric (COSINE_DISTANCE)
- `spring.ai.vectorstore.pgvector.table-name`: Table name (embeddings)
- `spring.ai.vectorstore.pgvector.initialize-schema`: Auto-create schema

### Directory Configuration (Standalone)
- `app.processor.standalone.input-directory`: Input files location
- `app.processor.standalone.processed-directory`: Processed files location
- `app.processor.standalone.error-directory`: Error files location

### Monitoring Configuration
- `app.monitoring.rabbitmq.enabled`: Enable RabbitMQ metrics publishing
- `app.monitoring.rabbitmq.queue-name`: RabbitMQ queue name for metrics
- `app.monitoring.instance-id`: Optional stable instance ID override
- `app.monitoring.public-app-uri`: Optional public app URI to derive `publicHostname`

### Enhanced Chunking Configuration
- `app.chunking.max-words-per-chunk`: Maximum words per chunk (800-1200 recommended for Q&A)
- `app.chunking.overlap-words`: Overlap words between chunks for context continuity
- `app.chunking.min-meaningful-words`: Minimum meaningful words per chunk (default: 100)

## Deployment Profiles

### Local Development
```bash
./mvnw spring-boot:run -Dspring.profiles.active=local
```

### Standalone Processing
```bash
./mvnw spring-boot:run -Dspring.profiles.active=standalone
```

### Cloud/SCDF Deployment
```bash
./mvnw spring-boot:run -Dspring.profiles.active=cloud
```

## Key Features

### Enhanced Text Chunking
- **Semantic boundaries**: Paragraph-based splitting preserves document structure
- **Configurable size**: 1000 words per chunk (default, configurable)
- **Minimum size**: Configurable meaningful words minimum (default: 100, ignoring excessive whitespace)
- **Overlap**: 150 words overlap maintains context continuity
- **Better Q&A context**: Larger chunks provide more comprehensive context for question answering
- **Whitespace filtering**: Ignores excessive spaces and empty lines in word counting

### Message Processing
- **Multiple formats**: JSON, plain text, byte arrays
- **URL extraction**: Supports `fileUrl`, `url`, `file_url`, `content` fields
- **WebHDFS support**: Specialized handling for Hadoop Distributed File System
- **Fallback processing**: Direct content processing for backward compatibility

### Monitoring & Metrics
- **Instance reporting**: Automatic startup reporting to metrics queue
- **Processing metrics**: Success/error counters with batch processing
- **Health checks**: Custom health indicators for vector store connectivity
- **Performance monitoring**: Batch processing and connection pooling optimizations
