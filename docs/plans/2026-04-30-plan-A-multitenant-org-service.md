# Plan-A: 多租户基础 + org-service 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立多租户隔离的最底层基础设施 + org-service 微服务，使后续 IAM 升级、OPAC、所有业务服务都能基于"教育局 → 学校 → 班级"的组织树和 per-school PG schema 工作。完成后可以演示"建区域 → 建学校（自动建 schema）→ 建年级班级"的完整流程。

**Architecture:** PostgreSQL 单实例 + `gcrf_region` 公共 schema + 每校独立 `school_NNN` schema。MyBatis 拦截器在每个 SQL 之前自动 `SET search_path` 切换 tenant。新增 `org-service` (8090) 提供组织树 CRUD + 学校 provisioning（建节点 + 建 schema + 跑 per-school migration）+ Excel 批量导入。前端在 web-admin 加 `/system/organizations` 组织管理页（el-tree）。

**Tech Stack:** Java 21 / Spring Boot 3.2.2 / Spring Cloud 2023.0.0 / Spring Cloud Alibaba 2023.0.1.0 / MyBatis-Plus 3.5.9 / PostgreSQL 15 (含 ltree 扩展) / Flyway 9.x / Vue 3 + Element Plus / EasyExcel 3.x。沿用现有 `common-mybatis` / `common-security` / `common-web` 模块。

**Spec 来源：** [docs/specs/2026-04-30-regional-platform-master-design.md](../specs/2026-04-30-regional-platform-master-design.md) §2 + §3 + §4.1，[ADR-001](../adr/ADR-001-multi-tenant-strategy.md)，[architecture/01-multi-tenant-isolation.md](../architecture/01-multi-tenant-isolation.md)，[prd/01-regional-cloud-platform.md](../prd/01-regional-cloud-platform.md) §2.1。

**总周期估计：** 2 周（一个全栈工程师全职）。

---

## File Structure

### 后端 — `common-mybatis` 扩展（多租户基础）

