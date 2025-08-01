# Metadata Implementation Guide

## Overview

This document explains the implementation of metadata support in the embedProc application. The feature allows storing two 6-digit integer reference numbers (`refnum1` and `refnum2`) as metadata alongside text embeddings in the pgvector table.

## Architecture

The metadata functionality is implemented using Spring AI's Document metadata feature, which stores additional data alongside embeddings in the PostgreSQL pgvector table without requiring schema changes.

### Key Components

1. **EmbeddingService** - Unified service that handles both plain embeddings and embeddings with metadata
2. **MetadataConfig** - Configuration properties for metadata validation
3. **ScdfStreamProcessor** - Cloud deployment processor with metadata support
4. **StandaloneDirectoryProcessor** - Local directory processor with metadata support

### Design Philosophy

**Metadata is just an optional parameter, not a separate service.** The EmbeddingService handles both cases:
- Plain embeddings: `storeEmbedding(text)`
- Embeddings with metadata: `storeEmbeddingWithMetadata(text, refnum1, refnum2)`

This unified approach eliminates service duplication and follows single responsibility principles.

## Implementation Details

### Document Metadata Storage

Reference numbers are stored as metadata in the Spring AI Document object:

```java
Map<String, Object> metadata = new HashMap<>();
metadata.put("refnum1", refnum1);
metadata.put("refnum2", refnum2);
metadata.put("timestamp", System.currentTimeMillis());

Document doc = new Document(text, metadata);
```

### Database Schema

The pgvector table automatically stores metadata in a JSONB column, allowing for flexible storage of reference numbers without explicit schema modifications.

## Usage Examples

### Storing Embeddings with Metadata

```java
@Autowired
private EmbeddingService embeddingService;

// Store plain embedding (no metadata)
embeddingService.storeEmbedding("Your text content here");

// Store embedding with metadata
embeddingService.storeEmbeddingWithMetadata(
    "Your text content here", 
    123456,  // refnum1
    789012   // refnum2
);

// Store multiple embeddings with metadata in parallel
List<EmbeddingService.TextWithMetadata> textList = Arrays.asList(
    new EmbeddingService.TextWithMetadata("Text 1", 123456, 789012),
    new EmbeddingService.TextWithMetadata("Text 2", 234567, 890123)
);
embeddingService.storeEmbeddingsWithMetadataParallel(textList);
```

### Querying with Reference Numbers

```java
@Autowired
private ReferenceNumberVectorQueryProcessor queryProcessor;

// Regular similarity search
queryProcessor.runQuery("search query", 5);

// Similarity search with reference number filtering
queryProcessor.runQueryWithReferenceNumbers(
    "search query", 
    5,       // topK results
    123456,  // refnum1 filter (null to ignore)
    null     // refnum2 filter (null to ignore)
);

// Find documents by reference numbers only
queryProcessor.findByReferenceNumbers(123456, 789012, 10);

// Get statistics about reference numbers in database
queryProcessor.getReferenceNumberStatistics();
```

## Configuration

### Application Properties

Add the following configuration to your application properties files:

```properties
# Reference Number Configuration
app.reference-numbers.enable-validation=true
app.reference-numbers.min-value=100000
app.reference-numbers.max-value=999999
app.reference-numbers.enable-indexing=true
app.reference-numbers.batch-size=10
app.reference-numbers.enable-parallel-processing=true
```

### Configuration Properties

| Property | Description | Default |
|----------|-------------|---------|
| `app.reference-numbers.enable-validation` | Enable 6-digit validation | `true` |
| `app.reference-numbers.min-value` | Minimum reference number value | `100000` |
| `app.reference-numbers.max-value` | Maximum reference number value | `999999` |
| `app.reference-numbers.enable-indexing` | Enable metadata indexing | `true` |
| `app.reference-numbers.batch-size` | Batch processing size | `10` |
| `app.reference-numbers.enable-parallel-processing` | Enable parallel processing | `true` |

## Spring Profiles

### Using Reference Number Processor

To use the reference number standalone processor, activate the `standalone-refnum` profile:

