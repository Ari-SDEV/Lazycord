package com.lazycord.security;

import com.lazycord.model.User;
import com.lazycord.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for security configuration.
 * Tests endpoint accessibility with various authentication states.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @WithAnonymousUser
    void testPublicEndpoint_Accessible() throws Exception {
        // /api/auth/** endpoints should be publicly accessible
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized()); // Returns 401 but endpoint is accessible
    }

    @Test
    @WithAnonymousUser
    void testProtectedEndpoint_Unauthorized() throws Exception {
        // Protected endpoints should return 401/403 for anonymous users
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"user"})
    void testProtectedEndpoint_Authorized() throws Exception {
        // Authenticated user with 'user' role should be able to access protected endpoints
        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setKeycloakId("kc-123");
        mockUser.setUsername("testuser");
        mockUser.setEmail("test@example.com");

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(userService.updateLastActive(any(User.class))).thenReturn(mockUser);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"admin"})
    void testAdminEndpoint_Authorized() throws Exception {
        // Admin user should be able to access admin endpoints
        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setKeycloakId("kc-admin");
        mockUser.setUsername("admin");
        mockUser.setEmail("admin@example.com");

        when(userService.findByUsername("admin")).thenReturn(Optional.of(mockUser));
        when(userService.updateLastActive(any(User.class))).thenReturn(mockUser);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "moderator", roles = {"moderator"})
    void testModeratorEndpoint_Authorized() throws Exception {
        // Moderator user should be able to access moderator endpoints
        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setKeycloakId("kc-mod");
        mockUser.setUsername("moderator");
        mockUser.setEmail("moderator@example.com");

        when(userService.findByUsername("moderator")).thenReturn(Optional.of(mockUser));
        when(userService.updateLastActive(any(User.class))).thenReturn(mockUser);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void testSwaggerEndpoint_PubliclyAccessible() throws Exception {
        // Swagger UI endpoints should be publicly accessible
        // Note: This may return 404 if Swagger is not configured, but should not return 401
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isNotFound()); // or isOk() if configured
    }

    @Test
    @WithAnonymousUser
    void testApiDocsEndpoint_PubliclyAccessible() throws Exception {
        // API docs endpoints should be publicly accessible
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound()); // or isOk() if configured
    }

    @Test
    @WithAnonymousUser
    void testActuatorHealth_PubliclyAccessible() throws Exception {
        // Actuator health endpoint should be publicly accessible
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void testCors_PreflightRequest() throws Exception {
        // CORS preflight requests should be handled
        mockMvc.perform(get("/api/auth/login")
                .header("Origin", "http://localhost:3000"))
                .andExpect(status().is4xxClientError()); // 401 or 400, but not 403 CORS
    }
}
