# 校园图书馆管理系统 PRD

**日期：** 2026-04-30
**状态：** Approved
**主用户：** 校馆员（学校管理员、馆长、馆员、操作员）
**主 spec：** [`docs/specs/2026-04-30-regional-platform-master-design.md`](../specs/2026-04-30-regional-platform-master-design.md)
**当前实现：** 本仓 `backend/` + `web-admin/`（已完成 Phase 1）

---

## 1. 与现有 GCRF 系统的关系

本 PRD 在现有 GCRF 校园系统之上**做加法**：

- ✅ 已有：编目（部分）、流通（借/还/续/预约）、读者管理、盘点、统计
- ➕ 新增：CNMARC/USMARC 自动获取、馆藏地批量转移、智能馆藏分析、智能采购推荐
- ➕ 新增：自定义角色 + 功能权限、Edge Agent 离线借还
- 🔄 升级：组织结构对接区域云、IAM 升级为 OIDC

## 2. 角色与权限

| 角色                | 权限                           |
| ------------------- | ------------------------------ |
| SCHOOL_ADMIN        | 本校全权                       |
| SCHOOL_LIBRARY_HEAD | 馆藏 + 用户 + 报表             |
| LIBRARIAN           | 借还 + 编目 + 盘点             |
| OPERATOR            | 借还工位（最小权限）           |
| TEACHER             | 借阅 + 推荐购书 + 班级阅读报表 |

**自定义角色** ⭐（招标硬性条款）：支持按"功能模块 × 数据范围"组合自定义角色。

## 3. 功能模块（来自需求一/二两大块）

### 3.1 编目管理

#### 3.1.1 编目入库

**功能**：

- 新增 / 修改 / 查询编目记录
- 自定义编目项（支持 CNMARC + USMARC）
- 自动获取 MARC（输 ISBN → 自动拉取国家图书馆 / 第三方 API）
- 复本入库：输开始条码 + 复本数 → 连续生成
- 批量修改种次号
- 导出 Excel

**关键 API**：

- `POST /api/v1/catalog/marc/import`（上传 MARC 文件 / ISBN 批量）
- `GET /api/v1/catalog/marc/by-isbn/{isbn}`
- `POST /api/v1/catalog/copies/batch`（开始条码 + 数量）
- `PATCH /api/v1/catalog/{id}/seed-number`（种次号批量修改）
- `GET /api/v1/catalog/export?format=xlsx`

#### 3.1.2 总分馆联合编目 ⭐

**功能**：

- 总馆/分馆共享编目
- 各馆编目信息统一标准管理
- 信息共享（其他馆已编目时复用 MARC）

**关键 API**：

- `GET /api/v1/catalog/region-library?isbn=`（查区域总库）
- `POST /api/v1/catalog/copy-from-region/{isbn}`（从区域库复制 MARC）

#### 3.1.3 报表与书标打印

**功能**：

- 编目入库报表（按时间段 / 编目人 / 分类）
- 书标打印（多规格）：按 ISBN / 题名 / 分类号 / 条码范围 / 入库日期 / 编目人 查询
- 单个 / 批量打印

**关键 API**：

- `GET /api/v1/catalog/reports?type=intake&from=&to=`
- `POST /api/v1/print/labels?range=&isbn=&size=A4|TZ`

#### 3.1.4 新书登记 + 馆藏地转移

**功能**：

- 导入 CNMARC / USMARC 数据
- 按分类号 / 条码范围 / 入库日期 批量转移图书到其他馆藏地

**关键 API**：

- `POST /api/v1/catalog/marc/upload`
- `POST /api/v1/catalog/copies/transfer?from=&to=&filter=`

#### 3.1.5 智能馆藏分析 ⭐

> 招标硬性条款：投标人须提供截图

**功能**：

- 综合评分（图书数量综合 + 图书质量 + 图书结构 + 各维度分析）
- 对比本校馆藏 vs 推荐书单 vs 优质图书数据库
- 雷达图展示 5 大类 22 小类

**关键 API**：

- `GET /api/v1/intelligence/collection-analysis?node=`
- `POST /api/v1/intelligence/collection-analysis/generate`（异步生成，落库报告）
- `GET /api/v1/intelligence/collection-analysis/reports`

#### 3.1.6 智能图书推荐 ⭐（采购）

> 招标硬性条款：投标人须提供截图

**功能**：

- 基于本校馆藏书目 + 各级推荐书目 + 借阅数据 + 采购数据库
- 推荐采购书单（含分数 + 理由）
- 一键加入采购清单

**关键 API**：

- `GET /api/v1/intelligence/purchase-suggestions?budget=&category=`
- `POST /api/v1/purchase-list`（加入待采购）

#### 3.1.7 微信图书馆活动管理

**功能**：

- 活动创建 → 提交至微信图书馆首页展示栏
- 活动设置：封面 / 标题 / 说明 / 活动书目 / 答题任务 / 评分榜

**关键 API**：

- `POST /api/v1/wx-activities`
- `PUT /api/v1/wx-activities/{id}`
- `POST /api/v1/wx-activities/{id}/publish`

