# GCRF Library Management System - Auth Service
# Multi-stage production Dockerfile optimized for database connections
# Service: auth-service (Port 8081)
# Dependencies: PostgreSQL, Redis, Nacos
# Last Updated: 2025-11-01

# ===========================
# Stage 1: Dependencies Cache
# ===========================
FROM eclipse-temurin:21-jdk-alpine AS deps

# Install Maven
RUN apk add --no-cache maven

WORKDIR /build

# Copy parent POM and common modules for dependency resolution
COPY pom.xml .
COPY common/common-core/pom.xml common/common-core/
COPY common/common-web/pom.xml common/common-web/
COPY common/common-security/pom.xml common/common-security/
COPY common/common-mybatis/pom.xml common/common-mybatis/

# Copy service POM
COPY auth-service/pom.xml auth-service/

# Download dependencies (cached unless POM changes)
RUN cd auth-service && mvn dependency:go-offline -B

# ===========================
# Stage 2: Application Build
# ===========================
FROM eclipse-temurin:21-jdk-alpine AS builder

# Install Maven
RUN apk add --no-cache maven

WORKDIR /build

# Copy everything from deps stage
COPY --from=deps /build /build
COPY --from=deps /root/.m2 /root/.m2

# Copy common modules source code
COPY common/common-core/src common/common-core/src
COPY common/common-web/src common/common-web/src
COPY common/common-security/src common/common-security/src
COPY common/common-mybatis/src common/common-mybatis/src

# Copy service source code
COPY auth-service/src auth-service/src

# Build the application (skip tests for faster build)
RUN cd auth-service && mvn clean package -DskipTests -B \
    && mv target/*.jar target/app.jar

# ===========================
# Stage 3: Runtime Image
# ===========================
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks and create non-root user
RUN apk add --no-cache curl \
    && addgroup -S spring \
    && adduser -S spring -G spring

# Create application directory
WORKDIR /app

# Create logs directory with proper permissions
RUN mkdir -p /var/log/gcrf-library \
    && chown -R spring:spring /var/log/gcrf-library

# Copy JAR from builder
COPY --from=builder /build/auth-service/target/app.jar app.jar

# Set ownership
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

# Expose service port
EXPOSE 8081

# Health check configuration
# Checks /actuator/health endpoint with authentication consideration
HEALTHCHECK --interval=30s \
            --timeout=10s \
            --start-period=60s \
            --retries=3 \
            CMD curl -f http://localhost:8081/actuator/health || exit 1

# JVM Configuration - Optimized for database connections and production
ENV JAVA_OPTS="-XX:+UseG1GC \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -XX:+ExitOnOutOfMemoryError \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/var/log/gcrf-library/heap-dump.hprof \
               -Djava.security.egd=file:/dev/./urandom \
               -Dfile.encoding=UTF-8 \
               -Duser.timezone=Asia/Shanghai \
               -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"

# Additional JVM options for database optimization
ENV DB_OPTS="-Djava.net.preferIPv4Stack=true \
             -Dcom.zaxxer.hikari.aliveBypassWindowMs=500 \
             -Dcom.zaxxer.hikari.housekeepingPeriodMs=30000"

# Security options
ENV SECURITY_OPTS="-Djava.security.properties=/dev/null \
                   -Dnetworkaddress.cache.ttl=60 \
                   -Dnetworkaddress.cache.negative.ttl=10"

# Entry point with combined JVM options
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS $DB_OPTS $SECURITY_OPTS -jar app.jar"]

# Labels for image metadata
LABEL maintainer="GCRF Library System Team" \
      service="auth-service" \
      version="1.0.0-SNAPSHOT" \
      description="GCRF Library Management System - Authentication Service" \
      base.image="eclipse-temurin:21-jre-alpine" \
      java.version="21"