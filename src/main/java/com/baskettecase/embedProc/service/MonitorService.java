package com.baskettecase.embedProc.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.Counter;
import java.util.HashMap;
import java.util.Map;

@Service
@Profile({"local", "cloud", "scdf"})
public class MonitorService {

    @Autowired
    private Counter embeddingProcessedCounter;
    
    @Autowired
    private Counter embeddingErrorCounter;
    
    @Autowired
    private Counter chunksReceivedCounter;

    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        double received = chunksReceivedCounter.count();
        double processed = embeddingProcessedCounter.count();
        double errors = embeddingErrorCounter.count();
        
        metrics.put("chunksReceived", (long) received);
        metrics.put("chunksProcessed", (long) processed);
        metrics.put("chunksErrored", (long) errors);
        metrics.put("successRate", received > 0 ? (processed / received * 100.0) : 0.0);
        
        return metrics;
    }
}