package com.lazycord.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for KeycloakUserService.
 * Tests user CRUD operations and password management in Keycloak.
 */
@ExtendWith(MockitoExtension.class)
class KeycloakUserServiceTest {

    @Mock
    private Keycloak keycloakAdmin;

    @Mock
    private RealmsResource realmsResource;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @Mock
    private RolesResource rolesResource;

    @Mock
    private RoleResource roleResource;

    @Mock
    private RoleMappingResource roleMappingResource;

    @Mock
    private RoleScopeResource roleScopeResource;

    @InjectMocks
    private KeycloakUserService keycloakUserService;

    private static final String TEST_KEYCLOAK_ID = "test-keycloak-id-123";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String REALM_NAME = "lazycord";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(keycloakUserService, "keycloakServerUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(keycloakUserService, "keycloakRealm", REALM_NAME);
        ReflectionTestUtils.setField(keycloakUserService, "adminUsername", "admin");
        ReflectionTestUtils.setField(keycloakUserService, "adminPassword", "admin");
        ReflectionTestUtils.setField(keycloakUserService, "adminClientId", "admin-cli");
        ReflectionTestUtils.setField(keycloakUserService, "adminRealm", "master");
        ReflectionTestUtils.setField(keycloakUserService, "keycloakAdmin", keycloakAdmin);
    }

    @Test
    void testCreateUser() {
        // Arrange
        when(keycloakAdmin.realm(REALM_NAME)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.searchByUsername(TEST_USERNAME, true)).thenReturn(Collections.emptyList());
        when(usersResource.searchByEmail(TEST_EMAIL, true)).thenReturn(Collections.emptyList());

        // Create success response with Location header
        Response successResponse = Response.status(201)
                .location(URI.create("http://localhost:8080/admin/realms/lazycord/users/" + TEST_KEYCLOAK_ID))
                .build();
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(successResponse);

        // Act
        String result = keycloakUserService.createUser(
                TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD, List.of("user"));

        // Assert
        assertNotNull(result);
        assertEquals(TEST_KEYCLOAK_ID, result);
        verify(usersResource).create(any(UserRepresentation.class));
    }

