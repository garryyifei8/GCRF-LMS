# Library Gateway - API网关服务

## 概述

Library Gateway是国创睿峰智能图书馆管理系统的API网关服务，基于Spring Cloud Gateway实现，提供统一的请求入口、路由转发、认证鉴权、限流熔断等功能。

## 核心功能

### 1. 路由管理
- **动态路由**: 支持从Nacos动态加载路由配置
- **服务发现**: 自动发现注册到Nacos的微服务
- **负载均衡**: 基于Ribbon实现客户端负载均衡
- **路径重写**: 支持路径前缀去除和重写

### 2. 认证鉴权
- **JWT验证**: 验证请求中的JWT Token
- **白名单机制**: 支持配置免认证的路径
- **用户信息传递**: 将用户信息添加到请求头传递给下游服务
- **Token自动刷新**: 支持Token过期自动刷新

### 3. 限流熔断
- **基于Sentinel**: 使用Sentinel实现限流和熔断
- **QPS限流**: 支持按QPS限流
- **自定义降级**: 自定义限流降级响应
- **实时监控**: 集成Sentinel Dashboard实时监控

### 4. 跨域支持
- **全局CORS配置**: 支持跨域请求
- **灵活配置**: 可配置允许的域名、方法、头部
- **Credentials支持**: 支持携带Cookie

### 5. 异常处理
- **全局异常捕获**: 统一处理所有网关异常
- **友好错误响应**: 返回统一格式的错误信息
- **异常日志记录**: 记录详细的异常堆栈

### 6. 监控指标
- **健康检查**: 提供健康检查接口
- **路由查询**: 查看所有已配置的路由
- **服务列表**: 查看所有注册的微服务
- **Prometheus集成**: 支持Prometheus监控

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.7.18 | 基础框架 |
| Spring Cloud Gateway | 3.1.x | 网关框架 |
| Nacos | 2.2.3 | 服务注册与配置中心 |
| Sentinel | 1.8.x | 限流熔断 |
| JWT | 0.11.5 | Token验证 |
| Redis | 6.0+ | 限流缓存 |

## 项目结构

```
library-gateway/
├── src/main/java/com/gcrf/library/gateway/
│   ├── GatewayApplication.java          # 启动类
│   ├── config/                          # 配置类
│   │   ├── CorsConfig.java             # CORS配置
│   │   └── SentinelConfig.java         # Sentinel配置
│   ├── filter/                          # 过滤器
│   │   └── AuthFilter.java             # 认证过滤器
│   ├── util/                            # 工具类
│   │   └── JwtUtil.java                # JWT工具类
│   ├── exception/                       # 异常处理
│   │   └── GlobalExceptionHandler.java # 全局异常处理器
│   └── controller/                      # 控制器
│       └── HealthController.java       # 健康检查
└── src/main/resources/
    └── application.yml                  # 配置文件
```

## 路由配置

### 服务路由映射

| 前端请求路径 | 后端服务 | 端口 | 说明 |
|------------|---------|------|------|
| `/api/auth/**` | auth-service | 8081 | 认证服务 |
| `/api/books/**` | book-service | 8082 | 图书服务 |
| `/api/circulation/**` | circulation-service | 8083 | 流通服务 |
| `/api/readers/**` | reader-service | 8084 | 读者服务 |
| `/api/system/**` | system-service | 8085 | 系统服务 |
| `/api/recommend/**` | recommend-service | 8086 | 推荐服务 |
| `/api/nlp/**` | nlp-service | 8087 | NLP服务 |
| `/api/vision/**` | vision-service | 8088 | 视觉识别服务 |
| `/api/analytics/**` | analytics-service | 8089 | 数据分析服务 |
| `/api/notifications/**` | notification-service | 8090 | 通知服务 |
| `/api/files/**` | file-service | 8091 | 文件服务 |
| `/api/search/**` | search-service | 8092 | 搜索服务 |

### 路由示例

**请求**:
```
GET http://localhost:8080/api/books/list
```

**转发到**:
```
GET http://book-service:8082/books/list
```

## 认证流程

### 1. 登录获取Token

```bash
# 用户登录
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}

# 响应
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "expiresIn": 7200
  }
}
```

### 2. 携带Token访问受保护资源

