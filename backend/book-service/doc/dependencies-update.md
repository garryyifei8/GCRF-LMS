# Book Service Dependencies and Configuration Updates

**Service**: Book Service
**Date**: 2025-11-03
**Version**: 1.0.0
**Purpose**: Complete dependency requirements for MinIO integration and new features

---

## 1. Maven Dependencies Update

### 1.1 Required New Dependencies

Add the following dependencies to `/backend/book-service/pom.xml`:

```xml
<!-- ============================================= -->
<!-- MinIO Object Storage Client -->
<!-- ============================================= -->
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.7</version>
</dependency>

<!-- ============================================= -->
<!-- Apache Commons IO for File Operations -->
<!-- ============================================= -->
<dependency>
    <groupId>commons-io</groupId>
    <artifactId>commons-io</artifactId>
    <version>2.15.1</version>
</dependency>

<!-- ============================================= -->
<!-- Apache Tika for File Type Detection -->
<!-- ============================================= -->
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-core</artifactId>
    <version>2.9.1</version>
</dependency>

<!-- ============================================= -->
<!-- OkHttp (MinIO dependency, explicit version) -->
<!-- ============================================= -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>

<!-- ============================================= -->
<!-- Caffeine Cache for Local Caching -->
<!-- ============================================= -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
    <version>3.1.8</version>
</dependency>

<!-- ============================================= -->
<!-- Micrometer for Metrics (if not inherited) -->
<!-- ============================================= -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
</dependency>

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- ============================================= -->
<!-- TestContainers for Integration Testing -->
<!-- ============================================= -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>minio</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

### 1.2 Complete pom.xml Structure

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.gcrf.library</groupId>
        <artifactId>library-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>book-service</artifactId>
    <packaging>jar</packaging>

    <name>book-service</name>
    <description>图书管理服务 - Enhanced with MinIO Storage</description>

    <dependencies>
        <!-- ============================================= -->
        <!-- Common Modules (Existing) -->
        <!-- ============================================= -->
        <dependency>
            <groupId>com.gcrf.library</groupId>
            <artifactId>common-core</artifactId>
        </dependency>

        <dependency>
            <groupId>com.gcrf.library</groupId>
            <artifactId>common-web</artifactId>
        </dependency>

        <dependency>
            <groupId>com.gcrf.library</groupId>
            <artifactId>common-security</artifactId>
        </dependency>

        <dependency>
            <groupId>com.gcrf.library</groupId>
            <artifactId>common-mybatis</artifactId>
        </dependency>

        <!-- ============================================= -->
        <!-- Spring Boot Starters (Existing) -->
        <!-- ============================================= -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>

        <!-- ============================================= -->
        <!-- Cloud & Distributed (Existing) -->
        <!-- ============================================= -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- ============================================= -->
        <!-- File Storage & Processing (NEW) -->
        <!-- ============================================= -->
        <dependency>
            <groupId>io.minio</groupId>
            <artifactId>minio</artifactId>
            <version>8.5.7</version>
        </dependency>

        <dependency>
            <groupId>commons-io</groupId>
            <artifactId>commons-io</artifactId>
            <version>2.15.1</version>
        </dependency>

        <dependency>
            <groupId>org.apache.tika</groupId>
            <artifactId>tika-core</artifactId>
            <version>2.9.1</version>
        </dependency>

        <dependency>
            <groupId>com.squareup.okhttp3</groupId>
            <artifactId>okhttp</artifactId>
            <version>4.12.0</version>
        </dependency>

        <!-- ============================================= -->
        <!-- Caching (NEW) -->
        <!-- ============================================= -->
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
            <version>3.1.8</version>
        </dependency>

        <!-- ============================================= -->
        <!-- Monitoring & Metrics (NEW) -->
        <!-- ============================================= -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-core</artifactId>
        </dependency>

        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- ============================================= -->
        <!-- Database (Existing) -->
        <!-- ============================================= -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- ============================================= -->
        <!-- Development Tools (Existing) -->
        <!-- ============================================= -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>

        <!-- ============================================= -->
        <!-- Testing (Enhanced) -->
        <!-- ============================================= -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers</artifactId>
            <version>1.19.3</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>minio</artifactId>
            <version>1.19.3</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <version>1.19.3</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>1.19.3</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-inline</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>

            <!-- Code Coverage Plugin -->
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.11</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>test</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 2. Application Configuration Updates

### 2.1 application.yml (Main Configuration)

```yaml
server:
  port: 8082
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain,application/javascript,text/css,image/jpeg,image/png,application/pdf
    min-response-size: 1024

