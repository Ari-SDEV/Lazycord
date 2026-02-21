package com.lazycord.config;

import org.keycloak.admin.client.Keycloak;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration for providing mock beans.
 */
@TestConfiguration
public class TestConfig {

    /**
     * Provides a mock Keycloak admin client for tests.
     * This avoids making actual calls to Keycloak during unit tests.
     */
    @Bean
    @Primary
    public Keycloak keycloakAdminClient() {
        return Mockito.mock(Keycloak.class);
    }
}
