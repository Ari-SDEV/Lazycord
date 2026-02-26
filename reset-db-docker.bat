@echo off
echo 🗑️  Lazycord Datenbank Reset (Docker)
echo ======================================

set POSTGRES_CONTAINER=lazycord-postgres

REM Prüfe ob Container läuft
docker ps | findstr "%POSTGRES_CONTAINER%" > nul
if errorlevel 1 (
    echo ⚠️  PostgreSQL Container läuft nicht.
    echo    Starte: docker compose up -d postgres
    docker compose up -d postgres
    echo ⏳ Warte auf PostgreSQL...
    timeout /t 5 > nul
)

echo 🧹 Setze Datenbank zurück...

REM Führe SQL aus
docker exec %POSTGRES_CONTAINER% psql -U postgres -d postgres -c "SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = 'lazycord' AND pid <> pg_backend_pid();"
docker exec %POSTGRES_CONTAINER% psql -U postgres -d postgres -c "DROP DATABASE IF EXISTS lazycord;"
docker exec %POSTGRES_CONTAINER% psql -U postgres -d postgres -c "CREATE DATABASE lazycord OWNER lazycord;"
docker exec %POSTGRES_CONTAINER% psql -U postgres -d postgres -c "SELECT 'Datenbank zurückgesetzt' as status;"

echo.
echo ✅ Datenbank erfolgreich zurückgesetzt!
echo.
echo 🚀 Starte jetzt die Anwendung neu:
echo    docker compose restart backend
echo    # oder:
echo    docker compose up -d --build
echo.
pause
