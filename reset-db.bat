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

echo 🧹 Lösche alle Tabellen und Flyway History...

REM SQL ausführen
docker exec -i %POSTGRES_CONTAINER% psql -U lazycord -d lazycord <<'EOF'
-- Alle Tabellen löschen
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP
        EXECUTE 'DROP TABLE IF EXISTS ' || quote_ident(r.tablename) || ' CASCADE';
    END LOOP;
END $$;

-- Flyway Schema History löschen
DROP TABLE IF EXISTS flyway_schema_history CASCADE;

-- Sequences löschen
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = 'public') LOOP
        EXECUTE 'DROP SEQUENCE IF EXISTS ' || quote_ident(r.sequence_name) || ' CASCADE';
    END LOOP;
END $$;

SELECT 'Datenbank wurde zurückgesetzt' as status;
EOF

echo.
echo ✅ Datenbank erfolgreich zurückgesetzt!
echo.
echo 🚀 Du kannst jetzt die Anwendung starten:
echo    start-local.bat
echo.
pause
