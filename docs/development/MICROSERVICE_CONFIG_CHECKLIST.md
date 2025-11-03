# 微服务配置检查清单

**用途**: 启动新的微服务或修复服务配置问题时的快速参考指南

**版本**: 1.0
**最后更新**: 2025-10-26

---

## 配置修复三步法

### ✅ 第一步：添加Nacos服务组配置

**文件**: `{service-name}/src/main/resources/application.yml`

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        username: nacos
        password: nacos
        group: LIBRARY_GROUP  # ⚠️ 必须添加这一行！
```

**验证方法**:
```bash
# 启动服务后，检查Nacos
TOKEN="<your-nacos-token>"
curl -s "http://localhost:8848/nacos/v1/ns/instance/list?accessToken=$TOKEN&serviceName=<service-name>&groupName=LIBRARY_GROUP" | jq .
# 应该看到 "hosts": [{"ip": "...", "port": ..., "healthy": true}]
```

---

### ✅ 第二步：禁用业务服务CORS配置

**文件**: `{service-name}/src/main/resources/application.yml`

在文件末尾添加：

```yaml
# 禁用业务服务CORS配置，由Gateway统一处理CORS
library:
  web:
    cors:
      enabled: false
```

**验证方法**:
```bash
# 启动服务后，查看日志
# 应该看到: "CORS未启用，跨域请求将被拒绝"
```

---

### ✅ 第三步：修复Gateway路由配置

**文件**: `gateway-service/src/main/resources/application.yml`

**错误配置** ❌:
```yaml
routes:
  - id: your-service
    uri: lb://your-service
    predicates:
      - Path=/api/v1/yourpath/**
    filters:
      - StripPrefix=2  # ❌ 删除这一行！
```

**正确配置** ✅:
```yaml
routes:
  - id: your-service
    uri: lb://your-service
    predicates:
      - Path=/api/v1/yourpath/**
    # 不使用StripPrefix，保留完整路径 /api/v1/yourpath/**
```

**验证方法**:
```bash
# 重启Gateway后，检查路由配置
# Gateway日志应该显示: filters=[] (空的filters)
```

---

## 常见错误快速诊断

### 错误1: 503 Service Unavailable

**症状**: 浏览器Network请求返回503
**原因**: Gateway找不到微服务
**排查**:
```bash
# 1. 检查服务是否在运行
lsof -i :<service-port>

# 2. 检查Nacos注册
curl "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=<service-name>&groupName=LIBRARY_GROUP"

# 3. 检查日志中是否有注册成功的消息
grep "nacos registry" <service-name>/logs/*.log
```

**解决**: 应用第一步 - 添加Nacos group配置

---

### 错误2: CORS Policy Error

**症状**: 浏览器控制台显示 "Access-Control-Allow-Origin header contains multiple values"
**原因**: 业务服务和Gateway都添加了CORS头
**排查**:
```bash
# 检查响应头
curl -v http://localhost:8080/api/v1/<your-endpoint>
# 查看是否有两个 access-control-allow-origin 头
```

**解决**: 应用第二步 - 禁用业务服务CORS

---

### 错误3: 404 Not Found / 500 Internal Server Error

**症状**:
- 404: 找不到资源
- 500: 业务服务日志显示 "NoResourceFoundException: No static resource xxx"

**原因**: Gateway的StripPrefix移除了路径，导致业务服务收到不完整的路径
**排查**:
```bash
# 检查业务服务日志
tail -f <service-name>/logs/spring.log
# 查看 "请求URI: /xxx" 是否缺少 /api/v1 前缀
```

**解决**: 应用第三步 - 移除Gateway StripPrefix

---

## 已修复的服务列表

| 服务 | Nacos Group | CORS禁用 | Gateway路由 | 状态 |
|------|-------------|----------|-------------|------|
| auth-service | ✅ | ✅ | ✅ 无StripPrefix | ✅ 已测试 |
| reader-service | ✅ | ✅ | ✅ 无StripPrefix | ✅ 已测试 |
| book-service | ✅ | ✅ | ✅ 无StripPrefix | ✅ 已测试 |
| circulation-service | ✅ | ✅ | ✅ 无StripPrefix | ✅ 已测试 |
| system-service | ❌ | ❌ | ❓ 未检查 | ❌ 待修复 |
| notification-service | ❌ | ❌ | ❓ 未检查 | ❌ 待修复 |

---

## 启动新服务的完整流程

### 1. 应用配置修复（启动前）

```bash
# 1. 修改 application.yml
vim <service-name>/src/main/resources/application.yml

# 添加:
# - group: LIBRARY_GROUP (在nacos.discovery下)
# - library.web.cors.enabled: false (文件末尾)

# 2. 修改 Gateway application.yml
vim gateway-service/src/main/resources/application.yml

# 移除对应路由的 StripPrefix 过滤器
```

### 2. 启动服务

```bash
cd <service-name>
HOMEBREW_NO_AUTO_UPDATE=1 JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn spring-boot:run
```

### 3. 验证服务注册

```bash
# 等待5-10秒后
TOKEN="<your-token>"
curl -s "http://localhost:8848/nacos/v1/ns/instance/list?accessToken=$TOKEN&serviceName=<service-name>&groupName=LIBRARY_GROUP" | jq .
```

**期望输出**:
```json
{
  "hosts": [
    {
      "ip": "10.50.1.18",
      "port": 8xxx,
      "healthy": true
    }
  ]
}
```

### 4. 重启Gateway（如修改了路由配置）

```bash
# 杀掉Gateway进程
lsof -ti :8080 | xargs kill

# 重启Gateway
cd gateway-service
HOMEBREW_NO_AUTO_UPDATE=1 JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn spring-boot:run
```

### 5. 测试API端点

```bash
# 通过Gateway测试
curl -v http://localhost:8080/api/v1/<your-endpoint>

# 检查响应
# ✅ HTTP 200 OK
# ✅ 单个 access-control-allow-origin 头
# ✅ 正确的JSON数据
```

---

## 配置模板

### 业务服务 application.yml 模板

```yaml
server:
  port: 8xxx  # 您的服务端口

spring:
  application:
    name: your-service  # 服务名称

  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/your_database
    username: postgres
    password: gcrf_secure_2024

  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        username: nacos
        password: nacos
        group: LIBRARY_GROUP  # ⚠️ 必需！

  redis:
    host: localhost
    port: 6379

mybatis-plus:
  mapper-locations: classpath*:mapper/**/*Mapper.xml
  type-aliases-package: com.gcrf.library.yourservice.entity

