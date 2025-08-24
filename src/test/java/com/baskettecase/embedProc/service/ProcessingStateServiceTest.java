package com.baskettecase.embedProc.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProcessingStateService
 */
public class ProcessingStateServiceTest {

    private ProcessingStateService processingStateService;

    @BeforeEach
    public void setUp() {
        processingStateService = new ProcessingStateService(event -> {});
    }

    @Test
    public void testInitialState() {
        // Processing should be disabled by default (changed to match textProc pattern)
        assertFalse(processingStateService.isProcessingEnabled());
        
        ProcessingStateService.ProcessingStateInfo stateInfo = processingStateService.getProcessingStateInfo();
        assertFalse(stateInfo.isEnabled());
        assertEquals("STOPPED", stateInfo.getStatus());
        assertEquals("IDLE", stateInfo.getConsumerStatus());
        assertEquals("Initial state - processing disabled by default", stateInfo.getLastChangeReason());
        assertNotNull(stateInfo.getLastChanged());
    }

    @Test
    public void testDisableProcessing() {
        // First enable it
        processingStateService.enableProcessing("Test setup");
        assertTrue(processingStateService.isProcessingEnabled());
        
        // Disable processing
        boolean stateChanged = processingStateService.disableProcessing("Test disable");
        assertTrue(stateChanged);
        assertFalse(processingStateService.isProcessingEnabled());
        
        ProcessingStateService.ProcessingStateInfo stateInfo = processingStateService.getProcessingStateInfo();
        assertFalse(stateInfo.isEnabled());
        assertEquals("STOPPED", stateInfo.getStatus());
        assertEquals("IDLE", stateInfo.getConsumerStatus());
        assertEquals("Test disable", stateInfo.getLastChangeReason());
    }

    @Test
    public void testEnableProcessing() {
        // Initially disabled (no need to disable first)
        assertFalse(processingStateService.isProcessingEnabled());
        
        // Then enable
        boolean stateChanged = processingStateService.enableProcessing("Test enable");
        assertTrue(stateChanged);
        assertTrue(processingStateService.isProcessingEnabled());
        
        ProcessingStateService.ProcessingStateInfo stateInfo = processingStateService.getProcessingStateInfo();
        assertTrue(stateInfo.isEnabled());
        assertEquals("STARTED", stateInfo.getStatus());
        assertEquals("CONSUMING", stateInfo.getConsumerStatus());
        assertEquals("Test enable", stateInfo.getLastChangeReason());
    }

    @Test
    public void testToggleProcessing() {
        // Initially disabled, toggle should enable
        assertFalse(processingStateService.isProcessingEnabled());
        
        ProcessingStateService.ProcessingStateInfo toggleResult = processingStateService.toggleProcessing("Test toggle 1");
        assertTrue(toggleResult.isEnabled()); // Now enabled
        assertFalse(toggleResult.getPreviousState()); // Was disabled
        assertEquals("Test toggle 1", toggleResult.getLastChangeReason());
        
        // Toggle again should disable
        ProcessingStateService.ProcessingStateInfo toggleResult2 = processingStateService.toggleProcessing("Test toggle 2");
        assertFalse(toggleResult2.isEnabled()); // Now disabled
        assertTrue(toggleResult2.getPreviousState()); // Was enabled
        assertEquals("Test toggle 2", toggleResult2.getLastChangeReason());
    }

    @Test
    public void testNoStateChangeWhenAlreadyInDesiredState() {
        // Try to disable when already disabled
        assertFalse(processingStateService.isProcessingEnabled());
        boolean stateChanged = processingStateService.disableProcessing("Redundant disable");
        assertFalse(stateChanged); // No change
        
        // Enable first
        processingStateService.enableProcessing("Setup for test");
        
        // Try to enable when already enabled
        boolean stateChanged2 = processingStateService.enableProcessing("Redundant enable");
        assertFalse(stateChanged2); // No change
    }

    @Test
    public void testReasonHandling() {
        // First enable, then test with null reason for disable
        processingStateService.enableProcessing("Setup");
        processingStateService.disableProcessing(null);
        ProcessingStateService.ProcessingStateInfo stateInfo = processingStateService.getProcessingStateInfo();
        assertEquals("Processing disabled via API", stateInfo.getLastChangeReason());
        
        // Test with empty reason
        processingStateService.enableProcessing("");
        ProcessingStateService.ProcessingStateInfo stateInfo2 = processingStateService.getProcessingStateInfo();
        assertEquals("Processing enabled via API", stateInfo2.getLastChangeReason());
    }
}
