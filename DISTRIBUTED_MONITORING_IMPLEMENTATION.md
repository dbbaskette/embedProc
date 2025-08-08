# Distributed Monitoring Implementation

## Overview

This implementation adds **distributed monitoring capabilities** alongside the existing local UI, solving the horizontal scaling monitoring problem where multiple instances create fragmented UI views.

## Architecture

### Current Solution
```
[LB] → [Worker+LocalUI] [Worker+LocalUI] [Worker+LocalUI]  ✅ Local UIs remain
           ↓              ↓              ↓
        [RabbitMQ] → [Future Centralized Monitor]           ✅ Unified view
```

### Benefits
- **Local UI**: Still available for development, debugging, and direct instance inspection
- **Distributed Monitoring**: Each instance publishes metrics to RabbitMQ for centralized aggregation
- **Zero Breaking Changes**: Existing functionality preserved
- **Flexible Configuration**: Can enable/disable RabbitMQ publishing per environment

## Implementation Details

### 1. Core Services

#### MonitorService
- **Purpose**: Aggregates metrics and provides monitoring data
- **Features**:
  - Tracks total chunks, processed chunks, errors
  - Calculates processing rate and uptime
  - Determines instance status (IDLE/PROCESSING/ERROR)
  - Auto-generates instance IDs from app name + instance index

#### MetricsPublisher (Interface)
- **RabbitMQMetricsPublisher**: Publishes metrics to RabbitMQ with circuit breaker
- **NoOpMetricsPublisher**: No-op implementation when RabbitMQ is disabled

### 2. Message Format

Published to RabbitMQ as JSON:
```json
{
  "instanceId": "embedProc-worker-1",
  "timestamp": "2025-01-03T10:30:00Z",
  "totalChunks": 1500,
  "processedChunks": 750,
  "errorCount": 2,
  "processingRate": 12.5,
  "uptime": "2h 15m",
  "status": "PROCESSING",
  "meta": { "service": "embedProc" }
}
```

### 3. Configuration

#### Enable Distributed Monitoring (Cloud Profile)
```properties
# application-cloud.properties
app.monitoring.rabbitmq.enabled=true
app.monitoring.rabbitmq.queue-name=pipeline.metrics
```

#### Local Development
```properties
# application-local.properties
app.monitoring.rabbitmq.enabled=false  # Just local UI
spring.main.web-application-type=servlet  # Enable web for UI
```

#### Test RabbitMQ Locally
```properties
app.monitoring.rabbitmq.enabled=true
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

### 4. Local UI Features

- **Real-time Dashboard**: Beautiful, responsive monitoring interface
- **Auto-refresh**: Updates every 5 seconds (configurable)
- **Progress Tracking**: Visual progress bars and metrics
- **Instance Details**: ID, status, uptime, processing rate
- **Error Monitoring**: Error counts and success rates

#### Accessing the UI
- **Local Development**: `http://localhost:8080/` (with `local` profile)
- **Cloud Deployment**: Main application URL (load balancer will route)

### 5. Integration Points

#### EmbeddingService Integration
```java
// Automatic metrics tracking
monitorService.incrementTotalChunks(texts.size());    // On batch start
monitorService.incrementProcessedChunks(1);           // On success
monitorService.incrementErrors(1);                    // On error
```

#### Circuit Breaker Protection
- RabbitMQ failures don't affect processing
- 1-minute circuit breaker timeout
- Automatic recovery when RabbitMQ returns

## Usage Examples

### 1. Local Development with UI Only
```bash
java -jar embedProc.jar --spring.profiles.active=standalone,local
# Access UI at http://localhost:8080/
```

### 2. Cloud Deployment with Distributed Monitoring
```bash
# Cloud deployment (automatic via manifest.yml)
# Publishes metrics to RabbitMQ + provides local UI
# Access UI at main app URL
```

### 3. API Access
```bash
# Get metrics via REST API
curl http://localhost:8080/api/metrics
```

## Configuration Reference

| Property | Default | Description |
|----------|---------|-------------|
| `app.monitoring.rabbitmq.enabled` | `false` | Enable RabbitMQ metrics publishing |
| `app.monitoring.rabbitmq.queue-name` | `pipeline.metrics` | RabbitMQ queue name |
| `spring.main.web-application-type` | `servlet` | Enable web for monitoring UI |

## Future Centralized Dashboard

The RabbitMQ metrics stream is now ready for your centralized dashboard application, which could:

1. **Consume** from `pipeline.metrics` queue
2. **Aggregate** metrics from all instances
3. **Display** unified dashboard showing:
   - Total system throughput
   - Per-instance health status
   - Overall progress across all instances
   - Error rates and performance trends
   - Real-time system overview

## Benefits Achieved

✅ **Dual Monitoring Strategy**: Local UI + Distributed metrics  
✅ **Horizontal Scale Support**: No more UI bouncing between instances  
✅ **Development Friendly**: Local UI still available for debugging  
✅ **Production Ready**: Circuit breaker, retry logic, error handling  
✅ **Zero Breaking Changes**: All existing functionality preserved  
✅ **Flexible Configuration**: Enable/disable per environment  
✅ **Modern UI**: Beautiful, responsive monitoring interface  
✅ **API Access**: REST endpoints for programmatic access