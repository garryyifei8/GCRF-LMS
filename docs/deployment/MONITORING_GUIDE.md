# GCRF Library 监控系统完整指南

**版本**: 1.0.0
**更新日期**: 2025-11-01
**适用环境**: Production / Staging

---

## 📚 目录

1. [系统概述](#系统概述)
2. [快速开始](#快速开始)
3. [架构说明](#架构说明)
4. [监控指标](#监控指标)
5. [告警规则](#告警规则)
6. [Grafana 仪表板](#grafana-仪表板)
7. [运维手册](#运维手册)
8. [故障排查](#故障排查)
9. [性能优化](#性能优化)
10. [相关文档](#相关文档)

---

## 系统概述

### 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| **Prometheus** | 2.48.0 | 时间序列数据库,指标采集 |
| **Grafana** | 10.2.2 | 可视化仪表板 |
| **Node Exporter** | 1.7.0 | 服务器指标 (CPU, Memory, Disk) |
| **PostgreSQL Exporter** | 0.15.0 | 数据库指标 |
| **Redis Exporter** | 1.55.0 | 缓存指标 |
| **Spring Boot Actuator** | 3.2.2 | 应用健康检查和指标 |
| **Micrometer** | 1.12.2 | 应用指标采集库 |

### 监控覆盖范围

#### 微服务 (100%)
- ✅ Gateway Service (8080) - API网关
- ✅ Auth Service (8081) - 认证服务
- ✅ Book Service (8082) - 图书服务
- ✅ Circulation Service (8083) - 流通服务
- ✅ Reader Service (8084) - 读者服务

#### 基础设施 (100%)
- ✅ PostgreSQL (5432) - 主数据库
- ✅ Redis (6379) - 缓存
- ✅ Host Server - 宿主机

#### 指标类型
- ✅ 应用性能 (APM) - 响应时间、错误率、吞吐量
- ✅ 基础设施 - CPU、内存、磁盘、网络
- ✅ JVM - 堆内存、GC、线程
- ✅ 数据库 - 连接、查询、缓存命中率
- ✅ 缓存 - Redis 命中率、内存使用
- ✅ 业务指标 - 自定义业务指标

---

## 快速开始

### 1. 启动监控栈

```bash
cd /path/to/project/deployment
./scripts/start-monitoring.sh
```

**输出示例**:
```
================================================
   GCRF Library - Starting Monitoring Stack
================================================

✓ Backend network found
→ Starting monitoring services...
→ Waiting for services to start...
→ Checking service health...

✓ prometheus is running
✓ grafana is running
✓ node-exporter is running
✓ postgres-exporter is running
✓ redis-exporter is running

================================================
   Monitoring Stack Started Successfully!
================================================

Access Points:
  • Prometheus:  http://localhost:9090
  • Grafana:     http://localhost:3000  (admin/admin)
  • Node Exporter: http://localhost:9100/metrics

Next Steps:
  1. Open Grafana: http://localhost:3000
  2. Login with admin/admin
  3. Import recommended dashboards:
     - Spring Boot 2.1 System Monitor (ID: 11378)
     - PostgreSQL Database (ID: 9628)
     - Redis Dashboard (ID: 11835)
     - Node Exporter Full (ID: 1860)
```

### 2. 访问 Grafana

1. 打开浏览器访问: **http://localhost:3000**
2. 登录:
   - Username: `admin`
   - Password: `admin`
   - (首次登录可跳过修改密码)
3. 导入推荐仪表板 (详见 `deployment/monitoring/GRAFANA_QUICKSTART.md`)

### 3. 验证 Prometheus Targets

访问: **http://localhost:9090/targets**

确认所有 targets 状态为 **UP**:
```
✓ gateway-service (1 / 1 up)
✓ auth-service (1 / 1 up)
✓ book-service (1 / 1 up)
✓ circulation-service (1 / 1 up)
✓ reader-service (1 / 1 up)
✓ node-exporter (1 / 1 up)
✓ postgres-exporter (1 / 1 up)
✓ redis-exporter (1 / 1 up)
```

### 4. 验证服务 Actuator 端点

```bash
# Gateway Service
curl http://localhost:8080/actuator/health | jq
curl http://localhost:8080/actuator/prometheus | head -20

# Auth Service
curl http://localhost:8081/actuator/health | jq

# 查看所有可用端点
curl http://localhost:8080/actuator | jq
```

---

## 架构说明

### 监控架构图

```
┌─────────────────────────────────────────────────────────┐
│                    Grafana (10.2.2)                     │
│               可视化仪表板 (Port 3000)                    │
│            - 数据查询和可视化                             │
│            - 用户界面                                    │
└─────────────────────┬───────────────────────────────────┘
                      │ Query (PromQL)
                      ▼
┌─────────────────────────────────────────────────────────┐
│                 Prometheus (2.48.0)                     │
│            时间序列数据库 (Port 9090)                      │
│            - 15s 采集间隔                                 │
│            - 15天数据保留                                 │
│            - 10GB 存储上限                                │
│            - 70+ 告警规则                                 │
└──┬────┬────┬────┬────┬────┬────┬────┬────────────────────┘
   │    │    │    │    │    │    │    │
   │    │    │    │    │    │    │    └─ /actuator/prometheus
   │    │    │    │    │    │    └──────── :9121/metrics
   │    │    │    │    │    └───────────── :9187/metrics
   │    │    │    │    └────────────────── :9100/metrics
   │    │    │    └─────────────────────── :8084/actuator/prometheus
   │    │    └──────────────────────────── :8083/actuator/prometheus
   │    └───────────────────────────────── :8082/actuator/prometheus
   └────────────────────────────────────── :8081/actuator/prometheus

  ┌────────┐ ┌──────┐ ┌──────┐ ┌────────┐ ┌────────┐
  │Gateway │ │ Auth │ │ Book │ │Circula-│ │Reader  │
  │Service │ │Service│ │Service│ │tion   │ │Service │
  │:8080   │ │:8081 │ │:8082 │ │:8083   │ │:8084   │
  └────────┘ └──────┘ └──────┘ └────────┘ └────────┘
       │        │        │        │         │
       │ Actuator Endpoints:                │
       │ - /actuator/health                 │
       │ - /actuator/metrics                │
       │ - /actuator/prometheus             │
       └────────────────────────────────────┘

  ┌──────────┐ ┌──────────┐ ┌──────────┐
  │   Node   │ │PostgreSQL│ │  Redis   │
  │ Exporter │ │ Exporter │ │ Exporter │
  │  :9100   │ │  :9187   │ │  :9121   │
  └──────────┘ └──────────┘ └──────────┘
       │            │            │
       ▼            ▼            ▼
  ┌────────┐ ┌──────────┐ ┌──────────┐
  │ Host   │ │PostgreSQL│ │  Redis   │
  │ Server │ │  :5432   │ │  :6379   │
  └────────┘ └──────────┘ └──────────┘
```

### 网络架构

```
┌─────────────────────────────────────────────┐
│        gcrf-monitoring-network              │
│  - prometheus                               │
│  - grafana                                  │
│  - node-exporter                            │
│  - postgres-exporter (bridge to backend)    │
│  - redis-exporter (bridge to backend)       │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│         gcrf-backend-network                │
│  - gateway-service                          │
│  - auth-service                             │
│  - book-service                             │
│  - circulation-service                      │
│  - reader-service                           │
│  - postgres-primary                         │
│  - redis-master                             │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│        gcrf-frontend-network                │
│  - web-admin                                │
└─────────────────────────────────────────────┘
```

### 数据流

1. **采集阶段** (Scrape):
   - Prometheus 每15秒主动拉取 (pull) 各个 target 的指标
   - 通过 `/metrics` 或 `/actuator/prometheus` 端点获取数据
   - 数据格式: Prometheus Text Format

2. **存储阶段** (Storage):
   - 时间序列数据存储在本地磁盘
   - 保留15天历史数据
   - 存储上限10GB

3. **查询阶段** (Query):
   - Grafana 通过 PromQL 查询 Prometheus
   - 实时查询或历史数据回溯

4. **告警阶段** (Alerting):
   - Prometheus 每15秒评估告警规则
   - 触发的告警显示在 Prometheus Alerts 页面
   - (未来可集成 AlertManager 发送通知)

---

## 监控指标

详细指标说明见: `deployment/monitoring/ACTUATOR_CONFIG_TEMPLATE.md`

### 核心指标清单

#### 1. JVM 指标
- `jvm_memory_used_bytes{area="heap"}` - 堆内存使用量
- `jvm_memory_max_bytes{area="heap"}` - 堆内存最大值
- `jvm_gc_pause_seconds_sum` - GC 暂停总时间
- `jvm_threads_live` - 活跃线程数

#### 2. HTTP 指标
- `http_server_requests_seconds_count` - HTTP 请求总数
- `http_server_requests_seconds_sum` - HTTP 请求总耗时
- 标签: `method`, `uri`, `status`, `application`

#### 3. 数据库指标 (HikariCP)
- `hikaricp_connections_active` - 活跃连接数
- `hikaricp_connections_idle` - 空闲连接数
- `hikaricp_connections_max` - 最大连接数
- `hikaricp_connections_pending` - 等待连接的请求数

#### 4. Redis 指标
- `redis_memory_used_bytes` - 已用内存
- `redis_keyspace_hits_total` - 缓存命中总数
- `redis_keyspace_misses_total` - 缓存未命中总数
- `redis_connected_clients` - 已连接客户端数

#### 5. PostgreSQL 指标
- `pg_stat_database_numbackends` - 连接数
- `pg_stat_database_blks_hit` - 缓存命中块数
- `pg_stat_database_blks_read` - 磁盘读取块数
- `pg_replication_lag_seconds` - 复制延迟

#### 6. 系统指标
- `node_cpu_seconds_total` - CPU 时间
- `node_memory_MemAvailable_bytes` - 可用内存
- `node_filesystem_avail_bytes` - 可用磁盘空间
- `node_network_receive_bytes_total` - 网络接收字节数

---

## 告警规则

详细告警规则见: `deployment/monitoring/ALERTS_GUIDE.md`

### 告警统计

- **总规则数**: 70+
- **Critical 告警**: 15条 (服务宕机、资源耗尽)
- **Warning 告警**: 40条 (性能下降、资源预警)
- **Info 告警**: 15条 (信息性通知)

### 告警分类

| 类别 | 规则数 | 示例 |
|------|--------|------|
| availability | 5 | ServiceDown, PostgreSQLDown |
| resource | 10 | HighCPUUsage, HighMemoryUsage |
| database | 12 | HighConnections, LowCacheHitRatio |
| cache | 10 | RedisHighMemoryUsage, LowCacheHitRate |
| jvm | 10 | HighJVMHeapUsage, FrequentFullGC |
| performance | 15 | HighHTTPResponseTime, HighErrorRate |

### 告警响应 SLA

- **Critical**: 5分钟内响应,15分钟内解决或升级
- **Warning**: 30分钟内响应,2小时内解决
- **Info**: 工作时间内处理

---

## Grafana 仪表板

详细指南见: `deployment/monitoring/GRAFANA_QUICKSTART.md`

### 推荐仪表板

| ID | 名称 | 用途 | 优先级 |
|----|------|------|--------|
| 11378 | Spring Boot 2.1 System Monitor | JVM、HTTP、数据库连接池 | ⭐⭐⭐ 必须 |
| 9628 | PostgreSQL Database | 数据库性能、连接、查询 | ⭐⭐⭐ 必须 |
| 11835 | Redis Dashboard | Redis性能、命中率、内存 | ⭐⭐⭐ 必须 |
| 1860 | Node Exporter Full | 服务器CPU、内存、磁盘 | ⭐⭐ 推荐 |

### 导入步骤

1. 登录 Grafana: http://localhost:3000
2. 点击左侧菜单 `+` → `Import dashboard`
3. 输入仪表板 ID (如: 11378)
4. 选择 `Prometheus` 数据源
5. 点击 `Import`

### 自定义查询示例

```promql
# JVM 堆内存使用率
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100

# HTTP 请求速率
rate(http_server_requests_seconds_count[5m])

# P95 响应时间
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# 数据库连接池使用率
(hikaricp_connections_active / hikaricp_connections_max) * 100

# Redis 缓存命中率
(redis_keyspace_hits_total / (redis_keyspace_hits_total + redis_keyspace_misses_total)) * 100
```

---

## 运维手册

### 日常运维任务

#### 1. 每日检查 (5分钟)
- [ ] 检查 Grafana 仪表板,查看是否有异常
- [ ] 检查 Prometheus Alerts: http://localhost:9090/alerts
- [ ] 查看 Critical 告警并处理
- [ ] 确认所有服务 targets 状态为 UP

#### 2. 每周检查 (15分钟)
- [ ] 检查磁盘空间使用率
- [ ] 检查 Prometheus 数据存储大小
- [ ] 审查 Warning 告警趋势
- [ ] 检查慢查询日志

#### 3. 每月检查 (1小时)
- [ ] 审查告警规则有效性
- [ ] 优化慢查询和性能瓶颈
- [ ] 检查监控数据完整性
- [ ] 更新仪表板和告警阈值

### 常用运维命令

#### 监控服务管理
```bash
# 启动监控栈
cd deployment && ./scripts/start-monitoring.sh

# 停止监控栈
cd deployment && ./scripts/stop-monitoring.sh

# 重启 Prometheus
docker restart gcrf-prometheus

# 重启 Grafana
docker restart gcrf-grafana

# 查看监控服务日志
docker logs gcrf-prometheus --tail 100
docker logs gcrf-grafana --tail 100
```

#### 数据管理
```bash
# 检查 Prometheus 数据大小
docker exec gcrf-prometheus du -sh /prometheus

# 清理旧数据 (慎用!)
docker exec gcrf-prometheus promtool tsdb delete --time-min=-24h /prometheus

# 备份 Grafana 仪表板
docker exec gcrf-grafana grafana-cli admin export > grafana-backup.json
```

#### 告警管理
```bash
# 热重载 Prometheus 配置 (不重启)
curl -X POST http://localhost:9090/-/reload

# 验证告警规则语法
docker exec gcrf-prometheus promtool check rules /etc/prometheus/alerts/*.yml

# 查看当前触发的告警
curl http://localhost:9090/api/v1/alerts | jq '.data.alerts[] | select(.state=="firing")'
```

---

## 故障排查

详细排查指南见后续创建的 `TROUBLESHOOTING_METRICS.md`

### 快速诊断

#### 问题 1: Prometheus Target DOWN
```bash
# 1. 检查服务是否运行
docker ps | grep <service-name>

# 2. 检查网络连通性
docker exec gcrf-prometheus wget -O- http://<service>:<port>/actuator/prometheus

# 3. 检查服务日志
docker logs <service-name> --tail 50

# 4. 重启服务
docker restart <service-name>
```

#### 问题 2: Grafana 仪表板无数据
```bash
# 1. 检查 Prometheus 是否运行
curl http://localhost:9090/-/healthy

# 2. 检查数据源配置
# Grafana UI → Configuration → Data Sources → Prometheus

# 3. 手动查询 Prometheus
curl 'http://localhost:9090/api/v1/query?query=up'

# 4. 检查时间范围
# Grafana 右上角时间选择器,调整为 "Last 15 minutes"
```

#### 问题 3: 告警规则未触发
```bash
# 1. 检查告警规则是否加载
curl http://localhost:9090/api/v1/rules | jq '.data.groups'

# 2. 检查规则评估间隔
# prometheus.yml: evaluation_interval: 15s

# 3. 手动测试 PromQL
# Prometheus UI → Graph → 输入告警表达式

# 4. 检查 'for' 时间是否满足
# 告警需持续 'for' 时间才会触发
```

---

## 性能优化

### Prometheus 优化

#### 1. 调整采集间隔
```yaml
# prometheus.yml
global:
  scrape_interval: 15s  # 生产环境推荐 15-30s
  scrape_timeout: 10s
```

#### 2. 数据保留策略
```yaml
# docker-compose.monitoring.yml
command:
  - '--storage.tsdb.retention.time=15d'  # 根据磁盘空间调整
  - '--storage.tsdb.retention.size=10GB'
```

#### 3. 查询优化
- 使用 `rate()` 代替 `irate()` 计算平均速率
- 使用 `increase()` 计算增量
- 避免高基数标签 (如: `user_id`, `request_id`)
- 使用 `recording rules` 预计算复杂查询

### Grafana 优化

#### 1. 仪表板性能
- 限制查询时间范围 (默认: Last 1 hour)
- 减少面板数量 (< 15个/页面)
- 使用变量 (Variables) 过滤数据
- 启用查询缓存

#### 2. 告警性能
- 合理设置 `for` 时间,避免频繁评估
- 使用 `group_interval` 减少告警通知频率

---

## 相关文档

### 快速入门文档
- **Grafana 快速入门**: `deployment/monitoring/GRAFANA_QUICKSTART.md`
- **Actuator 配置模板**: `deployment/monitoring/ACTUATOR_CONFIG_TEMPLATE.md`
- **告警规则指南**: `deployment/monitoring/ALERTS_GUIDE.md`

### 配置文件
- **Prometheus 配置**: `deployment/monitoring/prometheus/prometheus.yml`
- **Docker Compose**: `deployment/docker-compose.monitoring.yml`
- **告警规则**: `deployment/monitoring/prometheus/alerts/*.yml`

### 脚本文件
- **启动脚本**: `deployment/scripts/start-monitoring.sh`
- **停止脚本**: `deployment/scripts/stop-monitoring.sh`

---

## 附录

### 端口清单

| 服务 | 端口 | 用途 |
|------|------|------|
| Prometheus | 9090 | Web UI & API |
| Grafana | 3000 | Web UI |
| Node Exporter | 9100 | Metrics endpoint |
| PostgreSQL Exporter | 9187 | Metrics endpoint |
| Redis Exporter | 9121 | Metrics endpoint |
| Gateway Service | 8080 | `/actuator/prometheus` |
| Auth Service | 8081 | `/actuator/prometheus` |
| Book Service | 8082 | `/actuator/prometheus` |
| Circulation Service | 8083 | `/actuator/prometheus` |
| Reader Service | 8084 | `/actuator/prometheus` |

### 数据目录

| 组件 | 数据目录 | 用途 |
|------|---------|------|
| Prometheus | `/prometheus` | 时间序列数据 |
| Grafana | `/var/lib/grafana` | 仪表板、用户、配置 |

---

**版本**: 1.0.0
**最后更新**: 2025-11-01
**维护团队**: GCRF Library DevOps Team
