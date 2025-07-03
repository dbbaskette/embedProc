package com.baskettecase.embedProc.service;

public interface MetricsPublisher {
    void publishMetrics(MonitorService.MonitoringData monitoringData);
}