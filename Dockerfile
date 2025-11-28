# ============================================
# MULTI-STAGE BUILD FOR SPRING BOOT APPLICATION
# University Archive System - Al-Quds University
# ============================================

# Stage 1: Build Stage
FROM maven:3.9-eclipse-temurin-17-alpine AS build

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml first (for dependency caching)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .

# Download dependencies (this layer is cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds in CI/CD)
RUN mvn clean package -DskipTests -Dspring.profiles.active=prod

# ============================================
# Stage 2: Runtime Stage
# ============================================
FROM eclipse-temurin:17-jre-alpine

# Install useful utilities
RUN apk add --no-cache curl tzdata

# Set timezone
ENV TZ=Asia/Jerusalem

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Create directory structure for hierarchical file storage
# Format: uploads/{year}/{semester}/{professorId}/{courseCode}/{documentType}/
RUN mkdir -p /app/uploads /app/logs && \
    chown -R appuser:appgroup /app

# Copy the built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership of the jar file
RUN chown appuser:appgroup app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Set environment variables (can be overridden at runtime)
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run the application with optimized JVM settings
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
