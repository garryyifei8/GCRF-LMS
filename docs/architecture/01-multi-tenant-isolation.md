# 多租户隔离设计

**日期：** 2026-04-30
**状态：** Approved
**ADR：** [ADR-001](../adr/ADR-001-multi-tenant-strategy.md)

---

## 1. 隔离模型

```
PostgreSQL 实例 (一台)
├── public                  ── 系统级表（Flyway history）
├── gcrf_region             ── 区域公共 schema
│   ├── org_node            ── 组织树
│   ├── user                ── 全局用户
│   ├── role / permission   ── RBAC
│   ├── parent_student_link
│   ├── collection_standard ── 馆藏标准
│   ├── inappropriate_book  ── 不适宜书库
│   └── region_config
│
├── school_001              ── 实验小学
│   ├── book_catalog
│   ├── book_copy
│   ├── reader
│   ├── borrow_record
│   └── ... (15 张表)
│
├── school_002              ── 第二中学
│   └── (同上 15 张表)
│
└── school_NNN
```

## 2. Schema 命名

- 区域公共：`gcrf_region`（固定）
- 学校：`school_<ID>` 其中 `<ID>` 是 6 位数字编号（如 `school_000001`）
- 编号生成规则：教育局开通学校时由 `org-service` 自动分配，永不复用

## 3. Schema 路由

### 3.1 请求流（含 JWT）

```
浏览器
  ├─ Authorization: Bearer <JWT>
  │     payload: { sub, tenant: "school_001", roles, scope }
  ▼
API Gateway
  ├─ 验签 JWT
  ├─ 提取 tenant
  ▼
微服务（如 circulation-service）
  ├─ TenantContextFilter 拦截器
  │   - tenant = "school_001"
  │   - ThreadLocal.set(tenant)
  ▼
Mapper / SQL 执行
  ├─ DataSource AOP 切面
  │   - 在连接获取后立即:
  │     SET search_path TO school_001, gcrf_region;
  │   - 业务 SQL 不需要带 schema 前缀
  ▼
PostgreSQL
```

### 3.2 跨校查询（OPAC、区域统计）

- 必须在 `gcrf_region` 中维护**物化视图**，定期刷新
- 例：`gcrf_region.cross_school_book_view`
  ```sql
  CREATE MATERIALIZED VIEW gcrf_region.cross_school_book_view AS
  SELECT 'school_001' AS schema_name, * FROM school_001.book_catalog
  UNION ALL
  SELECT 'school_002', * FROM school_002.book_catalog;
  ```
- 每 5 分钟 refresh（应付新书入库）
- ES 索引同样维护跨校 alias

### 3.3 业务代码示例

```java
// circulation-service - BorrowMapper
@Mapper
public interface BorrowMapper {
    // SQL 不带 schema —— 由 search_path 切换
    @Select("SELECT * FROM borrow_record WHERE reader_id = #{readerId}")
    List<BorrowRecord> findByReader(@Param("readerId") Long readerId);
}

// 调用前由 TenantContextFilter 已设置 tenant
List<BorrowRecord> records = borrowMapper.findByReader(123L);
```

## 4. Flyway Migration 策略

### 4.1 区域公共（gcrf_region）

- 每服务下：`db/migration/region/V*.sql`
- Flyway profile: `region`，启动时一次性 migrate

### 4.2 学校 schema（school_NNN）

- 每服务下：`db/migration/per-school/V*.sql`
- 模板里使用占位符 `${schema}`：
  ```sql
  CREATE TABLE IF NOT EXISTS ${schema}.book_catalog (...);
  ```
- 新建学校时由 `org-service` 触发：

  ```java
  // 1. 创建 schema
  jdbc.execute("CREATE SCHEMA school_001");

  // 2. 对该 schema 跑 per-school migration
  Flyway.configure()
    .schemas("school_001")
    .placeholders(Map.of("schema", "school_001"))
    .locations("classpath:db/migration/per-school")
    .load()
    .migrate();
  ```

- 后续升级：批量遍历所有学校 schema 跑新版 migration

## 5. 权限校验（防越权）

每个 API 调用都经 `TenantGuard` 过滤器校验：

```java
@Component
public class TenantGuard {
  public void check(JwtClaims claims, Long resourceSchoolId) {
    if (claims.tenant().equals("school_" + resourceSchoolId)) return;  // 同租户
    if (claims.roles().contains("REGION_ADMIN")) return;                // 区域管理员
    if (claims.dataScope() == REGION) return;                           // 区域读取权限
    throw new ForbiddenException("cross-tenant access denied");
  }
}
```

## 6. 监控

- `pg_class` / `pg_namespace` 行数（schema 数量监控）
- 每 schema 大小（找出异常大的学校）
- 跨校物化视图 refresh 耗时

## 7. 备份与迁移

- 全区备份：`pg_dump --schema=gcrf_region --schema='school_*'`
- 单校备份：`pg_dump --schema=school_001`
- 单校迁出独立 DB：
  ```sql
  pg_dump --schema=school_001 -Fc | pg_restore -d new_db
  -- 然后更新 org_node.tenant_schema = 'public' 在新 DB 中
  -- 路由层添加新 DB 连接
  ```

## 8. 风险

- **schema 数 > 1000**：PG 元数据查询变慢 → 监控 + 切到独立 DB 选项
- **跨 schema JOIN** 慢：避免，改走物化视图或 ES
- **业务代码忘记设 search_path**：兜底统一用 `@Tenant` 注解强制

## 9. 关联文档

- [ADR-001](../adr/ADR-001-multi-tenant-strategy.md)
- [05-data-model](05-data-model.md)
- [06-api-spec](06-api-spec.md)
