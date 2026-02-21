package com.lazycord.service;

import com.lazycord.dto.UserRegistrationRequest;
import com.lazycord.model.User;
import com.lazycord.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 * Tests user CRUD operations, syncing with Keycloak, and user progression.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeycloakUserService keycloakUserService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID testUserId;
    private static final String TEST_KEYCLOAK_ID = "test-keycloak-id-123";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setKeycloakId(TEST_KEYCLOAK_ID);
        testUser.setUsername(TEST_USERNAME);
        testUser.setEmail(TEST_EMAIL);
        testUser.setPoints(0);
        testUser.setXp(0);
        testUser.setLevel(1);
        testUser.setRank("Newbie");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setLastActive(LocalDateTime.now());
    }

    @Test
    void testCreateUser() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest(
                TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD);

        when(userRepository.existsByUsername(TEST_USERNAME)).thenReturn(false);
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(keycloakUserService.createUser(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD, List.of("user")))
                .thenReturn(TEST_KEYCLOAK_ID);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(testUserId);
            return savedUser;
        });

        // Act
        User result = userService.createUser(request);

        // Assert
        assertNotNull(result);
        assertEquals(testUserId, result.getId());
        assertEquals(TEST_KEYCLOAK_ID, result.getKeycloakId());
        assertEquals(TEST_USERNAME, result.getUsername());
        assertEquals(TEST_EMAIL, result.getEmail());
        assertEquals(0, result.getPoints());
        assertEquals(0, result.getXp());
        assertEquals(1, result.getLevel());
        assertEquals("Newbie", result.getRank());

        verify(userRepository).save(any(User.class));
        verify(keycloakUserService).createUser(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD, List.of("user"));
    }

    @Test
    void testCreateUser_DuplicateUsername() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest(
                TEST_USERNAME, "other@example.com", TEST_PASSWORD);

        when(userRepository.existsByUsername(TEST_USERNAME)).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.createUser(request);
        });

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testCreateUser_DuplicateEmail() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest(
                "otheruser", TEST_EMAIL, TEST_PASSWORD);

        when(userRepository.existsByUsername("otheruser")).thenReturn(false);
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.createUser(request);
        });

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testCreateUser_KeycloakFailure() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest(
                TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD);

        when(userRepository.existsByUsername(TEST_USERNAME)).thenReturn(false);
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(keycloakUserService.createUser(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD, List.of("user")))
                .thenReturn(null); // Keycloak creation failed

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.createUser(request);
        });

        assertEquals("Failed to create user in Keycloak", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testGetUserById() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findById(testUserId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUserId, result.get().getId());
        assertEquals(TEST_USERNAME, result.get().getUsername());
    }

    @Test
    void testGetUserById_NotFound() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findById(testUserId);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetUserByKeycloakId() {
        // Arrange
        when(userRepository.findByKeycloakId(TEST_KEYCLOAK_ID)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findByKeycloakId(TEST_KEYCLOAK_ID);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(TEST_KEYCLOAK_ID, result.get().getKeycloakId());
    }

    @Test
    void testSyncUserWithKeycloak_UserExists() {
        // Arrange
        UserRepresentation keycloakUser = new UserRepresentation();
        keycloakUser.setId(TEST_KEYCLOAK_ID);
        keycloakUser.setUsername(TEST_USERNAME);
        keycloakUser.setEmail(TEST_EMAIL);

        when(keycloakUserService.getUserById(TEST_KEYCLOAK_ID)).thenReturn(Optional.of(keycloakUser));
        when(userRepository.findByKeycloakId(TEST_KEYCLOAK_ID)).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.syncUserWithKeycloak(TEST_KEYCLOAK_ID);

        // Assert
        assertNotNull(result);
        assertEquals(testUserId, result.getId());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testSyncUserWithKeycloak_NewUser() {
        // Arrange
        UserRepresentation keycloakUser = new UserRepresentation();
        keycloakUser.setId(TEST_KEYCLOAK_ID);
        keycloakUser.setUsername(TEST_USERNAME);
        keycloakUser.setEmail(TEST_EMAIL);

        when(keycloakUserService.getUserById(TEST_KEYCLOAK_ID)).thenReturn(Optional.of(keycloakUser));
        when(userRepository.findByKeycloakId(TEST_KEYCLOAK_ID)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(testUserId);
            return savedUser;
        });

        // Act
        User result = userService.syncUserWithKeycloak(TEST_KEYCLOAK_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_KEYCLOAK_ID, result.getKeycloakId());
        assertEquals(TEST_USERNAME, result.getUsername());
        assertEquals(TEST_EMAIL, result.getEmail());
        assertEquals(0, result.getPoints());
        assertEquals(0, result.getXp());
        assertEquals(1, result.getLevel());
        assertEquals("Newbie", result.getRank());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void testSyncUserWithKeycloak_UserNotFoundInKeycloak() {
        // Arrange
        when(keycloakUserService.getUserById(TEST_KEYCLOAK_ID)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.syncUserWithKeycloak(TEST_KEYCLOAK_ID);
        });

        assertEquals("User not found in Keycloak", exception.getMessage());
    }

    @Test
    void testUpdateUser() {
        // Arrange
        User updateRequest = new User();
        updateRequest.setAvatarUrl("https://example.com/new-avatar.png");
        updateRequest.setEmail("updated@example.com");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(keycloakUserService.updateUser(TEST_KEYCLOAK_ID, "updated@example.com", null))
                .thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.updateUser(testUserId, updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals("https://example.com/new-avatar.png", result.getAvatarUrl());
        assertEquals("updated@example.com", result.getEmail());
        verify(keycloakUserService).updateUser(TEST_KEYCLOAK_ID, "updated@example.com", null);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testUpdateUser_NotFound() {
        // Arrange
        User updateRequest = new User();
        updateRequest.setAvatarUrl("https://example.com/avatar.png");

        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.updateUser(testUserId, updateRequest);
        });

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void testUpdateLastActive() {
        // Arrange
        LocalDateTime beforeUpdate = testUser.getLastActive();
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.updateLastActive(testUser);

        // Assert
        assertNotNull(result);
        assertNotEquals(beforeUpdate, result.getLastActive());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testDeleteUser() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(keycloakUserService.deleteUser(TEST_KEYCLOAK_ID)).thenReturn(true);
        doNothing().when(userRepository).delete(any(User.class));

        // Act
        assertDoesNotThrow(() -> userService.deleteUser(testUserId));

        // Assert
        verify(keycloakUserService).deleteUser(TEST_KEYCLOAK_ID);
        verify(userRepository).delete(any(User.class));
    }

    @Test
    void testDeleteUser_NotFound() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.deleteUser(testUserId);
        });

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void testFindAll() {
        // Arrange
        List<User> users = Arrays.asList(
                createUser("user1", "user1@test.com", "kc-1"),
                createUser("user2", "user2@test.com", "kc-2")
        );
        when(userRepository.findAll()).thenReturn(users);

        // Act
        List<User> result = userService.findAll();

        // Assert
        assertEquals(2, result.size());
        assertEquals("user1", result.get(0).getUsername());
        assertEquals("user2", result.get(1).getUsername());
    }

    @Test
    void testFindByUsername() {
        // Arrange
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findByUsername(TEST_USERNAME);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(TEST_USERNAME, result.get().getUsername());
    }

    @Test
    void testFindByEmail() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findByEmail(TEST_EMAIL);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(TEST_EMAIL, result.get().getEmail());
    }

    @Test
    void testExistsByUsername() {
        // Arrange
        when(userRepository.existsByUsername(TEST_USERNAME)).thenReturn(true);

        // Act
        boolean result = userService.existsByUsername(TEST_USERNAME);

        // Assert
        assertTrue(result);
    }

    @Test
    void testExistsByEmail() {
        // Arrange
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        // Act
        boolean result = userService.existsByEmail(TEST_EMAIL);

        // Assert
        assertTrue(result);
    }

    @Test
    void testAddXp() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.addXp(testUserId, 150);

        // Assert
        assertNotNull(result);
        assertEquals(150, result.getXp());
        assertEquals(2, result.getLevel()); // Level up should occur
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testAddPoints() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.addPoints(testUserId, 50);

        // Assert
        assertNotNull(result);
        assertEquals(50, result.getPoints());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testFindTopUsersByPoints() {
        // Arrange
        User user1 = createUser("user1", "user1@test.com", "kc-1");
        user1.setPoints(100);
        User user2 = createUser("user2", "user2@test.com", "kc-2");
        user2.setPoints(200);
        User user3 = createUser("user3", "user3@test.com", "kc-3");
        user3.setPoints(50);

        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2, user3));

        // Act
        List<User> result = userService.findTopUsersByPoints(2);

        // Assert
        assertEquals(2, result.size());
        assertEquals("user2", result.get(0).getUsername()); // Highest points
        assertEquals("user1", result.get(1).getUsername());
    }

    private User createUser(String username, String email, String keycloakId) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(email);
        user.setKeycloakId(keycloakId);
        user.setPoints(0);
        user.setXp(0);
        user.setLevel(1);
        return user;
    }
}
