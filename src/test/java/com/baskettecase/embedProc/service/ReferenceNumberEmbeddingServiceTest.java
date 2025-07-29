package com.baskettecase.embedProc.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.vectorstore.VectorStore;
import io.micrometer.core.instrument.Counter;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.baskettecase.embedProc.service.ReferenceNumberEmbeddingService.TextWithReferenceNumbers;

/**
 * Unit tests for ReferenceNumberEmbeddingService
 */
public class ReferenceNumberEmbeddingServiceTest {

    @Mock
    private VectorStore vectorStore;
    
    @Mock
    private Counter embeddingProcessedCounter;
    
    @Mock
    private Counter embeddingErrorCounter;
    
    @Mock
    private MonitorService monitorService;
    
    private ReferenceNumberEmbeddingService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ReferenceNumberEmbeddingService(
            vectorStore, 
            embeddingProcessedCounter, 
            embeddingErrorCounter, 
            monitorService
        );
    }

    @Test
    void testValidReferenceNumbers() {
        // Test valid 6-digit reference numbers
        assertDoesNotThrow(() -> {
            service.storeEmbeddingWithReferenceNumbers("Test content", 123456, 789012);
        });
        
        verify(vectorStore, times(1)).add(any());
        verify(embeddingProcessedCounter, times(1)).increment();
    }

    @Test
    void testInvalidReferenceNumbers() {
        // Test invalid reference numbers (too small) - service should handle gracefully without throwing
        // The service logs errors but doesn't throw exceptions for invalid reference numbers
        assertDoesNotThrow(() -> {
            service.storeEmbeddingWithReferenceNumbers("Test content", 12345, 789012);
        });
        
        // Test invalid reference numbers (too large) - service should handle gracefully without throwing
        assertDoesNotThrow(() -> {
            service.storeEmbeddingWithReferenceNumbers("Test content", 123456, 1000000);
        });
        
        // Test null reference numbers - service should handle gracefully without throwing
        assertDoesNotThrow(() -> {
            service.storeEmbeddingWithReferenceNumbers("Test content", null, 789012);
        });
    }

    @Test
    void testTextWithReferenceNumbers() {
        TextWithReferenceNumbers textWithRef = new TextWithReferenceNumbers(
            "Sample text content", 123456, 789012
        );
        
        assertEquals("Sample text content", textWithRef.getText());
        assertEquals(Integer.valueOf(123456), textWithRef.getRefnum1());
        assertEquals(Integer.valueOf(789012), textWithRef.getRefnum2());
        
        // Test toString method
        String toString = textWithRef.toString();
        assertTrue(toString.contains("123456"));
        assertTrue(toString.contains("789012"));
    }

    @Test
    void testEmptyTextHandling() {
        // Test empty text handling
        assertDoesNotThrow(() -> {
            service.storeEmbeddingWithReferenceNumbers("", 123456, 789012);
        });
        
        assertDoesNotThrow(() -> {
            service.storeEmbeddingWithReferenceNumbers(null, 123456, 789012);
        });
        
        // Verify that vectorStore.add was not called for empty/null text
        verify(vectorStore, never()).add(any());
    }

    @Test
    void testBoundaryValues() {
        // Test minimum valid values
        assertDoesNotThrow(() -> {
            service.storeEmbeddingWithReferenceNumbers("Test content", 100000, 100000);
        });
        
        // Test maximum valid values
        assertDoesNotThrow(() -> {
            service.storeEmbeddingWithReferenceNumbers("Test content", 999999, 999999);
        });
        
        verify(vectorStore, times(2)).add(any());
        verify(embeddingProcessedCounter, times(2)).increment();
    }
}
