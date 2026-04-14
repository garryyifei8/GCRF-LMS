# 数据库 Schema 参考文档

本目录下的 SQL 文件是各服务数据库 schema 的**参考文档**，用于历史查阅和 schema 设计讨论。

## ⚠️ 重要：这些文件不会自动执行

从 2026-04-14 起，数据库 schema 的实际执行由各服务的 Flyway 迁移管理：

| 服务 | Flyway 迁移目录 |
|------|----------------|
| auth-service | `backend/auth-service/src/main/resources/db/migration/` |
| book-service | `backend/book-service/src/main/resources/db/migration/` |
| circulation-service | `backend/circulation-service/src/main/resources/db/migration/` |
| reader-service | `backend/reader-service/src/main/resources/db/migration/` |
| system-service | `backend/system-service/src/main/resources/db/migration/` |
| notification-service | `backend/notification-service/src/main/resources/db/migration/` |
| recommend-service | `backend/recommend-service/src/main/resources/db/migration/` |

## 文件列表

| 文件 | 对应服务 | 数据库 | 状态 |
|------|---------|--------|------|
| `01_auth_service.sql` | auth-service | auth_service | 参考 |
| `02_book_service.sql` | book-service | book_service | 参考 |
| `02_book_simple.sql` | book-service | book_service | 参考（简化版） |
| `04_circulation_service.sql` | circulation-service | circulation_service | 参考（borrows/reserves） |
| `04_reader_service.sql` | reader-service | reader_service | 参考 |
| `04_reader_service_corrected.sql` | reader-service | reader_service | 参考（修正版） |
| `04_reader_simple.sql` | reader-service | reader_service | 参考（简化版） |
| `05_system_service.sql` | system-service | system_service | 参考 |
| `06_notification_service.sql` | notification-service | notification_service | 参考 |

## 新增 schema 变更流程

1. **不要**直接修改本目录文件
2. 在对应服务的 `db/migration/` 下创建 `V{N}__description.sql`
3. N 从该服务当前最大版本号 + 1 开始
4. 服务启动时 Flyway 自动应用

## 数据库创建

数据库本身的 CREATE DATABASE 由 `deployment/postgresql/init-scripts/01-create-databases.sql` 负责。
