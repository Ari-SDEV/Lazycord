package com.lazycord.utils;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mock a Keycloak authenticated user for security tests.
 * 
 * Example usage:
 * <pre>
 * @Test
 * @WithMockKeycloakUser(username = "testuser", roles = {"user", "admin"})
 * void testProtectedEndpoint() {
 *     // Test with authenticated user
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockKeycloakUserSecurityContextFactory.class)
public @interface WithMockKeycloakUser {

    /**
     * The username of the mock user.
     * Defaults to "testuser".
     */
    String username() default "testuser";

    /**
     * The Keycloak ID of the mock user.
     * Defaults to "test-keycloak-id".
     */
    String keycloakId() default "test-keycloak-id";

    /**
     * The email of the mock user.
     * Defaults to "test@example.com".
     */
    String email() default "test@example.com";

    /**
     * The roles assigned to the mock user.
     * Defaults to {"user"}.
     */
    String[] roles() default {"user"};

    /**
     * Whether the user is enabled.
     * Defaults to true.
     */
    boolean enabled() default true;
}
