package com.baskettecase.embedProc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HeartbeatScheduler {

    private final MonitorService monitorService;
    private final MetricsPublisher metricsPublisher;
    private final boolean rabbitEnabled;

    public HeartbeatScheduler(MonitorService monitorService,
                              MetricsPublisher metricsPublisher,
                              @Value("${app.monitoring.rabbitmq.enabled:false}") boolean rabbitEnabled) {
        this.monitorService = monitorService;
        this.metricsPublisher = metricsPublisher;
        this.rabbitEnabled = rabbitEnabled;
    }

    @Scheduled(fixedDelayString = "${app.monitoring.emit-interval-seconds:10}000")
    public void emitHeartbeat() {
        if (!rabbitEnabled) {
            return;
        }
        metricsPublisher.publishMetrics(monitorService.getMonitoringDataWithEvent("HEARTBEAT", null));
    }
}


