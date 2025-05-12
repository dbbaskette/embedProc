# Quick Reference

## pgvector Integration (Spring AI)

**Required Environment Variables / Properties:**
- `POSTGRES_HOST` (e.g. localhost)
- `POSTGRES_PORT` (e.g. 5432)
- `POSTGRES_DB` (your database name)
- `POSTGRES_USER` (your username)
- `POSTGRES_PASSWORD` (your password)

**Spring Boot Properties (can be set in application-scdf.properties):**
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
