# P1: 数据库 Schema 对齐 + Flyway 迁移管理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align database schemas with entity definitions across all backend services, remove legacy circulation code, and introduce Flyway migrations for version-controlled schema management.

**Architecture:** Each service owns its own database and Flyway migration history. Baseline migrations (V001) are derived from existing reference schemas in `backend/database/schema/`. Legacy circulation code and duplicated auth schema are removed.

**Tech Stack:** Spring Boot 3.2.2, Flyway Core, PostgreSQL 15, Maven

**Spec:** `docs/specs/2026-04-14-p1-schema-alignment-flyway-design.md`

---

## File Map

### Deleted Files

| File                                                                    | Reason                                                                         |
| ----------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| `backend/circulation-service/.../controller/CirculationController.java` | Legacy endpoint, replaced by BorrowController/ReserveController/FineController |
| `backend/circulation-service/.../service/CirculationService.java`       | Legacy service (class, not interface)                                          |
| `backend/circulation-service/.../entity/CirculationRecord.java`         | Legacy entity                                                                  |
| `backend/circulation-service/.../mapper/CirculationRecordMapper.java`   | Legacy mapper                                                                  |
| `backend/database/schema/03_circulation_service.sql`                    | Legacy schema                                                                  |
| `backend/database/schema/03_circulation_simple.sql`                     | Legacy schema                                                                  |
| `backend/database/schema/circulation_records_simple.sql`                | Legacy schema                                                                  |

### New Files

| File                                                                              | Responsibility                                |
| --------------------------------------------------------------------------------- | --------------------------------------------- |
| `backend/auth-service/src/main/resources/db/migration/V001__baseline.sql`         | Flyway baseline for auth_service DB           |
| `backend/circulation-service/src/main/resources/db/migration/V001__baseline.sql`  | Flyway baseline for circulation_service DB    |
| `backend/reader-service/src/main/resources/db/migration/V001__baseline.sql`       | Flyway baseline for reader_service DB         |
| `backend/system-service/src/main/resources/db/migration/V001__baseline.sql`       | Flyway baseline for system_service DB         |
| `backend/notification-service/src/main/resources/db/migration/V001__baseline.sql` | Flyway baseline for notification_service DB   |
| `backend/database/schema/README.md`                                               | Document that schema files are reference-only |

### Modified Files

| File                                                                   | Change                                |
| ---------------------------------------------------------------------- | ------------------------------------- |
| `backend/pom.xml`                                                      | Add flyway-core dependency to parent  |
| `backend/database/schema/01_auth_service.sql`                          | Remove 5 dead tables, keep only users |
| `backend/database/schema/04_reader_service.sql`                        | Add reader_types table                |
| `backend/gateway-service/src/main/resources/application.yml`           | Remove `/api/v1/circulation/**` route |
| 5 service `application.yml` files                                      | Add Flyway config                     |
| 5 service test files (if they reference deleted CirculationController) | Remove obsolete tests                 |

---

## Task 1: Delete legacy CirculationController and related code

**Files:**

- Delete: `backend/circulation-service/src/main/java/com/gcrf/library/circulation/controller/CirculationController.java`
- Delete: `backend/circulation-service/src/main/java/com/gcrf/library/circulation/service/CirculationService.java`
- Delete: `backend/circulation-service/src/main/java/com/gcrf/library/circulation/entity/CirculationRecord.java`
- Delete: `backend/circulation-service/src/main/java/com/gcrf/library/circulation/mapper/CirculationRecordMapper.java`

- [ ] **Step 1: Check for any references to CirculationController/Service/Record/Mapper**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
grep -rn "CirculationController\|CirculationService\|CirculationRecord\|CirculationRecordMapper" backend/circulation-service/src/ --include="*.java" | grep -v "circulation-service/src/main/java/com/gcrf/library/circulation/controller/CirculationController\|circulation-service/src/main/java/com/gcrf/library/circulation/service/CirculationService\|circulation-service/src/main/java/com/gcrf/library/circulation/entity/CirculationRecord\|circulation-service/src/main/java/com/gcrf/library/circulation/mapper/CirculationRecordMapper"
```

Note any external references — tests or other files that import these classes.

- [ ] **Step 2: Delete the 4 Java files**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
rm backend/circulation-service/src/main/java/com/gcrf/library/circulation/controller/CirculationController.java
rm backend/circulation-service/src/main/java/com/gcrf/library/circulation/service/CirculationService.java
rm backend/circulation-service/src/main/java/com/gcrf/library/circulation/entity/CirculationRecord.java
rm backend/circulation-service/src/main/java/com/gcrf/library/circulation/mapper/CirculationRecordMapper.java
```

