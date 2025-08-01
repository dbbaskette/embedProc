package com.baskettecase.embedProc.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.vectorstore.VectorStore;
import io.micrometer.core.instrument.Counter;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.baskettecase.embedProc.service.EmbeddingService.TextWithMetadata;

/**
 * Unit tests for the unified EmbeddingService
 */
public class EmbeddingServiceTest {

    @Mock
    private VectorStore vectorStore;
    
    @Mock
    private Counter embeddingProcessedCounter;
    
    @Mock
    private Counter embeddingErrorCounter;
    
    @Mock
    private MonitorService monitorService;
    
    private EmbeddingService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new EmbeddingService(
            vectorStore, 
            embeddingProcessedCounter, 
            embeddingErrorCounter, 
            monitorService
        );
    }

    @Test
    void testStoreEmbeddingWithoutMetadata() {
        // Test regular embedding storage
        assertDoesNotThrow(() -> {
            service.storeEmbedding("Test content");
        });
        
        verify(vectorStore, times(1)).add(any());
        verify(embeddingProcessedCounter, times(1)).increment();
    }

    @Test
    void testStoreEmbeddingWithMetadata() {
        // Test embedding storage with metadata
        assertDoesNotThrow(() -> {
            service.storeEmbeddingWithMetadata("Test content", 123456, 789012);
        });
        
        verify(vectorStore, times(1)).add(any());
        verify(embeddingProcessedCounter, times(1)).increment();
    }

    @Test
    void testTextWithMetadata() {
        TextWithMetadata textWithMetadata = new TextWithMetadata(
            "Sample text content", 123456, 789012
        );
        
        assertEquals("Sample text content", textWithMetadata.getText());
        assertEquals(Integer.valueOf(123456), textWithMetadata.getRefnum1());
        assertEquals(Integer.valueOf(789012), textWithMetadata.getRefnum2());
        
        // Test toString method
        String toString = textWithMetadata.toString();
        assertTrue(toString.contains("123456"));
        assertTrue(toString.contains("789012"));
    }

    @Test
    void testInvalidMetadata() {
        // Test invalid metadata - service should handle gracefully without throwing
        assertDoesNotThrow(() -> {
            service.storeEmbeddingWithMetadata("Test content", 12345, 789012);
        });
        
        // Test null metadata - service should handle gracefully without throwing
        assertDoesNotThrow(() -> {
            service.storeEmbeddingWithMetadata("Test content", null, 789012);
        });
    }

    @Test
    void testEmptyTextHandling() {
        // Test empty text handling for both regular and metadata methods
        assertDoesNotThrow(() -> {
            service.storeEmbedding("");
            service.storeEmbeddingWithMetadata("", 123456, 789012);
        });
        
        // Verify that vectorStore.add was not called for empty text
        verify(vectorStore, never()).add(any());
    }

    @Test
    void testBoundaryValues() {
        // Test minimum and maximum valid metadata values
        assertDoesNotThrow(() -> {
            service.storeEmbeddingWithMetadata("Test content", 100000, 100000);
            service.storeEmbeddingWithMetadata("Test content", 999999, 999999);
        });
        
        verify(vectorStore, times(2)).add(any());
        verify(embeddingProcessedCounter, times(2)).increment();
    }
}