| 文件                                                                                                                                | 责任                                                                         |
| ----------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| `backend/common/common-mybatis/src/main/java/com/gcrf/library/common/tenant/TenantContext.java`                                     | ThreadLocal 持有当前 tenant schema                                           |
| `backend/common/common-mybatis/src/main/java/com/gcrf/library/common/tenant/TenantContextFilter.java`                               | Servlet Filter — 从 JWT 提取 tenant claim 写入 TenantContext                 |
| `backend/common/common-mybatis/src/main/java/com/gcrf/library/common/tenant/SearchPathInterceptor.java`                             | MyBatis 拦截器 — 每条 SQL 前自动 `SET search_path TO ${tenant}, gcrf_region` |
| `backend/common/common-mybatis/src/main/java/com/gcrf/library/common/tenant/TenantAutoConfiguration.java`                           | Spring Boot AutoConfiguration — 自动注册 Filter + Interceptor                |
| `backend/common/common-mybatis/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | 注册 AutoConfiguration                                                       |
| `backend/common/common-mybatis/src/main/java/com/gcrf/library/common/tenant/PerSchoolFlywayService.java`                            | 给指定 schema 跑 `db/migration/per-school/V*.sql` 模板                       |
| `backend/common/common-mybatis/src/test/java/com/gcrf/library/common/tenant/TenantContextTest.java`                                 | 单测                                                                         |
| `backend/common/common-mybatis/src/test/java/com/gcrf/library/common/tenant/SearchPathInterceptorTest.java`                         | 单测                                                                         |

### 后端 — `org-service`（新模块）

| 文件                                                                                                     | 责任                                               |
| -------------------------------------------------------------------------------------------------------- | -------------------------------------------------- |
| `backend/org-service/pom.xml`                                                                            | Maven 模块定义                                     |
| `backend/org-service/src/main/java/com/gcrf/library/org/OrgServiceApplication.java`                      | Spring Boot 入口                                   |
| `backend/org-service/src/main/java/com/gcrf/library/org/domain/entity/OrgNode.java`                      | 组织节点实体                                       |
| `backend/org-service/src/main/java/com/gcrf/library/org/domain/dto/OrgNodeCreateDTO.java`                | 创建请求 DTO                                       |
| `backend/org-service/src/main/java/com/gcrf/library/org/domain/dto/OrgNodeUpdateDTO.java`                | 更新请求 DTO                                       |
| `backend/org-service/src/main/java/com/gcrf/library/org/domain/dto/OrgNodeMoveDTO.java`                  | 移动请求 DTO                                       |
| `backend/org-service/src/main/java/com/gcrf/library/org/domain/dto/SchoolCreateDTO.java`                 | 建学校请求 DTO                                     |
| `backend/org-service/src/main/java/com/gcrf/library/org/domain/vo/OrgNodeVO.java`                        | 节点响应 VO                                        |
| `backend/org-service/src/main/java/com/gcrf/library/org/domain/vo/OrgTreeNodeVO.java`                    | 树形响应 VO                                        |
| `backend/org-service/src/main/java/com/gcrf/library/org/mapper/OrgNodeMapper.java`                       | MyBatis Mapper                                     |
| `backend/org-service/src/main/java/com/gcrf/library/org/service/OrgNodeService.java`                     | 服务接口                                           |
| `backend/org-service/src/main/java/com/gcrf/library/org/service/impl/OrgNodeServiceImpl.java`            | 服务实现                                           |
| `backend/org-service/src/main/java/com/gcrf/library/org/service/SchoolProvisioningService.java`          | 服务接口（建学校）                                 |
| `backend/org-service/src/main/java/com/gcrf/library/org/service/impl/SchoolProvisioningServiceImpl.java` | 实现：建节点 + 建 schema + 跑 per-school migration |
| `backend/org-service/src/main/java/com/gcrf/library/org/service/OrgImportService.java`                   | 服务接口（Excel 导入）                             |
| `backend/org-service/src/main/java/com/gcrf/library/org/service/impl/OrgImportServiceImpl.java`          | EasyExcel 解析 → 批量建节点                        |
| `backend/org-service/src/main/java/com/gcrf/library/org/controller/OrgNodeController.java`               | REST: GET/POST/PUT/DELETE/POST move                |
| `backend/org-service/src/main/java/com/gcrf/library/org/controller/SchoolProvisioningController.java`    | REST: POST /org/schools                            |
| `backend/org-service/src/main/java/com/gcrf/library/org/controller/OrgImportController.java`             | REST: POST /org/nodes/import                       |
| `backend/org-service/src/main/resources/application.yml`                                                 | 主配置                                             |
| `backend/org-service/src/main/resources/application-k8s.yml`                                             | K8s profile                                        |
| `backend/org-service/src/main/resources/db/migration/region/V001__create_org_node.sql`                   | gcrf_region.org_node + extension ltree             |
| `backend/org-service/src/main/resources/db/migration/region/V002__create_org_node_type_dict.sql`         | 节点类型字典种子                                   |
| `backend/org-service/src/main/resources/db/migration/per-school/V001__init_school_schema.sql`            | 学校 schema 初始化模板（含核心表占位）             |
| `backend/org-service/src/test/java/com/gcrf/library/org/service/OrgNodeServiceTest.java`                 | 服务单测                                           |
| `backend/org-service/src/test/java/com/gcrf/library/org/controller/OrgNodeControllerTest.java`           | Controller 集成测试                                |

### 前端 — `web-admin`

| 文件                                           | 责任         |
| ---------------------------------------------- | ------------ |
| `web-admin/src/api/org.js`                     | API 封装     |
| `web-admin/src/views/system/organizations.vue` | 组织树管理页 |
| `web-admin/src/router/index.js`                | 注册新路由   |

### 部署

| 文件                                      | 责任                                    |
| ----------------------------------------- | --------------------------------------- |
| `deployment/k8s/10-services.yaml`         | 加 `gcrf-org` Deployment + Service      |
| `deployment/k8s/02-configmap.yaml`        | service-discovery 加 `org-service` 条目 |
| `/tmp/gcrf-deploy/Dockerfile-org-service` | 镜像构建                                |

---

## Tasks

### Task 1: TenantContext + 单测

**Files:**

- Create: `backend/common/common-mybatis/src/main/java/com/gcrf/library/common/tenant/TenantContext.java`
- Create: `backend/common/common-mybatis/src/test/java/com/gcrf/library/common/tenant/TenantContextTest.java`

- [ ] **Step 1: Write the failing test**

```java
// backend/common/common-mybatis/src/test/java/com/gcrf/library/common/tenant/TenantContextTest.java
package com.gcrf.library.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTest {

    @AfterEach
    void clear() { TenantContext.clear(); }

    @Test
    void resolveSearchPath_returnsRegionOnly_whenNoTenant() {
        assertThat(TenantContext.resolveSearchPath()).isEqualTo("gcrf_region");
    }

    @Test
    void resolveSearchPath_prependsTenant_whenSet() {
        TenantContext.setTenant("school_001");
        assertThat(TenantContext.resolveSearchPath()).isEqualTo("school_001, gcrf_region");
    }

    @Test
    void clear_removesTenant() {
        TenantContext.setTenant("school_001");
        TenantContext.clear();
        assertThat(TenantContext.getTenant()).isNull();
    }

    @Test
    void threadLocal_isolatesAcrossThreads() throws Exception {
        TenantContext.setTenant("school_001");
        Thread other = new Thread(() -> {
            assertThat(TenantContext.getTenant()).isNull();
        });
        other.start();
        other.join();
        assertThat(TenantContext.getTenant()).isEqualTo("school_001");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl common/common-mybatis -Dtest=TenantContextTest
```

Expected: COMPILATION FAILURE — `TenantContext` not found.

- [ ] **Step 3: Write minimal implementation**

```java
// backend/common/common-mybatis/src/main/java/com/gcrf/library/common/tenant/TenantContext.java
package com.gcrf.library.common.tenant;

/**
 * 当前请求的多租户 schema 上下文（ThreadLocal）。
 * 由 TenantContextFilter 在请求进入时写入，由 SearchPathInterceptor 读取。
 */
public final class TenantContext {

    public static final String REGION_SCHEMA = "gcrf_region";

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenant(String tenantSchema) {
        CURRENT.set(tenantSchema);
    }

    public static String getTenant() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    /**
     * 返回 PostgreSQL search_path 字符串。
     * 有 tenant：`<tenant>, gcrf_region`；无 tenant：`gcrf_region`。
     */
    public static String resolveSearchPath() {
        String t = CURRENT.get();
        return t == null || t.isBlank() ? REGION_SCHEMA : t + ", " + REGION_SCHEMA;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl common/common-mybatis -Dtest=TenantContextTest
```

Expected: `Tests run: 4, Failures: 0`.

- [ ] **Step 5: Commit**

```bash
git add backend/common/common-mybatis/src/main/java/com/gcrf/library/common/tenant/TenantContext.java \
        backend/common/common-mybatis/src/test/java/com/gcrf/library/common/tenant/TenantContextTest.java
git commit -m "feat(common): add TenantContext for per-request tenant schema"
```

---

### Task 2: SearchPathInterceptor + 单测

**Files:**

- Create: `backend/common/common-mybatis/src/main/java/com/gcrf/library/common/tenant/SearchPathInterceptor.java`
- Create: `backend/common/common-mybatis/src/test/java/com/gcrf/library/common/tenant/SearchPathInterceptorTest.java`

- [ ] **Step 1: Write the failing test**

```java
// backend/common/common-mybatis/src/test/java/com/gcrf/library/common/tenant/SearchPathInterceptorTest.java
package com.gcrf.library.common.tenant;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Invocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SearchPathInterceptorTest {

    @AfterEach
    void clear() { TenantContext.clear(); }

    @Test
    void intercept_setsSearchPathToRegion_whenNoTenant() throws Throwable {
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(conn.createStatement()).thenReturn(stmt);

        Method prepare = StatementHandler.class.getMethod("prepare", Connection.class, Integer.class);
        Invocation inv = new Invocation(mock(StatementHandler.class), prepare, new Object[]{conn, null});

        new SearchPathInterceptor().intercept(inv);

        verify(stmt).execute("SET search_path TO gcrf_region");
    }

    @Test
    void intercept_setsSearchPathToTenantThenRegion_whenTenantSet() throws Throwable {
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(conn.createStatement()).thenReturn(stmt);
        TenantContext.setTenant("school_001");

        Method prepare = StatementHandler.class.getMethod("prepare", Connection.class, Integer.class);
        Invocation inv = new Invocation(mock(StatementHandler.class), prepare, new Object[]{conn, null});

        new SearchPathInterceptor().intercept(inv);

        verify(stmt).execute("SET search_path TO school_001, gcrf_region");
    }

    @Test
    void intercept_rejectsUnsafeSchemaName() throws Throwable {
        TenantContext.setTenant("school_001; DROP TABLE foo --");
        SearchPathInterceptor sut = new SearchPathInterceptor();

        assertThat(sut.isSafeSearchPath("school_001; DROP TABLE foo --")).isFalse();
        assertThat(sut.isSafeSearchPath("school_001, gcrf_region")).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl common/common-mybatis -Dtest=SearchPathInterceptorTest
```

Expected: COMPILATION FAILURE — `SearchPathInterceptor` not found.

- [ ] **Step 3: Write minimal implementation**

```java
// backend/common/common-mybatis/src/main/java/com/gcrf/library/common/tenant/SearchPathInterceptor.java
package com.gcrf.library.common.tenant;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

import java.sql.Connection;
import java.sql.Statement;
import java.util.regex.Pattern;

/**
 * MyBatis 拦截器：在每个 SQL 执行前 SET search_path 切换 PostgreSQL schema。
 * 使 mapper 写裸 SQL（不带 schema 前缀）即可在多租户下工作。
 */
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare",
               args = {Connection.class, Integer.class})
})
public class SearchPathInterceptor implements Interceptor {

    /** 仅允许 字母/数字/下划线/逗号/空格 ，防止 SQL 注入到 search_path 字符串。 */
    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9_, ]+$");

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Connection conn = (Connection) invocation.getArgs()[0];
        String path = TenantContext.resolveSearchPath();
        if (!isSafeSearchPath(path)) {
            throw new IllegalStateException("unsafe search_path: " + path);
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET search_path TO " + path);
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    /** 公开供单测验证白名单逻辑。 */
    public boolean isSafeSearchPath(String path) {
        return path != null && SAFE.matcher(path).matches();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl common/common-mybatis -Dtest=SearchPathInterceptorTest
```

Expected: `Tests run: 3, Failures: 0`.

- [ ] **Step 5: Commit**

```bash
git add backend/common/common-mybatis/src/main/java/com/gcrf/library/common/tenant/SearchPathInterceptor.java \
        backend/common/common-mybatis/src/test/java/com/gcrf/library/common/tenant/SearchPathInterceptorTest.java
git commit -m "feat(common): add SearchPathInterceptor for per-request schema switch"
```

---

### Task 3: TenantContextFilter（Servlet Filter）+ 单测

**Files:**

- Create: `backend/common/common-mybatis/src/main/java/com/gcrf/library/common/tenant/TenantContextFilter.java`
- Create: `backend/common/common-mybatis/src/test/java/com/gcrf/library/common/tenant/TenantContextFilterTest.java`

- [ ] **Step 1: Write the failing test**

```java
// backend/common/common-mybatis/src/test/java/com/gcrf/library/common/tenant/TenantContextFilterTest.java
package com.gcrf.library.common.tenant;

import com.gcrf.library.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TenantContextFilterTest {

    @AfterEach
    void clear() { TenantContext.clear(); }

    @Test
    void noAuthHeader_doesNotSetTenant_andClearsAfter() throws Exception {
        JwtUtil jwt = mock(JwtUtil.class);
        TenantContextFilter f = new TenantContextFilter(jwt);
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        f.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        assertThat(TenantContext.getTenant()).isNull();
    }

    @Test
    void validToken_setsTenantFromClaim() throws Exception {
        JwtUtil jwt = mock(JwtUtil.class);
        Claims claims = mock(Claims.class);
        when(jwt.parseToken("abc")).thenReturn(claims);
        when(claims.get("tenant", String.class)).thenReturn("school_001");

        TenantContextFilter f = new TenantContextFilter(jwt) {
            @Override
            protected void onChainBeforeClear(HttpServletRequest req, HttpServletResponse res) {
                assertThat(TenantContext.getTenant()).isEqualTo("school_001");
            }
        };
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer abc");
        MockHttpServletResponse res = new MockHttpServletResponse();

        f.doFilter(req, res, mock(FilterChain.class));

        assertThat(TenantContext.getTenant()).isNull(); // cleared after filter
    }

    @Test
    void invalidToken_ignoredAndContinues() throws Exception {
        JwtUtil jwt = mock(JwtUtil.class);
        when(jwt.parseToken(any())).thenThrow(new RuntimeException("bad token"));
        TenantContextFilter f = new TenantContextFilter(jwt);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer bad");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        f.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        assertThat(TenantContext.getTenant()).isNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl common/common-mybatis -Dtest=TenantContextFilterTest
```

Expected: COMPILATION FAILURE — `TenantContextFilter` not found.

- [ ] **Step 3: Write minimal implementation**

```java
// backend/common/common-mybatis/src/main/java/com/gcrf/library/common/tenant/TenantContextFilter.java
package com.gcrf.library.common.tenant;

import com.gcrf.library.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet Filter — 从 Authorization: Bearer <jwt> 提取 tenant claim 并写入
 * TenantContext，请求结束后清理 ThreadLocal。
 */
@Slf4j
@RequiredArgsConstructor
public class TenantContextFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String auth = req.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                try {
                    Claims claims = jwtUtil.parseToken(auth.substring(7));
                    String tenant = claims.get("tenant", String.class);
                    if (tenant != null && !tenant.isBlank()) {
                        TenantContext.setTenant(tenant);
                    }
                } catch (Exception e) {
                    log.debug("tenant filter: invalid token, ignoring: {}", e.getMessage());
                }
            }
            onChainBeforeClear(req, res);
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
        }
    }

    /** 测试钩子。生产代码空实现。 */
    protected void onChainBeforeClear(HttpServletRequest req, HttpServletResponse res) {
        // intentionally empty
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl common/common-mybatis -Dtest=TenantContextFilterTest
```

Expected: `Tests run: 3, Failures: 0`.

- [ ] **Step 5: Commit**

```bash
git add backend/common/common-mybatis/src/main/java/com/gcrf/library/common/tenant/TenantContextFilter.java \
        backend/common/common-mybatis/src/test/java/com/gcrf/library/common/tenant/TenantContextFilterTest.java
git commit -m "feat(common): add TenantContextFilter to extract tenant from JWT"
```

---

### Task 4: TenantAutoConfiguration + AutoConfiguration imports

**Files:**

- Create: `backend/common/common-mybatis/src/main/java/com/gcrf/library/common/tenant/TenantAutoConfiguration.java`
- Create: `backend/common/common-mybatis/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: `backend/common/common-mybatis/pom.xml` (add `common-security` dependency for `JwtUtil`)

- [ ] **Step 1: Add common-security dependency to common-mybatis pom**

Open `backend/common/common-mybatis/pom.xml` and add inside `<dependencies>` (after the `common-core` dependency):

```xml
<!-- Common Security for JwtUtil -->
<dependency>
    <groupId>com.gcrf.library</groupId>
    <artifactId>common-security</artifactId>
</dependency>
<!-- Spring Web for OncePerRequestFilter -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
</dependency>
```

- [ ] **Step 2: Create AutoConfiguration**

```java
// backend/common/common-mybatis/src/main/java/com/gcrf/library/common/tenant/TenantAutoConfiguration.java
package com.gcrf.library.common.tenant;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.gcrf.library.common.utils.JwtUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * 多租户基础设施自动装配。
 * 业务服务通过 {@code gcrf.tenant.enabled=true}（默认 true）启用。
 */
@AutoConfiguration(after = MybatisPlusAutoConfiguration.class)
@ConditionalOnClass(JwtUtil.class)
@ConditionalOnProperty(prefix = "gcrf.tenant", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TenantAutoConfiguration {

    @Bean
    public SearchPathInterceptor searchPathInterceptor() {
        return new SearchPathInterceptor();
    }

    @Bean
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilter(JwtUtil jwtUtil) {
        FilterRegistrationBean<TenantContextFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new TenantContextFilter(jwtUtil));
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 100); // after security filter
        reg.addUrlPatterns("/*");
        return reg;
    }
}
```

- [ ] **Step 3: Register AutoConfiguration**

```
# backend/common/common-mybatis/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.gcrf.library.common.tenant.TenantAutoConfiguration
```

- [ ] **Step 4: Run common-mybatis full build**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean install -pl common/common-mybatis -am -DskipITs
```

Expected: `BUILD SUCCESS` (existing tests + new tenant tests all pass).

- [ ] **Step 5: Commit**

```bash
git add backend/common/common-mybatis/pom.xml \
        backend/common/common-mybatis/src/main/java/com/gcrf/library/common/tenant/TenantAutoConfiguration.java \
        backend/common/common-mybatis/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
git commit -m "feat(common): auto-register tenant filter and search-path interceptor"
```

---

### Task 5: PerSchoolFlywayService（给指定 schema 跑 per-school migration）

**Files:**

- Create: `backend/common/common-mybatis/src/main/java/com/gcrf/library/common/tenant/PerSchoolFlywayService.java`
- Create: `backend/common/common-mybatis/src/test/java/com/gcrf/library/common/tenant/PerSchoolFlywayServiceTest.java`
- Modify: `backend/common/common-mybatis/pom.xml` (add Flyway dependency)

- [ ] **Step 1: Add Flyway dependency**

In `backend/common/common-mybatis/pom.xml` `<dependencies>`:

```xml
<!-- Flyway for per-school migrations -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

- [ ] **Step 2: Write the failing test (testcontainers PG)**

```java
// backend/common/common-mybatis/src/test/java/com/gcrf/library/common/tenant/PerSchoolFlywayServiceTest.java
package com.gcrf.library.common.tenant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class PerSchoolFlywayServiceTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void migrateSchema_createsAllTablesInTargetSchema() throws Exception {
        DataSource ds = DataSourceBuilder.create()
            .url(PG.getJdbcUrl())
            .username(PG.getUsername())
            .password(PG.getPassword())
            .driverClassName("org.postgresql.Driver")
            .build();

        // create empty schema
        try (Connection c = ds.getConnection()) {
            c.createStatement().execute("CREATE SCHEMA school_test");
        }

        new PerSchoolFlywayService(ds).migrateSchool("school_test", "classpath:db/migration/per-school-test");

        try (Connection c = ds.getConnection()) {
            c.createStatement().execute("SET search_path TO school_test");
            ResultSet rs = c.createStatement().executeQuery(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema='school_test' AND table_name='probe'");
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }
}
```

Also create test fixture:

```sql
-- backend/common/common-mybatis/src/test/resources/db/migration/per-school-test/V001__probe.sql
CREATE TABLE IF NOT EXISTS probe (id BIGSERIAL PRIMARY KEY, note TEXT);
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl common/common-mybatis -Dtest=PerSchoolFlywayServiceTest
```

Expected: COMPILATION FAILURE — `PerSchoolFlywayService` not found.

- [ ] **Step 4: Write minimal implementation**

```java
// backend/common/common-mybatis/src/main/java/com/gcrf/library/common/tenant/PerSchoolFlywayService.java
package com.gcrf.library.common.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

import javax.sql.DataSource;

/**
 * 给指定 PostgreSQL schema 跑 per-school 模板 migration。
 * 由 SchoolProvisioningService 在新建学校时调用。
 */
@Slf4j
@RequiredArgsConstructor
public class PerSchoolFlywayService {

    private static final String DEFAULT_LOCATION = "classpath:db/migration/per-school";

    private final DataSource dataSource;

    public MigrateResult migrateSchool(String schemaName) {
        return migrateSchool(schemaName, DEFAULT_LOCATION);
    }

    public MigrateResult migrateSchool(String schemaName, String migrationLocation) {
        if (schemaName == null || !schemaName.matches("^[a-z][a-z0-9_]+$")) {
            throw new IllegalArgumentException("invalid schema name: " + schemaName);
        }
        log.info("running per-school flyway for schema={}, location={}", schemaName, migrationLocation);

        Flyway fw = Flyway.configure()
            .dataSource(dataSource)
            .schemas(schemaName)
            .defaultSchema(schemaName)
            .locations(migrationLocation)
            .placeholders(java.util.Map.of("schema", schemaName))
            .baselineOnMigrate(true)
            .table("flyway_schema_history") // table inside the school schema
            .load();

        MigrateResult result = fw.migrate();
        log.info("per-school migration done: schema={}, migrationsExecuted={}",
                 schemaName, result.migrationsExecuted);
        return result;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl common/common-mybatis -Dtest=PerSchoolFlywayServiceTest
```

Expected: `Tests run: 1, Failures: 0` (testcontainers spins up PG, creates schema, runs migration, asserts table exists).

- [ ] **Step 6: Commit**

```bash
git add backend/common/common-mybatis/pom.xml \
        backend/common/common-mybatis/src/main/java/com/gcrf/library/common/tenant/PerSchoolFlywayService.java \
        backend/common/common-mybatis/src/test/java/com/gcrf/library/common/tenant/PerSchoolFlywayServiceTest.java \
        backend/common/common-mybatis/src/test/resources/db/migration/per-school-test/V001__probe.sql
git commit -m "feat(common): add PerSchoolFlywayService for per-tenant schema migration"
```

---

### Task 6: org-service Maven 模块骨架

**Files:**

- Create: `backend/org-service/pom.xml`
- Create: `backend/org-service/src/main/java/com/gcrf/library/org/OrgServiceApplication.java`
- Create: `backend/org-service/src/main/resources/application.yml`
- Create: `backend/org-service/src/main/resources/application-k8s.yml`
- Modify: `backend/pom.xml` (add module)

- [ ] **Step 1: Add module to parent pom**

In `backend/pom.xml`, find `<modules>` block and add:

```xml
<module>org-service</module>
```

(insert after `<module>analytics-service</module>`)

- [ ] **Step 2: Create org-service pom**

```xml
<!-- backend/org-service/pom.xml -->
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

    <artifactId>org-service</artifactId>
    <packaging>jar</packaging>
    <name>org-service</name>
    <description>组织树 + 多租户 schema 路由 + 学校 provisioning</description>

    <dependencies>
        <dependency><groupId>com.gcrf.library</groupId><artifactId>common-core</artifactId></dependency>
        <dependency><groupId>com.gcrf.library</groupId><artifactId>common-web</artifactId></dependency>
        <dependency><groupId>com.gcrf.library</groupId><artifactId>common-security</artifactId></dependency>
        <dependency><groupId>com.gcrf.library</groupId><artifactId>common-mybatis</artifactId></dependency>

        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>

        <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
        <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-database-postgresql</artifactId></dependency>

        <!-- Excel 导入 -->
        <dependency><groupId>com.alibaba</groupId><artifactId>easyexcel</artifactId><version>3.3.4</version></dependency>

        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
        <dependency><groupId>org.testcontainers</groupId><artifactId>postgresql</artifactId><scope>test</scope></dependency>
        <dependency><groupId>org.testcontainers</groupId><artifactId>junit-jupiter</artifactId><scope>test</scope></dependency>
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

- [ ] **Step 3: Create application entry point**

```java
// backend/org-service/src/main/java/com/gcrf/library/org/OrgServiceApplication.java
package com.gcrf.library.org;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.gcrf.library")
@MapperScan("com.gcrf.library.org.mapper")
public class OrgServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrgServiceApplication.class, args);
    }
}
```

- [ ] **Step 4: Create application.yml**

```yaml
# backend/org-service/src/main/resources/application.yml
server:
  port: 8090

spring:
  application:
    name: org-service
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/gcrf_main
    username: postgres
    password: gcrf_secure_2024
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
    enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

- [ ] **Step 5: Create K8s profile config**

```yaml
# backend/org-service/src/main/resources/application-k8s.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:postgresql.edu-infra.svc.cluster.local}:5432/${DB_NAME:gcrf_main}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD}
  data:
    redis:
      host: ${REDIS_HOST:redis.edu-infra.svc.cluster.local}
      port: ${REDIS_PORT:6379}
      database: ${REDIS_DB:1}
      password: ${REDIS_PASSWORD}

logging:
  level:
    com.gcrf.library: INFO
    org.flywaydb: INFO
```

- [ ] **Step 6: Verify build**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl org-service -am
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git add backend/pom.xml backend/org-service/pom.xml \
        backend/org-service/src/main/java/com/gcrf/library/org/OrgServiceApplication.java \
        backend/org-service/src/main/resources/application.yml \
        backend/org-service/src/main/resources/application-k8s.yml
git commit -m "feat(org): scaffold org-service module with tenant + flyway"
```

---

### Task 7: Region schema migration（org_node 表 + ltree extension）

**Files:**

- Create: `backend/org-service/src/main/resources/db/migration/region/V001__create_org_node.sql`
- Create: `backend/org-service/src/main/resources/db/migration/region/V002__seed_org_node_type_dict.sql`

- [ ] **Step 1: Create V001 — org_node table**

```sql
-- backend/org-service/src/main/resources/db/migration/region/V001__create_org_node.sql
CREATE EXTENSION IF NOT EXISTS ltree;

CREATE TABLE IF NOT EXISTS org_node (
    id              BIGSERIAL    PRIMARY KEY,
    parent_id       BIGINT       REFERENCES org_node(id) ON DELETE RESTRICT,
    type            VARCHAR(30)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(50)  UNIQUE NOT NULL,
    path            LTREE        NOT NULL,
    tenant_schema   VARCHAR(50),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    metadata        JSONB        NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_org_node_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE INDEX IF NOT EXISTS idx_org_node_path_gist ON org_node USING GIST (path);
CREATE INDEX IF NOT EXISTS idx_org_node_parent ON org_node(parent_id);
CREATE INDEX IF NOT EXISTS idx_org_node_type ON org_node(type);
CREATE INDEX IF NOT EXISTS idx_org_node_tenant ON org_node(tenant_schema) WHERE tenant_schema IS NOT NULL;

CREATE TABLE IF NOT EXISTS org_node_type (
    code           VARCHAR(30)  PRIMARY KEY,
    name           VARCHAR(50)  NOT NULL,
    parent_types   TEXT[]       NOT NULL DEFAULT '{}',
    display_order  INT          NOT NULL DEFAULT 0
);
```

- [ ] **Step 2: Create V002 — seed default node types**

```sql
-- backend/org-service/src/main/resources/db/migration/region/V002__seed_org_node_type_dict.sql
INSERT INTO org_node_type(code, name, parent_types, display_order) VALUES
    ('REGION',     '教育局/区域', '{}',                                 10),
    ('DISTRICT',   '区/县',       '{REGION}',                           20),
    ('SCHOOL',     '学校',        '{REGION,DISTRICT}',                  30),
    ('SUB_SCHOOL', '分校',        '{SCHOOL}',                           40),
    ('BRANCH',     '分馆',        '{SCHOOL,SUB_SCHOOL}',                50),
    ('STAGE',      '学段',        '{SCHOOL,SUB_SCHOOL}',                60),
    ('GRADE',      '年级',        '{SCHOOL,SUB_SCHOOL,STAGE}',          70),
    ('CLASS',      '班级',        '{GRADE}',                            80)
ON CONFLICT (code) DO NOTHING;
```

- [ ] **Step 3: Verify Flyway runs at startup (manually)**

Start the service against a local PG:

```bash
createdb gcrf_main 2>/dev/null || true
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn spring-boot:run -pl org-service
```

In another terminal verify:

```bash
psql -h localhost -U postgres -d gcrf_main -c "\dt gcrf_region.*"
```

Expected output:

```
                List of relations
   Schema    |     Name      | Type  |   Owner
-------------+---------------+-------+-----------
 gcrf_region | org_node      | table | postgres
 gcrf_region | org_node_type | table | postgres
```

Stop the service (`Ctrl-C`).

- [ ] **Step 4: Commit**

```bash
git add backend/org-service/src/main/resources/db/migration/region/
git commit -m "feat(org): add region schema migration for org_node + node type dict"
```

---

### Task 8: OrgNode entity + DTOs + VOs + Mapper

**Files:**

- Create: `backend/org-service/src/main/java/com/gcrf/library/org/domain/entity/OrgNode.java`
- Create: `backend/org-service/src/main/java/com/gcrf/library/org/domain/dto/OrgNodeCreateDTO.java`
- Create: `backend/org-service/src/main/java/com/gcrf/library/org/domain/dto/OrgNodeUpdateDTO.java`
- Create: `backend/org-service/src/main/java/com/gcrf/library/org/domain/dto/OrgNodeMoveDTO.java`
- Create: `backend/org-service/src/main/java/com/gcrf/library/org/domain/dto/SchoolCreateDTO.java`
- Create: `backend/org-service/src/main/java/com/gcrf/library/org/domain/vo/OrgNodeVO.java`
- Create: `backend/org-service/src/main/java/com/gcrf/library/org/domain/vo/OrgTreeNodeVO.java`
- Create: `backend/org-service/src/main/java/com/gcrf/library/org/mapper/OrgNodeMapper.java`

- [ ] **Step 1: Create OrgNode entity**

```java
// backend/org-service/src/main/java/com/gcrf/library/org/domain/entity/OrgNode.java
package com.gcrf.library.org.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "org_node", autoResultMap = true)
public class OrgNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;
    private String type;
    private String name;
    private String code;
    /** ltree 字段在 Java 端用 String 表示，PG 自动转换。 */
    private String path;
    private String tenantSchema;
    private String status;
    @TableField(typeHandler = com.gcrf.library.common.handler.JsonTypeHandler.class)
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: Create DTOs**

```java
// backend/org-service/src/main/java/com/gcrf/library/org/domain/dto/OrgNodeCreateDTO.java
package com.gcrf.library.org.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OrgNodeCreateDTO {
    private Long parentId;

    @NotBlank
    @Pattern(regexp = "^(REGION|DISTRICT|SCHOOL|SUB_SCHOOL|BRANCH|STAGE|GRADE|CLASS)$")
    private String type;

    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_\\-]+$", message = "code must be alphanumeric / dash / underscore")
    private String code;

    private String metadata;
}
```

```java
// backend/org-service/src/main/java/com/gcrf/library/org/domain/dto/OrgNodeUpdateDTO.java
package com.gcrf.library.org.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrgNodeUpdateDTO {
    @NotBlank
    private String name;
    private String status; // ACTIVE / INACTIVE
    private String metadata;
}
```

```java
// backend/org-service/src/main/java/com/gcrf/library/org/domain/dto/OrgNodeMoveDTO.java
package com.gcrf.library.org.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrgNodeMoveDTO {
    @NotNull
    private Long newParentId;
}
```

```java
// backend/org-service/src/main/java/com/gcrf/library/org/domain/dto/SchoolCreateDTO.java
package com.gcrf.library.org.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SchoolCreateDTO {
    @NotNull
    private Long parentId;          // REGION or DISTRICT id

    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_\\-]+$")
    private String code;
}
```

- [ ] **Step 3: Create VOs**

```java
// backend/org-service/src/main/java/com/gcrf/library/org/domain/vo/OrgNodeVO.java
package com.gcrf.library.org.domain.vo;

import com.gcrf.library.org.domain.entity.OrgNode;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrgNodeVO {
    private Long id;
    private Long parentId;
    private String type;
    private String name;
    private String code;
    private String path;
    private String tenantSchema;
    private String status;
    private String metadata;
    private LocalDateTime createdAt;

    public static OrgNodeVO from(OrgNode e) {
        OrgNodeVO v = new OrgNodeVO();
        v.id = e.getId();
        v.parentId = e.getParentId();
        v.type = e.getType();
        v.name = e.getName();
        v.code = e.getCode();
        v.path = e.getPath();
        v.tenantSchema = e.getTenantSchema();
        v.status = e.getStatus();
        v.metadata = e.getMetadata();
        v.createdAt = e.getCreatedAt();
        return v;
    }
}
```

```java
// backend/org-service/src/main/java/com/gcrf/library/org/domain/vo/OrgTreeNodeVO.java
package com.gcrf.library.org.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OrgTreeNodeVO {
    private Long id;
    private Long parentId;
    private String type;
    private String name;
    private String code;
    private String path;
    private String tenantSchema;
    private String status;
    private List<OrgTreeNodeVO> children = new ArrayList<>();
}
```

- [ ] **Step 4: Create Mapper**

```java
// backend/org-service/src/main/java/com/gcrf/library/org/mapper/OrgNodeMapper.java
package com.gcrf.library.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.org.domain.entity.OrgNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrgNodeMapper extends BaseMapper<OrgNode> {

    @Select("SELECT * FROM org_node WHERE path <@ #{rootPath}::ltree ORDER BY path")
    List<OrgNode> findSubtree(@Param("rootPath") String rootPath);

    @Select("SELECT * FROM org_node WHERE parent_id = #{parentId} ORDER BY id")
    List<OrgNode> findByParent(@Param("parentId") Long parentId);

    @Select("SELECT * FROM org_node WHERE parent_id IS NULL ORDER BY id")
    List<OrgNode> findRoots();

    /**
     * 移动子树：把所有 path 以 oldPath 开头的节点改为以 newPath 开头。
     */
    @Update("""
        UPDATE org_node
           SET path = (#{newPath}::ltree || subpath(path, nlevel(#{oldPath}::ltree)))
         WHERE path <@ #{oldPath}::ltree
        """)
    int moveSubtree(@Param("oldPath") String oldPath, @Param("newPath") String newPath);
}
```

- [ ] **Step 5: Verify compile**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl org-service -am
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add backend/org-service/src/main/java/com/gcrf/library/org/domain/ \
        backend/org-service/src/main/java/com/gcrf/library/org/mapper/OrgNodeMapper.java
git commit -m "feat(org): add OrgNode entity, DTOs, VOs and Mapper"
```

---

### Task 9: OrgNodeService — 查询（findRoots / findChildren / findById / findTree）

**Files:**

- Create: `backend/org-service/src/main/java/com/gcrf/library/org/service/OrgNodeService.java`
- Create: `backend/org-service/src/main/java/com/gcrf/library/org/service/impl/OrgNodeServiceImpl.java`
- Create: `backend/org-service/src/test/java/com/gcrf/library/org/service/OrgNodeServiceQueryTest.java`

- [ ] **Step 1: Write failing tests**

```java
// backend/org-service/src/test/java/com/gcrf/library/org/service/OrgNodeServiceQueryTest.java
package com.gcrf.library.org.service;

import com.gcrf.library.org.domain.vo.OrgTreeNodeVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
class OrgNodeServiceQueryTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired OrgNodeService svc;

    @Test
    void findRoots_returnsTopLevelNodesOnly() {
        List<OrgTreeNodeVO> roots = svc.findRoots();
        // V002 seed has no node data; this is empty unless test fixtures inserted.
        assertThat(roots).isNotNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl org-service -Dtest=OrgNodeServiceQueryTest
```

Expected: COMPILATION FAILURE — `OrgNodeService` not found.

- [ ] **Step 3: Write service interface**

```java
// backend/org-service/src/main/java/com/gcrf/library/org/service/OrgNodeService.java
package com.gcrf.library.org.service;

import com.gcrf.library.org.domain.dto.OrgNodeCreateDTO;
import com.gcrf.library.org.domain.dto.OrgNodeUpdateDTO;
import com.gcrf.library.org.domain.vo.OrgNodeVO;
import com.gcrf.library.org.domain.vo.OrgTreeNodeVO;

import java.util.List;

public interface OrgNodeService {
    List<OrgTreeNodeVO> findRoots();
    List<OrgTreeNodeVO> findChildren(Long parentId);
    OrgNodeVO findById(Long id);
    OrgTreeNodeVO findSubtree(Long rootId);
    OrgNodeVO create(OrgNodeCreateDTO dto);
    OrgNodeVO update(Long id, OrgNodeUpdateDTO dto);
    void delete(Long id);
    OrgNodeVO move(Long id, Long newParentId);
}
```

- [ ] **Step 4: Write service impl (query methods only — create/update/delete/move come in next tasks)**

```java
// backend/org-service/src/main/java/com/gcrf/library/org/service/impl/OrgNodeServiceImpl.java
package com.gcrf.library.org.service.impl;

import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.org.domain.dto.OrgNodeCreateDTO;
import com.gcrf.library.org.domain.dto.OrgNodeUpdateDTO;
import com.gcrf.library.org.domain.entity.OrgNode;
import com.gcrf.library.org.domain.vo.OrgNodeVO;
import com.gcrf.library.org.domain.vo.OrgTreeNodeVO;
import com.gcrf.library.org.mapper.OrgNodeMapper;
import com.gcrf.library.org.service.OrgNodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgNodeServiceImpl implements OrgNodeService {

    private final OrgNodeMapper mapper;

    @Override
    public List<OrgTreeNodeVO> findRoots() {
        return toTreeFlat(mapper.findRoots());
    }

    @Override
    public List<OrgTreeNodeVO> findChildren(Long parentId) {
        return toTreeFlat(mapper.findByParent(parentId));
    }

    @Override
    public OrgNodeVO findById(Long id) {
        OrgNode e = mapper.selectById(id);
        if (e == null) throw new BusinessException("org node not found: " + id);
        return OrgNodeVO.from(e);
    }

    @Override
    public OrgTreeNodeVO findSubtree(Long rootId) {
        OrgNode root = mapper.selectById(rootId);
        if (root == null) throw new BusinessException("org node not found: " + rootId);
        List<OrgNode> all = mapper.findSubtree(root.getPath());
        return assembleTree(all, root.getId());
    }

    @Override
    public OrgNodeVO create(OrgNodeCreateDTO dto) {
        throw new UnsupportedOperationException("implemented in Task 10");
    }

    @Override
    public OrgNodeVO update(Long id, OrgNodeUpdateDTO dto) {
        throw new UnsupportedOperationException("implemented in Task 11");
    }

    @Override
    public void delete(Long id) {
        throw new UnsupportedOperationException("implemented in Task 12");
    }

    @Override
    public OrgNodeVO move(Long id, Long newParentId) {
        throw new UnsupportedOperationException("implemented in Task 13");
    }

    // ---------- helpers ----------

    private List<OrgTreeNodeVO> toTreeFlat(List<OrgNode> nodes) {
        List<OrgTreeNodeVO> result = new ArrayList<>();
        for (OrgNode n : nodes) {
            OrgTreeNodeVO v = new OrgTreeNodeVO();
            v.setId(n.getId());
            v.setParentId(n.getParentId());
            v.setType(n.getType());
            v.setName(n.getName());
            v.setCode(n.getCode());
            v.setPath(n.getPath());
            v.setTenantSchema(n.getTenantSchema());
            v.setStatus(n.getStatus());
            result.add(v);
        }
        return result;
    }

    private OrgTreeNodeVO assembleTree(List<OrgNode> nodes, Long rootId) {
        Map<Long, OrgTreeNodeVO> byId = new HashMap<>();
        OrgTreeNodeVO rootVo = null;
        for (OrgNode n : nodes) {
            OrgTreeNodeVO v = new OrgTreeNodeVO();
            v.setId(n.getId());
            v.setParentId(n.getParentId());
            v.setType(n.getType());
            v.setName(n.getName());
            v.setCode(n.getCode());
            v.setPath(n.getPath());
            v.setTenantSchema(n.getTenantSchema());
            v.setStatus(n.getStatus());
            byId.put(v.getId(), v);
            if (v.getId().equals(rootId)) rootVo = v;
        }
        for (OrgTreeNodeVO v : byId.values()) {
            if (v.getId().equals(rootId)) continue;
            OrgTreeNodeVO parent = byId.get(v.getParentId());
            if (parent != null) parent.getChildren().add(v);
        }
        return rootVo;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl org-service -Dtest=OrgNodeServiceQueryTest
```

Expected: `Tests run: 1, Failures: 0`.

- [ ] **Step 6: Commit**

```bash
git add backend/org-service/src/main/java/com/gcrf/library/org/service/ \
        backend/org-service/src/test/java/com/gcrf/library/org/service/OrgNodeServiceQueryTest.java
git commit -m "feat(org): add OrgNodeService query methods (roots/children/subtree)"
```

---

### Task 10: OrgNodeService.create — 含 type 校验 + path 计算

**Files:**

- Modify: `backend/org-service/src/main/java/com/gcrf/library/org/service/impl/OrgNodeServiceImpl.java`
- Create: `backend/org-service/src/test/java/com/gcrf/library/org/service/OrgNodeServiceCreateTest.java`

- [ ] **Step 1: Write failing tests**

```java
// backend/org-service/src/test/java/com/gcrf/library/org/service/OrgNodeServiceCreateTest.java
package com.gcrf.library.org.service;

import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.org.domain.dto.OrgNodeCreateDTO;
import com.gcrf.library.org.domain.vo.OrgNodeVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@Transactional
class OrgNodeServiceCreateTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired OrgNodeService svc;

    @Test
    void create_root_REGION_succeeds() {
        OrgNodeCreateDTO dto = new OrgNodeCreateDTO();
        dto.setType("REGION");
        dto.setName("天河区教育局");
        dto.setCode("th_edu");

        OrgNodeVO v = svc.create(dto);

        assertThat(v.getId()).isNotNull();
        assertThat(v.getPath()).isEqualTo(String.valueOf(v.getId()));
    }

    @Test
    void create_child_DISTRICT_under_REGION_succeeds() {
        OrgNodeCreateDTO root = new OrgNodeCreateDTO();
        root.setType("REGION"); root.setName("天河"); root.setCode("th");
        Long rootId = svc.create(root).getId();

        OrgNodeCreateDTO child = new OrgNodeCreateDTO();
        child.setParentId(rootId); child.setType("DISTRICT"); child.setName("石牌"); child.setCode("sp");

        OrgNodeVO v = svc.create(child);

        assertThat(v.getPath()).isEqualTo(rootId + "." + v.getId());
    }

    @Test
    void create_invalidParentType_isRejected() {
        OrgNodeCreateDTO root = new OrgNodeCreateDTO();
        root.setType("REGION"); root.setName("R"); root.setCode("r1");
        Long rootId = svc.create(root).getId();

        OrgNodeCreateDTO bad = new OrgNodeCreateDTO();
        bad.setParentId(rootId); bad.setType("CLASS"); bad.setName("C"); bad.setCode("c1");

        assertThatThrownBy(() -> svc.create(bad))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("CLASS cannot be child of REGION");
    }

    @Test
    void create_duplicateCode_isRejected() {
        OrgNodeCreateDTO a = new OrgNodeCreateDTO();
        a.setType("REGION"); a.setName("A"); a.setCode("dup");
        svc.create(a);

        OrgNodeCreateDTO b = new OrgNodeCreateDTO();
        b.setType("REGION"); b.setName("B"); b.setCode("dup");

        assertThatThrownBy(() -> svc.create(b))
            .hasMessageContaining("dup");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl org-service -Dtest=OrgNodeServiceCreateTest
```

Expected: `UnsupportedOperationException` from current stub.

- [ ] **Step 3: Implement create() and replace stub**

In `OrgNodeServiceImpl.java`, add field + replace `create()`:

```java
// at class top — add Mapper for type dict
private final com.gcrf.library.org.mapper.OrgNodeMapper mapper;
private final com.baomidou.mybatisplus.core.toolkit.Wrappers wrappersUnused = null; // placeholder, remove if linter complains

// Add a small helper to load type-parent rules
private static final java.util.Map<String, java.util.Set<String>> ALLOWED_PARENTS = java.util.Map.of(
    "REGION",     java.util.Set.of(),
    "DISTRICT",   java.util.Set.of("REGION"),
    "SCHOOL",     java.util.Set.of("REGION", "DISTRICT"),
    "SUB_SCHOOL", java.util.Set.of("SCHOOL"),
    "BRANCH",     java.util.Set.of("SCHOOL", "SUB_SCHOOL"),
    "STAGE",      java.util.Set.of("SCHOOL", "SUB_SCHOOL"),
    "GRADE",      java.util.Set.of("SCHOOL", "SUB_SCHOOL", "STAGE"),
    "CLASS",      java.util.Set.of("GRADE")
);

@Override
@org.springframework.transaction.annotation.Transactional
public OrgNodeVO create(OrgNodeCreateDTO dto) {
    // 1. validate type vs parent
    OrgNode parent = null;
    if (dto.getParentId() != null) {
        parent = mapper.selectById(dto.getParentId());
        if (parent == null) throw new BusinessException("parent not found: " + dto.getParentId());
    }
    java.util.Set<String> allowedParents = ALLOWED_PARENTS.getOrDefault(dto.getType(), java.util.Set.of());
    String parentType = parent == null ? null : parent.getType();
    boolean ok = parent == null ? allowedParents.isEmpty()
                                 : allowedParents.contains(parentType);
    if (!ok) {
        throw new BusinessException(dto.getType() + " cannot be child of " + parentType);
    }

    // 2. depth check (max 6 levels)
    if (parent != null && parent.getPath().split("\\.").length >= 6) {
        throw new BusinessException("max org tree depth (6) exceeded");
    }

    // 3. unique code
    OrgNode probe = mapper.selectOne(
        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrgNode>()
            .eq(OrgNode::getCode, dto.getCode()));
    if (probe != null) throw new BusinessException("code already exists: " + dto.getCode());

    // 4. insert with placeholder path; then update path = parent.path + . + id
    OrgNode entity = new OrgNode();
    entity.setParentId(dto.getParentId());
    entity.setType(dto.getType());
    entity.setName(dto.getName());
    entity.setCode(dto.getCode());
    entity.setPath("0"); // temporary, replaced below
    entity.setStatus("ACTIVE");
    entity.setMetadata(dto.getMetadata() == null ? "{}" : dto.getMetadata());
    mapper.insert(entity);

    String path = parent == null
        ? String.valueOf(entity.getId())
        : parent.getPath() + "." + entity.getId();
    entity.setPath(path);
    mapper.updateById(entity);

    return OrgNodeVO.from(entity);
}
```

(also clean up unused `wrappersUnused`; final code in your Edit should remove that line)

- [ ] **Step 4: Run test to verify it passes**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl org-service -Dtest=OrgNodeServiceCreateTest
```

Expected: `Tests run: 4, Failures: 0`.

- [ ] **Step 5: Commit**

```bash
git add backend/org-service/src/main/java/com/gcrf/library/org/service/impl/OrgNodeServiceImpl.java \
        backend/org-service/src/test/java/com/gcrf/library/org/service/OrgNodeServiceCreateTest.java
git commit -m "feat(org): implement OrgNodeService.create with type and depth validation"
```

---

### Task 11: OrgNodeService.update + delete

**Files:**

- Modify: `backend/org-service/src/main/java/com/gcrf/library/org/service/impl/OrgNodeServiceImpl.java`
- Create: `backend/org-service/src/test/java/com/gcrf/library/org/service/OrgNodeServiceUpdateDeleteTest.java`

- [ ] **Step 1: Write failing tests**

```java
// backend/org-service/src/test/java/com/gcrf/library/org/service/OrgNodeServiceUpdateDeleteTest.java
package com.gcrf.library.org.service;

import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.org.domain.dto.OrgNodeCreateDTO;
import com.gcrf.library.org.domain.dto.OrgNodeUpdateDTO;
import com.gcrf.library.org.domain.vo.OrgNodeVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest @Testcontainers @Transactional
class OrgNodeServiceUpdateDeleteTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired OrgNodeService svc;

    private Long createRoot(String code) {
        OrgNodeCreateDTO d = new OrgNodeCreateDTO();
        d.setType("REGION"); d.setName("R"); d.setCode(code);
        return svc.create(d).getId();
    }

    @Test
    void update_changesNameAndStatus() {
        Long id = createRoot("u1");
        OrgNodeUpdateDTO d = new OrgNodeUpdateDTO();
        d.setName("天河区教育局-改"); d.setStatus("INACTIVE");

        OrgNodeVO v = svc.update(id, d);

        assertThat(v.getName()).isEqualTo("天河区教育局-改");
        assertThat(v.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void delete_leafNode_succeeds() {
        Long id = createRoot("d1");
        svc.delete(id);
        assertThatThrownBy(() -> svc.findById(id)).isInstanceOf(BusinessException.class);
    }

    @Test
    void delete_nodeWithChildren_isRejected() {
        Long parent = createRoot("d2");
        OrgNodeCreateDTO child = new OrgNodeCreateDTO();
        child.setParentId(parent); child.setType("DISTRICT"); child.setName("c"); child.setCode("c1");
        svc.create(child);

        assertThatThrownBy(() -> svc.delete(parent))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("has children");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl org-service -Dtest=OrgNodeServiceUpdateDeleteTest
```

Expected: 3 tests fail (current stubs throw `UnsupportedOperationException`).

- [ ] **Step 3: Replace stub `update()` and `delete()` in `OrgNodeServiceImpl`**

```java
@Override
@org.springframework.transaction.annotation.Transactional
public OrgNodeVO update(Long id, OrgNodeUpdateDTO dto) {
    OrgNode e = mapper.selectById(id);
    if (e == null) throw new BusinessException("org node not found: " + id);
    e.setName(dto.getName());
    if (dto.getStatus() != null) e.setStatus(dto.getStatus());
    if (dto.getMetadata() != null) e.setMetadata(dto.getMetadata());
    mapper.updateById(e);
    return OrgNodeVO.from(e);
}

@Override
@org.springframework.transaction.annotation.Transactional
public void delete(Long id) {
    OrgNode e = mapper.selectById(id);
    if (e == null) return;
    int childCount = mapper.findByParent(id).size();
    if (childCount > 0) throw new BusinessException("node has children: " + id);
    mapper.deleteById(id);
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl org-service -Dtest=OrgNodeServiceUpdateDeleteTest
```

Expected: `Tests run: 3, Failures: 0`.

- [ ] **Step 5: Commit**

```bash
git add backend/org-service/src/main/java/com/gcrf/library/org/service/impl/OrgNodeServiceImpl.java \
        backend/org-service/src/test/java/com/gcrf/library/org/service/OrgNodeServiceUpdateDeleteTest.java
git commit -m "feat(org): implement OrgNodeService.update and delete"
```

---

### Task 12: OrgNodeService.move — 含子树 path 重写

**Files:**

- Modify: `backend/org-service/src/main/java/com/gcrf/library/org/service/impl/OrgNodeServiceImpl.java`
- Create: `backend/org-service/src/test/java/com/gcrf/library/org/service/OrgNodeServiceMoveTest.java`

- [ ] **Step 1: Write failing test**

```java
// backend/org-service/src/test/java/com/gcrf/library/org/service/OrgNodeServiceMoveTest.java
package com.gcrf.library.org.service;

import com.gcrf.library.org.domain.dto.OrgNodeCreateDTO;
import com.gcrf.library.org.domain.vo.OrgNodeVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest @Testcontainers @Transactional
class OrgNodeServiceMoveTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired OrgNodeService svc;

    @Test
    void move_subtree_rewritesPathOfAllDescendants() {
        // R1 -> D1 -> S1
        // R2
        // move D1 (with S1) to R2
        Long r1 = create("REGION", null, "r1");
        Long r2 = create("REGION", null, "r2");
        Long d1 = create("DISTRICT", r1, "d1");
        Long s1 = create("SCHOOL",   d1, "s1");

        OrgNodeVO moved = svc.move(d1, r2);

        assertThat(moved.getParentId()).isEqualTo(r2);
        assertThat(moved.getPath()).isEqualTo(r2 + "." + d1);

        OrgNodeVO sChild = svc.findById(s1);
        assertThat(sChild.getPath()).isEqualTo(r2 + "." + d1 + "." + s1);
    }

    @Test
    void move_rejectsCycle() {
        Long r = create("REGION", null, "rc");
        Long c = create("DISTRICT", r, "cc");
        // can't move root under its own descendant
        assertThatThrownBy(() -> svc.move(r, c))
            .hasMessageContaining("cycle");
    }

    private Long create(String type, Long parent, String code) {
        OrgNodeCreateDTO d = new OrgNodeCreateDTO();
        d.setType(type); d.setParentId(parent); d.setName(code); d.setCode(code);
        return svc.create(d).getId();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl org-service -Dtest=OrgNodeServiceMoveTest
```

Expected: stub throws `UnsupportedOperationException`.

- [ ] **Step 3: Replace stub `move()` in `OrgNodeServiceImpl`**

```java
@Override
@org.springframework.transaction.annotation.Transactional
public OrgNodeVO move(Long id, Long newParentId) {
    OrgNode self = mapper.selectById(id);
    if (self == null) throw new BusinessException("node not found: " + id);

    // forbid moving onto itself or its descendant
    OrgNode newParent = mapper.selectById(newParentId);
    if (newParent == null) throw new BusinessException("new parent not found: " + newParentId);
    if (newParent.getPath().equals(self.getPath()) ||
        newParent.getPath().startsWith(self.getPath() + ".")) {
        throw new BusinessException("cycle: cannot move into own descendant");
    }

    // type validation under new parent
    java.util.Set<String> allowed = ALLOWED_PARENTS.getOrDefault(self.getType(), java.util.Set.of());
    if (!allowed.contains(newParent.getType())) {
        throw new BusinessException(self.getType() + " cannot be child of " + newParent.getType());
    }

    String oldPath = self.getPath();
    String newPath = newParent.getPath() + "." + self.getId();

    self.setParentId(newParentId);
    self.setPath(newPath);
    mapper.updateById(self);

    // rewrite descendants
    mapper.moveSubtree(oldPath, newPath);

    return OrgNodeVO.from(self);
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl org-service -Dtest=OrgNodeServiceMoveTest
```

Expected: `Tests run: 2, Failures: 0`.

- [ ] **Step 5: Commit**

```bash
git add backend/org-service/src/main/java/com/gcrf/library/org/service/impl/OrgNodeServiceImpl.java \
        backend/org-service/src/test/java/com/gcrf/library/org/service/OrgNodeServiceMoveTest.java
git commit -m "feat(org): implement OrgNodeService.move with subtree path rewrite"
```

---

### Task 13: OrgNodeController + REST API

**Files:**

- Create: `backend/org-service/src/main/java/com/gcrf/library/org/controller/OrgNodeController.java`
- Create: `backend/org-service/src/test/java/com/gcrf/library/org/controller/OrgNodeControllerTest.java`

- [ ] **Step 1: Write failing controller test**

```java
// backend/org-service/src/test/java/com/gcrf/library/org/controller/OrgNodeControllerTest.java
package com.gcrf.library.org.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.org.domain.dto.OrgNodeCreateDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class OrgNodeControllerTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void post_creates_then_get_returns() throws Exception {
        OrgNodeCreateDTO d = new OrgNodeCreateDTO();
        d.setType("REGION"); d.setName("天河"); d.setCode("th_ctrl");

        mvc.perform(post("/api/v1/org/nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(d)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.code").value("th_ctrl"));

        mvc.perform(get("/api/v1/org/nodes").param("parentId", ""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.code=='th_ctrl')]").exists());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl org-service -Dtest=OrgNodeControllerTest
```

Expected: COMPILATION FAILURE — `OrgNodeController` not found.

- [ ] **Step 3: Implement controller**

```java
// backend/org-service/src/main/java/com/gcrf/library/org/controller/OrgNodeController.java
package com.gcrf.library.org.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.org.domain.dto.OrgNodeCreateDTO;
import com.gcrf.library.org.domain.dto.OrgNodeMoveDTO;
import com.gcrf.library.org.domain.dto.OrgNodeUpdateDTO;
import com.gcrf.library.org.domain.vo.OrgNodeVO;
import com.gcrf.library.org.domain.vo.OrgTreeNodeVO;
import com.gcrf.library.org.service.OrgNodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/org/nodes")
@RequiredArgsConstructor
public class OrgNodeController {

    private final OrgNodeService service;

    @GetMapping
    public Result<List<OrgTreeNodeVO>> list(@RequestParam(required = false) Long parentId) {
        return Result.success(parentId == null ? service.findRoots() : service.findChildren(parentId));
    }

    @GetMapping("/{id}")
    public Result<OrgNodeVO> getById(@PathVariable Long id) {
        return Result.success(service.findById(id));
    }

    @GetMapping("/{id}/subtree")
    public Result<OrgTreeNodeVO> subtree(@PathVariable Long id) {
        return Result.success(service.findSubtree(id));
    }

    @PostMapping
    public Result<OrgNodeVO> create(@Valid @RequestBody OrgNodeCreateDTO dto) {
        return Result.success(service.create(dto));
    }

    @PutMapping("/{id}")
    public Result<OrgNodeVO> update(@PathVariable Long id,
                                    @Valid @RequestBody OrgNodeUpdateDTO dto) {
        return Result.success(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/move")
    public Result<OrgNodeVO> move(@PathVariable Long id,
                                  @Valid @RequestBody OrgNodeMoveDTO dto) {
        return Result.success(service.move(id, dto.getNewParentId()));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl org-service -Dtest=OrgNodeControllerTest
```

Expected: `Tests run: 1, Failures: 0`.

- [ ] **Step 5: Commit**

```bash
git add backend/org-service/src/main/java/com/gcrf/library/org/controller/OrgNodeController.java \
        backend/org-service/src/test/java/com/gcrf/library/org/controller/OrgNodeControllerTest.java
git commit -m "feat(org): expose OrgNode REST API (list/create/update/delete/move)"
```

---

### Task 14: SchoolProvisioning — per-school migration template

**Files:**

- Create: `backend/org-service/src/main/resources/db/migration/per-school/V001__init_school_schema.sql`

- [ ] **Step 1: Create per-school migration template**

```sql
-- backend/org-service/src/main/resources/db/migration/per-school/V001__init_school_schema.sql
-- Initial per-school schema scaffolding.
-- Each school's schema gets its own copy of these tables.
-- Will be extended by other services (book-service, circulation-service, ...)
-- in their own per-school migrations as features come online.

CREATE TABLE IF NOT EXISTS school_meta (
    school_code         VARCHAR(50)  PRIMARY KEY,
    school_name         VARCHAR(200) NOT NULL,
    initialized_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    metadata            JSONB        NOT NULL DEFAULT '{}'
);

-- Reader (will be expanded by reader-service migrations)
CREATE TABLE IF NOT EXISTS reader (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    card_number     VARCHAR(30)  UNIQUE,
    grade           VARCHAR(20),
    class           VARCHAR(20),
    max_borrow      INT          NOT NULL DEFAULT 5,
    borrow_days     INT          NOT NULL DEFAULT 30,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_reader_user_id ON reader(user_id);

-- Book catalog skeleton (will be expanded by book-service migrations)
CREATE TABLE IF NOT EXISTS book_catalog (
    id              BIGSERIAL    PRIMARY KEY,
    isbn            VARCHAR(20),
    title           VARCHAR(500) NOT NULL,
    author          VARCHAR(500),
    classification  VARCHAR(50),
    total_count     INT          NOT NULL DEFAULT 0,
    available_count INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_book_catalog_isbn ON book_catalog(isbn);

-- Book copy (will be expanded)
CREATE TABLE IF NOT EXISTS book_copy (
    id              BIGSERIAL    PRIMARY KEY,
    catalog_id      BIGINT       NOT NULL REFERENCES book_catalog(id),
    barcode         VARCHAR(50)  UNIQUE NOT NULL,
    call_no         VARCHAR(100),
    status          VARCHAR(20)  NOT NULL DEFAULT 'IN',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Borrow record skeleton (will be expanded)
CREATE TABLE IF NOT EXISTS borrow_record (
    id                BIGSERIAL    PRIMARY KEY,
    reader_id         BIGINT       NOT NULL,
    copy_id           BIGINT       NOT NULL,
    borrow_at         TIMESTAMPTZ  NOT NULL,
    due_at            TIMESTAMPTZ  NOT NULL,
    return_at         TIMESTAMPTZ,
    idempotency_key   VARCHAR(120) UNIQUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_borrow_active
    ON borrow_record(reader_id, return_at) WHERE return_at IS NULL;
```

- [ ] **Step 2: Commit**

```bash
git add backend/org-service/src/main/resources/db/migration/per-school/V001__init_school_schema.sql
git commit -m "feat(org): add per-school migration template (school_meta + reader + book + borrow)"
```

---

### Task 15: SchoolProvisioningService — 建节点 + 建 schema + 跑 migration

**Files:**

- Create: `backend/org-service/src/main/java/com/gcrf/library/org/service/SchoolProvisioningService.java`
- Create: `backend/org-service/src/main/java/com/gcrf/library/org/service/impl/SchoolProvisioningServiceImpl.java`
- Create: `backend/org-service/src/test/java/com/gcrf/library/org/service/SchoolProvisioningServiceTest.java`

- [ ] **Step 1: Write failing test**

```java
// backend/org-service/src/test/java/com/gcrf/library/org/service/SchoolProvisioningServiceTest.java
package com.gcrf.library.org.service;

import com.gcrf.library.org.domain.dto.OrgNodeCreateDTO;
import com.gcrf.library.org.domain.dto.SchoolCreateDTO;
import com.gcrf.library.org.domain.vo.OrgNodeVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest @Testcontainers
class SchoolProvisioningServiceTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired OrgNodeService orgService;
    @Autowired SchoolProvisioningService prov;
    @Autowired JdbcTemplate jdbc;

    @Test
    void createSchool_buildsSchemaAndRunsMigration() {
        OrgNodeCreateDTO region = new OrgNodeCreateDTO();
        region.setType("REGION"); region.setName("天河"); region.setCode("th_prov");
        Long regionId = orgService.create(region).getId();

        SchoolCreateDTO sd = new SchoolCreateDTO();
        sd.setParentId(regionId); sd.setName("实验小学"); sd.setCode("syxx_prov");
        OrgNodeVO school = prov.createSchool(sd);

        assertThat(school.getTenantSchema()).matches("^school_\\d+$");

        String schema = school.getTenantSchema();
        Integer tableCount = jdbc.queryForObject(
            "SELECT count(*) FROM information_schema.tables WHERE table_schema = ?",
            Integer.class, schema);
        assertThat(tableCount).isGreaterThanOrEqualTo(4); // school_meta + reader + book_catalog + book_copy + borrow_record
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl org-service -Dtest=SchoolProvisioningServiceTest
```

Expected: COMPILATION FAILURE — `SchoolProvisioningService` not found.

- [ ] **Step 3: Write service interface and impl**

```java
// backend/org-service/src/main/java/com/gcrf/library/org/service/SchoolProvisioningService.java
package com.gcrf.library.org.service;

import com.gcrf.library.org.domain.dto.SchoolCreateDTO;
import com.gcrf.library.org.domain.vo.OrgNodeVO;

public interface SchoolProvisioningService {
    OrgNodeVO createSchool(SchoolCreateDTO dto);
}
```

```java
// backend/org-service/src/main/java/com/gcrf/library/org/service/impl/SchoolProvisioningServiceImpl.java
package com.gcrf.library.org.service.impl;

import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.tenant.PerSchoolFlywayService;
import com.gcrf.library.org.domain.dto.OrgNodeCreateDTO;
import com.gcrf.library.org.domain.dto.SchoolCreateDTO;
import com.gcrf.library.org.domain.entity.OrgNode;
import com.gcrf.library.org.domain.vo.OrgNodeVO;
import com.gcrf.library.org.mapper.OrgNodeMapper;
import com.gcrf.library.org.service.OrgNodeService;
import com.gcrf.library.org.service.SchoolProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolProvisioningServiceImpl implements SchoolProvisioningService {

    private final OrgNodeService orgService;
    private final OrgNodeMapper mapper;
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    @Override
    @Transactional
    public OrgNodeVO createSchool(SchoolCreateDTO dto) {
        // 1. create org node of type SCHOOL
        OrgNodeCreateDTO nodeDto = new OrgNodeCreateDTO();
        nodeDto.setParentId(dto.getParentId());
        nodeDto.setType("SCHOOL");
        nodeDto.setName(dto.getName());
        nodeDto.setCode(dto.getCode());
        OrgNodeVO created = orgService.create(nodeDto);

        // 2. derive schema name (school_<id> zero-padded to 6 digits)
        String schema = "school_" + String.format("%06d", created.getId());

        // 3. create schema in same transaction
        if (!schema.matches("^school_\\d+$")) {
            throw new BusinessException("derived schema invalid: " + schema);
        }
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS " + schema);

        // 4. update org_node.tenant_schema = schema
        OrgNode entity = mapper.selectById(created.getId());
        entity.setTenantSchema(schema);
        mapper.updateById(entity);

        // 5. run per-school flyway migration (NOTE: outside the @Transactional boundary
        //    of the controller because Flyway opens its own connection — this is fine
        //    because step 3 and 4 are committed atomically before the request returns;
        //    if step 6 fails, schema exists but is empty — operator can re-run.)
        new PerSchoolFlywayService(dataSource).migrateSchool(schema);

        // 6. seed school_meta inside the new schema
        jdbc.execute("INSERT INTO " + schema + ".school_meta (school_code, school_name) VALUES ('"
            + dto.getCode().replace("'", "''") + "', '"
            + dto.getName().replace("'", "''") + "')");

        log.info("school provisioned: id={}, schema={}, code={}", created.getId(), schema, dto.getCode());
        return OrgNodeVO.from(entity);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl org-service -Dtest=SchoolProvisioningServiceTest
```

Expected: `Tests run: 1, Failures: 0`.

- [ ] **Step 5: Commit**

```bash
git add backend/org-service/src/main/java/com/gcrf/library/org/service/SchoolProvisioningService.java \
        backend/org-service/src/main/java/com/gcrf/library/org/service/impl/SchoolProvisioningServiceImpl.java \
        backend/org-service/src/test/java/com/gcrf/library/org/service/SchoolProvisioningServiceTest.java
git commit -m "feat(org): add SchoolProvisioning to create org node + schema + run migration"
```

---

### Task 16: SchoolProvisioningController + REST API

**Files:**

- Create: `backend/org-service/src/main/java/com/gcrf/library/org/controller/SchoolProvisioningController.java`

- [ ] **Step 1: Implement controller**

```java
// backend/org-service/src/main/java/com/gcrf/library/org/controller/SchoolProvisioningController.java
package com.gcrf.library.org.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.org.domain.dto.SchoolCreateDTO;
import com.gcrf.library.org.domain.vo.OrgNodeVO;
import com.gcrf.library.org.service.SchoolProvisioningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/org/schools")
@RequiredArgsConstructor
public class SchoolProvisioningController {

    private final SchoolProvisioningService service;

    @PostMapping
    public Result<OrgNodeVO> createSchool(@Valid @RequestBody SchoolCreateDTO dto) {
        return Result.success(service.createSchool(dto));
    }
}
```

- [ ] **Step 2: Smoke test via mvn**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl org-service
```

Expected: all org-service tests still pass.

- [ ] **Step 3: Commit**

```bash
git add backend/org-service/src/main/java/com/gcrf/library/org/controller/SchoolProvisioningController.java
git commit -m "feat(org): expose POST /api/v1/org/schools provisioning endpoint"
```

---

### Task 17: Excel 批量导入（OrgImportService + Controller）

**Files:**

- Create: `backend/org-service/src/main/java/com/gcrf/library/org/service/OrgImportService.java`
- Create: `backend/org-service/src/main/java/com/gcrf/library/org/service/impl/OrgImportServiceImpl.java`
- Create: `backend/org-service/src/main/java/com/gcrf/library/org/domain/dto/OrgImportRow.java`
- Create: `backend/org-service/src/main/java/com/gcrf/library/org/controller/OrgImportController.java`
- Create: `backend/org-service/src/test/java/com/gcrf/library/org/service/OrgImportServiceTest.java`
- Create: `backend/org-service/src/test/resources/org-import-sample.xlsx` (binary fixture — generate via test code or check in pre-built)

- [ ] **Step 1: Write failing test**

```java
// backend/org-service/src/test/java/com/gcrf/library/org/service/OrgImportServiceTest.java
package com.gcrf.library.org.service;

import com.alibaba.excel.EasyExcel;
import com.gcrf.library.org.domain.dto.OrgImportRow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest @Testcontainers @Transactional
class OrgImportServiceTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired OrgImportService importService;
    @Autowired OrgNodeService orgService;

    @Test
    void importExcel_createsHierarchy() throws Exception {
        // build excel in memory: 3 rows (region, district, school)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, OrgImportRow.class).sheet("Sheet1").doWrite(List.of(
            row("REGION",   null,        "th_imp",  "天河区教育局"),
            row("DISTRICT", "th_imp",    "sp_imp",  "石牌街"),
            row("SCHOOL",   "sp_imp",    "syxx_imp","实验小学")
        ));
        MockMultipartFile mf = new MockMultipartFile("file", "import.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());

        var report = importService.importExcel(mf);
        assertThat(report.getCreated()).isEqualTo(3);
        assertThat(report.getFailed()).isEqualTo(0);
    }

    private OrgImportRow row(String type, String parentCode, String code, String name) {
        OrgImportRow r = new OrgImportRow();
        r.setType(type); r.setParentCode(parentCode); r.setCode(code); r.setName(name);
        return r;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl org-service -Dtest=OrgImportServiceTest
```

Expected: COMPILATION FAILURE.

- [ ] **Step 3: Implement DTO + service + controller**

```java
// backend/org-service/src/main/java/com/gcrf/library/org/domain/dto/OrgImportRow.java
package com.gcrf.library.org.domain.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class OrgImportRow {
    @ExcelProperty("类型")        private String type;       // REGION / DISTRICT / SCHOOL / ...
    @ExcelProperty("上级 code")   private String parentCode; // null for root
    @ExcelProperty("code")        private String code;
    @ExcelProperty("名称")        private String name;
}
```

```java
// backend/org-service/src/main/java/com/gcrf/library/org/service/OrgImportService.java
package com.gcrf.library.org.service;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

public interface OrgImportService {

    @Data @Builder
    class ImportReport {
        int created;
        int failed;
        java.util.List<String> errors;
    }

    ImportReport importExcel(MultipartFile file);
}
```

```java
// backend/org-service/src/main/java/com/gcrf/library/org/service/impl/OrgImportServiceImpl.java
package com.gcrf.library.org.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.ReadListener;
import com.gcrf.library.org.domain.dto.OrgImportRow;
import com.gcrf.library.org.domain.dto.OrgNodeCreateDTO;
import com.gcrf.library.org.domain.entity.OrgNode;
import com.gcrf.library.org.mapper.OrgNodeMapper;
import com.gcrf.library.org.service.OrgImportService;
import com.gcrf.library.org.service.OrgNodeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgImportServiceImpl implements OrgImportService {

    private final OrgNodeService orgService;
    private final OrgNodeMapper mapper;

    @Override
    @Transactional
    public ImportReport importExcel(MultipartFile file) {
        List<OrgImportRow> rows = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), OrgImportRow.class, new ReadListener<OrgImportRow>() {
                @Override public void invoke(OrgImportRow row, com.alibaba.excel.context.AnalysisContext ctx) {
                    rows.add(row);
                }
                @Override public void doAfterAllAnalysed(com.alibaba.excel.context.AnalysisContext ctx) {}
            }).sheet().doRead();
        } catch (IOException e) {
            throw new RuntimeException("read excel failed", e);
        }

        int created = 0, failed = 0;
        List<String> errors = new ArrayList<>();
        for (OrgImportRow row : rows) {
            try {
                Long parentId = null;
                if (row.getParentCode() != null && !row.getParentCode().isBlank()) {
                    OrgNode parent = mapper.selectOne(
                        new LambdaQueryWrapper<OrgNode>().eq(OrgNode::getCode, row.getParentCode()));
                    if (parent == null) throw new IllegalArgumentException("parent code not found: " + row.getParentCode());
                    parentId = parent.getId();
                }
                OrgNodeCreateDTO dto = new OrgNodeCreateDTO();
                dto.setParentId(parentId);
                dto.setType(row.getType());
                dto.setName(row.getName());
                dto.setCode(row.getCode());
                orgService.create(dto);
                created++;
            } catch (Exception e) {
                failed++;
                errors.add(row.getCode() + ": " + e.getMessage());
                log.warn("import row failed: {}", row, e);
            }
        }

        return ImportReport.builder().created(created).failed(failed).errors(errors).build();
    }
}
```

```java
// backend/org-service/src/main/java/com/gcrf/library/org/controller/OrgImportController.java
package com.gcrf.library.org.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.org.service.OrgImportService;
import com.gcrf.library.org.service.OrgImportService.ImportReport;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/org/nodes")
@RequiredArgsConstructor
public class OrgImportController {

    private final OrgImportService importService;

    @PostMapping("/import")
    public Result<ImportReport> importExcel(@RequestParam("file") MultipartFile file) {
        return Result.success(importService.importExcel(file));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl org-service -Dtest=OrgImportServiceTest
```

Expected: `Tests run: 1, Failures: 0`.

- [ ] **Step 5: Commit**

```bash
git add backend/org-service/src/main/java/com/gcrf/library/org/domain/dto/OrgImportRow.java \
        backend/org-service/src/main/java/com/gcrf/library/org/service/OrgImportService.java \
        backend/org-service/src/main/java/com/gcrf/library/org/service/impl/OrgImportServiceImpl.java \
        backend/org-service/src/main/java/com/gcrf/library/org/controller/OrgImportController.java \
        backend/org-service/src/test/java/com/gcrf/library/org/service/OrgImportServiceTest.java
git commit -m "feat(org): add Excel batch import via EasyExcel"
```

---

### Task 18: 前端 — API client + 组织树页面

**Files:**

- Create: `web-admin/src/api/org.js`
- Create: `web-admin/src/views/system/organizations.vue`
- Modify: `web-admin/src/router/index.js`

- [ ] **Step 1: Create API client**

```javascript
// web-admin/src/api/org.js
import request from "@/utils/request";

export function listOrgNodes(parentId) {
  return request({
    url: "/api/v1/org/nodes",
    method: "get",
    params: { parentId },
  });
}

export function getOrgNode(id) {
  return request({ url: `/api/v1/org/nodes/${id}`, method: "get" });
}

export function getOrgSubtree(id) {
  return request({ url: `/api/v1/org/nodes/${id}/subtree`, method: "get" });
}

export function createOrgNode(data) {
  return request({ url: "/api/v1/org/nodes", method: "post", data });
}

export function updateOrgNode(id, data) {
  return request({ url: `/api/v1/org/nodes/${id}`, method: "put", data });
}

export function deleteOrgNode(id) {
  return request({ url: `/api/v1/org/nodes/${id}`, method: "delete" });
}

export function moveOrgNode(id, newParentId) {
  return request({
    url: `/api/v1/org/nodes/${id}/move`,
    method: "post",
    data: { newParentId },
  });
}

export function createSchool(data) {
  return request({ url: "/api/v1/org/schools", method: "post", data });
}

export function importOrgExcel(file) {
  const fd = new FormData();
  fd.append("file", file);
  return request({
    url: "/api/v1/org/nodes/import",
    method: "post",
    data: fd,
    headers: { "Content-Type": "multipart/form-data" },
  });
}
```

- [ ] **Step 2: Create organizations.vue page**

```vue
<!-- web-admin/src/views/system/organizations.vue -->
<template>
  <div class="page-container">
    <div class="page-header">
      <h1 class="page-header-title">组织架构管理</h1>
      <p class="page-header-description">
        教育局 → 学校 → 年级 → 班级 多级组织树
      </p>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="10">
        <div class="card">
          <div class="card-header">
            <span>组织树</span>
            <div>
              <el-button
                size="small"
                type="primary"
                :icon="Plus"
                @click="onAddRoot"
                >新增根节点</el-button
              >
              <el-button size="small" :icon="Upload" @click="onImportClick"
                >批量导入</el-button
              >
              <input
                ref="fileInput"
                type="file"
                accept=".xlsx"
                hidden
                @change="onImport"
              />
            </div>
          </div>
          <div class="card-content">
            <el-tree
              v-loading="loading"
              :data="tree"
              node-key="id"
              :props="{ label: 'name', children: 'children' }"
              highlight-current
              @node-click="onSelect"
            >
              <template #default="{ node, data }">
                <span class="org-node-row">
                  <el-tag size="small" :type="tagType(data.type)">{{
                    typeLabel(data.type)
                  }}</el-tag>
                  <span class="org-node-name">{{ node.label }}</span>
                  <span class="org-node-code">[{{ data.code }}]</span>
                </span>
              </template>
            </el-tree>
          </div>
        </div>
      </el-col>

      <el-col :xs="24" :md="14">
        <div class="card">
          <div class="card-header">
            <span>{{ current ? "节点详情" : "请选择节点" }}</span>
            <div v-if="current">
              <el-button size="small" :icon="Plus" @click="onAddChild"
                >添加子节点</el-button
              >
              <el-button
                v-if="canCreateSchool"
                size="small"
                type="success"
                @click="onCreateSchool"
                >建学校</el-button
              >
              <el-button
                size="small"
                type="danger"
                :icon="Delete"
                @click="onDelete"
                >删除</el-button
              >
            </div>
          </div>
          <div v-if="current" class="card-content">
            <el-form label-width="120px">
              <el-form-item label="ID">{{ current.id }}</el-form-item>
              <el-form-item label="类型">{{
                typeLabel(current.type)
              }}</el-form-item>
              <el-form-item label="路径">{{ current.path }}</el-form-item>
              <el-form-item label="租户 schema">
                <el-tag v-if="current.tenantSchema">{{
                  current.tenantSchema
                }}</el-tag>
                <span v-else class="text-muted">—</span>
              </el-form-item>
              <el-form-item label="名称">
                <el-input v-model="editName" />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="editStatus">
                  <el-option label="ACTIVE" value="ACTIVE" />
                  <el-option label="INACTIVE" value="INACTIVE" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="onSave">保存</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 新增节点对话框 -->
    <el-dialog
      v-model="addVisible"
      :title="addParent ? `在 ${addParent.name} 下新增` : '新增根节点'"
      width="500px"
    >
      <el-form :model="addForm" label-width="100px">
        <el-form-item label="类型" required>
          <el-select v-model="addForm.type" placeholder="选择类型">
            <el-option
              v-for="t in availableTypes"
              :key="t"
              :label="typeLabel(t)"
              :value="t"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="addForm.name" />
        </el-form-item>
        <el-form-item label="code" required>
          <el-input v-model="addForm.code" placeholder="字母数字下划线" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" @click="onAddSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 建学校对话框 -->
    <el-dialog
      v-model="schoolVisible"
      title="新建学校（自动建 schema）"
      width="500px"
    >
      <el-form :model="schoolForm" label-width="100px">
        <el-form-item label="名称" required
          ><el-input v-model="schoolForm.name"
        /></el-form-item>
        <el-form-item label="code" required
          ><el-input v-model="schoolForm.code"
        /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="schoolVisible = false">取消</el-button>
        <el-button
          type="success"
          :loading="schoolLoading"
          @click="onSchoolSubmit"
          >建学校</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Delete, Upload } from "@element-plus/icons-vue";
import {
  listOrgNodes,
  getOrgNode,
  getOrgSubtree,
  createOrgNode,
  updateOrgNode,
  deleteOrgNode,
  createSchool,
  importOrgExcel,
} from "@/api/org";

const TYPE_LABELS = {
  REGION: "教育局",
  DISTRICT: "区县",
  SCHOOL: "学校",
  SUB_SCHOOL: "分校",
  BRANCH: "分馆",
  STAGE: "学段",
  GRADE: "年级",
  CLASS: "班级",
};
const PARENT_RULES = {
  REGION: [],
  DISTRICT: ["REGION"],
  SCHOOL: ["REGION", "DISTRICT"],
  SUB_SCHOOL: ["SCHOOL"],
  BRANCH: ["SCHOOL", "SUB_SCHOOL"],
  STAGE: ["SCHOOL", "SUB_SCHOOL"],
  GRADE: ["SCHOOL", "SUB_SCHOOL", "STAGE"],
  CLASS: ["GRADE"],
};

const tree = ref([]);
const loading = ref(false);
const current = ref(null);
const editName = ref("");
const editStatus = ref("ACTIVE");

const addVisible = ref(false);
const addParent = ref(null);
const addForm = reactive({ type: "", name: "", code: "" });
const availableTypes = computed(() => {
  if (!addParent.value) return ["REGION"];
  return Object.keys(PARENT_RULES).filter((t) =>
    PARENT_RULES[t].includes(addParent.value.type),
  );
});

const schoolVisible = ref(false);
const schoolForm = reactive({ name: "", code: "" });
const schoolLoading = ref(false);
const canCreateSchool = computed(
  () =>
    current.value &&
    (current.value.type === "REGION" || current.value.type === "DISTRICT"),
);

const fileInput = ref();

function typeLabel(t) {
  return TYPE_LABELS[t] || t;
}
function tagType(t) {
  if (t === "REGION") return "danger";
  if (t === "DISTRICT") return "warning";
  if (t === "SCHOOL") return "success";
  return "";
}

async function loadRoots() {
  loading.value = true;
  try {
    const res = await listOrgNodes();
    tree.value = (res.data || []).map((n) => ({ ...n, children: [] }));
    // lazy load children: fetch on click
  } finally {
    loading.value = false;
  }
}

async function onSelect(node) {
  current.value = node;
  editName.value = node.name;
  editStatus.value = node.status;
  // expand children if not loaded
  if (node.children?.length === 0) {
    const res = await listOrgNodes(node.id);
    node.children = (res.data || []).map((n) => ({ ...n, children: [] }));
  }
}

function onAddRoot() {
  addParent.value = null;
  addForm.type = "REGION";
  addForm.name = "";
  addForm.code = "";
  addVisible.value = true;
}

function onAddChild() {
  addParent.value = current.value;
  addForm.type = availableTypes.value[0] || "";
  addForm.name = "";
  addForm.code = "";
  addVisible.value = true;
}

async function onAddSubmit() {
  try {
    await createOrgNode({
      parentId: addParent.value?.id,
      type: addForm.type,
      name: addForm.name,
      code: addForm.code,
    });
    ElMessage.success("已创建");
    addVisible.value = false;
    await loadRoots();
  } catch (e) {
    /* error already shown by request interceptor */
  }
}

async function onSave() {
  await updateOrgNode(current.value.id, {
    name: editName.value,
    status: editStatus.value,
  });
  ElMessage.success("已保存");
  current.value.name = editName.value;
  current.value.status = editStatus.value;
}

async function onDelete() {
  await ElMessageBox.confirm(`确定删除节点 "${current.value.name}"？`, "确认", {
    type: "warning",
  });
  await deleteOrgNode(current.value.id);
  ElMessage.success("已删除");
  current.value = null;
  await loadRoots();
}

function onCreateSchool() {
  schoolForm.name = "";
  schoolForm.code = "";
  schoolVisible.value = true;
}

async function onSchoolSubmit() {
  schoolLoading.value = true;
  try {
    const res = await createSchool({
      parentId: current.value.id,
      ...schoolForm,
    });
    ElMessage.success(`学校已创建，schema=${res.data.tenantSchema}`);
    schoolVisible.value = false;
    await loadRoots();
  } finally {
    schoolLoading.value = false;
  }
}

function onImportClick() {
  fileInput.value.click();
}

async function onImport(ev) {
  const file = ev.target.files[0];
  if (!file) return;
  const res = await importOrgExcel(file);
  ElMessage.success(
    `导入完成：成功 ${res.data.created} / 失败 ${res.data.failed}`,
  );
  ev.target.value = "";
  await loadRoots();
}

onMounted(loadRoots);
</script>

<style scoped>
.org-node-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.org-node-name {
  font-weight: 500;
}
.org-node-code {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.text-muted {
  color: var(--el-text-color-secondary);
}
</style>
```

- [ ] **Step 3: Register route**

In `web-admin/src/router/index.js`, find the `system` route children block and add at the start:

```js
{
  path: 'organizations',
  name: 'SystemOrganizations',
  component: () => import('@/views/system/organizations.vue'),
  meta: { title: '组织架构', icon: 'OfficeBuilding', requireAuth: true }
},
```

- [ ] **Step 4: Build frontend**

```bash
cd web-admin
npm run build 2>&1 | tail -5
```

Expected: `built in X.XXs` (no compile errors).

- [ ] **Step 5: Commit**

```bash
git add web-admin/src/api/org.js \
        web-admin/src/views/system/organizations.vue \
        web-admin/src/router/index.js
git commit -m "feat(web-admin): add organization tree management page"
```

---

### Task 19: K8s 部署 manifest

**Files:**

- Modify: `deployment/k8s/10-services.yaml` (add gcrf-org Deployment + Service)
- Modify: `deployment/k8s/02-configmap.yaml` (add org-service to service-discovery)
- Create: `/tmp/gcrf-deploy/Dockerfile-org-service`

- [ ] **Step 1: Add gcrf-org Deployment to 10-services.yaml**

Append to `deployment/k8s/10-services.yaml`:

```yaml
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gcrf-org
  namespace: gcrf-prod
spec:
  replicas: 1
  selector:
    matchLabels:
      app: gcrf-org
  template:
    metadata:
      labels:
        app: gcrf-org
    spec:
      containers:
        - name: org-service
          image: gcrf/org-service:latest
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8090
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
                  { secretKeyRef: { name: gcrf-secrets, key: db-password } },
              }
            - {
                name: REDIS_HOST,
                valueFrom:
                  { configMapKeyRef: { name: gcrf-config, key: redis.host } },
              }
            - { name: REDIS_DB, value: "1" }
            - {
                name: REDIS_PASSWORD,
                valueFrom:
                  { secretKeyRef: { name: gcrf-secrets, key: redis-password } },
              }
          volumeMounts:
            - name: discovery-config
              mountPath: /app/config
          readinessProbe:
            httpGet: { path: /actuator/health, port: 8090 }
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:
            httpGet: { path: /actuator/health, port: 8090 }
            initialDelaySeconds: 60
            periodSeconds: 20
      volumes:
        - name: discovery-config
          configMap:
            name: gcrf-service-discovery
---
apiVersion: v1
kind: Service
metadata:
  name: gcrf-org
  namespace: gcrf-prod
spec:
  selector:
    app: gcrf-org
  ports:
    - port: 8090
      targetPort: 8090
```

- [ ] **Step 2: Add org-service to service-discovery configmap**

In `deployment/k8s/02-configmap.yaml`, find the `instances:` block and add:

```yaml
org-service:
  - uri: http://gcrf-org:8090
```

- [ ] **Step 3: Create Dockerfile**

```dockerfile
# /tmp/gcrf-deploy/Dockerfile-org-service
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY org-service.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

- [ ] **Step 4: Build + push image to cluster**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean package -pl org-service -am -DskipTests -q
cp org-service/target/org-service-1.0.0-SNAPSHOT.jar /tmp/gcrf-deploy/org-service.jar
cd /tmp/gcrf-deploy
docker build --platform linux/amd64 -t gcrf/org-service:latest -f Dockerfile-org-service .
docker save gcrf/org-service:latest | gzip > org-service.tar.gz

# SCP to all 3 nodes (need t1/t2/t3 hosts via sshpass)
for host in t1@192.168.1.20 t2@192.168.1.19 t3@192.168.1.21; do
  sshpass -p gcrf scp -o StrictHostKeyChecking=no org-service.tar.gz $host:/tmp/
done

# Import on each node
for host in t1@192.168.1.20 t2@192.168.1.19 t3@192.168.1.21; do
  sshpass -p gcrf ssh -o StrictHostKeyChecking=no $host \
    "echo gcrf | sudo -S sh -c 'gunzip -c /tmp/org-service.tar.gz | ctr -n k8s.io images import -'"
done
```

- [ ] **Step 5: Apply manifests**

```bash
sshpass -p gcrf ssh t1@192.168.1.20 "echo gcrf | sudo -S kubectl apply -f -" \
  < /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/deployment/k8s/02-configmap.yaml
sshpass -p gcrf ssh t1@192.168.1.20 "echo gcrf | sudo -S kubectl apply -f -" \
  < /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/deployment/k8s/10-services.yaml

sshpass -p gcrf ssh t1@192.168.1.20 \
  "echo gcrf | sudo -S kubectl rollout status deployment/gcrf-org -n gcrf-prod --timeout=180s"
```

Expected: `deployment "gcrf-org" successfully rolled out`.

- [ ] **Step 6: Commit**

```bash
git add deployment/k8s/10-services.yaml deployment/k8s/02-configmap.yaml
git commit -m "feat(infra): add gcrf-org K8s deployment and service-discovery entry"
```

---

### Task 20: 端到端验证

- [ ] **Step 1: Smoke-test API directly via curl**

```bash
# Use admin token from existing system; replace ADMIN_TOKEN with a real one
TOKEN=$(curl -sf -X POST http://192.168.1.19:31080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")

# Create REGION
curl -sf -X POST http://192.168.1.19:31080/api/v1/org/nodes \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"type":"REGION","name":"天河区教育局","code":"th_smoke"}'

# Create DISTRICT
PARENT=$(curl -sf "http://192.168.1.19:31080/api/v1/org/nodes" \
  -H "Authorization: Bearer $TOKEN" | python3 -c "import sys,json;print([n for n in json.load(sys.stdin)['data'] if n['code']=='th_smoke'][0]['id'])")
curl -sf -X POST http://192.168.1.19:31080/api/v1/org/nodes \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"parentId\":$PARENT,\"type\":\"DISTRICT\",\"name\":\"石牌街\",\"code\":\"sp_smoke\"}"

# Create SCHOOL via provisioning (auto creates schema)
curl -sf -X POST http://192.168.1.19:31080/api/v1/org/schools \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"parentId\":$PARENT,\"name\":\"实验小学\",\"code\":\"syxx_smoke\"}"
```

Expected: each call returns HTTP 200 with `{"code":200,"data":...}`. Last call's data has `tenantSchema: "school_NNNNNN"`.

- [ ] **Step 2: Verify schema and tables in DB**

```bash
sshpass -p gcrf ssh t1@192.168.1.20 \
  "echo gcrf | sudo -S kubectl exec -n edu-infra postgresql-0 -- \
   psql -U postgres -d gcrf_main -c '\dt school_*.*' " | head -20
```

Expected output shows `school_NNNNNN.school_meta`, `school_NNNNNN.reader`, `school_NNNNNN.book_catalog`, etc.

- [ ] **Step 3: Browser end-to-end**

1. Open `http://192.168.1.19:31080/system/organizations`
2. Click 新增根节点 → 类型 REGION → name `天河` / code `th_e2e` → 确定
3. Click the new node → 添加子节点 → 类型 DISTRICT → name `石牌` / code `sp_e2e` → 确定
4. Click `sp_e2e` → 建学校 → name `实验小学` / code `syxx_e2e` → 建学校
5. The success toast shows `schema=school_NNNNNN`. The detail panel shows `tenant schema` tag.

- [ ] **Step 4: Tag release**

```bash
git tag -a v1.1.0-plan-A -m "Plan-A multi-tenant + org-service complete"
git push origin v1.1.0-plan-A
```

- [ ] **Step 5: Commit any remaining doc / config polish**

(only if there are residual changes to commit, otherwise skip)

```bash
git status --short
git commit -am "chore: post plan-A polish" || true
```

---

## Self-Review

### 1. Spec coverage check

| Spec section                             | Task                                                                              |
| ---------------------------------------- | --------------------------------------------------------------------------------- |
| §2.1 组织树最深 6 层                     | Task 10 (depth check), Task 12 (move)                                             |
| §2.1 ltree path                          | Task 7 (V001), Task 10 (path calc), Task 12 (subtree rewrite)                     |
| §2.1 节点类型字典                        | Task 7 (V002 seed)                                                                |
| §2.1 物化路径校验 path prefix            | covered via `path <@` in Mapper.findSubtree                                       |
| §3.1 多租户 schema 路由 (search_path)    | Task 1, 2, 3, 4                                                                   |
| §3.1 per-school migration 框架           | Task 5 (PerSchoolFlywayService), Task 14 (template), Task 15 (call)               |
| PRD §2.1 组织树 CRUD + 拖拽              | Task 13 (REST), Task 18 (UI: el-tree + add/delete; **drag is left out — Plan B**) |
| PRD §2.1 批量导入 Excel                  | Task 17                                                                           |
| ADR-001 schema_name regex 防注入         | Task 5 (`migrateSchool` regex)                                                    |
| Architecture §4.1 path prefix 中间件校验 | covered by SearchPathInterceptor + future TenantGuard (deferred to Plan-B IAM)    |

**Gap intentionally deferred:**

- 组织树拖拽位置 (UI)：Plan-A 暂不做，标为 Plan-B 候选（move API 已就绪）。
- 组织节点 → 用户授权 / data_scope 校验：依赖 IAM 升级，归 Plan-B。

### 2. Placeholder scan

Searched the plan for `TBD / TODO / FIXME / 待 / 未`：none found in step content. Stub `UnsupportedOperationException` lines in Task 9 are intentional and replaced in Tasks 10–12 — that's documented.

### 3. Type / signature consistency

| Symbol                                                  | Defined                       | Used                                                   |
| ------------------------------------------------------- | ----------------------------- | ------------------------------------------------------ |
| `TenantContext.resolveSearchPath()`                     | Task 1                        | Task 2                                                 |
| `TenantContext.setTenant(String)`                       | Task 1                        | Task 3, 4                                              |
| `JwtUtil.parseToken(String)`                            | existing in `common-security` | Task 3                                                 |
| `OrgNodeMapper.moveSubtree(oldPath, newPath)`           | Task 8                        | Task 12                                                |
| `OrgNodeService.create(dto)`                            | Task 9 (interface)            | Task 10 (impl), Task 13 (controller), Task 17 (import) |
| `OrgNodeService.move(id, newParentId)`                  | Task 9 (interface)            | Task 12 (impl), Task 13 (controller)                   |
| `SchoolProvisioningService.createSchool(dto)`           | Task 15                       | Task 16, frontend Task 18                              |
| `PerSchoolFlywayService.migrateSchool(schema)`          | Task 5                        | Task 15                                                |
| `OrgImportService.ImportReport.{created,failed,errors}` | Task 17                       | Task 17 controller, frontend Task 18                   |

All consistent.

### 4. Scope check

20 tasks, ~5 steps each, ~100 atomic actions. Estimated 2 weeks for one full-stack engineer (or ~1 week with subagent-driven parallelism). Single deliverable: provisioning a school end-to-end with schema isolation. ✅ Plan-sized.

---

## Execution Handoff

Plan complete and saved to `docs/plans/2026-04-30-plan-A-multitenant-org-service.md`. Two execution options:

**1. Subagent-Driven (recommended)** — fresh subagent per task with two-stage review (spec compliance → code quality). Same session, fast iteration, ~1 week wall time with parallelism.

**2. Inline Execution** — execute tasks in this session using executing-plans, batch with checkpoints. Heavier on context but simpler.

**Which approach?**
