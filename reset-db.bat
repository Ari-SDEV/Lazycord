@echo off
chcp 65001 > nul
echo 🗑️  Lazycord Datenbank Reset (Windows)
echo ========================================

REM Prüfe ob Docker läuft
docker info > nul 2>&1
if errorlevel 1 (
    echo ❌ Docker läuft nicht. Bitte starte Docker Desktop.
    pause
    exit /b 1
)

set POSTGRES_CONTAINER=lazycord-postgres

REM Prüfe ob Container läuft
docker ps | findstr "%POSTGRES_CONTAINER%" > nul
if errorlevel 1 (
    echo ⚠️  PostgreSQL Container läuft nicht. Starte docker-compose...
    docker-compose up -d postgres
    echo ⏳ Warte auf PostgreSQL...
    timeout /t 5 > nul
)

echo 🧹 Setze Datenbank zurück...

REM SQL Befehle einzeln ausführen
docker exec %POSTGRES_CONTAINER% psql -U postgres -d postgres -c "SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = 'lazycord' AND pid <> pg_backend_pid();"
docker exec %POSTGRES_CONTAINER% psql -U postgres -d postgres -c "DROP DATABASE IF EXISTS lazycord;"
docker exec %POSTGRES_CONTAINER% psql -U postgres -d postgres -c "CREATE DATABASE lazycord OWNER lazycord;"
docker exec %POSTGRES_CONTAINER% psql -U lazycord -d lazycord -c "SELECT 'Datenbank wurde zurückgesetzt' as status;"

echo.
echo ✅ Datenbank erfolgreich zurückgesetzt!
echo.
echo 🚀 Du kannst jetzt die Anwendung starten:
echo    docker compose up -d --build
echo.
pause
