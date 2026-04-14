# 多阶段构建Dockerfile for Book Service
# Base image: Eclipse Temurin JDK 21

# ==================== Stage 1: Dependencies ====================
FROM eclipse-temurin:21-jdk-alpine AS deps
WORKDIR /build

# 复制Maven配置文件
COPY pom.xml .
COPY ../pom.xml ../pom.xml

# 下载依赖 (利用Docker缓存层)
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -B

# ==================== Stage 2: Build ====================
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build

# 从deps阶段复制依赖
COPY --from=deps /root/.m2 /root/.m2

# 复制所有源代码
COPY pom.xml .
COPY src ./src

# 编译打包 (跳过测试以加快构建)
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests -B

# ==================== Stage 3: Runtime ====================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 安装运行时依赖
RUN apk add --no-cache \
    curl \
    tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 从build阶段复制jar包
COPY --from=build /build/target/*.jar app.jar

# 创建非root用户
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 暴露端口
EXPOSE 8082

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8082/actuator/health || exit 1

# JVM参数优化
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
