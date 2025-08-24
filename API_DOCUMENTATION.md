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
