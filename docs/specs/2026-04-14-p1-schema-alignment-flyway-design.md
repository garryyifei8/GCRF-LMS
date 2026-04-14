# P1: 数据库 Schema 对齐 + Flyway 迁移管理设计

**日期：** 2026-04-14
**状态：** Approved
**优先级：** P1（阻塞 — schema 不一致导致运行时错误）
**背景：** 生产就绪审计发现 circulation 双表并存、reader_types 表缺失、auth schema 含大量死代码、schema 管理无版本控制。

---

## 缺口总览

| #   | 问题                                                    | 修复策略                                                          |
| --- | ------------------------------------------------------- | ----------------------------------------------------------------- |
| 1   | circulation 服务双表并存（borrows+circulation_records） | 统一到 borrows/reserves，删除 legacy CirculationController 和旧表 |
| 2   | reader_types 表缺失                                     | 新建 CREATE TABLE + 初始化数据                                    |
| 3   | 01_auth_service.sql 含 5 张死表                         | 清理，只保留 users                                                |
| 4   | 5 个服务无 Flyway 版本控制                              | 为 auth/circulation/reader/system/notification 创建 V001 基线     |

---

## Section 1：Circulation 统一

### 删除的文件

**Java 源码：**

- `backend/circulation-service/src/main/java/com/gcrf/library/circulation/controller/CirculationController.java`
- `backend/circulation-service/src/main/java/com/gcrf/library/circulation/service/CirculationService.java`
- `backend/circulation-service/src/main/java/com/gcrf/library/circulation/entity/CirculationRecord.java`
- `backend/circulation-service/src/main/java/com/gcrf/library/circulation/mapper/CirculationRecordMapper.java`
- 对应测试文件（如存在）

**SQL 文件（legacy schemas）：**

- `backend/database/schema/03_circulation_service.sql`
- `backend/database/schema/03_circulation_simple.sql`
- `backend/database/schema/circulation_records_simple.sql`

### 保留的文件

- `04_circulation_service.sql` → 作为 Flyway V001 基线内容来源
- `BorrowController` / `ReserveController` / `FineController`
- `Borrow` / `Reserve` entities

### 网关路由变更

- 移除 `/api/v1/circulation/**` 路由
- 保留 P0 已添加的 `/api/v1/borrows/**`、`/api/v1/reserves/**`、`/api/v1/fines/**`

---

## Section 2：reader_types 表 + auth schema 清理

### 2A：reader_types 表

ReaderType 实体存在但数据库无表。创建以下 SQL 并作为 reader-service Flyway V001 的一部分：

```sql
CREATE TABLE IF NOT EXISTS reader_types (
    id BIGSERIAL PRIMARY KEY,
    type_code VARCHAR(50) NOT NULL UNIQUE,
    type_name VARCHAR(100) NOT NULL,
    max_borrow_count INTEGER NOT NULL DEFAULT 5,
    max_borrow_days INTEGER NOT NULL DEFAULT 30,
    max_renew_count INTEGER NOT NULL DEFAULT 1,
    deposit_amount INTEGER NOT NULL DEFAULT 0,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

INSERT INTO reader_types (type_code, type_name, max_borrow_count, max_borrow_days, max_renew_count, sort_order)
VALUES
    ('STUDENT', '学生', 10, 30, 2, 1),
    ('TEACHER', '教师', 20, 60, 3, 2),
    ('STAFF', '教职工', 15, 45, 2, 3),
    ('EXTERNAL', '校外读者', 3, 15, 1, 4)
ON CONFLICT (type_code) DO NOTHING;
```

### 2B：01_auth_service.sql 清理

**删除**下列表定义（auth-service Java 代码不使用）：

- `roles`
- `permissions`
- `user_roles`
- `role_permissions`
- `login_logs`

**保留**：

- `users` 表（auth-service 唯一使用的表）

在文件头添加说明注释：权限模型见 `05_system_service.sql`。

---

## Section 3：Flyway 基线迁移

### 目标

为 5 个无 Flyway 管理的服务创建 `V001__baseline.sql` 基线迁移，使启动时自动创建表结构。

### 基线策略