# 禁用业务服务CORS配置，由Gateway统一处理CORS
library:
  web:
    cors:
      enabled: false  # ⚠️ 必需！

logging:
  level:
    com.gcrf.library: debug
```

### Gateway路由配置模板

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: your-service
          uri: lb://your-service
          predicates:
            - Path=/api/v1/yourpath/**
          # 不使用StripPrefix，保留完整路径 /api/v1/yourpath/**
```

---

## 常用命令速查

### 检查端口占用
```bash
lsof -i :8080  # 检查Gateway
lsof -i :8081  # 检查Auth Service
lsof -i :8082  # 检查Book Service
lsof -i :8083  # 检查Circulation Service
lsof -i :8084  # 检查Reader Service
```

### 杀掉服务进程
```bash
lsof -ti :8080 | xargs kill  # 杀掉Gateway
lsof -ti :8084 | xargs kill  # 杀掉Reader Service
```

### 检查Nacos服务列表
```bash
TOKEN="your-token"
curl -s "http://localhost:8848/nacos/v1/ns/instance/list?accessToken=$TOKEN&serviceName=reader-service&groupName=LIBRARY_GROUP" | jq .
```

### 测试API端点
```bash
# 登录获取token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r .data.accessToken)

# 使用token调用API
curl -v -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/readers?pageNum=1&pageSize=20"
```

---

## 问题反馈

如果遇到此清单未覆盖的问题，请：
1. 查看详细测试报告: `Phase3_Integration_Test_Report.md`
2. 查看服务日志: `<service-name>/logs/spring.log`
3. 查看CLAUDE.md了解项目开发规范

**最后更新**: 2025-10-26
