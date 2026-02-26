@echo off
chcp 65001 > nul
setlocal EnableDelayedExpansion

echo 🚀 Lazycord - Local Development (Windows)
echo =========================================

REM Prüfe ob .env existiert
if not exist ".env" (
    echo ⚠️  .env Datei nicht gefunden. Erstelle Standard-Konfiguration...
    (
        echo # Keycloak Configuration
        echo KEYCLOAK_CLIENT_SECRET=your-client-secret-here
        echo.
        echo # Database
        echo SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/lazycord
        echo SPRING_DATASOURCE_USERNAME=lazycord
        echo SPRING_DATASOURCE_PASSWORD=lazycord
        echo.
        echo # Redis
        echo SPRING_REDIS_HOST=localhost
        echo SPRING_REDIS_PORT=6379
    ) > .env
    echo ✅ .env Datei erstellt
)

echo.
echo 📋 Schritt 1: Docker Services starten...
echo ----------------------------------------

REM Stoppe vorhandene Container
echo 🛑 Stoppe vorhandene Container...
docker-compose down > nul 2>&1

REM Starte Infrastruktur
echo 🐳 Starte PostgreSQL, Redis und Keycloak...
docker-compose up -d postgres redis keycloak

echo.
echo ⏳ Warte auf Services...
echo ----------------------------------------

REM Warte auf PostgreSQL
echo 🐘 Warte auf PostgreSQL...
:wait_postgres
docker exec lazycord-postgres pg_isready -U lazycord > nul 2>&1
if errorlevel 1 (
    timeout /t 2 > nul
    echo    noch warten...
    goto wait_postgres
)
echo ✅ PostgreSQL bereit

REM Warte auf Redis
echo 🔴 Warte auf Redis...
:wait_redis
docker exec lazycord-redis redis-cli ping | findstr "PONG" > nul
if errorlevel 1 (
    timeout /t 2 > nul
    echo    noch warten...
    goto wait_redis
)
echo ✅ Redis bereit

REM Warte auf Keycloak
echo 🔐 Warte auf Keycloak (dies kann 1-2 Minuten dauern)...
:wait_keycloak
curl -s http://localhost:8081/health/ready > nul 2>&1
if errorlevel 1 (
    timeout /t 3 > nul
    echo    noch warten...
    goto wait_keycloak
)
echo ✅ Keycloak bereit

echo.
echo ======================================
echo 🎉 Lazycord Services sind bereit!
echo ======================================
echo.
echo 📍 Services:
echo    🌐 Frontend:    http://localhost:3000
echo    🔧 Backend:     http://localhost:8080
echo    🔐 Keycloak:    http://localhost:8081
echo    🐘 PostgreSQL:  localhost:5432
echo    🔴 Redis:       localhost:6379
echo.
echo 📚 Wichtige Befehle:
echo    reset-db.bat  - Datenbank zurücksetzen
echo    docker-compose down  - Alle stoppen
echo.
echo 💡 Tipp: Starte Backend und Frontend manuell:
echo    cd backend ^&^& mvnw spring-boot:run
echo    cd web ^&^& npm run dev
echo.

pause
