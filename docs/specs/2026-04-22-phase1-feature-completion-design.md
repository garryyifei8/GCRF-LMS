# Phase 1: 上线反馈功能补完设计

**日期：** 2026-04-22
**状态：** Approved
**背景：** 测试人员上线反馈（test_feedback060430.md）列出 9 大模块功能缺失。系统已部署可用，但相对于需求文档存在功能缺口。Phase 1 聚焦 5 个高 ROI 快速可完成的缺口（个人中心、Dashboard 增强、读者增强、系统管理、初始化向导）。

---

## 5 个功能模块

### F1. 个人中心增强

**现有**：`info.vue` (137 行) + `password.vue` (152 行)，UI 已完整。

**新增**：

1. 顶栏铃铛图标 + 红点未读数 + 下拉消息列表
2. `/profile/help` 帮助页（静态使用说明 + FAQ）
3. `/profile/feedback` 问题反馈表单（写入 `system_feedback` 表）

### F2. Dashboard 增强

**新增**：

1. 右下角 floating action button（悬浮 3 按钮：借书 / 还书 / 办证）→ 点击跳路由
2. "导出报表" 按钮接通 → 调 analytics export endpoint，下载 Excel

### F3. 读者管理增强

**新增**：

1. 学生/教师列表每行加 **"借阅历史"** 按钮 → 弹窗调 `GET /api/v1/borrows?readerId=X`
2. 学生页加 **"按年级批量注销"** 按钮 → 弹窗选年级 + 二次确认 → 批量 PATCH `status=CANCELLED`

### F4. 系统管理实现

**现有**：`backup.vue` (364 行) + `config.vue` (284 行) UI 已有，需接通后端。

**实现**：

1. **backup**：「立即备份」→ 后端 `pg_dump gcrf_*` → 生成 `.sql.gz` → 返回下载 URL（保留最近 10 个）
2. **config**：保存表单 → 写入 `system_config` 表（KV 存储：library_name、student_max_borrow、teacher_max_borrow、borrow_days、fine_per_day）

### F5. 管理员初始化向导

**新增**：

- 路由 `/init`（4 步 stepper）：
  1. 欢迎页 + 系统使用说明
  2. 图书馆基本信息（名称、地址、Logo）
  3. 借阅规则（学生最大册数、教师最大册数、借阅天数、罚金/天）
  4. 完成 → 写入 `system_config.initialized=true` + 跳 dashboard
- 路由守卫：登录成功后检查 `system_config.initialized`，无 → 跳 `/init`

---

## 后端改动

### 新增表（gcrf_system 库）

