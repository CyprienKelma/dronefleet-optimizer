# Build stage
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy Gradle wrapper and root configuration
COPY gradle gradle
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .

# Copy shared/libs dependencies if any (based on settings.gradle)
COPY shared/java shared/java
COPY libs/java libs/java

# Copy service code
COPY services/state_manager services/state_manager

# Build application
RUN ./gradlew :services:state_manager:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy JAR from build stage
COPY --from=build /app/services/state_manager/build/libs/*.jar app.jar

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
