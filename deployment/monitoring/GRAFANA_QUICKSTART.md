# Grafana 快速入门指南

**版本**: 1.0.0
**更新日期**: 2025-11-01
**Grafana版本**: 10.2.2

---

## 📊 访问 Grafana

启动监控栈后,访问:
- **URL**: http://localhost:3000
- **默认账号**: admin
- **默认密码**: admin

首次登录会提示修改密码(可跳过)。

---

## 🎨 推荐仪表板导入

### 方法1: 通过ID导入(推荐)

1. 点击左侧菜单 `+` → `Import dashboard`
2. 输入以下仪表板ID
3. 选择 `Prometheus` 数据源
4. 点击 `Import`

#### 推荐仪表板列表

| 仪表板名称 | ID | 用途 | 优先级 |
|---------|-----|------|--------|
| **Spring Boot 2.1 System Monitor** | 11378 | JVM、HTTP、数据库连接池监控 | ⭐⭐⭐ 必须 |
| **PostgreSQL Database** | 9628 | 数据库性能、连接、查询统计 | ⭐⭐⭐ 必须 |
| **Redis Dashboard** | 11835 | Redis性能、命中率、内存使用 | ⭐⭐⭐ 必须 |
| **Node Exporter Full** | 1860 | 服务器CPU、内存、磁盘、网络 | ⭐⭐ 推荐 |
| **Spring Boot Statistics** | 6756 | Spring Boot应用统计 | ⭐ 可选 |
| **JVM (Micrometer)** | 4701 | 深入的JVM指标分析 | ⭐ 可选 |

---

### 方法2: 通过JSON文件导入

如果你有自定义的仪表板JSON文件:

1. 将JSON文件放入: `deployment/monitoring/grafana/dashboards/`
2. 重启Grafana: `docker restart gcrf-grafana`
3. Grafana会自动加载新的仪表板

---

## 🔧 常用操作

### 1. 查看所有仪表板
- 左侧菜单 → `Dashboards` → `Browse`

### 2. 搜索仪表板
- 点击顶部搜索框,输入关键词

### 3. 设置首页仪表板
- 打开想要设为首页的仪表板
- 点击右上角 ⭐ (收藏)
- 左侧菜单 → `Dashboards` → `Settings` → `General` → Set as homepage

### 4. 创建文件夹
- 左侧菜单 → `Dashboards` → `New folder`
- 推荐结构:
  ```
  GCRF Library System/
  ├── Infrastructure (Node, Postgres, Redis)
  ├── Backend Services (Gateway, Auth, Book, etc.)
  └── Custom Metrics
  ```

### 5. 调整时间范围
- 右上角时间选择器
- 常用选项:
  - Last 5 minutes
  - Last 15 minutes
  - Last 1 hour
  - Last 24 hours

### 6. 自动刷新
- 右上角刷新按钮旁边的下拉菜单
- 推荐: 15s 或 30s

---

## 📈 关键指标监控

### Spring Boot 服务(11378)

导入后查看以下关键面板:

1. **Quick Facts**
   - Uptime (运行时间)
   - Start Time (启动时间)
   - Heap Used (堆内存使用)
   - Non-Heap Used (非堆内存)

2. **JVM Memory**
   - Heap Memory Usage (堆内存趋势)
   - Non-Heap Memory Usage
   - GC Count & Time (垃圾回收)

3. **HTTP Statistics**
   - Request Count (请求总数)
   - Response Time (P50, P95, P99)
   - Error Rate (错误率)
   - Active Sessions (活跃会话)

4. **DataSource**
   - Active Connections (活跃连接)
   - Idle Connections (空闲连接)
   - Connection Usage (连接使用率)

5. **Threads**
   - Live Threads (活跃线程)
   - Daemon Threads (守护线程)
   - Peak Threads (峰值线程)

---

### PostgreSQL 数据库(9628)

导入后关注:

1. **Database Size**
   - Total Size (总大小)
   - Growth Rate (增长率)

2. **Connections**
   - Active Connections (活跃连接数)
   - Idle Connections (空闲连接)
   - Max Connections (最大连接数)

3. **Queries**
   - Query Rate (查询速率)
   - Transaction Rate (事务速率)
   - Slow Queries (慢查询)

4. **Cache Hit Ratio**
   - > 95% 为良好
   - < 90% 需要优化

5. **Replication Lag**
   - 主从复制延迟
   - 理想值: < 1s

---

### Redis 缓存(11835)

导入后关注:

1. **Memory Usage**
   - Used Memory (已使用内存)
   - Max Memory (最大内存限制)
   - Memory Fragmentation Ratio

