package com.lazycord.service;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class KeycloakUserService {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakUserService.class);

    @Value("${keycloak.auth-server-url:http://localhost:8080}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm:lazycord}")
    private String keycloakRealm;

    @Value("${keycloak.admin-username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin-password:admin}")
    private String adminPassword;

    @Value("${keycloak.resource:admin-cli}")
    private String adminClientId;

    @Value("${keycloak.realm:master}")
    private String adminRealm;

    private Keycloak keycloakAdmin;

    @PostConstruct
    public void init() {
        keycloakAdmin = KeycloakBuilder.builder()
                .serverUrl(keycloakServerUrl)
                .realm(adminRealm)
                .clientId(adminClientId)
                .username(adminUsername)
                .password(adminPassword)
                .build();
    }

    private RealmResource getRealm() {
        return keycloakAdmin.realm(keycloakRealm);
    }

    public Optional<UserRepresentation> getUserById(String userId) {
        try {
            UserRepresentation user = getRealm().users().get(userId).toRepresentation();
            return Optional.ofNullable(user);
        } catch (Exception e) {
            logger.warn("User not found in Keycloak: {}", userId);
            return Optional.empty();
        }
    }

    public Optional<UserRepresentation> getUserByUsername(String username) {
        List<UserRepresentation> users = getRealm().users().searchByUsername(username, true);
        if (users != null && !users.isEmpty()) {
            return Optional.of(users.get(0));
        }
        return Optional.empty();
    }

    public Optional<UserRepresentation> getUserByEmail(String email) {
        List<UserRepresentation> users = getRealm().users().searchByEmail(email, true);
        if (users != null && !users.isEmpty()) {
            return Optional.of(users.get(0));
        }
        return Optional.empty();
    }

    public String createUser(String username, String email, String password, List<String> roles) {
        try {
            UsersResource usersResource = getRealm().users();
            
            // Check if user already exists
            if (getUserByUsername(username).isPresent()) {
                logger.warn("User with username {} already exists", username);
                return null;
            }
            if (getUserByEmail(email).isPresent()) {
                logger.warn("User with email {} already exists", email);
                return null;
            }
            
            UserRepresentation user = new UserRepresentation();
            user.setUsername(username);
            user.setEmail(email);
            user.setEmailVerified(true);
            user.setEnabled(true);
            
            // Set password
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(false);
            user.setCredentials(Collections.singletonList(credential));
            
            var response = usersResource.create(user);
            
            if (response.getStatus() == 201) {
                String userId = extractUserId(response);
                
                // Assign roles
                if (roles != null && !roles.isEmpty()) {
                    assignRolesToUser(userId, roles);
                }
                
                logger.info("User {} created in Keycloak with ID: {}", username, userId);
                return userId;
            } else {
                logger.error("Failed to create user in Keycloak: HTTP {}", response.getStatus());
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Failed to create user in Keycloak: {}", e.getMessage(), e);
            return null;
        }
    }

    public boolean updateUser(String userId, String email, Boolean enabled) {
        try {
            UserResource userResource = getRealm().users().get(userId);
            UserRepresentation user = userResource.toRepresentation();
            
            if (email != null) {
                user.setEmail(email);
            }
            if (enabled != null) {
                user.setEnabled(enabled);
            }
            
            userResource.update(user);
            logger.info("User {} updated in Keycloak", userId);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to update user in Keycloak: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean resetPassword(String userId, String newPassword) {
        try {
            UserResource userResource = getRealm().users().get(userId);
            
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(newPassword);
            credential.setTemporary(false);
            
            userResource.resetPassword(credential);
            logger.info("Password reset for user {}", userId);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to reset password: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean deleteUser(String userId) {
        try {
            getRealm().users().get(userId).remove();
            logger.info("User {} deleted from Keycloak", userId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to delete user from Keycloak: {}", e.getMessage(), e);
            return false;
        }
    }

    private void assignRolesToUser(String userId, List<String> roleNames) {
        try {
            RealmResource realm = getRealm();
            var userRoles = realm.users().get(userId).roles();
            
            for (String roleName : roleNames) {
                try {
                    var role = realm.roles().get(roleName).toRepresentation();
                    userRoles.realmLevel().add(Collections.singletonList(role));
                } catch (Exception e) {
                    logger.warn("Role {} not found", roleName);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to assign roles: {}", e.getMessage());
        }
    }

    private String extractUserId(jakarta.ws.rs.core.Response response) {
        if (response.getStatus() == 201) {
            String location = response.getLocation() != null ? response.getLocation().toString() : "";
            if (!location.isEmpty()) {
                return location.substring(location.lastIndexOf('/') + 1);
            }
        }
        return null;
    }
}