- [ ] **Step 3: Delete any tests that reference these classes**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
grep -rln "CirculationController\|CirculationService\|CirculationRecord" backend/circulation-service/src/test/ --include="*.java"
```

For each file found (from Step 1 or this grep), if it's ONLY testing the deleted classes, delete it. If it tests a mix, modify it to remove only the deleted references.

- [ ] **Step 4: Compile to verify no broken imports**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
mvn clean compile -pl circulation-service -am
```

Expected: BUILD SUCCESS. If imports are broken elsewhere, fix them.

- [ ] **Step 5: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/circulation-service/
git commit -m "refactor(circulation): remove legacy CirculationController and related code

- Delete CirculationController (legacy /api/v1/circulation endpoint)
- Delete CirculationService (legacy non-interface service)
- Delete CirculationRecord entity and mapper
- Functionality replaced by BorrowController/ReserveController/FineController"
```

---

## Task 2: Delete legacy circulation SQL files

**Files:**

- Delete: `backend/database/schema/03_circulation_service.sql`
- Delete: `backend/database/schema/03_circulation_simple.sql`
- Delete: `backend/database/schema/circulation_records_simple.sql`

- [ ] **Step 1: Delete the 3 legacy SQL files**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
rm backend/database/schema/03_circulation_service.sql
rm backend/database/schema/03_circulation_simple.sql
rm backend/database/schema/circulation_records_simple.sql
```

- [ ] **Step 2: Commit**

```bash
git add backend/database/schema/
git commit -m "chore(docs): remove legacy circulation SQL schemas

- 03_circulation_service.sql (superseded by 04_circulation_service.sql)
- 03_circulation_simple.sql (legacy simplified version)
- circulation_records_simple.sql (legacy standalone version)

The active schema is 04_circulation_service.sql (borrows/reserves tables)"
```

---

## Task 3: Remove /api/v1/circulation route from gateway

**Files:**

- Modify: `backend/gateway-service/src/main/resources/application.yml`

- [ ] **Step 1: Read the current gateway config**

```bash
grep -n "circulation-service\|/api/v1/circulation" backend/gateway-service/src/main/resources/application.yml
```

- [ ] **Step 2: Remove the circulation-service route block**

Delete these lines from `backend/gateway-service/src/main/resources/application.yml` (the block starting with the circulation-service id):

```yaml
# 流通服务路由
- id: circulation-service
  uri: lb://circulation-service
  predicates:
    - Path=/api/v1/circulation/**
  # 不使用StripPrefix，保留完整路径 /api/v1/circulation/**
```

The P0-added routes (`circulation-borrows`, `circulation-reserves`, `circulation-fines`) remain.

- [ ] **Step 3: Verify YAML is valid**

```bash
python3 -c "import yaml; yaml.safe_load(open('backend/gateway-service/src/main/resources/application.yml'))" && echo "YAML OK"
```

- [ ] **Step 4: Commit**

```bash
git add backend/gateway-service/src/main/resources/application.yml
git commit -m "fix(gateway): remove legacy /api/v1/circulation route

- Replaced by /api/v1/borrows, /api/v1/reserves, /api/v1/fines routes
- CirculationController was removed in previous commit"
```

---

## Task 4: Clean up 01_auth_service.sql

**Files:**

- Modify: `backend/database/schema/01_auth_service.sql`

- [ ] **Step 1: Rewrite the file to keep only users table**

Replace the ENTIRE content of `backend/database/schema/01_auth_service.sql` with:

