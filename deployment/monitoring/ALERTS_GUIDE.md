# GCRF Library 告警规则指南

**版本**: 1.0.0
**更新日期**: 2025-11-01
**Prometheus版本**: 2.48.0

---

## 📋 告警规则概览

本系统配置了 **70+ 条告警规则**,覆盖基础设施、应用服务、JVM、数据库、缓存等多个维度。

### 告警规则文件:

| 文件 | 规则数 | 覆盖范围 |
|------|--------|---------|
| `alerts/infrastructure-alerts.yml` | 40+ | 服务可用性、系统资源、PostgreSQL、Redis |
| `alerts/service-alerts.yml` | 30+ | 服务健康、HTTP性能、JVM、业务指标 |

---

## 🎯 告警分类

### 1. 按严重程度分类

#### Critical (严重)
- **影响**: 服务不可用或即将不可用
- **响应时间**: 立即处理 (5分钟内)
- **示例**:
  - ServiceDown - 服务宕机
  - PostgreSQLDown - 数据库不可用
  - RedisDown - 缓存不可用
  - CriticalCPUUsage - CPU > 95%
  - CriticalMemoryUsage - 内存 > 95%
  - CriticalJVMHeapUsage - JVM 堆内存 > 95%

#### Warning (警告)
- **影响**: 性能下降或资源即将耗尽
- **响应时间**: 30分钟内处理
- **示例**:
  - HighCPUUsage - CPU > 85%
  - HighMemoryUsage - 内存 > 85%
  - HighHTTPResponseTime - 响应时间 > 2s
  - HighHTTPErrorRate - 错误率 > 5%
  - HighJVMHeapUsage - JVM 堆内存 > 85%

#### Info (信息)
- **影响**: 需要关注但不紧急
- **响应时间**: 工作时间内处理
- **示例**:
  - HighNetworkTraffic - 网络流量过高
  - HighErrorLogRate - ERROR 日志激增
  - HighWarnLogRate - WARN 日志持续高频

### 2. 按类别分类

| 类别 | 规则数 | 描述 |
|------|--------|------|
| **availability** | 5 | 服务可用性 (宕机、Exporter宕机) |
| **resource** | 10 | 系统资源 (CPU、内存、磁盘) |
| **database** | 12 | 数据库 (连接、死锁、缓存命中率、复制) |
| **cache** | 10 | 缓存 (Redis内存、命中率、连接) |
| **health** | 3 | 服务健康检查 |
| **performance** | 15 | 性能指标 (响应时间、I/O、慢查询) |
| **jvm** | 10 | JVM指标 (堆内存、GC、线程) |
| **reliability** | 3 | 可靠性 (错误率) |
| **business** | 2 | 业务指标 (借书失败率、登录失败率) |

---

## 📊 关键告警规则详解

### 基础设施告警

#### 1. 服务可用性

```yaml
# 微服务宕机告警
- alert: ServiceDown
  expr: up{job=~"gateway-service|auth-service|..."} == 0
  for: 1m
  severity: critical
```

**触发条件**: 服务在过去1分钟内无法访问
**影响**: 服务完全不可用
**处理步骤**:
1. 检查服务日志: `docker logs gcrf-<service-name>`
2. 检查容器状态: `docker ps -a | grep gcrf`
3. 尝试重启服务: `docker restart gcrf-<service-name>`
4. 检查依赖服务 (Nacos, PostgreSQL, Redis)

---

#### 2. CPU 使用率

```yaml
# CPU 使用率警告 (85%)
- alert: HighCPUUsage
  expr: |
    100 - (avg by (instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 85
  for: 5m
  severity: warning

# CPU 使用率严重 (95%)
- alert: CriticalCPUUsage
  expr: ... > 95
  for: 2m
  severity: critical
```

**触发条件**: CPU 使用率超过阈值并持续指定时间
**影响**: 系统响应变慢,可能失去响应
**处理步骤**:
1. 查看进程 CPU 占用: `docker stats`
2. 检查是否有异常进程: `docker exec <container> top`
3. 检查业务负载是否异常
4. 考虑水平扩容

---

#### 3. 内存使用率

```yaml
- alert: HighMemoryUsage
  expr: |
    (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100 > 85
  for: 5m
  severity: warning
```

**触发条件**: 可用内存低于15%
**影响**: 可能触发 OOM Killer
**处理步骤**:
1. 检查内存使用: `docker stats`
2. 检查是否有内存泄漏: 查看 JVM 堆内存趋势
3. 重启内存占用高的服务
4. 考虑增加内存限制或物理内存

---

#### 4. 磁盘空间

```yaml
- alert: HighDiskUsage
  expr: |
    (1 - (node_filesystem_avail_bytes / node_filesystem_size_bytes)) * 100 > 80
  for: 5m
  severity: warning
```

**触发条件**: 磁盘使用率超过80%
**影响**: 日志无法写入,服务可能异常
**处理步骤**:
1. 检查磁盘使用: `df -h`
2. 清理日志文件: `docker logs --since 1h <container>` (先确认无需保留)
3. 清理 Docker 资源:
   ```bash
   docker system prune -a --volumes
   ```
