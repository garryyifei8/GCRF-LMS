# ===== Build Stage =====
FROM eclipse-temurin:21-jdk-alpine AS builder

# Install Maven since mvnw is not present
RUN apk add --no-cache maven

# Set working directory
WORKDIR /build

# Copy parent POM for dependency resolution
COPY pom.xml .

# Copy common modules (required dependencies)
COPY common/common-core/pom.xml common/common-core/
COPY common/common-core/src common/common-core/src

COPY common/common-web/pom.xml common/common-web/
COPY common/common-web/src common/common-web/src

COPY common/common-security/pom.xml common/common-security/
COPY common/common-security/src common/common-security/src

COPY common/common-mybatis/pom.xml common/common-mybatis/
COPY common/common-mybatis/src common/common-mybatis/src

# Copy gateway service POM
COPY gateway-service/pom.xml gateway-service/

# Download dependencies (this layer will be cached)
RUN cd gateway-service && mvn dependency:go-offline -B

# Copy gateway service source
COPY gateway-service/src gateway-service/src

# Build the application
RUN cd gateway-service && mvn clean package -DskipTests -B

# ===== Runtime Stage =====
FROM eclipse-temurin:21-jre-alpine

# Install curl for health check
RUN apk add --no-cache curl

# Create non-root user and group
RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /build/gateway-service/target/*.jar app.jar

# Change ownership to non-root user
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

# Expose gateway port
EXPOSE 8080

# Health check configuration
# Gateway uses /actuator/health endpoint (configured in application.yml)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options optimized for containers
# - G1GC for better performance in containers
# - MaxRAMPercentage for dynamic memory allocation
# - ExitOnOutOfMemoryError for fail-fast behavior
# - Enhanced security random generator
ENV JAVA_OPTS="-XX:+UseG1GC \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"

# Additional gateway-specific JVM options for reactive stack
ENV GATEWAY_OPTS="-Dreactor.netty.pool.maxConnections=1000 \
                   -Dreactor.netty.pool.maxIdleTime=20s"

# Start the application with combined JVM options
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS $GATEWAY_OPTS -jar app.jar"]