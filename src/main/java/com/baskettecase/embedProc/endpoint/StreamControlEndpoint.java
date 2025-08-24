package com.baskettecase.embedProc.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.cloud.stream.endpoint.BindingsEndpoint;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Custom Spring Boot Actuator endpoint for controlling Spring Cloud Stream bindings.
 * Provides start/stop operations for message consumers at runtime.
 */
@Component
@Endpoint(id = "stream-control")
public class StreamControlEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(StreamControlEndpoint.class);
    
    private final BindingsEndpoint bindingsEndpoint;
    
    public StreamControlEndpoint(BindingsEndpoint bindingsEndpoint) {
        this.bindingsEndpoint = bindingsEndpoint;
    }
    
    /**
     * Controls the specified binding based on the action parameter.
     * 
     * @param bindingName The name of the binding to control (e.g., "embedProc-in-0")
     * @param action The action to perform: "start", "stop", or "status"
     * @return Result of the operation
     */
    @WriteOperation
    public Map<String, Object> controlBinding(String bindingName, String action) {
        logger.info("Controlling binding: {} with action: {}", bindingName, action);
        
        try {
            // Query current state
            var currentState = bindingsEndpoint.queryState(bindingName);
            logger.info("Current state before {}: {}", action, currentState);
            
            // Handle different actions
            switch (action.toLowerCase()) {
                case "start":
                    return startBindingInternal(bindingName, currentState);
                case "stop":
                    return stopBindingInternal(bindingName, currentState);
                case "status":
                    return getBindingStateInternal(bindingName, currentState);
                default:
                    return Map.of(
                        "operation", action,
                        "bindingName", bindingName,
                        "success", false,
                        "error", "Unsupported action. Use 'start', 'stop', or 'status'"
                    );
            }
            
        } catch (Exception e) {
            logger.error("Failed to control binding {} with action {}: {}", bindingName, action, e.getMessage(), e);
            return Map.of(
                "operation", action,
                "bindingName", bindingName,
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * Internal method to start a binding.
     */
    private Map<String, Object> startBindingInternal(String bindingName, Object currentState) {
        try {
            // Start the binding using the documented REST approach
            changeBindingState(bindingName, "STARTED");
            
            // Wait a moment for the change to take effect
            Thread.sleep(1000);
            
            // Query new state
            var newState = bindingsEndpoint.queryState(bindingName);
            
            return Map.of(
                "operation", "start",
                "bindingName", bindingName,
                "success", true,
                "previousState", currentState,
                "newState", newState
            );
            
        } catch (Exception e) {
            logger.error("Failed to start binding {}: {}", bindingName, e.getMessage(), e);
            return Map.of(
                "operation", "start",
                "bindingName", bindingName,
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * Internal method to stop a binding.
     */
    private Map<String, Object> stopBindingInternal(String bindingName, Object currentState) {
        try {
            // Stop the binding using the documented REST approach
            changeBindingState(bindingName, "STOPPED");
            
            // Wait a moment for the change to take effect
            Thread.sleep(1000);
            
            // Query new state
            var newState = bindingsEndpoint.queryState(bindingName);
            
            return Map.of(
                "operation", "stop",
                "bindingName", bindingName,
                "success", true,
                "previousState", currentState,
                "newState", newState
            );
            
        } catch (Exception e) {
            logger.error("Failed to stop binding {}: {}", bindingName, e.getMessage(), e);
            return Map.of(
                "operation", "stop",
                "bindingName", bindingName,
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * Internal method to get binding state.
     */
    private Map<String, Object> getBindingStateInternal(String bindingName, Object currentState) {
        return Map.of(
            "operation", "status",
            "bindingName", bindingName,
            "success", true,
            "state", currentState
        );
    }
    
    /**
     * Helper method to change binding state using reflection.
     */
    private void changeBindingState(String bindingName, String stateName) throws Exception {
        logger.info("Attempting to change binding {} to state {}", bindingName, stateName);
        
        // Find changeState method that accepts State enum
        Class<?> bindingsClass = bindingsEndpoint.getClass();
        java.lang.reflect.Method[] methods = bindingsClass.getMethods();
        
        boolean methodFound = false;
        for (java.lang.reflect.Method method : methods) {
            if ("changeState".equals(method.getName())) {
                Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length == 2 && 
                    String.class.equals(paramTypes[0]) && 
                    paramTypes[1].isEnum()) {
                    
                    // Found the enum-based method, get the correct enum value
                    Object[] enumConstants = paramTypes[1].getEnumConstants();
                    for (Object enumConstant : enumConstants) {
                        if (stateName.equals(enumConstant.toString())) {
                            method.invoke(bindingsEndpoint, bindingName, enumConstant);
                            logger.info("Successfully called changeState with {} enum", stateName);
                            methodFound = true;
                            break;
                        }
                    }
                    if (methodFound) break;
                }
            }
        }
        
        if (!methodFound) {
            throw new RuntimeException("Unable to find appropriate changeState method or enum value for " + stateName);
        }
    }
}