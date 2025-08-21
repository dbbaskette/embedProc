# Information Document Type Implementation

This document describes the implementation of support for information documents stored in the `/processed_files/` directory that don't follow the policy document naming pattern.

## 🎯 **Problem Addressed**

Files like `glossary.pdf.txt` in the `/processed_files/` directory were:
- ✅ Being processed successfully
- ❌ Generating warnings about not matching the refnum pattern
- ❌ Getting default reference numbers (100001-200001)
- ❌ Not getting the appropriate `doctype=information` metadata

## 🔧 **Solution Implemented**

### 1. **Added New Document Type**

Enhanced `DocumentType.java`:
```java
public enum DocumentType {
    POLICY("policy"),
    REFERENCE("reference"),
    INFORMATION("information"),  // NEW
    UNKNOWN("unknown");
}
```

### 2. **Updated Path Detection Logic**

Enhanced `DocumentType.fromUrl()` method:
```java
public static DocumentType fromUrl(String fileUrl) {
    String lowerUrl = fileUrl.toLowerCase();
    if (lowerUrl.contains("/policies")) {
        return POLICY;
    } else if (lowerUrl.contains("/reference")) {
        return REFERENCE;
    } else if (lowerUrl.contains("/processed_files")) {  // NEW
        return INFORMATION;
    } else {
        return UNKNOWN;
    }
}
```

### 3. **Updated Metadata Generation**

Enhanced both `createDocumentWithMetadata()` and `storeEmbeddingWithMetadata()` methods:
```java
switch (documentType) {
    case POLICY:
        // Validate and include reference numbers
        validateReferenceNumber(refnum1, "refnum1");
        validateReferenceNumber(refnum2, "refnum2");
        metadata.put("refnum1", refnum1);
        metadata.put("refnum2", refnum2);
        break;
    case REFERENCE:
    case INFORMATION:  // NEW - Same treatment as REFERENCE
        // Add doctype=information (no refnums needed)
        metadata.put("doctype", "information");
        break;
    case UNKNOWN:
    default:
        // Existing logic for backward compatibility
        break;
}
```

### 4. **Enhanced Tests**

Added comprehensive tests for the new `INFORMATION` document type:
```java
@Test
public void testInformationDocumentType() {
    assertEquals(DocumentType.INFORMATION, 
        DocumentType.fromUrl("http://big-data-005.kuhn-labs.com:9870/webhdfs/v1/processed_files/glossary.pdf.txt?op=OPEN"));
    assertEquals(DocumentType.INFORMATION, 
        DocumentType.fromUrl("/processed_files/guide.pdf.txt"));
}
```

## 🎉 **Expected Results**

### **Before Changes**
```
2025-08-21 12:21:00.196 [textproc-embedproc.rag-stream-1] WARN [,] c.b.e.processor.ScdfStreamProcessor - Filename does not match pattern <refnum1>-<refnum2>.[ext].txt: glossary.pdf.txt
2025-08-21 12:21:00.196 [textproc-embedproc.rag-stream-1] INFO [,] c.b.e.processor.ScdfStreamProcessor - Using default reference numbers - refnum1: 100001, refnum2: 200001
```

**Metadata stored**:
```json
{
  "refnum1": 100001,
  "refnum2": 200001,
  "timestamp": 1692625260196,
  "sourcePath": "http://big-data-005.kuhn-labs.com:9870/webhdfs/v1/processed_files/glossary.pdf.txt?op=OPEN"
}
```

### **After Changes**
No warning messages, clean processing.

**Metadata stored**:
```json
{
  "doctype": "information",
  "timestamp": 1692625260196,
  "sourcePath": "http://big-data-005.kuhn-labs.com:9870/webhdfs/v1/processed_files/glossary.pdf.txt?op=OPEN"
}
```

## 📊 **Document Type Mapping**

| Source Path | Document Type | Metadata |
|-------------|---------------|----------|
| `/policies/` | `POLICY` | `refnum1`, `refnum2` |
| `/reference/` | `REFERENCE` | `doctype=information` |
| `/processed_files/` | `INFORMATION` | `doctype=information` |
| Other paths | `UNKNOWN` | `documentType`, optional refnums |

## 🔍 **Path Detection Examples**

| URL | Detected Type |
|-----|---------------|
| `http://hdfs.com:9870/webhdfs/v1/policies/123456-789012.pdf.txt` | `POLICY` |
| `http://hdfs.com:9870/webhdfs/v1/reference/manual.txt` | `REFERENCE` |
| `http://hdfs.com:9870/webhdfs/v1/processed_files/glossary.pdf.txt` | `INFORMATION` |
| `http://hdfs.com:9870/webhdfs/v1/uploads/document.txt` | `UNKNOWN` |

## ✅ **Benefits**

1. **Clean Processing**: No more warnings for information documents
2. **Appropriate Metadata**: Information documents get `doctype=information`
3. **No Reference Numbers**: Information documents don't get unnecessary refnums
4. **Consistent Behavior**: Same treatment as reference documents
5. **Backward Compatibility**: Existing policy and reference documents unchanged

## 🚀 **Deployment Impact**

When deployed, files in `/processed_files/` will:
- ✅ Process without warnings
- ✅ Get `doctype=information` metadata
- ✅ Not get reference number validation
- ✅ Be properly categorized for search and retrieval

## 📋 **Testing Results**

- ✅ All existing tests pass
- ✅ New INFORMATION type tests pass
- ✅ Compilation successful
- ✅ Case-insensitive path matching works
- ✅ getValue() method returns correct string

The system now properly handles information documents stored in `/processed_files/` with appropriate metadata! 🎯
