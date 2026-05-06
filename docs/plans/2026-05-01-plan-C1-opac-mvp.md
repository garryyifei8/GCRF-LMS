# Plan-C1: OPAC MVP 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建一个公开访问的 OPAC（Online Public Access Catalog）后端服务，支持跨校馆藏检索 / 详情 / 中图法浏览 / 借阅排行 / 相关推荐 / 新书速递 / IP 限流，能用 mock 数据演示"输入关键词跨校查到 N 个学校 X 册可借"全链路。

**Architecture:** 新增 `opac-service`（端口 8092）。跨校检索通过 PostgreSQL 物化视图 + `pg_trgm` GIN 索引实现（避免部署 ES）。视图 UNION 所有 `school_NNNNNN.book_catalog`，每次新建学校或显式调 admin endpoint 时 refresh。限流走 Redis INCR/EXPIRE。中图法 22 小类用静态 JSON 资源加载到内存。

**Tech Stack:** Java 21 / Spring Boot 3.2.2 / MyBatis-Plus 3.5.9 / PostgreSQL 15（pg_trgm + ltree）/ Redis 7 / Vue 3 公开站留待 plan-C3。

**Spec 来源：** [docs/specs/2026-04-30-regional-platform-master-design.md](../specs/2026-04-30-regional-platform-master-design.md) §4.2，[docs/prd/02-regional-opac.md](../prd/02-regional-opac.md)，[docs/architecture/06-api-spec.md](../architecture/06-api-spec.md) §2.7。Plan-A 已交付的多租户 schema 路由 + org-service 是本 plan 的依赖。

**总周期估计：** 5-7 工作日（一个全栈工程师全职，沿用 plan-A 的 subagent 节奏）。

**已知妥协**：

- 用 `pg_trgm` 而不是 ES → 中文按字符 trigram，无分词；`ILIKE '%xx%'` 性能在百万级数据 < 200ms（够 demo）
- 跨校物化视图每次 refresh 都 DROP + CREATE → MVP 阶段 OK，多校上线后改为 `REFRESH MATERIALIZED VIEW CONCURRENTLY`
- 不做自动补全 / 检索词排行 / 协同过滤 → plan-C1.5 / plan-C2 处理
- OPAC 站点是后端 API only，前端走 plan-C3

---

## File Structure

### 后端 — `opac-service`（新模块）

| 文件                                                                                              | 责任                                  |
| ------------------------------------------------------------------------------------------------- | ------------------------------------- |
| `backend/opac-service/pom.xml`                                                                    | Maven 模块定义                        |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/OpacServiceApplication.java`            | Spring Boot 入口                      |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/domain/dto/SearchRequest.java`          | 检索请求                              |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/domain/vo/BookSearchItemVO.java`        | 列表项 VO                             |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/domain/vo/BookDetailVO.java`            | 详情 VO（含跨校 availability）        |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/domain/vo/SchoolAvailabilityVO.java`    | 单学校在馆状态                        |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/domain/vo/RankingItemVO.java`           | 排行榜项                              |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/domain/vo/ClcNodeVO.java`               | 中图法节点                            |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/domain/vo/PageVO.java`                  | 通用分页响应                          |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/mapper/BookSearchMapper.java`           | 跨校物化视图查询                      |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/mapper/CrossSchoolMapper.java`          | 跨校原表查询（详情/排行）             |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/service/SearchService.java` + impl      | 关键词检索 + 高级筛选                 |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/service/BookDetailService.java` + impl  | 详情 + 跨校 availability              |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/service/ClcService.java` + impl         | 中图法树 + 分类浏览                   |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/service/RankingService.java` + impl     | 借阅排行                              |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/service/RecommendService.java` + impl   | 相关推荐（同分类）                    |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/service/NewArrivalsService.java` + impl | 新书速递（按入库时间）                |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/service/SearchMviewService.java` + impl | 物化视图 refresh                      |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/controller/SearchController.java`       | search / new-arrivals / suggest       |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/controller/BookDetailController.java`   | book by isbn                          |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/controller/ClcController.java`          | clc tree / clc browse                 |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/controller/RankingController.java`      | rankings                              |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/controller/RecommendController.java`    | recommend                             |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/controller/AdminController.java`        | refresh mview                         |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/ratelimit/RateLimit.java`               | 注解                                  |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/ratelimit/RedisRateLimiter.java`        | INCR+EXPIRE 实现                      |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/ratelimit/RateLimitInterceptor.java`    | Spring MVC interceptor                |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/config/WebMvcConfig.java`               | 注册 interceptor                      |
| `backend/opac-service/src/main/java/com/gcrf/library/opac/config/PublicEndpointConfig.java`       | OPAC 路径放行匿名访问                 |
| `backend/opac-service/src/main/resources/application.yml`                                         | 主配置                                |
| `backend/opac-service/src/main/resources/application-k8s.yml`                                     | K8s profile                           |
| `backend/opac-service/src/main/resources/application-test.yml`                                    | test profile（无 Redis）              |
| `backend/opac-service/src/main/resources/data/clc-22-categories.json`                             | 5 大类 22 小类静态树                  |
| `backend/opac-service/src/main/resources/db/migration/region/V001__opac_search_setup.sql`         | pg_trgm + refresh function + 物化视图 |
| `backend/opac-service/src/test/java/...`                                                          | 集成测试                              |

### 部署

| 文件                                       | 责任                                             |
| ------------------------------------------ | ------------------------------------------------ |
| `deployment/k8s/10-services.yaml`          | 加 `gcrf-opac` Deployment + Service              |
| `deployment/k8s/02-configmap.yaml`         | service-discovery 加 `opac-service`              |
| nginx configmap                            | 加 `/api/v1/opac` location 块（在 K8s 里 patch） |
| `/tmp/gcrf-deploy/Dockerfile-opac-service` | 镜像                                             |

---

## Tasks

### Task 1: opac-service Maven 骨架

**Files:**

- Modify: `backend/pom.xml`
- Create: `backend/opac-service/pom.xml`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/OpacServiceApplication.java`
- Create: `backend/opac-service/src/main/resources/application.yml`
- Create: `backend/opac-service/src/main/resources/application-k8s.yml`
- Create: `backend/opac-service/src/main/resources/application-test.yml`

- [ ] **Step 1: Add module to parent pom**

In `backend/pom.xml` `<modules>`, add `<module>opac-service</module>` after `<module>org-service</module>`.

- [ ] **Step 2: Create opac-service pom**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.gcrf.library</groupId>
        <artifactId>library-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>opac-service</artifactId>
    <packaging>jar</packaging>
    <name>opac-service</name>
    <description>区域 OPAC 公开检索（跨校馆藏检索 + 浏览 + 排行 + 推荐）</description>

    <dependencies>
        <dependency><groupId>com.gcrf.library</groupId><artifactId>common-core</artifactId></dependency>
        <dependency><groupId>com.gcrf.library</groupId><artifactId>common-web</artifactId></dependency>
        <dependency><groupId>com.gcrf.library</groupId><artifactId>common-security</artifactId></dependency>
        <dependency><groupId>com.gcrf.library</groupId><artifactId>common-mybatis</artifactId></dependency>

        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-redis</artifactId></dependency>

        <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
        <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-database-postgresql</artifactId></dependency>

        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
        <dependency><groupId>org.testcontainers</groupId><artifactId>postgresql</artifactId><scope>test</scope></dependency>
        <dependency><groupId>org.testcontainers</groupId><artifactId>junit-jupiter</artifactId><scope>test</scope></dependency>
        <dependency>
            <groupId>com.redis</groupId>
            <artifactId>testcontainers-redis</artifactId>
            <version>2.2.4</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: Create Application entry**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/OpacServiceApplication.java
package com.gcrf.library.opac;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.gcrf.library")
@MapperScan("com.gcrf.library.opac.mapper")
public class OpacServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpacServiceApplication.class, args);
    }
}
```

- [ ] **Step 4: Create application.yml**

```yaml
# backend/opac-service/src/main/resources/application.yml
server:
  port: 8092

spring:
  application:
    name: opac-service
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:gcrf_main}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DB:2}
      timeout: 3000ms
  flyway:
    enabled: true
    locations: classpath:db/migration/region
    schemas: gcrf_region
    create-schemas: true
    baseline-on-migrate: true
  jackson:
    time-zone: Asia/Shanghai
    date-format: yyyy-MM-dd HH:mm:ss

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto

gcrf:
  tenant:
    enabled: false # OPAC is public; tenant filter is a no-op here
  opac:
    rate-limit:
      enabled: true
    search-mview:
      auto-refresh-on-startup: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

- [ ] **Step 5: Create application-k8s.yml**

```yaml
# backend/opac-service/src/main/resources/application-k8s.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:postgresql.edu-infra.svc.cluster.local}:5432/${DB_NAME:gcrf_main}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD}
  data:
    redis:
      host: ${REDIS_HOST:redis.edu-infra.svc.cluster.local}
      port: ${REDIS_PORT:6379}
      database: ${REDIS_DB:2}
      password: ${REDIS_PASSWORD}

logging:
  level:
    com.gcrf.library: INFO
    org.flywaydb: INFO
```

- [ ] **Step 6: Create application-test.yml**

```yaml
# backend/opac-service/src/main/resources/application-test.yml
spring:
  data:
    redis:
      host: localhost # overridden by testcontainers in tests
      port: 6379

gcrf:
  opac:
    rate-limit:
      enabled: false # disable in tests so we don't hit Redis
    search-mview:
      auto-refresh-on-startup: false
```

- [ ] **Step 7: Verify build**

Run:

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl opac-service -am
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/pom.xml backend/opac-service/pom.xml \
        backend/opac-service/src/main/java/com/gcrf/library/opac/OpacServiceApplication.java \
        backend/opac-service/src/main/resources/application.yml \
        backend/opac-service/src/main/resources/application-k8s.yml \
        backend/opac-service/src/main/resources/application-test.yml
