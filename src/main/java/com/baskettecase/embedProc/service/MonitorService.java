package com.baskettecase.embedProc.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import java.time.Duration;
import java.time.Instant;

@Service
@Profile({"local", "cloud", "standalone"})
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
    private final AtomicLong filesProcessed = new AtomicLong(0);
    private final AtomicLong filesTotal = new AtomicLong(0);
    private volatile String currentFile = null;
    private volatile String lastError = null;

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

    public void setCurrentFile(String filename) {
        this.currentFile = filename;
        publishMetrics();
    }

    public void incrementFilesProcessed() {
        filesProcessed.incrementAndGet();
        logger.debug("Files processed updated: {}", filesProcessed.get());
        publishMetrics();
    }

    public void setFilesTotal(long total) {
        filesTotal.set(total);
        logger.debug("Files total set to: {}", total);
        publishMetrics();
    }

    public void setLastError(String error) {
        this.lastError = error;
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
            determineStatus(),
            currentFile,
            filesProcessed.get(),
            filesTotal.get(),
            lastError,
            getMemoryUsedMB(),
            getPendingMessages(),
            new MonitoringData.Meta("embedProc", null, null, null, null)
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

    private long getMemoryUsedMB() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        return (totalMemory - freeMemory) / (1024 * 1024);
    }

    private long getPendingMessages() {
        // This would require RabbitMQ admin API access
        // For now, return estimate based on processing status
        long total = totalChunks.get();
        long processed = processedChunks.get();
        return Math.max(0, total - processed);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MonitoringData {
        private final String instanceId;
        private final String timestamp;
        private final long totalChunks;
        private final long processedChunks;
        private final long errorCount;
        private final double processingRate;
        private final String uptime;
        private final String status;
        // Priority 1 fields
        private final String currentFile;
        private final long filesProcessed;
        private final long filesTotal;
        private final String lastError;
        // Priority 2 fields
        private final long memoryUsedMB;
        private final long pendingMessages;
        private final Meta meta;

        public MonitoringData(String instanceId, String timestamp, long totalChunks, 
                             long processedChunks, long errorCount, double processingRate, 
                             String uptime, String status, String currentFile, 
                             long filesProcessed, long filesTotal, String lastError,
                             long memoryUsedMB, long pendingMessages, Meta meta) {
            this.instanceId = instanceId;
            this.timestamp = timestamp;
            this.totalChunks = totalChunks;
            this.processedChunks = processedChunks;
            this.errorCount = errorCount;
            this.processingRate = processingRate;
            this.uptime = uptime;
            this.status = status;
            this.currentFile = currentFile;
            this.filesProcessed = filesProcessed;
            this.filesTotal = filesTotal;
            this.lastError = lastError;
            this.memoryUsedMB = memoryUsedMB;
            this.pendingMessages = pendingMessages;
            this.meta = meta;
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
        public String getCurrentFile() { return currentFile; }
        public long getFilesProcessed() { return filesProcessed; }
        public long getFilesTotal() { return filesTotal; }
        public String getLastError() { return lastError; }
        public long getMemoryUsedMB() { return memoryUsedMB; }
        public long getPendingMessages() { return pendingMessages; }
        public Meta getMeta() { return meta; }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Meta {
            private final String service;
            private final String processingState;
            private final String bindingState;
            private final Boolean hdfsProcessedDirExists;
            private final String inputMode;

            public Meta(String service,
                        String processingState,
                        String bindingState,
                        Boolean hdfsProcessedDirExists,
                        String inputMode) {
                this.service = service;
                this.processingState = processingState;
                this.bindingState = bindingState;
                this.hdfsProcessedDirExists = hdfsProcessedDirExists;
                this.inputMode = inputMode;
            }

            public String getService() { return service; }
            public String getProcessingState() { return processingState; }
            public String getBindingState() { return bindingState; }
            public Boolean getHdfsProcessedDirExists() { return hdfsProcessedDirExists; }
            public String getInputMode() { return inputMode; }
        }
    }
}