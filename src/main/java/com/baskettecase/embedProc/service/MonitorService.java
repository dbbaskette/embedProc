package com.baskettecase.embedProc.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import java.time.Duration;
import java.time.Instant;

@Service
@Profile({"local", "cloud"})
public class MonitorService {

    private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);
    
    private final Counter embeddingProcessedCounter;
    private final Counter embeddingErrorCounter;
    private final MeterRegistry meterRegistry;
    private final MetricsPublisher metricsPublisher;
    private final String instanceId;
    private final Instant startTime;
    
    // Tracking metrics
    private final AtomicLong totalChunks = new AtomicLong(0);
    private final AtomicLong processedChunks = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);

    @Autowired
    public MonitorService(Counter embeddingProcessedCounter, 
                         Counter embeddingErrorCounter,
                         MeterRegistry meterRegistry,
                         MetricsPublisher metricsPublisher,
                         @Value("${spring.application.name:embedProc}") String appName,
                         @Value("${CF_INSTANCE_INDEX:${INSTANCE_ID:0}}") String instanceIndex) {
        this.embeddingProcessedCounter = embeddingProcessedCounter;
        this.embeddingErrorCounter = embeddingErrorCounter;
        this.meterRegistry = meterRegistry;
        this.metricsPublisher = metricsPublisher;
        this.instanceId = appName + "-" + instanceIndex;
        this.startTime = Instant.now();
        
        logger.info("MonitorService initialized for instance: {}", instanceId);
    }

    public void incrementTotalChunks(long count) {
        totalChunks.addAndGet(count);
        logger.debug("Total chunks updated: {} (new total: {})", count, totalChunks.get());
        publishMetrics();
    }

    public void incrementProcessedChunks(long count) {
        processedChunks.addAndGet(count);
        logger.debug("Processed chunks updated: {} (new total: {})", count, processedChunks.get());
        publishMetrics();
    }

    public void incrementErrors(long count) {
        errorCount.addAndGet(count);
        logger.debug("Errors updated: {} (new total: {})", count, errorCount.get());
        publishMetrics();
    }

    public MonitoringData getMonitoringData() {
        return new MonitoringData(
            instanceId,
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            totalChunks.get(),
            processedChunks.get(),
            errorCount.get(),
            calculateProcessingRate(),
            getUptime(),
            determineStatus()
        );
    }

    private void publishMetrics() {
        try {
            metricsPublisher.publishMetrics(getMonitoringData());
        } catch (Exception e) {
            logger.debug("Failed to publish metrics to RabbitMQ: {}", e.getMessage());
            // Don't let RabbitMQ failures affect the main processing
        }
    }

    private double calculateProcessingRate() {
        long uptime = Duration.between(startTime, Instant.now()).toSeconds();
        if (uptime == 0) return 0.0;
        return (double) processedChunks.get() / uptime;
    }

    private String getUptime() {
        Duration uptime = Duration.between(startTime, Instant.now());
        long hours = uptime.toHours();
        long minutes = uptime.toMinutesPart();
        return String.format("%dh %dm", hours, minutes);
    }

    private String determineStatus() {
        long total = totalChunks.get();
        long processed = processedChunks.get();
        long errors = errorCount.get();
        
        if (errors > processed * 0.1) { // More than 10% error rate
            return "ERROR";
        } else if (total > 0 && processed < total) {
            return "PROCESSING";
        } else {
            return "IDLE";
        }
    }

    public static class MonitoringData {
        private final String instanceId;
        private final String timestamp;
        private final long totalChunks;
        private final long processedChunks;
        private final long errorCount;
        private final double processingRate;
        private final String uptime;
        private final String status;

        public MonitoringData(String instanceId, String timestamp, long totalChunks, 
                             long processedChunks, long errorCount, double processingRate, 
                             String uptime, String status) {
            this.instanceId = instanceId;
            this.timestamp = timestamp;
            this.totalChunks = totalChunks;
            this.processedChunks = processedChunks;
            this.errorCount = errorCount;
            this.processingRate = processingRate;
            this.uptime = uptime;
            this.status = status;
        }

        // Getters
        public String getInstanceId() { return instanceId; }
        public String getTimestamp() { return timestamp; }
        public long getTotalChunks() { return totalChunks; }
        public long getProcessedChunks() { return processedChunks; }
        public long getErrorCount() { return errorCount; }
        public double getProcessingRate() { return processingRate; }
        public String getUptime() { return uptime; }
        public String getStatus() { return status; }
    }
}