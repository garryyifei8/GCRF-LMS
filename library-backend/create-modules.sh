#!/bin/bash

# 业务微服务列表
BUSINESS_SERVICES=("auth-service:8081" "book-service:8082" "circulation-service:8083" "reader-service:8084" "system-service:8085")

# AI微服务列表
AI_SERVICES=("recommend-service:8086" "nlp-service:8087" "vision-service:8088" "analytics-service:8089")

# 基础服务列表
BASE_SERVICES=("notification-service:8090" "file-service:8091" "search-service:8092")

# 创建服务pom.xml的函数
create_service_pom() {
    local service_name=$1
    local port=$2
    local service_dir="${service_name}"
    
    cat > "${service_dir}/pom.xml" << POMEOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.gcrf.library</groupId>
        <artifactId>library-backend</artifactId>
        <version>1.0.0</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>${service_name}</artifactId>
    <packaging>jar</packaging>

    <name>${service_name}</name>
    <description>${service_name}</description>

    <dependencies>
        <!-- 公共模块 -->
        <dependency>
            <groupId>com.gcrf.library</groupId>
            <artifactId>library-common</artifactId>
        </dependency>

        <!-- API模块 -->
        <dependency>
            <groupId>com.gcrf.library</groupId>
            <artifactId>library-api</artifactId>
        </dependency>

        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Nacos服务注册发现 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>

        <!-- Nacos配置中心 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>

        <!-- MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>

        <!-- MyBatis Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
        </dependency>

        <!-- Druid -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid-spring-boot-starter</artifactId>
        </dependency>

        <!-- OpenFeign -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>

        <!-- Sentinel -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
        </dependency>

        <!-- Knife4j -->
        <dependency>
            <groupId>com.github.xiaoymin</groupId>
            <artifactId>knife4j-openapi3-spring-boot-starter</artifactId>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <finalName>\${project.artifactId}</finalName>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>2.7.18</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
POMEOF

    # 创建application.yml
    mkdir -p "${service_dir}/src/main/resources"
    cat > "${service_dir}/src/main/resources/application.yml" << YMLEOF
server:
  port: ${port}

spring:
  application:
    name: ${service_name}
  profiles:
    active: dev
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
      config:
        server-addr: localhost:8848
        file-extension: yaml

# MyBatis Plus
mybatis-plus:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.gcrf.library.${service_name/-/}.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

# Knife4j
knife4j:
  enable: true
  setting:
    language: zh_cn
YMLEOF

    echo "Created ${service_name} pom.xml and application.yml"
}

# 创建所有业务微服务
for service in "${BUSINESS_SERVICES[@]}"; do
    IFS=':' read -r name port <<< "$service"
    create_service_pom "$name" "$port"
done

# 创建所有AI微服务
for service in "${AI_SERVICES[@]}"; do
    IFS=':' read -r name port <<< "$service"
    create_service_pom "$name" "$port"
done

# 创建所有基础服务
for service in "${BASE_SERVICES[@]}"; do
    IFS=':' read -r name port <<< "$service"
    create_service_pom "$name" "$port"
done

echo "All service modules created successfully!"
