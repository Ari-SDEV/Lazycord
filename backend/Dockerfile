# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Create wait script inline to avoid line ending issues
RUN echo '#!/bin/sh' > /wait-for-keycloak.sh && \
    echo 'KEYCLOAK_URL="${KEYCLOAK_AUTH_SERVER_URL:-http://keycloak:8080}"' >> /wait-for-keycloak.sh && \
    echo 'MAX_RETRIES=60' >> /wait-for-keycloak.sh && \
    echo 'RETRY_INTERVAL=5' >> /wait-for-keycloak.sh && \
    echo 'echo "Waiting for Keycloak at ${KEYCLOAK_URL}..."' >> /wait-for-keycloak.sh && \
    echo 'for i in $(seq 1 $MAX_RETRIES); do' >> /wait-for-keycloak.sh && \
    echo '  if wget --no-verbose --tries=1 --spider "${KEYCLOAK_URL}/realms/master" 2>/dev/null; then' >> /wait-for-keycloak.sh && \
    echo '    echo "Keycloak is ready!"' >> /wait-for-keycloak.sh && \
    echo '    sleep 5' >> /wait-for-keycloak.sh && \
    echo '    exec "$@"' >> /wait-for-keycloak.sh && \
    echo '  fi' >> /wait-for-keycloak.sh && \
    echo '  echo "Attempt $i/$MAX_RETRIES: Keycloak not ready, waiting ${RETRY_INTERVAL}s..."' >> /wait-for-keycloak.sh && \
    echo '  sleep $RETRY_INTERVAL' >> /wait-for-keycloak.sh && \
    echo 'done' >> /wait-for-keycloak.sh && \
    echo 'echo "Keycloak timeout - starting anyway..."' >> /wait-for-keycloak.sh && \
    echo 'exec "$@"' >> /wait-for-keycloak.sh && \
    chmod +x /wait-for-keycloak.sh && \
    apk add --no-cache wget

EXPOSE 8080
ENTRYPOINT ["/wait-for-keycloak.sh", "java", "-jar", "app.jar"]
