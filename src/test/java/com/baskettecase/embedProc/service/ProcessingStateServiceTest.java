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
        processingStateService = new ProcessingStateService();
    }

    @Test
    public void testInitialState() {
        // Processing should be enabled by default
        assertTrue(processingStateService.isProcessingEnabled());
        
        ProcessingStateService.ProcessingStateInfo stateInfo = processingStateService.getProcessingStateInfo();
        assertTrue(stateInfo.isEnabled());
        assertEquals("STARTED", stateInfo.getStatus());
        assertEquals("CONSUMING", stateInfo.getConsumerStatus());
        assertEquals("Initial state", stateInfo.getLastChangeReason());
        assertNotNull(stateInfo.getLastChanged());
    }

    @Test
    public void testDisableProcessing() {
        // Initially enabled
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
        // First disable
        processingStateService.disableProcessing("Test setup");
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
        // Initially enabled, toggle should disable
        assertTrue(processingStateService.isProcessingEnabled());
        
        ProcessingStateService.ProcessingStateInfo toggleResult = processingStateService.toggleProcessing("Test toggle 1");
        assertFalse(toggleResult.isEnabled()); // Now disabled
        assertTrue(toggleResult.getPreviousState()); // Was enabled
        assertEquals("Test toggle 1", toggleResult.getLastChangeReason());
        
        // Toggle again should enable
        ProcessingStateService.ProcessingStateInfo toggleResult2 = processingStateService.toggleProcessing("Test toggle 2");
        assertTrue(toggleResult2.isEnabled()); // Now enabled
        assertFalse(toggleResult2.getPreviousState()); // Was disabled
        assertEquals("Test toggle 2", toggleResult2.getLastChangeReason());
    }

    @Test
    public void testNoStateChangeWhenAlreadyInDesiredState() {
        // Try to enable when already enabled
        assertTrue(processingStateService.isProcessingEnabled());
        boolean stateChanged = processingStateService.enableProcessing("Redundant enable");
        assertFalse(stateChanged); // No change
        
        // Disable first
        processingStateService.disableProcessing("Setup for test");
        
        // Try to disable when already disabled
        boolean stateChanged2 = processingStateService.disableProcessing("Redundant disable");
        assertFalse(stateChanged2); // No change
    }

    @Test
    public void testReasonHandling() {
        // Test with null reason
        processingStateService.disableProcessing(null);
        ProcessingStateService.ProcessingStateInfo stateInfo = processingStateService.getProcessingStateInfo();
        assertEquals("Processing disabled via API", stateInfo.getLastChangeReason());
        
        // Test with empty reason
        processingStateService.enableProcessing("");
        ProcessingStateService.ProcessingStateInfo stateInfo2 = processingStateService.getProcessingStateInfo();
        assertEquals("Processing enabled via API", stateInfo2.getLastChangeReason());
    }
}
