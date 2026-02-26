#!/bin/bash

# Lazycord - Reset Database Script
# Dieses Script setzt die PostgreSQL Datenbank zurück und entfernt alle Flyway-Migrationen

set -e

echo "🗑️  Lazycord Datenbank Reset"
echo "=============================="

# Prüfe ob Docker läuft
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker läuft nicht. Bitte starte Docker Desktop."
    exit 1
fi

# Container Name
POSTGRES_CONTAINER="lazycord-postgres"

# Prüfe ob Container läuft
if ! docker ps | grep -q "$POSTGRES_CONTAINER"; then
    echo "⚠️  PostgreSQL Container läuft nicht. Starte docker-compose..."
    docker-compose up -d postgres
    echo "⏳ Warte auf PostgreSQL..."
    sleep 5
fi

echo "🧹 Lösche alle Tabellen und Flyway History..."

# Führe SQL aus um alles zu löschen
docker exec -i "$POSTGRES_CONTAINER" psql -U lazycord -d lazycord <<'EOF'
-- Alle Tabellen löschen (außer in Flyway Schema)
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

-- Bestätigung
SELECT 'Datenbank wurde zurückgesetzt' as status;
EOF

echo "✅ Datenbank erfolgreich zurückgesetzt!"
echo ""
echo "🚀 Du kannst jetzt die Anwendung starten:"
echo "   ./start-local.sh"
