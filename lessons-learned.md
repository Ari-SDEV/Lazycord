# Lessons Learned - Lazycord Project

## Keycloak Integration

### Version Compatibility
- **Keycloak 26.x** ist NICHT kompatibel mit Spring Boot 3.4
- **Keycloak 24.x** funktioniert mit Spring Boot 3.4
- BOM (Bill of Materials) für Keycloak verwenden, um Versionskonflikte zu vermeiden

### Häufige Keycloak-Fehler

#### 1. `getSubject()` nicht gefunden
**Problem:** `AccessTokenResponse.getSubject()` existiert nicht in Keycloak 24
**Lösung:** Token manuell parsen oder aus dem JWT Payload extrahieren
```java
// Statt:
String keycloakId = tokenResponse.getSubject();

// Besser:
String keycloakId = extractUserIdFromToken(tokenResponse.getToken());
// JWT parsen: split by ".", Base64 decode payload, "sub" claim extrahieren
```

#### 2. `refreshToken()` Methode nicht gefunden
**Problem:** `KeycloakBuilder.refreshToken()` oder `tokenManager.refreshToken()` existiert nicht
**Lösung:** Direkter REST-Call an Keycloak Token Endpoint
```java
// Statt:
Keycloak keycloak = KeycloakBuilder.builder()...refreshToken(refreshToken).build();
AccessTokenResponse response = keycloak.tokenManager().refreshToken();

// Besser:
HttpClient client = HttpClient.newHttpClient();
String formData = "grant_type=refresh_token&client_id=" + clientId + "&refresh_token=" + refreshToken;
// POST an /realms/{realm}/protocol/openid-connect/token
```

#### 3. `setRefreshTokenLifespan()` nicht gefunden
**Problem:** `RealmRepresentation.setRefreshTokenLifespan()` existiert nicht in Keycloak 24
**Lösung:** Entweder entfernen oder `setSsoSessionIdleTimeout()` verwenden
```java
// Statt:
realm.setRefreshTokenLifespan(1800);

// Besser:
// Entweder ganz entfernen (Keycloak Defaults verwenden)
// Oder: realm.setSsoSessionIdleTimeout(1800);
```

### Keycloak Dependencies
```xml
<properties>
    <keycloak.version>24.0.0</keycloak.version>
</properties>

<dependencies>
    <!-- Keycloak Admin Client -->
    <dependency>
        <groupId>org.keycloak</groupId>
        <artifactId>keycloak-admin-client</artifactId>
        <version>${keycloak.version}</version>
    </dependency>
    
    <!-- JAX-RS API für Keycloak -->
    <dependency>
        <groupId>jakarta.ws.rs</groupId>
        <artifactId>jakarta.ws.rs-api</artifactId>
        <version>3.1.0</version>
    </dependency>
    
    <!-- RESTEasy Client (benötigt für Keycloak Admin Client) -->
    <dependency>
        <groupId>org.jboss.resteasy</groupId>
        <artifactId>resteasy-client</artifactId>
        <version>6.2.7.Final</version>
    </dependency>
    <dependency>
        <groupId>org.jboss.resteasy</groupId>
        <artifactId>resteasy-jackson2-provider</artifactId>
        <version>6.2.7.Final</version>
    </dependency>
</dependencies>
```

## Maven Build

### Häufige Fehler

#### 1. `package javax.ws.rs.core does not exist`
**Ursache:** Keycloak Admin Client braucht JAX-RS Implementation
**Lösung:** `resteasy-client` und `jakarta.ws.rs-api` hinzufügen

#### 2. `package org.springframework.security.oauth2.jwt does not exist`
**Ursache:** OAuth2 Resource Server fehlt
**Lösung:** `spring-boot-starter-oauth2-resource-server` hinzufügen

#### 3. `cannot find symbol` für Keycloak Methoden
**Ursache:** API-Änderungen zwischen Keycloak Versionen
**Lösung:** 
- Auf stabile Version (24.x) downgraden
- Oder: REST-Calls statt Admin Client verwenden

## CI/CD

### GitHub Actions

#### Discord Notification
- Webhook URL direkt im Workflow (kein Secret nötig für öffentliche Repos)
- `if: always()` verwenden, damit Notification bei Failure/Success kommt
- Embed-Format für schöne Darstellung

#### Maven Wrapper
- `mvnw` kann kaputt sein (404 Fehler)
- Fallback: `mvn` direkt verwenden (ist in ubuntu-latest vorinstalliert)

## Spring Boot

### Security Config
- **OAuth2 Resource Server** für JWT Validation
- **Keycloak Adapter** NICHT verwenden (deprecated in SB3)
- Eigenen `JwtAuthFilter` für Token Parsing

### Best Practices
1. Keycloak Admin Client nur für Init/Setup verwenden
2. Für Auth-Flows direkt REST-Calls verwenden (stabiler)
3. JWT Parsing selbst implementieren (weniger Dependencies)

## Debugging

### Build-Fehler analysieren
1. Erste Fehlermeldung ist meist die wichtigste
2. `cannot find symbol` = API-Änderung oder fehlende Dependency
3. `package ... does not exist` = Dependency fehlt im pom.xml

### Logs lesen
- GitHub Actions Logs sind nicht öffentlich zugänglich
- Lokal bauen: `mvn clean compile` für schnelles Feedback
- Dependencies prüfen: `mvn dependency:tree`

## Nächste Schritte für stabiles Keycloak

1. **Option A:** Keycloak Admin Client komplett entfernen, direkt REST API verwenden
2. **Option B:** Keycloak 24.x mit bekannten Workarounds verwenden
3. **Option C:** Spring Security OAuth2 nur mit JWT (ohne Keycloak Client Lib)

Empfohlen: **Option C** für weniger Dependencies und mehr Kontrolle.
