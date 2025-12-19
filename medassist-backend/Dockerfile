# ============================================
# PharmaLens - AI Pharmaceutical Intelligence Platform
# Multi-stage Dockerfile
# ============================================
# Stage 1: Build stage
# Stage 2: Runtime stage (optimized)
# ============================================

# ==================== BUILD STAGE ====================
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set working directory
WORKDIR /app

# Install Maven
RUN apk add --no-cache maven

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds - tests should run in CI)
RUN mvn clean package -DskipTests -B

# Extract layers for better caching
RUN java -Djarmode=layertools -jar target/*.jar extract --destination extracted

# ==================== RUNTIME STAGE ====================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Add labels for container metadata
LABEL maintainer="PharmaLens Team"
LABEL version="2.0.0"
LABEL description="PharmaLens - AI Pharmaceutical Intelligence Platform API"

# Create non-root user for security
RUN addgroup -g 1001 -S pharmalens && \
    adduser -u 1001 -S pharmalens -G pharmalens

# Set working directory
WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Copy extracted layers from builder (in order of change frequency)
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./

# Change ownership to non-root user
RUN chown -R pharmalens:pharmalens /app

# Switch to non-root user
USER pharmalens

# Expose application port
EXPOSE 8080

# Health check configuration
# Checks /actuator/health endpoint every 30 seconds
# Allows 60 seconds for startup, retries 3 times before marking unhealthy
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM optimization flags for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=prod"

# Run the application using Spring Boot's layered jar launcher
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
