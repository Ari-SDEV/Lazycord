package com.lazycord.controller;

import com.lazycord.dto.UserDto;
import com.lazycord.dto.UserLoginRequest;
import com.lazycord.dto.UserRegistrationRequest;
import com.lazycord.model.User;
import com.lazycord.service.UserService;
import jakarta.validation.Valid;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:1420", "tauri://localhost"})
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    @Value("${keycloak.auth-server-url:http://localhost:8080}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm:lazycord}")
    private String keycloakRealm;

    @Value("${keycloak.resource:lazycord-backend}")
    private String keycloakClientId;

    @Value("${keycloak.credentials.secret:}")
    private String keycloakClientSecret;

    @Value("${keycloak.frontend-client-id:lazycord-frontend}")
    private String frontendClientId;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationRequest request) {
        try {
            logger.info("Registering user: {}", request.getUsername());
            
            User user = userService.createUser(request);
            
            // Return tokens by logging in the newly created user
            return login(new UserLoginRequest(request.getUsername(), request.getPassword()));
            
        } catch (RuntimeException e) {
            logger.error("Registration failed: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserLoginRequest request) {
        try {
            logger.info("Login attempt for user: {}", request.getUsername());
            
            // Authenticate with Keycloak
            Keycloak keycloak = KeycloakBuilder.builder()
                    .serverUrl(keycloakServerUrl)
                    .realm(keycloakRealm)
                    .clientId(frontendClientId)
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .grantType("password")
                    .build();

            AccessTokenResponse tokenResponse = keycloak.tokenManager().getAccessToken();
            
            // Sync user with local database - parse userId from token
            String accessToken = tokenResponse.getToken();
            String keycloakId = extractUserIdFromToken(accessToken);
            User user = userService.syncUserWithKeycloak(keycloakId);
            
            // Update last active
            userService.updateLastActive(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("access_token", tokenResponse.getToken());
            response.put("refresh_token", tokenResponse.getRefreshToken());
            response.put("expires_in", tokenResponse.getExpiresIn());
            response.put("refresh_expires_in", tokenResponse.getRefreshExpiresIn());
            response.put("token_type", tokenResponse.getTokenType());
            response.put("user", new UserDto(user));
            
            logger.info("Login successful for user: {}", request.getUsername());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Login failed: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refresh_token");
        
        if (refreshToken == null || refreshToken.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Refresh token is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        
        try {
            logger.info("Refreshing access token");
            
            // Use Keycloak's token endpoint to refresh via REST call
            AccessTokenResponse tokenResponse = refreshAccessToken(refreshToken);
            
            Map<String, Object> response = new HashMap<>();
            response.put("access_token", tokenResponse.getToken());
            response.put("refresh_token", tokenResponse.getRefreshToken());
            response.put("expires_in", tokenResponse.getExpiresIn());
            response.put("token_type", tokenResponse.getTokenType());
            
            logger.info("Token refreshed successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Token refresh failed: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid refresh token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        
        try {
            String username = authentication.getName();
            logger.info("Getting current user: {}", username);
            
            Optional<User> userOpt = userService.findByUsername(username);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                userService.updateLastActive(user);
                return ResponseEntity.ok(new UserDto(user));
            } else {
                // User might be authenticated but not synced to local DB
                Map<String, String> error = new HashMap<>();
                error.put("error", "User profile not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
        } catch (Exception e) {
            logger.error("Error getting current user: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        try {
            SecurityContextHolder.clearContext();
            logger.info("User logged out");
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Logged out successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Logout error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Logout failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Extract user ID from JWT token (sub claim)
     */
    private String extractUserIdFromToken(String accessToken) {
        try {
            // Simple JWT parsing - split by . and decode payload
            String[] parts = accessToken.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(java.util.Base64.getDecoder().decode(parts[1]));
                // Extract sub claim using simple string parsing
                int subIndex = payload.indexOf("\"sub\":");
                if (subIndex >= 0) {
                    int start = payload.indexOf("\"", subIndex + 6) + 1;
                    int end = payload.indexOf("\"", start);
                    return payload.substring(start, end);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to extract user ID from token", e);
        }
        return null;
    }

    /**
     * Refresh access token using Keycloak token endpoint
     */
    private AccessTokenResponse refreshAccessToken(String refreshToken) {
        try {
            String tokenUrl = keycloakServerUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token";
            
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String formData = "grant_type=refresh_token"
                    + "&client_id=" + frontendClientId
                    + "&refresh_token=" + java.net.URLEncoder.encode(refreshToken, java.nio.charset.StandardCharsets.UTF_8);
            
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(formData))
                    .build();
            
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                // Parse JSON response manually
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(response.body(), AccessTokenResponse.class);
            } else {
                throw new RuntimeException("Token refresh failed: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            logger.error("Failed to refresh token", e);
            throw new RuntimeException("Token refresh failed", e);
        }
    }
}
