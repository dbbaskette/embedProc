package com.baskettecase.embedProc;

import com.baskettecase.embedProc.config.ProcessorProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Main entry point for the embedProc Spring Boot application.
 * Skeleton for future embedding processor logic.
 */
@SpringBootApplication
@EnableConfigurationProperties(ProcessorProperties.class) // Explicitly enable your custom properties
public class EmbedProcApplication {

    /**
     * Main method to launch the Spring Boot application.
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        SpringApplication.run(EmbedProcApplication.class, args);
    }

}
