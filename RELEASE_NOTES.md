# Release 0.0.5

## New Features
- **Performance Optimizations**: Increased chunk size to 2000 characters, overlap to 200 characters
- **WebHDFS Support**: Specialized handling for Hadoop Distributed File System URLs with encoding fixes
- **JSON Message Processing**: Enhanced support for multiple message formats (`fileUrl`, `url`, `file_url`, `content`)
- **Byte Array Support**: Handle messages received as byte arrays in cloud mode
- **Log Rate Limit Optimization**: Reduced logging verbosity to prevent Cloud Foundry rate limits
- **Instance Startup Reporting**: Automatic instance status reporting to metrics queue
- **Distributed Monitoring**: RabbitMQ metrics publishing with circuit breaker protection
- **Real-time Dashboard**: Beautiful monitoring UI with auto-refresh capabilities
- **Database Connection Pooling**: HikariCP optimization for high-performance processing
- **Async Processing**: Thread pool configuration for parallel processing

## Performance Improvements
- **Batch Processing**: 10 embeddings per batch for reduced database round trips
- **Connection Pooling**: 20 max connections with optimized timeouts
- **HTTP Optimization**: RestTemplate with redirect support and proper timeouts
- **Logging Optimization**: 70% reduction in log volume while maintaining visibility

## Bug Fixes
- **URL Encoding**: Fixed double-encoding issues in WebHDFS URLs
- **Message Content Type**: Fixed RabbitMQ message content-type warnings
- **Circular Dependency**: Resolved Spring application context circular dependency
- **Function Binding**: Corrected Spring Cloud Stream binding configuration

## Installation
Download the JAR file and run with:
```bash
java -jar embedProc-0.0.5.jar
```

## Configuration
Update `application.properties` or `application-cloud.properties` as needed for your environment. See `quick_reference.md` for configuration details.