```bash
# 携带Token访问
GET http://localhost:8080/api/books/list
Authorization: Bearer eyJhbGc...

# 响应
{
  "code": 200,
  "message": "成功",
  "data": [...]
}
```

### 3. 白名单路径（无需Token）

以下路径无需认证即可访问：

- `/api/auth/login` - 用户登录
- `/api/auth/face-login` - 人脸识别登录
- `/api/auth/register` - 用户注册
- `/actuator/**` - 健康检查和监控
- `/error` - 错误页面

## 限流配置

### 限流规则

| 服务 | QPS限制 | 令牌桶容量 |
|-----|---------|-----------|
| 认证服务 | 100 | 200 |
| 图书服务 | 100 | 200 |
| 流通服务 | 100 | 200 |
| 读者服务 | 100 | 200 |
| 系统服务 | 100 | 200 |
| 推荐服务 | 50 | 100 |
| NLP服务 | 50 | 100 |
| 视觉服务 | 30 | 60 |
| 分析服务 | 50 | 100 |
| 通知服务 | 100 | 200 |
| 文件服务 | 50 | 100 |
| 搜索服务 | 100 | 200 |

### 限流响应

当请求被限流时，返回：

```json
{
  "code": 429,
  "message": "系统繁忙，请稍后再试",
  "data": null
}
```

## 快速启动

### 1. 环境准备

确保以下服务已启动：

- **Nacos**: http://localhost:8848
- **Redis**: localhost:6379
- **Sentinel Dashboard** (可选): http://localhost:8858

### 2. 配置文件

修改 `application.yml` 中的配置：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848  # Nacos地址
  redis:
    host: localhost                  # Redis地址
    port: 6379

jwt:
  secret: YOUR_SECRET_KEY_HERE       # JWT密钥（生产环境必须修改）
```

### 3. 启动服务

```bash
# 方式1: Maven启动
mvn spring-boot:run

# 方式2: JAR包启动
mvn clean package -DskipTests
java -jar target/library-gateway.jar

# 方式3: Docker启动
docker build -t library-gateway:latest .
docker run -d -p 8080:8080 library-gateway:latest
```

### 4. 验证服务

```bash
# 检查健康状态
curl http://localhost:8080/actuator/health

# 查看所有路由
curl http://localhost:8080/actuator/routes

# 查看所有服务
curl http://localhost:8080/actuator/services
```

## 配置说明

### 环境变量

支持以下环境变量：

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `NACOS_HOST` | localhost | Nacos服务器地址 |
| `NACOS_PORT` | 8848 | Nacos服务器端口 |
| `NACOS_NAMESPACE` | dev | Nacos命名空间 |
| `NACOS_USERNAME` | nacos | Nacos用户名 |
| `NACOS_PASSWORD` | nacos | Nacos密码 |
| `REDIS_HOST` | localhost | Redis服务器地址 |
| `REDIS_PORT` | 6379 | Redis服务器端口 |
| `REDIS_PASSWORD` | (空) | Redis密码 |
| `JWT_SECRET` | MySecretKey... | JWT密钥 |
| `SENTINEL_DASHBOARD` | localhost:8858 | Sentinel控制台地址 |

### Docker环境变量示例

```bash
docker run -d \
  -p 8080:8080 \
  -e NACOS_HOST=nacos-server \
  -e NACOS_PORT=8848 \
  -e REDIS_HOST=redis-server \
  -e REDIS_PORT=6379 \
  -e JWT_SECRET=YOUR_SECRET_KEY \
  library-gateway:latest
```

## 监控和管理

### 健康检查

```bash
# 基本健康检查
curl http://localhost:8080/actuator/health

# 响应
{
  "status": "UP",
  "timestamp": "2025-10-11T10:00:00",
  "service": "library-gateway"
}
```

### 路由查询

```bash
# 查看所有路由
curl http://localhost:8080/actuator/routes

# 查看特定路由
curl http://localhost:8080/actuator/gateway/routes/{routeId}
```

### 服务列表

```bash
# 查看所有注册的服务
curl http://localhost:8080/actuator/services

