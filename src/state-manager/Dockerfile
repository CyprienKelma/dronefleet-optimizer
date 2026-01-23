# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy Gradle wrapper and dependencies
COPY gradle gradle
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build application
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy JAR from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose port (Cloud Run uses PORT env var, default 8080)
EXPOSE 8080

# Note: Cloud Run uses its own health check mechanism via Actuator endpoints
# The /actuator/health endpoint is automatically checked by Cloud Run
# No Docker HEALTHCHECK needed here

# Run application
ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-XX:+UseStringDeduplication", \
    "-Xms256m", \
    "-Xmx1g", \
    "-jar", \
    "app.jar"]
