# GCRF Library 监控指标故障排查指南

**版本**: 1.0.0
**更新日期**: 2025-11-01

---

## 🚨 紧急故障响应流程

### 1. 识别问题
- 检查 Prometheus Alerts: http://localhost:9090/alerts
- 检查 Grafana 仪表板异常
- 检查用户报告或监控告警通知

### 2. 快速诊断
```bash
# 一键健康检查
cd deployment/scripts
./test-monitoring.sh  # (Task 6 将创建此脚本)
```

### 3. 应急处理
- Critical 告警: 立即处理 (< 5分钟响应)
- Warning 告警: 30分钟内响应
- 记录所有操作和变更

---

## 📊 常见监控问题

### 问题 1: Prometheus Target DOWN

**症状**: Target 状态显示 DOWN, 无法采集指标

**诊断步骤**:
```bash
# 1. 检查服务是否运行
docker ps | grep <service-name>

# 2. 检查服务日志
docker logs <service-name> --tail 100

# 3. 检查网络连通性
docker exec gcrf-prometheus ping <service-name>
docker exec gcrf-prometheus wget -O- http://<service>:<port>/actuator/prometheus

# 4. 检查 Actuator 配置
curl http://localhost:<port>/actuator | jq '.["_links"]'
```

**可能原因**:
- 服务未启动或已崩溃
- 网络问题 (Docker 网络配置错误)
- Actuator 未正确配置
- 端口冲突

**解决方案**:
```bash
# 重启服务
docker restart <service-name>

# 检查 application.yml 配置
# 确认 management.endpoints.web.exposure.include 包含 prometheus

# 验证端点可访问
curl http://localhost:<port>/actuator/prometheus
```

---

### 问题 2: Grafana 仪表板无数据

**症状**: Grafana 仪表板显示 "No data" 或空白

**诊断步骤**:
```bash
# 1. 检查 Prometheus 是否运行
curl http://localhost:9090/-/healthy

# 2. 手动查询 Prometheus
curl 'http://localhost:9090/api/v1/query?query=up' | jq

# 3. 检查数据源配置
# Grafana UI → Configuration → Data Sources → Prometheus
# URL: http://prometheus:9090
```

**可能原因**:
- Prometheus 未运行
- Grafana 数据源配置错误
- 时间范围选择错误
- 查询语法错误

**解决方案**:
```bash
# 重启 Prometheus 和 Grafana
docker restart gcrf-prometheus gcrf-grafana

# 重新配置数据源 (Grafana UI)

# 调整时间范围: Last 15 minutes

# 简化查询,测试基础指标
# 查询: up
```

---

### 问题 3: 指标数据不完整

**症状**: 部分指标缺失,数据断断续续

**诊断步骤**:
```bash
# 1. 检查 Prometheus 存储空间
docker exec gcrf-prometheus df -h /prometheus

# 2. 检查采集间隔配置
# prometheus.yml: scrape_interval: 15s

# 3. 检查服务 CPU/内存
docker stats
```

**可能原因**:
- 磁盘空间不足
- Prometheus 资源不足
- 采集超时 (scrape_timeout)
- 服务响应慢

**解决方案**:
```bash
# 清理磁盘空间
docker system prune -a

# 增加 Prometheus 资源限制 (docker-compose.monitoring.yml)
resources:
  limits:
    memory: 2G
    cpus: '2'

# 调整采集超时
scrape_timeout: 15s
```

---

### 问题 4: 告警规则未触发

**症状**: 指标超过阈值但告警未触发

**诊断步骤**:
```bash
# 1. 检查告警规则是否加载
curl http://localhost:9090/api/v1/rules | jq '.data.groups[] | .name'

# 2. 手动测试 PromQL 表达式
# Prometheus UI → Graph → 粘贴告警表达式

# 3. 检查告警评估日志
docker logs gcrf-prometheus | grep "evaluating rule"
```

**可能原因**:
- 告警规则语法错误
- `for` 时间未满足
- 表达式返回空结果
- 告警规则文件未加载

**解决方案**:
```bash
# 验证规则语法
docker exec gcrf-prometheus promtool check rules /etc/prometheus/alerts/*.yml

# 热重载配置
curl -X POST http://localhost:9090/-/reload

# 检查告警状态
curl http://localhost:9090/api/v1/alerts | jq
```