# 响应
{
  "count": 12,
  "services": [
    "auth-service",
    "book-service",
    ...
  ],
  "timestamp": "2025-10-11T10:00:00"
}
```

### Sentinel监控

访问 Sentinel Dashboard:

```
http://localhost:8858
用户名: sentinel
密码: sentinel
```

在控制台可以查看：
- 实时QPS
- 响应时间
- 异常统计
- 限流规则
- 熔断规则

## 性能优化

### 1. JVM参数优化

```bash
java -Xms512m -Xmx512m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -jar library-gateway.jar
```

### 2. Netty参数调优

```yaml
spring:
  cloud:
    gateway:
      httpclient:
        pool:
          max-connections: 500
          max-idle-time: 30s
        connect-timeout: 5000
        response-timeout: 10s
```

### 3. Redis连接池优化

```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 16    # 增加最大连接数
        max-idle: 16
        min-idle: 4
```

## 故障排查

### 1. 服务无法注册到Nacos

**现象**: 网关启动后，Nacos控制台看不到服务

**排查步骤**:
```bash
# 1. 检查Nacos是否启动
curl http://localhost:8848/nacos/

# 2. 检查网络连通性
ping nacos-server

# 3. 查看网关日志
tail -f logs/gateway.log | grep "nacos"

# 4. 检查配置
grep -A 5 "nacos:" application.yml
```

### 2. 路由转发失败

**现象**: 请求返回503 Service Unavailable

**排查步骤**:
```bash
# 1. 检查目标服务是否启动
curl http://localhost:8081/actuator/health  # auth-service

# 2. 检查Nacos服务列表
curl http://localhost:8080/actuator/services

# 3. 检查路由配置
curl http://localhost:8080/actuator/routes

# 4. 查看网关日志
tail -f logs/gateway.log | grep "route"
```

### 3. 认证失败

**现象**: 请求返回401 Unauthorized

**排查步骤**:
```bash
# 1. 检查Token是否有效
# 在 https://jwt.io 解析Token

# 2. 检查JWT密钥配置
grep "jwt.secret" application.yml

# 3. 检查白名单配置
grep -A 5 "whitelist" application.yml

# 4. 查看认证日志
tail -f logs/gateway.log | grep "Auth"
```

### 4. 限流问题

**现象**: 请求返回429 Too Many Requests

**排查步骤**:
```bash
# 1. 检查Redis连接
redis-cli ping

# 2. 查看限流配置
grep -A 10 "RequestRateLimiter" application.yml

# 3. 查看Sentinel Dashboard
# 访问 http://localhost:8858

# 4. 调整限流参数
# 修改 replenishRate 和 burstCapacity
```

## 开发规范

### 1. 添加新路由

在 `application.yml` 中添加：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: new-service
          uri: lb://new-service
          predicates:
            - Path=/api/new/**
          filters:
            - StripPrefix=1
```

### 2. 添加白名单路径

在 `application.yml` 中添加：

```yaml
gateway:
  whitelist:
    paths:
      - /api/public/**
```

### 3. 自定义过滤器

```java
@Component
public class CustomFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 自定义逻辑
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
```

## 安全加固

### 1. 修改JWT密钥

生产环境必须修改JWT密钥：

```bash
# 生成随机密钥（至少32字符）
openssl rand -base64 32

# 修改配置
export JWT_SECRET=YOUR_GENERATED_SECRET
```

### 2. 启用HTTPS

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: your_password
    key-store-type: PKCS12
```

### 3. 限制访问IP

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/auth/**
            - RemoteAddr=192.168.1.0/24  # 仅允许内网访问
```

## 测试

### 单元测试

```bash
mvn test
```

### 集成测试

```bash
# 启动网关
mvn spring-boot:run

# 运行集成测试
mvn verify
```

### 压力测试

```bash
# 使用Apache Bench
ab -n 10000 -c 100 http://localhost:8080/api/auth/login

# 使用JMeter
jmeter -n -t gateway-test.jmx -l results.jtl
```

## 版本历史

- **v1.0.0** (2025-10-11)
  - 初始版本
  - 实现基本路由转发
  - 实现JWT认证
  - 实现Sentinel限流
  - 实现CORS支持

## 技术支持

- **负责人**: 王五（Java高级工程师）
- **项目**: 国创睿峰智能图书馆管理系统
- **创建日期**: 2025-10-11

---

**版本**: v1.0.0
**最后更新**: 2025-10-11