git commit -m "feat(common): scaffold opac-service module"
```

Allowed scopes (from commitlint): gateway, auth, book, circulation, reader, system, notification, recommend, chat, analytics, common, web-admin, infra, docs. We use `common` for opac-service work (same precedent as plan-A).

---

### Task 2: Region migration — pg_trgm + refresh function + 跨校物化视图

**Files:**

- Create: `backend/opac-service/src/main/resources/db/migration/region/V001__opac_search_setup.sql`

- [ ] **Step 1: Create V001 migration**

```sql
-- backend/opac-service/src/main/resources/db/migration/region/V001__opac_search_setup.sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Cross-school book search materialized view, lazily populated.
-- Initial create is empty; refresh_book_search_mview() rebuilds it from
-- all school_* schemas detected at runtime.
CREATE MATERIALIZED VIEW IF NOT EXISTS book_search_mview AS
SELECT
    NULL::TEXT  AS school_schema,
    NULL::BIGINT AS book_id,
    NULL::TEXT  AS isbn,
    NULL::TEXT  AS title,
    NULL::TEXT  AS author,
    NULL::TEXT  AS classification,
    NULL::INT   AS total_count,
    NULL::INT   AS available_count,
    NULL::TIMESTAMPTZ AS created_at
WHERE FALSE;

