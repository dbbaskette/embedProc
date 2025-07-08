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

## Instance Startup Reporting

**Automatic Behavior:**
- Each instance reports itself to metrics queue on startup
- Uses `ApplicationReadyEvent` to ensure app is fully initialized
- Instance ID format: `{appName}-{instanceIndex}`

**Configuration:**
```properties
# Enable RabbitMQ metrics publishing (cloud profile)
app.monitoring.rabbitmq.enabled=true
app.monitoring.rabbitmq.queue-name=embedproc.metrics
```

**Startup Message Format:**
```json
{
  "instanceId": "embedProc-0",
  "timestamp": "2025-01-03T10:30:00Z",
  "totalChunks": 0,
  "processedChunks": 0,
  "errorCount": 0,
  "processingRate": 0.0,
  "uptime": "0h 0m",
  "status": "STARTED"
}
```



## Required Dependencies
- `spring-boot-starter-web`: Required for embedding with Spring AI Ollama integration (provides `RestClient.Builder` bean)

## Common Startup Errors
- **Missing RestClient.Builder**: Add `spring-boot-starter-web` to your dependencies if you see UnsatisfiedDependencyException for `RestClient.Builder`.

## Typical pom.xml snippet
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
