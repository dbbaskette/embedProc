# Reference Numbers Implementation Guide

## Overview

This document explains the implementation of reference number support in the embedProc application. The feature allows storing two 6-digit integer reference numbers (`refnum1` and `refnum2`) alongside text embeddings in the pgvector table.

## Architecture

The reference number functionality is implemented using Spring AI's Document metadata feature, which stores additional data alongside embeddings in the PostgreSQL pgvector table without requiring schema changes.

### Key Components

1. **ReferenceNumberEmbeddingService** - Enhanced service for storing embeddings with reference numbers
2. **ReferenceNumberVectorQueryProcessor** - Query processor with reference number filtering capabilities
3. **ReferenceNumberStandaloneProcessor** - Example processor demonstrating usage
4. **ReferenceNumberConfig** - Configuration properties for reference number validation

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

### Storing Embeddings with Reference Numbers

```java
@Autowired
private ReferenceNumberEmbeddingService embeddingService;

// Store single embedding with reference numbers
embeddingService.storeEmbeddingWithReferenceNumbers(
    "Your text content here", 
    123456,  // refnum1
    789012   // refnum2
);

// Store multiple embeddings in batch
List<TextWithReferenceNumbers> textList = Arrays.asList(
    new TextWithReferenceNumbers("Text 1", 123456, 789012),
    new TextWithReferenceNumbers("Text 2", 234567, 890123)
);
embeddingService.storeEmbeddingsWithReferenceNumbers(textList);
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
embeddingService.storeEmbeddingsWithReferenceNumbers(textList);

// Parallel batch processing (faster for large datasets)
embeddingService.storeEmbeddingsWithReferenceNumbersParallel(textList);
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
    
    private final ReferenceNumberEmbeddingService embeddingService;
    private final ReferenceNumberVectorQueryProcessor queryProcessor;
    
    public MyReferenceNumberProcessor(
            ReferenceNumberEmbeddingService embeddingService,
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
        
        embeddingService.storeEmbeddingsWithReferenceNumbers(data);
        
        // Query with reference number filtering
        queryProcessor.runQueryWithReferenceNumbers("AI", 3, 123456, null);
        
        // Get statistics
        queryProcessor.getReferenceNumberStatistics();
    }
}
```

## Migration from Existing Code

To migrate existing code to use reference numbers:

1. Replace `EmbeddingService` with `ReferenceNumberEmbeddingService`
2. Replace `VectorQueryProcessor` with `ReferenceNumberVectorQueryProcessor`
3. Update method calls to include reference number parameters
4. Add reference number configuration properties

## Troubleshooting

### Common Issues

1. **Validation Errors**: Ensure reference numbers are 6-digit integers (100000-999999)
2. **Profile Issues**: Make sure to activate the correct Spring profile (`standalone-refnum`)
3. **Configuration**: Verify reference number properties are properly configured

### Debug Logging

Enable debug logging for reference number operations:

```properties
logging.level.com.baskettecase.embedProc.service.ReferenceNumberEmbeddingService=DEBUG
logging.level.com.baskettecase.embedProc.processor.ReferenceNumberVectorQueryProcessor=DEBUG
```

## Future Enhancements

Potential improvements for production use:

1. **Custom SQL Queries**: Implement direct PostgreSQL queries for better filtering performance
2. **Indexing Strategy**: Add specific indexes on reference number metadata fields
3. **Advanced Filtering**: Support range queries and complex filtering conditions
4. **Bulk Operations**: Enhanced bulk insert/update operations for large datasets