CREATE INDEX IF NOT EXISTS idx_book_search_title_trgm
    ON book_search_mview USING GIN (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_book_search_author_trgm
    ON book_search_mview USING GIN (author gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_book_search_isbn_trgm
    ON book_search_mview USING GIN (isbn gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_book_search_clc
    ON book_search_mview (classification);
CREATE INDEX IF NOT EXISTS idx_book_search_school
    ON book_search_mview (school_schema);

-- Refresh function: rebuilds the mview from all school_* schemas.
CREATE OR REPLACE FUNCTION refresh_book_search_mview() RETURNS INT AS $$
DECLARE
    sch       TEXT;
    union_sql TEXT := '';
    row_count INT;
BEGIN
    FOR sch IN
        SELECT nspname FROM pg_namespace WHERE nspname LIKE 'school\_%' ESCAPE '\' ORDER BY nspname
    LOOP
        IF union_sql <> '' THEN
            union_sql := union_sql || ' UNION ALL ';
        END IF;
        union_sql := union_sql || format($f$
            SELECT %L AS school_schema, id AS book_id, isbn, title, author,
                   classification, total_count, available_count, created_at
            FROM %I.book_catalog
        $f$, sch, sch);
    END LOOP;

    -- Drop and recreate; CONCURRENTLY refresh requires a unique index, leave for plan-C2
    EXECUTE 'DROP MATERIALIZED VIEW IF EXISTS book_search_mview CASCADE';

    IF union_sql = '' THEN
        EXECUTE $sql$
            CREATE MATERIALIZED VIEW book_search_mview AS
            SELECT NULL::TEXT  AS school_schema,
                   NULL::BIGINT AS book_id,
                   NULL::TEXT  AS isbn,
                   NULL::TEXT  AS title,
                   NULL::TEXT  AS author,
                   NULL::TEXT  AS classification,
                   NULL::INT   AS total_count,
                   NULL::INT   AS available_count,
                   NULL::TIMESTAMPTZ AS created_at
            WHERE FALSE
        $sql$;
    ELSE
        EXECUTE 'CREATE MATERIALIZED VIEW book_search_mview AS ' || union_sql;
    END IF;

    -- Recreate indexes after DROP CASCADE
    EXECUTE 'CREATE INDEX idx_book_search_title_trgm  ON book_search_mview USING GIN (title gin_trgm_ops)';
    EXECUTE 'CREATE INDEX idx_book_search_author_trgm ON book_search_mview USING GIN (author gin_trgm_ops)';
    EXECUTE 'CREATE INDEX idx_book_search_isbn_trgm   ON book_search_mview USING GIN (isbn gin_trgm_ops)';
    EXECUTE 'CREATE INDEX idx_book_search_clc         ON book_search_mview (classification)';
    EXECUTE 'CREATE INDEX idx_book_search_school      ON book_search_mview (school_schema)';

    SELECT count(*) INTO row_count FROM book_search_mview;
    RETURN row_count;
END;
$$ LANGUAGE plpgsql;
```

- [ ] **Step 2: Verify migration applies cleanly via testcontainers**

We will write the actual integration test in Task 5; for now, just verify the SQL parses by running:

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn process-resources -pl opac-service
ls -la opac-service/target/classes/db/migration/region/V001__opac_search_setup.sql
```

Expected: file present.

- [ ] **Step 3: Commit**

```bash
git add backend/opac-service/src/main/resources/db/migration/region/V001__opac_search_setup.sql
git commit -m "feat(common): add OPAC search setup (pg_trgm + cross-school mview + refresh function)"
```

---

### Task 3: 中图法 22 小类静态资源

**Files:**

- Create: `backend/opac-service/src/main/resources/data/clc-22-categories.json`

- [ ] **Step 1: Create JSON resource**

```json
[
  { "code": "A", "name": "马列毛邓理论", "parentCode": null, "children": [] },
  {
    "code": "B",
    "name": "哲学、宗教",
    "parentCode": null,
    "children": [
      { "code": "B0", "name": "哲学理论" },
      { "code": "B2", "name": "中国哲学" },
      { "code": "B5", "name": "欧美哲学" },
      { "code": "B8", "name": "心理学" },
      { "code": "B9", "name": "宗教" }
    ]
  },
  {
    "code": "C",
    "name": "社会科学总论",
    "parentCode": null,
    "children": [{ "code": "C8", "name": "统计学" }]
  },
  {
    "code": "D",
    "name": "政治、法律",
    "parentCode": null,
    "children": [
      { "code": "D0", "name": "政治理论" },
      { "code": "D9", "name": "法律" }
    ]
  },
  { "code": "E", "name": "军事", "parentCode": null, "children": [] },
  {
    "code": "F",
    "name": "经济",
    "parentCode": null,
    "children": [
      { "code": "F0", "name": "政治经济学" },
      { "code": "F8", "name": "财政、金融" }
    ]
  },
  {
    "code": "G",
    "name": "文化、科学、教育、体育",
    "parentCode": null,
    "children": [
      { "code": "G0", "name": "文化理论" },
      { "code": "G4", "name": "教育" },
      { "code": "G8", "name": "体育" }
    ]
  },
  {
    "code": "H",
    "name": "语言、文字",
    "parentCode": null,
    "children": [
      { "code": "H0", "name": "语言学" },
      { "code": "H1", "name": "汉语" },
      { "code": "H3", "name": "英语" }
    ]
  },
  {
    "code": "I",
    "name": "文学",
    "parentCode": null,
    "children": [
      { "code": "I0", "name": "文学理论" },
      { "code": "I2", "name": "中国文学" },
      { "code": "I3/7", "name": "外国文学" }
    ]
  },
  {
    "code": "J",
    "name": "艺术",
    "parentCode": null,
    "children": [
      { "code": "J0", "name": "艺术理论" },
      { "code": "J2", "name": "绘画" },
      { "code": "J6", "name": "音乐" },
      { "code": "J9", "name": "电影、电视" }
    ]
  },
  {
    "code": "K",
    "name": "历史、地理",
    "parentCode": null,
    "children": [
      { "code": "K2", "name": "中国历史" },
      { "code": "K9", "name": "地理" }
    ]
  },
  { "code": "N", "name": "自然科学总论", "parentCode": null, "children": [] },
  {
    "code": "O",
    "name": "数理科学和化学",
    "parentCode": null,
    "children": [
      { "code": "O1", "name": "数学" },
      { "code": "O4", "name": "物理学" },
      { "code": "O6", "name": "化学" }
    ]
  },
  {
    "code": "P",
    "name": "天文学、地球科学",
    "parentCode": null,
    "children": []
  },
  { "code": "Q", "name": "生物科学", "parentCode": null, "children": [] },
  { "code": "R", "name": "医药、卫生", "parentCode": null, "children": [] },
  { "code": "S", "name": "农业科学", "parentCode": null, "children": [] },
  {
    "code": "T",
    "name": "工业技术",
    "parentCode": null,
    "children": [
      { "code": "TP", "name": "自动化、计算机" },
      { "code": "TU", "name": "建筑科学" }
    ]
  },
  { "code": "U", "name": "交通运输", "parentCode": null, "children": [] },
  { "code": "V", "name": "航空、航天", "parentCode": null, "children": [] },
  { "code": "X", "name": "环境科学", "parentCode": null, "children": [] },
  { "code": "Z", "name": "综合性图书", "parentCode": null, "children": [] }
]
```

- [ ] **Step 2: Commit**

```bash
git add backend/opac-service/src/main/resources/data/clc-22-categories.json
git commit -m "feat(common): add CLC 22 categories static resource"
```

---

### Task 4: Domain DTOs / VOs

**Files:**

- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/domain/dto/SearchRequest.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/domain/vo/PageVO.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/domain/vo/BookSearchItemVO.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/domain/vo/BookDetailVO.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/domain/vo/SchoolAvailabilityVO.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/domain/vo/RankingItemVO.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/domain/vo/ClcNodeVO.java`

- [ ] **Step 1: SearchRequest DTO**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/domain/dto/SearchRequest.java
package com.gcrf.library.opac.domain.dto;

import lombok.Data;

@Data
public class SearchRequest {
    private String q;             // keyword (matches title / author / isbn)
    private String clc;           // CLC code prefix, e.g. "I" or "I2"
    private String school;        // school_schema, e.g. "school_000003" (null = all schools)
    private int pageNum = 1;
    private int pageSize = 20;
}
```

- [ ] **Step 2: PageVO**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/domain/vo/PageVO.java
package com.gcrf.library.opac.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageVO<T> {
    private List<T> records;
    private long total;
    private int pageNum;
    private int pageSize;
}
```

- [ ] **Step 3: BookSearchItemVO**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/domain/vo/BookSearchItemVO.java
package com.gcrf.library.opac.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookSearchItemVO {
    private String schoolSchema;
    private Long bookId;
    private String isbn;
    private String title;
    private String author;
    private String classification;
    private Integer totalCount;
    private Integer availableCount;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 4: SchoolAvailabilityVO**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/domain/vo/SchoolAvailabilityVO.java
package com.gcrf.library.opac.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SchoolAvailabilityVO {
    private String schoolSchema;
    private String schoolName;
    private Integer totalCount;
    private Integer availableCount;
}
```

- [ ] **Step 5: BookDetailVO**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/domain/vo/BookDetailVO.java
package com.gcrf.library.opac.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class BookDetailVO {
    private String isbn;
    private String title;
    private String author;
    private String classification;
    private List<SchoolAvailabilityVO> schools;
}
```

- [ ] **Step 6: RankingItemVO**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/domain/vo/RankingItemVO.java
package com.gcrf.library.opac.domain.vo;

import lombok.Data;

@Data
public class RankingItemVO {
    private int rank;
    private String isbn;
    private String title;
    private String author;
    private String classification;
    private long borrowCount;
}
```

- [ ] **Step 7: ClcNodeVO**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/domain/vo/ClcNodeVO.java
package com.gcrf.library.opac.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClcNodeVO {
    private String code;
    private String name;
    private String parentCode;
    private List<ClcNodeVO> children = new ArrayList<>();
}
```

- [ ] **Step 8: Verify compile**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl opac-service -am
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 9: Commit**

```bash
git add backend/opac-service/src/main/java/com/gcrf/library/opac/domain/
git commit -m "feat(common): add OPAC domain DTOs and VOs"
```

---

### Task 5: SearchMviewService — refresh wrapper + integration test

**Files:**

- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/service/SearchMviewService.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/service/impl/SearchMviewServiceImpl.java`
- Create: `backend/opac-service/src/test/java/com/gcrf/library/opac/service/SearchMviewServiceTest.java`

- [ ] **Step 1: Write failing test**

```java
// backend/opac-service/src/test/java/com/gcrf/library/opac/service/SearchMviewServiceTest.java
package com.gcrf.library.opac.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class SearchMviewServiceTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired SearchMviewService svc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void refresh_emptyCluster_succeedsWithZeroRows() {
        int rows = svc.refresh();
        assertThat(rows).isZero();
        Integer count = jdbc.queryForObject(
            "SELECT count(*) FROM gcrf_region.book_search_mview", Integer.class);
        assertThat(count).isZero();
    }

    @Test
    void refresh_withSchoolBooks_returnsRowCount() {
        // simulate one school
        jdbc.execute("CREATE SCHEMA school_000001");
        jdbc.execute("CREATE TABLE school_000001.book_catalog ("
            + " id BIGSERIAL PRIMARY KEY, isbn TEXT, title TEXT, author TEXT,"
            + " classification TEXT, total_count INT DEFAULT 0,"
            + " available_count INT DEFAULT 0,"
            + " created_at TIMESTAMPTZ NOT NULL DEFAULT NOW())");
        jdbc.update("INSERT INTO school_000001.book_catalog "
            + "(isbn, title, author, classification, total_count, available_count) VALUES "
            + "('9787111000001', '深入理解计算机系统', 'Bryant', 'TP', 5, 3),"
            + "('9787111000002', '算法导论', 'Cormen', 'TP', 8, 7)");

        int rows = svc.refresh();
        assertThat(rows).isEqualTo(2);

        Integer found = jdbc.queryForObject(
            "SELECT count(*) FROM gcrf_region.book_search_mview WHERE title ILIKE '%深入%'",
            Integer.class);
        assertThat(found).isEqualTo(1);
    }
}
```

- [ ] **Step 2: Run test to confirm failure**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl opac-service -am -Dtest=SearchMviewServiceTest
```

Expected: COMPILATION FAILURE — `SearchMviewService` not found.

- [ ] **Step 3: Write interface**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/service/SearchMviewService.java
package com.gcrf.library.opac.service;

public interface SearchMviewService {
    /** Calls the PG function refresh_book_search_mview() and returns row count. */
    int refresh();
}
```

- [ ] **Step 4: Write impl**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/service/impl/SearchMviewServiceImpl.java
package com.gcrf.library.opac.service.impl;

import com.gcrf.library.opac.service.SearchMviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchMviewServiceImpl implements SearchMviewService {

    private final JdbcTemplate jdbc;

    @Value("${gcrf.opac.search-mview.auto-refresh-on-startup:true}")
    private boolean autoRefreshOnStartup;

    @Override
    public int refresh() {
        Integer rows = jdbc.queryForObject(
            "SELECT gcrf_region.refresh_book_search_mview()", Integer.class);
        int count = rows == null ? 0 : rows;
        log.info("OPAC search mview refreshed: {} rows", count);
        return count;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (autoRefreshOnStartup) {
            try {
                refresh();
            } catch (Exception e) {
                log.warn("startup mview refresh failed: {}", e.getMessage());
            }
        }
    }
}
```

- [ ] **Step 5: Run test to verify pass**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl opac-service -am -Dtest=SearchMviewServiceTest
```

Expected: `Tests run: 2, Failures: 0`.

- [ ] **Step 6: Commit**

```bash
git add backend/opac-service/src/main/java/com/gcrf/library/opac/service/SearchMviewService.java \
        backend/opac-service/src/main/java/com/gcrf/library/opac/service/impl/SearchMviewServiceImpl.java \
        backend/opac-service/src/test/java/com/gcrf/library/opac/service/SearchMviewServiceTest.java
git commit -m "feat(common): add SearchMviewService for cross-school mview refresh"
```

---

### Task 6: BookSearchMapper + SearchService

**Files:**

- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/mapper/BookSearchMapper.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/service/SearchService.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/service/impl/SearchServiceImpl.java`
- Create: `backend/opac-service/src/test/java/com/gcrf/library/opac/service/SearchServiceTest.java`

- [ ] **Step 1: Write failing test**

```java
// backend/opac-service/src/test/java/com/gcrf/library/opac/service/SearchServiceTest.java
package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.dto.SearchRequest;
import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import com.gcrf.library.opac.domain.vo.PageVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class SearchServiceTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired SearchService search;
    @Autowired SearchMviewService mview;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.execute("DROP SCHEMA IF EXISTS school_000001 CASCADE");
        jdbc.execute("DROP SCHEMA IF EXISTS school_000002 CASCADE");
        jdbc.execute("CREATE SCHEMA school_000001");
        jdbc.execute("CREATE TABLE school_000001.book_catalog ("
            + " id BIGSERIAL PRIMARY KEY, isbn TEXT, title TEXT, author TEXT,"
            + " classification TEXT, total_count INT DEFAULT 0,"
            + " available_count INT DEFAULT 0,"
            + " created_at TIMESTAMPTZ NOT NULL DEFAULT NOW())");
        jdbc.update("INSERT INTO school_000001.book_catalog "
            + "(isbn, title, author, classification, total_count, available_count) VALUES "
            + "('9787111000001', '深入理解计算机系统', 'Bryant', 'TP', 5, 3),"
            + "('9787111000002', '算法导论', 'Cormen', 'TP', 8, 7),"
            + "('9787020002207', '红楼梦', '曹雪芹', 'I2', 4, 2)");
        jdbc.execute("CREATE SCHEMA school_000002");
        jdbc.execute("CREATE TABLE school_000002.book_catalog (LIKE school_000001.book_catalog INCLUDING ALL)");
        jdbc.update("INSERT INTO school_000002.book_catalog "
            + "(isbn, title, author, classification, total_count, available_count) VALUES "
            + "('9787111000001', '深入理解计算机系统', 'Bryant', 'TP', 3, 3)");
        mview.refresh();
    }

    @Test
    void search_byKeyword_findsAcrossSchools() {
        SearchRequest req = new SearchRequest();
        req.setQ("深入");
        PageVO<BookSearchItemVO> p = search.search(req);
        assertThat(p.getTotal()).isEqualTo(2);
        assertThat(p.getRecords()).extracting(BookSearchItemVO::getSchoolSchema)
            .containsExactlyInAnyOrder("school_000001", "school_000002");
    }

    @Test
    void search_byClc_filtersToCategory() {
        SearchRequest req = new SearchRequest();
        req.setClc("I");
        PageVO<BookSearchItemVO> p = search.search(req);
        assertThat(p.getTotal()).isEqualTo(1);
        assertThat(p.getRecords().get(0).getTitle()).isEqualTo("红楼梦");
    }

    @Test
    void search_bySchool_limitsToOneSchool() {
        SearchRequest req = new SearchRequest();
        req.setSchool("school_000001");
        PageVO<BookSearchItemVO> p = search.search(req);
        assertThat(p.getTotal()).isEqualTo(3);
    }

    @Test
    void search_emptyQuery_returnsAll() {
        PageVO<BookSearchItemVO> p = search.search(new SearchRequest());
        assertThat(p.getTotal()).isEqualTo(4);
    }

    @Test
    void search_pagination() {
        SearchRequest req = new SearchRequest();
        req.setPageNum(1); req.setPageSize(2);
        PageVO<BookSearchItemVO> p = search.search(req);
        assertThat(p.getRecords()).hasSize(2);
        assertThat(p.getTotal()).isEqualTo(4);
    }
}
```

- [ ] **Step 2: Run test to confirm failure**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl opac-service -am -Dtest=SearchServiceTest
```

Expected: COMPILATION FAILURE.

- [ ] **Step 3: Mapper**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/mapper/BookSearchMapper.java
package com.gcrf.library.opac.mapper;

import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BookSearchMapper {

    @Select("""
        <script>
        SELECT school_schema, book_id, isbn, title, author, classification,
               total_count, available_count, created_at
          FROM gcrf_region.book_search_mview
         <where>
           <if test="q != null and q != ''">
             AND (title ILIKE CONCAT('%', #{q}, '%')
               OR author ILIKE CONCAT('%', #{q}, '%')
               OR isbn ILIKE CONCAT(#{q}, '%'))
           </if>
           <if test="clc != null and clc != ''">
             AND classification LIKE CONCAT(#{clc}, '%')
           </if>
           <if test="school != null and school != ''">
             AND school_schema = #{school}
           </if>
         </where>
         ORDER BY title
         LIMIT #{pageSize} OFFSET #{offset}
        </script>
        """)
    List<BookSearchItemVO> search(@Param("q") String q, @Param("clc") String clc,
                                  @Param("school") String school,
                                  @Param("pageSize") int pageSize, @Param("offset") int offset);

    @Select("""
        <script>
        SELECT count(*) FROM gcrf_region.book_search_mview
         <where>
           <if test="q != null and q != ''">
             AND (title ILIKE CONCAT('%', #{q}, '%')
               OR author ILIKE CONCAT('%', #{q}, '%')
               OR isbn ILIKE CONCAT(#{q}, '%'))
           </if>
           <if test="clc != null and clc != ''">
             AND classification LIKE CONCAT(#{clc}, '%')
           </if>
           <if test="school != null and school != ''">
             AND school_schema = #{school}
           </if>
         </where>
        </script>
        """)
    long count(@Param("q") String q, @Param("clc") String clc, @Param("school") String school);
}
```

- [ ] **Step 4: Service interface**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/service/SearchService.java
package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.dto.SearchRequest;
import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import com.gcrf.library.opac.domain.vo.PageVO;

public interface SearchService {
    PageVO<BookSearchItemVO> search(SearchRequest req);
}
```

- [ ] **Step 5: Service impl**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/service/impl/SearchServiceImpl.java
package com.gcrf.library.opac.service.impl;

import com.gcrf.library.opac.domain.dto.SearchRequest;
import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import com.gcrf.library.opac.domain.vo.PageVO;
import com.gcrf.library.opac.mapper.BookSearchMapper;
import com.gcrf.library.opac.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final BookSearchMapper mapper;

    @Override
    public PageVO<BookSearchItemVO> search(SearchRequest req) {
        int pageNum = Math.max(1, req.getPageNum());
        int pageSize = Math.max(1, Math.min(100, req.getPageSize()));
        int offset = (pageNum - 1) * pageSize;
        List<BookSearchItemVO> records =
            mapper.search(req.getQ(), req.getClc(), req.getSchool(), pageSize, offset);
        long total = mapper.count(req.getQ(), req.getClc(), req.getSchool());
        return new PageVO<>(records, total, pageNum, pageSize);
    }
}
```

- [ ] **Step 6: Run test**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl opac-service -am -Dtest=SearchServiceTest
```

