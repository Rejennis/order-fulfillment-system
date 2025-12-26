# Multi-stage Dockerfile for Order Fulfillment System
# Stage 1: Build stage - Uses Maven to compile and package the application
# Stage 2: Runtime stage - Minimal JRE image with only the JAR file

# =============================================================================
# BUILD STAGE
# =============================================================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

# Set working directory
WORKDIR /app

# Copy Maven wrapper files first (for caching)
COPY .mvn/ .mvn/
COPY mvnw ./
COPY mvnw.cmd ./

# Copy POM file (dependency layer - cached unless POM changes)
COPY pom.xml ./

# Download dependencies (cached layer)
# This step is separate to leverage Docker layer caching
# Dependencies are only re-downloaded when pom.xml changes
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src/ ./src/

# Build the application
# -DskipTests: Skip tests in Docker build (tests run in CI)
# -B: Batch mode (non-interactive)
# clean: Remove previous build artifacts
# package: Compile, test, and package as JAR
RUN ./mvnw clean package -DskipTests -B

# Verify JAR was created
RUN ls -la target/ && \
    test -f target/order-fulfillment-system-*.jar || \
    (echo "ERROR: JAR file not found!" && exit 1)

# =============================================================================
# RUNTIME STAGE
# =============================================================================
FROM eclipse-temurin:17-jre-alpine

# Add metadata labels (following OCI standards)
LABEL maintainer="rejennis"
LABEL description="Order Fulfillment System - Production-ready REST API"
LABEL version="1.0.0"

# Create non-root user for security
# Running as root is a security risk - always use dedicated user
RUN addgroup -S spring && \
    adduser -S spring -G spring

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
# Using wildcard to handle version-specific JAR names
COPY --from=builder /app/target/order-fulfillment-system-*.jar app.jar

# Change ownership to non-root user
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

# Expose application port (documented, but docker run -p still needed)
EXPOSE 8080

# Health check
# Docker will mark container as unhealthy if this fails
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM tuning for containers
# -XX:+UseContainerSupport: JVM respects container memory limits
# -XX:MaxRAMPercentage=75.0: Use up to 75% of container memory
# -Djava.security.egd: Faster startup (non-blocking random)
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=prod"

# Start the application
# Using shell form to allow environment variable expansion
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# =============================================================================
# BUILD & RUN INSTRUCTIONS
# =============================================================================
# Build image:
#   docker build -t order-fulfillment-system:latest .
#
# Run container:
#   docker run -p 8080:8080 \
#              -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/orderfulfillment \
#              -e SPRING_DATASOURCE_USERNAME=postgres \
#              -e SPRING_DATASOURCE_PASSWORD=postgres \
#              order-fulfillment-system:latest
#
# Run with docker-compose:
#   docker-compose up
#
# =============================================================================
# OPTIMIZATION NOTES
# =============================================================================
# 1. Multi-stage build reduces final image size (builder stage discarded)
# 2. Layer caching: Dependencies downloaded only when pom.xml changes
# 3. Alpine base: Minimal OS footprint (~50MB vs ~200MB for full Linux)
# 4. JRE instead of JDK: No compiler needed in production (~150MB savings)
# 5. Non-root user: Security best practice
# 6. Health check: Container orchestration support (Kubernetes, Docker Swarm)
#
# Image size comparison:
# - Full JDK with Ubuntu: ~600MB
# - This optimized build: ~250MB
# =============================================================================
