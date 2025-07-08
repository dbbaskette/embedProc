package com.baskettecase.embedProc.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class InstanceStartupReporter {

    private static final Logger logger = LoggerFactory.getLogger(InstanceStartupReporter.class);
    
    private final MetricsPublisher metricsPublisher;
    private final ObjectMapper objectMapper;
    private final String instanceId;
    private final String appName;
    private final String instanceIndex;

    @Autowired
    public InstanceStartupReporter(MetricsPublisher metricsPublisher,
                                   ObjectMapper objectMapper,
                                   @Value("${spring.application.name:embedProc}") String appName,
                                   @Value("${CF_INSTANCE_INDEX:${INSTANCE_ID:0}}") String instanceIndex) {
        this.metricsPublisher = metricsPublisher;
        this.objectMapper = objectMapper;
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
            startupMessage.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            startupMessage.put("status", "STARTED");
            startupMessage.put("message", "Instance started successfully");
            
            // Convert to JSON
            String jsonMessage = objectMapper.writeValueAsString(startupMessage);
            
            // Publish to metrics queue
            metricsPublisher.publishMetrics(new MonitorService.MonitoringData(
                instanceId,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                0, // totalChunks
                0, // processedChunks
                0, // errorCount
                0.0, // processingRate
                "0h 0m", // uptime
                "STARTED" // status
            ));
            
            logger.info("Instance startup reported to metrics queue: {}", instanceId);
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize startup message: {}", e.getMessage());
        } catch (Exception e) {
            logger.warn("Failed to report instance startup to metrics queue: {}", e.getMessage());
            // Don't throw exception - we don't want startup reporting failures to affect application startup
        }
    }
} 