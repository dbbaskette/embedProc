package com.baskettecase.textProc.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ExtractionService provides utilities to extract text content from files using Apache Tika.
 * Supports both advanced and simple extraction methods.
 */
/**
 * ExtractionService provides utilities to extract text content from files using Apache Tika.
 * <p>
 * Supports both advanced (metadata-aware) and simple extraction methods. Used by both standalone and SCDF profiles.
 * Logs all extraction attempts and errors.
 */
@Service
public class ExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(ExtractionService.class);

    /**
     * Extracts text content from a file using Apache Tika's AutoDetectParser.
     * This method provides more control and access to metadata.
     *
     * @param filePath The path to the file.
     * @return The extracted text content, or null if an error occurs during parsing.
     * @throws IOException If an I/O error occurs reading the file.
     */
    /**
     * Extracts text content from a file using Apache Tika's AutoDetectParser for advanced parsing and metadata.
     * @param filePath The path to the file.
     * @return The extracted text content, or null if an error occurs during parsing.
     * @throws IOException If an I/O error occurs reading the file.
     */
    /**
     * Extracts text content from a file using Apache Tika's AutoDetectParser for advanced parsing and metadata.
     *
     * @param filePath The path to the file.
     * @return The extracted text content, or null if an error occurs during parsing.
     * @throws IOException If an I/O error occurs reading the file.
     */
    public String extractTextFromFile(Path filePath) throws IOException {
        logger.info("Extracting text from file: {}", filePath);

        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1); // -1 for unlimited text
        Metadata metadata = new Metadata(); // Can be used to inspect file metadata
        ParseContext context = new ParseContext();

        try (InputStream stream = Files.newInputStream(filePath)) {
            parser.parse(stream, handler, metadata, context);
            String extractedText = handler.toString();
            logger.debug("Successfully extracted {} characters from {}", extractedText.length(), filePath);
            // Example of accessing metadata:
            // String contentType = metadata.get(Metadata.CONTENT_TYPE);
            // logger.debug("Content type: {}", contentType);
            return extractedText;
        } catch (SAXException | TikaException e) {
            logger.error("Tika parsing error for file {}: {}", filePath, e.getMessage());
            return null; // Or rethrow as a custom application exception
        }
    }

    /**
     * Simpler Tika facade for text extraction.
     *
     * @param filePath The path to the file.
     * @return The extracted text content.
     * @throws IOException   If an I/O error occurs.
     * @throws TikaException If a Tika parsing error occurs.
     */
    /**
     * Extracts text using Tika's simple facade for quick text extraction.
     * @param filePath The path to the file.
     * @return The extracted text content.
     * @throws IOException   If an I/O error occurs.
     * @throws TikaException If a Tika parsing error occurs.
     */
    /**
     * Extracts text using Tika's simple facade for quick text extraction.
     *
     * @param filePath The path to the file.
     * @return The extracted text content.
     * @throws IOException   If an I/O error occurs.
     * @throws TikaException If a Tika parsing error occurs.
     */
    public String extractTextWithTikaFacade(Path filePath) throws IOException, TikaException {
        logger.info("Extracting text (facade) from file: {}", filePath);
        Tika tika = new Tika();
        return tika.parseToString(filePath);
    }
}
