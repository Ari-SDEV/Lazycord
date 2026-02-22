package com.lazycord.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for KeycloakUserService using WebClient.
 */
@ExtendWith(MockitoExtension.class)
class KeycloakUserServiceTest {

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
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private KeycloakUserService keycloakUserService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TEST_REALM = "lazycord";
    private static final String TEST_BASE_URL = "http://localhost:8080";
    private static final String TEST_ADMIN_TOKEN = "test-admin-token";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(keycloakUserService, "baseUrl", TEST_BASE_URL);
        ReflectionTestUtils.setField(keycloakUserService, "realm", TEST_REALM);
        ReflectionTestUtils.setField(keycloakUserService, "clientId", "lazycord-backend");

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
    }

    @Test
    void createUser_Success() {
        // Arrange
        when(keycloakTokenService.getServiceAccountToken()).thenReturn(TEST_ADMIN_TOKEN);

        String username = "testuser";
        String email = "test@example.com";
        String password = "password123";
        String role = "user";
        String firstName = "Test";
        String lastName = "User";
        String userId = "test-user-id-123";

        // Mock the POST request to create user
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.empty());

        // Mock the GET request to find created user
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        ObjectNode user = objectMapper.createObjectNode();
        user.put("id", userId);
        user.put("username", username);
        
        ArrayNode users = objectMapper.createArrayNode();
        users.add(user);
        
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(users));

        // Mock password reset (PUT request)
        when(webClient.put()).thenReturn(requestBodyUriSpec);

        // Mock role assignment (GET then POST)
        when(requestBodyUriSpec.uri(contains("/roles/"))).thenReturn(requestBodySpec);
        when(requestHeadersUriSpec.uri(contains("/roles/"))).thenReturn(requestHeadersSpec);
        
        ObjectNode role = objectMapper.createObjectNode();
        role.put("id", "role-id-123");
        role.put("name", role);
        
        when(responseSpec.bodyToMono(JsonNode.class))
            .thenReturn(Mono.just(users))  // First call for user search
            .thenReturn(Mono.just(role));    // Second call for role

        // Act
        String result = keycloakUserService.createUser(username, email, password, role, firstName, lastName);

        // Assert
        assertEquals(userId, result);
    }

    @Test
    void createUser_NoServiceAccountToken() {
        // Arrange
        when(keycloakTokenService.getServiceAccountToken()).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            keycloakUserService.createUser("test", "test@test.com", "password", "user", "Test", "User");
        });
        assertTrue(exception.getMessage().contains("Service account not configured"));
    }

    @Test
    void getUserById_Success() {
        // Arrange
        when(keycloakTokenService.getServiceAccountToken()).thenReturn(TEST_ADMIN_TOKEN);

        String userId = "test-user-id";
        String username = "testuser";
        
        // Mock GET request
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        ObjectNode user = objectMapper.createObjectNode();
        user.put("id", userId);
        user.put("username", username);
        
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(user));

        // Act
        JsonNode result = keycloakUserService.getUserById(userId);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.get("id").asText());
        assertEquals(username, result.get("username").asText());
    }

    @Test
    void findUserIdByUsername_Success() {
        // Arrange
        when(keycloakTokenService.getServiceAccountToken()).thenReturn(TEST_ADMIN_TOKEN);

        String username = "testuser";
        String userId = "test-user-id";
        
        // Mock GET request
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        ObjectNode user = objectMapper.createObjectNode();
        user.put("id", userId);
        user.put("username", username);
        
        ArrayNode users = objectMapper.createArrayNode();
        users.add(user);
        
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(users));

        // Act
        String result = keycloakUserService.findUserIdByUsername(username);

        // Assert
        assertEquals(userId, result);
    }

    @Test
    void findUserIdByUsername_NotFound() {
        // Arrange
        when(keycloakTokenService.getServiceAccountToken()).thenReturn(TEST_ADMIN_TOKEN);

        // Mock GET request - empty array
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        ArrayNode emptyUsers = objectMapper.createArrayNode();
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(emptyUsers));

        // Act
        String result = keycloakUserService.findUserIdByUsername("nonexistent");

        // Assert
        assertNull(result);
    }

    @Test
    void getUserRealmRoles_Success() {
        // Arrange
        when(keycloakTokenService.getServiceAccountToken()).thenReturn(TEST_ADMIN_TOKEN);

        String userId = "test-user-id";
        
        // Mock GET request
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        ObjectNode adminRole = objectMapper.createObjectNode();
        adminRole.put("name", "admin");
        
        ObjectNode userRole = objectMapper.createObjectNode();
        userRole.put("name", "user");
        
        ArrayNode roles = objectMapper.createArrayNode();
        roles.add(adminRole);
        roles.add(userRole);
        
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(roles));

        // Act
        List<String> result = keycloakUserService.getUserRealmRoles(userId);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.contains("admin"));
        assertTrue(result.contains("user"));
    }

    @Test
    void getUserRealmRoles_NoToken() {
        // Arrange
        when(keycloakTokenService.getServiceAccountToken()).thenReturn(null);

        // Act
        List<String> result = keycloakUserService.getUserRealmRoles("test-user-id");

        // Assert
        assertTrue(result.isEmpty());
    }
}
