package com.lazycord;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Lazycord Backend application.
 * Lazycord is a Discord-like real-time messaging platform built with Spring Boot.
 */
@SpringBootApplication
public class LazycordApplication {

    public static void main(final String[] args) {
        SpringApplication.run(LazycordApplication.class, args);
    }
}
