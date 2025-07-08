# Quick Reference

## pgvector Integration (Spring AI)

**Required Environment Variables / Properties:**
- `POSTGRES_HOST` (e.g. localhost)
- `POSTGRES_PORT` (e.g. 5432)
- `POSTGRES_DB` (your database name)
- `POSTGRES_USER` (your username)
- `POSTGRES_PASSWORD` (your password)

**Spring Boot Properties (can be set in application-cloud.properties):**
```properties
spring.datasource.url=jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}
spring.datasource.username=${POSTGRES_USER}
spring.datasource.password=${POSTGRES_PASSWORD}
spring.ai.vectorstore.pgvector.enabled=true
spring.ai.vectorstore.pgvector.table-name=embeddings
```

- Table name defaults to `embeddings` but can be changed.
- All values can be set via environment variables or deployment properties.

**Log message formats:**
- Success: `[VectorStoreService] Successfully stored embedding for text preview: '...'`
- Error: `[VectorStoreService] Failed to store embedding for text preview: '...'. Error: <error message>`

## Monitoring Configuration

**RabbitMQ Metrics Publishing:**
```properties
# Enable RabbitMQ metrics publishing (cloud profile)
app.monitoring.rabbitmq.enabled=true
app.monitoring.rabbitmq.queue-name=embedproc.metrics
```

**Instance Startup Reporting:**
- Automatic behavior: Each instance reports on startup
- Instance ID format: `{appName}-{instanceIndex}`
- See `DISTRIBUTED_MONITORING_IMPLEMENTATION.md` for details



## Required Dependencies
- `spring-boot-starter-web`: Required for embedding with Spring AI Ollama integration (provides `RestClient.Builder` bean)

## Cloud Mode Features

**Message Format Support:**
- JSON with `fileUrl`, `url`, `file_url`, or `content` fields
- Plain text with "Processed file:" prefix
- Byte array messages (auto-converted to UTF-8)

**WebHDFS Support:**
- Automatic detection of `/webhdfs/` URLs
- Handles double-encoding issues (`%2520` → `%20`)
- Adds required `?op=OPEN` parameter
- Uses `URI` instead of `String` to prevent re-encoding

**Performance Optimizations:**
- Chunk size: 2000 characters (was 1000)
- Overlap: 200 characters (was 100)
- Batch processing: 10 embeddings per batch
- Connection pooling: 20 max connections

## Common Startup Errors
- **Missing RestClient.Builder**: Add `spring-boot-starter-web` to your dependencies if you see UnsatisfiedDependencyException for `RestClient.Builder`.

## Typical pom.xml snippet
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
