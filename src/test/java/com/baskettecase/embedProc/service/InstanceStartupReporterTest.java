package com.baskettecase.embedProc.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstanceStartupReporterTest {

    @Mock
    private MetricsPublisher metricsPublisher;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Test
    void testOnApplicationReady() {
        // Create the reporter with mocked dependencies
        InstanceStartupReporter reporter = new InstanceStartupReporter(
            metricsPublisher, 
            objectMapper, 
            "testApp", 
            "1"
        );

        // Call the method under test
        reporter.onApplicationReady();

        // Verify that publishMetrics was called
        verify(metricsPublisher, times(1)).publishMetrics(any(MonitorService.MonitoringData.class));
    }

    @Test
    void testOnApplicationReadyWithException() {
        // Create the reporter with mocked dependencies
        InstanceStartupReporter reporter = new InstanceStartupReporter(
            metricsPublisher, 
            objectMapper, 
            "testApp", 
            "1"
        );

        // Make the metricsPublisher throw an exception
        doThrow(new RuntimeException("Test exception"))
            .when(metricsPublisher).publishMetrics(any(MonitorService.MonitoringData.class));

        // Call the method under test - should not throw exception
        reporter.onApplicationReady();

        // Verify that publishMetrics was called (even though it failed)
        verify(metricsPublisher, times(1)).publishMetrics(any(MonitorService.MonitoringData.class));
    }
} 