---

### 问题 5: JVM 堆内存持续增长

**症状**: `jvm_memory_used_bytes{area="heap"}` 持续上升,不回落

**诊断步骤**:
```bash
# 1. 查看 JVM 内存趋势 (Grafana)

# 2. 检查 GC 频率
# 指标: jvm_gc_pause_seconds_count

# 3. 生成 Heap Dump
docker exec <container> jmap -dump:live,format=b,file=/tmp/heap.hprof 1

# 4. 分析 Heap Dump (本地)
docker cp <container>:/tmp/heap.hprof ./
# 使用 Eclipse MAT 或 JProfiler 分析
```

**可能原因**:
- 内存泄漏
- 对象生命周期过长
- 缓存未设置过期时间
- 堆内存设置过小

**解决方案**:
```bash
# 分析 Heap Dump,找出占用内存最多的对象

# 优化代码,修复内存泄漏

# 增加堆内存 (Dockerfile ENV)
ENV JAVA_OPTS="-Xms1g -Xmx2g"

# 调整 GC 参数
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

---

### 问题 6: 数据库连接池耗尽

**症状**: `hikaricp_connections_active / hikaricp_connections_max` 接近100%

**诊断步骤**:
```bash
# 1. 检查活跃连接数
# Grafana 查看: hikaricp_connections_active

# 2. 检查等待连接数
# 指标: hikaricp_connections_pending

# 3. 检查数据库慢查询
docker exec gcrf-postgres-primary psql -U postgres -d <database> -c "
  SELECT pid, usename, application_name, state, query_start, query
  FROM pg_stat_activity
  WHERE state = 'active'
  ORDER BY query_start;
"
```

**可能原因**:
- 慢查询占用连接过久
- 连接池配置过小
- 连接泄漏 (未正确关闭)
- 并发请求过多

**解决方案**:
```yaml
# 增加连接池大小 (application.yml)
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # 增加到20
      minimum-idle: 5
      connection-timeout: 30000

# 优化慢查询 (添加索引)

# 检查代码中是否有连接泄漏
# 确保使用 try-with-resources 或 @Transactional
```

---

### 问题 7: Redis 缓存命中率低

**症状**: `redis_keyspace_hits / (redis_keyspace_hits + redis_keyspace_misses)` < 70%

**诊断步骤**:
```bash
# 1. 检查命中率
docker exec gcrf-redis-master redis-cli INFO stats | grep keyspace

# 2. 检查缓存 Key 分布
docker exec gcrf-redis-master redis-cli --scan --pattern "*" | head -20

# 3. 检查 Key 过期时间
docker exec gcrf-redis-master redis-cli TTL <key>
```

**可能原因**:
- 缓存预热不足
- TTL 过短
- 缓存 Key 设计不合理
- 缓存穿透

**解决方案**:
```java
// 增加缓存时间
@Cacheable(value = "books", key = "#id", ttl = 3600)  // 1小时

// 缓存预热 (应用启动时)
@PostConstruct
public void warmupCache() {
    List<Book> hotBooks = bookService.getHotBooks();
    hotBooks.forEach(book -> cacheManager.put(book.getId(), book));
}

// 缓存空值,防止穿透
@Cacheable(value = "books", key = "#id", unless = "#result == null")
public Book getBook(Long id) {
    return bookRepository.findById(id).orElse(null);
}
```

---

### 问题 8: HTTP 响应时间过高

**症状**: P95 响应时间 > 2秒

**诊断步骤**:
```bash
# 1. 查看 Grafana 定位慢接口
# 按 uri 分组查看响应时间

# 2. 检查应用日志
docker logs <service> --tail 100 | grep "slow"

# 3. 检查数据库慢查询
docker exec gcrf-postgres-primary psql -U postgres -d <database> -c "
  SELECT query, mean_exec_time, calls
  FROM pg_stat_statements
  ORDER BY mean_exec_time DESC
  LIMIT 10;
"

# 4. 检查是否有锁等待
# 指标: jvm_threads_states{state="blocked"}
```

**可能原因**:
- 数据库慢查询
- 缺少索引
- N+1 查询问题
- 外部API调用超时
- 锁竞争

**解决方案**:
```java
// 优化数据库查询 - 添加索引
CREATE INDEX idx_book_title ON book(title);

