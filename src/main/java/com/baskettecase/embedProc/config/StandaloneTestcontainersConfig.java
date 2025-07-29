package com.baskettecase.embedProc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import jakarta.annotation.PreDestroy;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration for standalone mode that uses Testcontainers to provide
 * a PostgreSQL database with pgvector extension.
 * This eliminates the need for manual database setup.
 */
@Configuration
@Profile("standalone")
public class StandaloneTestcontainersConfig {

    private static final Logger logger = LoggerFactory.getLogger(StandaloneTestcontainersConfig.class);
    
    private PostgreSQLContainer<?> postgres;

    @Bean
    public DataSource dataSource() {
        logger.info("Starting PostgreSQL container with pgvector extension for standalone mode...");
        
        // Start PostgreSQL container with pgvector extension
        postgres = new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16"))
                .withDatabaseName("embedproc_standalone")
                .withUsername("embedproc")
                .withPassword("embedproc")
                .withInitScript("standalone-init-pgvector.sql");
        
        postgres.start();
        
        logger.info("PostgreSQL container started successfully!");
        logger.info("Database URL: {}", postgres.getJdbcUrl());
        logger.info("Database Username: {}", postgres.getUsername());
        
        // Configure Spring Boot DataSource to use the container
        return DataSourceBuilder.create()
                .url(postgres.getJdbcUrl())
                .username(postgres.getUsername())
                .password(postgres.getPassword())
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    @PreDestroy
    public void cleanup() {
        if (postgres != null && postgres.isRunning()) {
            logger.info("Stopping PostgreSQL container...");
            postgres.stop();
        }
    }
}
