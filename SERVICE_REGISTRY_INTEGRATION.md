# EmbedProc Service Registry Integration

This document describes the service registry integration added to embedProc to match the configuration used in hdfsWatcher.

## 🎯 **Overview**

EmbedProc has been configured to work with Cloud Foundry Service Registry (Eureka) for service discovery, matching the setup used in hdfsWatcher. This enables the service to register itself with the service registry when deployed via SCDF with the `imc-services` binding.

## 🔧 **Changes Made**

### 1. **Maven Dependencies Added**

Added the following dependencies to `pom.xml`:

```xml
<!-- Service Registry Dependencies for Cloud Foundry -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
<dependency>
    <groupId>io.pivotal.cfenv</groupId>
    <artifactId>java-cfenv-boot</artifactId>
    <version>2.4.0</version>
</dependency>
<dependency>
    <groupId>io.pivotal.spring.cloud</groupId>
    <artifactId>spring-cloud-services-starter-service-registry</artifactId>
    <version>4.1.3</version>
</dependency>
```

### 2. **Service Registry Configuration**

Added to `application-cloud.properties`:

```properties
# Service Registry Configuration
spring.cloud.service-registry.auto-registration.enabled=true
spring.cloud.service-registry.auto-registration.register-management=true
spring.cloud.service-registry.auto-registration.fail-fast=false
```

### 3. **Enhanced Management Endpoints**

Updated management endpoints to support service discovery:

```properties
# Enhanced management endpoints for service discovery
management.endpoints.web.exposure.include=health,metrics,info,env,configprops,discovery,prometheus
management.endpoint.health.show-details=always
management.endpoint.health.show-components=always
management.endpoint.metrics.enabled=true
management.metrics.export.prometheus.enabled=true
```

### 4. **Service Discovery Logging**

Added detailed logging configuration for service discovery debugging:

```properties
# Service discovery logging
logging.level.org.springframework.cloud.netflix.eureka=DEBUG
logging.level.com.netflix.discovery=DEBUG
logging.level.org.springframework.cloud.service=DEBUG

# Application logging
logging.level.com.baskettecase.embedProc=INFO

# Reduce Spring Cloud Stream debug logging to prevent log rate limits
logging.level.org.springframework.cloud.stream=WARN
logging.level.org.springframework.integration=WARN
logging.level.org.springframework.amqp=WARN
logging.level.org.springframework.messaging=WARN

# Reduce general Spring debug logging
logging.level.org.springframework=WARN
```

### 5. **Enhanced Logging Pattern**

Updated logging pattern to include trace information:

```properties
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId:-},%X{spanId:-}] %logger{36} - %msg%n
```

## 🚀 **SCDF Deployment Integration**

### Stream Definition Binding

When deploying via SCDF, bind the service registry using the deployer property:

```
deployer.embedProc.cloudfoundry.services: "imc-services"
```

This binding will:
- ✅ Automatically configure Eureka client settings from the bound service
- ✅ Enable service registration with the service registry
- ✅ Expose discovery endpoints for service monitoring
- ✅ Provide service discovery capabilities to other applications

### Service Registration Behavior

With these configurations:

1. **Auto-Registration**: Service automatically registers with Eureka on startup
2. **Management Registration**: Management endpoints are also registered for monitoring
3. **Fail-Fast Disabled**: Service will start even if service registry is temporarily unavailable
4. **Health Checks**: Service registry health is included in actuator health checks

## 📊 **Available Endpoints**

With the service registry integration, the following endpoints are now available:

| Endpoint | Description |
|----------|-------------|
| `/actuator/discovery` | Service discovery information |
| `/actuator/health` | Health status including service registry |
| `/actuator/info` | Application information |
| `/actuator/env` | Environment properties |
| `/actuator/configprops` | Configuration properties |
| `/actuator/metrics` | Application metrics |
| `/actuator/prometheus` | Prometheus metrics |

## 🔍 **Verification**

### 1. **Compilation Test** ✅
```bash
./mvnw -q compile
```

### 2. **Unit Tests** ✅
```bash
./mvnw -q test -Dtest=DocumentTypeTest
```

### 3. **Service Registry Health Check**

When deployed, check service registry status:
```bash
curl http://your-app-url/actuator/health
```

Look for `eureka` or `serviceRegistry` in the health response.

### 4. **Discovery Endpoint**

Check service discovery information:
```bash
curl http://your-app-url/actuator/discovery
```

## 🎉 **Benefits**

1. **Service Discovery**: Other services can discover embedProc through the service registry
2. **Load Balancing**: Enables client-side load balancing for multiple instances
3. **Health Monitoring**: Service registry provides centralized health monitoring
4. **Dynamic Configuration**: Supports dynamic service configuration updates
5. **Fault Tolerance**: Improved resilience through service registry patterns

## 🔧 **Configuration Notes**

- **No Code Changes Required**: The integration is purely configuration-based
- **Cloud Profile Only**: Service registry features are only active in the `cloud` profile
- **Backward Compatible**: Existing functionality remains unchanged
- **Auto-Configuration**: Spring Cloud Services handles most configuration automatically

## 📋 **Matching hdfsWatcher**

EmbedProc now has the same service registry configuration as hdfsWatcher:

| Feature | hdfsWatcher | embedProc |
|---------|-------------|-----------|
| **Eureka Client** | ✅ | ✅ |
| **CF Environment** | ✅ | ✅ |
| **Service Registry Starter** | ✅ | ✅ |
| **Auto-Registration** | ✅ | ✅ |
| **Management Endpoints** | ✅ | ✅ |
| **Service Discovery Logging** | ✅ | ✅ |
| **Enhanced Health Checks** | ✅ | ✅ |

Both services are now ready for deployment with service registry binding via SCDF's `deployer.*.cloudfoundry.services: "imc-services"` configuration.
