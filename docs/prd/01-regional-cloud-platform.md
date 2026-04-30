# 区域图书馆云平台 PRD

**日期：** 2026-04-30
**状态：** Approved
**主用户：** 教育局/区域管理员
**主 spec：** [`docs/specs/2026-04-30-regional-platform-master-design.md`](../specs/2026-04-30-regional-platform-master-design.md)

---

## 1. 角色与权限

| 角色             | 权限                                                   |
| ---------------- | ------------------------------------------------------ |
| REGION_ADMIN     | 区域全权（组织、用户、馆藏标准、不适宜书库、数据中心） |
| REGION_LIBRARIAN | 区域读取 + 馆藏标准维护 + 不适宜书库扫描               |

## 2. 功能模块（来自需求 1-5）

### 2.1 组织架构管理

**功能**：

- 多级组织树（教育局 → 区/县 → 学校 → 分校 → 分馆 → 学段 → 年级 → 班级，最深 6 层）
- 自定义组织节点类型字典（`org_node_type` 表）
- 各级单位增删改本单位用户
- 权限管理：功能权限 + 数据查看权限（`SELF` / `CLASS` / `GRADE` / `SCHOOL` / `REGION`）

**UI**：

- 左侧组织树（el-tree） + 右侧节点详情
- 拖拽调整节点位置
- 批量导入组织结构（Excel）

**关键 API**：

- `GET /api/v1/org/nodes?parentId=&depth=`
- `POST /api/v1/org/nodes`
- `PUT /api/v1/org/nodes/{id}`
- `POST /api/v1/org/nodes/{id}/move`
- `POST /api/v1/org/nodes/import`（Excel 批量）

### 2.2 用户管理

**功能**：

- 多角色管理（管理员、馆员、教师、学生）
- 批量导入用户（CSV/Excel）+ 现有用户打包下载
- 与一卡通对接（学校提供 API → `auth-service` 同步）
- 区域统一账号

**UI**：

- 用户列表（按学校/年级/班级筛选）
- 行内操作：编辑 / 重置密码 / 启停 / 解绑微信
- 顶栏批量导入 / 导出
- 一卡通配置入口（每校单独配 API endpoint）

**关键 API**：

- `GET /api/v1/users?node=&role=&status=&keyword=`
- `POST /api/v1/users`
- `POST /api/v1/users/import`（multipart）
- `GET /api/v1/users/export?node=`（zip）
- `POST /api/v1/users/{id}/reset-password`
- `POST /api/v1/users/sync-from-card-system`（一卡通同步）

### 2.3 馆藏资源管理

**功能**：

- 馆藏检索（条码、索书号、馆藏地、状态、ISBN、书名、作者、分类号）
- 按组织/学校的馆藏统计（柱状图）
- 资源类型：图书 / 期刊 / 电子资源
- 流通数据：借阅量、生均借阅量、人均借阅值
- 区域总馆藏库（< 400 万册），新书入库不断扩充
- 编目命中率 > 90%（区域总库已编目时直接复用 MARC）

**UI**：

- 顶部检索栏（多字段联合）
- 列表/卡片视图切换
- 统计页：分校柱状图、增长曲线、流通率仪表盘

**关键 API**：

- `GET /api/v1/catalog/search?q=&isbn=&clc=&school=&status=`
- `GET /api/v1/catalog/stats?groupBy=school|orgNode&metric=count|growth|circulation`
- `GET /api/v1/catalog/region-library?isbn=`（查总库 MARC，新书编目用）
- `GET /api/v1/catalog/types`（图书/期刊/电子资源分类）

### 2.4 馆藏标准管理 ⭐

> 招标硬性条款：投标人须提供管理界面截图

**功能**：

- 自定义馆藏标准（生均数、馆藏借阅率、馆藏流通率、生均借书量等）
- 按学校类型（小学/初中/高中）配置不同标准值
- **图书馆馆藏质量体系**：自动生成区域/学校的馆藏质量报告
- 按 **5 大类 22 小类** 进行馆藏与标准对比 + 达标比例
- **不适宜图书书库** ⭐：定期扫描馆藏 + 自动下架

