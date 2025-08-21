# Simplified Processed Files Logic Implementation

This document describes the simplified logic for handling all files from the `/processed_files/` directory based on their filename patterns.

## 🎯 **Simplified Approach**

All files are now in `/processed_files/` directory, so the logic is much simpler:

1. **Files with refnum pattern** (`123456-789012.pdf.txt`) → **POLICY** type with refnum metadata
2. **Files without refnum pattern** (`glossary.pdf.txt`) → **INFORMATION** type with `doctype=information`

## 🔧 **Implementation Details**

### **Enhanced DocumentType.fromUrl() Logic**

```java
public static DocumentType fromUrl(String fileUrl) {
    String lowerUrl = fileUrl.toLowerCase();
    if (lowerUrl.contains("/policies")) {
        return POLICY;
    } else if (lowerUrl.contains("/reference")) {
        return REFERENCE;
    } else if (lowerUrl.contains("/processed_files")) {
        // Extract filename and check pattern
        String filename = extractFilename(fileUrl);
        if (filename != null && matchesRefnumPattern(filename)) {
            return POLICY; // Has refnum pattern, treat as policy
        } else {
            return INFORMATION; // No refnum pattern, treat as information
        }
    } else {
        return UNKNOWN;
    }
}
```

### **Pattern Matching Logic**

```java
private static boolean matchesRefnumPattern(String filename) {
    // Pattern: 6 digits, dash, 6 digits, then anything
    return filename.matches("^\\d{6}-\\d{6}\\..+");
}
```

### **Filename Extraction**

```java
private static String extractFilename(String fileUrl) {
    // Remove query parameters and extract filename from path
    String urlWithoutQuery = fileUrl.split("\\?")[0];
    int lastSlash = urlWithoutQuery.lastIndexOf('/');
    if (lastSlash >= 0 && lastSlash < urlWithoutQuery.length() - 1) {
        return urlWithoutQuery.substring(lastSlash + 1);
    }
    return null;
}
```

## 📊 **Processing Examples**

### **Policy Documents (with refnum pattern)**

| Filename | Pattern Match | Document Type | Metadata |
|----------|---------------|---------------|----------|
| `123456-789012.pdf.txt` | ✅ Matches | `POLICY` | `refnum1: 123456`, `refnum2: 789012` |
| `100001-200001.docx.txt` | ✅ Matches | `POLICY` | `refnum1: 100001`, `refnum2: 200001` |
| `555555-666666.doc.txt` | ✅ Matches | `POLICY` | `refnum1: 555555`, `refnum2: 666666` |

### **Information Documents (no refnum pattern)**

| Filename | Pattern Match | Document Type | Metadata |
|----------|---------------|---------------|----------|
| `glossary.pdf.txt` | ❌ No match | `INFORMATION` | `doctype: "information"` |
| `manual-guide.txt` | ❌ No match | `INFORMATION` | `doctype: "information"` |
| `readme.txt` | ❌ No match | `INFORMATION` | `doctype: "information"` |
| `123-456.txt` | ❌ Too few digits | `INFORMATION` | `doctype: "information"` |

## 🎉 **Expected Behavior**

### **For `glossary.pdf.txt` (Your Example)**

**Before**: 
```
WARN - Filename does not match pattern <refnum1>-<refnum2>.[ext].txt: glossary.pdf.txt
INFO - Using default reference numbers - refnum1: 100001, refnum2: 200001
```

**After**: 
```
INFO - Processing document type: INFORMATION
(No warnings, clean processing)
```

**Metadata stored**:
```json
{
  "doctype": "information",
  "timestamp": 1692625260196,
  "sourcePath": "http://big-data-005.kuhn-labs.com:9870/webhdfs/v1/processed_files/glossary.pdf.txt?op=OPEN"
}
```

### **For `123456-789012.pdf.txt` (Policy Document)**

**Processing**:
```
INFO - Processing document type: POLICY
INFO - Extracted refnum1: 123456, refnum2: 789012
```

**Metadata stored**:
```json
{
  "refnum1": 123456,
  "refnum2": 789012,
  "timestamp": 1692625260196,
  "sourcePath": "http://big-data-005.kuhn-labs.com:9870/webhdfs/v1/processed_files/123456-789012.pdf.txt?op=OPEN"
}
```

## 🔍 **Pattern Matching Rules**

The refnum pattern requires:
- **Exactly 6 digits**
- **Single dash**
- **Exactly 6 digits**
- **Dot and extension**

| Pattern | Matches | Reason |
|---------|---------|--------|
| `123456-789012.pdf.txt` | ✅ | Perfect match |
| `100001-200001.docx.txt` | ✅ | Perfect match |
| `123-456.txt` | ❌ | Too few digits |
| `1234567-789012.txt` | ❌ | Too many digits |
| `123456_789012.txt` | ❌ | Underscore instead of dash |
| `glossary.pdf.txt` | ❌ | No numeric pattern |

## ✅ **Benefits of Simplified Approach**

1. **Single Directory**: All files in `/processed_files/` - no path complexity
2. **Pattern-Based**: Simple filename pattern determines document type
3. **No Configuration**: No need to configure multiple directories
4. **Clean Processing**: No warnings for information documents
5. **Appropriate Metadata**: Each type gets correct metadata automatically
6. **Backward Compatible**: Existing policy documents still work

## 📋 **Testing Coverage**

- ✅ Policy documents with refnum pattern in `/processed_files/`
- ✅ Information documents without refnum pattern in `/processed_files/`
- ✅ Edge cases (wrong digit count, wrong separators)
- ✅ URL query parameter handling
- ✅ Case-insensitive directory matching
- ✅ Existing `/policies/` and `/reference/` directories still work

The implementation is now much simpler and handles your use case perfectly! 🎯
