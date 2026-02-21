package com.lazycord.service;

import jakarta.annotation.PostConstruct;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class KeycloakInitService {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakInitService.class);

    @Value("${keycloak.auth-server-url:http://localhost:8080}")
    private String keycloakServerUrl;

    @Value("${keycloak.admin-username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin-password:admin}")
    private String adminPassword;

    @Value("${keycloak.realm:master}")
    private String adminRealm;

    @Value("${keycloak.resource:admin-cli}")
    private String adminClientId;

    private static final String LAZYCORD_REALM = "lazycord";
    private static final String BACKEND_CLIENT_ID = "lazycord-backend";
    private static final String FRONTEND_CLIENT_ID = "lazycord-frontend";

    @PostConstruct
    public void initialize() {
        try {
            logger.info("Initializing Keycloak realm and configuration...");
            
            Keycloak keycloakAdmin = KeycloakBuilder.builder()
                    .serverUrl(keycloakServerUrl)
                    .realm(adminRealm)
                    .clientId(adminClientId)
                    .username(adminUsername)
                    .password(adminPassword)
                    .build();

            // Create or update realm
            createRealmIfNotExists(keycloakAdmin);

            // Get realm resource
            RealmResource realmResource = keycloakAdmin.realm(LAZYCORD_REALM);

            // Create roles
            createRolesIfNotExists(realmResource);

            // Create clients
            createBackendClientIfNotExists(realmResource);
            createFrontendClientIfNotExists(realmResource);

            // Create default users
            createDefaultUsersIfNotExists(realmResource);

            logger.info("Keycloak initialization completed successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Keycloak: {}", e.getMessage(), e);
        }
    }

    private void createRealmIfNotExists(Keycloak keycloakAdmin) {
        try {
            keycloakAdmin.realm(LAZYCORD_REALM).toRepresentation();
            logger.info("Realm '{}' already exists, skipping creation", LAZYCORD_REALM);
        } catch (Exception e) {
            logger.info("Creating realm '{}'", LAZYCORD_REALM);
            
            RealmRepresentation realm = new RealmRepresentation();
            realm.setRealm(LAZYCORD_REALM);
            realm.setEnabled(true);
            realm.setDisplayName("Lazycord");
            realm.setDisplayNameHtml("<div class=\"kc-logo-text\"><span>Lazycord</span></div>");
            
            // Token settings
            realm.setAccessTokenLifespan(300); // 5 minutes
            realm.setRefreshTokenLifespan(1800); // 30 minutes
            
            keycloakAdmin.realms().create(realm);
            logger.info("Realm '{}' created successfully", LAZYCORD_REALM);
        }
    }

    private void createRolesIfNotExists(RealmResource realmResource) {
        String[] roles = {"admin", "moderator", "user"};
        
        for (String roleName : roles) {
            try {
                realmResource.roles().get(roleName).toRepresentation();
                logger.info("Role '{}' already exists", roleName);
            } catch (Exception e) {
                logger.info("Creating role '{}'", roleName);
                
                RoleRepresentation role = new RoleRepresentation();
                role.setName(roleName);
                role.setDescription("Lazycord " + roleName + " role");
                
                realmResource.roles().create(role);
                logger.info("Role '{}' created successfully", roleName);
            }
        }
    }

    private void createBackendClientIfNotExists(RealmResource realmResource) {
        try {
            realmResource.clients().get(BACKEND_CLIENT_ID).toRepresentation();
            logger.info("Client '{}' already exists", BACKEND_CLIENT_ID);
        } catch (Exception e) {
            logger.info("Creating backend client '{}'", BACKEND_CLIENT_ID);
            
            ClientRepresentation client = new ClientRepresentation();
            client.setClientId(BACKEND_CLIENT_ID);
            client.setName("Lazycord Backend");
            client.setDescription("Confidential client for Lazycord backend API");
            client.setProtocol("openid-connect");
            client.setPublicClient(false);
            client.setBearerOnly(false);
            client.setServiceAccountsEnabled(true);
            client.setAuthorizationServicesEnabled(true);
            client.setDirectAccessGrantsEnabled(true);
            client.setStandardFlowEnabled(true);
            client.setRedirectUris(Arrays.asList("http://localhost:8080/*", "http://localhost:3000/*"));
            client.setWebOrigins(Arrays.asList("+", "http://localhost:3000", "http://localhost:8080"));
            
            realmResource.clients().create(client);
            
            // Assign manage-users role to service account
            String serviceAccountId = realmResource.clients().findByClientId(BACKEND_CLIENT_ID)
                    .stream().findFirst().map(ClientRepresentation::getId).orElse(null);
            
            if (serviceAccountId != null) {
                RoleRepresentation manageUsersRole = realmResource.roles().get("admin").toRepresentation();
                realmResource.users().get(serviceAccountId).roles().realmLevel().add(Arrays.asList(manageUsersRole));
            }
            
            logger.info("Backend client '{}' created successfully", BACKEND_CLIENT_ID);
        }
    }

    private void createFrontendClientIfNotExists(RealmResource realmResource) {
        try {
            realmResource.clients().get(FRONTEND_CLIENT_ID).toRepresentation();
            logger.info("Client '{}' already exists", FRONTEND_CLIENT_ID);
        } catch (Exception e) {
            logger.info("Creating frontend client '{}'", FRONTEND_CLIENT_ID);
            
            ClientRepresentation client = new ClientRepresentation();
            client.setClientId(FRONTEND_CLIENT_ID);
            client.setName("Lazycord Frontend");
            client.setDescription("Public client for Lazycord frontend");
            client.setProtocol("openid-connect");
            client.setPublicClient(true);
            client.setBearerOnly(false);
            client.setStandardFlowEnabled(true);
            client.setDirectAccessGrantsEnabled(true);
            client.setRedirectUris(Arrays.asList(
                "http://localhost:3000/*",
                "http://localhost:1420/*",
                "tauri://localhost/*"
            ));
            client.setWebOrigins(Arrays.asList("+", "http://localhost:3000"));
            
            realmResource.clients().create(client);
            logger.info("Frontend client '{}' created successfully", FRONTEND_CLIENT_ID);
        }
    }

    private void createDefaultUsersIfNotExists(RealmResource realmResource) {
        // Create admin user
        createUserIfNotExists(realmResource, "admin", "admin123", "admin@lazycord.local", 
                Arrays.asList("admin", "moderator", "user"));
        
        // Create moderator user
        createUserIfNotExists(realmResource, "moderator", "mod123", "moderator@lazycord.local", 
                Arrays.asList("moderator", "user"));
        
        // Create regular user
        createUserIfNotExists(realmResource, "user", "user123", "user@lazycord.local", 
                Arrays.asList("user"));
    }

    private void createUserIfNotExists(RealmResource realmResource, String username, 
            String password, String email, List<String> roleNames) {
        try {
            List<org.keycloak.representations.idm.UserRepresentation> users = 
                    realmResource.users().searchByUsername(username, true);
            
            if (users != null && !users.isEmpty()) {
                logger.info("User '{}' already exists", username);
                return;
            }
            
            logger.info("Creating user '{}' with roles {}", username, roleNames);
            
            org.keycloak.representations.idm.UserRepresentation user = 
                    new org.keycloak.representations.idm.UserRepresentation();
            user.setUsername(username);
            user.setEmail(email);
            user.setEmailVerified(true);
            user.setEnabled(true);
            
            // Set password
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(false);
            user.setCredentials(Arrays.asList(credential));
            
            // Create user
            var response = realmResource.users().create(user);
            String userId = extractUserId(response);
            
            if (userId != null) {
                // Assign roles
                List<RoleRepresentation> rolesToAssign = new ArrayList<>();
                for (String roleName : roleNames) {
                    try {
                        RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
                        rolesToAssign.add(role);
                    } catch (Exception ex) {
                        logger.warn("Role '{}' not found", roleName);
                    }
                }
                
                if (!rolesToAssign.isEmpty()) {
                    realmResource.users().get(userId).roles().realmLevel().add(rolesToAssign);
                }
                
                logger.info("User '{}' created successfully with roles {}", username, roleNames);
            }
            
        } catch (Exception e) {
            logger.error("Failed to create user '{}': {}", username, e.getMessage());
        }
    }

    private String extractUserId(javax.ws.rs.core.Response response) {
        if (response.getStatus() == 201) {
            String location = response.getLocation() != null ? response.getLocation().toString() : "";
            if (!location.isEmpty()) {
                return location.substring(location.lastIndexOf('/') + 1);
            }
        }
        return null;
    }
}
