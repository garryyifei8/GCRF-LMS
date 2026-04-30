# ADR-004: Edge Agent 栈 = Spring Boot + SQLite + WSS

**Status**: Accepted
**Date**: 2026-04-30
**Decider**: GCRF 项目组
**Related**: [主 spec](../specs/2026-04-30-regional-platform-master-design.md) / [03-edge-agent-sync-protocol](../architecture/03-edge-agent-sync-protocol.md)

---

## Context

ADR-002 决定每校部署一台 Edge Agent，断网仍可借还。
现需选择：开发语言 / 本地存储 / 同步通道。

## Considered Options

### 语言

| 选项                              | 优                                            | 劣                    |
| --------------------------------- | --------------------------------------------- | --------------------- |
| **Java Spring Boot 精简（选定）** | 复用 GCRF 团队栈，与云端代码可复用 DTO/Mapper | 镜像 ~200MB，启动较慢 |
| Go                                | 单二进制 ~30MB，启动快                        | 团队需新栈            |
| Node.js                           | 与前端栈统一                                  | 不适合长期跑后台服务  |

### 本地存储

| 选项               | 优                                   | 劣                                        |
| ------------------ | ------------------------------------ | ----------------------------------------- |
| **SQLite（选定）** | 零运维（一个文件）、AES-256 加密简单 | 数据 > 5 万条性能略降（学校场景内不会超） |
| 嵌入式 PG          | 与云端同结构                         | 重，运维与 SQLite 持平却失去 PG 集群能力  |
| 仅内存缓存         | 最快                                 | 重启丢数据，不可接受                      |

### 同步通道

| 选项            | 优                                    | 劣                           |
| --------------- | ------------------------------------- | ---------------------------- |
| **WSS（选定）** | 走 443，企业网友好；双向流；mTLS 标准 | 长连接断线重连要处理         |
| MQTT            | 物联网友好、QoS 灵活                  | 学校网管未必放 1883/8883     |
| HTTP 轮询       | 最简单                                | 实时性差，云端推送学校延迟高 |

## Decision

- **语言**：Java Spring Boot 精简版
- **本地存储**：SQLite + AES-256
- **同步通道**：WSS（双向 mTLS）

## Rationale

- 团队既有 GCRF 全栈 Java，复用成本最低
- Spring Boot 3 + GraalVM Native（后续）可降镜像到 50MB
- SQLite 完全够用（200 校 × 几千读者 × 几万册）
- WSS 在中国大部分学校防火墙开箱直通 443

## Consequences

### Positive

- 复用 GCRF 团队 Java 技能栈，无需新人
- SQLite 零运维（一个文件）
- WSS 走 443，学校防火墙开箱通过
- 与云端 sync-service Java 代码可共享 DTO/Codec

### Negative / Risks

- Java 启动慢（10-15 秒），适合长期运行不适合频繁重启
- 镜像体积 ~200MB（GraalVM Native 后续优化到 50MB）
- WSS 长连接断线重连需要心跳 + 退避策略

### Mitigation

- 提供 systemd unit 守护，崩溃自动拉起
- 启动后在 LAN 上展示"启动中"页面（避免工位扫码失败）
- 后续启用 Spring Native（GraalVM）裁剪镜像

## Implementation Notes

- 项目骨架：`backend/edge-agent/`（独立 Maven 模块）
- Maven 打包：`mvn package -P edge-agent` → `edge-agent.jar`
- Docker 镜像：`gcrf/edge-agent:1.0.0`（amd64 + arm64）
- 启动参数（环境变量）：
  - `EDGE_SCHOOL_ID` / `EDGE_DEPLOY_TOKEN`
  - `EDGE_CLOUD_ENDPOINT=wss://sync.gcrf.cloud`
  - `EDGE_DATA_DIR=/var/lib/gcrf-edge`
  - `EDGE_DB_PASSWORD`（SQLite AES-256 密钥）
- HTTP API：`:8095` 提供 LAN 访问
- 心跳：每 30 秒上报一次状态到云端

---

**Last Updated**: 2026-04-30
