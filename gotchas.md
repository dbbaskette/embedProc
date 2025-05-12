# Gotchas

## pgvector Storage Issues

- **Database Connection Failure:**
  - If the app cannot connect to Postgres, embeddings will not be stored. Check your datasource properties and environment variables.
  - Look for error logs from `VectorStoreService`.

- **Table/Schema Errors:**
  - If the `embeddings` table does not exist or has the wrong schema, storage will fail. Spring AI should auto-create the table if configured correctly.
  - Check for error messages in logs.

- **General Troubleshooting:**
  - Success and failure of embedding storage are logged by `VectorStoreService`.
  - Use these logs to verify that embeddings are being persisted, and to diagnose issues.


## Application Fails to Start: Missing RestClient.Builder

If you see an error like:

> Parameter 1 of method ollamaApi in org.springframework.ai.autoconfigure.ollama.OllamaAutoConfiguration required a bean of type 'org.springframework.web.client.RestClient$Builder' that could not be found.

This means the `spring-boot-starter-web` dependency is missing from your `pom.xml`. This starter is required for Spring Boot to auto-configure the `RestClient.Builder` bean needed by Spring AI's Ollama integration.

**Solution:**
Add the following to your dependencies:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

After adding, rebuild your project.