```bash
java -jar embedProc.jar --spring.profiles.active=standalone-refnum,local
```

## Validation

Reference numbers are validated to ensure they are 6-digit integers:

- **Range**: 100000 - 999999
- **Type**: Integer (not null)
- **Validation**: Automatic validation in service methods

## Performance Considerations

### Batch Processing

The service supports batch processing for better performance:

```java
// Sequential batch processing
embeddingService.storeEmbeddingsWithMetadataParallel(textList);

// Parallel batch processing (faster for large datasets)
embeddingService.storeEmbeddingsWithMetadataParallelParallel(textList);
```

### Query Performance

- Reference number filtering is currently implemented using in-memory filtering
- For production use with large datasets, consider implementing custom SQL queries for better performance
- The pgvector table's JSONB metadata column supports indexing for improved query performance

## Error Handling

The implementation includes comprehensive error handling:

- **Validation Errors**: Invalid reference numbers are rejected with descriptive error messages
- **Processing Errors**: Individual embedding failures don't stop batch processing
- **Query Errors**: Query failures are logged and handled gracefully

## Monitoring

The reference number functionality integrates with the existing monitoring system:

- Processed chunks counter includes reference number embeddings
- Error counter tracks reference number processing failures
- Monitor service provides real-time statistics

## Examples

### Complete Usage Example

```java
@Component
@Profile("standalone-refnum")
public class MyReferenceNumberProcessor implements CommandLineRunner {
    
    private final EmbeddingService embeddingService;
    private final ReferenceNumberVectorQueryProcessor queryProcessor;
    
    public MyReferenceNumberProcessor(
            EmbeddingService embeddingService,
            ReferenceNumberVectorQueryProcessor queryProcessor) {
        this.embeddingService = embeddingService;
        this.queryProcessor = queryProcessor;
    }
    
    @Override
    public void run(String... args) throws Exception {
        // Store embeddings with reference numbers
        List<TextWithReferenceNumbers> data = Arrays.asList(
            new TextWithReferenceNumbers("Document about AI", 123456, 789012),
            new TextWithReferenceNumbers("Document about ML", 234567, 890123)
        );
        
        embeddingService.storeEmbeddingsWithMetadataParallel(data);
        
        // Query with reference number filtering
        queryProcessor.runQueryWithReferenceNumbers("AI", 3, 123456, null);
        
        // Get statistics
        queryProcessor.getReferenceNumberStatistics();
    }
}
```

## Architecture Benefits

The unified EmbeddingService provides several advantages:

1. **Single Responsibility**: One service handles embedding, metadata is just optional data
2. **Reduced Complexity**: No need to choose between different embedding services  
3. **Code Reduction**: Eliminated ~400+ lines of duplicate code
4. **Logical Design**: Service named after primary function (embedding), not secondary features (metadata)
5. **Easier Testing**: Single service to test with both plain and metadata scenarios

## Migration from Existing Code

**No migration needed!** The EmbeddingService now handles both cases:
- Use `storeEmbedding(text)` for plain embeddings
- Use `storeEmbeddingWithMetadata(text, refnum1, refnum2)` for embeddings with metadata

All processors automatically use the unified service.

## Troubleshooting

### Common Issues

1. **Validation Errors**: Ensure reference numbers are 6-digit integers (100000-999999)
2. **Profile Issues**: Make sure to activate the correct Spring profile (`standalone` or `cloud`)
3. **Configuration**: Verify reference number properties are properly configured

### Debug Logging

Enable debug logging for reference number operations:

```properties
logging.level.com.baskettecase.embedProc.service.EmbeddingService=DEBUG
logging.level.com.baskettecase.embedProc.processor.ReferenceNumberVectorQueryProcessor=DEBUG
```

## Future Enhancements

Potential improvements for production use:

1. **Custom SQL Queries**: Implement direct PostgreSQL queries for better filtering performance
2. **Indexing Strategy**: Add specific indexes on reference number metadata fields
3. **Advanced Filtering**: Support range queries and complex filtering conditions
4. **Bulk Operations**: Enhanced bulk insert/update operations for large datasets
