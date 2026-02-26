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
COPY wait-for-keycloak.sh /wait-for-keycloak.sh
RUN chmod +x /wait-for-keycloak.sh && apk add --no-cache wget
EXPOSE 8080
ENTRYPOINT ["/wait-for-keycloak.sh", "java", "-jar", "app.jar"]
