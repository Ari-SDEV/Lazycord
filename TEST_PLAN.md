# Lazycord - Comprehensive Test Plan

## Overview
This document outlines the complete testing strategy for all Lazycord Epics.

---

## Test Categories

### 1. Unit Tests
- Location: `backend/src/test/java/com/lazycord/`
- Framework: JUnit 5 + Mockito
- Coverage Target: 80%+

### 2. Integration Tests
- Location: `backend/src/test/java/com/lazycord/integration/`
- Framework: Spring Boot Test + TestContainers
- Database: PostgreSQL (Docker)

### 3. E2E Tests
- Location: `web/e2e/`
- Framework: Playwright
- Browser: Chromium, Firefox, WebKit

---

## Epic Test Coverage

### Epic #1-2: Authentication
**Test Classes:**
- `AuthControllerTest.java`
- `UserServiceTest.java`
- `JwtAuthFilterTest.java`

**Test Cases:**
- [x] User registration with valid data
- [x] User registration with duplicate username
- [x] Login with valid credentials
- [x] Login with invalid credentials
- [x] Token refresh
- [x] Logout
- [x] Access protected endpoint without token
- [x] Access protected endpoint with expired token

### Epic #3-4: Chat System
**Test Classes:**
- `ChannelServiceTest.java`
- `MessageServiceTest.java`
- `ChatWebSocketControllerTest.java`
- `ChannelRepositoryTest.java`

**Test Cases:**
- [x] Create channel
- [x] Join channel
- [x] Leave channel
- [x] Send message
- [x] Receive message via WebSocket
- [x] Load message history
- [x] Delete message (owner/admin)
- [x] Cannot delete others' messages

### Epic #5-6: Gamification
**Test Classes:**
- `GamificationServiceTest.java`
- `MissionServiceTest.java`
- `ShopServiceTest.java`
- `ChannelServiceTest.java`

**Test Cases:**
- [x] XP calculation and level up
- [x] Points addition and deduction
- [x] Mission completion
- [x] Mission reward claiming
- [x] Shop purchase
- [x] Item equip/unequip
- [x] Insufficient points handling

### Epic #7: File Sharing
**Test Classes:**
- `FileAttachmentServiceTest.java`
- `FileStorageServiceTest.java`
- `FileControllerTest.java`

**Test Cases:**
- [x] Upload valid image file
- [x] Upload valid document file
- [x] Upload oversized file (rejection)
- [x] Upload invalid file type (rejection)
- [x] File deduplication
- [x] Download file
- [x] Delete own file
- [x] Cannot delete others' files
- [x] Image preview generation

### Epic #8: Notifications
**Test Classes:**
- `NotificationServiceTest.java`
- `NotificationControllerTest.java`

**Test Cases:**
- [x] Mention notification created
- [x] Message notification created
- [x] Mission complete notification
- [x] Level up notification
- [x] WebSocket notification delivery
- [x] Mark as read
- [x] Mark all as read
- [x] Unread count calculation

### Epic #9: Moderation
**Test Classes:**
- `ModerationServiceTest.java`
- `ModerationControllerTest.java`

**Test Cases:**
- [x] Ban user (temporary)
- [x] Ban user (permanent)
- [x] Mute user (temporary)
- [x] Mute user (permanent)
- [x] Unban user
- [x] Unmute user
- [x] Muted user cannot send messages
- [x] Banned user cannot join channel
- [x] Create report
- [x] Resolve report
- [x] List active bans/mutes

---

## Running Tests

### Backend Tests
```bash
cd backend
./mvnw test
```

### With Coverage
```bash
./mvnw test jacoco:report
# Report: target/site/jacoco/index.html
```

### Integration Tests
```bash
./mvnw verify -P integration-tests
```

### Frontend Tests
```bash
cd web
npm test
```

### E2E Tests
```bash
cd web
npm run test:e2e
```

---

## CI/CD Integration

Tests run automatically on:
- Pull Request creation
- Push to main
- Nightly builds

Failed tests block merge.
