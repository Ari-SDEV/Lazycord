package com.lazycord.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lazycord.dto.UserDto;
import com.lazycord.dto.UserLoginRequest;
import com.lazycord.dto.UserRegistrationRequest;
import com.lazycord.model.User;
import com.lazycord.service.KeycloakTokenService;
import com.lazycord.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:1420", "tauri://localhost"})
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final KeycloakTokenService keycloakTokenService;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${keycloak.auth-server-url:http://localhost:8080}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm:lazycord}")
    private String keycloakRealm;

    @Value("${keycloak.frontend-client-id:lazycord-frontend}")
    private String frontendClientId;

    private WebClient getWebClient() {
        return webClientBuilder.baseUrl(keycloakServerUrl).build();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationRequest request) {
        try {
            log.info("Registering user: {}", request.getUsername());
            
            User user = userService.createUser(request);
            
            // Return tokens by logging in the newly created user
            return login(new UserLoginRequest(request.getUsername(), request.getPassword()));
            
        } catch (RuntimeException e) {
            log.error("Registration failed: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserLoginRequest request) {
        try {
            log.info("Login attempt for user: {}", request.getUsername());
            
            String tokenUrl = "/realms/%s/protocol/openid-connect/token".formatted(keycloakRealm);
            
            String formData = String.format(
                "grant_type=password&client_id=%s&username=%s&password=%s",
                frontendClientId, request.getUsername(), request.getPassword()
            );
            
            JsonNode tokenResponse = getWebClient()
                .post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromValue(formData))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            if (tokenResponse == null || !tokenResponse.has("access_token")) {
                throw new RuntimeException("Invalid credentials");
            }
            
            String accessToken = tokenResponse.get("access_token").asText();
            String keycloakId = extractUserIdFromToken(accessToken);
            User user = userService.syncUserWithKeycloak(keycloakId);
            
            // Update last active
            userService.updateLastActive(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("access_token", accessToken);
            response.put("refresh_token", tokenResponse.get("refresh_token").asText());
            response.put("expires_in", tokenResponse.get("expires_in").asInt());
            response.put("token_type", tokenResponse.get("token_type").asText("Bearer"));
            response.put("user", new UserDto(user));
            
            log.info("Login successful for user: {}", request.getUsername());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
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
            log.info("Refreshing access token");
            
            String tokenUrl = "/realms/%s/protocol/openid-connect/token".formatted(keycloakRealm);
            
            String formData = String.format(
                "grant_type=refresh_token&client_id=%s&refresh_token=%s",
                frontendClientId, refreshToken
            );
            
            JsonNode tokenResponse = getWebClient()
                .post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromValue(formData))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            if (tokenResponse == null || !tokenResponse.has("access_token")) {
                throw new RuntimeException("Invalid refresh token");
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("access_token", tokenResponse.get("access_token").asText());
            response.put("refresh_token", tokenResponse.get("refresh_token").asText());
            response.put("expires_in", tokenResponse.get("expires_in").asInt());
            response.put("token_type", tokenResponse.get("token_type").asText("Bearer"));
            
            log.info("Token refreshed successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
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
            log.info("Getting current user: {}", username);
            
            Optional<User> userOpt = userService.findByUsername(username);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                userService.updateLastActive(user);
                return ResponseEntity.ok(new UserDto(user));
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User profile not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
        } catch (Exception e) {
            log.error("Error getting current user: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        try {
            SecurityContextHolder.clearContext();
            log.info("User logged out");
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Logged out successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Logout error: {}", e.getMessage());
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
            String[] parts = accessToken.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(Base64.getDecoder().decode(parts[1]));
                int subIndex = payload.indexOf("\"sub\":");
                if (subIndex >= 0) {
                    int start = payload.indexOf("\"", subIndex + 6) + 1;
                    int end = payload.indexOf("\"", start);
                    return payload.substring(start, end);
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract user ID from token", e);
        }
        return null;
    }
}
