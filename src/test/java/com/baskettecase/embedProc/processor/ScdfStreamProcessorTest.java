package com.baskettecase.embedProc.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.baskettecase.embedProc.service.EmbeddingService;
import com.baskettecase.embedProc.processor.VectorQueryProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScdfStreamProcessorTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private VectorQueryProcessor vectorQueryProcessor;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RestTemplate restTemplate;

    private ScdfStreamProcessor processor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new ScdfStreamProcessor(
            embeddingService, 
            vectorQueryProcessor, 
            "test query", 
            300,  // maxWordsPerChunk
            30,   // overlapWords
            objectMapper, 
            restTemplate
        );
    }

    @Test
    void testChunkTextEnhanced_SmallParagraph() {
        // Test with a small paragraph that should remain intact
        String text = "This is a small paragraph that should not be split.";
        
        List<String> chunks = ReflectionTestUtils.invokeMethod(processor, "chunkTextEnhanced", text, 300, 30);
        
        assertNotNull(chunks);
        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0));
    }

    @Test
    void testChunkTextEnhanced_MultipleParagraphs() {
        // Test with multiple paragraphs
        String text = "First paragraph.\n\nSecond paragraph.\n\nThird paragraph.";
        
        List<String> chunks = ReflectionTestUtils.invokeMethod(processor, "chunkTextEnhanced", text, 300, 30);
        
        assertNotNull(chunks);
        assertEquals(3, chunks.size());
        assertEquals("First paragraph.", chunks.get(0).trim());
        assertEquals("Second paragraph.", chunks.get(1).trim());
        assertEquals("Third paragraph.", chunks.get(2).trim());
    }

    @Test
    void testChunkTextEnhanced_LargeParagraph() {
        // Test with a large paragraph that needs splitting
        StringBuilder largeParagraph = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            largeParagraph.append("word").append(i).append(" ");
        }
        String text = largeParagraph.toString();
        
        List<String> chunks = ReflectionTestUtils.invokeMethod(processor, "chunkTextEnhanced", text, 300, 30);
        
        assertNotNull(chunks);
        assertTrue(chunks.size() > 1, "Large paragraph should be split into multiple chunks");
        
        // Verify each chunk is within size limits
        for (String chunk : chunks) {
            String[] words = chunk.split("\\s+");
            assertTrue(words.length <= 300, "Chunk should not exceed max word count");
            assertTrue(words.length > 0, "Chunk should not be empty");
        }
    }

    @Test
    void testChunkTextEnhanced_EmptyText() {
        // Test with empty text
        List<String> chunks = ReflectionTestUtils.invokeMethod(processor, "chunkTextEnhanced", "", 300, 30);
        
        assertNotNull(chunks);
        assertEquals(0, chunks.size());
    }

    @Test
    void testChunkTextEnhanced_NullText() {
        // Test with null text
        List<String> chunks = ReflectionTestUtils.invokeMethod(processor, "chunkTextEnhanced", (String) null, 300, 30);
        
        assertNotNull(chunks);
        assertEquals(0, chunks.size());
    }

    @Test
    void testChunkParagraph_SmallParagraph() {
        // Test chunking a small paragraph
        String paragraph = "This is a small paragraph.";
        
        List<String> chunks = ReflectionTestUtils.invokeMethod(processor, "chunkParagraph", paragraph, 300, 30);
        
        assertNotNull(chunks);
        assertEquals(1, chunks.size());
        assertEquals(paragraph.trim(), chunks.get(0));
    }

    @Test
    void testChunkParagraph_LargeParagraph() {
        // Test chunking a large paragraph
        StringBuilder largeParagraph = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            largeParagraph.append("word").append(i).append(" ");
        }
        String paragraph = largeParagraph.toString();
        
        List<String> chunks = ReflectionTestUtils.invokeMethod(processor, "chunkParagraph", paragraph, 300, 30);
        
        assertNotNull(chunks);
        assertTrue(chunks.size() > 1, "Large paragraph should be split");
        
        // Verify overlap between chunks
        for (int i = 1; i < chunks.size(); i++) {
            String prevChunk = chunks.get(i - 1);
            String currChunk = chunks.get(i);
            
            // Check that there's some overlap (simplified check)
            String[] prevWords = prevChunk.split("\\s+");
            String[] currWords = currChunk.split("\\s+");
            
            // The last few words of previous chunk should appear in current chunk
            int overlapSize = Math.min(30, prevWords.length);
            if (overlapSize > 0) {
                String lastWordOfPrev = prevWords[prevWords.length - 1];
                assertTrue(currChunk.contains(lastWordOfPrev), "Chunks should have overlap");
            }
        }
    }
} 