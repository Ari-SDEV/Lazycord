package com.lazycord.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for KeycloakInitService.
 * Tests realm initialization, client creation, and role management.
 */
@ExtendWith(MockitoExtension.class)
class KeycloakInitServiceTest {

    @Mock
    private Keycloak keycloakAdmin;

    @Mock
    private RealmsResource realmsResource;

    @Mock
    private RealmResource realmResource;

    @Mock
    private RolesResource rolesResource;

    @Mock
    private ClientsResource clientsResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private RoleResource roleResource;

    @Mock
    private ClientResource clientResource;

    @Mock
    private UserResource userResource;

    @Mock
    private RoleMappingResource roleMappingResource;

    @Mock
    private RoleScopeResource roleScopeResource;

    @InjectMocks
    private KeycloakInitService keycloakInitService;

    private static final String LAZYCORD_REALM = "lazycord";
    private static final String BACKEND_CLIENT_ID = "lazycord-backend";
    private static final String FRONTEND_CLIENT_ID = "lazycord-frontend";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(keycloakInitService, "keycloakServerUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(keycloakInitService, "adminUsername", "admin");
        ReflectionTestUtils.setField(keycloakInitService, "adminPassword", "admin");
        ReflectionTestUtils.setField(keycloakInitService, "adminRealm", "master");
        ReflectionTestUtils.setField(keycloakInitService, "adminClientId", "admin-cli");
        
        // Mock buildKeycloakAdmin to return our mocked Keycloak instance
        keycloakInitService = spy(keycloakInitService);
        doReturn(keycloakAdmin).when(keycloakInitService).buildKeycloakAdmin();
    }

    @Test
    void testInitKeycloakRealm_Success() {
        // Arrange
        when(keycloakAdmin.realms()).thenReturn(realmsResource);
        when(realmsResource.findAll()).thenReturn(new ArrayList<>());
        when(realmsResource.realm(LAZYCORD_REALM)).thenReturn(realmResource);
        when(realmResource.roles()).thenReturn(rolesResource);
        when(realmResource.clients()).thenReturn(clientsResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(rolesResource.get(anyString())).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(new RoleRepresentation());
        when(clientsResource.findByClientId(BACKEND_CLIENT_ID)).thenReturn(Collections.emptyList());
        when(clientsResource.findByClientId(FRONTEND_CLIENT_ID)).thenReturn(Collections.emptyList());

        // Create success response
        Response successResponse = Response.status(201)
                .location(URI.create("http://localhost:8080/admin/realms/lazycord/users/test-id"))
                .build();
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(successResponse);
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
        when(usersResource.searchByUsername(anyString(), anyBoolean())).thenReturn(Collections.emptyList());

        // Act & Assert - Should not throw any exception
        assertDoesNotThrow(() -> {
            ReflectionTestUtils.invokeMethod(keycloakInitService, "initialize");
        });
    }

    @Test
    void testInitKeycloakRealm_AlreadyExists() {
        // Arrange - Realm already exists
        when(keycloakAdmin.realms()).thenReturn(realmsResource);
        RealmRepresentation existingRealm = new RealmRepresentation();
        existingRealm.setRealm(LAZYCORD_REALM);
        when(realmsResource.findAll()).thenReturn(List.of(existingRealm));
        when(realmsResource.realm(LAZYCORD_REALM)).thenReturn(realmResource);
        when(realmResource.toRepresentation()).thenReturn(existingRealm);
        when(realmResource.roles()).thenReturn(rolesResource);
        when(realmResource.clients()).thenReturn(clientsResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(rolesResource.get(anyString())).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(new RoleRepresentation());
        when(clientsResource.findByClientId(BACKEND_CLIENT_ID)).thenReturn(Collections.emptyList());
        when(clientsResource.findByClientId(FRONTEND_CLIENT_ID)).thenReturn(Collections.emptyList());
        when(usersResource.searchByUsername(anyString(), anyBoolean())).thenReturn(Collections.emptyList());

        Response successResponse = Response.status(201)
                .location(URI.create("http://localhost:8080/admin/realms/lazycord/users/test-id"))
                .build();
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(successResponse);
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);

        // Act & Assert
        assertDoesNotThrow(() -> {
            ReflectionTestUtils.invokeMethod(keycloakInitService, "initialize");
        });

        verify(realmsResource, never()).create(any(RealmRepresentation.class));
    }

    @Test
    void testCreateRealm() {
        // Arrange
        when(keycloakAdmin.realms()).thenReturn(realmsResource);
        when(realmsResource.realm(LAZYCORD_REALM)).thenReturn(realmResource);
        when(realmResource.toRepresentation()).thenThrow(new NotFoundException("Realm not found"));

        // Act
        ReflectionTestUtils.invokeMethod(keycloakInitService, "createRealmIfNotExists", keycloakAdmin);

        // Assert
        verify(realmsResource).create(any(RealmRepresentation.class));
    }

    @Test
    void testCreateClients() {
        // Arrange
        when(keycloakAdmin.realms()).thenReturn(realmsResource);
        when(realmsResource.realm(LAZYCORD_REALM)).thenReturn(realmResource);
        when(realmResource.toRepresentation()).thenReturn(new RealmRepresentation());
        when(realmResource.clients()).thenReturn(clientsResource);
        when(realmResource.roles()).thenReturn(rolesResource);
        when(clientsResource.findByClientId(BACKEND_CLIENT_ID)).thenReturn(Collections.emptyList());
        when(clientsResource.findByClientId(FRONTEND_CLIENT_ID)).thenReturn(Collections.emptyList());
        when(rolesResource.get("admin")).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(new RoleRepresentation());

        // Mock users for service account role assignment
        ClientRepresentation clientRepresentation = new ClientRepresentation();
        clientRepresentation.setId("service-account-id");
        when(clientsResource.findByClientId(BACKEND_CLIENT_ID))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(clientRepresentation));

        // Act
        ReflectionTestUtils.invokeMethod(keycloakInitService, "initialize");

        // Assert
        verify(clientsResource, atLeastOnce()).create(any(ClientRepresentation.class));
    }

