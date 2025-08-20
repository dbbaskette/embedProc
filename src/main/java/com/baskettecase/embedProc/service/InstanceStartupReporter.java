package com.baskettecase.embedProc.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class InstanceStartupReporter {

    private static final Logger logger = LoggerFactory.getLogger(InstanceStartupReporter.class);
    
    private final MetricsPublisher metricsPublisher;
    private final ObjectMapper objectMapper;
    private final MonitorService monitorService;
    private final String instanceId;
    private final String appName;
    private final String instanceIndex;

    @Autowired
    public InstanceStartupReporter(MetricsPublisher metricsPublisher,
                                   ObjectMapper objectMapper,
                                   MonitorService monitorService,
                                   @Value("${spring.application.name:embedProc}") String appName,
                                   @Value("${CF_INSTANCE_INDEX:${INSTANCE_ID:0}}") String instanceIndex) {
        this.metricsPublisher = metricsPublisher;
        this.objectMapper = objectMapper;
        this.monitorService = monitorService;
        this.appName = appName;
        this.instanceIndex = instanceIndex;
        this.instanceId = appName + "-" + instanceIndex;
        
        logger.info("InstanceStartupReporter initialized for instance: {}", instanceId);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            // Create startup message
            Map<String, Object> startupMessage = new HashMap<>();
            startupMessage.put("eventType", "INSTANCE_STARTUP");
            startupMessage.put("instanceId", instanceId);
            startupMessage.put("appName", appName);
            startupMessage.put("instanceIndex", instanceIndex);
            startupMessage.put("timestamp", OffsetDateTime.now().toString());
            startupMessage.put("status", "STARTED");
            startupMessage.put("message", "Instance started successfully");
            
            // Publish startup INIT event with status STARTING and processingStage starting
            metricsPublisher.publishMetrics(monitorService.getMonitoringDataWithEvent("INIT", null, "STARTING", "starting"));
            
            logger.info("Instance startup reported to metrics queue: {}", instanceId);
            
        } catch (Exception e) {
            logger.warn("Failed to report instance startup to metrics queue: {}", e.getMessage());
            // Don't throw exception - we don't want startup reporting failures to affect application startup
        }
    }

    private long getMemoryUsedMB() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        return (totalMemory - freeMemory) / (1024 * 1024);
    }
} 