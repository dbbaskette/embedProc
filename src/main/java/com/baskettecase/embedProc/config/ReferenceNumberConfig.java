package com.baskettecase.embedProc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration class for reference number functionality.
 * This class provides configuration properties and setup for the enhanced
 * pgvector table with refnum1 and refnum2 support.
 */
@Configuration
@Profile({"standalone", "cloud", "local"})
@EnableConfigurationProperties(ReferenceNumberConfig.ReferenceNumberProperties.class)
public class ReferenceNumberConfig {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceNumberConfig.class);

    public ReferenceNumberConfig() {
        logger.info("ReferenceNumberConfig initialized - Reference number support enabled");
    }

    /**
     * Configuration properties for reference number functionality
     */
    @ConfigurationProperties(prefix = "app.reference-numbers")
    public static class ReferenceNumberProperties {
        
        /**
         * Enable reference number validation
         */
        private boolean enableValidation = true;
        
        /**
         * Minimum value for reference numbers (6-digit constraint)
         */
        private int minValue = 100000;
        
        /**
         * Maximum value for reference numbers (6-digit constraint)
         */
        private int maxValue = 999999;
        
        /**
         * Enable reference number indexing for better query performance
         */
        private boolean enableIndexing = true;
        
        /**
         * Default batch size for processing embeddings with reference numbers
         */
        private int batchSize = 10;
        
        /**
         * Enable parallel processing for reference number embeddings
         */
        private boolean enableParallelProcessing = true;

        // Getters and setters
        public boolean isEnableValidation() {
            return enableValidation;
        }

        public void setEnableValidation(boolean enableValidation) {
            this.enableValidation = enableValidation;
        }

        public int getMinValue() {
            return minValue;
        }

        public void setMinValue(int minValue) {
            this.minValue = minValue;
        }

        public int getMaxValue() {
            return maxValue;
        }

        public void setMaxValue(int maxValue) {
            this.maxValue = maxValue;
        }

        public boolean isEnableIndexing() {
            return enableIndexing;
        }

        public void setEnableIndexing(boolean enableIndexing) {
            this.enableIndexing = enableIndexing;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public boolean isEnableParallelProcessing() {
            return enableParallelProcessing;
        }

        public void setEnableParallelProcessing(boolean enableParallelProcessing) {
            this.enableParallelProcessing = enableParallelProcessing;
        }

        @Override
        public String toString() {
            return "ReferenceNumberProperties{" +
                    "enableValidation=" + enableValidation +
                    ", minValue=" + minValue +
                    ", maxValue=" + maxValue +
                    ", enableIndexing=" + enableIndexing +
                    ", batchSize=" + batchSize +
                    ", enableParallelProcessing=" + enableParallelProcessing +
                    '}';
        }
    }
}