    @Test
    void testCreateRealmRoles() {
        // Arrange
        when(keycloakAdmin.realms()).thenReturn(realmsResource);
        when(realmsResource.realm(LAZYCORD_REALM)).thenReturn(realmResource);
        when(realmResource.toRepresentation()).thenReturn(new RealmRepresentation());
        when(realmResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get("admin")).thenReturn(roleResource);
        when(rolesResource.get("moderator")).thenReturn(roleResource);
        when(rolesResource.get("user")).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenThrow(new NotFoundException("Role not found"));

        // Act
        ReflectionTestUtils.invokeMethod(keycloakInitService, "createRolesIfNotExists", realmResource);

        // Assert
        verify(rolesResource, times(3)).create(any(RoleRepresentation.class));
    }

    @Test
    void testCreateRealmRoles_AlreadyExist() {
        // Arrange - Roles already exist
        when(rolesResource.get("admin")).thenReturn(roleResource);
        when(rolesResource.get("moderator")).thenReturn(roleResource);
        when(rolesResource.get("user")).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(new RoleRepresentation());

        // Act
        ReflectionTestUtils.invokeMethod(keycloakInitService, "createRolesIfNotExists", realmResource);

        // Assert - No new roles should be created
        verify(rolesResource, never()).create(any(RoleRepresentation.class));
    }

    @Test
    void testCreateBackendClient_AlreadyExists() {
        // Arrange - Backend client already exists
        when(clientsResource.findByClientId(BACKEND_CLIENT_ID))
                .thenReturn(List.of(new ClientRepresentation()));

        // Act
        ReflectionTestUtils.invokeMethod(keycloakInitService, "createBackendClientIfNotExists", realmResource);

        // Assert - No new client should be created
        verify(clientsResource, never()).create(any(ClientRepresentation.class));
    }

    @Test
    void testCreateFrontendClient_AlreadyExists() {
        // Arrange - Frontend client already exists
        when(clientsResource.findByClientId(FRONTEND_CLIENT_ID))
                .thenReturn(List.of(new ClientRepresentation()));

        // Act
        ReflectionTestUtils.invokeMethod(keycloakInitService, "createFrontendClientIfNotExists", realmResource);

        // Assert - No new client should be created
        verify(clientsResource, never()).create(any(ClientRepresentation.class));
    }
}
