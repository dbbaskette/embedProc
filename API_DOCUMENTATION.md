# API Documentation

This document provides documentation for the API endpoints available in the `embedProc` application.

## Base Path

The base path for all API endpoints is `/api/processing`.

**Note:** These endpoints are only active when the application is run with the `cloud` profile.

---

## Endpoints

### 1. Get Processing State

- **Method:** `GET`
- **Path:** `/state`
- **Description:** Retrieves the current state of the processing service.
- **Response Body:** A JSON object containing the processing state information.

**Example Response:**

```json
{
    "enabled": true,
    "status": "PROCESSING",
    "consumerStatus": "RUNNING",
    "lastChanged": "2025-08-24T12:00:00Z",
    "lastChangeReason": "Started via API endpoint",
    "timestamp": "2025-08-24T12:01:00Z"
}
```

### 2. Start Processing

- **Method:** `POST`
- **Path:** `/start`
- **Description:** Starts the file processing service.
- **Response Body:** A JSON object confirming the action and providing the current state.

**Example Response:**

```json
{
    "success": true,
    "message": "Processing started successfully",
    "stateChanged": true,
    "enabled": true,
    "status": "PROCESSING",
    "consumerStatus": "RUNNING",
    "lastChanged": "2025-08-24T12:05:00Z",
    "timestamp": "2025-08-24T12:05:00Z"
}
```

### 3. Stop Processing

- **Method:** `POST`
- **Path:** `/stop`
- **Description:** Stops the file processing service.
- **Response Body:** A JSON object confirming the action and providing the current state.

**Example Response:**

```json
{
    "success": true,
    "message": "Processing stopped successfully. Messages will remain in queue.",
    "stateChanged": true,
    "enabled": false,
    "status": "STOPPED",
    "consumerStatus": "PAUSED",
    "lastChanged": "2025-08-24T12:10:00Z",
    "timestamp": "2025-08-24T12:10:00Z"
}
```

### 4. Toggle Processing

- **Method:** `POST`
- **Path:** `/toggle`
- **Description:** Toggles the processing state between enabled and disabled.
- **Response Body:** A JSON object confirming the action and providing the current and previous states.

**Example Response:**

```json
{
    "success": true,
    "message": "Processing stopped successfully. Previous state: enabled, Current state: disabled. Messages will remain in queue.",
    "action": "stopped",
    "previousState": {
        "enabled": true,
        "status": "STARTED"
    },
    "currentState": {
        "enabled": false,
        "status": "STOPPED",
        "consumerStatus": "PAUSED"
    },
    "lastChanged": "2025-08-24T12:15:00Z",
    "timestamp": "2025-08-24T12:15:00Z"
}
```

### 5. Get Files Processed Count

- **Method:** `GET`
- **Path:** `/files-processed`
- **Description:** Retrieves the number of files that have been processed by the application.
- **Response Body:** A JSON object containing file processing statistics.

**Example Response:**

```json
{
    "filesProcessed": 42,
    "filesTotal": 100,
    "timestamp": "2025-08-24T12:20:00Z"
}
```

**Response Fields:**
- `filesProcessed`: Number of files successfully processed
- `filesTotal`: Total number of files to be processed (if known)
- `timestamp`: Current timestamp when the response was generated

### 6. Get Comprehensive Processing Status

- **Method:** `GET`
- **Path:** `/status`
- **Description:** Retrieves comprehensive processing status including file counts, processing state, system metrics, and error information.
- **Response Body:** A JSON object with detailed processing information.

**Example Response:**

```json
{
    "processing": {
        "enabled": true,
        "status": "STARTED",
        "consumerStatus": "CONSUMING",
        "lastChanged": "2025-08-24T12:00:00Z",
        "lastChangeReason": "Initial state"
    },
    "files": {
        "processed": 42,
        "total": 100,
        "current": "document-123.txt"
    },
    "chunks": {
        "processed": 850,
        "total": 2000,
        "pending": 1150,
        "rate": 12.5
    },
    "system": {
        "status": "PROCESSING",
        "uptime": "2h 15m",
        "memoryMB": 512,
        "errors": 0,
        "lastError": null
    },
    "timestamp": "2025-08-24T12:25:00Z"
}
```

**Response Fields:**
- `processing`: Current processing state and configuration
- `files`: File processing statistics
- `chunks`: Text chunk processing metrics  
- `system`: System health and performance metrics
- `timestamp`: Current timestamp when the response was generated

## Troubleshooting

### No Files Being Processed

If `filesProcessed` shows 0 despite the application running:

1. **Check Processing State**: Use `GET /state` to verify processing is enabled
2. **Check Message Queue**: In Cloud Foundry deployments, verify messages are being published to the `textInput` RabbitMQ queue
3. **Check Logs**: Review application logs for processing activity and errors
4. **Use Comprehensive Status**: Use `GET /status` to get detailed system information

### 7. Reset Processing Counters

- **Method:** `POST`
- **Path:** `/reset-counters`
- **Description:** Resets all processing counters (files processed, chunks processed, errors, etc.) to zero. Useful for testing or starting fresh tracking.
- **Response Body:** A JSON object confirming the reset operation.

**Example Response:**

```json
{
    "success": true,
    "message": "All processing counters have been reset to zero",
    "timestamp": "2025-08-24T14:30:00Z"
}
```

**Response Fields:**
- `success`: Always true if the operation succeeds
- `message`: Confirmation message
- `timestamp`: Current timestamp when the reset was performed

**Note:** This operation resets:
- Files processed count
- Files total count  
- Chunks processed count
- Total chunks count
- Error count
- Current file being processed
- Last error message