- 每个服务的 `V001__baseline.sql` 来自清理后的现有 SQL schema 文件
- 所有 `CREATE TABLE` 加 `IF NOT EXISTS`
- 设置 `baseline-on-migrate: true` 以兼容已有表的环境

### 每个服务的基线内容

| 服务                 | V001 文件路径                                                                     | 包含表                                                                                                                                                     |
| -------------------- | --------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| auth-service         | `backend/auth-service/src/main/resources/db/migration/V001__baseline.sql`         | users                                                                                                                                                      |
| circulation-service  | `backend/circulation-service/src/main/resources/db/migration/V001__baseline.sql`  | borrows, reserves, renew_records, fine_records, circulation_rules, violation_records                                                                       |
| reader-service       | `backend/reader-service/src/main/resources/db/migration/V001__baseline.sql`       | readers, reader_types, card_records, reader_behavior_logs, reader_favorites, reader_reviews, reader_notifications                                          |
| system-service       | `backend/system-service/src/main/resources/db/migration/V001__baseline.sql`       | departments, system_configs, config_histories, dict_types, dict_items, roles, permissions, role_permissions, menus, role_menus, operation_logs, login_logs |
| notification-service | `backend/notification-service/src/main/resources/db/migration/V001__baseline.sql` | notifications, email_logs, sms_logs, notification_templates, notification_subscriptions                                                                    |

### Flyway 配置（每个服务的 application.yml）

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    table: flyway_schema_history
```

### 依赖检查

确认根 parent pom 或每个服务 pom.xml 包含 `flyway-core` 依赖。book-service 已有 Flyway 可作模板。

### 部署脚本变更

- `deployment/postgresql/init-scripts/01-create-databases.sql` 保留（只 CREATE DATABASE）
- `backend/database/schema/` 目录保留作为参考文档，添加 README 说明其为历史参考而非执行源

---

## 修改文件清单

### 删除（Section 1）

| 文件                                                                                                           |
| -------------------------------------------------------------------------------------------------------------- |
| `backend/circulation-service/src/main/java/com/gcrf/library/circulation/controller/CirculationController.java` |
| `backend/circulation-service/src/main/java/com/gcrf/library/circulation/service/CirculationService.java`       |
| `backend/circulation-service/src/main/java/com/gcrf/library/circulation/entity/CirculationRecord.java`         |
| `backend/circulation-service/src/main/java/com/gcrf/library/circulation/mapper/CirculationRecordMapper.java`   |
| `backend/database/schema/03_circulation_service.sql`                                                           |
| `backend/database/schema/03_circulation_simple.sql`                                                            |
| `backend/database/schema/circulation_records_simple.sql`                                                       |

### 新建（Section 2 + 3）

| 文件                                                                              |
| --------------------------------------------------------------------------------- |
| `backend/auth-service/src/main/resources/db/migration/V001__baseline.sql`         |
| `backend/circulation-service/src/main/resources/db/migration/V001__baseline.sql`  |
| `backend/reader-service/src/main/resources/db/migration/V001__baseline.sql`       |
| `backend/system-service/src/main/resources/db/migration/V001__baseline.sql`       |
| `backend/notification-service/src/main/resources/db/migration/V001__baseline.sql` |
| `backend/database/schema/README.md`（说明文件）                                   |

### 修改

| 文件                                                              | 变更                               |
| ----------------------------------------------------------------- | ---------------------------------- |
| `backend/database/schema/01_auth_service.sql`                     | 删除 5 张死表                      |
| `backend/database/schema/04_reader_service.sql`                   | 添加 reader_types 表               |
| `backend/gateway-service/src/main/resources/application.yml`      | 移除 `/api/v1/circulation/**` 路由 |
| `backend/auth-service/src/main/resources/application.yml`         | 添加 Flyway 配置                   |
| `backend/circulation-service/src/main/resources/application.yml`  | 添加 Flyway 配置                   |
| `backend/reader-service/src/main/resources/application.yml`       | 添加 Flyway 配置                   |
| `backend/system-service/src/main/resources/application.yml`       | 添加 Flyway 配置                   |
| `backend/notification-service/src/main/resources/application.yml` | 添加 Flyway 配置                   |
| 5 个服务的 `pom.xml`（如未继承 flyway-core）                      | 添加依赖                           |
