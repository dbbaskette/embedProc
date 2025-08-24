package com.baskettecase.embedProc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baskettecase.embedProc.endpoint.StreamControlEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Service for managing Spring Cloud Stream binding lifecycle.
 * Listens to processing state events and manages consumer bindings accordingly.
 * Uses a custom StreamControlEndpoint to properly pause/resume bindings
 * ensuring messages remain in the queue when processing is disabled.
 */
@Service
public class ConsumerLifecycleService {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerLifecycleService.class);
    
    // The binding name for the embedProc function input
    private static final String BINDING_NAME = "embedProc-in-0";
    
    @Autowired(required = false)
    private StreamControlEndpoint streamControlEndpoint;
    
    /**
     * Initialize the binding state to STOPPED when the application context is refreshed.
     * This ensures that regardless of the auto-startup configuration, the binding starts in STOPPED state
     * to match the ProcessingStateService default state.
     */
    @EventListener
    public void handleContextRefresh(ContextRefreshedEvent event) {
        // Use a slight delay to ensure StreamControlEndpoint is fully initialized
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 2 second delay
                logger.info("Initializing binding {} to STOPPED state", BINDING_NAME);
                changeBindingState("STOPPED");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while initializing binding state");
            }
        }).start();
    }
    
    /**
     * Event listener for processing started events.
     * @param event The processing started event
     */
    @EventListener
    public void handleProcessingStarted(ProcessingStateService.ProcessingStartedEvent event) {
        logger.info("Received processing started event, starting binding: {}", BINDING_NAME);
        changeBindingState("STARTED");
    }
    
    /**
     * Event listener for processing stopped events.
     * @param event The processing stopped event
     */
    @EventListener
    public void handleProcessingStopped(ProcessingStateService.ProcessingStoppedEvent event) {
        logger.info("Received processing stopped event, stopping binding: {}", BINDING_NAME);
        changeBindingState("STOPPED");
    }
    
    /**
     * Change the state of the binding using the StreamControlEndpoint.
     * 
     * @param stateName The state name ("STARTED" or "STOPPED")
     */
    private void changeBindingState(String stateName) {
        logger.info("Attempting to change binding {} state to {}", BINDING_NAME, stateName);
        
        if (streamControlEndpoint == null) {
            logger.warn("StreamControlEndpoint not available, cannot control binding state. " +
                       "Make sure spring-boot-starter-actuator is on the classpath and actuator endpoints are enabled.");
            return;
        }
        
        try {
            if ("STARTED".equals(stateName)) {
                var result = streamControlEndpoint.controlBinding(BINDING_NAME, "start");
                logger.info("Start binding result: {}", result);
            } else if ("STOPPED".equals(stateName)) {
                var result = streamControlEndpoint.controlBinding(BINDING_NAME, "stop");
                logger.info("Stop binding result: {}", result);
            } else {
                logger.warn("Unsupported state name: {}. Use STARTED or STOPPED.", stateName);
                return;
            }
            
        } catch (Exception e) {
            logger.error("Failed to change binding state for {}: {}", BINDING_NAME, e.getMessage(), e);
            // Don't throw the exception - log it and continue
            // This prevents the application from failing if there are binding control issues
        }
    }
    
    /**
     * Checks if the binding is currently running.
     * @return true if binding is running, false otherwise
     */
    public boolean areConsumersRunning() {
        if (streamControlEndpoint == null) {
            return false;
        }
        
        try {
            var stateResult = streamControlEndpoint.controlBinding(BINDING_NAME, "status");
            if (stateResult != null && Boolean.TRUE.equals(stateResult.get("success"))) {
                var state = stateResult.get("state");
                // Check if the state indicates running
                return state != null && state.toString().contains("running");
            }
            return false;
        } catch (Exception e) {
            logger.warn("Failed to query binding state: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets the status of the binding.
     * @return A string describing the binding status
     */
    public String getConsumerStatus() {
        if (streamControlEndpoint == null) {
            return "StreamControlEndpoint not available";
        }
        
        try {
            var stateResult = streamControlEndpoint.controlBinding(BINDING_NAME, "status");
            if (stateResult != null && Boolean.TRUE.equals(stateResult.get("success"))) {
                var state = stateResult.get("state");
                if (state != null) {
                    // Extract meaningful status from the state information
                    String stateStr = state.toString();
                    if (stateStr.contains("state=running")) {
                        return "Binding " + BINDING_NAME + ": running";
                    } else if (stateStr.contains("state=stopped")) {
                        return "Binding " + BINDING_NAME + ": stopped";
                    } else {
                        return "Binding " + BINDING_NAME + ": " + stateStr;
                    }
                }
            }
            return "Binding " + BINDING_NAME + ": unknown";
        } catch (Exception e) {
            logger.warn("Failed to get binding status: {}", e.getMessage());
            return "Binding " + BINDING_NAME + ": error - " + e.getMessage();
        }
    }
}