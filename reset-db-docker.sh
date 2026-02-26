#!/bin/bash

# Lazycord - Complete Database Reset Script for Docker
# Dieses Script setzt die PostgreSQL Datenbank komplett zurück

set -e

echo "🗑️  Lazycord Datenbank Reset (Docker)"
echo "======================================"

# Container Namen
POSTGRES_CONTAINER="lazycord-postgres"

# Prüfe ob Container läuft
if ! docker ps | grep -q "$POSTGRES_CONTAINER"; then
    echo "⚠️  PostgreSQL Container läuft nicht."
    echo "   Starte: docker compose up -d postgres"
    exit 1
fi

echo "🧹 Lösche komplette Datenbank 'lazycord'..."

# Lösche die komplette Datenbank und erstelle sie neu
docker exec -i "$POSTGRES_CONTAINER" psql -U postgres -d postgres <<'EOF'
-- Alle Verbindungen zur lazycord DB beenden
SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname = 'lazycord'
  AND pid <> pg_backend_pid();

-- Datenbank löschen
DROP DATABASE IF EXISTS lazycord;

-- Datenbank neu erstellen
CREATE DATABASE lazycord OWNER lazycord;

-- Bestätigung
SELECT 'Datenbank lazycord wurde zurückgesetzt und neu erstellt' as status;
EOF

echo ""
echo "✅ Datenbank erfolgreich zurückgesetzt!"
echo ""
echo "🚀 Starte jetzt die Anwendung neu:"
echo "   docker compose restart backend"
echo "   # oder:"
echo "   docker compose up -d"
echo ""
