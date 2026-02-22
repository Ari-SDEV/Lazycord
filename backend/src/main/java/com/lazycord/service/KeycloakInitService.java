package com.lazycord.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakInitService {

    private final WebClient.Builder webClientBuilder;
    private final KeycloakTokenService keycloakTokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${keycloak.auth-server-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${keycloak.realm:master}")
    private String adminRealm;

    @Value("${keycloak.admin-username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin-password:admin}")
    private String adminPassword;

    private static final String LAZYCORD_REALM = "lazycord";
    private static final String BACKEND_CLIENT_ID = "lazycord-backend";
    private static final String FRONTEND_CLIENT_ID = "lazycord-frontend";

    private WebClient getWebClient() {
        return webClientBuilder.baseUrl(baseUrl).build();
    }

    @PostConstruct
    public void initialize() {
        try {
            log.info("Initializing Keycloak realm and configuration...");
            
            // Get admin token first
            String adminToken = getAdminToken();
            if (adminToken == null) {
                log.warn("Could not get admin token, skipping Keycloak initialization");
                return;
            }

            // Create realm
            createRealmIfNotExists(adminToken);

            // Create roles
            createRolesIfNotExists(adminToken);

            // Create clients
            createBackendClientIfNotExists(adminToken);
            createFrontendClientIfNotExists(adminToken);

            // Create default users
            createDefaultUsersIfNotExists(adminToken);

            log.info("Keycloak initialization completed successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Keycloak: {}", e.getMessage(), e);
        }
    }

    private String getAdminToken() {
        String tokenUrl = baseUrl + "/realms/master/protocol/openid-connect/token";
        
        String formData = String.format(
            "grant_type=password&client_id=admin-cli&username=%s&password=%s",
            adminUsername, adminPassword
        );

        try {
            JsonNode response = getWebClient()
                .post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromValue(formData))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

            if (response != null && response.has("access_token")) {
                return response.get("access_token").asText();
            }
        } catch (Exception e) {
            log.error("Failed to get admin token: {}", e.getMessage());
        }
        return null;
    }

    private void createRealmIfNotExists(String adminToken) {
        // Check if realm exists
        String realmUrl = "/admin/realms/" + LAZYCORD_REALM;
        
        try {
            getWebClient()
                .get()
                .uri(baseUrl + realmUrl)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .toBodilessEntity()
                .block();
            log.info("Realm '{}' already exists", LAZYCORD_REALM);
            return;
        } catch (Exception e) {
            // Realm doesn't exist, create it
        }

        // Create realm
        ObjectNode realm = objectMapper.createObjectNode();
        realm.put("realm", LAZYCORD_REALM);
        realm.put("enabled", true);
        realm.put("displayName", "Lazycord");
        realm.put("displayNameHtml", "<div class='kc-logo-text'><span>Lazycord</span></div>");
        realm.put("accessTokenLifespan", 300); // 5 minutes

        try {
            getWebClient()
                .post()
                .uri(baseUrl + "/admin/realms")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(realm))
                .retrieve()
                .toBodilessEntity()
                .block();
            log.info("Realm '{}' created successfully", LAZYCORD_REALM);
        } catch (Exception e) {
            log.error("Failed to create realm: {}", e.getMessage());
        }
    }

    private void createRolesIfNotExists(String adminToken) {
        String[] roles = {"admin", "moderator", "user"};
        
        for (String roleName : roles) {
            // Check if role exists
            String roleUrl = "/admin/realms/%s/roles/%s".formatted(LAZYCORD_REALM, roleName);
            
            try {
                getWebClient()
                    .get()
                    .uri(baseUrl + roleUrl)
                    .header("Authorization", "Bearer " + adminToken)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
                log.info("Role '{}' already exists", roleName);
                continue;
            } catch (Exception e) {
                // Role doesn't exist
            }

            // Create role
            ObjectNode role = objectMapper.createObjectNode();
            role.put("name", roleName);
            role.put("description", "Lazycord " + roleName + " role");

            try {
                getWebClient()
                    .post()
                    .uri(baseUrl + "/admin/realms/%s/roles".formatted(LAZYCORD_REALM))
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(role))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
                log.info("Role '{}' created", roleName);
            } catch (Exception e) {
                log.error("Failed to create role '{}': {}", roleName, e.getMessage());
            }
        }
    }

    private void createBackendClientIfNotExists(String adminToken) {
        // Check if client exists
        String clientsUrl = "/admin/realms/%s/clients?clientId=%s".formatted(LAZYCORD_REALM, BACKEND_CLIENT_ID);
        
        try {
            JsonNode clients = getWebClient()
                .get()
                .uri(baseUrl + clientsUrl)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            if (clients != null && clients.isArray() && clients.size() > 0) {
                log.info("Client '{}' already exists", BACKEND_CLIENT_ID);
                return;
            }
        } catch (Exception e) {
            // Error checking
        }

        // Create client
        ObjectNode client = objectMapper.createObjectNode();
        client.put("clientId", BACKEND_CLIENT_ID);
        client.put("name", "Lazycord Backend");
        client.put("description", "Confidential client for Lazycord backend API");
        client.put("protocol", "openid-connect");
        client.put("publicClient", false);
        client.put("bearerOnly", false);
        client.put("serviceAccountsEnabled", true);
        client.put("authorizationServicesEnabled", true);
        client.put("directAccessGrantsEnabled", true);
        client.put("standardFlowEnabled", true);
        
        ArrayNode redirectUris = objectMapper.createArrayNode();
        redirectUris.add("http://localhost:8080/*");
        redirectUris.add("http://localhost:3000/*");
        client.set("redirectUris", redirectUris);
        
        ArrayNode webOrigins = objectMapper.createArrayNode();
        webOrigins.add("+");
        webOrigins.add("http://localhost:3000");
        webOrigins.add("http://localhost:8080");
        client.set("webOrigins", webOrigins);

        try {
            getWebClient()
                .post()
                .uri(baseUrl + "/admin/realms/%s/clients".formatted(LAZYCORD_REALM))
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(client))
                .retrieve()
                .toBodilessEntity()
                .block();
            log.info("Backend client '{}' created", BACKEND_CLIENT_ID);
        } catch (Exception e) {
            log.error("Failed to create backend client: {}", e.getMessage());
        }
    }

    private void createFrontendClientIfNotExists(String adminToken) {
        // Check if client exists
        String clientsUrl = "/admin/realms/%s/clients?clientId=%s".formatted(LAZYCORD_REALM, FRONTEND_CLIENT_ID);
        
        try {
            JsonNode clients = getWebClient()
                .get()
                .uri(baseUrl + clientsUrl)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            if (clients != null && clients.isArray() && clients.size() > 0) {
                log.info("Client '{}' already exists", FRONTEND_CLIENT_ID);
                return;
            }
        } catch (Exception e) {
            // Error checking
        }

        // Create client
        ObjectNode client = objectMapper.createObjectNode();
        client.put("clientId", FRONTEND_CLIENT_ID);
        client.put("name", "Lazycord Frontend");
        client.put("description", "Public client for Lazycord frontend");
        client.put("protocol", "openid-connect");
        client.put("publicClient", true);
        client.put("bearerOnly", false);
        client.put("standardFlowEnabled", true);
        client.put("directAccessGrantsEnabled", true);
        
        ArrayNode redirectUris = objectMapper.createArrayNode();
        redirectUris.add("http://localhost:3000/*");
        redirectUris.add("http://localhost:1420/*");
        redirectUris.add("tauri://localhost/*");
        client.set("redirectUris", redirectUris);
        
        ArrayNode webOrigins = objectMapper.createArrayNode();
        webOrigins.add("+");
        webOrigins.add("http://localhost:3000");
        client.set("webOrigins", webOrigins);

        try {
            getWebClient()
                .post()
                .uri(baseUrl + "/admin/realms/%s/clients".formatted(LAZYCORD_REALM))
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(client))
                .retrieve()
                .toBodilessEntity()
                .block();
            log.info("Frontend client '{}' created", FRONTEND_CLIENT_ID);
        } catch (Exception e) {
            log.error("Failed to create frontend client: {}", e.getMessage());
        }
    }

    private void createDefaultUsersIfNotExists(String adminToken) {
        // Create admin user
        createUserIfNotExists(adminToken, "admin", "admin123", "admin@lazycord.local", 
                List.of("admin", "moderator", "user"));
        
        // Create moderator user
        createUserIfNotExists(adminToken, "moderator", "mod123", "moderator@lazycord.local", 
                List.of("moderator", "user"));
        
        // Create regular user
        createUserIfNotExists(adminToken, "user", "user123", "user@lazycord.local", 
                List.of("user"));
    }

    private void createUserIfNotExists(String adminToken, String username, String password, 
            String email, List<String> roleNames) {
        // Check if user exists
        String usersUrl = "/admin/realms/%s/users?username=%s&exact=true".formatted(LAZYCORD_REALM, username);
        
        try {
            JsonNode users = getWebClient()
                .get()
                .uri(baseUrl + usersUrl)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            if (users != null && users.isArray() && users.size() > 0) {
                log.info("User '{}' already exists", username);
                return;
            }
        } catch (Exception e) {
            // Error checking
        }

        // Create user
        ObjectNode user = objectMapper.createObjectNode();
        user.put("username", username);
        user.put("email", email);
        user.put("emailVerified", true);
        user.put("enabled", true);
        
        // Password credentials
        ObjectNode credential = objectMapper.createObjectNode();
        credential.put("type", "password");
        credential.put("value", password);
        credential.put("temporary", false);
        ArrayNode credentials = objectMapper.createArrayNode();
        credentials.add(credential);
        user.set("credentials", credentials);

        String userId = null;
        try {
            JsonNode response = getWebClient()
                .post()
                .uri(baseUrl + "/admin/realms/%s/users".formatted(LAZYCORD_REALM))
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(user))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            log.info("User '{}' created", username);
            
            // Get user ID from search (since POST doesn't return body)
            JsonNode createdUsers = getWebClient()
                .get()
                .uri(baseUrl + "/admin/realms/%s/users?username=%s&exact=true".formatted(LAZYCORD_REALM, username))
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            if (createdUsers != null && createdUsers.isArray() && createdUsers.size() > 0) {
                userId = createdUsers.get(0).get("id").asText();
            }
        } catch (Exception e) {
            log.error("Failed to create user '{}': {}", username, e.getMessage());
            return;
        }

        // Assign roles
        if (userId != null) {
            for (String roleName : roleNames) {
                assignRoleToUser(adminToken, userId, roleName);
            }
            log.info("User '{}' created with roles {}", username, roleNames);
        }
    }

    private void assignRoleToUser(String adminToken, String userId, String roleName) {
        // Get role
        String roleUrl = "/admin/realms/%s/roles/%s".formatted(LAZYCORD_REALM, roleName);
        JsonNode role = null;
        
        try {
            role = getWebClient()
                .get()
                .uri(baseUrl + roleUrl)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        } catch (Exception e) {
            log.warn("Role '{}' not found", roleName);
            return;
        }

        if (role == null) return;

        // Assign role
        String assignUrl = "/admin/realms/%s/users/%s/role-mappings/realm".formatted(LAZYCORD_REALM, userId);
        
        ArrayNode roles = objectMapper.createArrayNode();
        ObjectNode roleObj = objectMapper.createObjectNode();
        roleObj.put("id", role.get("id").asText());
        roleObj.put("name", role.get("name").asText());
        roles.add(roleObj);

        try {
            getWebClient()
                .post()
                .uri(baseUrl + assignUrl)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(roles))
                .retrieve()
                .toBodilessEntity()
                .block();
        } catch (Exception e) {
            log.error("Failed to assign role '{}': {}", roleName, e.getMessage());
        }
    }
}