    @Test
    void testCreateUser_DuplicateUsername() {
        // Arrange
        when(keycloakAdmin.realm(REALM_NAME)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        UserRepresentation existingUser = new UserRepresentation();
        existingUser.setUsername(TEST_USERNAME);
        when(usersResource.searchByUsername(TEST_USERNAME, true)).thenReturn(List.of(existingUser));

        // Act
        String result = keycloakUserService.createUser(
                TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD, List.of("user"));

        // Assert
        assertNull(result);
        verify(usersResource, never()).create(any(UserRepresentation.class));
    }

    @Test
    void testCreateUser_DuplicateEmail() {
        // Arrange
        when(keycloakAdmin.realm(REALM_NAME)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.searchByUsername(TEST_USERNAME, true)).thenReturn(Collections.emptyList());

        UserRepresentation existingUser = new UserRepresentation();
        existingUser.setEmail(TEST_EMAIL);
        when(usersResource.searchByEmail(TEST_EMAIL, true)).thenReturn(List.of(existingUser));

        // Act
        String result = keycloakUserService.createUser(
                TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD, List.of("user"));

        // Assert
        assertNull(result);
        verify(usersResource, never()).create(any(UserRepresentation.class));
    }

    @Test
    void testCreateUser_FailedResponse() {
        // Arrange
        when(keycloakAdmin.realm(REALM_NAME)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.searchByUsername(TEST_USERNAME, true)).thenReturn(Collections.emptyList());
        when(usersResource.searchByEmail(TEST_EMAIL, true)).thenReturn(Collections.emptyList());

        // Create failed response
        Response failedResponse = Response.status(400).build();
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(failedResponse);

        // Act
        String result = keycloakUserService.createUser(
                TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD, List.of("user"));

        // Assert
        assertNull(result);
    }

    @Test
    void testGetUserById() {
        // Arrange
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId(TEST_KEYCLOAK_ID);
        userRepresentation.setUsername(TEST_USERNAME);
        userRepresentation.setEmail(TEST_EMAIL);

        when(keycloakAdmin.realm(REALM_NAME)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(TEST_KEYCLOAK_ID)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRepresentation);

        // Act
        Optional<UserRepresentation> result = keycloakUserService.getUserById(TEST_KEYCLOAK_ID);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(TEST_KEYCLOAK_ID, result.get().getId());
        assertEquals(TEST_USERNAME, result.get().getUsername());
    }

    @Test
    void testGetUserById_NotFound() {
        // Arrange
        when(keycloakAdmin.realm(REALM_NAME)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(TEST_KEYCLOAK_ID)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenThrow(new NotFoundException("User not found"));

        // Act
        Optional<UserRepresentation> result = keycloakUserService.getUserById(TEST_KEYCLOAK_ID);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetUserByUsername() {
        // Arrange
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId(TEST_KEYCLOAK_ID);
        userRepresentation.setUsername(TEST_USERNAME);

        when(keycloakAdmin.realm(REALM_NAME)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.searchByUsername(TEST_USERNAME, true)).thenReturn(List.of(userRepresentation));

        // Act
        Optional<UserRepresentation> result = keycloakUserService.getUserByUsername(TEST_USERNAME);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(TEST_USERNAME, result.get().getUsername());
    }

    @Test
    void testGetUserByUsername_NotFound() {
        // Arrange
        when(keycloakAdmin.realm(REALM_NAME)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.searchByUsername(TEST_USERNAME, true)).thenReturn(Collections.emptyList());

        // Act
        Optional<UserRepresentation> result = keycloakUserService.getUserByUsername(TEST_USERNAME);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testUpdateUser() {
        // Arrange
        UserRepresentation existingUser = new UserRepresentation();
        existingUser.setId(TEST_KEYCLOAK_ID);
        existingUser.setUsername(TEST_USERNAME);
        existingUser.setEmail("old@example.com");

        when(keycloakAdmin.realm(REALM_NAME)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(TEST_KEYCLOAK_ID)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(existingUser);

        // Act
        boolean result = keycloakUserService.updateUser(
                TEST_KEYCLOAK_ID, "new@example.com", true);

        // Assert
        assertTrue(result);
        verify(userResource).update(any(UserRepresentation.class));
    }

    @Test
    void testUpdateUser_Failure() {
        // Arrange
        when(keycloakAdmin.realm(REALM_NAME)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(TEST_KEYCLOAK_ID)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenThrow(new NotFoundException("User not found"));

        // Act
        boolean result = keycloakUserService.updateUser(
                TEST_KEYCLOAK_ID, "new@example.com", true);

        // Assert
        assertFalse(result);
    }

    @Test
    void testDeleteUser() {
        // Arrange
        when(keycloakAdmin.realm(REALM_NAME)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(TEST_KEYCLOAK_ID)).thenReturn(userResource);
        doNothing().when(userResource).remove();

        // Act
        boolean result = keycloakUserService.deleteUser(TEST_KEYCLOAK_ID);

        // Assert
        assertTrue(result);
        verify(userResource).remove();
    }

    @Test
    void testDeleteUser_Failure() {
        // Arrange
        when(keycloakAdmin.realm(REALM_NAME)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(TEST_KEYCLOAK_ID)).thenReturn(userResource);
        doThrow(new NotFoundException("User not found")).when(userResource).remove();

        // Act
        boolean result = keycloakUserService.deleteUser(TEST_KEYCLOAK_ID);

        // Assert
        assertFalse(result);
    }

    @Test
    void testResetPassword() {
        // Arrange
        when(keycloakAdmin.realm(REALM_NAME)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(TEST_KEYCLOAK_ID)).thenReturn(userResource);
        doNothing().when(userResource).resetPassword(any(CredentialRepresentation.class));

        // Act
        boolean result = keycloakUserService.resetPassword(TEST_KEYCLOAK_ID, "newpassword123");

        // Assert
        assertTrue(result);
        verify(userResource).resetPassword(any(CredentialRepresentation.class));
    }

    @Test
    void testResetPassword_Failure() {
        // Arrange
        when(keycloakAdmin.realm(REALM_NAME)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(TEST_KEYCLOAK_ID)).thenReturn(userResource);
        doThrow(new NotFoundException("User not found"))
                .when(userResource).resetPassword(any(CredentialRepresentation.class));

        // Act
        boolean result = keycloakUserService.resetPassword(TEST_KEYCLOAK_ID, "newpassword123");

        // Assert
        assertFalse(result);
    }

    @Test
    void testGetUserByEmail() {
        // Arrange
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId(TEST_KEYCLOAK_ID);
        userRepresentation.setEmail(TEST_EMAIL);

        when(keycloakAdmin.realm(REALM_NAME)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.searchByEmail(TEST_EMAIL, true)).thenReturn(List.of(userRepresentation));

        // Act
        Optional<UserRepresentation> result = keycloakUserService.getUserByEmail(TEST_EMAIL);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(TEST_EMAIL, result.get().getEmail());
    }
}
