package com.lazycord.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for KeycloakInitService.
 * Tests realm initialization using WebClient.
 */
@ExtendWith(MockitoExtension.class)
class KeycloakInitServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private KeycloakTokenService keycloakTokenService;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private KeycloakInitService keycloakInitService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(keycloakInitService, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(keycloakInitService, "adminRealm", "master");
        ReflectionTestUtils.setField(keycloakInitService, "adminUsername", "admin");
        ReflectionTestUtils.setField(keycloakInitService, "adminPassword", "admin");

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
    }

    @Test
    void testInitialize_NoKeycloakConnection() {
        // Arrange - No admin token available
        when(keycloakTokenService.getServiceAccountToken()).thenReturn(null);

        // Act & Assert - Should not throw, just log warning
        assertDoesNotThrow(() -> keycloakInitService.initialize());
    }

    @Test
    void testInitialize_Success() throws Exception {
        // Arrange
        String adminToken = "test-admin-token";
        when(keycloakTokenService.getServiceAccountToken()).thenReturn(adminToken);

        // Mock realm exists check (throws exception = doesn't exist)
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenThrow(new RuntimeException("Realm not found"));

        // Mock realm creation
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(null);

        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> keycloakInitService.initialize());
    }
}