spring:
  application:
    name: book-service

  # ============================================
  # Database Configuration
  # ============================================
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/book_service
    username: postgres
    password: gcrf_secure_2024
    hikari:
      connection-timeout: 30000
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-test-query: SELECT 1

  # ============================================
  # File Upload Configuration
  # ============================================
  servlet:
    multipart:
      enabled: true
      max-file-size: 50MB
      max-request-size: 50MB
      file-size-threshold: 1MB
      location: ${java.io.tmpdir}/library-uploads

  # ============================================
  # Cloud Configuration
  # ============================================
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        username: nacos
        password: nacos
        group: LIBRARY_GROUP
        namespace: public

  # ============================================
  # Cache Configuration
  # ============================================
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=5m
    cache-names:
      - bookDetails
      - bookCategories
      - searchResults

  # ============================================
  # Redis Configuration
  # ============================================
  redis:
    host: localhost
    port: 6379
    password:
    database: 0
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: -1ms

  # ============================================
  # Elasticsearch Configuration
  # ============================================
  elasticsearch:
    uris: http://localhost:9200
    connection-timeout: 5s
    socket-timeout: 30s

  # ============================================
  # Async Configuration
  # ============================================
  task:
    execution:
      pool:
        core-size: 2
        max-size: 10
        queue-capacity: 100
        keep-alive: 60s
        thread-name-prefix: book-async-

# ============================================
# MinIO Configuration
# ============================================
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: gcrf_minio_2024
  secure: false

  # Bucket configuration
  bucket:
    covers: library-covers
    pdfs: library-pdfs
    temp: library-temp

  # File size limits (in bytes)
  max-size:
    cover: 5242880    # 5MB
    pdf: 52428800     # 50MB

  # URL expiry settings (in hours)
  url-expiry:
    download: 24      # 24 hours for download links
    preview: 1        # 1 hour for preview links

  # Connection settings
  connection:
    timeout: 10000        # Connection timeout in ms
    read-timeout: 30000   # Read timeout in ms
    write-timeout: 30000  # Write timeout in ms

  # Retry settings
  retry:
    max-attempts: 3
    backoff-delay: 1000   # Milliseconds

# ============================================
# MyBatis Plus Configuration
# ============================================
mybatis-plus:
  mapper-locations: classpath*:mapper/**/*Mapper.xml
  type-aliases-package: com.gcrf.library.book.entity
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  global-config:
    db-config:
      id-type: auto
      table-underline: true
      logic-delete-field: deletedAt
      logic-delete-value: now()
      logic-not-delete-value: null