**UI**：

- 标准配置页：表单 + 预览生效范围
- 达标分析页：5 大类 22 小类雷达图 + 达标率仪表盘
- 不适宜书库：列表 + 扫描按钮 + 下架审批流
- 馆藏质量报告：可下载 PDF（带电子签）

**关键 API**：

- `GET /api/v1/standards`
- `PUT /api/v1/standards/{key}?schoolType=`
- `POST /api/v1/standards/check?node=`（触发达标测算）
- `GET /api/v1/reports/qualification?node=&format=pdf|xlsx`
- `GET /api/v1/inappropriate-books?source=&page=`
- `POST /api/v1/inappropriate-books`
- `POST /api/v1/inappropriate-books/scan/{schoolId}`（扫描该校馆藏命中清单）
- `POST /api/v1/inappropriate-books/auto-takedown`（自动下架）
- `GET /api/v1/inappropriate-books/takedown-records`

### 2.5 数据管理（数据中心）

**功能**：

- 按组织生成数据展示模块
- 自定义模块：馆藏资源 / 用户 / 流通率 / 热门图书 / 排行榜 / 馆藏分布图 / 借阅喜好
- **数据密钥嵌入**：生成 token，可在甲方 BI 系统嵌入

**UI**：

- 仪表盘自定义编辑器（拖拽 widget）
- 密钥管理：新建 / 撤销 / 续期
- iframe 预览

**关键 API**：

- `GET /api/v1/dashboards?node=`
- `POST /api/v1/dashboards`
- `POST /api/v1/dashboards/{id}/keys`
- `GET /api/v1/dashboards/embed/{key}`（公开，需 key）
- `GET /api/v1/analytics/region/overview?node=`
- `GET /api/v1/analytics/region/by-school?metric=`
- `GET /api/v1/analytics/region/borrow-preference?node=`

## 3. 关键页面（Wireframe 摘要）

```
┌──────────────────────────────────────────────────────┐
│ 顶栏：国创睿峰区域云平台    [搜索] 🔔 admin@教育局     │
├──────────┬───────────────────────────────────────────┤
│ 侧栏     │ 面包屑: 首页 / 馆藏标准管理               │
│          │                                           │
│ 仪表盘   │ Tab: [借阅规则][罚款][预约][品牌][馆藏标准]│
│ 组织架构 │                                           │
│ 用户管理 │  ┌──────────────────────┐               │
│ 馆藏资源 │  │ 雷达图：5 大类达标率   │               │
│ 馆藏标准 │  ├──────────────────────┤               │
│ 不适宜书 │  │ 表格：22 小类对比详情  │               │
│ 数据中心 │  └──────────────────────┘               │
│ 系统管理 │                                           │
└──────────┴───────────────────────────────────────────┘
```

## 4. 验收标准

- [ ] 6 层组织树可创建、移动、删除
- [ ] 用户批量导入 1000 条 CSV < 5 秒
- [ ] 馆藏检索 P95 < 500ms
- [ ] 5 大类 22 小类达标雷达图正确生成
- [ ] 不适宜书库扫描全区 200 校 < 10 分钟
- [ ] 数据密钥嵌入可在第三方 BI 验证

## 5. 技术栈

- 前端：Vue 3 + Element Plus（沿用 GCRF web-admin 风格）
- 后端：`org-service` (8090) + `standard-service` (8091) + 现有 `system-service` (8085) + `analytics-service` (8087)
- 数据库：`gcrf_region` schema（多校共享）

## 6. 关联文档

- 顶层 PRD：[`00-overview.md`](00-overview.md)
- 多租户：[`../architecture/01-multi-tenant-isolation.md`](../architecture/01-multi-tenant-isolation.md)
- 数据模型：[`../architecture/05-data-model.md`](../architecture/05-data-model.md)
