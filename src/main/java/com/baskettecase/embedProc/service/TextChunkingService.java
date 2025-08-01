package com.baskettecase.embedProc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TextChunkingService {

    private final int maxWordsPerChunk;
    private final int minMeaningfulWords;

    public TextChunkingService(
            @Value("${app.chunking.max-words-per-chunk:1000}") int maxWordsPerChunk,
            @Value("${app.chunking.min-meaningful-words:100}") int minMeaningfulWords) {
        this.maxWordsPerChunk = maxWordsPerChunk;
        this.minMeaningfulWords = minMeaningfulWords;
    }

    /**
     * Chunks text using semantic boundaries (paragraphs) for better context.
     * @param text The input text to chunk.
     * @return A list of text chunks.
     */
    public List<String> chunkTextEnhanced(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        String[] paragraphs = text.split("\\n\\s*\\n");
        StringBuilder currentChunkBuilder = new StringBuilder();
        int currentWordCount = 0;

        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) continue;

            int paragraphWordCount = countMeaningfulWords(paragraph);

            if (currentWordCount > 0 && currentWordCount + paragraphWordCount > maxWordsPerChunk) {
                String chunk = currentChunkBuilder.toString().trim();
                if (countMeaningfulWords(chunk) >= minMeaningfulWords) {
                    chunks.add(chunk);
                }
                currentChunkBuilder.setLength(0);
                currentWordCount = 0;
            }

            if (currentChunkBuilder.length() > 0) {
                currentChunkBuilder.append("\n\n");
            }
            currentChunkBuilder.append(paragraph.trim());
            currentWordCount += paragraphWordCount;
        }

        if (currentChunkBuilder.length() > 0) {
            String chunk = currentChunkBuilder.toString().trim();
            if (countMeaningfulWords(chunk) >= minMeaningfulWords) {
                chunks.add(chunk);
            }
        }

        return chunks;
    }

    /**
     * Counts meaningful words, ignoring excessive whitespace and empty lines.
     */
    private int countMeaningfulWords(String text) {
        if (text == null || text.trim().isEmpty()) return 0;

        String[] words = text.trim().split("\\s+");
        int meaningfulWordCount = 0;
        for (String word : words) {
            if (word != null && !word.trim().isEmpty() && word.matches(".*[a-zA-Z0-9].*")) {
                meaningfulWordCount++;
            }
        }
        return meaningfulWordCount;
    }
}
