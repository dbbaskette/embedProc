# Gotchas and Edge Cases

## Spring Configuration Issues

### Embedding Model Bean Conflicts
- **Issue**: Multiple embedding models (Ollama and OpenAI) causing Spring bean conflicts
- **Error**: "found 2: ollamaEmbeddingModel,openAiEmbeddingModel"
- **Solution**: Profile-based `@Primary` bean configuration in `EmbeddingModelConfig`
- **Prevention**: Use profile-specific configurations for multi-provider setups
- **Impact**: Resolves Cloud Foundry deployment failures

### Maven Parameter Name Retention
- **Issue**: Spring Framework 6.1+ requires parameter name information for dependency injection
- **Error**: "This may be due to missing parameter name information"
- **Solution**: Added `-parameters` compiler flag in Maven configuration
- **Prevention**: Always include `-parameters` flag for Spring Framework 6.1+

## Logging and Performance

### Cloud Foundry Log Rate Limits
- **Issue**: Verbose logging can trigger Cloud Foundry log rate limits
- **Solution**: Reduced logging levels in `application-cloud.properties`
- **Impact**: Prevents application termination due to excessive logging

### Circular Dependency
- **Issue**: `ApplicationConfig` had unused `@Autowired` field causing circular dependency
- **Solution**: Removed unused field
- **Prevention**: Always check for unused autowired fields

## Message Processing

### Multiple Message Formats
- **Issue**: Upstream processors send messages in various formats (JSON, plain text, byte arrays)
- **Solution**: Enhanced `extractFileUrl()` method handles multiple formats
- **Supported Formats**:
  - JSON with `fileUrl`, `url`, `file_url`, `content` fields
  - Plain text: "Processed file: http://..."
  - Direct URLs or content

### WebHDFS URL Encoding
- **Issue**: WebHDFS URLs have double-encoding issues (`%2520` instead of `%20`)
- **Solution**: `fixWebHdfsUrl()` method handles encoding and adds required `?op=OPEN` parameter
- **Prevention**: Always use the URL fixing method for WebHDFS URLs

## Enhanced Chunking Strategy

### Chunk Size Considerations
- **Issue**: Very small chunks (< configurable minimum meaningful words) provide poor context for Q&A
- **Solution**: Default 1000 words with 150-word overlap and configurable meaningful word minimum provides better Q&A context
- **Recommendation**: Use 800-1200 words for optimal Q&A results
- **Whitespace handling**: Algorithm ignores excessive spaces and empty lines in word counting
- **Configurable minimum**: Set `app.chunking.min-meaningful-words` to adjust minimum chunk size

### Paragraph Boundary Detection
- **Issue**: Documents without clear paragraph breaks may create very large chunks
- **Solution**: Enhanced algorithm combines short paragraphs to create meaningful chunks of at least 100 words
- **Prevention**: Ensure documents have proper paragraph formatting for optimal chunking

### Memory Usage
- **Issue**: Large documents with many small chunks can increase memory usage
- **Solution**: Process chunks in batches (10 at a time)
- **Monitoring**: Watch memory usage with very large documents

### Performance Impact
- **Issue**: More chunks = more API calls to embedding service
- **Solution**: Batch processing reduces database round trips
- **Balance**: Smaller chunks improve search precision but increase processing time
