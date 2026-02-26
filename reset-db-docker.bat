@echo off
echo 🗑️  Lazycord Datenbank Reset (Docker)
echo ======================================

set POSTGRES_CONTAINER=lazycord-postgres

REM Prüfe ob Container läuft
docker ps | findstr "%POSTGRES_CONTAINER%" > nul
if errorlevel 1 (
    echo ⚠️  PostgreSQL Container läuft nicht.
    echo    Starte: docker compose up -d postgres
    pause
    exit /b 1
)

echo 🧹 Lösche komplette Datenbank 'lazycord'...

docker exec -i %POSTGRES_CONTAINER% psql -U postgres -d postgres <<'EOF'
SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname = 'lazycord'
  AND pid <> pg_backend_pid();

DROP DATABASE IF EXISTS lazycord;

CREATE DATABASE lazycord OWNER lazycord;

SELECT 'Datenbank lazycord wurde zurückgesetzt und neu erstellt' as status;
EOF

echo.
echo ✅ Datenbank erfolgreich zurückgesetzt!
echo.
echo 🚀 Starte jetzt die Anwendung neu:
echo    docker compose restart backend
echo    # oder:
echo    docker compose up -d
echo.
pause
