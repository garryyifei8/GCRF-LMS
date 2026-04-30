# ADR-002: 部署拓扑 = SaaS + 校内 Edge Agent

**Status**: Accepted
**Date**: 2026-04-30
**Decider**: GCRF 项目组
**Related**: [主 spec](../specs/2026-04-30-regional-platform-master-design.md) / [02-deployment-topology](../architecture/02-deployment-topology.md)

---

## Context

中国 K12 学校的网络环境复杂：城区学校宽带稳定，乡镇学校网络抖动甚至日断电。
图书馆借还书是高频刚需操作（开学日单校单日 2000+ 次借还），断网时仍需保证业务可用。
但教育局又希望集中管控数据，避免 200 校各自维护本地服务器。

## Considered Options

### A. 集中式 SaaS（区域统一云端，学校 = 浏览器用户）

- 全部跑在区域中心 K8s，学校无任何本地基础设施
- ✅ 运维最简单、数据 100% 在区域中心
- ❌ 网络断 = 业务停（最大风险）
- 适合：网络环境优良地区

### B. 区域中心 + 学校本地节点（边缘部署）

- 学校本地部署校园系统（K8s 或 Docker Compose）
- 数据先在校落库，再异步同步到区域
- ✅ 断网仍能借还
- ❌ 200 校 = 200 套本地基础设施，运维最重
- ❌ 学校 IT 能力不一致，本地 K8s 几乎不可行
- 适合：学校有强私有化诉求

### C. 集中式 SaaS + 校内离线代理 Edge Agent （选定）

- 主流量走区域云
- 学校只部署一台轻量 Edge Agent（一台台式机/小服务器跑 Docker）
- Edge Agent 缓存：本校账号 + 当日借还队列 + 当前在借列表
- 网络恢复后代理把队列重放到云端
- 兼顾在线体验 + 离线韧性，运维比 B 轻 10 倍

## Decision

**选择 C**：集中式 SaaS + 校内 Edge Agent。

## Rationale

- A 的断网风险在乡镇学校无法接受
- B 的运维成本（200 套本地 K8s）几乎不可行
- C 用一台 Mini PC 解决断网问题，硬件成本 1000 元/校（一次性）
- Edge Agent 用 Docker Compose 单机部署，学校 IT 可掌握
- 数据仍以云端为权威，符合教育局合规

## Consequences

### Positive

- 网络断仍可借还（本地 SQLite）
- 200 校仅需 200 台 Mini PC，不需要 200 套 K8s
- 数据 100% 在区域中心（合规集中管理）
- 复用现有 GCRF K8s 集群（已部署 192.168.1.19/20/21）

### Negative / Risks

- Edge Agent 运维成本：每校需有人懂 docker-compose
- 同步冲突需要严格处理（已设计幂等 + 先到先借）
- Edge Agent 升级需要远程推送（设计 OTA 机制）

### Mitigation

- 提供一键部署脚本（`./install-edge-agent.sh school_id deploy_token`）
- 远程监控通道（心跳上报到 sync-service）
- Edge Agent 自动升级（每日检查云端版本，凌晨自动拉取）
- 提供 1 个 Mini PC 推荐型号清单 + 采购指引

## Implementation Notes

- Edge Agent 镜像 `gcrf/edge-agent:latest`（amd64 / arm64 双架构）
- 启动需 `EDGE_DEPLOY_TOKEN` + `EDGE_SCHOOL_ID` 环境变量
- 监听 `:8095` 提供 LAN HTTP API（借还工位浏览器调用）
- 出站 WSS 到 `wss://sync.gcrf.cloud/edge/{schoolId}`
- 本地数据：`/var/lib/gcrf-edge/data.sqlite`（AES-256 加密）

---

**Last Updated**: 2026-04-30
