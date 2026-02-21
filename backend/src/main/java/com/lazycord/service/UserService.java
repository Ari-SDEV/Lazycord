package com.lazycord.service;

import com.lazycord.dto.UserRegistrationRequest;
import com.lazycord.model.User;
import com.lazycord.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUserService;

    public UserService(UserRepository userRepository, KeycloakUserService keycloakUserService) {
        this.userRepository = userRepository;
        this.keycloakUserService = keycloakUserService;
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User createUser(UserRegistrationRequest request) {
        // Check if user already exists locally
        if (existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Create user in Keycloak
        String keycloakId = keycloakUserService.createUser(
            request.getUsername(),
            request.getEmail(),
            request.getPassword(),
            List.of("user")
        );

        if (keycloakId == null) {
            throw new RuntimeException("Failed to create user in Keycloak");
        }

        // Create user in local database
        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPoints(0);
        user.setXp(0);
        user.setLevel(1);
        user.setRank("Newbie");

        User savedUser = userRepository.save(user);
        logger.info("User created: {} with ID: {}", request.getUsername(), savedUser.getId());
        
        return savedUser;
    }

    @Transactional
    public User syncUserWithKeycloak(String keycloakId) {
        var keycloakUser = keycloakUserService.getUserById(keycloakId);
        
        if (keycloakUser.isEmpty()) {
            throw new RuntimeException("User not found in Keycloak");
        }

        var kcUser = keycloakUser.get();
        String username = kcUser.getUsername();
        String email = kcUser.getEmail();

        return userRepository.findByKeycloakId(keycloakId)
            .orElseGet(() -> {
                // Create new local user
                User newUser = new User();
                newUser.setKeycloakId(keycloakId);
                newUser.setUsername(username);
                newUser.setEmail(email);
                newUser.setPoints(0);
                newUser.setXp(0);
                newUser.setLevel(1);
                newUser.setRank("Newbie");
                
                logger.info("Synced new user from Keycloak: {}", username);
                return userRepository.save(newUser);
            });
    }

    @Transactional
    public User updateUser(UUID id, User updatedUser) {
        User existingUser = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Only allow updating certain fields locally
        if (updatedUser.getAvatarUrl() != null) {
            existingUser.setAvatarUrl(updatedUser.getAvatarUrl());
        }
        
        // Sync with Keycloak if needed
        if (updatedUser.getEmail() != null && !updatedUser.getEmail().equals(existingUser.getEmail())) {
            keycloakUserService.updateUser(existingUser.getKeycloakId(), updatedUser.getEmail(), null);
            existingUser.setEmail(updatedUser.getEmail());
        }

        return userRepository.save(existingUser);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Delete from Keycloak
        keycloakUserService.deleteUser(user.getKeycloakId());
        
        // Delete from local database
        userRepository.delete(user);
        logger.info("User deleted: {}", id);
    }

    @Transactional
    public User updateLastActive(User user) {
        user.setLastActive(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional
    public void updateLastActiveById(UUID id) {
        userRepository.findById(id).ifPresent(this::updateLastActive);
    }

    @Transactional
    public User addXp(UUID userId, int amount) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.addXp(amount);
        return userRepository.save(user);
    }

    @Transactional
    public User addPoints(UUID userId, int amount) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.addPoints(amount);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> findTopUsersByPoints(int limit) {
        return findAll().stream()
            .sorted((u1, u2) -> Integer.compare(u2.getPoints(), u1.getPoints()))
            .limit(limit)
            .toList();
    }
}
