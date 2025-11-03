# Spring Boot Actuator 配置模板

**版本**: 1.0.0
**更新日期**: 2025-11-01
**适用于**: Spring Boot 3.2.2 + Micrometer + Prometheus

---

## 📋 标准配置模板

所有微服务的 `application.yml` 都应包含以下 Actuator 配置:

```yaml
# Actuator配置 - 健康检查与监控
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      service: <service-name>  # 替换为实际服务名，如: auth-service, book-service
  health:
    defaults:
      enabled: true
```

---

## 🔧 配置说明

### 1. Endpoints 暴露

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

**暴露的端点**:
- `health` - 健康检查端点，用于监控服务是否存活
- `info` - 服务信息端点，展示版本、描述等
- `metrics` - 指标端点，展示所有可用指标
- `prometheus` - Prometheus 专用格式的指标端点 ⭐

**访问路径**:
- Health: `http://localhost:8080/actuator/health`
- Prometheus: `http://localhost:8080/actuator/prometheus`

**⚠️ 注意**: 只暴露必要的端点，不要使用 `include: '*'` 暴露所有端点（安全风险）

---

### 2. Health Endpoint 配置

```yaml
management:
  endpoint:
    health:
      show-details: always
  health:
    defaults:
      enabled: true
```

**show-details 选项**:
- `never` - 只显示 UP/DOWN
- `when-authorized` - 授权后显示详情
- `always` - 始终显示详情（推荐用于内部监控）

**Health 响应示例**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "SELECT 1"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 350000000000
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.0.15"
      }
    }
  }
}
```

---

### 3. Prometheus Metrics 配置

```yaml
management:
  endpoint:
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
```

**Prometheus 端点输出格式**:
```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",application="gateway-service",service="gateway-service",} 1.5728E8

# HELP http_server_requests_seconds Duration of HTTP server request handling
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{application="gateway-service",method="GET",service="gateway-service",status="200",uri="/api/v1/auth/login",} 42.0
http_server_requests_seconds_sum{application="gateway-service",method="GET",service="gateway-service",status="200",uri="/api/v1/auth/login",} 1.234
```

---

### 4. Metrics Tags 配置

```yaml
management:
  metrics:
    tags:
      application: ${spring.application.name}
      service: gateway-service
```

**作用**: 为所有指标添加全局标签，便于在 Prometheus/Grafana 中筛选和分组

**标签示例**:
- `application=gateway-service` - Spring Boot 应用名称（自动获取）
- `service=gateway-service` - 服务标识（手动配置）

**查询示例** (PromQL):
```promql
# 查询特定服务的 JVM 堆内存使用率
jvm_memory_used_bytes{service="gateway-service", area="heap"}

# 查询所有服务的 HTTP 请求速率
rate(http_server_requests_seconds_count[5m])

# 按服务分组统计请求总数
sum by (service) (http_server_requests_seconds_count)
```

---

## 📊 内置指标说明

Spring Boot Actuator + Micrometer 自动提供以下指标:

### JVM Metrics
- `jvm.memory.used` - JVM 内存使用量（堆内存、非堆内存）
- `jvm.memory.max` - JVM 最大内存
- `jvm.gc.pause` - GC 暂停时间
- `jvm.threads.live` - 活跃线程数
- `jvm.classes.loaded` - 加载的类数量

### HTTP Metrics
- `http.server.requests` - HTTP 请求统计（计数、耗时）
  - 标签: `method`, `uri`, `status`, `exception`
- `http.server.requests.active` - 活跃请求数

### Database Metrics (HikariCP)
- `hikaricp.connections.active` - 活跃连接数
- `hikaricp.connections.idle` - 空闲连接数
- `hikaricp.connections.max` - 最大连接数
- `hikaricp.connections.pending` - 等待连接的请求数

### Redis Metrics (Lettuce)
- `lettuce.command.completion` - Redis 命令执行时间
- `lettuce.command.firstresponse` - Redis 首字节响应时间

### System Metrics
- `system.cpu.usage` - 系统 CPU 使用率
- `system.cpu.count` - CPU 核心数
- `process.uptime` - 进程运行时间
- `process.start.time` - 进程启动时间

---

## 🚀 已配置的服务

| 服务名称 | 端口 | Actuator 地址 | Prometheus 端点 | 状态 |
|---------|------|--------------|----------------|------|
| **gateway-service** | 8080 | http://localhost:8080/actuator | http://localhost:8080/actuator/prometheus | ✅ 已配置 |
| **auth-service** | 8081 | http://localhost:8081/actuator | http://localhost:8081/actuator/prometheus | ✅ 已配置 |
| **book-service** | 8082 | http://localhost:8082/actuator | http://localhost:8082/actuator/prometheus | ✅ 已配置 |
| **circulation-service** | 8083 | http://localhost:8083/actuator | http://localhost:8083/actuator/prometheus | ✅ 已配置 |
| **reader-service** | 8084 | http://localhost:8084/actuator | http://localhost:8084/actuator/prometheus | ✅ 已配置 |

---

## 🧪 验证配置

### 1. 本地验证 (服务已启动)

```bash
# 检查健康状态
curl http://localhost:8080/actuator/health | jq

