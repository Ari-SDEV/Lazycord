package com.lazycord.utils;

import com.lazycord.dto.UserDto;
import com.lazycord.dto.UserLoginRequest;
import com.lazycord.dto.UserRegistrationRequest;
import com.lazycord.model.User;
import org.keycloak.representations.idm.UserRepresentation;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Factory class for creating test data objects.
 * Provides centralized test data creation for consistency across tests.
 */
public class TestDataFactory {

    // Constants for test data
    public static final String TEST_USER_ID = "test-user-id-123";
    public static final String TEST_KEYCLOAK_ID = "keycloak-id-456";
    public static final String TEST_USERNAME = "testuser";
    public static final String TEST_EMAIL = "test@example.com";
    public static final String TEST_PASSWORD = "password123";
    public static final String TEST_AVATAR_URL = "https://example.com/avatar.png";

    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_EMAIL = "admin@lazycord.local";
    public static final String ADMIN_PASSWORD = "admin123";

    public static final String MODERATOR_USERNAME = "moderator";
    public static final String MODERATOR_EMAIL = "moderator@lazycord.local";
    public static final String MODERATOR_PASSWORD = "mod123";

    /**
     * Creates a basic test user with default values.
     */
    public static User createTestUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setKeycloakId(TEST_KEYCLOAK_ID);
        user.setUsername(TEST_USERNAME);
        user.setEmail(TEST_EMAIL);
        user.setAvatarUrl(TEST_AVATAR_URL);
        user.setPoints(100);
        user.setXp(500);
        user.setLevel(3);
        user.setRank("Regular");
        user.setCreatedAt(LocalDateTime.now().minusDays(7));
        user.setLastActive(LocalDateTime.now());
        return user;
    }

    /**
     * Creates a test user with custom values.
     */
    public static User createUser(String username, String email, String keycloakId) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setKeycloakId(keycloakId);
        user.setUsername(username);
        user.setEmail(email);
        user.setPoints(0);
        user.setXp(0);
        user.setLevel(1);
        user.setRank("Newbie");
        user.setCreatedAt(LocalDateTime.now());
        user.setLastActive(LocalDateTime.now());
        return user;
    }

    /**
     * Creates a test admin user.
     */
    public static User createAdminUser() {
        User user = createUser(ADMIN_USERNAME, ADMIN_EMAIL, "admin-keycloak-id");
        user.setPoints(1000);
        user.setXp(5000);
        user.setLevel(10);
        user.setRank("Veteran");
        return user;
    }

    /**
     * Creates a test moderator user.
     */
    public static User createModeratorUser() {
        User user = createUser(MODERATOR_USERNAME, MODERATOR_EMAIL, "moderator-keycloak-id");
        user.setPoints(500);
        user.setXp(2500);
        user.setLevel(7);
        user.setRank("Expert");
        return user;
    }

    /**
     * Creates a Keycloak UserRepresentation for testing.
     */
    public static UserRepresentation createKeycloakUser() {
        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setId(TEST_KEYCLOAK_ID);
        kcUser.setUsername(TEST_USERNAME);
        kcUser.setEmail(TEST_EMAIL);
        kcUser.setEmailVerified(true);
        kcUser.setEnabled(true);
        return kcUser;
    }

    /**
     * Creates a Keycloak UserRepresentation with custom values.
     */
    public static UserRepresentation createKeycloakUser(String id, String username, String email) {
        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setId(id);
        kcUser.setUsername(username);
        kcUser.setEmail(email);
        kcUser.setEmailVerified(true);
        kcUser.setEnabled(true);
        return kcUser;
    }

    /**
     * Creates a user registration request with default values.
     */
    public static UserRegistrationRequest createRegistrationRequest() {
        return new UserRegistrationRequest(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD);
    }

    /**
     * Creates a user registration request with custom values.
     */
    public static UserRegistrationRequest createRegistrationRequest(String username, String email, String password) {
        return new UserRegistrationRequest(username, email, password);
    }

    /**
     * Creates a user login request with default values.
     */
    public static UserLoginRequest createLoginRequest() {
        return new UserLoginRequest(TEST_USERNAME, TEST_PASSWORD);
    }

    /**
     * Creates a user login request with custom values.
     */
    public static UserLoginRequest createLoginRequest(String username, String password) {
        return new UserLoginRequest(username, password);
    }

    /**
     * Creates a UserDto from a User entity.
     */
    public static UserDto createUserDto(User user) {
        return new UserDto(user);
    }

    /**
     * Creates a UserDto with default values.
     */
    public static UserDto createUserDto() {
        return new UserDto(createTestUser());
    }

    /**
     * Creates a list of test users.
     */
    public static List<User> createTestUsers(int count) {
        return Arrays.asList(
            createUser("user1", "user1@example.com", "kc-1"),
            createUser("user2", "user2@example.com", "kc-2"),
            createUser("user3", "user3@example.com", "kc-3")
        );
    }
}