#### 3.1.8 角色管理 ⭐

> 招标硬性条款：投标人须提供截图

**功能**：

- 新建角色 + 配置功能权限（按模块）
- 数据范围（SELF / CLASS / GRADE / SCHOOL）

**关键 API**：

- `GET /api/v1/roles`
- `POST /api/v1/roles`
- `PUT /api/v1/roles/{id}/permissions`

### 3.2 流通管理（已有，本轮微调）

- 借/还/续借/预约（已有）
- 扫码借还（Edge Agent 在工位浏览器中扫码 → LAN HTTP → Edge Agent → 云端）
- 逾期判断、挂失锁定（已有）
- 损坏 / 丢失剔旧登记（待补 — 加入图书状态字典）
- 借阅规则自动校验（已有，在 `circulation-service`）
- **微信预约 + 通知**（接入 `wechat-service`）

### 3.3 读者管理（已有，本轮微调）

- 学生 / 教师分类管理（已有）
- 新增 / 编辑 / 注销 / 停用 / 恢复（已有）
- 按年级批量注销（已有，Phase 1 完成）
- 借阅历史查询（已有，Phase 1 完成）

### 3.4 统计分析（部分已有，本轮扩展）

#### 3.4.1 图书分类统计

- 馆藏的各种图书数量、种数、册数、总价
- 点击分类 → 下一级
- 导出 Excel

#### 3.4.2 馆情数据分析

- 自动化管理日流量统计
- 生均藏书量、生均借书量
- 馆藏质量分析、馆藏分类分析

#### 3.4.3 阅读质量分析 ⭐

- 阅读书目借阅统计
- 阅读资源使用统计
- 阅读认证统计
- 按年级 / 班级 / 分类 统计

#### 3.4.4 学生统计信息

字段：一卡通学号、姓名、阅读数据（书数）、性别、年级、班级
统计类别：全校 / 年级 / 班级 / 时间段
导出 Excel

**关键 API**：

- `GET /api/v1/analytics/school/category-stats?level=`
- `GET /api/v1/analytics/school/circulation?range=`
- `GET /api/v1/analytics/school/reading-quality?level=class|grade|category`
- `GET /api/v1/analytics/school/student-stats?range=&dimension=`
- `GET /api/v1/analytics/school/export?type=&format=xlsx`

### 3.5 一卡通对接

**功能**：

- 与本校一卡通系统数据无缝对接
- 学生信息、借阅卡同步
- 可配置每校一卡通 API endpoint

**关键 API**：

- `POST /api/v1/integrations/card-system/config`
- `POST /api/v1/integrations/card-system/sync`

## 4. Edge Agent 集成

校园系统 + Edge Agent 协同：

- Edge Agent 提供 `localhost:8095` LAN HTTP API
- 校园 web-admin 检测到 Edge Agent 后，借还操作走本地（断网仍可用）
- Edge Agent 异步同步到云端

详见 [`../architecture/03-edge-agent-sync-protocol.md`](../architecture/03-edge-agent-sync-protocol.md)。

## 5. 关键页面

```
┌──────────────────────────────────────────────────────┐
│ 国创睿峰 · 实验小学图书馆                  馆员 admin │
├──────────┬───────────────────────────────────────────┤
│ 概览     │ 借阅概览（已有）                          │
│ 流通管理 │                                           │
│ 图书管理 │ ┌──────────────┐ ┌──────────────┐         │
│  ├编目  │ │ 智能馆藏分析  │ │ 智能采购推荐  │         │
│  ├编目  │ │ 综合评分 87  │ │ 推荐 32 本   │         │
│  ├典藏  │ │ 雷达图...    │ │ 列表...     │         │
│  └盘点  │ └──────────────┘ └──────────────┘         │
│ 读者管理 │                                           │
│ AI 智能  │  最近借阅 | 热门图书 | 阅读测评统计       │
│ 系统管理 │                                           │
└──────────┴───────────────────────────────────────────┘
```

## 6. 验收标准

- [ ] CNMARC / USMARC 通过 ISBN 自动获取
- [ ] 复本入库 100 本批量生成 < 3 秒
- [ ] 智能馆藏分析报告（5 大类 22 小类雷达图）
- [ ] 智能采购推荐书单（带分数 + 理由）
- [ ] 自定义角色：界面可创建并验证生效
- [ ] 阅读质量分析按年级 / 班级 / 分类统计
- [ ] Edge Agent 离线借还闭环（拔网线测试）

## 7. 技术栈

- 前端：Vue 3 + Element Plus（沿用现有 web-admin）
- 后端：现有 GCRF 8 个微服务 + 新增 `recommend-service`（独立出去）
- Edge Agent：Spring Boot 精简 + SQLite（详见 ADR-004）

## 8. 关联文档

- 顶层 PRD：[`00-overview.md`](00-overview.md)
- Edge Agent 同步协议：[`../architecture/03-edge-agent-sync-protocol.md`](../architecture/03-edge-agent-sync-protocol.md)
- 现有 GCRF 系统：本仓 `backend/` 与 `web-admin/`
