package com.baskettecase.embedProc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicBoolean;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Service to manage the processing state of the embedProc application.
 * Controls whether the application should process messages from the queue or remain idle.
 */
@Service
public class ProcessingStateService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessingStateService.class);
    
    private final AtomicBoolean processingEnabled = new AtomicBoolean(true);
    private volatile OffsetDateTime lastStateChange = OffsetDateTime.now(ZoneOffset.UTC);
    private volatile String lastChangeReason = "Initial state";
    
    /**
     * Check if processing is currently enabled
     * @return true if processing is enabled, false if stopped
     */
    public boolean isProcessingEnabled() {
        return processingEnabled.get();
    }
    
    /**
     * Enable processing (start processing messages)
     * @param reason Reason for enabling processing
     * @return true if state changed, false if already enabled
     */
    public boolean enableProcessing(String reason) {
        boolean previousState = processingEnabled.getAndSet(true);
        if (!previousState) {
            lastStateChange = OffsetDateTime.now(ZoneOffset.UTC);
            lastChangeReason = (reason != null && !reason.trim().isEmpty()) ? reason : "Processing enabled via API";
            logger.info("Processing ENABLED: {}", lastChangeReason);
            return true;
        }
        logger.debug("Processing already enabled, no state change");
        return false;
    }
    
    /**
     * Disable processing (stop processing messages, leave them in queue)
     * @param reason Reason for disabling processing
     * @return true if state changed, false if already disabled
     */
    public boolean disableProcessing(String reason) {
        boolean previousState = processingEnabled.getAndSet(false);
        if (previousState) {
            lastStateChange = OffsetDateTime.now(ZoneOffset.UTC);
            lastChangeReason = (reason != null && !reason.trim().isEmpty()) ? reason : "Processing disabled via API";
            logger.info("Processing DISABLED: {}", lastChangeReason);
            return true;
        }
        logger.debug("Processing already disabled, no state change");
        return false;
    }
    
    /**
     * Toggle processing state
     * @param reason Reason for toggling
     * @return ProcessingStateInfo with the new state and change details
     */
    public ProcessingStateInfo toggleProcessing(String reason) {
        boolean wasEnabled = processingEnabled.get();
        
        if (wasEnabled) {
            disableProcessing((reason != null && !reason.trim().isEmpty()) ? reason : "Processing toggled off via API");
        } else {
            enableProcessing((reason != null && !reason.trim().isEmpty()) ? reason : "Processing toggled on via API");
        }
        
        return new ProcessingStateInfo(
            !wasEnabled,  // current state (opposite of what it was)
            wasEnabled,   // previous state
            lastStateChange,
            lastChangeReason
        );
    }
    
    /**
     * Get current processing state information
     * @return ProcessingStateInfo with current state details
     */
    public ProcessingStateInfo getProcessingStateInfo() {
        return new ProcessingStateInfo(
            processingEnabled.get(),
            null, // previous state not tracked for read-only operations
            lastStateChange,
            lastChangeReason
        );
    }
    
    /**
     * Data class to hold processing state information
     */
    public static class ProcessingStateInfo {
        private final boolean enabled;
        private final Boolean previousState;
        private final OffsetDateTime lastChanged;
        private final String lastChangeReason;
        
        public ProcessingStateInfo(boolean enabled, Boolean previousState, OffsetDateTime lastChanged, String lastChangeReason) {
            this.enabled = enabled;
            this.previousState = previousState;
            this.lastChanged = lastChanged;
            this.lastChangeReason = lastChangeReason;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public Boolean getPreviousState() {
            return previousState;
        }
        
        public OffsetDateTime getLastChanged() {
            return lastChanged;
        }
        
        public String getLastChangeReason() {
            return lastChangeReason;
        }
        
        public String getStatus() {
            return enabled ? "STARTED" : "STOPPED";
        }
        
        public String getConsumerStatus() {
            return enabled ? "CONSUMING" : "IDLE";
        }
    }
}