Expected: `Tests run: 5, Failures: 0`.

- [ ] **Step 7: Commit**

```bash
git add backend/opac-service/src/main/java/com/gcrf/library/opac/mapper/BookSearchMapper.java \
        backend/opac-service/src/main/java/com/gcrf/library/opac/service/SearchService.java \
        backend/opac-service/src/main/java/com/gcrf/library/opac/service/impl/SearchServiceImpl.java \
        backend/opac-service/src/test/java/com/gcrf/library/opac/service/SearchServiceTest.java
git commit -m "feat(common): add OPAC SearchService with cross-school keyword + clc + school filters"
```

---

### Task 7: BookDetailService — by ISBN with cross-school availability

**Files:**

- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/mapper/CrossSchoolMapper.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/service/BookDetailService.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/service/impl/BookDetailServiceImpl.java`
- Create: `backend/opac-service/src/test/java/com/gcrf/library/opac/service/BookDetailServiceTest.java`

- [ ] **Step 1: Write failing test**

```java
// backend/opac-service/src/test/java/com/gcrf/library/opac/service/BookDetailServiceTest.java
package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.BookDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class BookDetailServiceTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired BookDetailService detailSvc;
    @Autowired SearchMviewService mview;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.execute("DROP SCHEMA IF EXISTS school_000001 CASCADE");
        jdbc.execute("DROP SCHEMA IF EXISTS school_000002 CASCADE");
        jdbc.execute("CREATE SCHEMA school_000001");
        jdbc.execute("CREATE TABLE school_000001.book_catalog ("
            + " id BIGSERIAL PRIMARY KEY, isbn TEXT, title TEXT, author TEXT,"
            + " classification TEXT, total_count INT DEFAULT 0,"
            + " available_count INT DEFAULT 0, created_at TIMESTAMPTZ DEFAULT NOW())");
        jdbc.execute("CREATE TABLE school_000001.school_meta (school_code TEXT PRIMARY KEY, school_name TEXT)");
        jdbc.update("INSERT INTO school_000001.school_meta VALUES ('s1', '实验小学')");
        jdbc.update("INSERT INTO school_000001.book_catalog (isbn, title, author, classification, total_count, available_count)"
            + " VALUES ('9787111000001', '深入理解计算机系统', 'Bryant', 'TP', 5, 3)");

        jdbc.execute("CREATE SCHEMA school_000002");
        jdbc.execute("CREATE TABLE school_000002.book_catalog (LIKE school_000001.book_catalog INCLUDING ALL)");
        jdbc.execute("CREATE TABLE school_000002.school_meta (school_code TEXT PRIMARY KEY, school_name TEXT)");
        jdbc.update("INSERT INTO school_000002.school_meta VALUES ('s2', '第二中学')");
        jdbc.update("INSERT INTO school_000002.book_catalog (isbn, title, author, classification, total_count, available_count)"
            + " VALUES ('9787111000001', '深入理解计算机系统', 'Bryant', 'TP', 3, 1)");

        mview.refresh();
    }

    @Test
    void getByIsbn_aggregatesAcrossSchools() {
        BookDetailVO d = detailSvc.getByIsbn("9787111000001");
        assertThat(d.getTitle()).isEqualTo("深入理解计算机系统");
        assertThat(d.getSchools()).hasSize(2);
        assertThat(d.getSchools()).extracting("schoolName")
            .containsExactlyInAnyOrder("实验小学", "第二中学");
    }

    @Test
    void getByIsbn_notFound_returnsNull() {
        BookDetailVO d = detailSvc.getByIsbn("0000000000");
        assertThat(d).isNull();
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl opac-service -am -Dtest=BookDetailServiceTest
```

- [ ] **Step 3: Mapper**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/mapper/CrossSchoolMapper.java
package com.gcrf.library.opac.mapper;

import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import com.gcrf.library.opac.domain.vo.SchoolAvailabilityVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrossSchoolMapper {

    /** All books matching the given ISBN across all schools (from mview). */
    @Select("SELECT school_schema, book_id, isbn, title, author, classification,"
          + " total_count, available_count, created_at"
          + " FROM gcrf_region.book_search_mview WHERE isbn = #{isbn}")
    List<BookSearchItemVO> findByIsbn(@Param("isbn") String isbn);
}
```

- [ ] **Step 4: Service interface**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/service/BookDetailService.java
package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.BookDetailVO;

public interface BookDetailService {
    /** Returns null if the ISBN isn't found in any school. */
    BookDetailVO getByIsbn(String isbn);
}
```

- [ ] **Step 5: Service impl**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/service/impl/BookDetailServiceImpl.java
package com.gcrf.library.opac.service.impl;

import com.gcrf.library.opac.domain.vo.BookDetailVO;
import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import com.gcrf.library.opac.domain.vo.SchoolAvailabilityVO;
import com.gcrf.library.opac.mapper.CrossSchoolMapper;
import com.gcrf.library.opac.service.BookDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookDetailServiceImpl implements BookDetailService {

    private final CrossSchoolMapper mapper;
    private final JdbcTemplate jdbc;

    @Override
    public BookDetailVO getByIsbn(String isbn) {
        List<BookSearchItemVO> rows = mapper.findByIsbn(isbn);
        if (rows.isEmpty()) return null;

        BookSearchItemVO any = rows.get(0);
        BookDetailVO d = new BookDetailVO();
        d.setIsbn(any.getIsbn());
        d.setTitle(any.getTitle());
        d.setAuthor(any.getAuthor());
        d.setClassification(any.getClassification());

        List<SchoolAvailabilityVO> schools = new ArrayList<>();
        for (BookSearchItemVO r : rows) {
            String name = lookupSchoolName(r.getSchoolSchema());
            schools.add(new SchoolAvailabilityVO(
                r.getSchoolSchema(), name, r.getTotalCount(), r.getAvailableCount()));
        }
        d.setSchools(schools);
        return d;
    }

    private String lookupSchoolName(String schema) {
        if (schema == null || !schema.matches("^school_\\d+$")) return schema;
        try {
            return jdbc.queryForObject(
                "SELECT school_name FROM " + schema + ".school_meta LIMIT 1", String.class);
        } catch (Exception e) {
            return schema;
        }
    }
}
```

- [ ] **Step 6: Run test**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl opac-service -am -Dtest=BookDetailServiceTest
```

Expected: `Tests run: 2, Failures: 0`.

- [ ] **Step 7: Commit**

```bash
git add backend/opac-service/src/main/java/com/gcrf/library/opac/mapper/CrossSchoolMapper.java \
        backend/opac-service/src/main/java/com/gcrf/library/opac/service/BookDetailService.java \
        backend/opac-service/src/main/java/com/gcrf/library/opac/service/impl/BookDetailServiceImpl.java \
        backend/opac-service/src/test/java/com/gcrf/library/opac/service/BookDetailServiceTest.java
git commit -m "feat(common): add OPAC BookDetailService with cross-school availability"
```

---

### Task 8: ClcService — load 22 categories + browse

**Files:**

- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/service/ClcService.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/service/impl/ClcServiceImpl.java`
- Create: `backend/opac-service/src/test/java/com/gcrf/library/opac/service/ClcServiceTest.java`

- [ ] **Step 1: Failing test**

```java
// backend/opac-service/src/test/java/com/gcrf/library/opac/service/ClcServiceTest.java
package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.ClcNodeVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ClcServiceTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired ClcService clc;

    @Test
    void getTree_loads22Categories() {
        List<ClcNodeVO> tree = clc.getTree();
        assertThat(tree).hasSize(22);
        ClcNodeVO i = tree.stream().filter(n -> "I".equals(n.getCode())).findFirst().orElseThrow();
        assertThat(i.getName()).isEqualTo("文学");
        assertThat(i.getChildren()).extracting("code")
            .contains("I0", "I2", "I3/7");
    }
}
```

- [ ] **Step 2: Run failing**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl opac-service -am -Dtest=ClcServiceTest
```

- [ ] **Step 3: Interface**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/service/ClcService.java
package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.ClcNodeVO;

import java.util.List;

public interface ClcService {
    List<ClcNodeVO> getTree();
}
```

- [ ] **Step 4: Impl**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/service/impl/ClcServiceImpl.java
package com.gcrf.library.opac.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.opac.domain.vo.ClcNodeVO;
import com.gcrf.library.opac.service.ClcService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClcServiceImpl implements ClcService {

    private final ObjectMapper mapper;
    private List<ClcNodeVO> tree = Collections.emptyList();

    @PostConstruct
    void load() {
        try (InputStream in = new ClassPathResource("data/clc-22-categories.json").getInputStream()) {
            tree = mapper.readValue(in, new TypeReference<List<ClcNodeVO>>() {});
            log.info("CLC tree loaded: {} top-level categories", tree.size());
        } catch (Exception e) {
            log.error("failed to load CLC tree", e);
        }
    }

    @Override
    public List<ClcNodeVO> getTree() { return tree; }
}
```

- [ ] **Step 5: Run test**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl opac-service -am -Dtest=ClcServiceTest
```

Expected: `Tests run: 1, Failures: 0`.

- [ ] **Step 6: Commit**

```bash
git add backend/opac-service/src/main/java/com/gcrf/library/opac/service/ClcService.java \
        backend/opac-service/src/main/java/com/gcrf/library/opac/service/impl/ClcServiceImpl.java \
        backend/opac-service/src/test/java/com/gcrf/library/opac/service/ClcServiceTest.java
git commit -m "feat(common): add OPAC ClcService loading CLC 22 categories from JSON"
```

---

### Task 9: NewArrivalsService

**Files:**

- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/service/NewArrivalsService.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/service/impl/NewArrivalsServiceImpl.java`
- Create: `backend/opac-service/src/test/java/com/gcrf/library/opac/service/NewArrivalsServiceTest.java`

- [ ] **Step 1: Failing test**

```java
// backend/opac-service/src/test/java/com/gcrf/library/opac/service/NewArrivalsServiceTest.java
package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class NewArrivalsServiceTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired NewArrivalsService svc;
    @Autowired SearchMviewService mview;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.execute("DROP SCHEMA IF EXISTS school_000001 CASCADE");
        jdbc.execute("CREATE SCHEMA school_000001");
        jdbc.execute("CREATE TABLE school_000001.book_catalog ("
            + " id BIGSERIAL PRIMARY KEY, isbn TEXT, title TEXT, author TEXT,"
            + " classification TEXT, total_count INT DEFAULT 0,"
            + " available_count INT DEFAULT 0, created_at TIMESTAMPTZ DEFAULT NOW())");
        jdbc.update("INSERT INTO school_000001.book_catalog (isbn, title, author, classification, total_count, available_count, created_at)"
            + " VALUES ('1', 'Old', 'a', 'I', 1, 1, NOW() - INTERVAL '60 days')");
        jdbc.update("INSERT INTO school_000001.book_catalog (isbn, title, author, classification, total_count, available_count, created_at)"
            + " VALUES ('2', 'New', 'b', 'I', 1, 1, NOW())");
        mview.refresh();
    }

    @Test
    void newArrivals_filtersByDays() {
        List<BookSearchItemVO> recent = svc.newArrivals(null, 30, 10);
        assertThat(recent).hasSize(1);
        assertThat(recent.get(0).getTitle()).isEqualTo("New");
    }
}
```

- [ ] **Step 2: Run failing**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl opac-service -am -Dtest=NewArrivalsServiceTest
```

