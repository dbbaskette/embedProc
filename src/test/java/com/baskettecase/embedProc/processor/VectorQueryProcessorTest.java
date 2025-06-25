package com.baskettecase.embedProc.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VectorQueryProcessorTest {

    @Mock
    private VectorStore vectorStore;

    private VectorQueryProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new VectorQueryProcessor(vectorStore);
    }

    @Test
    void runQuery_WithValidQuery_ShouldExecuteSearch() {
        // Arrange
        String queryText = "test query";
        int topK = 5;
        List<Document> mockResults = Arrays.asList(
                new Document("Test content 1"),
                new Document("Test content 2")
        );
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(mockResults);

        // Act
        processor.runQuery(queryText, topK);

        // Assert
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void runQuery_WithEmptyQuery_ShouldSkipSearch() {
        // Act
        processor.runQuery("", 5);
        processor.runQuery(null, 5);
        processor.runQuery("   ", 5);

        // Assert
        verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void runQuery_WithNoResults_ShouldHandleGracefully() {
        // Arrange
        String queryText = "test query";
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());

        // Act
        processor.runQuery(queryText, 5);

        // Assert
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void runQuery_WithException_ShouldHandleError() {
        // Arrange
        String queryText = "test query";
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenThrow(new RuntimeException("Database error"));

        // Act
        processor.runQuery(queryText, 5);

        // Assert
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }
} 