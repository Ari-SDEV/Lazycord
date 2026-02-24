# Lazycord - Features Dokumentation

Dieses Dokument beschreibt alle Features des Lazycord Projekts.

---

## Inhaltsverzeichnis

1. [Projektübersicht](#projektübersicht)
2. [Epic #1-2: Authentifizierung](#epic-1-2-authentifizierung)
3. [Epic #3-4: Chat System](#epic-3-4-chat-system)
4. [Epic #5-6: Gamification](#epic-5-6-gamification)
5. [Epic #7: File Sharing](#epic-7-file-sharing)
6. [Epic #8: Push Notifications](#epic-8-push-notifications)
7. [Epic #9: Moderation Tools](#epic-9-moderation-tools)
8. [Technische Architektur](#technische-architektur)
9. [API Endpunkte](#api-endpunkte)

---

## Projektübersicht

**Lazycord** ist eine Chat-Anwendung mit Gamification-Elementen, ähnlich wie Discord, mit folgenden Hauptfeatures:

- Echtzeit-Chat mit WebSocket
- Benutzer-Authentifizierung via Keycloak
- XP/Level-System mit Missionen
- File-Sharing
- Push Notifications
- Moderation Tools

---

## Epic #1-2: Authentifizierung

### Features
- **Registrierung**: Benutzer können sich mit Username, Email und Passwort registrieren
- **Login**: JWT-basierte Authentifizierung
- **Token Refresh**: Automatisches Token-Refresh
- **Keycloak Integration**: User-Management via Keycloak

### Technische Details
- **Backend**: Spring Security mit OAuth2 Resource Server
- **Token**: JWT Tokens mit Access- und Refresh-Token
- **Speicher**: Tokens in localStorage (Frontend)

### Code-Locations
```
Backend:
- src/main/java/com/lazycord/controller/AuthController.java
- src/main/java/com/lazycord/service/KeycloakTokenService.java
- src/main/java/com/lazycord/security/JwtAuthFilter.java

Frontend:
- web/src/stores/authStore.ts
- web/src/components/Login.tsx
```

### API Endpunkte
```
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
GET  /api/auth/me
POST /api/auth/logout
```

---

## Epic #3-4: Chat System

### Features
- **Channel erstellen**: Öffentliche, private und Direkt-Message-Channel
- **Nachrichten senden**: Textnachrichten in Echtzeit
- **WebSocket**: STOMP over WebSocket für Echtzeit-Updates
- **Channel-Liste**: Übersicht aller verfügbaren Channel
- **Mitglieder**: Channel beitreten/verlassen

### Technische Details
- **WebSocket**: Spring WebSocket mit STOMP
- **Broker**: SimpleBroker für Topic-Queues
- **Endpoint**: /ws/chat

### Code-Locations
```
Backend:
- src/main/java/com/lazycord/model/Channel.java
- src/main/java/com/lazycord/model/Message.java
- src/main/java/com/lazycord/model/ChannelMember.java
- src/main/java/com/lazycord/config/WebSocketConfig.java
- src/main/java/com/lazycord/controller/ChatWebSocketController.java
- src/main/java/com/lazycord/service/MessageService.java
- src/main/java/com/lazycord/service/ChannelService.java

Frontend:
- web/src/components/Chat.tsx
- web/src/stores/chatStore.ts
```

### API Endpunkte
```
WebSocket:
/app/chat.send
/app/chat.join
/app/chat.leave
/topic/channel/{channelId}

REST:
GET    /api/channels
POST   /api/channels
GET    /api/channels/{id}/join
GET    /api/channels/{id}/leave
GET    /api/messages/channel/{channelId}
```

---

## Epic #5-6: Gamification

### Features
- **XP System**: Punkte für Aktivitäten
- **Level-Up**: 20 Levels mit steigenden XP-Anforderungen
- **Ränge**: 8 Ränge (Newbie → Legend), dynamisch aus Datenbank
- **Missionen**: Daily, Weekly, Monthly, One-Time, Achievement
- **Shop**: Kauf von Avatar Frames, Badges, Themes, Titles
- **Inventory**: Ausrüsten von gekauften Items

### Technische Details
- **XP Levels**: 0, 100, 250, 500, 1000, 2000, 3500, 5500, 8000, 11000, 15000, 20000, 26000, 33000, 41000, 50000, 60000, 72000, 85000, 100000
- **Ränge**: Konfigurierbar in Datenbank (minLevel, maxLevel)
- **Missionen**: Zeitlich begrenzt oder permanent

### Code-Locations
```
Backend:
- src/main/java/com/lazycord/model/Mission.java
- src/main/java/com/lazycord/model/MissionProgress.java
- src/main/java/com/lazycord/model/ShopItem.java
- src/main/java/com/lazycord/model/UserInventory.java
- src/main/java/com/lazycord/model/Rank.java
- src/main/java/com/lazycord/service/GamificationService.java
- src/main/java/com/lazycord/service/MissionService.java
- src/main/java/com/lazycord/service/ShopService.java
- src/main/java/com/lazycord/service/RankService.java
- src/main/java/com/lazycord/controller/MissionController.java
- src/main/java/com/lazycord/controller/ShopController.java
- src/main/java/com/lazycord/controller/RankController.java

Frontend:
- web/src/components/Missions.tsx
- web/src/components/Shop.tsx
- web/src/stores/notificationStore.ts (für Mission-Notifications)
```

### API Endpunkte
```
Gamification:
GET    /api/missions
GET    /api/missions/my
POST   /api/missions/{id}/start
POST   /api/missions/{id}/claim

Shop:
GET    /api/shop/items
GET    /api/shop/inventory
POST   /api/shop/items/{id}/purchase
POST   /api/shop/items/{id}/equip
POST   /api/shop/items/{id}/unequip

Ranks:
GET    /api/ranks
GET    /api/ranks/{id}
GET    /api/ranks/level/{level}
POST   /api/ranks (Admin)
PUT    /api/ranks/{id} (Admin)
DELETE /api/ranks/{id} (Admin)
```

---

## Epic #7: File Sharing

### Features
- **Datei-Upload**: Bis 10MB (JPG, PNG, GIF, PDF, TXT, ZIP)
- **Drag & Drop**: Einfaches Hochladen via Drag & Drop
- **Deduplizierung**: SHA-256 Hash für automatische Deduplizierung
- **Bild-Vorschau**: Direkte Anzeige von Bildern im Chat
- **Download**: Download-Links für alle Dateitypen
- **Soft Delete**: Dateien werden markiert, nicht physisch gelöscht

### Technische Details
- **Speicher**: Lokales Dateisystem (/app/uploads)
- **Hashing**: SHA-256 für Datei-Deduplizierung
- **Validierung**: MIME-Type und Dateigrößen-Prüfung
- **Docker**: Upload-Volume für Persistenz

### Code-Locations
```
Backend:
- src/main/java/com/lazycord/model/FileAttachment.java
- src/main/java/com/lazycord/service/FileStorageService.java
- src/main/java/com/lazycord/service/FileAttachmentService.java
- src/main/java/com/lazycord/controller/FileController.java

Frontend:
- web/src/components/FileUpload.tsx
- web/src/components/Chat.tsx (Attachment-Display)
```

### API Endpunkte
```
POST   /api/files/upload
GET    /api/files/{fileId}
GET    /api/files/{fileId}/preview
GET    /api/files/channel/{channelId}
GET    /api/files/my
GET    /api/files/storage
DELETE /api/files/{fileId}
```

---

## Epic #8: Push Notifications

### Features
- **Echtzeit**: Notifications via WebSocket
- **Typen**: Mention, Message, Mission Complete, Level Up, System
- **Badge**: Unread-Count im UI
- **History**: Alle Notifications speicherbar
- **Mark as Read**: Einzeln oder alle auf einmal

### Technische Details
- **WebSocket**: /user/queue/notifications
- **Storage**: PostgreSQL
- **Real-time**: Spring SimpMessagingTemplate

### Code-Locations
```
Backend:
- src/main/java/com/lazycord/model/Notification.java
- src/main/java/com/lazycord/service/NotificationService.java
- src/main/java/com/lazycord/controller/NotificationController.java

Frontend:
- web/src/components/NotificationBell.tsx
- web/src/stores/notificationStore.ts
```

### API Endpunkte
```
GET    /api/notifications
GET    /api/notifications/unread
GET    /api/notifications/count
POST   /api/notifications/{id}/read
POST   /api/notifications/read-all
DELETE /api/notifications/{id}
```

---

## Epic #9: Moderation Tools

### Features
- **Ban**: Temporär (1h, 1d, 1w) oder permanent
- **Mute**: Temporär oder permanent
- **Unban/Unmute**: Mit Begründung
- **Reports**: Nutzer können andere melden (Spam, Harassment, etc.)
- **Listen**: Aktive Bans/Mutes pro Channel

### Technische Details
- **Ban**: User wird aus Channel entfernt und kann nicht beitreten
- **Mute**: User kann Nachrichten nicht senden
- **Expiration**: Automatische Überprüfung bei Ablauf
- **Berechtigungen**: Nur Owner/Admins können moderieren

### Code-Locations
```
Backend:
- src/main/java/com/lazycord/model/ChannelBan.java
- src/main/java/com/lazycord/model/ChannelMute.java
- src/main/java/com/lazycord/model/Report.java
- src/main/java/com/lazycord/service/ModerationService.java
- src/main/java/com/lazycord/service/ReportService.java
- src/main/java/com/lazycord/controller/ModerationController.java
```

### API Endpunkte
```
Moderation:
POST   /api/moderation/channels/{id}/ban
DELETE /api/moderation/channels/{id}/ban/{userId}
POST   /api/moderation/channels/{id}/mute
DELETE /api/moderation/channels/{id}/mute/{userId}
GET    /api/moderation/channels/{id}/bans
GET    /api/moderation/channels/{id}/mutes
POST   /api/moderation/reports
```

---

## Technische Architektur

### Tech Stack
- **Backend**: Spring Boot 3.4, Java 21
- **Frontend**: React 18, TypeScript, Vite
- **Datenbank**: PostgreSQL 17
- **Cache**: Redis 7
- **Auth**: Keycloak 24
- **Real-time**: WebSocket (STOMP)

### Verzeichnisstruktur
```
Lazycord/
├── backend/                    # Spring Boot Backend
│   src/main/java/com/lazycord/
│   ├── config/              # Konfiguration (Security, WebSocket)
│   ├── controller/          # REST APIs
│   ├── model/                # Entities
│   ├── repository/           # JPA Repositories
│   ├── service/              # Business Logic
│   └── security/             # JWT Filter, etc.
│
├── web/                       # React Frontend
│   src/
│   ├── components/           # React Components
│   ├── stores/              # Zustand Stores
│   └── types/               # TypeScript Types
│
└── docker-compose.yml         # Local Deployment
```

### Datenbank-Schema
- **Users**: Core User Daten
- **Channels, Messages, ChannelMembers**: Chat
- **Missions, MissionProgress**: Gamification
- **ShopItems, UserInventory**: Shop
- **Ranks**: Dynamische Ränge
- **FileAttachments**: Datei-Meta-Daten
- **Notifications**: Push Notifications
- **ChannelBans, ChannelMutes, Reports**: Moderation

---

## Lokale Entwicklung

### Starten
```bash
./start-local.sh
```

### URLs
- Frontend: http://localhost:3000
- Backend: http://localhost:8080
- Keycloak: http://localhost:8081 (admin/admin)

### Default User
- admin/admin123 (Admin)
- moderator/mod123 (Moderator)
- user/user123 (User)

---

## Zusammenfassung

Alle 9 Epics sind implementiert und auf main gemerged:

1. ✅ Projekt Setup
2. ✅ Authentifizierung
3. ✅ WebSocket Chat Backend
4. ✅ Frontend Chat UI
5. ✅ Gamification System
6. ✅ Frontend Gamification
7. ✅ File Sharing
8. ✅ Push Notifications
9. ✅ Moderation Tools

**Status**: Produktionsbereit für Testing
