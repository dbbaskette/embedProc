package com.baskettecase.embedProc.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@ConditionalOnProperty(name = "app.monitoring.rabbitmq.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpMetricsPublisher implements MetricsPublisher {

    private static final Logger logger = LoggerFactory.getLogger(NoOpMetricsPublisher.class);

    public NoOpMetricsPublisher() {
        logger.info("NoOpMetricsPublisher initialized - RabbitMQ metrics publishing disabled");
    }

    @Override
    public void publishMetrics(MonitorService.MonitoringData monitoringData) {
        // No-op implementation
        logger.trace("Metrics publishing disabled - skipping publish for instance: {}", 
                    monitoringData.getInstanceId());
    }
}