- [ ] **Step 3: Interface + impl**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/service/NewArrivalsService.java
package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import java.util.List;

public interface NewArrivalsService {
    List<BookSearchItemVO> newArrivals(String school, int days, int limit);
}
```

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/service/impl/NewArrivalsServiceImpl.java
package com.gcrf.library.opac.service.impl;

import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import com.gcrf.library.opac.service.NewArrivalsService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewArrivalsServiceImpl implements NewArrivalsService {

    private final JdbcTemplate jdbc;

    @Override
    public List<BookSearchItemVO> newArrivals(String school, int days, int limit) {
        int safeDays = Math.max(1, Math.min(365, days));
        int safeLimit = Math.max(1, Math.min(100, limit));
        String sql = "SELECT school_schema, book_id, isbn, title, author, classification,"
                   + " total_count, available_count, created_at"
                   + " FROM gcrf_region.book_search_mview"
                   + " WHERE created_at >= NOW() - (? || ' days')::INTERVAL";
        if (school != null && !school.isBlank()) {
            sql += " AND school_schema = ?";
        }
        sql += " ORDER BY created_at DESC LIMIT ?";
        Object[] args = (school != null && !school.isBlank())
            ? new Object[] { String.valueOf(safeDays), school, safeLimit }
            : new Object[] { String.valueOf(safeDays), safeLimit };
        return jdbc.query(sql, args, new BeanPropertyRowMapper<>(BookSearchItemVO.class));
    }
}
```

- [ ] **Step 4: Run test**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl opac-service -am -Dtest=NewArrivalsServiceTest
```

Expected: `Tests run: 1, Failures: 0`.

- [ ] **Step 5: Commit**

```bash
git add backend/opac-service/src/main/java/com/gcrf/library/opac/service/NewArrivalsService.java \
        backend/opac-service/src/main/java/com/gcrf/library/opac/service/impl/NewArrivalsServiceImpl.java \
        backend/opac-service/src/test/java/com/gcrf/library/opac/service/NewArrivalsServiceTest.java
git commit -m "feat(common): add OPAC NewArrivalsService"
```

---

### Task 10: RecommendService — same-classification top-N

**Files:**

- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/service/RecommendService.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/service/impl/RecommendServiceImpl.java`
- Create: `backend/opac-service/src/test/java/com/gcrf/library/opac/service/RecommendServiceTest.java`

- [ ] **Step 1: Failing test**

```java
// backend/opac-service/src/test/java/com/gcrf/library/opac/service/RecommendServiceTest.java
package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class RecommendServiceTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired RecommendService svc;
    @Autowired SearchMviewService mview;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.execute("DROP SCHEMA IF EXISTS school_000001 CASCADE");
        jdbc.execute("CREATE SCHEMA school_000001");
        jdbc.execute("CREATE TABLE school_000001.book_catalog ("
            + " id BIGSERIAL PRIMARY KEY, isbn TEXT, title TEXT, author TEXT,"
            + " classification TEXT, total_count INT DEFAULT 0,"
            + " available_count INT DEFAULT 0, created_at TIMESTAMPTZ DEFAULT NOW())");
        jdbc.update("INSERT INTO school_000001.book_catalog (isbn, title, author, classification, total_count, available_count) VALUES "
            + "('isbn1','深入理解计算机系统','Bryant','TP',5,3),"
            + "('isbn2','算法导论','Cormen','TP',8,7),"
            + "('isbn3','红楼梦','曹雪芹','I2',4,2)");
        mview.refresh();
    }

    @Test
    void related_returnsSameClassification() {
        List<BookSearchItemVO> rel = svc.related("isbn1", 5);
        assertThat(rel).extracting("classification").allMatch(c -> c.equals("TP"));
        assertThat(rel).extracting("isbn").doesNotContain("isbn1");
    }
}
```

- [ ] **Step 2: Run failing**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl opac-service -am -Dtest=RecommendServiceTest
```

- [ ] **Step 3: Interface + impl**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/service/RecommendService.java
package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import java.util.List;

public interface RecommendService {
    List<BookSearchItemVO> related(String isbn, int limit);
}
```

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/service/impl/RecommendServiceImpl.java
package com.gcrf.library.opac.service.impl;

import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import com.gcrf.library.opac.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    private final JdbcTemplate jdbc;

    @Override
    public List<BookSearchItemVO> related(String isbn, int limit) {
        int safeLimit = Math.max(1, Math.min(50, limit));
        String clc = jdbc.query(
            "SELECT classification FROM gcrf_region.book_search_mview WHERE isbn = ? LIMIT 1",
            new Object[] { isbn },
            rs -> rs.next() ? rs.getString(1) : null);
        if (clc == null) return Collections.emptyList();

        return jdbc.query(
            "SELECT school_schema, book_id, isbn, title, author, classification,"
          + " total_count, available_count, created_at"
          + " FROM gcrf_region.book_search_mview"
          + " WHERE classification = ? AND isbn <> ?"
          + " ORDER BY available_count DESC LIMIT ?",
            new Object[] { clc, isbn, safeLimit },
            new BeanPropertyRowMapper<>(BookSearchItemVO.class));
    }
}
```

- [ ] **Step 4: Run test**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl opac-service -am -Dtest=RecommendServiceTest
```

Expected: `Tests run: 1, Failures: 0`.

- [ ] **Step 5: Commit**

