# Gotchas

## pgvector Storage Issues

- **Database Connection Failure:**
  - If the app cannot connect to Postgres, embeddings will not be stored. Check your datasource properties and environment variables.
  - Look for error logs from `EmbeddingService`.
  - The service will retry failed operations up to 3 times before giving up.

- **Table/Schema Errors:**
  - If the `embeddings` table does not exist or has the wrong schema, storage will fail. Spring AI should auto-create the table if configured correctly.
  - Check for error messages in logs.

- **General Troubleshooting:**
  - Success and failure of embedding storage are logged by `EmbeddingService`.
  - Monitor the `embeddings.processed` and `embeddings.errors` metrics for operational insights.
  - Use these logs to verify that embeddings are being persisted, and to diagnose issues.

## Version Compatibility Issues

- **Spring Boot Version Mismatch:**
  - Ensure `pom.xml` versions match those specified in `versions.txt`
  - Version mismatches can cause dependency resolution issues

- **JAR File Versioning:**
  - Scripts must reference the correct JAR version (currently 0.0.5)
  - Check `standalone.sh`, `manifest.yml`, and build scripts for version consistency

- **Spring Boot 3.5.3 vs Spring Cloud Version Compatibility:**
  - **Issue**: Spring Boot 3.5.3 is not compatible with Spring Cloud 2024.0.x
  - **Error**: `Spring Boot [3.5.3] is not compatible with this Spring Cloud release train`
  - **Solution**: Use Spring Cloud 2025.0.0 (Northfields) with Spring Boot 3.5.x
  - **Fix Applied**: Updated `spring-cloud.version` from `2024.0.1` to `2025.0.0`
  - **Status**: ✅ Fixed - All tests passing


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