2. **Hit Rate**
   - Hits/Misses (命中/未命中)
   - Hit Ratio (命中率)
   - 理想值: > 90%

3. **Commands**
   - Commands/sec (每秒命令数)
   - Slow Commands (慢命令)

4. **Connections**
   - Connected Clients (已连接客户端)
   - Blocked Clients (阻塞客户端)

5. **Persistence**
   - Last Save Time (最后保存时间)
   - Changes Since Last Save

---

### 服务器指标(1860)

导入后关注:

1. **CPU**
   - CPU Usage (CPU使用率)
   - CPU Cores (CPU核心数)
   - Load Average (负载平均值)

2. **Memory**
   - Memory Usage (内存使用率)
   - Available Memory (可用内存)
   - Swap Usage (交换分区使用)

3. **Disk**
   - Disk Usage (磁盘使用率)
   - Disk I/O (磁盘读写)
   - Inode Usage (inode使用率)

4. **Network**
   - Network Traffic (网络流量)
   - Packet Rate (数据包速率)
   - Error Rate (错误率)

---

## ⚠️ 告警阈值建议

### 服务健康
- ✅ 正常: Response Time P95 < 500ms
- ⚠️ 警告: Response Time P95 > 1s
- 🔴 严重: Response Time P95 > 2s

### JVM内存
- ✅ 正常: Heap Usage < 70%
- ⚠️ 警告: Heap Usage > 85%
- 🔴 严重: Heap Usage > 95%

### 数据库连接
- ✅ 正常: Active Connections < 50
- ⚠️ 警告: Active Connections > 80
- 🔴 严重: Active Connections > 95

### Redis命中率
- ✅ 正常: Hit Ratio > 90%
- ⚠️ 警告: Hit Ratio < 80%
- 🔴 严重: Hit Ratio < 70%

### 服务器CPU
- ✅ 正常: CPU Usage < 70%
- ⚠️ 警告: CPU Usage > 85%
- 🔴 严重: CPU Usage > 95%

---

## 🎨 自定义仪表板示例

### 创建简单的面板

1. 点击 `+ → Dashboard → Add visualization`
2. 选择 `Prometheus` 数据源
3. 输入 PromQL 查询,例如:
   ```promql
   # JVM堆内存使用率
   jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

   # HTTP请求速率
   rate(http_server_requests_seconds_count[5m])

   # 数据库连接池使用率
   hikaricp_connections_active / hikaricp_connections_max * 100
   ```
4. 选择可视化类型 (Time series, Gauge, Bar chart, etc.)
5. 点击 `Apply`

---

## 🔍 故障排查

### 问题1: 仪表板显示"No data"

**原因**:
- Prometheus未成功抓取数据
- 服务未暴露 `/actuator/prometheus` 端点
- 时间范围选择错误

**解决**:
1. 检查 Prometheus targets: http://localhost:9090/targets
2. 确认所有targets状态为 "UP"
3. 调整时间范围为 "Last 15 minutes"
4. 检查服务是否启动: `docker ps`

---

### 问题2: 仪表板导入失败

**原因**:
- 网络问题无法从Grafana官网下载
- 仪表板ID错误
- Grafana版本不兼容

**解决**:
1. 确认网络连接正常
2. 尝试手动下载JSON: https://grafana.com/grafana/dashboards/{ID}
3. 使用"Upload JSON file"方式导入
4. 升级Grafana到最新版本

---

### 问题3: 数据源连接失败

**原因**:
- Prometheus服务未启动
- 网络配置错误

**解决**:
```bash
# 检查Prometheus状态
docker ps | grep prometheus

# 检查网络连接
docker exec gcrf-grafana wget -O- http://prometheus:9090/api/v1/status/config

# 重启Grafana
docker restart gcrf-grafana
```

---

## 📚 更多资源

- **Grafana官方文档**: https://grafana.com/docs/grafana/latest/
- **仪表板市场**: https://grafana.com/grafana/dashboards/
- **PromQL查询语法**: https://prometheus.io/docs/prometheus/latest/querying/basics/
- **Micrometer文档**: https://micrometer.io/docs/registry/prometheus

---

## 🚀 下一步

1. ✅ 导入4个推荐仪表板
2. ✅ 设置自动刷新(15s-30s)
3. ✅ 将常用仪表板加入收藏
4. ⏭️ 配置告警规则 (Phase 4)
5. ⏭️ 创建自定义业务指标仪表板

---

**需要帮助?** 查看完整监控文档: `docs/deployment/MONITORING_GUIDE.md`
