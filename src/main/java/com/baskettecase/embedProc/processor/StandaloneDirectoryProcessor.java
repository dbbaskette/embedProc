package com.baskettecase.embedProc.processor;

import com.baskettecase.textProc.config.ProcessorProperties;
import com.baskettecase.textProc.service.ExtractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Stream;

@Component
/**
 * StandaloneDirectoryProcessor processes files in a directory using Apache Tika and custom extraction logic.
 * <p>
 * Activated only under the 'standalone' Spring profile. Monitors the input directory, processes files, and moves them
 * to output, error, or processed directories based on processing results. Handles all directory creation automatically.
 * <p>
 * Error handling ensures files causing exceptions are moved to the error directory and logs are written for all steps.
 * Uses {@link ExtractionService} for text extraction.
 */
@Profile("standalone") // Only active for 'standalone' profile
public class StandaloneDirectoryProcessor implements CommandLineRunner {
    // TODO: Implement standalone directory processing logic for embedProc

    private static final Logger logger = LoggerFactory.getLogger(StandaloneDirectoryProcessor.class);

    private final ProcessorProperties.Standalone standaloneProps;
    private final ExtractionService extractionService;

    /**
     * Constructs a StandaloneDirectoryProcessor with injected configuration and extraction service.
     * @param processorProperties Configuration properties for processor directories and mode.
     * @param extractionService Service for extracting text from files.
     */
    public StandaloneDirectoryProcessor(ProcessorProperties processorProperties, ExtractionService extractionService) {
        this.standaloneProps = processorProperties.getStandalone();
        this.extractionService = extractionService;
    }

    /**
     * Entry point for the standalone processor. Logs configuration, ensures directories exist,
     * and processes all files found in the input directory.
     * @param args Command-line arguments (unused).
     */
    @Override
    public void run(String... args) throws Exception {
        logger.info("--- STANDALONE MODE ACTIVATED ---");
        logger.info("Input directory: {}", standaloneProps.getInputDirectory());
        logger.info("Output directory: {}", standaloneProps.getOutputDirectory());
        logger.info("Error directory: {}", standaloneProps.getErrorDirectory());
        logger.info("Processed directory: {}", standaloneProps.getProcessedDirectory());

        Path inputDir = Paths.get(standaloneProps.getInputDirectory());
        Path outputDir = Paths.get(standaloneProps.getOutputDirectory());
        Path errorDir = Paths.get(standaloneProps.getErrorDirectory());
        Path processedDir = Paths.get(standaloneProps.getProcessedDirectory());

        createDirectoriesIfNotExist(inputDir, outputDir, errorDir, processedDir);

        try (Stream<Path> fileStream = Files.list(inputDir)) {
            fileStream.filter(Files::isRegularFile).forEach(this::processFile);
        } catch (IOException e) {
            logger.error("Error reading input directory {}: {}", inputDir, e.getMessage(), e);
        }
        logger.info("--- STANDALONE MODE FINISHED ---");
    }

    /**
     * Processes a single file: extracts text, writes output, and moves the file to processed or error directories.
     * @param filePath Path to the file to process.
     */
    private void processFile(Path filePath) {
        logger.info("Processing file: {}", filePath.getFileName());
        Path outputDir = Paths.get(standaloneProps.getOutputDirectory());
        Path errorDir = Paths.get(standaloneProps.getErrorDirectory());
        Path processedDir = Paths.get(standaloneProps.getProcessedDirectory());

        try {
            String extractedText = extractionService.extractTextFromFile(filePath); // Or use extractTextWithTikaFacade

            if (extractedText != null && !extractedText.isBlank()) {
                Path outputFile = outputDir.resolve(filePath.getFileName().toString() + ".txt");
                Files.writeString(outputFile, extractedText, StandardCharsets.UTF_8);
                logger.info("Successfully extracted text to: {}", outputFile);
                moveToDirectory(filePath, processedDir.resolve(filePath.getFileName()), "processed");
            } else if (extractedText == null) { // Indicates a parsing error handled by TikaExtractionService
                logger.warn("Failed to extract text from (parser error): {}. Moving to error directory.", filePath.getFileName());
                moveToDirectory(filePath, errorDir.resolve(filePath.getFileName()), "error");
            } else { // Extracted text is blank
                logger.warn("Extracted text is blank for: {}. Moving to error directory as per policy (or processed if preferred).", filePath.getFileName());
                moveToDirectory(filePath, errorDir.resolve(filePath.getFileName()), "error"); // Or processedDir
            }
        } catch (IOException e) {
            logger.error("IO error processing file {}: {}", filePath.getFileName(), e.getMessage(), e);
            moveToDirectory(filePath, errorDir.resolve(filePath.getFileName()), "error");
        } catch (Exception e) { // Catch any other unexpected errors during processing
            logger.error("Unexpected error processing file {}: {}", filePath.getFileName(), e.getMessage(), e);
            moveToDirectory(filePath, errorDir.resolve(filePath.getFileName()), "error");
        }
    }

    /**
     * Moves a file to a target directory and logs the action.
     * @param sourceFile The file to move.
     * @param targetFile The destination path.
     * @param type The type of move ("processed" or "error").
     */
    private void moveToDirectory(Path sourceFile, Path targetFile, String type) {
        try {
            Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Moved {} to {} directory: {}", sourceFile.getFileName(), type, targetFile);
        } catch (IOException e) {
            logger.error("Failed to move {} to {} directory ({}): {}",
                    sourceFile.getFileName(), type, targetFile, e.getMessage());
        }
    }

    private void createDirectoriesIfNotExist(Path... paths) throws IOException {
        for (Path path : paths) {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                logger.info("Created directory: {}", path.toAbsolutePath());
            }
        }
    }
}