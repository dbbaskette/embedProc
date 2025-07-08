# Gotchas

## Profile Configuration Changes

- **SCDF Profile Removed:**
  - The `application-scdf.properties` file has been removed
  - All SCDF functionality is now consolidated into the `cloud` profile
  - Java components updated to use `@Profile("cloud")` instead of `@Profile("scdf")`
  - Update deployment configurations to use `--spring.profiles.active=cloud` instead of `scdf`

- **Database Name Update:**
  - Standalone database name changed from `scdf-db` to `embedproc-db`
  - Update your local PostgreSQL setup if using the standalone profile

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

## Instance Startup Reporting Issues

- **Startup Reporting Failures:**
  - If RabbitMQ is unavailable during startup, the instance startup report will fail
  - This failure is logged but does not prevent the application from starting
  - Check logs for "Failed to report instance startup to metrics queue" messages
  - The application will continue to function normally even if startup reporting fails

- **Instance ID Generation:**
  - Instance IDs are generated as `{appName}-{instanceIndex}`
  - If `CF_INSTANCE_INDEX` or `INSTANCE_ID` environment variables are not set, defaults to "0"
  - Ensure your Cloud Foundry deployment has proper instance indexing for unique identification

- **Metrics Queue Configuration:**
  - Startup reporting requires `app.monitoring.rabbitmq.enabled=true`
  - If disabled, startup reporting will be skipped (no error)
  - Verify RabbitMQ service binding in Cloud Foundry for cloud deployments

## Spring Cloud Function Startup Warning

- **Issue**: During application startup, you may see warnings like:
  ```
  WARN o.s.c.f.c.c.BeanFactoryAwareFunctionRegistry - Failed to locate function 'embedProc' for function definition 'embedProc'. Returning null.
  ```

- **Root Cause**: This is a timing issue during Spring Cloud Function initialization. The function registry tries to locate the function before it's fully registered in the application context.

- **Impact**: This is a **false positive** - the function works correctly despite the warning. You can see successful processing in the logs:
  - "Successfully fetched WebHDFS content, length: 281936 characters"
  - "Generated 52 chunks from file content"
  - "Successfully stored embedding for text preview: '...'"

- **Solution**: No action required. This is a known behavior in Spring Cloud Function applications and doesn't affect functionality.

## SCDF Stream Processing Issues

- **Function Binding Mismatch:**
  - **Issue**: embedProc was embedding queue messages instead of processing file content
  - **Root Cause**: Spring Cloud Stream binding was incorrectly configured as `processText-in-0` instead of `embedProc-in-0`
  - **Solution**: Updated all configuration files to use correct function name `embedProc` matching the `@Bean` method
  - **Fixed Files**: `application.properties`, `application-cloud.properties`, `application-test.properties`
  - **SCDF Configuration**: Your SCDF config correctly uses `embedProc-in-0.destination: "textproc-embedproc"`
  - **Function Definition**: Added `spring.cloud.function.definition=embedProc` to ensure proper SCDF integration

- **File URL Processing:**
  - **Issue**: embedProc was treating file URLs as direct content instead of fetching file content
  - **Root Cause**: The processor expected direct file content but received JSON messages with file URLs
  - **Solution**: Enhanced `ScdfStreamProcessor` to parse JSON messages and fetch file content from URLs
  - **New Features**: 
    - JSON message parsing with multiple field name support (`fileUrl`, `url`, `file_url`, `content`)
    - HTTP file content fetching using RestTemplate
    - WebHDFS URL detection and specialized handling
    - Fallback to direct content processing for backward compatibility
  - **Dependencies**: Added `RestTemplate` bean to `ApplicationConfig` for HTTP requests
  - **WebHDFS Support**: Automatic detection of WebHDFS URLs with proper headers and redirect handling
  - **WebHDFS URL Fixes**: 
    - Handles double-encoding issues (`%2520` → `%20`) while preserving valid URL encoding
    - Automatically adds required `?op=OPEN` parameter
    - Preserves spaces as `%20` for WebHDFS compatibility
    - Only fixes double-encoded parts, doesn't decode valid URL encoding
    - Uses `URI` instead of `String` in `RestTemplate` to prevent re-encoding of already-encoded URLs

## Message Format Changes

- **Issue**: embedProc may receive messages in different formats from upstream processors
- **JSON Format**: `{"fileUrl": "http://example.com/file.txt"}` (original expected format)
- **New JSON Format**: `{"type": "HDFS", "url": "http://example.com/file.txt", "processed": true, "originalFile": "http://example.com/original.pdf"}` (from text processor)
- **Plain Text Format**: `"Processed file: http://example.com/file.txt"` (alternative format)
- **Message Type**: Messages may be received as byte arrays instead of strings
- **Impact**: If message format changes, embedProc may fail to extract URLs and treat entire message as content
- **Solution**: Updated `extractFileUrl()` method to handle multiple JSON formats and `embedProc()` to handle byte arrays
- **Detection**: Look for warnings like "Failed to parse message as JSON" in logs
- **Fallback**: If format is unrecognized, embedProc treats the entire message as direct file content

## RabbitMQ Message Content Type Warning

- **Issue**: You may see warnings like:
  ```
  WARN o.s.a.s.c.Jackson2JsonMessageConverter : Could not convert incoming message with content-type [text/plain], 'json' keyword missing.
  ```

- **Root Cause**: The `RabbitMQMetricsPublisher` was sending JSON messages with `content-type: text/plain` instead of `application/json`. This causes issues when other services try to consume these messages using `Jackson2JsonMessageConverter`.

- **Impact**: The warning appears in consuming services, but metrics publishing still works. However, it can cause parsing issues in monitoring applications.

- **Solution**: Fixed in `RabbitMQMetricsPublisher` to explicitly set `content-type: application/json` when sending messages. This ensures proper JSON message handling by consuming services.

- **Technical Details**: 
  - Messages are now sent using `Message` objects with explicit `MessageProperties`
  - Content type is set to `"application/json"` before sending
  - This prevents the `Jackson2JsonMessageConverter` warning in consuming services
