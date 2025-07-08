# Gotchas and Edge Cases

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
- **Issue**: Very small chunks (< 100 words) may lose context
- **Solution**: Default 300 words with 30-word overlap provides good balance
- **Recommendation**: Use 200-500 words for optimal results

### Paragraph Boundary Detection
- **Issue**: Documents without clear paragraph breaks may create very large chunks
- **Solution**: Fallback to word-based splitting for large paragraphs
- **Prevention**: Ensure documents have proper paragraph formatting

### Memory Usage
- **Issue**: Large documents with many small chunks can increase memory usage
- **Solution**: Process chunks in batches (10 at a time)
- **Monitoring**: Watch memory usage with very large documents

### Performance Impact
- **Issue**: More chunks = more API calls to embedding service
- **Solution**: Batch processing reduces database round trips
- **Balance**: Smaller chunks improve search precision but increase processing time
