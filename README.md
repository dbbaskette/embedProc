<img src="images/embedProc.jpg" alt="embedProc logo" width="200"/>

# embedProc: Spring Cloud Data Flow Embedding Processor

## Overview
embedProc is a Spring Cloud Data Flow (SCDF) stream processor that generates embedding vectors for input text using the Ollama Nomic model via Spring AI. It is designed for streaming data pipelines, enabling downstream services to consume and store embeddings for further processing or retrieval.

## Features
- Listens for text messages on an input queue (when the `scdf` profile is active)
- Uses the Ollama Nomic embedding model (or compatible) to generate embedding vectors
- Stores embeddings in PostgreSQL with pgvector via Spring AI's VectorStore
- Publishes embedding vectors to an output queue
- Logs all outgoing embeddings and persistence results for traceability

## Architecture
```
[Input Queue] --> [embedProc Processor] --> [Postgres/pgvector DB]
```
- Embeddings are generated using Spring AI EmbeddingModel (Ollama Nomic)
- Embeddings and input text are persisted directly in Postgres/pgvector using VectorStore
- All processing and storage events are logged
- No output queue is used; embeddings are stored only in the database

## How it works
- embedProc listens for text messages on an input channel (e.g., RabbitMQ, Kafka, or direct invocation).
- For each message, it generates an embedding and stores it in the Postgres/pgvector database.
- No message is sent downstream; the embedding is only persisted in the database.

## Usage
Run the processor with the `scdf` profile active. Example:
```sh
java -jar target/embedProc-0.0.1-SNAPSHOT.jar --spring.profiles.active=scdf \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/mydb \
  --spring.datasource.username=myuser \
  --spring.datasource.password=mypassword \
  --spring.ai.ollama.embedding.model=nomic-embed-text \
  --spring.ai.ollama.base-url=http://localhost:11434
```

### Example Message Flow
- Input: Message with a String payload (text)
- Side effect: Embedding and text persisted in Postgres/pgvector
- No output message is produced

## Setup
1. Clone the repository and ensure Java 21+ and Maven are installed.
2. Configure your Postgres instance with pgvector extension enabled.
3. Build the project:
   ```sh
   mvn clean package
   ```

## Configuration
Set the following properties (via `application-scdf.properties`, environment variables, or deployment properties):
```properties
spring.datasource.url=jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}
spring.datasource.username=${POSTGRES_USER}
spring.datasource.password=${POSTGRES_PASSWORD}
spring.ai.ollama.embedding.model=nomic-embed-text
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.vectorstore.pgvector.enabled=true
spring.ai.vectorstore.pgvector.table-name=embeddings
spring.profiles.active=scdf
```

## Usage
Run the processor with the `scdf` profile active. Example:
```sh
java -jar target/embedProc-0.0.1-SNAPSHOT.jar --spring.profiles.active=scdf \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/mydb \
  --spring.datasource.username=myuser \
  --spring.datasource.password=mypassword \
  --spring.ai.ollama.embedding.model=nomic-embed-text \
  --spring.ai.ollama.base-url=http://localhost:11434
```

### Example Message Flow
- Input: Message with a String payload (text)
- Output: Message with a List<Double> embedding payload
- Side effect: Embedding and text persisted in Postgres/pgvector

## Troubleshooting
- All outgoing embeddings and storage events are logged.
- Success log example:
  ```
  [VectorStoreService] Successfully stored embedding for text preview: 'This is a sample input...', size: 1536
  ```
- Error log example:
  ```
  [VectorStoreService] Failed to store embedding for text preview: 'This is a sample input...'. Error: <error message>
  ```
- For database or schema issues, see `gotchas.md`.

## Contribution
Contributions are welcome! Please open issues or pull requests for improvements, bug fixes, or new features.

## Project Structure & Ignore Rules

This project uses a `.gitignore` file to exclude build artifacts from version control. The `target/` directory, which contains compiled classes and other build outputs, is ignored by default. This helps keep the repository clean and prevents accidental commits of generated files.


This project provides a Spring Cloud Data Flow (SCDF) stream processor that generates embedding vectors for input text using the Ollama Nomic model via Spring AI. It is intended for use in streaming data pipelines where you need to convert text to embeddings for downstream processing.

## Features
- Listens for text messages on an input queue (when the `scdf` profile is active)
- Uses the Ollama Nomic model (or other supported Ollama embedding models) to generate embedding vectors
- Converts the embedding array to a `List<Double>` for compatibility
- Publishes the embedding vector to an output queue

## Required Configuration

You must provide the following configuration values, either in your `application-scdf.properties`, as environment variables, or as command-line arguments:

| Property                              | Example Value                | Required | Purpose                        |
|----------------------------------------|------------------------------|----------|--------------------------------|
| `spring.ai.ollama.embedding.model`     | `nomic-embed-text`           | Yes      | Which embedding model to use   |
| `spring.ai.ollama.base-url`            | `http://localhost:11434`     | If not default | Ollama server location      |
| `spring.profiles.active`               | `scdf`                       | Yes      | Activate SCDF profile          |

### Example `application-scdf.properties`
```properties
spring.ai.ollama.embedding.model=nomic-embed-text
spring.ai.ollama.base-url=http://localhost:11434
spring.profiles.active=scdf
```

### Example Command-Line Launch
```sh
java -jar your-app.jar \
  --spring.profiles.active=scdf \
  --spring.ai.ollama.embedding.model=nomic-embed-text \
  --spring.ai.ollama.base-url=http://localhost:11434
```

### Example Environment Variables
```sh
export SPRING_AI_OLLAMA_EMBEDDING_MODEL=nomic-embed-text
export SPRING_AI_OLLAMA_BASE_URL=http://localhost:11434
```

## Logging
You can increase logging verbosity by setting (in your properties file):
```properties
logging.level.com.example.embeddingprocessor=DEBUG
```

## Additional Notes
- Input and output channel names can be set by SCDF if needed.
- The processor is only active when the `scdf` profile is enabled.

## License
MIT
