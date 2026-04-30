# 部署拓扑

**日期：** 2026-04-30
**状态：** Approved
**ADR：** [ADR-002](../adr/ADR-002-deployment-topology.md)

---

## 1. 总览

```
              区域中心 K8s 集群（教育局机房 / 阿里云）
       ┌────────────────────────────────────────────────┐
       │  Master ×3 (HA)                                 │
       │  Worker ×N (按学校规模 ×4 ~ ×10)                │
       │  ─────────────────────────────────────────     │
       │  - 14 个微服务（auth/book/.../sync/wechat）       │
       │  - PG 主从 + 读副本                              │
       │  - Redis Cluster + RabbitMQ + ES + MinIO         │
       │  - Ingress NGINX + Cert-manager                  │
       └────────────────────────────────────────────────┘
                            ▲
                            │  HTTPS / WSS (443)
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
  实验小学 Edge        二中 Edge         …200 校 Edge
  (Mini PC, Docker)    (Mini PC, Docker)
        ▲
        │ LAN HTTP (80)
        ▼
  借还工位浏览器 + 扫码枪
```

## 2. 区域中心规划

### 2.1 集群规模（按 200 校估算）

| 组件                          | 副本           | CPU     | 内存    | 存储        |
| ----------------------------- | -------------- | ------- | ------- | ----------- |
| API Gateway                   | 3              | 1c × 3  | 2g × 3  | -           |
| 业务微服务                    | 各 2           | 1c × 28 | 2g × 28 | -           |
| PostgreSQL                    | 1 主 + 2 从    | 8c × 3  | 32g × 3 | 1TB SSD × 3 |
| Redis                         | 6（3 主 3 从） | 1c × 6  | 4g × 6  | -           |
| RabbitMQ                      | 3              | 1c × 3  | 4g × 3  | 100GB × 3   |
| ElasticSearch                 | 3              | 4c × 3  | 16g × 3 | 500GB × 3   |
| MinIO                         | 4              | 2c × 4  | 4g × 4  | 1TB × 4     |
| 监控（Prom + Loki + Grafana） | 1              | 4c      | 16g     | 500GB       |

**集群最低**：6 节点（3 master + 3 worker），每节点 16c64g，总规模 96c384g。

### 2.2 网络

- **Ingress NGINX** + Let's Encrypt（免费证书）或商用 SSL
- 公网域名：
  - `cloud.gcrf.region` → ① 区域云后台
  - `opac.gcrf.region` → ② OPAC 公开站
  - `wx.gcrf.region` → ③ 微信后端 API
  - `sync.gcrf.region` → Edge Agent WSS（443，可走 SLB）
  - `api.gcrf.region` → 内部统一 API 网关

## 3. 学校 Edge Agent

### 3.1 硬件

**最低**：i3-8100T / 8GB / 256GB SSD / 千兆网卡 / 静态 IP（约 1000 元）

**推荐**：i5-12400 / 16GB / 512GB SSD（约 2000 元，应对开学日峰值）

### 3.2 软件

```bash
# 安装 Docker（学校 IT 操作）
curl -fsSL https://get.docker.com | sh

# 拉取镜像
docker pull gcrf/edge-agent:1.0.0

# 一键部署脚本（云端预生成）
curl -sSL https://gcrf.region/scripts/install-edge.sh | \
  EDGE_SCHOOL_ID=000123 EDGE_DEPLOY_TOKEN=xxx bash
```

`install-edge.sh` 会：

1. 创建 `/var/lib/gcrf-edge` 数据目录
2. 生成 docker-compose.yml
3. `docker-compose up -d`
4. 注册 systemd unit（开机启动 + 崩溃自动拉起）
5. 配置防火墙（仅放 80/LAN，出站 443）

### 3.3 docker-compose.yml（云端模板）

```yaml
version: "3.8"
services:
  edge-agent:
    image: gcrf/edge-agent:1.0.0
    restart: always
    environment:
      EDGE_SCHOOL_ID: ${EDGE_SCHOOL_ID}
      EDGE_DEPLOY_TOKEN: ${EDGE_DEPLOY_TOKEN}
      EDGE_CLOUD_ENDPOINT: wss://sync.gcrf.region
      EDGE_DATA_DIR: /data
      EDGE_DB_PASSWORD: ${EDGE_DB_PASSWORD}
    volumes:
      - /var/lib/gcrf-edge:/data
    ports:
      - "80:8095" # LAN 借还工位访问
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8095/actuator/health"]
      interval: 30s
      retries: 3
```

### 3.4 升级机制（OTA）

- Edge Agent 启动后每天凌晨 03:00 检查云端版本（GET `/sync/version`）
- 发现新版 → 拉取镜像 → 滚动重启自己
- 升级失败回滚（保留上一个镜像 24 小时）

## 4. CI/CD

### 4.1 区域中心

- GitHub Actions / GitLab CI
- 多模块 Maven build → Docker buildx (amd64) → 推到私有 Registry
- ArgoCD 监听 K8s manifest 仓库，GitOps 部署

### 4.2 Edge Agent

- 同构建流程，镜像 tag 加 `edge-` 前缀
- 不走 ArgoCD（学校 docker-compose）
- 各 Edge Agent 自检版本主动拉新版

## 5. 监控告警

| 监控项             | 告警阈值                              |
| ------------------ | ------------------------------------- |
| 微服务可用性       | 任一 deployment ready < desired，告警 |
| API P95 延迟       | > 1s 持续 5 min，告警                 |
| PG 主从延迟        | > 30s，告警                           |
| Edge Agent 心跳    | 5 min 无心跳，告警（短信 + 钉钉）     |
| 同步队列积压       | > 1000 条持续 30 min，告警            |
| ES 集群 yellow/red | 立即告警                              |

## 6. 灾备

- 区域中心：每日全量备份 PG → MinIO；7 日保留
- 学校 Edge Agent：本地 SQLite 加密；丢失后从云端重新拉快照
- 跨机房容灾（可选 Phase 4）：异地 K8s + PG 物理复制

## 7. 关联文档

- [ADR-002 部署拓扑](../adr/ADR-002-deployment-topology.md)
- [03-edge-agent-sync-protocol](03-edge-agent-sync-protocol.md)
- 现有部署：[`../deployment/`](../deployment/)
