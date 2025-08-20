# Metadata Differentiation Implementation Summary

## Overview
This document summarizes the implementation of metadata differentiation based on download source in the embedProc service. The system now automatically detects document types from file URLs and applies different metadata accordingly.

## Implementation Details

### 1. New DocumentType Enum
- **File**: `src/main/java/com/baskettecase/embedProc/service/DocumentType.java`
- **Purpose**: Defines document types based on URL path patterns
- **Types**:
  - `POLICY`: Documents from `/policies` paths
  - `REFERENCE`: Documents from `/reference` paths  
  - `UNKNOWN`: Documents from other paths

### 2. Enhanced TextWithMetadata Class
- **File**: `src/main/java/com/baskettecase/embedProc/service/EmbeddingService.java`
- **New Fields**:
  - `documentType`: The detected document type
  - `sourcePath`: The source URL/path of the document
- **Backward Compatibility**: Existing constructors still work

### 3. Updated Metadata Creation
- **Policy Documents**: Include `refnum1`, `refnum2`, `timestamp`, `sourcePath` metadata
- **Reference Documents**: Include `doctype: "information"`, `timestamp`, `sourcePath` metadata (no refnums)
- **Unknown Documents**: Include `documentType: "unknown"`, `timestamp`, `sourcePath` metadata, plus refnums if available
- **All Documents**: Include `sourcePath` when available

### 4. Modified Processors
- **ScdfStreamProcessor**: Updated all 4 metadata creation points to include document type and source path
- **StandaloneDirectoryProcessor**: Updated to use new metadata structure for local files
- **FileDownloaderService**: No changes needed (detection happens in processors)

## Usage Examples

### Policy Document
```java
// URL: http://example.com/policies/contract.txt
// DocumentType: POLICY
// Metadata: refnum1, refnum2, timestamp, sourcePath
// No additional doctype field
```

### Reference Document  
```java
// URL: http://example.com/reference/manual.txt
// DocumentType: REFERENCE
// Metadata: timestamp, doctype: "information", sourcePath
// Note: No refnum1/refnum2 for reference documents
```

### Unknown Document
```java
// URL: http://example.com/documents/file.txt
// DocumentType: UNKNOWN
// Metadata: refnum1, refnum2, timestamp, documentType: "unknown", sourcePath
```

## Code Changes Summary

### Files Modified:
1. **DocumentType.java** - New enum for document type detection
2. **EmbeddingService.java** - Enhanced metadata creation and TextWithMetadata class
3. **ScdfStreamProcessor.java** - Updated all metadata creation calls
4. **StandaloneDirectoryProcessor.java** - Updated metadata creation calls
5. **MonitorService.java** - Fixed missing serverPort field

### Files Added:
1. **DocumentTypeTest.java** - Unit tests for DocumentType enum
2. **MetadataDemo.java** - Demonstration of metadata system

## Testing
- All existing tests pass
- New DocumentType tests verify correct URL pattern matching
- Demo shows metadata creation for different document types

## Benefits
1. **Automatic Detection**: No manual configuration needed
2. **Appropriate Metadata**: Each document type gets only the metadata it needs
3. **Reference Documents**: No unnecessary refnums for informational content
4. **Policy Documents**: Full refnum tracking for policy management
5. **Backward Compatibility**: Existing functionality preserved
6. **Extensible**: Easy to add new document types in the future
7. **Debugging**: Source path and document type help with troubleshooting

## Future Enhancements
- Add more document types (e.g., CONTRACT, MANUAL, etc.)
- Configurable metadata rules via properties
- Custom metadata templates per document type
- Integration with external classification services
