package com.lazycord.repository;

import com.lazycord.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserRepository.
 * Tests all custom query methods and JpaRepository functionality.
 * Uses H2 in-memory database for fast test execution.
 */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private static final String TEST_KEYCLOAK_ID = "test-keycloak-id-123";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_FIRST_NAME = "Test";
    private static final String TEST_LAST_NAME = "User";

    @BeforeEach
    void setUp() {
        // Clear repository before each test
        userRepository.deleteAll();

        // Create and save a test user
        testUser = new User();
        testUser.setKeycloakId(TEST_KEYCLOAK_ID);
        testUser.setUsername(TEST_USERNAME);
        testUser.setEmail(TEST_EMAIL);
        testUser.setFirstName(TEST_FIRST_NAME);
        testUser.setLastName(TEST_LAST_NAME);
        testUser.setPoints(0);
        testUser.setXp(0);
        testUser.setLevel(1);
        testUser.setRank("Newbie");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setLastActive(LocalDateTime.now());

        testUser = userRepository.save(testUser);
    }

    @Test
    void testFindByKeycloakId() {
        // Act
        Optional<User> result = userRepository.findByKeycloakId(TEST_KEYCLOAK_ID);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(TEST_KEYCLOAK_ID, result.get().getKeycloakId());
        assertEquals(TEST_USERNAME, result.get().getUsername());
        assertEquals(TEST_EMAIL, result.get().getEmail());
    }

    @Test
    void testFindByKeycloakId_NotFound() {
        // Act
        Optional<User> result = userRepository.findByKeycloakId("non-existent-id");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testFindByUsername() {
        // Act
        Optional<User> result = userRepository.findByUsername(TEST_USERNAME);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(TEST_USERNAME, result.get().getUsername());
        assertEquals(TEST_KEYCLOAK_ID, result.get().getKeycloakId());
    }

    @Test
    void testFindByUsername_NotFound() {
        // Act
        Optional<User> result = userRepository.findByUsername("nonexistentuser");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testFindByEmail() {
        // Act
        Optional<User> result = userRepository.findByEmail(TEST_EMAIL);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(TEST_EMAIL, result.get().getEmail());
        assertEquals(TEST_USERNAME, result.get().getUsername());
    }

    @Test
    void testFindByEmail_NotFound() {
        // Act
        Optional<User> result = userRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testExistsByUsername_Exists() {
        // Act
        boolean exists = userRepository.existsByUsername(TEST_USERNAME);

        // Assert
        assertTrue(exists);
    }

    @Test
    void testExistsByUsername_NotExists() {
        // Act
        boolean exists = userRepository.existsByUsername("nonexistentuser");

        // Assert
        assertFalse(exists);
    }

    @Test
    void testExistsByEmail_Exists() {
        // Act
        boolean exists = userRepository.existsByEmail(TEST_EMAIL);

        // Assert
        assertTrue(exists);
    }

    @Test
    void testExistsByEmail_NotExists() {
        // Act
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertFalse(exists);
    }

    @Test
    void testSaveUser() {
        // Arrange
        User newUser = new User();
        newUser.setKeycloakId("new-keycloak-id");
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setFirstName("New");
        newUser.setLastName("User");
        newUser.setPoints(0);
        newUser.setXp(0);
        newUser.setLevel(1);
        newUser.setRank("Newbie");

        // Act
        User savedUser = userRepository.save(newUser);

        // Assert
        assertNotNull(savedUser.getId());
        assertEquals("new-keycloak-id", savedUser.getKeycloakId());
        assertEquals("newuser", savedUser.getUsername());
        assertEquals("new@example.com", savedUser.getEmail());
        assertNotNull(savedUser.getCreatedAt());
    }

    @Test
    void testFindById() {
        // Act
        Optional<User> result = userRepository.findById(testUser.getId());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUser.getId(), result.get().getId());
    }

    @Test
    void testFindById_NotFound() {
        // Act
        Optional<User> result = userRepository.findById(UUID.randomUUID());

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testDeleteUser() {
        // Act
        userRepository.delete(testUser);

        // Assert
        assertFalse(userRepository.existsById(testUser.getId()));
        assertFalse(userRepository.existsByUsername(TEST_USERNAME));
        assertFalse(userRepository.existsByEmail(TEST_EMAIL));
    }

    @Test
    void testUpdateUser() {
        // Arrange
        testUser.setPoints(100);
        testUser.setXp(500);
        testUser.setLevel(3);
        testUser.setRank("Regular");
        testUser.setAvatarUrl("https://example.com/avatar.png");

        // Act
        User updatedUser = userRepository.save(testUser);

        // Assert
        assertEquals(100, updatedUser.getPoints());
        assertEquals(500, updatedUser.getXp());
        assertEquals(3, updatedUser.getLevel());
        assertEquals("Regular", updatedUser.getRank());
        assertEquals("https://example.com/avatar.png", updatedUser.getAvatarUrl());
    }

    @Test
    void testFindAll() {
        // Arrange - Add more users
        User user2 = new User();
        user2.setKeycloakId("kc-2");
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        user2.setFirstName("User");
        user2.setLastName("Two");
        user2.setPoints(0);
        user2.setXp(0);
        user2.setLevel(1);
        user2.setRank("Newbie");
        userRepository.save(user2);

        User user3 = new User();
        user3.setKeycloakId("kc-3");
        user3.setUsername("user3");
        user3.setEmail("user3@example.com");
        user3.setFirstName("User");
        user3.setLastName("Three");
        user3.setPoints(0);
        user3.setXp(0);
        user3.setLevel(1);
        user3.setRank("Newbie");
        userRepository.save(user3);

        // Act
        var allUsers = userRepository.findAll();

        // Assert
        assertEquals(3, allUsers.size());
    }

    @Test
    void testCount() {
        // Arrange - Add more users
        User user2 = new User();
        user2.setKeycloakId("kc-2");
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        user2.setFirstName("User");
        user2.setLastName("Two");
        user2.setPoints(0);
        user2.setXp(0);
        user2.setLevel(1);
        user2.setRank("Newbie");
        userRepository.save(user2);

        // Act
        long count = userRepository.count();

        // Assert
        assertEquals(2, count);
    }

    @Test
    void testExistsById() {
        // Act & Assert
        assertTrue(userRepository.existsById(testUser.getId()));
        assertFalse(userRepository.existsById(UUID.randomUUID()));
    }

    @Test
    void testUniqueConstraints() {
        // Arrange - Try to create a user with duplicate username
        User duplicateUsernameUser = new User();
        duplicateUsernameUser.setKeycloakId("different-kc-id");
        duplicateUsernameUser.setUsername(TEST_USERNAME); // Same username
        duplicateUsernameUser.setEmail("different@example.com");
        duplicateUsernameUser.setFirstName("Different");
        duplicateUsernameUser.setLastName("User");
        duplicateUsernameUser.setPoints(0);
        duplicateUsernameUser.setXp(0);
        duplicateUsernameUser.setLevel(1);
        duplicateUsernameUser.setRank("Newbie");

        // Act & Assert - Should throw exception due to unique constraint
        assertThrows(Exception.class, () -> {
            userRepository.save(duplicateUsernameUser);
            userRepository.flush(); // Force the persistence to happen
        });
    }

    @Test
    void testUniqueEmailConstraint() {
        // Arrange - Try to create a user with duplicate email
        User duplicateEmailUser = new User();
        duplicateEmailUser.setKeycloakId("different-kc-id-2");
        duplicateEmailUser.setUsername("differentuser");
        duplicateEmailUser.setEmail(TEST_EMAIL); // Same email
        duplicateEmailUser.setFirstName("Different");
        duplicateEmailUser.setLastName("User");
        duplicateEmailUser.setPoints(0);
        duplicateEmailUser.setXp(0);
        duplicateEmailUser.setLevel(1);
        duplicateEmailUser.setRank("Newbie");

        // Act & Assert - Should throw exception due to unique constraint
        assertThrows(Exception.class, () -> {
            userRepository.save(duplicateEmailUser);
            userRepository.flush(); // Force the persistence to happen
        });
    }

    @Test
    void testUniqueKeycloakIdConstraint() {
        // Arrange - Try to create a user with duplicate keycloakId
        User duplicateKeycloakUser = new User();
        duplicateKeycloakUser.setKeycloakId(TEST_KEYCLOAK_ID); // Same keycloakId
        duplicateKeycloakUser.setUsername("differentuser");
        duplicateKeycloakUser.setEmail("different@example.com");
        duplicateKeycloakUser.setFirstName("Different");
        duplicateKeycloakUser.setLastName("User");
        duplicateKeycloakUser.setPoints(0);
        duplicateKeycloakUser.setXp(0);
        duplicateKeycloakUser.setLevel(1);
        duplicateKeycloakUser.setRank("Newbie");

        // Act & Assert - Should throw exception due to unique constraint
        assertThrows(Exception.class, () -> {
            userRepository.save(duplicateKeycloakUser);
            userRepository.flush(); // Force the persistence to happen
        });
    }
}
