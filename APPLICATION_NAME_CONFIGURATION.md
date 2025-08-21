# EmbedProc Application Name Configuration

This document describes the application name configuration for service registry registration.

## 🎯 **Overview**

The application name is crucial for service registry registration as it identifies the service in the Eureka service registry. EmbedProc is now configured with proper application naming across all profiles.

## 🔧 **Configuration Applied**

### 1. **Base Application Properties**

Added to `src/main/resources/application.properties`:
```properties
# Application name for service registry and identification
spring.application.name=embedProc
```

This provides the default application name for all profiles.

### 2. **Cloud Profile Configuration**

Already configured in `src/main/resources/application-cloud.properties`:
```properties
# Cloud Foundry specific settings
spring.application.name=${vcap.application.name:embedProc}
info.app.name=@project.artifactId@
info.app.version=@project.version@
```

This configuration:
- ✅ Uses the Cloud Foundry application name from `VCAP_APPLICATION` if available
- ✅ Falls back to `embedProc` if not in Cloud Foundry environment
- ✅ Sets info endpoints with Maven project details

### 3. **Standalone Profile**

The standalone profile inherits the application name from the base properties, which is appropriate since it doesn't need service registry registration.

## 🚀 **Service Registry Registration**

With this configuration, when deployed via SCDF with service registry binding:

```
deployer.embedProc.cloudfoundry.services: "imc-services"
```

The service will register with the following characteristics:

| Property | Value | Source |
|----------|-------|--------|
| **Service Name** | `embedProc` (or CF app name) | `spring.application.name` |
| **Instance ID** | Auto-generated | Eureka client |
| **Management Port** | Same as server port | Auto-configuration |
| **Health Check** | `/actuator/health` | Spring Boot Actuator |

## 📊 **Registration Verification**

### 1. **Check Application Name**
```bash
curl http://your-app-url/actuator/info
```

Expected response:
```json
{
  "app": {
    "name": "embedProc",
    "version": "6.0.0"
  }
}
```

### 2. **Check Service Registry Health**
```bash
curl http://your-app-url/actuator/health
```

Look for `eureka` or `serviceRegistry` in the health response.

### 3. **Check Discovery Information**
```bash
curl http://your-app-url/actuator/discovery
```

This will show service discovery details including the registered service name.

## 🎉 **Benefits**

1. **Consistent Naming**: Application name is consistent across all environments
2. **Service Discovery**: Other services can discover embedProc by name
3. **Load Balancing**: Multiple instances register under the same service name
4. **Health Monitoring**: Service registry can monitor health by service name
5. **Configuration Flexibility**: Respects Cloud Foundry application naming

## 🔍 **Profile-Specific Behavior**

| Profile | Application Name | Service Registry |
|---------|------------------|------------------|
| **Default** | `embedProc` | Not registered |
| **Standalone** | `embedProc` | Not registered |
| **Cloud** | `${vcap.application.name:embedProc}` | ✅ Registered |

## ✅ **Verification Complete**

- ✅ Application name configured in base properties
- ✅ Cloud Foundry integration configured
- ✅ Service registry dependencies available
- ✅ Compilation successful
- ✅ Ready for SCDF deployment with service binding

EmbedProc is now properly configured to register with the service registry using the correct application name! 🎯
