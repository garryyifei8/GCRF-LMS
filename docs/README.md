# 国创睿峰图书馆云平台 — 文档中心

**项目**：国创睿峰智能图书馆云平台（GCRF Regional Library Cloud Platform）
**最后更新**：2026-04-30

> 本平台覆盖**4 个产品形态 + 1 个云中心**：区域云平台、区域 OPAC、微信图书馆、校园图书馆系统。
> 详见 [主设计 spec](specs/2026-04-30-regional-platform-master-design.md)。

---

## 🚀 快速入门

| 你的角色          | 从哪开始                                                                                                                    |
| ----------------- | --------------------------------------------------------------------------------------------------------------------------- |
| 项目经理 / 招标方 | [PRD 总览](prd/00-overview.md) → 各子 PRD                                                                                   |
| 架构师 / 评审     | [主 spec](specs/2026-04-30-regional-platform-master-design.md) → [架构总览](architecture/00-overview.md) → [4 个 ADR](adr/) |
| 开发工程师        | [架构总览](architecture/00-overview.md) → 对应子 [PRD](prd/) → [API 规范](architecture/06-api-spec.md)                      |
| 运维 / SRE        | [部署拓扑](architecture/02-deployment-topology.md) → [Edge Agent 同步协议](architecture/03-edge-agent-sync-protocol.md)     |
| 产品 / UI         | [PRD 总览](prd/00-overview.md) → 各子 PRD 关键页面                                                                          |

---

## 📑 PRD（产品需求）

- [00 顶层 PRD](prd/00-overview.md) — 4 平台总览 + 边界 + 价值主张
- [01 区域云平台](prd/01-regional-cloud-platform.md) — 教育局/区域管理后台
- [02 区域 OPAC](prd/02-regional-opac.md) — 公共检索网站
- [03 微信图书馆](prd/03-wechat-library.md) — 公众号 + 小程序
- [04 校园图书馆](prd/04-campus-library.md) — 学校管理后台（基于现有 GCRF 系统）

---

## 🏗 架构（技术架构）

- [00 顶层架构](architecture/00-overview.md) — 4 平台拓扑 + 微服务清单
- [01 多租户隔离](architecture/01-multi-tenant-isolation.md) — PG schema 隔离模型
- [02 部署拓扑](architecture/02-deployment-topology.md) — 区域中心 K8s + 学校 Edge Agent
- [03 Edge Agent 同步协议](architecture/03-edge-agent-sync-protocol.md) — WSS + 离线借还
- [04 IAM 统一认证](architecture/04-iam-sso.md) — OIDC + 4 种登录方式
- [05 数据模型](architecture/05-data-model.md) — gcrf_region + per-school schema
- [06 API 规范](architecture/06-api-spec.md) — 路径 / 命名 / 错误码 / 限流

---

## 📜 ADR（架构决策记录）

- [ADR-001 多租户策略](adr/ADR-001-multi-tenant-strategy.md) — 共享 PG + per-school schema
- [ADR-002 部署拓扑](adr/ADR-002-deployment-topology.md) — SaaS + Edge Agent
- [ADR-003 微信前端](adr/ADR-003-wechat-frontend-uniapp.md) — uni-app 单源码
- [ADR-004 Edge Agent 栈](adr/ADR-004-edge-agent-sync.md) — Spring Boot + SQLite + WSS

---

## 📐 主 Spec（唯一真相源）

- [2026-04-30 区域平台主设计](specs/2026-04-30-regional-platform-master-design.md)
- 历史 spec（Phase 1 时期）：见 [`specs/`](specs/) 内 2026-04-13 ~ 2026-04-22 系列

---

## 🛠 部署 / 运维

- [当前 K8s 部署清单](deployment/) — gcrf-prod 命名空间，3 节点集群
- [测试报告 2026-04-22](deployment/TEST_REPORT_20260422.md) — 96% 通过率
- [测试计划](deployment/TEST_PLAN.md) — 30 项 API 测试

---

## 🧪 测试

- [测试相关](testing/)
- 上线反馈（历史）：[testing/test_feedback060430.md](testing/test_feedback060430.md)

---

## 📂 进度跟踪

- [开发进度](development/DEVELOPMENT_PROGRESS.md) — 当前迭代与待办
- [配置清单](development/MICROSERVICE_CONFIG_CHECKLIST.md)
- [Chrome 调试指南](development/chrome-debugging-guide.md)

---

## 🗄 归档（历史文档）

历史文档归档在 [archives/](archives/)，按时间分组：

- [legacy-2026-04/](archives/legacy-2026-04/) — 2026-04-30 整理：DevPlan、library-backend、doc、PHASE6、STAGE15、QUICKSTART 等
- [phase15_cleanup/](archives/phase15_cleanup/) — Stage 15 清理
- 单文件：`PRD.md`、`DevPlan.md`、`PHASE2_DETAILED_PLAN.md`、`Phase1_Development_Plan.md`、`IMPLEMENTATION_PLAN_STAGE14.md`、`IMPLEMENTATION_PLAN_STAGE15.md`

---

## 🤖 工程指南

- [项目根 CLAUDE.md](../CLAUDE.md) — 开发约定（Java 21 / PG / Vue 3）
- [Harness Engineering 配置手册](Harness-Engineering-标准配置手册.md)

---

## 📡 系统当前状态（2026-04-30）

- **后端**：8 个微服务上线（auth/book/circulation/reader/system/notify/analytics/chat），144 个测试用例 100% 通过
- **前端**：Vue 3 + Element Plus，含 Phase 1 完工功能（消息中心 / 品牌设置 / Edge 图标按钮）
- **部署**：K8s `gcrf-prod`，节点 192.168.1.19/20/21
- **测试**：在线 30 项 API 96% 通过

下一阶段（M1，4-6 周）：见 [主 spec Appendix C](specs/2026-04-30-regional-platform-master-design.md)

- 新增 6 个微服务（org / standard / opac / wechat / sync / recommend）
- 区域云后台 + IAM 升级 + OPAC 基础检索

---

**反馈与贡献**：发 issue 到本仓 GitHub / 私信项目组。