4. 考虑扩容磁盘

---

### 数据库告警

#### 5. PostgreSQL 连接数

```yaml
- alert: PostgreSQLHighConnections
  expr: |
    (pg_stat_database_numbackends / pg_settings_max_connections) * 100 > 80
  for: 5m
  severity: warning
```

**触发条件**: 连接数超过最大连接数的80%
**影响**: 新连接可能被拒绝
**处理步骤**:
1. 检查连接数:
   ```sql
   SELECT count(*) FROM pg_stat_activity WHERE state = 'active';
   ```
2. 检查连接池配置 (HikariCP):
   - `spring.datasource.hikari.maximum-pool-size`
3. 检查是否有连接泄漏
4. 增加 PostgreSQL max_connections

---

#### 6. PostgreSQL 缓存命中率

```yaml
- alert: PostgreSQLLowCacheHitRatio
  expr: |
    (pg_stat_database_blks_hit / (pg_stat_database_blks_hit + pg_stat_database_blks_read)) * 100 < 90
  for: 10m
  severity: warning
```

**触发条件**: 缓存命中率低于90%
**理想值**: > 95%
**影响**: 查询性能下降,磁盘I/O增加
**处理步骤**:
1. 检查缓存命中率:
   ```sql
   SELECT datname,
          blks_hit::float/(blks_hit+blks_read) as cache_hit_ratio
   FROM pg_stat_database
   WHERE datname NOT IN ('template0', 'template1');
   ```
2. 增加 `shared_buffers` 配置
3. 优化慢查询,减少全表扫描
4. 添加合适的索引

---

### 缓存告警

#### 7. Redis 内存使用率

```yaml
- alert: RedisHighMemoryUsage
  expr: |
    (redis_memory_used_bytes / redis_memory_max_bytes) * 100 > 80
  for: 5m
  severity: warning
```

**触发条件**: 内存使用率超过80%
**影响**: 可能触发淘汰策略或拒绝写入
**处理步骤**:
1. 检查内存使用:
   ```bash
   docker exec gcrf-redis-master redis-cli INFO memory
   ```
2. 检查淘汰策略: `maxmemory-policy`
3. 清理过期 Key:
   ```bash
   redis-cli --scan --pattern "*" | xargs -L 100 redis-cli DEL
   ```
4. 考虑扩容 Redis 内存

---

#### 8. Redis 缓存命中率

```yaml
- alert: RedisLowCacheHitRate
  expr: |
    (redis_keyspace_hits_total / (redis_keyspace_hits_total + redis_keyspace_misses_total)) * 100 < 70
  for: 10m
  severity: warning
```

**触发条件**: 命中率低于70%
**理想值**: > 90%
**影响**: 缓存效果差,数据库压力大
**处理步骤**:
1. 检查命中率:
   ```bash
   docker exec gcrf-redis-master redis-cli INFO stats | grep keyspace
   ```
2. 优化缓存策略:
   - 增加缓存时间 (TTL)
   - 预热热点数据
   - 优化缓存 Key 设计
3. 检查是否有缓存穿透

---

### 应用服务告警

#### 9. HTTP 响应时间

```yaml
- alert: HighHTTPResponseTime
  expr: |
    histogram_quantile(0.95, ...) > 2
  for: 5m
  severity: warning
```

**触发条件**: P95 响应时间超过2秒
**影响**: 用户体验下降
**处理步骤**:
1. 查看 Grafana 仪表板定位慢接口
2. 检查应用日志
3. 检查数据库慢查询:
   ```sql
   SELECT * FROM pg_stat_statements
   ORDER BY mean_exec_time DESC
   LIMIT 10;
   ```
4. 优化业务逻辑或添加缓存

---

#### 10. HTTP 错误率

```yaml
- alert: HighHTTPErrorRate
  expr: |
    (sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (application, uri)
    /
    sum(rate(http_server_requests_seconds_count[5m])) by (application, uri)) * 100 > 5
  for: 5m
  severity: warning
```

**触发条件**: 5xx 错误率超过5%
**影响**: 服务部分功能异常
**处理步骤**:
1. 查看应用日志: `docker logs gcrf-<service> --tail 100`
2. 检查异常堆栈
3. 检查依赖服务状态 (数据库、Redis)
4. 如果是特定接口,考虑临时降级

---

#### 11. JVM 堆内存

```yaml
- alert: HighJVMHeapUsage
  expr: |
    (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100 > 85
  for: 5m
  severity: warning
```

**触发条件**: 堆内存使用率超过85%
**影响**: 频繁 Full GC,性能下降
**处理步骤**:
1. 检查 JVM 内存使用 (Grafana)
2. 检查 GC 日志
3. 生成 Heap Dump 分析:
   ```bash
   docker exec <container> jmap -dump:live,format=b,file=/tmp/heap.hprof <pid>
   ```
