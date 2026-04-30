# ADR-001: 多租户隔离策略 = 共享 PG + per-school schema

**Status**: Accepted
**Date**: 2026-04-30
**Decider**: GCRF 项目组
**Related**: [主 spec](../specs/2026-04-30-regional-platform-master-design.md) / [01-multi-tenant-isolation](../architecture/01-multi-tenant-isolation.md)

---

## Context

区域云平台覆盖一个教育局下 20 ~ 200+ 所学校，单个学校 99% 操作只涉及自己的数据，
但 1% 跨校操作（区域 OPAC 检索、教育局统计、馆藏标准达标测算）需要聚合所有学校数据。
数据隔离方式直接决定数据库设计、运维难度、合规性。

## Considered Options

### A. 共享 DB + tenant_id 字段隔离

- 一套 DB，每张表加 `region_id` / `school_id`
- ✅ 跨校检索/统计简单（OPAC 查全区方便）
- ❌ 学校间数据物理上不隔离，SQL 漏 `WHERE tenant_id` 即泄漏
- ✅ 运维最简单、成本最低
- 适合：纯 SaaS、无私有化诉求

### B. 共享 DB + 每校独立 schema （选定）

- 一个 PG 实例，每校一个 schema (`school_001`, `school_002`...)
- 跨校查询通过区域公共 schema + 物化视图
- ✅ 学校间逻辑隔离，加固后难以串库
- ✅ 备份/迁移可按校做（`pg_dump --schema=`）
- ✅ PG 原生支持 1000+ schema，元数据压力可控
- ⚠ 运维比 A 复杂（schema 创建脚本 + 迁移）

### C. 每校独立 DB

- 每校一个 PG DB（或同实例独立 DB）
- ✅ 学校私有化部署友好（DB 可拉回本地）
- ❌ 跨校检索需要 ETL → 区域数据中心
- ❌ 200 校 = 200 个 DB，运维成本最高
- 适合：合规要求严苛、学校强诉求私有化

## Decision

**选择 B**：共享 PostgreSQL 实例 + 每校独立 schema + 区域公共 schema。

## Rationale

- 大部分中国教育局区域平台属于**集中托管 SaaS**场景，A 太弱、C 太重，B 是最优 trade-off
- 学校 99% 操作走自己的 schema，逻辑隔离强，避免跨校 SQL 误伤
- 跨校 OPAC 检索通过 ES 聚合 + 物化视图，性能与 A 接近
- 备份/合规友好（可按校 pg_dump，可按校加密）
- 后续若某校强诉求私有化，可平滑迁移到 C（保留为退路）

## Consequences

### Positive

- 跨校 OPAC 检索可走物化视图，性能可控
- 单校查询完全在自己 schema，逻辑隔离
- 备份/迁移可按校做
- PG 元数据压力可承受（社区案例 1000+ schema）

### Negative / Risks

- schema 创建脚本必须严格幂等（200 校批量初始化）
- Flyway migration 每校都要跑（用 schema 占位符模板 + per-school 应用）
- 跨 schema 查询在某些 ORM 框架（如 JPA）需要手写 SQL
- Schema 数过多时（>1000）`pg_class` / `pg_namespace` 元数据查询会变慢，需监控

### Mitigation

- 使用 PostgreSQL 提供的 `search_path` 切换当前 schema，业务代码无感
- 每次跨 schema 查询都通过 `org-service` 路由，禁止业务代码硬编码 schema 名
- 监控 `pg_class` 体积，预留切到独立 DB 的迁移路径

## Implementation Notes

- `org_node` 表存 `tenant_schema` 字段（如 `school_001`）
- API Gateway 解析 JWT 中的 `tenant`，注入 PG 连接 `SET search_path TO school_001, gcrf_region;`
- Flyway migration 路径：`backend/<service>/src/main/resources/db/migration/per-school/V*.sql`
- 跨校查询写在 `gcrf_region` schema 的视图或物化视图中

---

**Last Updated**: 2026-04-30
