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

A high-performance text embedding processor built with **Spring AI** that supports both **standalone** and **Spring Cloud Data Flow (SCDF)** deployment modes. Generate and store text embeddings using multiple AI providers with PostgreSQL and pgvector for efficient vector operations.

## 🚀 Key Features

- **🔄 Dual Deployment Modes**: Standalone directory processing or SCDF stream processing
- **🤖 Multiple AI Providers**: 
  - **Ollama** (local inference) for standalone mode
  - **OpenAI** for cloud/SCDF deployments
- **💾 Vector Storage**: PostgreSQL with pgvector extension for similarity search
- **📊 Smart Document Processing**: Automatic chunking with configurable overlap
- **🔁 Resilient Operations**: Built-in retry mechanisms and error handling
- **📈 Observability**: Metrics collection and comprehensive logging
- **�️ Monitoring UI**: Web-based monitoring interface with real-time metrics
- **�🔒 Cloud-Ready**: Cloud Foundry and Kubernetes deployment support

## 📋 Prerequisites

- **Java 21+**
- **Maven 3.6.3+**
- **PostgreSQL 14+** with pgvector extension
- **Ollama server** (for standalone mode)
- **OpenAI API key** (for SCDF/cloud mode)

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

### 🖥️ Local Mode with Monitoring UI

**Use Case**: Same as standalone but with web-based monitoring interface.

#### Run with UI
```bash
java -jar target/embedProc-1.0.1.jar \
  --spring.profiles.active=local
```

Then open your browser to: **http://localhost:8080**

The monitoring UI shows:
- **Chunks Received**: Total text chunks created from input
- **Chunks Processed**: Successfully embedded chunks
- **Chunks Errored**: Failed processing attempts  
- **Success Rate**: Processing success percentage
- **Real-time Updates**: Auto-refreshes every 5 seconds

### ☁️ SCDF Mode (Stream Processing)

**Use Case**: Process streaming data in Spring Cloud Data Flow using OpenAI.

#### Deploy to SCDF
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
# OpenAI API Key (set via environment or SCDF deployment properties)
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

### 🌐 Cloud Mode with Monitoring UI

**Use Case**: Stream processing with web monitoring interface.

The monitoring UI is automatically available when deployed to cloud environments with the `cloud` or `scdf` profiles. Access it at the application's main URL.

## ⚙️ Configuration Reference

### Core Properties

| Property | Description | Default | Required |
|----------|-------------|---------|----------|
| `app.processor.mode` | Deployment mode | `standalone` | No |
| `spring.datasource.url` | PostgreSQL connection URL | - | Yes |
| `spring.datasource.username` | Database username | - | Yes |
| `spring.datasource.password` | Database password | - | Yes |

### Profile Overview

| Profile | Web UI | Processing Mode | Use Case |
|---------|--------|----------------|----------|
| `standalone` | ❌ | Directory batch | Headless file processing |
| `local` | ✅ | Directory batch | Local development with monitoring |
| `scdf` | ✅ | Stream processing | SCDF deployment |
| `cloud` | ✅ | Stream processing | Cloud Foundry deployment |

### Ollama Configuration (Standalone/Local)

| Property | Description | Default |
|----------|-------------|---------|
| `spring.ai.ollama.base-url` | Ollama server URL | `http://localhost:11434` |
| `spring.ai.ollama.embedding.model` | Model name | `nomic-embed-text` |

### OpenAI Configuration (SCDF/Cloud)

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

### Directory Configuration (Standalone/Local)

| Property | Description | Default |
|----------|-------------|---------|
| `app.processor.standalone.input-directory` | Input files location | `./data/input_files` |
| `app.processor.standalone.processed-directory` | Processed files location | `./data/processed_files` |
| `app.processor.standalone.error-directory` | Error files location | `./data/error_files` |

## 📊 Document Processing

### Text Chunking
- **Chunk Size**: 1000 characters
- **Overlap**: 100 characters
- **Metadata**: Preserved for each chunk

### Supported File Types
- Plain text (`.txt`)
- Easily extensible for other formats

### Processing Flow
1. **File Detection**: Monitor input directory
2. **Text Extraction**: Read and validate content
3. **Chunking**: Split large documents
4. **Embedding**: Generate vectors using AI model
5. **Storage**: Store in PostgreSQL with pgvector
6. **Cleanup**: Move processed files

## 📈 Monitoring & Observability

### Web Monitoring UI
- **Real-time Metrics**: Processing counters with auto-refresh
- **Visual Status**: Online/offline indicators
- **Success Tracking**: Processing success rates
- **Responsive Design**: Works on desktop and mobile

### Metrics
- `chunks.received` - Total text chunks created from input
- `embeddings.processed` - Successfully processed embeddings
- `embeddings.errors` - Failed embedding attempts

### Logging
```properties
# Enable detailed logging
logging.level.com.baskettecase.embedProc=DEBUG
logging.level.org.springframework.ai=INFO
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
│   ├── service/
│   │   ├── EmbeddingService.java         # Core embedding logic
│   │   └── MonitorService.java           # Monitoring metrics
│   ├── controller/MonitorController.java  # Web UI controller
│   └── processor/
│       ├── StandaloneDirectoryProcessor.java  # Standalone mode
│       ├── ScdfStreamProcessor.java           # SCDF mode
│       └── VectorQueryProcessor.java          # Query operations
└── main/resources/
    ├── application.properties                 # Base configuration
    ├── application-standalone.properties      # Standalone config
    ├── application-local.properties           # Local with UI config
    ├── application-scdf.properties           # SCDF config
    └── static/
        └── index.html                         # Monitoring UI
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
          value: "scdf"
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

- **Documentation**: Check the `implementation_details.md` for technical details
- **Known Issues**: See `gotchas.md` for common problems and solutions
- **Quick Reference**: Use `quick_reference.md` for command cheat sheets

---

<div align="center">
  <p>Built with ❤️ using Spring AI and Spring Boot</p>
</div>
