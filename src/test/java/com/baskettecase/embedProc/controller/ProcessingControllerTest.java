package com.baskettecase.embedProc.controller;

import com.baskettecase.embedProc.service.ProcessingStateService;
import com.baskettecase.embedProc.service.MonitorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProcessingController
 */
@ExtendWith(MockitoExtension.class)
public class ProcessingControllerTest {

    @Mock
    private ProcessingStateService processingStateService;

    @Mock
    private MonitorService monitorService;

    @Mock
    private MonitorService.MonitoringData monitoringData;

    private ProcessingController processingController;

    @BeforeEach
    public void setUp() {
        processingController = new ProcessingController(processingStateService, monitorService);
    }

    @Test
    public void testGetFilesProcessed() {
        // Arrange
        long expectedProcessedFiles = 42L;
        long expectedTotalFiles = 100L;
        
        when(monitorService.getMonitoringData()).thenReturn(monitoringData);
        when(monitoringData.getFilesProcessed()).thenReturn(expectedProcessedFiles);
        when(monitoringData.getFilesTotal()).thenReturn(expectedTotalFiles);

        // Act
        ResponseEntity<Map<String, Object>> response = processingController.getFilesProcessed();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertEquals(expectedProcessedFiles, responseBody.get("filesProcessed"));
        assertEquals(expectedTotalFiles, responseBody.get("filesTotal"));
        assertNotNull(responseBody.get("timestamp"));
        
        // Verify timestamp format (should be ISO format)
        String timestamp = (String) responseBody.get("timestamp");
        assertDoesNotThrow(() -> OffsetDateTime.parse(timestamp));
        
        // Verify interactions
        verify(monitorService, times(1)).getMonitoringData();
        verify(monitoringData, times(1)).getFilesProcessed();
        verify(monitoringData, times(1)).getFilesTotal();
    }

    @Test
    public void testGetFilesProcessedWithZeroValues() {
        // Arrange
        when(monitorService.getMonitoringData()).thenReturn(monitoringData);
        when(monitoringData.getFilesProcessed()).thenReturn(0L);
        when(monitoringData.getFilesTotal()).thenReturn(0L);

        // Act
        ResponseEntity<Map<String, Object>> response = processingController.getFilesProcessed();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertEquals(0L, responseBody.get("filesProcessed"));
        assertEquals(0L, responseBody.get("filesTotal"));
        assertNotNull(responseBody.get("timestamp"));
    }

    @Test
    public void testResetCounters() {
        // Act
        ResponseEntity<Map<String, Object>> response = processingController.resetCounters();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertEquals(true, responseBody.get("success"));
        assertEquals("All processing counters have been reset to zero", responseBody.get("message"));
        assertNotNull(responseBody.get("timestamp"));
        
        // Verify timestamp format (should be ISO format)
        String timestamp = (String) responseBody.get("timestamp");
        assertDoesNotThrow(() -> OffsetDateTime.parse(timestamp));
        
        // Verify the monitor service resetCounters method was called
        verify(monitorService, times(1)).resetCounters();
    }
}