package com.lazycord.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lazycord.dto.UserRegistrationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakUserService {

    private final WebClient.Builder webClientBuilder;
    private final KeycloakTokenService keycloakTokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${keycloak.auth-server-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${keycloak.realm:lazycord}")
    private String realm;

    @Value("${keycloak.resource:lazycord-backend}")
    private String clientId;

    private WebClient getWebClient() {
        return webClientBuilder.baseUrl(baseUrl).build();
    }

    /**
     * Gets the service account token for Keycloak Admin API calls.
     */
    private String getAdminToken() {
        return keycloakTokenService.getServiceAccountToken();
    }

    /**
     * Creates a new user in Keycloak.
     * Returns the created user's ID.
     */
    public String createUser(String username, String email, String password, String role, String firstName, String lastName) {
        String adminToken = getAdminToken();
        if (adminToken == null) {
            throw new RuntimeException("Service account not configured");
        }

        // Step 1: Create user
        // IMPORTANT: requiredActions must be an empty array to avoid "Account is not fully set up" error
        Map<String, Object> userPayload = Map.of(
            "username", username,
            "email", email,
            "enabled", true,
            "emailVerified", true,
            "firstName", firstName != null ? firstName : "",
            "lastName", lastName != null ? lastName : "",
            "requiredActions", Collections.emptyList()
        );

        String createUserUrl = "/admin/realms/%s/users".formatted(realm);
        String userId = getWebClient()
            .post()
            .uri(createUserUrl, realm)
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(userPayload))
            .exchangeToMono(resp -> {
                if (resp.statusCode().is2xxSuccessful()) {
                    // Keycloak returns 201 Created with Location header containing the user ID
                    String location = resp.headers().asHttpHeaders().getFirst("Location");
                    if (location != null && location.contains("/")) {
                        return Mono.just(location.substring(location.lastIndexOf("/") + 1));
                    }
                    // If no Location header, we need to search for the user
                    return Mono.just(username); // Fallback - will search below
                }
                if (resp.statusCode().value() == 409) {
                    return Mono.error(new RuntimeException("User already exists: " + username));
                }
                return resp.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(body -> Mono.error(new RuntimeException("Failed to create user: " + resp.statusCode() + " " + body)));
            })
            .block();

        // If we got username as fallback, search for the user
        if (userId == null || userId.equals(username)) {
            userId = findUserIdByUsername(username);
        }

        if (userId == null) {
            throw new RuntimeException("Failed to get user ID after creation");
        }

        // Step 2: Set password
        setUserPassword(userId, password);

        // Step 3: Assign role if specified
        if (role != null && !role.isEmpty()) {
            assignRealmRoleToUser(userId, role);
        }

        return userId;
    }

    /**
     * Updates an existing user in Keycloak.
     */
    public void updateUser(String keycloakId, String email, String firstName, String lastName, Boolean enabled) {
        String adminToken = getAdminToken();
        if (adminToken == null) {
            throw new RuntimeException("Service account not configured");
        }

        Map<String, Object> updates = new HashMap<>();
        if (email != null) updates.put("email", email);
        if (firstName != null) updates.put("firstName", firstName);
        if (lastName != null) updates.put("lastName", lastName);
        if (enabled != null) updates.put("enabled", enabled);

        if (updates.isEmpty()) {
            return; // Nothing to update
        }

        String updateUrl = "/admin/realms/%s/users/%s".formatted(realm, keycloakId);
        
        getWebClient()
            .put()
            .uri(updateUrl)
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(updates))
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    /**
     * Deletes a user from Keycloak.
     */
    public void deleteUser(String keycloakId) {
        String adminToken = getAdminToken();
        if (adminToken == null) {
            throw new RuntimeException("Service account not configured");
        }

        String deleteUrl = "/admin/realms/%s/users/%s".formatted(realm, keycloakId);
        
        getWebClient()
            .delete()
            .uri(deleteUrl)
            .header("Authorization", "Bearer " + adminToken)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    /**
     * Resets a user's password.
     */
    public void resetPassword(String keycloakId, String newPassword) {
        String adminToken = getAdminToken();
        if (adminToken == null) {
            throw new RuntimeException("Service account not configured");
        }

        Map<String, Object> credentialPayload = Map.of(
            "type", "password",
            "value", newPassword,
            "temporary", false
        );

        String resetUrl = "/admin/realms/%s/users/%s/reset-password".formatted(realm, keycloakId);
        
        getWebClient()
            .put()
            .uri(resetUrl)
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(credentialPayload))
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    /**
     * Finds a user by username and returns their Keycloak ID.
     */
    public String findUserIdByUsername(String username) {
        String adminToken = getAdminToken();
        if (adminToken == null) {
            return null;
        }

        String searchUrl = "/admin/realms/%s/users?username=%s&exact=true".formatted(realm, username);
        
        return getWebClient()
            .get()
            .uri(searchUrl)
            .header("Authorization", "Bearer " + adminToken)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(users -> {
                if (users.isArray() && users.size() > 0) {
                    return users.get(0).get("id").asText();
                }
                return null;
            })
            .block();
    }

    /**
     * Gets user details by ID.
     */
    public JsonNode getUserById(String keycloakId) {
        String adminToken = getAdminToken();
        if (adminToken == null) {
            return null;
        }

        String userUrl = "/admin/realms/%s/users/%s".formatted(realm, keycloakId);
        
        return getWebClient()
            .get()
            .uri(userUrl)
            .header("Authorization", "Bearer " + adminToken)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
    }

    /**
     * Gets user details by username.
     */
    public JsonNode getUserByUsername(String username) {
        String userId = findUserIdByUsername(username);
        if (userId == null) {
            return null;
        }
        return getUserById(userId);
    }

    /**
     * Assigns a realm role to a user.
     */
    public void assignRealmRoleToUser(String keycloakId, String roleName) {
        String adminToken = getAdminToken();
        if (adminToken == null) {
            throw new RuntimeException("Service account not configured");
        }

        // First, get the role representation
        String roleUrl = "/admin/realms/%s/roles/%s".formatted(realm, roleName);
        JsonNode role = getWebClient()
            .get()
            .uri(roleUrl)
            .header("Authorization", "Bearer " + adminToken)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

        if (role == null) {
            throw new RuntimeException("Role not found: " + roleName);
        }

        // Assign role to user
        String assignUrl = "/admin/realms/%s/users/%s/role-mappings/realm".formatted(realm, keycloakId);
        List<Map<String, Object>> rolesPayload = List.of(Map.of(
            "id", role.get("id").asText(),
            "name", role.get("name").asText()
        ));

        getWebClient()
            .post()
            .uri(assignUrl)
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(rolesPayload))
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    /**
     * Removes a realm role from a user.
     */
    public void removeRealmRoleFromUser(String keycloakId, String roleName) {
        String adminToken = getAdminToken();
        if (adminToken == null) {
            throw new RuntimeException("Service account not configured");
        }

        // Get the role representation
        String roleUrl = "/admin/realms/%s/roles/%s".formatted(realm, roleName);
        JsonNode role = getWebClient()
            .get()
            .uri(roleUrl)
            .header("Authorization", "Bearer " + adminToken)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

        if (role == null) {
            return; // Role doesn't exist, nothing to remove
        }

        // Remove role from user
        String removeUrl = "/admin/realms/%s/users/%s/role-mappings/realm".formatted(realm, keycloakId);
        List<Map<String, Object>> rolesPayload = List.of(Map.of(
            "id", role.get("id").asText(),
            "name", role.get("name").asText()
        ));

        getWebClient()
            .method(org.springframework.http.HttpMethod.DELETE)
            .uri(removeUrl)
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(rolesPayload))
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    /**
     * Gets all realm roles assigned to a user.
     */
    public List<String> getUserRealmRoles(String keycloakId) {
        String adminToken = getAdminToken();
        if (adminToken == null) {
            return Collections.emptyList();
        }

        String rolesUrl = "/admin/realms/%s/users/%s/role-mappings/realm".formatted(realm, keycloakId);
        
        return getWebClient()
            .get()
            .uri(rolesUrl)
            .header("Authorization", "Bearer " + adminToken)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(roles -> {
                List<String> roleNames = new ArrayList<>();
                if (roles.isArray()) {
                    for (JsonNode role : roles) {
                        roleNames.add(role.get("name").asText());
                    }
                }
                return roleNames;
            })
            .block();
    }

    private void setUserPassword(String userId, String password) {
        String adminToken = getAdminToken();
        if (adminToken == null) {
            throw new RuntimeException("Service account not configured");
        }

        Map<String, Object> credentialPayload = Map.of(
            "type", "password",
            "value", password,
            "temporary", false
        );

        String passwordUrl = "/admin/realms/%s/users/%s/reset-password".formatted(realm, userId);
        
        getWebClient()
            .put()
            .uri(passwordUrl)
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(credentialPayload))
            .retrieve()
            .toBodilessEntity()
            .onErrorResume(e -> {
                log.error("Failed to set password for user {}: {}", userId, e.getMessage());
                return Mono.empty();
            })
            .block();
    }
}