```sql
CREATE TABLE system_config (
    config_key   VARCHAR(100) PRIMARY KEY,
    config_value TEXT,
    description  VARCHAR(500),
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by   BIGINT
);

CREATE TABLE system_feedback (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT,
    user_name    VARCHAR(100),
    title        VARCHAR(200),
    content      TEXT,
    feedback_type VARCHAR(50), -- BUG / FEATURE / OTHER
    status       VARCHAR(20) DEFAULT 'PENDING',
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handled_at   TIMESTAMP,
    handled_by   BIGINT,
    response     TEXT
);

CREATE TABLE system_message (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    title        VARCHAR(200),
    content      TEXT,
    type         VARCHAR(50),  -- SYSTEM / BORROW / RESERVE / FEEDBACK
    is_read      BOOLEAN DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE system_backup (
    id           BIGSERIAL PRIMARY KEY,
    file_name    VARCHAR(255),
    file_size    BIGINT,
    file_path    VARCHAR(500),
    backup_type  VARCHAR(20), -- FULL / INCREMENTAL
    status       VARCHAR(20), -- SUCCESS / FAILED
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 新增/扩展 Controller

**system-service：**

- `GET /api/v1/system/config` → 返回所有配置（kv map）
- `PUT /api/v1/system/config` → 批量保存配置
- `GET /api/v1/system/config/initialized` → 返回 boolean
- `POST /api/v1/system/backup` → 触发备份
- `GET /api/v1/system/backup` → 备份列表
- `GET /api/v1/system/backup/{id}/download` → 下载
- `POST /api/v1/system/feedback` → 提交反馈
- `GET /api/v1/system/feedback?userId=X` → 反馈列表
- `GET /api/v1/system/messages?userId=X` → 消息列表
- `GET /api/v1/system/messages/unread-count?userId=X` → 红点数
- `PUT /api/v1/system/messages/{id}/read` → 标记已读

**reader-service：**

- `GET /api/v1/readers/{id}/borrow-history` → 借阅历史（调 circulation-service 的 Feign）
- `POST /api/v1/readers/batch-cancel-by-grade` body: `{grade, beforeDate}` → 按年级批量注销

**analytics-service：**

- `GET /api/v1/analytics/export/comprehensive-report` → 已存在，需返回 xlsx（当前返回 mock blob）

### 估计工作量

- ~6 个新 Controller endpoint（system）
- ~2 个 Controller endpoint（reader）
- ~1 个 Service 实现（备份 pg_dump）
- ~3 个 Flyway migration（新建 4 个表）

---

## 前端改动

### 新增组件/页面

| 文件                                             | 功能                       |
| ------------------------------------------------ | -------------------------- |
| `src/components/MessageCenter.vue`               | 顶栏铃铛 + 红点 + 下拉列表 |
| `src/views/profile/help.vue`                     | 帮助页（静态）             |
| `src/views/profile/feedback.vue`                 | 问题反馈表单               |
| `src/views/init/index.vue`                       | 初始化向导（4 步）         |
| `src/components/dashboard/QuickActions.vue`      | 右下角悬浮快捷按钮         |
| `src/components/readers/BorrowHistoryDialog.vue` | 借阅历史弹窗               |
| `src/components/readers/BatchCancelDialog.vue`   | 按年级批量注销弹窗         |

### 修改文件

| 文件                             | 变更                                            |
| -------------------------------- | ----------------------------------------------- |
| `src/router/index.js`            | + `/init`、`/profile/help`、`/profile/feedback` |
| `src/layouts/MainLayout.vue`     | 顶栏接入 MessageCenter                          |
| `src/views/dashboard/index.vue`  | 接 export API + 嵌 QuickActions                 |
| `src/views/readers/students.vue` | 加借阅历史按钮 + 批量注销按钮                   |
| `src/views/readers/teachers.vue` | 加借阅历史按钮                                  |
| `src/views/system/backup.vue`    | 接通 API                                        |
| `src/views/system/config.vue`    | 接通 API                                        |
| `src/api/system.js`              | 加配置/备份/反馈/消息 API                       |
| `src/api/readers.js`             | 加 borrow-history + batch-cancel-by-grade       |

---

## 验证

### 自动化

- 后端：每个新 endpoint 加 1 个集成测试
- 前端：每个新组件加 1 个 smoke test

### 手动

- 浏览器走完每个流程：
  - 登录 → 顶栏铃铛 → 看到消息
  - Dashboard → 点导出 → 下载 xlsx 文件
  - Dashboard → 点悬浮"借书" → 跳到 /circulation/borrow
  - 读者列表 → 点"借阅历史" → 弹窗显示历史
  - 系统设置 → 改图书馆名称 → 保存 → 刷新仍生效
  - 备份 → 点立即备份 → 下载 sql.gz
  - 个人中心 → 改密码 → 登出 → 用新密码登录成功
  - 首次部署后访问 → 自动跳 /init → 完成向导 → 跳 dashboard

---

## 执行策略

### Phase A — 后端（2 agents 并行）

- **B1**: system-service 加 4 个表 Flyway + Config/Backup/Feedback/Message 4 个 Controller（~400 行）
- **B2**: reader-service 加 borrow-history + batch-cancel-by-grade 端点（~150 行）

### Phase B — 前端（4 agents 并行，B 阶段在 A 完成后）

- **F1**: MessageCenter + 帮助页 + 反馈页 + 路由
- **F2**: Dashboard QuickActions + Excel 导出接通
- **F3**: 读者借阅历史弹窗 + 按年级批量注销弹窗
- **F4**: backup.vue + config.vue 接通后端
- **F5**: 初始化向导（4 步 stepper） + 路由守卫检查

### Phase C — 部署 + 验证

- 重新打包后端镜像（system + reader）+ 前端镜像
- ctr 导入 + kubectl rollout restart
- 跑 test-online.sh + 浏览器手动验证 5 个新功能
