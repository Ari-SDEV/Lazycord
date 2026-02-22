package com.lazycord.config;

import com.lazycord.service.KeycloakTokenService;
import com.lazycord.service.KeycloakUserService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Test configuration for providing mock beans.
 */
@TestConfiguration
public class TestConfig {

    /**
     * Provides a mock WebClient.Builder for tests.
     * This avoids making actual HTTP calls during unit tests.
     */
    @Bean
    @Primary
    public WebClient.Builder webClientBuilder() {
        return Mockito.mock(WebClient.Builder.class);
    }

    /**
     * Provides a mock KeycloakTokenService for tests.
     */
    @Bean
    @Primary
    public KeycloakTokenService keycloakTokenService() {
        return Mockito.mock(KeycloakTokenService.class);
    }

    /**
     * Provides a mock KeycloakUserService for tests.
     */
    @Bean
    @Primary
    public KeycloakUserService keycloakUserService() {
        return Mockito.mock(KeycloakUserService.class);
    }
}
