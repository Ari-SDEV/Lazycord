# Lazycord - Local Development Setup

Complete local deployment guide for Lazycord with Docker Compose.

## Prerequisites

- Docker & Docker Compose
- Git
- 4GB+ RAM available for Docker

## Quick Start

### 1. Clone and Navigate
```bash
git clone https://github.com/Ari-SDEV/Lazycord.git
cd Lazycord
```

### 2. Start Everything
```bash
./start-local.sh
```

This script will:
- Build all services (backend, frontend, databases)
- Wait for health checks
- Initialize Keycloak with default realm and users
- Display access URLs

### 3. Access the Application

| Service | URL | Credentials |
|---------|-----|-------------|
| Frontend | http://localhost:3000 | - |
| Backend API | http://localhost:8080 | - |
| Keycloak Admin | http://localhost:8081 | admin/admin |
| PostgreSQL | localhost:5432 | lazycord/lazycord |
| Redis | localhost:6379 | - |

### Default Users

| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | admin |
| moderator | mod123 | moderator |
| user | user123 | user |

## Manual Start (Alternative)

If you prefer manual control:

```bash
# Build and start
docker-compose up --build -d

# View logs
docker-compose logs -f

# Stop
docker-compose down -v
```

## Environment Variables

Copy `.env.example` to `.env` and customize:

```bash
cp .env.example .env
```

| Variable | Default | Description |
|----------|---------|-------------|
| POSTGRES_USER | lazycord | Database user |
| POSTGRES_PASSWORD | lazycord | Database password |
| KEYCLOAK_ADMIN | admin | Keycloak admin user |
| KEYCLOAK_ADMIN_PASSWORD | admin | Keycloak admin password |
| VITE_API_URL | http://localhost:8080 | Backend API URL |

## Services Architecture

```
┌─────────────────┐
│   Frontend      │ http://localhost:3000
│   (React + Vite)│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Backend       │ http://localhost:8080
│   (Spring Boot) │
└────────┬────────┘
         │
    ┌────┴────┬────────┐
    ▼         ▼        ▼
┌────────┐ ┌──────┐ ┌──────────┐
│PostgreSQL│ │ Redis │ │ Keycloak  │
│:5432    │ │:6379  │ │:8081     │
└────────┘ └──────┘ └──────────┘
```

## Development Workflow

### Backend Changes
```bash
# Rebuild after changes
docker-compose up -d --build backend
```

### Frontend Changes
```bash
# Rebuild after changes
docker-compose up -d --build frontend
```

### Database Migrations
Migrations run automatically on startup. To reset:
```bash
docker-compose down -v  # Removes volumes
docker-compose up -d     # Recreates everything
```

## Troubleshooting

### Services not starting
```bash
# Check logs
docker-compose logs -f [service-name]

# Restart specific service
docker-compose restart backend
```

### Port conflicts
Change ports in `docker-compose.yml` if needed:
```yaml
ports:
  - "8082:8080"  # Different host port
```

### Keycloak initialization fails
```bash
# Manual initialization
docker-compose exec backend curl http://keycloak:8080/health
docker-compose restart backend
```

### Reset everything
```bash
docker-compose down -v  # Removes all data
docker-compose up --build -d
```

## Health Checks

All services include health checks:
- PostgreSQL: `pg_isready`
- Redis: `redis-cli ping`
- Backend: `/actuator/health`
- Keycloak: Built-in health endpoint

## API Documentation

Once running, access:
- Swagger UI: http://localhost:8080/swagger-ui.html (if enabled)
- Actuator: http://localhost:8080/actuator

## Production Deployment

For production:
1. Change default passwords
2. Use external PostgreSQL/Redis
3. Configure SSL/TLS
4. Set up proper secrets management
5. Use production Keycloak configuration

## Support

For issues:
1. Check logs: `docker-compose logs`
2. Verify health: `docker-compose ps`
3. Review configuration in `.env`
