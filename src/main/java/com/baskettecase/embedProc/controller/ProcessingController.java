package com.baskettecase.embedProc.controller;

import com.baskettecase.embedProc.service.ProcessingStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * REST Controller for managing processing state
 * Only active in cloud profile where web endpoints are needed
 */
@RestController
@RequestMapping("/api/processing")
@Profile("cloud")
public class ProcessingController {

    private static final Logger logger = LoggerFactory.getLogger(ProcessingController.class);
    
    private final ProcessingStateService processingStateService;

    public ProcessingController(ProcessingStateService processingStateService) {
        this.processingStateService = processingStateService;
        logger.info("ProcessingController initialized for cloud profile");
    }

    /**
     * GET /api/processing/state - Get current processing state
     */
    @GetMapping("/state")
    public ResponseEntity<Map<String, Object>> getProcessingState() {
        logger.debug("GET /api/processing/state requested");
        
        ProcessingStateService.ProcessingStateInfo stateInfo = processingStateService.getProcessingStateInfo();
        
        Map<String, Object> response = Map.of(
            "enabled", stateInfo.isEnabled(),
            "status", stateInfo.getStatus(),
            "consumerStatus", stateInfo.getConsumerStatus(),
            "lastChanged", stateInfo.getLastChanged().toString(),
            "lastChangeReason", stateInfo.getLastChangeReason(),
            "timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/processing/start - Start file processing
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startProcessing() {
        logger.info("POST /api/processing/start requested");
        
        boolean stateChanged = processingStateService.enableProcessing("Started via API endpoint");
        ProcessingStateService.ProcessingStateInfo stateInfo = processingStateService.getProcessingStateInfo();
        
        Map<String, Object> response = Map.of(
            "success", true,
            "message", stateChanged ? "Processing started successfully" : "Processing was already enabled",
            "stateChanged", stateChanged,
            "enabled", stateInfo.isEnabled(),
            "status", stateInfo.getStatus(),
            "consumerStatus", stateInfo.getConsumerStatus(),
            "lastChanged", stateInfo.getLastChanged().toString(),
            "timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/processing/stop - Stop file processing
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopProcessing() {
        logger.info("POST /api/processing/stop requested");
        
        boolean stateChanged = processingStateService.disableProcessing("Stopped via API endpoint");
        ProcessingStateService.ProcessingStateInfo stateInfo = processingStateService.getProcessingStateInfo();
        
        Map<String, Object> response = Map.of(
            "success", true,
            "message", stateChanged ? "Processing stopped successfully. Messages will remain in queue." : "Processing was already disabled",
            "stateChanged", stateChanged,
            "enabled", stateInfo.isEnabled(),
            "status", stateInfo.getStatus(),
            "consumerStatus", stateInfo.getConsumerStatus(),
            "lastChanged", stateInfo.getLastChanged().toString(),
            "timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/processing/toggle - Toggle processing on/off
     */
    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggleProcessing() {
        logger.info("POST /api/processing/toggle requested");
        
        // Check current state before toggling
        boolean previousState = processingStateService.isProcessingEnabled();
        
        // Toggle the state
        ProcessingStateService.ProcessingStateInfo stateInfo = processingStateService.toggleProcessing("Toggled via API endpoint");
        
        String action = stateInfo.isEnabled() ? "started" : "stopped";
        String previousStateText = previousState ? "enabled" : "disabled";
        String currentStateText = stateInfo.isEnabled() ? "enabled" : "disabled";
        
        String message = String.format(
            "Processing %s successfully. Previous state: %s, Current state: %s. %s",
            action,
            previousStateText,
            currentStateText,
            stateInfo.isEnabled() ? "Now consuming messages from queue." : "Messages will remain in queue."
        );
        
        Map<String, Object> response = Map.of(
            "success", true,
            "message", message,
            "action", action,
            "previousState", Map.of(
                "enabled", previousState,
                "status", previousState ? "ENABLED" : "DISABLED"
            ),
            "currentState", Map.of(
                "enabled", stateInfo.isEnabled(),
                "status", stateInfo.getStatus(),
                "consumerStatus", stateInfo.getConsumerStatus()
            ),
            "lastChanged", stateInfo.getLastChanged().toString(),
            "timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString()
        );
        
        return ResponseEntity.ok(response);
    }
}
