#!/bin/sh
# wait-for-keycloak.sh
# Waits for Keycloak to be available before starting the backend

set -e

KEYCLOAK_URL="${KEYCLOAK_AUTH_SERVER_URL:-http://keycloak:8080}"
MAX_RETRIES=60
RETRY_INTERVAL=5

echo "Waiting for Keycloak at ${KEYCLOAK_URL}..."

for i in $(seq 1 $MAX_RETRIES); do
    if wget --no-verbose --tries=1 --spider "${KEYCLOAK_URL}/realms/master" 2>/dev/null || \
       wget --no-verbose --tries=1 --spider "${KEYCLOAK_URL}/health/ready" 2>/dev/null; then
        echo "Keycloak is ready!"
        echo "Waiting additional 10 seconds for Keycloak to fully initialize..."
        sleep 10
        echo "Starting backend..."
        exec "$@"
    fi
    
    echo "Keycloak not ready yet (attempt $i/$MAX_RETRIES). Retrying in ${RETRY_INTERVAL}s..."
    sleep $RETRY_INTERVAL
done

echo "Keycloak did not become ready after $MAX_RETRIES attempts. Starting backend anyway..."
exec "$@"