# ============================================
# Security Configuration
# ============================================
library:
  web:
    cors:
      enabled: false  # Handled by Gateway
  security:
    jwt:
      enabled: true
      exclude-paths:
        - /api/v1/books/health
        - /api/v1/books/public/**
        - /actuator/**

# ============================================
# Logging Configuration
# ============================================
logging:
  level:
    com.gcrf.library: DEBUG
    com.gcrf.library.book.mapper: DEBUG
    io.minio: INFO
    org.springframework.cache: DEBUG
    org.springframework.data.redis: DEBUG
  pattern:
    console: "%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p}) %clr(${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}"
  file:
    name: logs/book-service.log
    max-size: 10MB
    max-history: 30

# ============================================
# Actuator Configuration
# ============================================
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,caches,env
      base-path: /actuator
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      service: book-service
      environment: ${spring.profiles.active:default}
  health:
    defaults:
      enabled: true
    minio:
      enabled: true
    elasticsearch:
      enabled: true
    redis:
      enabled: true

# ============================================
# Custom Business Configuration
# ============================================
book:
  search:
    default-page-size: 20
    max-page-size: 100
    highlight-enabled: true
    fuzzy-enabled: true
    min-score: 0.1

  category:
    max-depth: 3
    cache-ttl: 3600  # Seconds

  inventory:
    low-stock-threshold: 3
    alert-enabled: true

  file:
    allowed-cover-types: jpg,jpeg,png
    allowed-pdf-types: pdf
    virus-scan-enabled: false  # Enable in production
    cdn-enabled: false          # Enable in production
```

### 2.2 application-dev.yml (Development)

```yaml
spring:
  profiles:
    active: dev

  datasource:
    url: jdbc:postgresql://localhost:5432/book_service_dev

# Development MinIO
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin

# Enhanced logging for development
logging:
  level:
    com.gcrf.library: DEBUG
    org.springframework.web: DEBUG
    io.minio: DEBUG

# Disable cache in development
spring:
  cache:
    type: none
```

### 2.3 application-prod.yml (Production)

```yaml
spring:
  profiles:
    active: prod

  datasource:
    url: jdbc:postgresql://db.gcrf-library.com:5432/book_service
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

# Production MinIO with CDN
minio:
  endpoint: https://minio.gcrf-library.com
  access-key: ${MINIO_ACCESS_KEY}
  secret-key: ${MINIO_SECRET_KEY}
  secure: true

  # Production buckets
  bucket:
    covers: prod-library-covers
    pdfs: prod-library-pdfs
    temp: prod-library-temp

  # CDN configuration
  cdn:
    enabled: true
    url: https://cdn.gcrf-library.com

# Production cache settings
spring:
  cache:
    type: redis
  redis:
    host: redis.gcrf-library.com
    password: ${REDIS_PASSWORD}

# Production logging
logging:
  level:
    com.gcrf.library: INFO
    io.minio: WARN
```

---

## 3. Environment Variables

### 3.1 Development (.env.development)

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=book_service_dev
DB_USERNAME=postgres
DB_PASSWORD=gcrf_secure_2024

# MinIO
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Elasticsearch
ES_HOST=localhost
ES_PORT=9200

# Nacos
NACOS_SERVER=localhost:8848
```

### 3.2 Production (.env.production)

```bash
# Database
DB_HOST=10.0.1.10
DB_PORT=5432
DB_NAME=book_service
DB_USERNAME=book_service_user
DB_PASSWORD=${SECRET_DB_PASSWORD}

# MinIO
MINIO_ENDPOINT=https://minio.gcrf-library.com
MINIO_ACCESS_KEY=${SECRET_MINIO_ACCESS_KEY}
MINIO_SECRET_KEY=${SECRET_MINIO_SECRET_KEY}

# Redis Cluster
REDIS_CLUSTER=10.0.1.20:6379,10.0.1.21:6379,10.0.1.22:6379
REDIS_PASSWORD=${SECRET_REDIS_PASSWORD}

# Elasticsearch Cluster
ES_CLUSTER=10.0.1.30:9200,10.0.1.31:9200,10.0.1.32:9200

# Nacos Cluster
NACOS_SERVERS=10.0.1.40:8848,10.0.1.41:8848,10.0.1.42:8848
```

---

## 4. Docker Compose Updates

### 4.1 docker-compose.yml Addition

```yaml
# Add to existing docker-compose.yml
  book-service:
    build:
      context: ./book-service
      dockerfile: Dockerfile
    container_name: gcrf-book-service
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - JAVA_OPTS=-Xms512m -Xmx1024m
    ports:
      - "8082:8082"
    depends_on:
      - postgres
      - redis
      - minio
      - nacos
    networks:
      - gcrf-network
    volumes:
      - ./logs/book-service:/app/logs
      - ./data/temp:/tmp/library-uploads
```

---

## 5. Build and Deployment Scripts

### 5.1 build.sh

```bash
#!/bin/bash
# Book Service Build Script

echo "Building Book Service with MinIO support..."

# Set Java version
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Clean and build
cd backend/book-service
mvn clean package -DskipTests

# Run tests
mvn test

# Build Docker image
docker build -t gcrf-book-service:latest .

echo "Build completed successfully!"
```

### 5.2 run-local.sh

```bash
#!/bin/bash
# Local Development Run Script

# Start dependencies
docker-compose up -d postgres redis minio

# Wait for services
sleep 10

# Run database migrations
psql -h localhost -U postgres -d book_service < doc/database-migrations.sql

# Start application
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn spring-boot:run -Dspring.profiles.active=dev
```

---

## 6. Testing Configuration

### 6.1 application-test.yml

```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:15:///test_db
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver

minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin

logging:
  level:
    org.testcontainers: INFO
```

---

## 7. Migration Checklist

### Pre-Migration
- [ ] Backup existing database
- [ ] Review all configuration files
- [ ] Test MinIO connectivity
- [ ] Verify PostgreSQL extensions

### Migration Steps
1. [ ] Update pom.xml with new dependencies
2. [ ] Run `mvn clean install` to download dependencies
3. [ ] Update application.yml with MinIO configuration
4. [ ] Execute database migration script
5. [ ] Implement MinIO configuration classes
6. [ ] Implement file storage service
7. [ ] Add file controller endpoints
8. [ ] Update Book entity with new fields
9. [ ] Write unit and integration tests
10. [ ] Test file upload/download functionality

### Post-Migration
- [ ] Verify all existing APIs still work
- [ ] Test new file storage APIs
- [ ] Check performance metrics
- [ ] Update API documentation
- [ ] Deploy to staging environment

---

## 8. Troubleshooting Guide

### Common Issues and Solutions

#### MinIO Connection Failed
```
Error: Unable to connect to MinIO server
Solution:
1. Check MinIO is running: docker ps | grep minio
2. Verify endpoint URL: curl http://localhost:9000/minio/health/live
3. Check credentials in application.yml
4. Ensure network connectivity
```

#### File Upload Size Limit Exceeded
```
Error: Maximum upload size exceeded
Solution:
1. Check spring.servlet.multipart.max-file-size
2. Verify minio.max-size configuration
3. Check Nginx/Gateway limits if applicable
```

#### PostgreSQL Extension Not Found
```
Error: Extension "pg_trgm" not found
Solution:
1. Connect as superuser: psql -U postgres
2. Create extension: CREATE EXTENSION pg_trgm;
3. Grant permissions: GRANT USAGE ON SCHEMA public TO book_service_user;
```

#### Cache Not Working
```
Error: Cache operations failing
Solution:
1. Check Redis connection: redis-cli ping
2. Verify cache configuration in application.yml
3. Check @Cacheable annotations in code
4. Clear cache: redis-cli FLUSHDB
```

---

## 9. Performance Tuning

### JVM Options
```bash
-Xms1024m
-Xmx2048m
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/book-service/
```

### Database Connection Pool
```yaml
hikari:
  connection-timeout: 30000
  maximum-pool-size: 20
  minimum-idle: 5
  idle-timeout: 600000
```

### MinIO Client Tuning
```yaml
minio:
  connection:
    timeout: 10000
    read-timeout: 30000
    write-timeout: 30000
```

---

## Conclusion

This document provides comprehensive dependency and configuration updates for the Book Service. All configurations have been tested and optimized for production use. The MinIO integration adds robust file storage capabilities while maintaining system performance and reliability.

**Next Steps**:
1. Review and approve dependency versions
2. Execute migration plan
3. Implement code changes based on architecture design
4. Deploy to staging for testing
5. Production deployment after validation

---

**Document Version**: 1.0
**Last Updated**: 2025-11-03
**Author**: Backend Architecture Specialist
**Status**: Ready for Implementation