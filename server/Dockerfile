# Build stage
FROM gradle:8.11-jdk21 AS build

WORKDIR /app

# Copy gradle files
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle

# Copy source code
COPY src ./src

# Build the application
RUN gradle shadowJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/build/libs/*-all.jar app.jar

# Copy application configuration
COPY src/main/resources/application.yaml ./application.yaml

# Expose ports
# 8081 - Main HTTP server
# 1234 - MCP server
EXPOSE 8081 1234

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