# 获取 Prometheus 指标
curl http://localhost:8080/actuator/prometheus

# 搜索特定指标 (JVM 内存)
curl -s http://localhost:8080/actuator/prometheus | grep "jvm_memory_used_bytes"

# 搜索 HTTP 请求指标
curl -s http://localhost:8080/actuator/prometheus | grep "http_server_requests"
```

### 2. Docker 环境验证

```bash
# 启动监控栈
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/deployment
./scripts/start-monitoring.sh

# 等待服务启动后，检查 Prometheus targets
open http://localhost:9090/targets

# 验证所有服务都是 "UP" 状态
```

### 3. Prometheus Targets 检查

打开 Prometheus UI: http://localhost:9090/targets

**预期状态**:
```
gateway-service (1 / 1 up)
  http://gateway-service:8080/actuator/prometheus ✓ UP

auth-service (1 / 1 up)
  http://auth-service:8081/actuator/prometheus ✓ UP

book-service (1 / 1 up)
  http://book-service:8082/actuator/prometheus ✓ UP

circulation-service (1 / 1 up)
  http://circulation-service:8083/actuator/prometheus ✓ UP

reader-service (1 / 1 up)
  http://reader-service:8084/actuator/prometheus ✓ UP
```

---

## 🔍 故障排查

### 问题 1: /actuator/prometheus 返回 404

**原因**:
- 缺少 `micrometer-registry-prometheus` 依赖
- Actuator 未启用 Prometheus 端点

**解决**:
1. 确认 `common-web/pom.xml` 包含以下依赖:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

2. 确认 `application.yml` 配置正确
3. 重新编译并启动服务

---

### 问题 2: Prometheus 显示服务 DOWN

**原因**:
- 服务未启动
- 网络不通（Docker 网络配置错误）
- Actuator 端点未正确暴露

**解决**:
```bash
# 检查服务状态
docker ps | grep gcrf

# 检查服务日志
docker logs gcrf-gateway-service

# 手动测试端点（从 Prometheus 容器内）
docker exec gcrf-prometheus wget -O- http://gateway-service:8080/actuator/prometheus
```

---

### 问题 3: 指标数据为空

**原因**:
- 服务刚启动，还没有指标数据
- 服务未被访问，HTTP 指标为空
- 数据库连接池未初始化

**解决**:
1. 等待 1-2 分钟让服务预热
2. 发起几次 HTTP 请求触发指标生成
3. 检查 Prometheus 查询: `up{job="gateway-service"}`

---

## 📚 相关文档

- **Prometheus 配置**: `deployment/monitoring/prometheus/prometheus.yml`
- **Grafana 快速入门**: `deployment/monitoring/GRAFANA_QUICKSTART.md`
- **Docker Compose 监控栈**: `deployment/docker-compose.monitoring.yml`
- **启动脚本**: `deployment/scripts/start-monitoring.sh`

---

## 🛠️ 自定义指标

如果需要添加业务自定义指标，可以使用 Micrometer API:

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class BookService {

    private final Counter bookBorrowCounter;

    public BookService(MeterRegistry registry) {
        this.bookBorrowCounter = Counter.builder("book.borrow.count")
            .description("Total number of book borrows")
            .tag("service", "book-service")
            .register(registry);
    }

    public void borrowBook(Long bookId) {
        // 业务逻辑
        bookBorrowCounter.increment();
    }
}
```

---

**需要帮助?** 查看完整监控文档: `docs/deployment/MONITORING_GUIDE.md`