```sql
-- =========================================
-- 认证服务数据库表结构
-- Database: auth_service
-- Description: 用户认证相关表（仅 users 表）
--
-- 注意：roles/permissions/menus 等权限模型在 system-service 的
-- 05_system_service.sql 中定义。auth-service Java 代码仅使用 users 表。
-- =========================================

-- 注意: 请在执行此脚本前,先手动创建auth_service数据库
-- CREATE DATABASE auth_service;
-- 连接到auth_service数据库后执行以下脚本

-- 启用UUID扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(50) UNIQUE NOT NULL,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    user_type VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    avatar_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_time TIMESTAMP,
    last_login_ip VARCHAR(50),
    failed_login_count INT DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_users_user_id ON users(user_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);
CREATE INDEX IF NOT EXISTS idx_users_user_type ON users(user_type);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

-- 插入默认管理员用户（密码：admin123，已 BCrypt 加密）
INSERT INTO users (user_id, username, password, email, user_type, status) VALUES
('admin', '系统管理员', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@gcrf.edu.cn', 'ADMIN', 'ACTIVE')
ON CONFLICT (user_id) DO NOTHING;
```

- [ ] **Step 2: Commit**

```bash
git add backend/database/schema/01_auth_service.sql
git commit -m "chore(docs): clean up auth schema, keep only users table

- Remove roles/permissions/user_roles/role_permissions/login_logs (dead code)
- Auth-service Java only uses users table
- Role/permission model belongs to system-service"
```

---

## Task 5: Add reader_types to 04_reader_service.sql

**Files:**

- Modify: `backend/database/schema/04_reader_service.sql`

- [ ] **Step 1: Append reader_types table to the file**

Add this at the end of `backend/database/schema/04_reader_service.sql` (after the last existing CREATE INDEX statement, before any comments or EOF):

```sql

-- 7. 读者类型表
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

CREATE INDEX IF NOT EXISTS idx_reader_types_type_code ON reader_types(type_code);
CREATE INDEX IF NOT EXISTS idx_reader_types_status ON reader_types(status);

-- 初始数据
INSERT INTO reader_types (type_code, type_name, max_borrow_count, max_borrow_days, max_renew_count, sort_order) VALUES
    ('STUDENT', '学生', 10, 30, 2, 1),
    ('TEACHER', '教师', 20, 60, 3, 2),
    ('STAFF', '教职工', 15, 45, 2, 3),
    ('EXTERNAL', '校外读者', 3, 15, 1, 4)
ON CONFLICT (type_code) DO NOTHING;
```

- [ ] **Step 2: Commit**

```bash
git add backend/database/schema/04_reader_service.sql
git commit -m "feat(docs): add reader_types table to reader schema

- Table was missing despite ReaderType entity existing in code
- Includes 4 default reader types (STUDENT/TEACHER/STAFF/EXTERNAL)"
```

---

## Task 6: Add Flyway dependency to parent pom

**Files:**

- Modify: `backend/pom.xml`

- [ ] **Step 1: Check if flyway-core exists in dependencyManagement**

```bash
grep -n "flyway" backend/pom.xml
```

Expected: no output (Flyway is not currently declared).

- [ ] **Step 2: Add flyway-core to dependencyManagement**

Find the `<dependencyManagement>` section in `backend/pom.xml`. Inside `<dependencies>` within that section, add:

```xml
            <dependency>
                <groupId>org.flywaydb</groupId>
                <artifactId>flyway-core</artifactId>
            </dependency>
            <dependency>
                <groupId>org.flywaydb</groupId>
                <artifactId>flyway-database-postgresql</artifactId>
            </dependency>
```

Note: Flyway 10+ requires `flyway-database-postgresql` as a separate dependency for PostgreSQL support. Spring Boot 3.2.2 uses Flyway 10.x. Version is inherited from Spring Boot BOM — don't specify it.

- [ ] **Step 3: Verify parent pom still compiles**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
mvn validate -pl . -N
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add backend/pom.xml
git commit -m "build(infra): add flyway-core to parent pom dependencyManagement

- Version inherited from Spring Boot 3.2.2 BOM
- flyway-database-postgresql required for Flyway 10+ PostgreSQL support"
```

---

## Task 7: Add Flyway dependency and create baseline for auth-service

**Files:**

- Modify: `backend/auth-service/pom.xml`
- Create: `backend/auth-service/src/main/resources/db/migration/V001__baseline.sql`
- Modify: `backend/auth-service/src/main/resources/application.yml`

- [ ] **Step 1: Add Flyway dependency to auth-service pom**

In `backend/auth-service/pom.xml`, inside `<dependencies>`, add:

```xml
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
```

- [ ] **Step 2: Create V001\_\_baseline.sql**

Create `backend/auth-service/src/main/resources/db/migration/V001__baseline.sql` with this exact content:

```sql
-- =========================================
-- V001: Baseline migration for auth-service
-- Database: auth_service
-- Description: Creates users table
-- =========================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(50) UNIQUE NOT NULL,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    user_type VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    avatar_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_time TIMESTAMP,
    last_login_ip VARCHAR(50),
    failed_login_count INT DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_users_user_id ON users(user_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);