```bash
git add backend/opac-service/src/main/java/com/gcrf/library/opac/service/RecommendService.java \
        backend/opac-service/src/main/java/com/gcrf/library/opac/service/impl/RecommendServiceImpl.java \
        backend/opac-service/src/test/java/com/gcrf/library/opac/service/RecommendServiceTest.java
git commit -m "feat(common): add OPAC RecommendService (same classification top-N)"
```

---

### Task 11: RankingService — borrow ranking from cross-school borrow_record

**Files:**

- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/service/RankingService.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/service/impl/RankingServiceImpl.java`
- Create: `backend/opac-service/src/test/java/com/gcrf/library/opac/service/RankingServiceTest.java`

The ranking aggregates borrow records across all `school_*` schemas in PG. Use a function for portability:

- [ ] **Step 1: Failing test**

```java
// backend/opac-service/src/test/java/com/gcrf/library/opac/service/RankingServiceTest.java
package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.RankingItemVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class RankingServiceTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired RankingService svc;
    @Autowired SearchMviewService mview;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.execute("DROP SCHEMA IF EXISTS school_000001 CASCADE");
        jdbc.execute("CREATE SCHEMA school_000001");
        jdbc.execute("CREATE TABLE school_000001.book_catalog ("
            + " id BIGSERIAL PRIMARY KEY, isbn TEXT, title TEXT, author TEXT,"
            + " classification TEXT, total_count INT DEFAULT 0, available_count INT DEFAULT 0,"
            + " created_at TIMESTAMPTZ DEFAULT NOW())");
        jdbc.execute("CREATE TABLE school_000001.book_copy ("
            + " id BIGSERIAL PRIMARY KEY, catalog_id BIGINT, barcode TEXT)");
        jdbc.execute("CREATE TABLE school_000001.borrow_record ("
            + " id BIGSERIAL PRIMARY KEY, copy_id BIGINT, reader_id BIGINT,"
            + " borrow_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),"
            + " due_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),"
            + " return_at TIMESTAMPTZ)");
        jdbc.update("INSERT INTO school_000001.book_catalog (isbn, title, author, classification) VALUES "
            + "('isbn1','深入理解计算机系统','Bryant','TP'),"
            + "('isbn2','算法导论','Cormen','TP')");
        jdbc.update("INSERT INTO school_000001.book_copy (catalog_id, barcode) VALUES "
            + "(1,'b1'),(1,'b2'),(2,'b3')");
        jdbc.update("INSERT INTO school_000001.borrow_record (copy_id, reader_id) VALUES "
            + "(1, 1), (1, 2), (2, 3), (3, 4)");
        // book 1 has 3 borrows, book 2 has 1
        mview.refresh();
    }

    @Test
    void borrowRanking_top10_ordersByCount() {
        List<RankingItemVO> top = svc.borrowRanking("THIS_MONTH", 10);
        assertThat(top).hasSizeGreaterThanOrEqualTo(2);
        assertThat(top.get(0).getIsbn()).isEqualTo("isbn1");
        assertThat(top.get(0).getBorrowCount()).isEqualTo(3L);
    }
}
```

- [ ] **Step 2: Run failing**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl opac-service -am -Dtest=RankingServiceTest
```

- [ ] **Step 3: Interface + impl**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/service/RankingService.java
package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.RankingItemVO;
import java.util.List;

public interface RankingService {
    /** range: THIS_WEEK / THIS_MONTH / THIS_TERM (last 6 months) */
    List<RankingItemVO> borrowRanking(String range, int limit);
}
```

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/service/impl/RankingServiceImpl.java
package com.gcrf.library.opac.service.impl;

import com.gcrf.library.opac.domain.vo.RankingItemVO;
import com.gcrf.library.opac.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final JdbcTemplate jdbc;

    private String intervalFor(String range) {
        if (range == null) return "30 days";
        return switch (range) {
            case "THIS_WEEK"  -> "7 days";
            case "THIS_TERM"  -> "180 days";
            case "THIS_MONTH" -> "30 days";
            default           -> "30 days";
        };
    }

    @Override
    public List<RankingItemVO> borrowRanking(String range, int limit) {
        int safeLimit = Math.max(1, Math.min(100, limit));
        String interval = intervalFor(range);

        // Discover schools
        List<String> schools = jdbc.queryForList(
            "SELECT nspname FROM pg_namespace WHERE nspname LIKE 'school\\_%' ESCAPE '\\'",
            String.class);
        if (schools.isEmpty()) return new ArrayList<>();

        // Build dynamic UNION ALL across schools
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < schools.size(); i++) {
            if (i > 0) sb.append(" UNION ALL ");
            String s = schools.get(i);
            // safe: discovered from pg_namespace
            sb.append("SELECT c.isbn, c.title, c.author, c.classification, b.id AS borrow_id ")
              .append("FROM ").append(s).append(".borrow_record b ")
              .append("JOIN ").append(s).append(".book_copy bc ON bc.id = b.copy_id ")
              .append("JOIN ").append(s).append(".book_catalog c ON c.id = bc.catalog_id ")
              .append("WHERE b.borrow_at >= NOW() - INTERVAL '").append(interval).append("'");
        }
        String sql = "SELECT isbn, title, author, classification, count(*) AS bc FROM ("
            + sb + ") t GROUP BY isbn, title, author, classification "
            + "ORDER BY bc DESC LIMIT ?";

        AtomicInteger rank = new AtomicInteger(1);
        RowMapper<RankingItemVO> mapper = (rs, i) -> {
            RankingItemVO v = new RankingItemVO();
            v.setRank(rank.getAndIncrement());
            v.setIsbn(rs.getString("isbn"));
            v.setTitle(rs.getString("title"));
            v.setAuthor(rs.getString("author"));
            v.setClassification(rs.getString("classification"));
            v.setBorrowCount(rs.getLong("bc"));
            return v;
        };
        return jdbc.query(sql, mapper, safeLimit);
    }
}
```

- [ ] **Step 4: Run test**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl opac-service -am -Dtest=RankingServiceTest
```

Expected: `Tests run: 1, Failures: 0`.

- [ ] **Step 5: Commit**

```bash
git add backend/opac-service/src/main/java/com/gcrf/library/opac/service/RankingService.java \
        backend/opac-service/src/main/java/com/gcrf/library/opac/service/impl/RankingServiceImpl.java \
        backend/opac-service/src/test/java/com/gcrf/library/opac/service/RankingServiceTest.java
git commit -m "feat(common): add OPAC RankingService aggregating borrow records across schools"
```

---

### Task 12: Rate limit (annotation + Redis + interceptor)

**Files:**

- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/ratelimit/RateLimit.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/ratelimit/RedisRateLimiter.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/ratelimit/RateLimitInterceptor.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/config/WebMvcConfig.java`
- Create: `backend/opac-service/src/test/java/com/gcrf/library/opac/ratelimit/RedisRateLimiterTest.java`

- [ ] **Step 1: Failing test (uses testcontainers Redis)**

```java
// backend/opac-service/src/test/java/com/gcrf/library/opac/ratelimit/RedisRateLimiterTest.java
package com.gcrf.library.opac.ratelimit;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class RedisRateLimiterTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");
    @Container static final RedisContainer REDIS = new RedisContainer("redis:7-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", REDIS::getFirstMappedPort);
        r.add("gcrf.opac.rate-limit.enabled", () -> true);
    }

    @Autowired RedisRateLimiter limiter;

    @Test
    void allows_underLimit() {
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryAcquire("test:1", 5, 1)).isTrue();
        }
    }

    @Test
    void rejects_atLimit() {
        for (int i = 0; i < 3; i++) limiter.tryAcquire("test:2", 3, 1);
        assertThat(limiter.tryAcquire("test:2", 3, 1)).isFalse();
    }
}
```

- [ ] **Step 2: Run failing**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl opac-service -am -Dtest=RedisRateLimiterTest
```

- [ ] **Step 3: Implement**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/ratelimit/RateLimit.java
package com.gcrf.library.opac.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RateLimit {
    /** Max requests in the window. */
    int value();
    /** Window in seconds. */
    int periodSeconds() default 1;
}
```

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/ratelimit/RedisRateLimiter.java
package com.gcrf.library.opac.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** INCR + EXPIRE atomic-ish rate limiter (window starts on first request). */
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private final StringRedisTemplate redis;

    public boolean tryAcquire(String key, int limit, int periodSeconds) {
        String redisKey = "ratelimit:" + key;
        Long count = redis.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redis.expire(redisKey, Duration.ofSeconds(periodSeconds));
        }
        return count != null && count <= limit;
    }
}
```

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/ratelimit/RateLimitInterceptor.java
package com.gcrf.library.opac.ratelimit;

import com.gcrf.library.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisRateLimiter limiter;
    private final ObjectMapper json;

    @Value("${gcrf.opac.rate-limit.enabled:true}")
    private boolean enabled;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        if (!enabled || !(handler instanceof HandlerMethod hm)) return true;

        Method m = hm.getMethod();
        RateLimit ann = m.getAnnotation(RateLimit.class);
        if (ann == null) return true;

        String ip = clientIp(req);
        String key = ip + ":" + m.getDeclaringClass().getSimpleName() + "." + m.getName();
        if (limiter.tryAcquire(key, ann.value(), ann.periodSeconds())) return true;

        res.setStatus(429);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(json.writeValueAsString(Result.error(429, "Too Many Requests")));
        return false;
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
```

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/config/WebMvcConfig.java
package com.gcrf.library.opac.config;