// 优化 N+1 查询 - 使用 JOIN FETCH
@Query("SELECT b FROM Book b LEFT JOIN FETCH b.author WHERE b.id = :id")
Book findByIdWithAuthor(@Param("id") Long id);

// 添加缓存
@Cacheable(value = "books", key = "#id")
public Book getBook(Long id) { ... }

// 外部API调用设置超时
RestTemplate restTemplate = new RestTemplate();
SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
factory.setConnectTimeout(3000);
factory.setReadTimeout(3000);
restTemplate.setRequestFactory(factory);
```

---

## 🔍 调试技巧

### 1. PromQL 查询调试

```promql
# 查看原始指标
jvm_memory_used_bytes

# 查看特定服务
jvm_memory_used_bytes{application="auth-service"}

# 计算百分比
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100

# 计算速率 (QPS)
rate(http_server_requests_seconds_count[5m])

# 计算 P95 响应时间
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# 按 URI 分组
sum by (uri) (rate(http_server_requests_seconds_count[5m]))
```

### 2. Grafana 变量使用

创建变量过滤服务:
- Variable name: `service`
- Query: `label_values(up, job)`
- 在查询中使用: `up{job="$service"}`

### 3. 日志关联

```bash
# 查找错误日志中的 Request ID
docker logs gcrf-auth-service | grep "Request ID: xxx"

# 在 Prometheus 查询该时间段的指标
# 时间选择器: 调整到日志时间前后5分钟
```

---

## 📋 故障排查检查清单

### 服务宕机
- [ ] 检查 Docker 容器状态: `docker ps -a`
- [ ] 检查服务日志: `docker logs <service>`
- [ ] 检查依赖服务 (Nacos, PostgreSQL, Redis)
- [ ] 检查端口冲突: `lsof -i :<port>`
- [ ] 重启服务: `docker restart <service>`

### 性能问题
- [ ] 查看 CPU/内存使用率 (Grafana)
- [ ] 检查 JVM 堆内存趋势
- [ ] 检查 GC 频率和暂停时间
- [ ] 检查数据库慢查询
- [ ] 检查 Redis 命中率
- [ ] 检查网络延迟

### 监控系统本身
- [ ] 检查 Prometheus 磁盘空间
- [ ] 检查 Prometheus 采集延迟
- [ ] 检查告警规则加载状态
- [ ] 检查 Grafana 数据源配置
- [ ] 验证指标数据完整性

---

## 🛠️ 常用诊断工具

### Docker 命令
```bash
docker ps                      # 查看运行中的容器
docker logs <container>        # 查看容器日志
docker exec -it <container> sh # 进入容器
docker stats                   # 实时资源使用
docker inspect <container>     # 查看容器详细信息
```

### Prometheus API
```bash
# 查询指标
curl 'http://localhost:9090/api/v1/query?query=up'

# 查询范围数据
curl 'http://localhost:9090/api/v1/query_range?query=up&start=...'

# 查看 targets
curl 'http://localhost:9090/api/v1/targets'

# 查看告警
curl 'http://localhost:9090/api/v1/alerts'
```

### PostgreSQL 查询
```sql
-- 查看活跃连接
SELECT count(*) FROM pg_stat_activity WHERE state = 'active';

-- 查看慢查询
SELECT * FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;

-- 查看缓存命中率
SELECT
  sum(blks_hit)::float / (sum(blks_hit) + sum(blks_read)) as cache_hit_ratio
FROM pg_stat_database;
```

### Redis 命令
```bash
# 查看信息
redis-cli INFO

# 查看内存使用
redis-cli INFO memory

# 查看连接数
redis-cli CLIENT LIST

# 查看慢查询
redis-cli SLOWLOG GET 10
```

---

## 📞 升级流程

### 何时升级

1. **Critical 告警未在15分钟内解决**
2. **多个服务同时故障**
3. **数据丢失风险**
4. **安全事件**

### 升级联系人

- **DevOps Lead**: (联系方式)
- **Backend Lead**: (联系方式)
- **On-call Engineer**: (联系方式)

---

**版本**: 1.0.0
**最后更新**: 2025-11-01
**维护团队**: GCRF Library DevOps Team
