# === Build stage ===
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Copy parent pom and common modules first (layer cache friendly)
COPY pom.xml .
COPY common/ common/

# Copy this service's pom to download deps
COPY reader-service/pom.xml reader-service/

# Pre-download dependencies (cached when source unchanged)
RUN mvn -pl reader-service -am dependency:go-offline -DskipTests -B

# Copy source and build
COPY reader-service/src reader-service/src
RUN mvn -pl reader-service -am clean package -DskipTests -B

# === Runtime stage ===
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=builder /build/reader-service/target/*.jar app.jar

EXPOSE 8084

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8084/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
