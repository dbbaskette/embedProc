package com.baskettecase.embedProc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class EmbedProcApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmbedProcApplication.class, args);
    }
}