4. 增加 JVM 堆内存: `-Xmx` 参数

---

#### 12. GC 频繁

```yaml
- alert: FrequentFullGC
  expr: |
    rate(jvm_gc_pause_seconds_count{action="end of major GC"}[5m]) > 0.1
  for: 5m
  severity: warning
```

**触发条件**: Full GC 频率超过 0.1次/秒
**影响**: 严重影响性能
**处理步骤**:
1. 检查内存泄漏
2. 优化对象生命周期
3. 调整 GC 参数:
   - 使用 G1GC: `-XX:+UseG1GC`
   - 调整堆内存比例
4. 考虑升级 JDK 版本

---

## 🔔 告警通知配置

### AlertManager 配置 (后续集成)

目前告警规则已配置,但 AlertManager 尚未部署。后续可集成 AlertManager 实现:
- 邮件通知
- 钉钉/企业微信通知
- Slack 通知
- PagerDuty 集成

---

## 📈 告警查看

### 1. Prometheus Alerts 页面

访问: **http://localhost:9090/alerts**

- **Inactive**: 未触发的告警规则
- **Pending**: 正在评估的告警 (未满足 `for` 时间)
- **Firing**: 已触发的告警

### 2. Grafana 告警面板

在 Grafana 仪表板中查看:
1. 导入 Prometheus 数据源
2. 创建 Alert List 面板
3. 配置告警规则可视化

---

## 🧪 告警测试

### 手动触发告警测试

#### 1. CPU 使用率告警
```bash
# 在容器内运行 CPU 密集任务
docker exec <container> dd if=/dev/zero of=/dev/null &
# 等待5分钟
# 停止任务
docker exec <container> pkill dd
```

#### 2. 内存使用率告警
```bash
# 分配大量内存 (慎用!)
docker exec <container> stress --vm 1 --vm-bytes 2G --timeout 300s
```

#### 3. 服务宕机告警
```bash
# 停止服务
docker stop gcrf-auth-service
# 等待1分钟观察告警
# 恢复服务
docker start gcrf-auth-service
```

---

## 📋 告警规则维护

### 添加新告警规则

1. 编辑对应的告警文件:
   - 基础设施: `alerts/infrastructure-alerts.yml`
   - 应用服务: `alerts/service-alerts.yml`

2. 遵循命名规范:
   ```yaml
   - alert: CamelCaseAlertName
     expr: <PromQL expression>
     for: <duration>
     labels:
       severity: critical|warning|info
       category: <category>
     annotations:
       summary: "简短描述"
       description: "详细描述,包含当前值和建议"
   ```

3. 重新加载配置:
   ```bash
   # 方式1: 重启 Prometheus
   docker restart gcrf-prometheus

   # 方式2: 热重载 (推荐)
   curl -X POST http://localhost:9090/-/reload
   ```

4. 验证规则:
   - 访问: http://localhost:9090/alerts
   - 检查新规则是否出现
   - 检查是否有语法错误

---

## 🔍 告警规则调试

### 验证 PromQL 表达式

在 Prometheus 查询页面 (http://localhost:9090/graph) 测试表达式:

```promql
# 测试 CPU 使用率
100 - (avg by (instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)

# 测试内存使用率
(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100

# 测试 JVM 堆内存
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100
```

### 检查告警规则状态

```bash
# 查看 Prometheus 日志
docker logs gcrf-prometheus --tail 100

# 检查规则加载情况
curl http://localhost:9090/api/v1/rules | jq
```

---

## 📚 相关文档

- **Grafana 快速入门**: `deployment/monitoring/GRAFANA_QUICKSTART.md`
- **Actuator 配置模板**: `deployment/monitoring/ACTUATOR_CONFIG_TEMPLATE.md`
- **Prometheus 配置**: `deployment/monitoring/prometheus/prometheus.yml`

---

## 🎓 最佳实践

### 1. 告警阈值设置

- **CPU/内存**: Warning 85%, Critical 95%
- **磁盘**: Warning 80%, Critical 90%
- **响应时间**: Warning 2s, Critical 5s
- **错误率**: Warning 5%, Critical 20%
- **数据库连接**: Warning 80%, Critical 95%
- **缓存命中率**: Warning < 90%, Critical < 70%

### 2. 告警持续时间 (for)

- **Critical 告警**: 1-2分钟 (快速响应)
- **Warning 告警**: 5-10分钟 (避免误报)
- **Info 告警**: 10分钟以上

### 3. 告警降噪

- 避免告警风暴: 相关告警分组
- 合理设置 `for` 时间,避免瞬时抖动
- 使用 `severity` 分级,优先处理 Critical

### 4. 告警处理 SLA

- **Critical**: 5分钟内响应,15分钟内解决或升级
- **Warning**: 30分钟内响应,2小时内解决
- **Info**: 工作时间内处理

---

**版本**: 1.0.0
**最后更新**: 2025-11-01
**维护人**: GCRF Library DevOps Team
