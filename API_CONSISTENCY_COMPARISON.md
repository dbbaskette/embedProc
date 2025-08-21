# API Consistency Comparison: embedProc vs hdfsWatcher vs textProc

This document compares the API endpoints across all three pipeline services to identify consistency and discrepancies.

## ✅ **CONSISTENT AREAS**

### Core Processing Control Endpoints
All three services implement the same core processing control pattern:

| Endpoint | embedProc | hdfsWatcher | textProc |
|----------|-----------|-------------|----------|
| **GET state** | `/api/processing/state` | `/api/processing-state` | `/api/processing/state` |
| **POST start** | `/api/processing/start` | `/api/processing/start` | `/api/processing/start` |
| **POST stop** | `/api/processing/stop` | `/api/processing/stop` | `/api/processing/stop` |
| **POST toggle** | `/api/processing/toggle` | `/api/processing/toggle` | `/api/processing/toggle` |

### Processing State Values
All services now use consistent processing state strings:
- **STARTED**: Processing is enabled and consuming/processing
- **STOPPED**: Processing is disabled, messages/files remain in queue/storage

### Common Response Structure
All services include these common fields:
- `status`: "success" or "error"
- `message`: Human-readable description
- `timestamp`: Response timestamp
- Processing state information

## ⚠️ **INCONSISTENCIES IDENTIFIED**

### 1. **Endpoint Path Variations**

| Service | State Endpoint Path |
|---------|-------------------|
| **embedProc** | `/api/processing/state` ✅ |
| **textProc** | `/api/processing/state` ✅ |
| **hdfsWatcher** | `/api/processing-state` ❌ |

**Recommendation**: hdfsWatcher should change to `/api/processing/state` for consistency.

### 2. **Response Field Structure**

#### GET State Response Comparison:

**embedProc**:
```json
{
  "enabled": true,
  "status": "STARTED",
  "consumerStatus": "CONSUMING",
  "lastChanged": "2025-01-01T12:34:56.789Z",
  "lastChangeReason": "Initial state",
  "timestamp": "2025-01-01T12:35:00.123Z"
}
```

**textProc**:
```json
{
  "processingState": "STARTED",
  "isProcessingEnabled": true,
  "consumerStatus": "running"
}
```

**hdfsWatcher**:
```json
{
  "status": "success",
  "processingEnabled": true,
  "processingState": "enabled",
  "timestamp": 1704067200000
}
```

### 3. **Field Naming Inconsistencies**

| Field Purpose | embedProc | textProc | hdfsWatcher |
|---------------|-----------|----------|-------------|
| **Processing enabled** | `enabled` | `isProcessingEnabled` | `processingEnabled` |
| **Processing state** | `status` | `processingState` | `processingState` |
| **Consumer status** | `consumerStatus` | `consumerStatus` | N/A |
| **Timestamp format** | ISO 8601 string | N/A | Unix timestamp |

### 4. **Consumer Status Values**

| Service | Running | Stopped |
|---------|---------|---------|
| **embedProc** | `"CONSUMING"` | `"IDLE"` |
| **textProc** | `"running"` | `"stopped"` |
| **hdfsWatcher** | N/A | N/A |

### 5. **Additional Response Fields**

**embedProc** (most comprehensive):
- `lastChanged`: Timestamp of last state change
- `lastChangeReason`: Reason for state change
- `stateChanged`: Boolean indicating if API call changed state

**hdfsWatcher** (processing-specific features):
- `immediatelyProcessedCount`: Files processed when enabling
- File management endpoints (`/api/files`, `/api/status`)

**textProc** (file tracking features):
- `/api/files/processed`: List of processed files
- `/api/files/pending`: Pending files information
- `/api/processing/reset`: Reset with HDFS clearing

### 6. **Base URL Patterns**

| Service | Base Pattern |
|---------|-------------|
| **embedProc** | `/api/processing/*` |
| **textProc** | `/api/processing/*` + `/api/files/*` |
| **hdfsWatcher** | `/api/*` (mixed patterns) |

## 🎯 **RECOMMENDED STANDARDIZATION**

### 1. **Endpoint Path Standardization**
All services should use:
```
GET    /api/processing/state
POST   /api/processing/start  
POST   /api/processing/stop
POST   /api/processing/toggle
```

### 2. **Response Field Standardization**
Standardize on embedProc's field naming (most comprehensive):

```json
{
  "enabled": boolean,
  "status": "STARTED" | "STOPPED", 
  "consumerStatus": "CONSUMING" | "IDLE",
  "lastChanged": "ISO 8601 timestamp",
  "lastChangeReason": "string",
  "timestamp": "ISO 8601 timestamp"
}
```

### 3. **Consumer Status Standardization**
Use embedProc's values:
- `"CONSUMING"`: Actively processing
- `"IDLE"`: Not processing

### 4. **Timestamp Format Standardization**
Use ISO 8601 strings consistently across all services.

### 5. **Success Response Structure**
For state-changing operations:
```json
{
  "success": true,
  "message": "descriptive message",
  "stateChanged": boolean,
  "enabled": boolean,
  "status": "STARTED" | "STOPPED",
  "consumerStatus": "CONSUMING" | "IDLE",
  "lastChanged": "ISO 8601 timestamp",
  "timestamp": "ISO 8601 timestamp"
}
```

## 📊 **Service-Specific Features to Preserve**

### embedProc
- Rich state change tracking (`lastChanged`, `lastChangeReason`)
- Detailed toggle responses with previous/current state
- Cloud profile requirement

### hdfsWatcher  
- File upload and management endpoints
- Immediate processing of pending files when enabling
- File hash tracking and reprocessing capabilities
- Comprehensive file status reporting

### textProc
- Processed files history with detailed metadata
- Pending files queue information
- HDFS integration with directory management
- Event-driven architecture with Spring events

## 🔧 **Implementation Priority**

### High Priority (Breaking Changes)
1. **hdfsWatcher**: Change `/api/processing-state` to `/api/processing/state`
2. **textProc**: Align field names (`isProcessingEnabled` → `enabled`)
3. **hdfsWatcher**: Use ISO 8601 timestamps instead of Unix timestamps

### Medium Priority (Enhancement)
1. Add `lastChanged` and `lastChangeReason` to textProc and hdfsWatcher
2. Standardize consumer status values across services
3. Add `stateChanged` field to all state-changing operations

### Low Priority (Nice to Have)
1. Standardize error response formats
2. Add comprehensive validation messages
3. Implement consistent logging patterns

## 🎉 **Current Strengths**

1. **Processing State Values**: All services now use `STARTED`/`STOPPED` ✅
2. **Core Endpoint Structure**: Similar patterns across services ✅
3. **Behavior Consistency**: All leave messages/files in place when stopped ✅
4. **HTTP Methods**: Consistent use of GET for state, POST for actions ✅

The services have strong foundational consistency with some field naming and response structure variations that can be aligned for a more unified API experience.
