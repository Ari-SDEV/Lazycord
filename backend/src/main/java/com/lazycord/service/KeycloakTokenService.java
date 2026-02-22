package com.lazycord.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Map;

@Service
@Slf4j
public class KeycloakTokenService {

    private final WebClient webClient;

    @Value("${keycloak.auth-server-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${keycloak.realm:lazycord}")
    private String realm;

    @Value("${keycloak.resource:lazycord-backend}")
    private String clientId;

    @Value("${keycloak.credentials.secret:}")
    private String clientSecret;

    @Value("${keycloak.admin-username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin-password:admin}")
    private String adminPassword;

    private String serviceAccountToken;
    private Instant tokenExpiry;

    public KeycloakTokenService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Gets a valid service account token for Keycloak Admin API calls.
     * Caches the token and refreshes it when needed.
     */
    public String getServiceAccountToken() {
        // Check if we have a valid cached token
        if (serviceAccountToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry.minusSeconds(60))) {
            return serviceAccountToken;
        }

        // Request new token using client credentials
        String tokenUrl = baseUrl + "/realms/master/protocol/openid-connect/token";
        
        String formData;
        if (clientSecret != null && !clientSecret.isEmpty()) {
            // Client credentials flow
            formData = String.format(
                "grant_type=client_credentials&client_id=%s&client_secret=%s",
                clientId, clientSecret
            );
        } else {
            // Password credentials flow (for admin user)
            formData = String.format(
                "grant_type=password&client_id=%s&username=%s&password=%s",
                "admin-cli", adminUsername, adminPassword
            );
        }

        try {
            JsonNode response = webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromValue(formData))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

            if (response != null && response.has("access_token")) {
                serviceAccountToken = response.get("access_token").asText();
                int expiresIn = response.has("expires_in") ? response.get("expires_in").asInt() : 300;
                tokenExpiry = Instant.now().plusSeconds(expiresIn);
                log.debug("Obtained new service account token, expires in {} seconds", expiresIn);
                return serviceAccountToken;
            }
        } catch (Exception e) {
            log.error("Failed to obtain service account token: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Validates a user token with Keycloak.
     */
    public boolean validateToken(String token) {
        String introspectUrl = baseUrl + "/realms/" + realm + "/protocol/openid-connect/token/introspect";
        
        String formData = String.format(
            "token=%s&client_id=%s&client_secret=%s",
            token, clientId, clientSecret
        );

        try {
            JsonNode response = webClient.post()
                .uri(introspectUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromValue(formData))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

            return response != null && response.has("active") && response.get("active").asBoolean();
        } catch (Exception e) {
            log.error("Failed to validate token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Gets user info from a token.
     */
    public JsonNode getUserInfo(String token) {
        String userInfoUrl = baseUrl + "/realms/" + realm + "/protocol/openid-connect/userinfo";
        
        try {
            return webClient.get()
                .uri(userInfoUrl)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        } catch (Exception e) {
            log.error("Failed to get user info: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Refreshes an access token using a refresh token.
     */
    public Map<String, String> refreshToken(String refreshToken) {
        String tokenUrl = baseUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        
        String formData = String.format(
            "grant_type=refresh_token&client_id=%s&client_secret=%s&refresh_token=%s",
            clientId, clientSecret, refreshToken
        );

        try {
            JsonNode response = webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromValue(formData))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

            if (response != null) {
                return Map.of(
                    "access_token", response.get("access_token").asText(),
                    "refresh_token", response.get("refresh_token").asText(),
                    "expires_in", String.valueOf(response.get("expires_in").asInt())
                );
            }
        } catch (Exception e) {
            log.error("Failed to refresh token: {}", e.getMessage());
        }

        return null;
    }
}