import com.gcrf.library.opac.ratelimit.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/opac/**");
    }
}
```

- [ ] **Step 4: Run test**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl opac-service -am -Dtest=RedisRateLimiterTest
```

Expected: `Tests run: 2, Failures: 0`.

- [ ] **Step 5: Commit**

```bash
git add backend/opac-service/src/main/java/com/gcrf/library/opac/ratelimit/ \
        backend/opac-service/src/main/java/com/gcrf/library/opac/config/WebMvcConfig.java \
        backend/opac-service/src/test/java/com/gcrf/library/opac/ratelimit/RedisRateLimiterTest.java
git commit -m "feat(common): add @RateLimit annotation + Redis-backed limiter + interceptor"
```

---

### Task 13: Controllers — expose all OPAC endpoints

**Files:**

- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/controller/SearchController.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/controller/BookDetailController.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/controller/ClcController.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/controller/RankingController.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/controller/RecommendController.java`
- Create: `backend/opac-service/src/main/java/com/gcrf/library/opac/controller/AdminController.java`
- Create: `backend/opac-service/src/test/java/com/gcrf/library/opac/controller/SearchControllerTest.java`

- [ ] **Step 1: Failing controller test**

```java
// backend/opac-service/src/test/java/com/gcrf/library/opac/controller/SearchControllerTest.java
package com.gcrf.library.opac.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
class SearchControllerTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired MockMvc mvc;

    @Test
    void search_emptyMview_returnsEmptyList() throws Exception {
        mvc.perform(get("/api/v1/opac/search").param("q", "anything"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records").isArray())
            .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void clcTree_returns22() throws Exception {
        mvc.perform(get("/api/v1/opac/clc/tree"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(22));
    }
}
```

- [ ] **Step 2: Run failing**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl opac-service -am -Dtest=SearchControllerTest
```

- [ ] **Step 3: Implement controllers**

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/controller/SearchController.java
package com.gcrf.library.opac.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.opac.domain.dto.SearchRequest;
import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import com.gcrf.library.opac.domain.vo.PageVO;
import com.gcrf.library.opac.ratelimit.RateLimit;
import com.gcrf.library.opac.service.NewArrivalsService;
import com.gcrf.library.opac.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/opac")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final NewArrivalsService newArrivalsService;

    @GetMapping("/search")
    @RateLimit(value = 10, periodSeconds = 1)
    public Result<PageVO<BookSearchItemVO>> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String clc,
        @RequestParam(required = false) String school,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "20") int pageSize) {
        SearchRequest req = new SearchRequest();
        req.setQ(q); req.setClc(clc); req.setSchool(school);
        req.setPageNum(pageNum); req.setPageSize(pageSize);
        return Result.success(searchService.search(req));
    }

    @GetMapping("/new-arrivals")
    @RateLimit(value = 30, periodSeconds = 60)
    public Result<List<BookSearchItemVO>> newArrivals(
        @RequestParam(required = false) String school,
        @RequestParam(defaultValue = "30") int days,
        @RequestParam(defaultValue = "20") int limit) {
        return Result.success(newArrivalsService.newArrivals(school, days, limit));
    }
}
```

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/controller/BookDetailController.java
package com.gcrf.library.opac.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.opac.domain.vo.BookDetailVO;
import com.gcrf.library.opac.ratelimit.RateLimit;
import com.gcrf.library.opac.service.BookDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/opac/books")
@RequiredArgsConstructor
public class BookDetailController {

    private final BookDetailService service;

    @GetMapping("/{isbn}")
    @RateLimit(value = 10, periodSeconds = 1)
    public Result<BookDetailVO> getByIsbn(@PathVariable String isbn) {
        return Result.success(service.getByIsbn(isbn));
    }
}
```

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/controller/ClcController.java
package com.gcrf.library.opac.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.opac.domain.dto.SearchRequest;
import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import com.gcrf.library.opac.domain.vo.ClcNodeVO;
import com.gcrf.library.opac.domain.vo.PageVO;
import com.gcrf.library.opac.ratelimit.RateLimit;
import com.gcrf.library.opac.service.ClcService;
import com.gcrf.library.opac.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/opac/clc")
@RequiredArgsConstructor
public class ClcController {

    private final ClcService clc;
    private final SearchService search;

    @GetMapping("/tree")
    @RateLimit(value = 1, periodSeconds = 60)
    public Result<List<ClcNodeVO>> tree() { return Result.success(clc.getTree()); }

    @GetMapping("/{code}/books")
    @RateLimit(value = 10, periodSeconds = 1)
    public Result<PageVO<BookSearchItemVO>> browse(
        @PathVariable String code,
        @RequestParam(required = false) String school,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "20") int pageSize) {
        SearchRequest req = new SearchRequest();
        req.setClc(code); req.setSchool(school);
        req.setPageNum(pageNum); req.setPageSize(pageSize);
        return Result.success(search.search(req));
    }
}
```

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/controller/RankingController.java
package com.gcrf.library.opac.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.opac.domain.vo.RankingItemVO;
import com.gcrf.library.opac.ratelimit.RateLimit;
import com.gcrf.library.opac.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/opac/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService service;

    @GetMapping("/borrow")
    @RateLimit(value = 30, periodSeconds = 60)
    public Result<List<RankingItemVO>> borrow(
        @RequestParam(defaultValue = "THIS_MONTH") String range,
        @RequestParam(defaultValue = "10") int limit) {
        return Result.success(service.borrowRanking(range, limit));
    }
}
```

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/controller/RecommendController.java
package com.gcrf.library.opac.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import com.gcrf.library.opac.ratelimit.RateLimit;
import com.gcrf.library.opac.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/opac/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService service;

    @GetMapping("/related")
    @RateLimit(value = 10, periodSeconds = 1)
    public Result<List<BookSearchItemVO>> related(
        @RequestParam String isbn,
        @RequestParam(defaultValue = "10") int limit) {
        return Result.success(service.related(isbn, limit));
    }
}
```

```java
// backend/opac-service/src/main/java/com/gcrf/library/opac/controller/AdminController.java
package com.gcrf.library.opac.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.opac.service.SearchMviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/opac/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SearchMviewService mview;

    @PostMapping("/refresh-search-mview")
    public Result<Integer> refresh() {
        return Result.success(mview.refresh());
    }
}
```

- [ ] **Step 4: Run test**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl opac-service -am -Dtest=SearchControllerTest
```

Expected: `Tests run: 2, Failures: 0`.

- [ ] **Step 5: Commit**

```bash
git add backend/opac-service/src/main/java/com/gcrf/library/opac/controller/ \
        backend/opac-service/src/test/java/com/gcrf/library/opac/controller/SearchControllerTest.java
git commit -m "feat(common): expose OPAC REST endpoints (search/detail/clc/ranking/recommend/admin)"
```

---

### Task 14: K8s deployment manifest

**Files:**

- Modify: `deployment/k8s/10-services.yaml`
- Modify: `deployment/k8s/02-configmap.yaml`
- Patch (live cluster): `gcrf-nginx-config` configmap

- [ ] **Step 1: Read existing patterns**

Inspect `deployment/k8s/10-services.yaml` to find the `gcrf-org` Deployment (added in plan-A Task 19). Match its style for env, probes, volumes.

- [ ] **Step 2: Append gcrf-opac block**

Append to `deployment/k8s/10-services.yaml`:

```yaml
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gcrf-opac
  namespace: gcrf-prod
spec:
  replicas: 1
  selector:
    matchLabels:
      app: gcrf-opac
  template:
    metadata:
      labels:
        app: gcrf-opac
    spec:
      containers:
        - name: opac-service
          image: gcrf/opac-service:latest
          imagePullPolicy: Never
          ports:
            - containerPort: 8092
          env:
            - { name: SPRING_PROFILES_ACTIVE, value: "prod,k8s" }
            - {
                name: DB_HOST,
                valueFrom:
                  { configMapKeyRef: { name: gcrf-config, key: db.host } },
              }
            - { name: DB_NAME, value: "gcrf_main" }
            - {
                name: DB_USER,
                valueFrom:
                  { configMapKeyRef: { name: gcrf-config, key: db.user } },
              }
            - {
                name: DB_PASSWORD,
                valueFrom:
                  { secretKeyRef: { name: gcrf-db-secret, key: db-password } },
              }
            - {
                name: REDIS_HOST,
                valueFrom:
                  { configMapKeyRef: { name: gcrf-config, key: redis.host } },
              }
            - { name: REDIS_DB, value: "2" }
            - {
                name: REDIS_PASSWORD,
                valueFrom:
                  {
                    secretKeyRef: { name: gcrf-db-secret, key: redis-password },
                  },
              }
          readinessProbe:
            httpGet: { path: /actuator/health, port: 8092 }
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:
            httpGet: { path: /actuator/health, port: 8092 }
            initialDelaySeconds: 60
            periodSeconds: 20
---
apiVersion: v1
kind: Service
metadata:
  name: gcrf-opac
  namespace: gcrf-prod
spec:
  selector:
    app: gcrf-opac
  ports:
    - port: 8092
      targetPort: 8092
```

(Use the EXACT secret/configmap key names that other services use; `gcrf-db-secret` was confirmed in plan-A Task 19.)

- [ ] **Step 3: Add to service-discovery configmap**

Edit `deployment/k8s/02-configmap.yaml`. Inside `gcrf-service-discovery` data, find `instances:` and add:

```yaml
opac-service:
  - uri: http://gcrf-opac:8092
```

- [ ] **Step 4: Create Dockerfile**

```dockerfile
# /tmp/gcrf-deploy/Dockerfile-opac-service
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY opac-service.jar app.jar
EXPOSE 8092
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

- [ ] **Step 5: Build + ship + import + apply + rollout**

```bash
# Build
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean package -pl opac-service -am -DskipTests -q
cp opac-service/target/opac-service-1.0.0-SNAPSHOT.jar /tmp/gcrf-deploy/opac-service.jar

cd /tmp/gcrf-deploy
docker build --platform linux/amd64 -t gcrf/opac-service:latest -f Dockerfile-opac-service .
docker save gcrf/opac-service:latest | gzip > opac-service.tar.gz

# Ship + import on all 3 nodes
for host in t1@192.168.1.20 t2@192.168.1.19 t3@192.168.1.21; do
  sshpass -p gcrf scp -o StrictHostKeyChecking=no -o PreferredAuthentications=password \
    opac-service.tar.gz $host:/tmp/
  sshpass -p gcrf ssh -o StrictHostKeyChecking=no -o PreferredAuthentications=password $host \
    "echo gcrf | sudo -S sh -c 'gunzip -c /tmp/opac-service.tar.gz | ctr -n k8s.io images import -'"
done

# Apply manifests
sshpass -p gcrf scp -o StrictHostKeyChecking=no -o PreferredAuthentications=password \
  /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/deployment/k8s/02-configmap.yaml \
  t1@192.168.1.20:/tmp/02-configmap.yaml
sshpass -p gcrf scp -o StrictHostKeyChecking=no -o PreferredAuthentications=password \
  /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/deployment/k8s/10-services.yaml \
  t1@192.168.1.20:/tmp/10-services.yaml

sshpass -p gcrf ssh -o StrictHostKeyChecking=no -o PreferredAuthentications=password t1@192.168.1.20 \
  "echo gcrf | sudo -S kubectl apply -f /tmp/02-configmap.yaml && echo gcrf | sudo -S kubectl apply -f /tmp/10-services.yaml"

sshpass -p gcrf ssh -o StrictHostKeyChecking=no -o PreferredAuthentications=password t1@192.168.1.20 \
  "echo gcrf | sudo -S kubectl rollout status deployment/gcrf-opac -n gcrf-prod --timeout=180s"
```

- [ ] **Step 6: Patch nginx configmap to route /api/v1/opac**

```bash
# Get the current nginx configmap, edit the location block, apply
sshpass -p gcrf ssh -o StrictHostKeyChecking=no -o PreferredAuthentications=password t1@192.168.1.20 \
  "echo gcrf | sudo -S kubectl get configmap gcrf-nginx-config -n gcrf-prod -o yaml" \
  > /tmp/gcrf-nginx-config.yaml

# Manually inject location /api/v1/opac block. Use sed/awk:
python3 - <<'PY'
import re
with open("/tmp/gcrf-nginx-config.yaml") as f:
    content = f.read()
new_block = """        location /api/v1/opac {
            proxy_pass http://gcrf-opac:8092;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        }
"""
if "location /api/v1/opac" in content:
    print("already has /api/v1/opac route, skipping")
else:
    # insert before the catch-all 'location /' block
    content = content.replace("        location / {", new_block + "        location / {")
    with open("/tmp/gcrf-nginx-config.yaml", "w") as f:
        f.write(content)
    print("patched")
PY

# Apply
sshpass -p gcrf scp -o StrictHostKeyChecking=no -o PreferredAuthentications=password \
  /tmp/gcrf-nginx-config.yaml t1@192.168.1.20:/tmp/
sshpass -p gcrf ssh -o StrictHostKeyChecking=no -o PreferredAuthentications=password t1@192.168.1.20 \
  "echo gcrf | sudo -S kubectl apply -f /tmp/gcrf-nginx-config.yaml"
sshpass -p gcrf ssh -o StrictHostKeyChecking=no -o PreferredAuthentications=password t1@192.168.1.20 \
  "echo gcrf | sudo -S kubectl rollout restart deployment/gcrf-web-admin -n gcrf-prod"
```

- [ ] **Step 7: Commit manifest changes**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add deployment/k8s/10-services.yaml deployment/k8s/02-configmap.yaml
git commit -m "feat(infra): add gcrf-opac K8s deployment + service-discovery entry"
```

---

### Task 15: End-to-end verification

- [ ] **Step 1: Confirm `/actuator/health` is UP**

```bash
sshpass -p gcrf ssh -o StrictHostKeyChecking=no -o PreferredAuthentications=password t1@192.168.1.20 \
  "echo gcrf | sudo -S kubectl exec -n gcrf-prod deployment/gcrf-opac -- wget -qO- http://localhost:8092/actuator/health"
```

Expected: `{"status":"UP",...}`.

- [ ] **Step 2: Refresh mview to pick up plan-A's school_000003 / 000006 (created in plan-A Task 20)**

```bash
curl -sf -X POST http://192.168.1.19:31080/api/v1/opac/admin/refresh-search-mview
```

Expected: `{"code":200,"data":N}` where N is row count (could be 0 if no real book data was seeded into the school schemas; that's fine).

- [ ] **Step 3: Seed mock book data into one of the existing schools**

```bash
sshpass -p gcrf ssh -o StrictHostKeyChecking=no -o PreferredAuthentications=password t1@192.168.1.20 \
  "echo gcrf | sudo -S kubectl exec -n edu-infra postgresql-0 -- psql -U postgres -d gcrf_main -c \"
INSERT INTO school_000003.book_catalog (isbn, title, author, classification, total_count, available_count) VALUES
  ('9787111000001', '深入理解计算机系统', 'Bryant', 'TP', 5, 3),
  ('9787111000002', '算法导论', 'Cormen', 'TP', 8, 7),
  ('9787020002207', '红楼梦', '曹雪芹', 'I2', 4, 2)
ON CONFLICT DO NOTHING;\""

curl -sf -X POST http://192.168.1.19:31080/api/v1/opac/admin/refresh-search-mview
```

Expected: now `{"code":200,"data":3}` (or more if school_000006 also seeded).

- [ ] **Step 4: API smoke test through nginx**

```bash
echo ">>> search by keyword"
curl -sf "http://192.168.1.19:31080/api/v1/opac/search?q=深入" | python3 -m json.tool

echo ">>> book detail"
curl -sf "http://192.168.1.19:31080/api/v1/opac/books/9787111000001" | python3 -m json.tool

echo ">>> CLC tree"
curl -sf "http://192.168.1.19:31080/api/v1/opac/clc/tree" | python3 -c "import sys,json;print(len(json.load(sys.stdin)['data']))"

echo ">>> CLC browse for I"
curl -sf "http://192.168.1.19:31080/api/v1/opac/clc/I/books" | python3 -m json.tool

echo ">>> recommend related to 9787111000001"
curl -sf "http://192.168.1.19:31080/api/v1/opac/recommend/related?isbn=9787111000001" | python3 -m json.tool

echo ">>> rankings (likely empty until borrows seeded)"
curl -sf "http://192.168.1.19:31080/api/v1/opac/rankings/borrow" | python3 -m json.tool
```

Expected: every endpoint returns `{"code":200, ...}` with sensible data.

- [ ] **Step 5: Rate limit smoke test**

```bash
for i in $(seq 1 15); do
  code=$(curl -s -o /dev/null -w '%{http_code}' "http://192.168.1.19:31080/api/v1/opac/search?q=test")
  echo "request $i -> HTTP $code"
done
```

Expected: most return 200, then a few return 429 (search endpoint is 10/sec/IP).

- [ ] **Step 6: Tag release**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git tag -a v1.2.0-plan-C1 -m "Plan-C1 OPAC MVP (PG full-text) complete"
git push origin v1.2.0-plan-C1
git push origin master
```

---

## Self-Review

### Spec coverage

| Spec section                                  | Task                                                                    |
| --------------------------------------------- | ----------------------------------------------------------------------- |
| §4.2 OPAC search keyword + clc + school + adv | Task 6                                                                  |
| §4.2 detail with cross-school in-stock        | Task 7                                                                  |
| §4.2 CLC 22 categories tree + browse          | Task 8 + Controller                                                     |
| §4.2 borrow ranking                           | Task 11                                                                 |
| §4.2 related recommendation                   | Task 10                                                                 |
| §4.2 new arrivals                             | Task 9                                                                  |
| §4.2 rate limit 10 req/s/IP for search        | Task 12 + 13 (`@RateLimit(value=10, periodSeconds=1)` on search)        |
| §4.2 不可预约（仅检索）                       | implicit — no booking endpoint exposed                                  |
| §4.2 SSR / SEO                                | **deferred to plan-C3**                                                 |
| §4.2 检索词排行 / 自动补全                    | **deferred to plan-C1.5 / C2**                                          |
| §4.2 协同过滤                                 | **deferred** — current `RecommendService` does same-classification only |

### Placeholder scan

No `TBD/TODO/FIXME` in step content. Stub usage in docs is intentional and explained.

### Type / signature consistency

- `BookSearchItemVO` field names: `schoolSchema`, `bookId`, `isbn`, `title`, `author`, `classification`, `totalCount`, `availableCount`, `createdAt` — used consistently across mapper SQL aliases, `BeanPropertyRowMapper`, controller responses.
- `SearchRequest`: `q` / `clc` / `school` / `pageNum` / `pageSize` — matched in mapper params and controller `@RequestParam`.
- `RankingItemVO.borrowCount` is `long` (matches `count(*)::bigint` from PG) — consistent.
- `SchoolAvailabilityVO(schoolSchema, schoolName, totalCount, availableCount)` — `@AllArgsConstructor` order matches usage in `BookDetailServiceImpl`.

All consistent.

### Scope check

15 tasks, ~5-7 steps each, ~85 atomic actions. Single deliverable: a publicly accessible OPAC API that searches / details / browses / ranks / recommends across multiple schools, with IP rate limiting. ✅ Plan-sized.

### Known limitations (intentional)

- No 自动补全 endpoint — saves a 6th endpoint
- No 检索词排行 — needs a search_log table + write path on every query (Plan-C1.5)
- No 协同过滤 — uses content-based same-classification (Plan-C2)
- No SSR / SEO front end — only API (Plan-C3)
- mview is `DROP + CREATE` — Plan-C2 will move to `REFRESH MATERIALIZED VIEW CONCURRENTLY` with a unique index

---

## Execution Handoff

Plan complete and saved to `docs/plans/2026-05-01-plan-C1-opac-mvp.md`. Two execution options:

**1. Subagent-Driven (recommended)** — fresh subagent per task with two-stage review. Same session, fast iteration, ~5 days wall time with parallelism.

**2. Inline Execution** — execute tasks in this session using executing-plans, batch with checkpoints.

Which approach?
