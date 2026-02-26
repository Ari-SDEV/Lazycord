#!/bin/bash

# Lazycord - Local Development Start Script
# Startet alle Services in der richtigen Reihenfolge

set -e

echo "🚀 Lazycord - Local Development"
echo "=================================="

# Prüfe ob .env Datei existiert
if [ ! -f ".env" ]; then
    echo "⚠️  .env Datei nicht gefunden. Erstelle Standard-Konfiguration..."
    cat > .env <<'EOF'
# Keycloak Configuration
KEYCLOAK_CLIENT_SECRET=your-client-secret-here

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/lazycord
SPRING_DATASOURCE_USERNAME=lazycord
SPRING_DATASOURCE_PASSWORD=lazycord

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
EOF
    echo "✅ .env Datei erstellt"
fi

# Lade Umgebungsvariablen
export $(grep -v '^#' .env | xargs)

echo ""
echo "📋 Schritt 1: Docker Services starten..."
echo "----------------------------------------"

# Stoppe vorhandene Container
echo "🛑 Stoppe vorhandene Container..."
docker-compose down > /dev/null 2>&1 || true

# Starte Infrastruktur (Postgres, Redis, Keycloak)
echo "🐳 Starte PostgreSQL, Redis und Keycloak..."
docker-compose up -d postgres redis keycloak

echo ""
echo "⏳ Warte auf Services..."
echo "----------------------------------------"

# Warte auf PostgreSQL
echo "🐘 Warte auf PostgreSQL..."
until docker exec lazycord-postgres pg_isready -U lazycord > /dev/null 2>&1; do
    sleep 2
    echo "   noch warten..."
done
echo "✅ PostgreSQL bereit"

# Warte auf Redis
echo "🔴 Warte auf Redis..."
until docker exec lazycord-redis redis-cli ping | grep -q "PONG"; do
    sleep 2
    echo "   noch warten..."
done
echo "✅ Redis bereit"

# Warte auf Keycloak
echo "🔐 Warte auf Keycloak..."
until curl -s http://localhost:8081/health/ready > /dev/null 2>&1; do
    sleep 3
    echo "   noch warten..."
done
echo "✅ Keycloak bereit"

echo ""
echo "📋 Schritt 2: Backend starten..."
echo "----------------------------------------"

# Prüfe ob Maven Wrapper existiert
if [ ! -f "./mvnw" ]; then
    echo "⚠️  Maven Wrapper nicht gefunden. Nutze system Maven..."
    cd backend
    mvn clean compile
    cd ..
else
    echo "🛠️  Kompiliere Backend..."
    cd backend
    ../mvnw clean compile -q
    cd ..
fi

echo "✅ Backend kompiliert"

echo ""
echo "📋 Schritt 3: Frontend starten..."
echo "----------------------------------------"

# Prüfe ob Node.js installiert ist
if ! command -v node > /dev/null 2>&1; then
    echo "❌ Node.js ist nicht installiert. Bitte installiere Node.js 18+."
    exit 1
fi

# Frontend Dependencies prüfen
if [ ! -d "web/node_modules" ]; then
    echo "📦 Installiere Frontend Dependencies..."
    cd web
    npm install
    cd ..
fi

echo "✅ Frontend bereit"

echo ""
echo "======================================"
echo "🎉 Lazycord ist bereit!"
echo "======================================"
echo ""
echo "📍 Services:"
echo "   🌐 Frontend:    http://localhost:3000"
echo "   🔧 Backend:     http://localhost:8080"
echo "   🔐 Keycloak:    http://localhost:8081"
echo "   🐘 PostgreSQL:  localhost:5432"
echo "   🔴 Redis:       localhost:6379"
echo ""
echo "📚 Dokumentation:"
echo "   LOCAL_SETUP.md - Detaillierte Setup-Anleitung"
echo "   TEST_PROTOCOL.md - Testprotokoll"
echo ""
echo "⚡ Schnellstart:"
echo "   cd backend && ./mvnw spring-boot:run  # Backend starten"
echo "   cd web && npm run dev               # Frontend starten"
echo ""
echo "🧹 Datenbank zurücksetzen:"
echo "   ./reset-db.sh"
echo ""
echo "🛑 Alles stoppen:"
echo "   docker-compose down"
echo ""

# Frage ob direkt gestartet werden soll
read -p "Soll Backend und Frontend jetzt gestartet werden? (j/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Jj]$ ]]; then
    echo "🚀 Starte Backend und Frontend..."
    
    # Starte Backend im Hintergrund
    cd backend
    ../mvnw spring-boot:run &
    BACKEND_PID=$!
    cd ..
    
    # Warte kurz
    sleep 5
    
    # Starte Frontend im Hintergrund
    cd web
    npm run dev &
    FRONTEND_PID=$!
    cd ..
    
    echo ""
    echo "✅ Backend (PID: $BACKEND_PID) und Frontend (PID: $FRONTEND_PID) gestartet!"
    echo ""
    echo "🛑 Zum Stoppen:"
    echo "   kill $BACKEND_PID $FRONTEND_PID"
    echo "   docker-compose down"
    echo ""
    echo "Drücke ENTER zum beenden (Services laufen weiter)..."
    read
fi

echo "👋 Fertig!"
