#!/bin/bash
# Lazycord Local Startup Script

set -e

echo "🚀 Starting Lazycord locally..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Create .env if it doesn't exist
if [ ! -f .env ]; then
    echo "📝 Creating .env file from .env.example..."
    cp .env.example .env
fi

# Build and start services
echo "🔨 Building and starting services..."
docker-compose down -v 2>/dev/null || true
docker-compose up --build -d

echo ""
echo "⏳ Waiting for services to be ready..."
sleep 10

# Check health
echo "🔍 Checking service health..."

# Wait for PostgreSQL
until docker-compose exec -T postgres pg_isready -U lazycord > /dev/null 2>&1; do
    echo "   Waiting for PostgreSQL..."
    sleep 2
done
echo "✅ PostgreSQL is ready"

# Wait for Redis
until docker-compose exec -T redis redis-cli ping > /dev/null 2>&1; do
    echo "   Waiting for Redis..."
    sleep 2
done
echo "✅ Redis is ready"

# Wait for Keycloak
until curl -s http://localhost:8081/health > /dev/null 2>&1; do
    echo "   Waiting for Keycloak..."
    sleep 5
done
echo "✅ Keycloak is ready"

# Wait for Backend
echo "   Waiting for Backend..."
sleep 30

# Initialize Keycloak realm and users
echo "🔧 Initializing Keycloak..."
docker-compose exec backend java -cp /app.jar com.lazycord.service.KeycloakInitService || true

echo ""
echo "✅ Lazycord is ready!"
echo ""
echo "📱 Frontend: http://localhost:3000"
echo "🔌 Backend API: http://localhost:8080"
echo "🔐 Keycloak Admin: http://localhost:8081 (admin/admin)"
echo ""
echo "📋 Useful commands:"
echo "   docker-compose logs -f backend    # View backend logs"
echo "   docker-compose logs -f frontend   # View frontend logs"
echo "   docker-compose down -v            # Stop and remove volumes"
echo ""