CREATE INDEX IF NOT EXISTS idx_users_user_type ON users(user_type);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

-- Default admin user (password: admin123, BCrypt hashed)
INSERT INTO users (user_id, username, password, email, user_type, status) VALUES
('admin', '系统管理员', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@gcrf.edu.cn', 'ADMIN', 'ACTIVE')
ON CONFLICT (user_id) DO NOTHING;
```

- [ ] **Step 3: Add Flyway config to application.yml**

In `backend/auth-service/src/main/resources/application.yml`, find the `spring:` section. Add this block as a sibling of `datasource:` (under `spring:`):

```yaml
flyway:
  enabled: true
  locations: classpath:db/migration
  baseline-on-migrate: true
  baseline-version: 0
  table: flyway_schema_history
```

- [ ] **Step 4: Compile to verify**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
mvn clean compile -pl auth-service -am
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add backend/auth-service/
git commit -m "feat(auth): add Flyway baseline migration for users table

- V001__baseline.sql creates users table with indexes
- Includes default admin user
- baseline-on-migrate=true for compatibility with existing DBs"
```

---

## Task 8: Create Flyway baseline for circulation-service

**Files:**

- Modify: `backend/circulation-service/pom.xml`
- Create: `backend/circulation-service/src/main/resources/db/migration/V001__baseline.sql`
- Modify: `backend/circulation-service/src/main/resources/application.yml`

- [ ] **Step 1: Add Flyway dependency to pom**

In `backend/circulation-service/pom.xml`, inside `<dependencies>`, add:

```xml
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
```

- [ ] **Step 2: Create V001\_\_baseline.sql**

Read the current content of `backend/database/schema/04_circulation_service.sql` first:

```bash
wc -l backend/database/schema/04_circulation_service.sql
```

Copy the CREATE TABLE, CREATE INDEX, and any trigger/function definitions from that file into `backend/circulation-service/src/main/resources/db/migration/V001__baseline.sql`. Add `IF NOT EXISTS` to all CREATE TABLE statements if not already present.

**Required tables (must all be in V001):**

- `borrows`
- `reserves`

Omit any test data or sample INSERT statements (Flyway production migration should not seed test data).

Add this header at the top:

```sql
-- =========================================
-- V001: Baseline migration for circulation-service
-- Database: circulation_service
-- Description: Creates borrows and reserves tables with indexes
-- =========================================
```

- [ ] **Step 3: Add Flyway config to application.yml**

In `backend/circulation-service/src/main/resources/application.yml`, add under `spring:`:

```yaml
flyway:
  enabled: true
  locations: classpath:db/migration
  baseline-on-migrate: true
  baseline-version: 0
  table: flyway_schema_history
```

- [ ] **Step 4: Compile**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
mvn clean compile -pl circulation-service -am
```

- [ ] **Step 5: Commit**

```bash
git add backend/circulation-service/
git commit -m "feat(circulation): add Flyway baseline migration

- V001__baseline.sql creates borrows and reserves tables
- Based on 04_circulation_service.sql reference schema"
```

---

## Task 9: Create Flyway baseline for reader-service

**Files:**

- Modify: `backend/reader-service/pom.xml`
- Create: `backend/reader-service/src/main/resources/db/migration/V001__baseline.sql`
- Modify: `backend/reader-service/src/main/resources/application.yml`

- [ ] **Step 1: Add Flyway dependency to pom**

In `backend/reader-service/pom.xml`, inside `<dependencies>`, add:

```xml
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
```

- [ ] **Step 2: Create V001\_\_baseline.sql**

Create `backend/reader-service/src/main/resources/db/migration/V001__baseline.sql`. Copy all CREATE TABLE and CREATE INDEX statements from `backend/database/schema/04_reader_service.sql` (including the reader_types table added in Task 5).

Add `IF NOT EXISTS` to all CREATE TABLE if missing.

Include these tables:

- `readers`
- `reader_types` (with default data: STUDENT/TEACHER/STAFF/EXTERNAL)
- `card_records`
- `reader_behavior_logs`
- `reader_favorites`
- `reader_reviews`
- `reader_notifications`

Header:

```sql
-- =========================================
-- V001: Baseline migration for reader-service
-- Database: reader_service
-- Description: Creates readers, reader_types and related tables
-- =========================================
```

- [ ] **Step 3: Add Flyway config**

In `backend/reader-service/src/main/resources/application.yml`, add under `spring:`:

```yaml
flyway:
  enabled: true
  locations: classpath:db/migration
  baseline-on-migrate: true
  baseline-version: 0
  table: flyway_schema_history
```

- [ ] **Step 4: Compile**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
mvn clean compile -pl reader-service -am
```

- [ ] **Step 5: Commit**

```bash
git add backend/reader-service/
git commit -m "feat(reader): add Flyway baseline migration

- V001__baseline.sql creates readers, reader_types, and related tables
- Includes default reader types (STUDENT/TEACHER/STAFF/EXTERNAL)"
```

---

## Task 10: Create Flyway baseline for system-service

**Files:**

- Modify: `backend/system-service/pom.xml`
- Create: `backend/system-service/src/main/resources/db/migration/V001__baseline.sql`
- Modify: `backend/system-service/src/main/resources/application.yml`

- [ ] **Step 1: Add Flyway dependency**

In `backend/system-service/pom.xml`:

```xml
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
```

- [ ] **Step 2: Create V001\_\_baseline.sql**

Copy all CREATE TABLE, CREATE INDEX, and INSERT statements from `backend/database/schema/05_system_service.sql` into `backend/system-service/src/main/resources/db/migration/V001__baseline.sql`.

Add `IF NOT EXISTS` to CREATE TABLE statements.
Preserve the initial data INSERTs (roles, permissions, menus default data).

Include these tables:

- `departments`
- `system_configs`
- `config_histories`
- `dict_types`
- `dict_items`
- `roles`
- `permissions`
- `role_permissions`
- `menus`
- `role_menus`
- `operation_logs`
- `login_logs`

Header:

```sql
-- =========================================
-- V001: Baseline migration for system-service
-- Database: system_service
-- Description: Creates all system management tables with default data
-- =========================================
```

- [ ] **Step 3: Add Flyway config**

In `backend/system-service/src/main/resources/application.yml`:

```yaml
flyway:
  enabled: true
  locations: classpath:db/migration
  baseline-on-migrate: true
  baseline-version: 0
  table: flyway_schema_history
```

- [ ] **Step 4: Compile**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
mvn clean compile -pl system-service -am
```

- [ ] **Step 5: Commit**

```bash
git add backend/system-service/
git commit -m "feat(system): add Flyway baseline migration

- V001__baseline.sql creates all system management tables
- Includes default roles, permissions, and menus"
```

---

## Task 11: Create Flyway baseline for notification-service

**Files:**

- Modify: `backend/notification-service/pom.xml`
- Create: `backend/notification-service/src/main/resources/db/migration/V001__baseline.sql`
- Modify: `backend/notification-service/src/main/resources/application.yml`

- [ ] **Step 1: Add Flyway dependency**

In `backend/notification-service/pom.xml`:

```xml
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
```

- [ ] **Step 2: Create V001\_\_baseline.sql**

Copy CREATE TABLE statements from `backend/database/schema/06_notification_service.sql`.

Include these tables:

- `notifications`
- `email_logs`
- `sms_logs`
- `notification_templates`
- `notification_subscriptions`

**IMPORTANT:** The original schema uses table partitioning for `email_logs` and `sms_logs`. If the partitioning is complex, simplify to non-partitioned tables for baseline — partitioning can be added in a later migration (V002). This keeps the baseline simple.

Preserve the default notification templates INSERT statements.

Header:

```sql
-- =========================================
-- V001: Baseline migration for notification-service
-- Database: notification_service
-- Description: Creates notification, email/sms log, and template tables
-- =========================================
```

- [ ] **Step 3: Add Flyway config**

In `backend/notification-service/src/main/resources/application.yml`:

```yaml
flyway:
  enabled: true
  locations: classpath:db/migration
  baseline-on-migrate: true
  baseline-version: 0
  table: flyway_schema_history
```

- [ ] **Step 4: Compile**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
mvn clean compile -pl notification-service -am
```

- [ ] **Step 5: Commit**

```bash
git add backend/notification-service/
git commit -m "feat(notification): add Flyway baseline migration

- V001__baseline.sql creates all notification tables
- Includes default notification templates"
```

---

## Task 12: Add README to backend/database/schema/

**Files:**

- Create: `backend/database/schema/README.md`

- [ ] **Step 1: Create README.md**

Create `backend/database/schema/README.md` with content:

```markdown
# 数据库 Schema 参考文档

本目录下的 SQL 文件是各服务数据库 schema 的**参考文档**，用于历史查阅和 schema 设计讨论。

## ⚠️ 重要：这些文件不会自动执行

从 2026-04-14 起，数据库 schema 的实际执行由各服务的 Flyway 迁移管理：

| 服务                 | Flyway 迁移目录                                                 |
| -------------------- | --------------------------------------------------------------- |
| auth-service         | `backend/auth-service/src/main/resources/db/migration/`         |
| book-service         | `backend/book-service/src/main/resources/db/migration/`         |
| circulation-service  | `backend/circulation-service/src/main/resources/db/migration/`  |
| reader-service       | `backend/reader-service/src/main/resources/db/migration/`       |
| system-service       | `backend/system-service/src/main/resources/db/migration/`       |
| notification-service | `backend/notification-service/src/main/resources/db/migration/` |
| recommend-service    | `backend/recommend-service/src/main/resources/db/migration/`    |

## 文件列表

| 文件                              | 对应服务             | 数据库               | 状态                     |
| --------------------------------- | -------------------- | -------------------- | ------------------------ |
| `01_auth_service.sql`             | auth-service         | auth_service         | 参考                     |
| `02_book_service.sql`             | book-service         | book_service         | 参考                     |
| `02_book_simple.sql`              | book-service         | book_service         | 参考（简化版）           |
| `04_circulation_service.sql`      | circulation-service  | circulation_service  | 参考（borrows/reserves） |
| `04_reader_service.sql`           | reader-service       | reader_service       | 参考                     |
| `04_reader_service_corrected.sql` | reader-service       | reader_service       | 参考（修正版）           |
| `04_reader_simple.sql`            | reader-service       | reader_service       | 参考（简化版）           |
| `05_system_service.sql`           | system-service       | system_service       | 参考                     |
| `06_notification_service.sql`     | notification-service | notification_service | 参考                     |

## 新增 schema 变更流程

1. **不要**直接修改本目录文件
2. 在对应服务的 `db/migration/` 下创建 `V{N}__description.sql`
3. N 从该服务当前最大版本号 + 1 开始
4. 服务启动时 Flyway 自动应用

## 数据库创建

数据库本身的 CREATE DATABASE 由 `deployment/postgresql/init-scripts/01-create-databases.sql` 负责。
```

- [ ] **Step 2: Commit**

```bash
git add backend/database/schema/README.md
git commit -m "docs(docs): add README for database schema directory

- Clarify that schema files are reference only, not executed
- Document Flyway migration paths for each service
- Guide for future schema changes"
```

---

## Task 13: Verification — build and test

- [ ] **Step 1: Full backend compile**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
mvn clean compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 2: Verify Flyway migrations are present**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
for svc in auth circulation reader system notification; do
  echo "=== $svc-service ==="
  ls backend/$svc-service/src/main/resources/db/migration/ 2>&1
done
```

Expected: Each service lists at least `V001__baseline.sql`.

- [ ] **Step 3: Verify Flyway config in each service**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
for svc in auth circulation reader system notification; do
  echo "=== $svc-service ==="
  grep -A2 "flyway:" backend/$svc-service/src/main/resources/application.yml
done
```

Expected: Each service shows Flyway configuration block.

- [ ] **Step 4: Run existing tests (sanity check)**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
mvn test -pl auth-service,reader-service,system-service
```

Some tests may fail due to Flyway running against the test database — that's expected if they use in-memory or test databases that don't exist yet. Note any NEW failures (compared to before this work).

- [ ] **Step 5: Commit spec and plan docs**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add docs/specs/2026-04-14-p1-schema-alignment-flyway-design.md docs/specs/2026-04-14-p1-schema-alignment-flyway-plan.md
git commit -m "docs(docs): add P1 schema alignment and Flyway migration spec and plan"
```
