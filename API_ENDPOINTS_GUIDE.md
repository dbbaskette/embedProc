# EmbedProc Processing Control API

This document describes the newly implemented API endpoints for controlling the processing state of the embedProc application.

## Overview

The API allows you to control whether the embedProc application processes messages from the RabbitMQ queue or remains idle, leaving messages in the queue for later processing.

## Base URL

When running in cloud profile with default settings:
```
http://localhost:8080/api/processing
```

## API Endpoints

### 1. GET /api/processing/state

**Purpose**: Get the current processing state

**Example Request**:
```bash
curl -X GET http://localhost:8080/api/processing/state
```

**Example Response**:
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

### 2. POST /api/processing/start

**Purpose**: Start/enable file processing

**Example Request**:
```bash
curl -X POST http://localhost:8080/api/processing/start
```

**Example Response**:
```json
{
  "success": true,
  "message": "Processing started successfully",
  "stateChanged": true,
  "enabled": true,
  "status": "STARTED",
  "consumerStatus": "CONSUMING",
  "lastChanged": "2025-01-01T12:35:10.456Z",
  "timestamp": "2025-01-01T12:35:10.456Z"
}
```

### 3. POST /api/processing/stop

**Purpose**: Stop/disable file processing (messages remain in queue)

**Example Request**:
```bash
curl -X POST http://localhost:8080/api/processing/stop
```

**Example Response**:
```json
{
  "success": true,
  "message": "Processing stopped successfully. Messages will remain in queue.",
  "stateChanged": true,
  "enabled": false,
  "status": "STOPPED",
  "consumerStatus": "IDLE",
  "lastChanged": "2025-01-01T12:35:20.789Z",
  "timestamp": "2025-01-01T12:35:20.789Z"
}
```

### 4. POST /api/processing/toggle

**Purpose**: Toggle processing state (if enabled → disable, if disabled → enable)

**Example Request**:
```bash
curl -X POST http://localhost:8080/api/processing/toggle
```

**Example Response** (when toggling from enabled to disabled):
```json
{
  "success": true,
  "message": "Processing stopped successfully. Previous state: enabled, Current state: disabled. Messages will remain in queue.",
  "action": "stopped",
  "previousState": {
    "enabled": true,
    "status": "ENABLED"
  },
  "currentState": {
    "enabled": false,
    "status": "STOPPED",
    "consumerStatus": "IDLE"
  },
  "lastChanged": "2025-01-01T12:35:30.123Z",
  "timestamp": "2025-01-01T12:35:30.123Z"
}
```

## Behavior Details

### When Processing is ENABLED
- **Status**: `STARTED`
- **Consumer Status**: `CONSUMING`
- **Behavior**: Application processes messages from RabbitMQ queue
- **Queue**: Messages are consumed and processed

### When Processing is DISABLED  
- **Status**: `STOPPED`
- **Consumer Status**: `IDLE`
- **Behavior**: Application ignores messages, leaves them in queue
- **Queue**: Messages remain untouched, ready for when processing is re-enabled

## Response Field Explanations

| Field | Description |
|-------|-------------|
| `enabled` | Boolean indicating if processing is enabled |
| `status` | String status: `STARTED` or `STOPPED` |
| `consumerStatus` | String indicating queue behavior: `CONSUMING` or `IDLE` |
| `lastChanged` | ISO 8601 timestamp of last state change |
| `lastChangeReason` | Human-readable reason for the last state change |
| `timestamp` | ISO 8601 timestamp of the API response |
| `stateChanged` | Boolean indicating if the API call changed the state |
| `action` | (toggle only) Action performed: `started` or `stopped` |
| `previousState` | (toggle only) State before the toggle |
| `currentState` | (toggle only) State after the toggle |

## Testing the API

You can use the provided test script:

```bash
./test-api-endpoints.sh
```

This script tests all endpoints and shows the expected behavior.

## Integration Notes

### Profile Requirements
- These endpoints are **only available in the `cloud` profile**
- The `standalone` profile disables web functionality
- Web server runs on port 8080 by default (configurable via `PORT` env var)

### SCDF Integration
- When processing is disabled, the SCDF stream processor will leave messages in the queue
- Messages are not lost - they wait until processing is re-enabled
- Other instances can still process messages if they have processing enabled

### Monitoring
- All state changes are logged
- The processing state can be monitored via the `/api/processing/state` endpoint
- State changes are tracked with timestamps and reasons

## Error Handling

All endpoints return HTTP 200 for successful operations. The `success` field in the response indicates the operation result.

If processing is already in the desired state:
- `start` when already enabled: `stateChanged: false`, message indicates no change needed
- `stop` when already disabled: `stateChanged: false`, message indicates no change needed

## Use Cases

1. **Emergency Stop**: Quickly stop processing during maintenance or issues
2. **Controlled Restart**: Stop processing, perform maintenance, then restart
3. **Load Balancing**: Temporarily disable some instances to shift load
4. **Debugging**: Stop processing to examine queue contents
5. **Gradual Rollouts**: Control which instances are processing during deployments
