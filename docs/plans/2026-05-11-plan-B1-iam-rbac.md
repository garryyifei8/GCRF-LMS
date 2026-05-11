# Plan-B1: 核心 IAM + RBAC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 auth-service 从单校用户名密码登录升级为支撑多平台、多租户的 IAM，落地 JWT 富化（tenant/roles/scope）、10 系统角色 + 11 模块权限 RBAC、refresh token 旋转、用户表中心化到 gcrf_region；副产品拆掉 system→auth Feign 链路。

**Architecture:** common-security 提供 SecurityContextFilter + 注解 + Helper（所有业务服务统一接入）；auth-service 原地升级，data source 切到 gcrf_region；Plan-A 的 TenantContextFilter 由 JWT.tenant claim 真正驱动；refresh token 存 Redis 30d；M1 仅 REGION_ADMIN 单向分配角色。

**Tech Stack:** Spring Boot 3.2.2 / Java 21 / Spring Security 6 / jjwt 0.12 / MyBatis-Plus / PostgreSQL 15 + ltree + dblink / Redis（refresh token + 权限缓存）/ Vue 3 + Element Plus

**Spec：** [`docs/specs/2026-05-09-plan-B1-iam-rbac-design.md`](../specs/2026-05-09-plan-B1-iam-rbac-design.md)

**周期估计：** 16 task / 1.5-2 周

**升级序列（每两 task 间生产 100% 工作）：**

```
Phase 1: common-security 打底（本地，未发布） — Task 1-4
Phase 2: gcrf_region schema 准备 — Task 5
Phase 3: auth-service 升级（先切库，再上 IAM 端点） — Task 6-10
Phase 4: 业务服务统一接入 + 前端 — Task 11-12
Phase 5: 拆 Feign + 部署 + 收口 — Task 13-16
```

---

## File Structure

### 新建文件

| 文件                                                                                                                                 | 责任                                                        |
| ------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------- |
| `backend/common/common-security/src/main/java/com/gcrf/library/common/security/context/SecurityContext.java`                         | 不可变 POJO：user_id / tenant / roles / scope / orgPath     |
| `backend/common/common-security/src/main/java/com/gcrf/library/common/security/context/SecurityContextHolder.java`                   | ThreadLocal 容器 + 静态 accessor                            |
| `backend/common/common-security/src/main/java/com/gcrf/library/common/security/filter/SecurityContextFilter.java`                    | OncePerRequestFilter：JWT → ThreadLocal                     |
| `backend/common/common-security/src/main/java/com/gcrf/library/common/security/annotation/RequireRole.java`                          | 注解                                                        |
| `backend/common/common-security/src/main/java/com/gcrf/library/common/security/annotation/RequirePermission.java`                    | 注解                                                        |
| `backend/common/common-security/src/main/java/com/gcrf/library/common/security/annotation/RequireScope.java`                         | 注解                                                        |
| `backend/common/common-security/src/main/java/com/gcrf/library/common/security/aspect/SecurityRequirementAspect.java`                | AspectJ 拦截三注解，写 SecurityContext 校验                 |
| `backend/common/common-security/src/main/java/com/gcrf/library/common/security/permission/PermissionLookup.java`                     | 接口：查 user_id → permissions（业务服务实现）              |
| `backend/common/common-security/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | 注册自动装配                                                |
| `backend/common/common-security/src/main/java/com/gcrf/library/common/security/config/SecurityContextAutoConfiguration.java`         | 装配 filter + aspect                                        |
| `backend/auth-service/src/main/resources/db/migration/region/V001__iam_baseline.sql`                                                 | gcrf_region.users + 4 RBAC 表 + seed                        |
| `backend/auth-service/src/main/java/com/gcrf/library/auth/entity/Role.java`                                                          | Entity                                                      |
| `backend/auth-service/src/main/java/com/gcrf/library/auth/entity/Permission.java`                                                    | Entity                                                      |
| `backend/auth-service/src/main/java/com/gcrf/library/auth/entity/UserRole.java`                                                      | Entity                                                      |
| `backend/auth-service/src/main/java/com/gcrf/library/auth/mapper/RoleMapper.java`                                                    | Mapper                                                      |
| `backend/auth-service/src/main/java/com/gcrf/library/auth/mapper/PermissionMapper.java`                                              | Mapper                                                      |
| `backend/auth-service/src/main/java/com/gcrf/library/auth/mapper/UserRoleMapper.java`                                                | Mapper                                                      |
| `backend/auth-service/src/main/java/com/gcrf/library/auth/service/RoleService.java`                                                  | 查角色 + 用户角色管理                                       |
| `backend/auth-service/src/main/java/com/gcrf/library/auth/service/PermissionService.java`                                            | 查权限                                                      |
| `backend/auth-service/src/main/java/com/gcrf/library/auth/service/RefreshTokenService.java`                                          | Redis 存/查/删 refresh                                      |
| `backend/auth-service/src/main/java/com/gcrf/library/auth/service/AuthPermissionLookup.java`                                         | 实现 common-security.PermissionLookup                       |
| `backend/auth-service/src/main/java/com/gcrf/library/auth/controller/RoleController.java`                                            | `/api/v1/roles`, `/api/v1/permissions`                      |
| `backend/auth-service/src/main/java/com/gcrf/library/auth/controller/UserRoleController.java`                                        | `/api/v1/users/{id}/roles`                                  |
| `backend/auth-service/src/main/java/com/gcrf/library/auth/dto/*`                                                                     | RoleVO/PermissionVO/AssignRoleRequest/EnrichedLoginResponse |
| `scripts/migrate-auth-users-to-region.sql`                                                                                           | 一次性 dblink 数据迁移 SQL                                  |
| `web-admin/src/api/role.js`                                                                                                          | API client                                                  |
| `web-admin/src/views/system/roles.vue`                                                                                               | 角色管理页                                                  |
| `web-admin/src/views/system/UserRoleDrawer.vue`                                                                                      | 用户角色分配抽屉组件                                        |

### 修改文件

| 文件                                                                                            | 修改                                                    |
| ----------------------------------------------------------------------------------------------- | ------------------------------------------------------- |
| `backend/common/common-security/src/main/java/com/gcrf/library/common/utils/JwtUtil.java`       | 密钥强制 ≥512 bits + 富 claim 工具方法                  |
| `backend/auth-service/src/main/resources/application.yml`                                       | datasource → gcrf_region；Flyway schema/table 隔离      |
| `backend/auth-service/src/main/resources/application-prod.yml`                                  | 同上                                                    |
| `backend/auth-service/src/main/java/com/gcrf/library/auth/entity/User.java`                     | 加 org_node_id / school_id / tenant_schema 字段         |
| `backend/auth-service/src/main/java/com/gcrf/library/auth/service/AuthService.java`             | login/refreshToken 富化；refresh 走 RefreshTokenService |
| `backend/auth-service/src/main/java/com/gcrf/library/auth/controller/AuthController.java`       | `/auth/me` 端点                                         |
| `backend/auth-service/src/main/java/com/gcrf/library/auth/dto/LoginResponse.java`               | 加 roles/tenant/scope/permissions 字段                  |
| `backend/auth-service/pom.xml`                                                                  | spring-boot-starter-aop + 已有 redisson                 |
| `backend/system-service/src/main/java/com/gcrf/library/system/controller/UserController.java`   | 拆 Feign，改本地 mapper                                 |
| `backend/system-service/src/main/java/com/gcrf/library/system/client/UserManagementClient.java` | **删除**（拆 Feign 后无引用）                           |
| `backend/system-service/pom.xml`                                                                | 删 common-feign 依赖（昨日加的，拆完不需要）            |
| `deployment/k8s/10-services.yaml`                                                               | gcrf-auth datasource URL → gcrf_main；JWT_SECRET env    |
| `web-admin/src/router/index.js`                                                                 | 加 /system/roles 路由                                   |
| `web-admin/src/views/system/users.vue`                                                          | "角色" 列 + 抽屉按钮                                    |

---

## Tasks

### Task 1: JwtUtil 密钥强度修复 + 富 claim 工具方法

修当前 400-bit 密钥告警；JwtUtil 增加生成富 claim 的便利方法（subject/userId/username/tenant/roles/scope 一次性塞入）。

**Files:**

- Modify: `backend/common/common-security/src/main/java/com/gcrf/library/common/utils/JwtUtil.java`
- Modify: `backend/common/common-security/src/test/java/com/gcrf/library/common/utils/JwtUtilTest.java`

- [ ] **Step 1: Write failing test for key length + new builder method**

Append to `JwtUtilTest.java`:

```java
@Test
void testKeyStrength_atLeast512Bits() {
    // 当 secret 长度 < 64 字节（512 bits）时应抛错或自动升级
    JwtUtil util = new JwtUtil();
    ReflectionTestUtils.setField(util, "secret", "too-short-secret-key");  // ~20 chars = 160 bits
    ReflectionTestUtils.setField(util, "expiration", 7200000L);
    assertThrows(IllegalStateException.class,
        () -> util.generateToken("user", Map.of()),
        "Expected IllegalStateException when secret < 64 bytes");
}

@Test
void testGenerateRichToken_carriesAllClaims() {
    Map<String, Object> claims = Map.of(
        "userId", 42L,
        "username", "alice",
        "tenant", "school_000001",
        "tenantId", 1L,
        "roles", java.util.List.of("LIBRARIAN", "TEACHER"),
        "scope", "SCHOOL"
    );
    String token = jwtUtil.generateToken("42", claims);

    Claims parsed = jwtUtil.parseToken(token);
    assertEquals("42", parsed.getSubject());
    assertEquals(42, ((Number) parsed.get("userId")).longValue());
    assertEquals("school_000001", parsed.get("tenant", String.class));
    assertEquals("SCHOOL", parsed.get("scope", String.class));
    @SuppressWarnings("unchecked")
    java.util.List<String> roles = parsed.get("roles", java.util.List.class);
    assertTrue(roles.contains("LIBRARIAN"));
}
```

- [ ] **Step 2: Run test to verify failure**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
mvn test -pl common/common-security -Dtest=JwtUtilTest#testKeyStrength_atLeast512Bits -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL — 当前实现接受任意短 secret（io.jsonwebtoken 在 sign 时才报 WeakKey），需要主动 fail-fast。

- [ ] **Step 3: Implement key length guard in JwtUtil**

In `JwtUtil.java`, replace the `getSignKey()` private method (or add it if missing) and update default secret in `@Value`:

```java
@Value("${jwt.secret:gcrf-library-iam-default-development-secret-key-do-not-use-in-production-2026}")
private String secret;

@Value("${jwt.expiration:1800000}")  // 30 min (was 2h)
private Long expiration;

private static final int MIN_SECRET_BYTES = 64;  // 512 bits for HS512

private SecretKey getSignKey() {
    byte[] keyBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    if (keyBytes.length < MIN_SECRET_BYTES) {
        throw new IllegalStateException(
            "jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes (512 bits) for HS512, got "
            + keyBytes.length + " bytes. Configure spring property `jwt.secret` with a longer value.");
    }
    return Keys.hmacShaKeyFor(keyBytes);
}
```

Note: default secret is 80 chars (640 bits) so dev profile passes; prod **must** set `JWT_SECRET` env to ≥64 chars.

- [ ] **Step 4: Run test to verify pass**

```bash
mvn test -pl common/common-security -Dtest=JwtUtilTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: all JwtUtilTest tests pass (10+ pre-existing + 2 new).

- [ ] **Step 5: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/common/common-security/src/main/java/com/gcrf/library/common/utils/JwtUtil.java \
        backend/common/common-security/src/test/java/com/gcrf/library/common/utils/JwtUtilTest.java
git commit -m "feat(common): enforce JWT secret ≥512 bits and shorten default TTL to 30min"
```

---

### Task 2: SecurityContext POJO + SecurityContextHolder ThreadLocal

不可变上下文 POJO + ThreadLocal 容器 + 静态 accessor。

**Files:**

- Create: `backend/common/common-security/src/main/java/com/gcrf/library/common/security/context/SecurityContext.java`
- Create: `backend/common/common-security/src/main/java/com/gcrf/library/common/security/context/SecurityContextHolder.java`
- Create: `backend/common/common-security/src/main/java/com/gcrf/library/common/security/context/Scope.java`
- Create: `backend/common/common-security/src/test/java/com/gcrf/library/common/security/context/SecurityContextHolderTest.java`

- [ ] **Step 1: Write failing test**

Create `SecurityContextHolderTest.java`:

```java
package com.gcrf.library.common.security.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SecurityContextHolderTest {

    @AfterEach
    void cleanup() { SecurityContextHolder.clear(); }

    @Test
    void emptyByDefault() {
        assertTrue(SecurityContextHolder.current().isEmpty());
        assertNull(SecurityContextHolder.currentUserId());
        assertNull(SecurityContextHolder.currentTenant());
        assertEquals(List.of(), SecurityContextHolder.currentRoles());
        assertFalse(SecurityContextHolder.hasRole("REGION_ADMIN"));
        assertEquals(Scope.SELF, SecurityContextHolder.currentScope());
    }

    @Test
    void setAndRead() {
        SecurityContext ctx = SecurityContext.builder()
            .userId(42L)
            .username("alice")
            .tenant("school_000001")
            .tenantId(1L)
            .roles(List.of("LIBRARIAN", "TEACHER"))
            .scope(Scope.SCHOOL)
            .orgPath("/100/200/305/")
            .build();
        SecurityContextHolder.set(ctx);

        assertEquals(42L, SecurityContextHolder.currentUserId());
        assertEquals("school_000001", SecurityContextHolder.currentTenant());
        assertEquals(Scope.SCHOOL, SecurityContextHolder.currentScope());
        assertTrue(SecurityContextHolder.hasRole("LIBRARIAN"));
        assertFalse(SecurityContextHolder.hasRole("REGION_ADMIN"));
        assertTrue(SecurityContextHolder.hasScope(Scope.SCHOOL));
        assertTrue(SecurityContextHolder.hasScope(Scope.SELF));      // SCHOOL ⊇ SELF
        assertFalse(SecurityContextHolder.hasScope(Scope.REGION));   // SCHOOL ⊉ REGION
        assertEquals("/100/200/305/", SecurityContextHolder.currentOrgPath());
    }

    @Test
    void clearWipesContext() {
        SecurityContextHolder.set(SecurityContext.builder().userId(1L).build());
        SecurityContextHolder.clear();
        assertTrue(SecurityContextHolder.current().isEmpty());
    }

    @Test
    void scopeHierarchy_regionContainsAll() {
        SecurityContextHolder.set(SecurityContext.builder()
            .scope(Scope.REGION).build());
        assertTrue(SecurityContextHolder.hasScope(Scope.REGION));
        assertTrue(SecurityContextHolder.hasScope(Scope.SCHOOL));
        assertTrue(SecurityContextHolder.hasScope(Scope.GRADE));
        assertTrue(SecurityContextHolder.hasScope(Scope.CLASS));
        assertTrue(SecurityContextHolder.hasScope(Scope.SELF));
    }
}
```

- [ ] **Step 2: Run failing**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
mvn test -pl common/common-security -Dtest=SecurityContextHolderTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: COMPILATION FAILURE — types not defined.

- [ ] **Step 3: Implement Scope enum**

```java
// SecurityContext: data scope levels.
// Ordinals matter: higher ordinal subsumes lower (REGION ⊇ SCHOOL ⊇ GRADE ⊇ CLASS ⊇ SELF).
package com.gcrf.library.common.security.context;

public enum Scope {
    SELF,
    CLASS,
    GRADE,
    SCHOOL,
    REGION;

    public boolean covers(Scope other) {
        return this.ordinal() >= other.ordinal();
    }
}
```

- [ ] **Step 4: Implement SecurityContext (immutable Lombok @Builder)**

```java
package com.gcrf.library.common.security.context;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SecurityContext {
    Long userId;
    String username;
    String tenant;       // school_000001 or null for region-level
    Long tenantId;
    @Builder.Default
    List<String> roles = List.of();
    @Builder.Default
    Scope scope = Scope.SELF;
    String orgPath;      // ltree '/100/200/305/' or null
}
```

- [ ] **Step 5: Implement SecurityContextHolder**

```java
package com.gcrf.library.common.security.context;

import java.util.List;
import java.util.Optional;

public final class SecurityContextHolder {

    private static final ThreadLocal<SecurityContext> CURRENT = new ThreadLocal<>();

    private static final SecurityContext EMPTY = SecurityContext.builder().build();

    private SecurityContextHolder() {}

    public static void set(SecurityContext ctx) { CURRENT.set(ctx); }
    public static void clear() { CURRENT.remove(); }

    public static Optional<SecurityContext> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    private static SecurityContext currentOrEmpty() {
        SecurityContext c = CURRENT.get();
        return c == null ? EMPTY : c;
    }

    public static Long currentUserId()      { return currentOrEmpty().getUserId(); }
    public static String currentUsername()  { return currentOrEmpty().getUsername(); }
    public static String currentTenant()    { return currentOrEmpty().getTenant(); }
    public static Long currentTenantId()    { return currentOrEmpty().getTenantId(); }
    public static List<String> currentRoles() { return currentOrEmpty().getRoles(); }
    public static Scope currentScope()      { return currentOrEmpty().getScope(); }
    public static String currentOrgPath()   { return currentOrEmpty().getOrgPath(); }

    public static boolean hasRole(String code) {
        return currentOrEmpty().getRoles().contains(code);
    }

    public static boolean hasScope(Scope required) {
        return currentOrEmpty().getScope().covers(required);
    }
}
```

- [ ] **Step 6: Run test to verify pass**

```bash
mvn test -pl common/common-security -Dtest=SecurityContextHolderTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: 4 tests pass.

- [ ] **Step 7: Commit**

```bash
git add backend/common/common-security/src/main/java/com/gcrf/library/common/security/context/ \
        backend/common/common-security/src/test/java/com/gcrf/library/common/security/context/
git commit -m "feat(common): add SecurityContext + SecurityContextHolder ThreadLocal"
```

---

### Task 3: SecurityContextFilter (JWT → ThreadLocal) + AutoConfiguration

OncePerRequestFilter，从 `Authorization: Bearer` 解析 JWT，富化的 claims 写 ThreadLocal；请求完后 clear。

**Files:**

- Create: `backend/common/common-security/src/main/java/com/gcrf/library/common/security/filter/SecurityContextFilter.java`
- Create: `backend/common/common-security/src/main/java/com/gcrf/library/common/security/config/SecurityContextAutoConfiguration.java`
- Create: `backend/common/common-security/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `backend/common/common-security/src/test/java/com/gcrf/library/common/security/filter/SecurityContextFilterTest.java`

- [ ] **Step 1: Write failing integration test**

Create `SecurityContextFilterTest.java`:

```java
package com.gcrf.library.common.security.filter;

import com.gcrf.library.common.security.context.Scope;
import com.gcrf.library.common.security.context.SecurityContext;
import com.gcrf.library.common.security.context.SecurityContextHolder;
import com.gcrf.library.common.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SecurityContextFilterTest {

    private JwtUtil jwtUtil;
    private SecurityContextFilter filter;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
            "test-secret-must-be-at-least-64-bytes-long-to-satisfy-hs512-requirement-2026!!");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 1800000L);
        filter = new SecurityContextFilter(jwtUtil);
        SecurityContextHolder.clear();
    }

    @Test
    void parsesRichJwtIntoContext() throws Exception {
        String token = jwtUtil.generateToken("42", Map.of(
            "userId", 42L,
            "username", "alice",
            "tenant", "school_000001",
            "tenantId", 1L,
            "roles", List.of("LIBRARIAN"),
            "scope", "SCHOOL",
            "orgPath", "/100/200/305/"
        ));

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = (request, response) -> {
            // Inside the chain, context should be populated.
            assertEquals(42L, SecurityContextHolder.currentUserId());
            assertEquals("school_000001", SecurityContextHolder.currentTenant());
            assertEquals(Scope.SCHOOL, SecurityContextHolder.currentScope());
            assertTrue(SecurityContextHolder.hasRole("LIBRARIAN"));
            assertEquals("/100/200/305/", SecurityContextHolder.currentOrgPath());
        };

        filter.doFilter(req, res, chain);

        // After chain, context must be cleared.
        assertTrue(SecurityContextHolder.current().isEmpty());
    }

    @Test
    void noAuthHeader_leavesContextEmpty() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, new MockFilterChain());

        assertTrue(SecurityContextHolder.current().isEmpty());
    }

    @Test
    void malformedToken_leavesContextEmptyAndDoesNotFail() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer not.a.valid.jwt.token");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, new MockFilterChain());

        // 200 chain still ran (auth enforced elsewhere if needed)
        assertEquals(HttpServletResponse.SC_OK, res.getStatus());
        assertTrue(SecurityContextHolder.current().isEmpty());
    }
}
```

- [ ] **Step 2: Run failing**

```bash
mvn test -pl common/common-security -Dtest=SecurityContextFilterTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: COMPILATION FAILURE — `SecurityContextFilter` not found.

- [ ] **Step 3: Implement SecurityContextFilter**

```java
package com.gcrf.library.common.security.filter;

import com.gcrf.library.common.security.context.Scope;
import com.gcrf.library.common.security.context.SecurityContext;
import com.gcrf.library.common.security.context.SecurityContextHolder;
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
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class SecurityContextFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = req.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                try {
                    Claims claims = jwtUtil.parseToken(token);
                    SecurityContextHolder.set(toContext(claims));
                } catch (Exception ex) {
                    // Don't fail the request here; downstream auth filters / @RequireRole will deny.
                    log.debug("SecurityContextFilter: invalid token, ignoring. {}", ex.getMessage());
                }
            }
            chain.doFilter(req, res);
        } finally {
            SecurityContextHolder.clear();
        }
    }

    private static SecurityContext toContext(Claims c) {
        Long userId = optLong(c, "userId");
        Long tenantId = optLong(c, "tenantId");
        Object rolesObj = c.get("roles");
        @SuppressWarnings("unchecked")
        List<String> roles = rolesObj instanceof List ? (List<String>) rolesObj : List.of();
        Scope scope;
        try {
            String s = c.get("scope", String.class);
            scope = s == null ? Scope.SELF : Scope.valueOf(s);
        } catch (IllegalArgumentException e) {
            scope = Scope.SELF;
        }
        return SecurityContext.builder()
            .userId(userId)
            .username(c.get("username", String.class))
            .tenant(c.get("tenant", String.class))
            .tenantId(tenantId)
            .roles(roles)
            .scope(scope)
            .orgPath(c.get("orgPath", String.class))
            .build();
    }

    private static Long optLong(Claims c, String key) {
        Object v = c.get(key);
        return v == null ? null : ((Number) v).longValue();
    }
}
```

- [ ] **Step 4: Create AutoConfiguration**

```java
// SecurityContextAutoConfiguration.java
package com.gcrf.library.common.security.config;

import com.gcrf.library.common.security.filter.SecurityContextFilter;
import com.gcrf.library.common.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@Slf4j
@AutoConfiguration
@ConditionalOnClass(JwtUtil.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityContextAutoConfiguration {

    @Bean
    public FilterRegistrationBean<SecurityContextFilter> securityContextFilter(JwtUtil jwtUtil) {
        FilterRegistrationBean<SecurityContextFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new SecurityContextFilter(jwtUtil));
        // Run BEFORE TenantContextFilter so its SecurityContext is available when tenant context is computed.
        // TenantContextFilter currently uses HIGHEST_PRECEDENCE + 100; we use HIGHEST_PRECEDENCE + 50.
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 50);
        reg.addUrlPatterns("/*");
        log.info("SecurityContextFilter registered (order={})", reg.getOrder());
        return reg;
    }
}
```

- [ ] **Step 5: Register auto-config**

Create `backend/common/common-security/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
com.gcrf.library.common.security.config.CommonSecurityConfig
com.gcrf.library.common.security.config.SecurityContextAutoConfiguration
```

- [ ] **Step 6: Run test to verify pass**

```bash
mvn test -pl common/common-security -Dtest=SecurityContextFilterTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: 3 tests pass.

- [ ] **Step 7: Commit**

```bash
git add backend/common/common-security/src/main/java/com/gcrf/library/common/security/filter/ \
        backend/common/common-security/src/main/java/com/gcrf/library/common/security/config/SecurityContextAutoConfiguration.java \
        backend/common/common-security/src/main/resources/META-INF/spring/ \
        backend/common/common-security/src/test/java/com/gcrf/library/common/security/filter/
git commit -m "feat(common): add SecurityContextFilter and auto-configuration"
```

---

### Task 4: @RequireRole / @RequirePermission / @RequireScope 注解 + AspectJ

注解 + AspectJ aspect 拦截方法调用，校验 ThreadLocal 上下文；不满足抛 403。

**Files:**

- Create: `backend/common/common-security/src/main/java/com/gcrf/library/common/security/annotation/RequireRole.java`
- Create: `.../annotation/RequirePermission.java`
- Create: `.../annotation/RequireScope.java`
- Create: `.../permission/PermissionLookup.java`
- Create: `.../aspect/SecurityRequirementAspect.java`
- Create: `.../exception/AccessDeniedException.java`
- Create: `.../config/SecurityRequirementAutoConfiguration.java`
- Modify: `backend/common/common-security/pom.xml` — 加 `spring-boot-starter-aop`
- Create: `.../aspect/SecurityRequirementAspectTest.java`

- [ ] **Step 1: Write failing test**

```java
// SecurityRequirementAspectTest.java
package com.gcrf.library.common.security.aspect;

import com.gcrf.library.common.security.annotation.RequirePermission;
import com.gcrf.library.common.security.annotation.RequireRole;
import com.gcrf.library.common.security.annotation.RequireScope;
import com.gcrf.library.common.security.context.Scope;
import com.gcrf.library.common.security.context.SecurityContext;
import com.gcrf.library.common.security.context.SecurityContextHolder;
import com.gcrf.library.common.security.exception.AccessDeniedException;
import com.gcrf.library.common.security.permission.PermissionLookup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SecurityRequirementAspectTest {

    static class FakePermissionLookup implements PermissionLookup {
        @Override public Set<String> lookup(Long userId) {
            return userId == 42L ? Set.of("book.read", "book.write") : Set.of();
        }
    }

    static class Target {
        @RequireRole("REGION_ADMIN")
        public String regionOnly() { return "ok"; }

        @RequirePermission("book.write")
        public String writeBook() { return "ok"; }

        @RequireScope(Scope.REGION)
        public String regionScope() { return "ok"; }
    }

    private Target proxy;

    @BeforeEach
    void setUp() {
        AspectJProxyFactory f = new AspectJProxyFactory(new Target());
        f.addAspect(new SecurityRequirementAspect(new FakePermissionLookup()));
        proxy = f.getProxy();
        SecurityContextHolder.clear();
    }
    @AfterEach
    void cleanup() { SecurityContextHolder.clear(); }

    @Test
    void requireRole_allowsWhenRolePresent() {
        SecurityContextHolder.set(SecurityContext.builder()
            .userId(1L).roles(List.of("REGION_ADMIN")).scope(Scope.REGION).build());
        assertEquals("ok", proxy.regionOnly());
    }

    @Test
    void requireRole_denies403WhenMissing() {
        SecurityContextHolder.set(SecurityContext.builder()
            .userId(1L).roles(List.of("LIBRARIAN")).scope(Scope.SCHOOL).build());
        assertThrows(AccessDeniedException.class, () -> proxy.regionOnly());
    }

    @Test
    void requirePermission_lookupsAndAllows() {
        SecurityContextHolder.set(SecurityContext.builder().userId(42L).build());
        assertEquals("ok", proxy.writeBook());
    }

    @Test
    void requirePermission_denies() {
        SecurityContextHolder.set(SecurityContext.builder().userId(99L).build());
        assertThrows(AccessDeniedException.class, () -> proxy.writeBook());
    }

    @Test
    void requireScope_allowsWhenCovered() {
        SecurityContextHolder.set(SecurityContext.builder()
            .userId(1L).scope(Scope.REGION).build());
        assertEquals("ok", proxy.regionScope());
    }

    @Test
    void requireScope_denies() {
        SecurityContextHolder.set(SecurityContext.builder()
            .userId(1L).scope(Scope.SCHOOL).build());
        assertThrows(AccessDeniedException.class, () -> proxy.regionScope());
    }
}
```

- [ ] **Step 2: Run failing**

```bash
mvn test -pl common/common-security -Dtest=SecurityRequirementAspectTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: COMPILATION FAILURE.

- [ ] **Step 3: Add AOP dependency**

In `backend/common/common-security/pom.xml`, add inside `<dependencies>`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

- [ ] **Step 4: Define annotations**

```java
// RequireRole.java
package com.gcrf.library.common.security.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    /** 满足任一即可（OR 语义）。 */
    String[] value();
}
```

```java
// RequirePermission.java
package com.gcrf.library.common.security.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    /** 例如 'book.write'。 */
    String value();
}
```

```java
// RequireScope.java
package com.gcrf.library.common.security.annotation;

import com.gcrf.library.common.security.context.Scope;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireScope {
    Scope value();
}
```

- [ ] **Step 5: Define PermissionLookup interface + AccessDeniedException**

```java
// PermissionLookup.java
package com.gcrf.library.common.security.permission;

import java.util.Set;

/**
 * 业务服务自己实现，命中时返回该 user 拥有的 permission code 集合（含缓存）。
 */
public interface PermissionLookup {
    Set<String> lookup(Long userId);
}
```

```java
// AccessDeniedException.java
package com.gcrf.library.common.security.exception;

import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.ResultCode;

public class AccessDeniedException extends BusinessException {
    public AccessDeniedException(String msg) {
        super(ResultCode.FORBIDDEN.getCode(), msg);
    }
}
```

Note: this reuses existing `ResultCode.FORBIDDEN` (status 403) from common-core. If not present, fall back to `super(403, msg)`.

- [ ] **Step 6: Implement aspect**

```java
package com.gcrf.library.common.security.aspect;

import com.gcrf.library.common.security.annotation.RequirePermission;
import com.gcrf.library.common.security.annotation.RequireRole;
import com.gcrf.library.common.security.annotation.RequireScope;
import com.gcrf.library.common.security.context.SecurityContextHolder;
import com.gcrf.library.common.security.exception.AccessDeniedException;
import com.gcrf.library.common.security.permission.PermissionLookup;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.Arrays;
import java.util.Set;

@Aspect
@RequiredArgsConstructor
public class SecurityRequirementAspect {

    private final PermissionLookup permissionLookup;

    @Around("@annotation(requireRole) || @within(requireRole)")
    public Object aroundRole(ProceedingJoinPoint pjp, RequireRole requireRole) throws Throwable {
        if (Arrays.stream(requireRole.value()).noneMatch(SecurityContextHolder::hasRole)) {
            throw new AccessDeniedException(
                "需要角色: " + String.join("/", requireRole.value()));
        }
        return pjp.proceed();
    }

    @Around("@annotation(requirePermission) || @within(requirePermission)")
    public Object aroundPermission(ProceedingJoinPoint pjp, RequirePermission requirePermission) throws Throwable {
        Long uid = SecurityContextHolder.currentUserId();
        if (uid == null) {
            throw new AccessDeniedException("未认证");
        }
        Set<String> perms = permissionLookup.lookup(uid);
        if (!perms.contains(requirePermission.value())) {
            throw new AccessDeniedException("缺少权限: " + requirePermission.value());
        }
        return pjp.proceed();
    }

    @Around("@annotation(requireScope) || @within(requireScope)")
    public Object aroundScope(ProceedingJoinPoint pjp, RequireScope requireScope) throws Throwable {
        if (!SecurityContextHolder.hasScope(requireScope.value())) {
            throw new AccessDeniedException("权限范围不足: 需要 " + requireScope.value());
        }
        return pjp.proceed();
    }
}
```

- [ ] **Step 7: AutoConfiguration for the aspect**

```java
// SecurityRequirementAutoConfiguration.java
package com.gcrf.library.common.security.config;

import com.gcrf.library.common.security.aspect.SecurityRequirementAspect;
import com.gcrf.library.common.security.permission.PermissionLookup;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@AutoConfiguration
@EnableAspectJAutoProxy
public class SecurityRequirementAutoConfiguration {

    /** 仅当业务服务提供了 PermissionLookup 实现时启用 aspect（permit 注解可不查 DB）。 */
    @Bean
    @ConditionalOnBean(PermissionLookup.class)
    public SecurityRequirementAspect securityRequirementAspect(PermissionLookup lookup) {
        return new SecurityRequirementAspect(lookup);
    }
}
```

Append to `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
com.gcrf.library.common.security.config.SecurityRequirementAutoConfiguration
```

- [ ] **Step 8: Run test**

```bash
mvn test -pl common/common-security -Dtest=SecurityRequirementAspectTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: 6 tests pass.

- [ ] **Step 9: Commit**

```bash
git add backend/common/common-security/pom.xml \
        backend/common/common-security/src/main/java/com/gcrf/library/common/security/annotation/ \
        backend/common/common-security/src/main/java/com/gcrf/library/common/security/permission/ \
        backend/common/common-security/src/main/java/com/gcrf/library/common/security/exception/ \
        backend/common/common-security/src/main/java/com/gcrf/library/common/security/aspect/ \
        backend/common/common-security/src/main/java/com/gcrf/library/common/security/config/SecurityRequirementAutoConfiguration.java \
        backend/common/common-security/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports \
        backend/common/common-security/src/test/java/com/gcrf/library/common/security/aspect/
git commit -m "feat(common): add @RequireRole/@RequirePermission/@RequireScope annotations + AOP aspect"
```

---

### Task 5: Flyway V001 — gcrf_region.users + 4 RBAC 表 + seed

新建 `db/migration/region/V001__iam_baseline.sql`，建 5 张表 + seed 10 角色 + 11 权限 + role-permission 映射 + admin 用户 + admin → REGION_ADMIN 绑定。同时配置 auth-service 用独立的 `flyway_schema_history_auth`（遵循 ADR-005）。

**Files:**

- Create: `backend/auth-service/src/main/resources/db/migration/region/V001__iam_baseline.sql`
- Modify: `backend/auth-service/src/main/resources/application.yml` — Flyway config + datasource locations
- (auth-service 真正切换 datasource 在 Task 6，本 task 只是把 migration 文件就位 + 配置就绪)

- [ ] **Step 1: Write V001 SQL**

```sql
-- backend/auth-service/src/main/resources/db/migration/region/V001__iam_baseline.sql
-- gcrf_region.users + auth_role / auth_permission / auth_role_permission / auth_user_role + seed
-- Owned by auth-service. Other services using gcrf_region (org-service, opac-service) have their own
-- flyway_schema_history_* per ADR-005.

-- ============ users (从 auth_service.users 迁移而来 + 扩展) ============

CREATE TABLE IF NOT EXISTS gcrf_region.users (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            VARCHAR(50) NOT NULL UNIQUE,
    username           VARCHAR(100) NOT NULL,
    password           VARCHAR(255) NOT NULL,
    email              VARCHAR(100),
    phone              VARCHAR(20),
    user_type          VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    avatar_url         VARCHAR(500),
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_time    TIMESTAMPTZ,
    last_login_ip      VARCHAR(50),
    failed_login_count INT NOT NULL DEFAULT 0,
    locked_until       TIMESTAMPTZ,
    org_node_id        BIGINT,
    school_id          BIGINT,
    tenant_schema      VARCHAR(64),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at         TIMESTAMPTZ
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email_alive
    ON gcrf_region.users (email) WHERE deleted_at IS NULL AND email IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_org_node  ON gcrf_region.users (org_node_id);
CREATE INDEX IF NOT EXISTS idx_users_school    ON gcrf_region.users (school_id);
CREATE INDEX IF NOT EXISTS idx_users_status    ON gcrf_region.users (status);
CREATE INDEX IF NOT EXISTS idx_users_user_type ON gcrf_region.users (user_type);

-- ============ auth_role ============

CREATE TABLE IF NOT EXISTS gcrf_region.auth_role (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(50) NOT NULL UNIQUE,
    name          VARCHAR(100) NOT NULL,
    description   TEXT,
    scope_default VARCHAR(20) NOT NULL,
    is_system     BOOLEAN NOT NULL DEFAULT false,
    school_id     BIGINT,
    sort_order    INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_role_school ON gcrf_region.auth_role (school_id);

INSERT INTO gcrf_region.auth_role (code, name, description, scope_default, is_system, sort_order) VALUES
    ('REGION_ADMIN',        '区域超管',     '教育局全权',               'REGION', true, 10),
    ('REGION_LIBRARIAN',    '区域馆员',     '区域读取 + 馆藏标准维护',   'REGION', true, 20),
    ('SCHOOL_ADMIN',        '学校管理员',   '本校全权',                  'SCHOOL', true, 30),
    ('SCHOOL_LIBRARY_HEAD', '学校馆长',     '本校馆藏 + 用户 + 报表',    'SCHOOL', true, 40),
    ('LIBRARIAN',           '学校馆员',     '本校借还 + 编目',           'SCHOOL', true, 50),
    ('OPERATOR',            '操作员',       '借还工位',                  'SCHOOL', true, 60),
    ('TEACHER',             '教师',         '借阅 + 班级阅读报表',       'CLASS',  true, 70),
    ('STUDENT',             '学生',         '借阅 + 预约 + 测评',        'SELF',   true, 80),
    ('PARENT',              '家长',         '查阅子女阅读情况',          'SELF',   true, 90),
    ('GUEST',               '游客',         'OPAC 只读',                 'SELF',   true, 100)
ON CONFLICT (code) DO NOTHING;

-- ============ auth_permission ============

CREATE TABLE IF NOT EXISTS gcrf_region.auth_permission (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(100) NOT NULL UNIQUE,
    module      VARCHAR(50) NOT NULL,
    action      VARCHAR(50) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_perm_module ON gcrf_region.auth_permission (module);

INSERT INTO gcrf_region.auth_permission (code, module, action, name, sort_order) VALUES
    ('book.read',         'book',         'read',  '图书查询',  10),
    ('book.write',        'book',         'write', '图书编辑',  20),
    ('circulation.read',  'circulation',  'read',  '流通查询',  30),
    ('circulation.write', 'circulation',  'write', '借还操作',  40),
    ('reader.read',       'reader',       'read',  '读者查询',  50),
    ('reader.write',      'reader',       'write', '读者编辑',  60),
    ('system.read',       'system',       'read',  '系统查询',  70),
    ('system.write',      'system',       'write', '系统编辑',  80),
    ('analytics.read',    'analytics',    'read',  '报表查询',  90),
    ('org.read',          'org',          'read',  '组织查询', 100),
    ('org.write',         'org',          'write', '组织编辑', 110),
    ('opac.read',         'opac',         'read',  '检索访问', 120)
ON CONFLICT (code) DO NOTHING;

-- ============ auth_role_permission ============

CREATE TABLE IF NOT EXISTS gcrf_region.auth_role_permission (
    role_id       BIGINT NOT NULL REFERENCES gcrf_region.auth_role(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES gcrf_region.auth_permission(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Helper view for seeding (joins role/perm by code).
DO $$
DECLARE rid BIGINT;
BEGIN
    -- REGION_ADMIN: ALL
    SELECT id INTO rid FROM gcrf_region.auth_role WHERE code = 'REGION_ADMIN';
    INSERT INTO gcrf_region.auth_role_permission(role_id, permission_id)
        SELECT rid, p.id FROM gcrf_region.auth_permission p
        ON CONFLICT DO NOTHING;

    -- REGION_LIBRARIAN: book.read, analytics.read, org.read, opac.read
    SELECT id INTO rid FROM gcrf_region.auth_role WHERE code = 'REGION_LIBRARIAN';
    INSERT INTO gcrf_region.auth_role_permission(role_id, permission_id)
        SELECT rid, id FROM gcrf_region.auth_permission
         WHERE code IN ('book.read','analytics.read','org.read','opac.read')
        ON CONFLICT DO NOTHING;

    -- SCHOOL_ADMIN: book/circulation/reader/system .* + analytics.read + org.read
    SELECT id INTO rid FROM gcrf_region.auth_role WHERE code = 'SCHOOL_ADMIN';
    INSERT INTO gcrf_region.auth_role_permission(role_id, permission_id)
        SELECT rid, id FROM gcrf_region.auth_permission
         WHERE module IN ('book','circulation','reader','system')
            OR code IN ('analytics.read','org.read','opac.read')
        ON CONFLICT DO NOTHING;

    -- SCHOOL_LIBRARY_HEAD: book/circulation/reader.* + analytics.read + opac.read
    SELECT id INTO rid FROM gcrf_region.auth_role WHERE code = 'SCHOOL_LIBRARY_HEAD';
    INSERT INTO gcrf_region.auth_role_permission(role_id, permission_id)
        SELECT rid, id FROM gcrf_region.auth_permission
         WHERE module IN ('book','circulation','reader')
            OR code IN ('analytics.read','opac.read')
        ON CONFLICT DO NOTHING;

    -- LIBRARIAN: book/circulation/reader.* + opac.read
    SELECT id INTO rid FROM gcrf_region.auth_role WHERE code = 'LIBRARIAN';
    INSERT INTO gcrf_region.auth_role_permission(role_id, permission_id)
        SELECT rid, id FROM gcrf_region.auth_permission
         WHERE module IN ('book','circulation','reader') OR code = 'opac.read'
        ON CONFLICT DO NOTHING;

    -- OPERATOR: circulation.write + opac.read
    SELECT id INTO rid FROM gcrf_region.auth_role WHERE code = 'OPERATOR';
    INSERT INTO gcrf_region.auth_role_permission(role_id, permission_id)
        SELECT rid, id FROM gcrf_region.auth_permission
         WHERE code IN ('circulation.write','opac.read')
        ON CONFLICT DO NOTHING;

    -- TEACHER: book.read + analytics.read + opac.read
    SELECT id INTO rid FROM gcrf_region.auth_role WHERE code = 'TEACHER';
    INSERT INTO gcrf_region.auth_role_permission(role_id, permission_id)
        SELECT rid, id FROM gcrf_region.auth_permission
         WHERE code IN ('book.read','analytics.read','opac.read')
        ON CONFLICT DO NOTHING;

    -- STUDENT / PARENT / GUEST: opac.read (+ book.read for STUDENT/PARENT)
    FOR rid IN
        SELECT id FROM gcrf_region.auth_role WHERE code IN ('STUDENT','PARENT')
    LOOP
        INSERT INTO gcrf_region.auth_role_permission(role_id, permission_id)
            SELECT rid, id FROM gcrf_region.auth_permission
             WHERE code IN ('book.read','opac.read')
            ON CONFLICT DO NOTHING;
    END LOOP;

    SELECT id INTO rid FROM gcrf_region.auth_role WHERE code = 'GUEST';
    INSERT INTO gcrf_region.auth_role_permission(role_id, permission_id)
        SELECT rid, id FROM gcrf_region.auth_permission WHERE code = 'opac.read'
        ON CONFLICT DO NOTHING;
END $$;

-- ============ auth_user_role ============

CREATE TABLE IF NOT EXISTS gcrf_region.auth_user_role (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES gcrf_region.users(id) ON DELETE CASCADE,
    role_id        BIGINT NOT NULL REFERENCES gcrf_region.auth_role(id) ON DELETE CASCADE,
    school_id      BIGINT,
    scope_override VARCHAR(20),
    scope_path     VARCHAR(500),
    assigned_by    BIGINT,
    assigned_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at     TIMESTAMPTZ
);
-- PG 15+ supports NULLS NOT DISTINCT — region-level assignment (school_id=NULL) treated as duplicate.
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_role
    ON gcrf_region.auth_user_role (user_id, role_id, school_id) NULLS NOT DISTINCT;
CREATE INDEX IF NOT EXISTS idx_user_role_user   ON gcrf_region.auth_user_role (user_id);
CREATE INDEX IF NOT EXISTS idx_user_role_school ON gcrf_region.auth_user_role (school_id);

-- ============ admin user + role bootstrap ============
-- Password is admin123 BCrypt-hashed, identical to current auth_service.users seed.
INSERT INTO gcrf_region.users (user_id, username, password, email, user_type, status) VALUES
    ('admin', '系统管理员',
     '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
     'admin@gcrf.edu.cn', 'ADMIN', 'ACTIVE')
ON CONFLICT (user_id) DO NOTHING;

-- Bind admin → REGION_ADMIN (idempotent).
INSERT INTO gcrf_region.auth_user_role (user_id, role_id, assigned_by)
    SELECT u.id, r.id, u.id
      FROM gcrf_region.users u, gcrf_region.auth_role r
     WHERE u.user_id = 'admin' AND r.code = 'REGION_ADMIN'
ON CONFLICT DO NOTHING;
```

- [ ] **Step 2: Configure auth-service Flyway to use region/ + isolated history table**

Edit `backend/auth-service/src/main/resources/application.yml`. Replace the existing `flyway:` block:

```yaml
spring:
  # ... existing config ...
  flyway:
    enabled: true
    locations: classpath:db/migration/region
    schemas: gcrf_region
    create-schemas: true
    baseline-on-migrate: false
    table: flyway_schema_history_auth
```

Apply the same to `application-prod.yml` if it has a flyway override (delete the override; base config wins).

- [ ] **Step 3: Move the legacy V001\_\_baseline.sql out of the way (it targeted the old auth_service DB)**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git mv backend/auth-service/src/main/resources/db/migration/V001__baseline.sql \
       docs/archives/legacy-2026-04/auth-service-V001__baseline-OLD-auth_service-db.sql
```

Note: this legacy SQL stays in archives for reference; it never runs against `gcrf_region`.

- [ ] **Step 4: Verify SQL processes-resources correctly**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn process-resources -pl auth-service -q
ls -la auth-service/target/classes/db/migration/region/
```

Expected: `V001__iam_baseline.sql` present.

- [ ] **Step 5: Smoke-test V001 against a fresh testcontainer PG**

Write `backend/auth-service/src/test/java/com/gcrf/library/auth/migration/V001MigrationIT.java`:

```java
package com.gcrf.library.auth.migration;

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
class V001MigrationIT {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void seedDataIsPresent() {
        Integer roleCount = jdbc.queryForObject(
            "SELECT count(*) FROM gcrf_region.auth_role WHERE is_system = true", Integer.class);
        assertThat(roleCount).isEqualTo(10);

        Integer permCount = jdbc.queryForObject(
            "SELECT count(*) FROM gcrf_region.auth_permission", Integer.class);
        assertThat(permCount).isEqualTo(11);

        Integer adminBound = jdbc.queryForObject(
            "SELECT count(*) FROM gcrf_region.auth_user_role ur " +
            "JOIN gcrf_region.users u ON u.id = ur.user_id " +
            "JOIN gcrf_region.auth_role r ON r.id = ur.role_id " +
            "WHERE u.user_id = 'admin' AND r.code = 'REGION_ADMIN'", Integer.class);
        assertThat(adminBound).isEqualTo(1);

        Integer regionAdminPerms = jdbc.queryForObject(
            "SELECT count(*) FROM gcrf_region.auth_role_permission rp " +
            "JOIN gcrf_region.auth_role r ON r.id = rp.role_id " +
            "WHERE r.code = 'REGION_ADMIN'", Integer.class);
        assertThat(regionAdminPerms).isEqualTo(11);  // all permissions
    }
}
```

Run:

```bash
mvn test -pl auth-service -Dtest=V001MigrationIT -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/auth-service/src/main/resources/db/migration/region/V001__iam_baseline.sql \
        backend/auth-service/src/main/resources/application.yml \
        backend/auth-service/src/main/resources/application-prod.yml \
        backend/auth-service/src/test/java/com/gcrf/library/auth/migration/V001MigrationIT.java \
        docs/archives/legacy-2026-04/auth-service-V001__baseline-OLD-auth_service-db.sql
git rm --cached backend/auth-service/src/main/resources/db/migration/V001__baseline.sql 2>/dev/null || true
git commit -m "feat(auth): add IAM/RBAC Flyway V001 in gcrf_region with seeds for 10 roles + 11 permissions"
```

---

### Task 6: auth-service entity + datasource swap

User entity 加 org_node_id / school_id / tenant_schema 字段；UserMapper 改指 gcrf_region.users（用 `@TableName("gcrf_region.users")` 或在 yml 配 default schema）；application.yml 切到 gcrf_main DB。

**Files:**

- Modify: `backend/auth-service/src/main/java/com/gcrf/library/auth/entity/User.java`
- Modify: `backend/auth-service/src/main/resources/application.yml`
- Modify: `backend/auth-service/src/main/resources/application-prod.yml`
- Modify: `deployment/k8s/10-services.yaml` — gcrf-auth datasource URL

- [ ] **Step 1: Update User entity with new columns**

Edit `User.java` — add 3 fields after existing ones:

```java
@TableField("org_node_id")
private Long orgNodeId;

@TableField("school_id")
private Long schoolId;

@TableField("tenant_schema")
private String tenantSchema;
```

Also change `@TableName("users")` → `@TableName("gcrf_region.users")` if Mybatis-Plus doesn't auto-prefix with schema.

(Confirm by looking at existing org-service entities for the pattern used.)

- [ ] **Step 2: Local datasource swap (application.yml dev profile)**

Replace `spring.datasource.url` in `application.yml`:

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/gcrf_main?currentSchema=gcrf_region,public&stringtype=unspecified
    username: postgres
    password: gcrf_secure_2024
```

The `currentSchema=gcrf_region,public` makes default-schema-aware queries find `users` without explicit prefix; explicit `@TableName("gcrf_region.users")` is still safest.

- [ ] **Step 3: K8s deployment update**

Edit `deployment/k8s/10-services.yaml` for `gcrf-auth`:

```yaml
- name: SPRING_DATASOURCE_URL
  value: "jdbc:postgresql://postgresql.edu-infra.svc.cluster.local:5432/gcrf_main?currentSchema=gcrf_region,public&stringtype=unspecified"
- name: JWT_SECRET
  valueFrom: { secretKeyRef: { name: gcrf-db-secret, key: JWT_SECRET } }
```

Also add `JWT_SECRET` to `deployment/k8s/01-secret.yaml` (existing gcrf-db-secret):

```yaml
JWT_SECRET: <base64 of 80+ char random string>
```

To generate the value:

```bash
openssl rand -base64 64 | tr -d '\n' | base64
```

- [ ] **Step 4: Verify existing tests still compile**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn compile -pl auth-service -am
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Run existing AuthServiceTest against new schema (testcontainers boots fresh)**

```bash
mvn test -pl auth-service -Dtest=AuthServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: existing tests still pass — V001 creates users table with admin row, BCrypt password identical to old seed.

- [ ] **Step 6: Commit**

```bash
git add backend/auth-service/src/main/java/com/gcrf/library/auth/entity/User.java \
        backend/auth-service/src/main/resources/application.yml \
        backend/auth-service/src/main/resources/application-prod.yml \
        deployment/k8s/10-services.yaml \
        deployment/k8s/01-secret.yaml
git commit -m "feat(auth): point auth-service datasource at gcrf_main/gcrf_region; add JWT_SECRET env"
```

---

### Task 7: Role / Permission / UserRole entities + mappers + services

CRUD 层。

**Files:**

- Create: `backend/auth-service/src/main/java/com/gcrf/library/auth/entity/Role.java`
- Create: `.../entity/Permission.java`
- Create: `.../entity/UserRole.java`
- Create: `.../mapper/RoleMapper.java`
- Create: `.../mapper/PermissionMapper.java`
- Create: `.../mapper/UserRoleMapper.java`
- Create: `.../service/RoleService.java`
- Create: `.../service/PermissionService.java`
- Create: `.../service/AuthPermissionLookup.java`
- Create: `.../service/RoleServiceTest.java`

- [ ] **Step 1: Write failing test for RoleService**

```java
// RoleServiceTest.java (Mockito unit test, no Spring)
package com.gcrf.library.auth.service;

import com.gcrf.library.auth.entity.Role;
import com.gcrf.library.auth.entity.UserRole;
import com.gcrf.library.auth.mapper.PermissionMapper;
import com.gcrf.library.auth.mapper.RoleMapper;
import com.gcrf.library.auth.mapper.UserRoleMapper;
import com.gcrf.library.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock RoleMapper roleMapper;
    @Mock UserRoleMapper userRoleMapper;
    @Mock PermissionMapper permissionMapper;

    @InjectMocks RoleService svc;

    @Test
    void listSystemRoles_returnsOnlyIsSystem() {
        Role r1 = new Role(); r1.setId(1L); r1.setCode("REGION_ADMIN"); r1.setIsSystem(true);
        Role r2 = new Role(); r2.setId(2L); r2.setCode("CUSTOM");      r2.setIsSystem(false);
        when(roleMapper.selectList(any())).thenReturn(List.of(r1, r2));

        List<Role> out = svc.listSystemRoles();
        assertThat(out).hasSize(2);  // service does NOT filter — caller decides
    }

    @Test
    void assignRole_insertsIfNotExists() {
        when(roleMapper.selectById(7L)).thenReturn(new Role());
        when(userRoleMapper.findExact(42L, 7L, null)).thenReturn(null);

        svc.assignRole(42L, 7L, null, null, 1L);

        verify(userRoleMapper).insert(any(UserRole.class));
    }

    @Test
    void assignRole_skipsIfAlreadyAssigned() {
        UserRole existing = new UserRole(); existing.setId(99L);
        when(roleMapper.selectById(7L)).thenReturn(new Role());
        when(userRoleMapper.findExact(42L, 7L, null)).thenReturn(existing);

        svc.assignRole(42L, 7L, null, null, 1L);

        verify(userRoleMapper, never()).insert(any());
    }

    @Test
    void assignRole_throwsWhenRoleNotFound() {
        when(roleMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> svc.assignRole(42L, 99L, null, null, 1L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void revokeRole_deletesByCompositeKey() {
        svc.revokeRole(42L, 7L, null);
        verify(userRoleMapper).deleteByUserRoleSchool(42L, 7L, null);
    }
}
```

- [ ] **Step 2: Run failing**

```bash
mvn test -pl auth-service -Dtest=RoleServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: COMPILATION FAILURE.

- [ ] **Step 3: Implement entities**

```java
// Role.java
package com.gcrf.library.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("gcrf_region.auth_role")
public class Role {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String description;
    @TableField("scope_default")
    private String scopeDefault;
    @TableField("is_system")
    private Boolean isSystem;
    @TableField("school_id")
    private Long schoolId;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
```

```java
// Permission.java
package com.gcrf.library.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("gcrf_region.auth_permission")
public class Permission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String module;
    private String action;
    private String name;
    private String description;
    @TableField("sort_order")
    private Integer sortOrder;
}
```

```java
// UserRole.java
package com.gcrf.library.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("gcrf_region.auth_user_role")
public class UserRole {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("role_id")
    private Long roleId;
    @TableField("school_id")
    private Long schoolId;
    @TableField("scope_override")
    private String scopeOverride;
    @TableField("scope_path")
    private String scopePath;
    @TableField("assigned_by")
    private Long assignedBy;
    @TableField("assigned_at")
    private LocalDateTime assignedAt;
    @TableField("expires_at")
    private LocalDateTime expiresAt;
}
```

- [ ] **Step 4: Mappers**

```java
// RoleMapper.java
package com.gcrf.library.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.auth.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("""
        SELECT r.* FROM gcrf_region.auth_role r
          JOIN gcrf_region.auth_user_role ur ON ur.role_id = r.id
         WHERE ur.user_id = #{userId}
           AND (ur.expires_at IS NULL OR ur.expires_at > now())
        """)
    List<Role> findByUserId(Long userId);
}
```

```java
// PermissionMapper.java
package com.gcrf.library.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.auth.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    @Select("""
        SELECT DISTINCT p.code
          FROM gcrf_region.auth_permission p
          JOIN gcrf_region.auth_role_permission rp ON rp.permission_id = p.id
          JOIN gcrf_region.auth_user_role ur ON ur.role_id = rp.role_id
         WHERE ur.user_id = #{userId}
           AND (ur.expires_at IS NULL OR ur.expires_at > now())
        """)
    List<String> findCodesByUserId(Long userId);

    @Select("""
        SELECT p.* FROM gcrf_region.auth_permission p
          JOIN gcrf_region.auth_role_permission rp ON rp.permission_id = p.id
         WHERE rp.role_id = #{roleId}
         ORDER BY p.sort_order
        """)
    List<Permission> findByRoleId(Long roleId);
}
```

```java
// UserRoleMapper.java
package com.gcrf.library.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.auth.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    @Select({"<script>",
        "SELECT * FROM gcrf_region.auth_user_role",
        " WHERE user_id = #{userId} AND role_id = #{roleId}",
        " AND <choose>",
        "   <when test='schoolId == null'>school_id IS NULL</when>",
        "   <otherwise>school_id = #{schoolId}</otherwise>",
        " </choose>",
        " LIMIT 1",
        "</script>"})
    UserRole findExact(@Param("userId") Long userId,
                       @Param("roleId") Long roleId,
                       @Param("schoolId") Long schoolId);

    @Update({"<script>",
        "DELETE FROM gcrf_region.auth_user_role",
        " WHERE user_id = #{userId} AND role_id = #{roleId}",
        " AND <choose>",
        "   <when test='schoolId == null'>school_id IS NULL</when>",
        "   <otherwise>school_id = #{schoolId}</otherwise>",
        " </choose>",
        "</script>"})
    int deleteByUserRoleSchool(@Param("userId") Long userId,
                               @Param("roleId") Long roleId,
                               @Param("schoolId") Long schoolId);
}
```

- [ ] **Step 5: RoleService**

```java
// RoleService.java
package com.gcrf.library.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcrf.library.auth.entity.Role;
import com.gcrf.library.auth.entity.UserRole;
import com.gcrf.library.auth.mapper.PermissionMapper;
import com.gcrf.library.auth.mapper.RoleMapper;
import com.gcrf.library.auth.mapper.UserRoleMapper;
import com.gcrf.library.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PermissionMapper permissionMapper;

    public List<Role> listSystemRoles() {
        return roleMapper.selectList(
            new LambdaQueryWrapper<Role>().orderByAsc(Role::getSortOrder));
    }

    public Role getById(Long id) {
        Role r = roleMapper.selectById(id);
        if (r == null) throw new BusinessException(404, "角色不存在: " + id);
        return r;
    }

    public List<Role> rolesOfUser(Long userId) {
        return roleMapper.findByUserId(userId);
    }

    @Transactional
    public void assignRole(Long userId, Long roleId, Long schoolId,
                           LocalDateTime expiresAt, Long operatorId) {
        if (roleMapper.selectById(roleId) == null) {
            throw new BusinessException(404, "角色不存在: " + roleId);
        }
        if (userRoleMapper.findExact(userId, roleId, schoolId) != null) {
            return;
        }
        UserRole ur = new UserRole();
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        ur.setSchoolId(schoolId);
        ur.setAssignedBy(operatorId);
        ur.setAssignedAt(LocalDateTime.now());
        ur.setExpiresAt(expiresAt);
        userRoleMapper.insert(ur);
    }

    @Transactional
    public void revokeRole(Long userId, Long roleId, Long schoolId) {
        userRoleMapper.deleteByUserRoleSchool(userId, roleId, schoolId);
    }
}
```

- [ ] **Step 6: PermissionService + AuthPermissionLookup**

```java
// PermissionService.java
package com.gcrf.library.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcrf.library.auth.entity.Permission;
import com.gcrf.library.auth.mapper.PermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionMapper permissionMapper;

    public List<Permission> listAll() {
        return permissionMapper.selectList(
            new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getSortOrder));
    }

    public List<Permission> listForRole(Long roleId) {
        return permissionMapper.findByRoleId(roleId);
    }

    public Set<String> codesForUser(Long userId) {
        return Set.copyOf(permissionMapper.findCodesByUserId(userId));
    }
}
```

```java
// AuthPermissionLookup.java
package com.gcrf.library.auth.service;

import com.gcrf.library.common.security.permission.PermissionLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class AuthPermissionLookup implements PermissionLookup {

    private final PermissionService permissionService;

    @Override
    @Cacheable(value = "perms", key = "#userId")
    public Set<String> lookup(Long userId) {
        return permissionService.codesForUser(userId);
    }
}
```

For other business services we don't bundle a PermissionLookup — they'll consume permissions via JWT-validation alone. AuthPermissionLookup in auth-service handles its own @RequirePermission methods (the role-assignment endpoint uses @RequireRole, so a lookup is only needed if a service annotates @RequirePermission).

Actually for M1 simplicity: auth-service is the only service that calls @RequirePermission (on a small number of endpoints). Other services use @RequireRole / @RequireScope which don't need PermissionLookup. So this bean only needs to exist in auth-service.

- [ ] **Step 7: Run test to verify pass**

```bash
mvn test -pl auth-service -Dtest=RoleServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: 5 tests pass.

- [ ] **Step 8: Commit**

```bash
git add backend/auth-service/src/main/java/com/gcrf/library/auth/entity/Role.java \
        backend/auth-service/src/main/java/com/gcrf/library/auth/entity/Permission.java \
        backend/auth-service/src/main/java/com/gcrf/library/auth/entity/UserRole.java \
        backend/auth-service/src/main/java/com/gcrf/library/auth/mapper/RoleMapper.java \
        backend/auth-service/src/main/java/com/gcrf/library/auth/mapper/PermissionMapper.java \
        backend/auth-service/src/main/java/com/gcrf/library/auth/mapper/UserRoleMapper.java \
        backend/auth-service/src/main/java/com/gcrf/library/auth/service/RoleService.java \
        backend/auth-service/src/main/java/com/gcrf/library/auth/service/PermissionService.java \
        backend/auth-service/src/main/java/com/gcrf/library/auth/service/AuthPermissionLookup.java \
        backend/auth-service/src/test/java/com/gcrf/library/auth/service/RoleServiceTest.java
git commit -m "feat(auth): add Role/Permission/UserRole entities + mappers + services"
```

---

### Task 8: AuthService JWT 富化 — login 返回 roles/tenant/scope

修改 `AuthService.login()` 在签发 JWT 时塞入富 claims；`LoginResponse` 增加 `roles/tenant/scope/permissions` 字段。

**Files:**

- Modify: `backend/auth-service/src/main/java/com/gcrf/library/auth/service/AuthService.java`
- Modify: `backend/auth-service/src/main/java/com/gcrf/library/auth/dto/LoginResponse.java`
- Modify: existing `AuthServiceTest.java` — 加 login enrichment 断言

- [ ] **Step 1: Extend failing test in AuthServiceTest**

Append to `AuthServiceTest.java`:

```java
@Test
@DisplayName("login_richResponse_carriesRolesTenantScope")
void login_richResponse_carriesRolesTenantScope() {
    User adminUser = new User();
    adminUser.setId(1L);
    adminUser.setUsername("admin");
    adminUser.setPassword(passwordEncoder.encode("admin123"));
    adminUser.setStatus("ACTIVE");
    adminUser.setUserType("ADMIN");
    adminUser.setTenantSchema(null);  // region-level user

    when(userMapper.selectOne(any())).thenReturn(adminUser);

    Role regionAdmin = new Role();
    regionAdmin.setId(1L);
    regionAdmin.setCode("REGION_ADMIN");
    regionAdmin.setScopeDefault("REGION");
    when(roleService.rolesOfUser(1L)).thenReturn(List.of(regionAdmin));
    when(permissionService.codesForUser(1L)).thenReturn(Set.of("book.read","book.write"));

    LoginRequest req = new LoginRequest();
    req.setUsername("admin");
    req.setPassword("admin123");

    LoginResponse resp = authService.login(req);

    assertNotNull(resp.getAccessToken());
    assertNotNull(resp.getRefreshToken());
    assertEquals(List.of("REGION_ADMIN"), resp.getRoles());
    assertNull(resp.getTenant());
    assertEquals("REGION", resp.getScope());
    assertTrue(resp.getPermissions().contains("book.read"));
}
```

(Add `@Mock RoleService roleService; @Mock PermissionService permissionService;` and inject into the field list.)

- [ ] **Step 2: Run failing**

```bash
mvn test -pl auth-service -Dtest=AuthServiceTest#login_richResponse_carriesRolesTenantScope -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: COMPILATION FAILURE — `getRoles()`, `getTenant()`, `getScope()`, `getPermissions()`, `getRefreshToken()` not on LoginResponse.

- [ ] **Step 3: Extend LoginResponse**

In `LoginResponse.java`, add fields and adjust constructor:

```java
private List<String> roles;
private String tenant;
private Long tenantId;
private String scope;
private Set<String> permissions;
private String refreshToken;
```

Use Lombok `@Data @AllArgsConstructor @NoArgsConstructor @Builder` so callers can use builder pattern.

- [ ] **Step 4: Update AuthService.login()**

In `AuthService.java`:

```java
@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final RefreshTokenService refreshTokenService;  // Task 9
    private final RedissonClient redissonClient;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginResponse login(LoginRequest request) {
        log.info("用户登录请求: username={}", request.getUsername());
        User user = findActiveUser(request.getUsername());
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USER_CREDENTIALS_ERROR);
        }
        return buildLoginResponse(user);
    }

    private LoginResponse buildLoginResponse(User user) {
        List<Role> roles = roleService.rolesOfUser(user.getId());
        List<String> roleCodes = roles.stream().map(Role::getCode).toList();
        String maxScope = roles.stream()
            .map(Role::getScopeDefault)
            .max(java.util.Comparator.comparingInt(s -> com.gcrf.library.common.security.context.Scope.valueOf(s).ordinal()))
            .orElse("SELF");

        Set<String> perms = permissionService.codesForUser(user.getId());

        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        if (user.getTenantSchema() != null) {
            claims.put("tenant", user.getTenantSchema());
            claims.put("tenantId", user.getSchoolId());
        }
        claims.put("roles", roleCodes);
        claims.put("scope", maxScope);

        String accessToken = jwtUtil.generateToken(user.getId().toString(), claims);
        String refreshToken = refreshTokenService.issue(user.getId());

        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(1800L)   // 30 min, matches JWT TTL
            .userId(user.getId())
            .username(user.getUsername())
            .userType(user.getUserType())
            .roles(roleCodes)
            .tenant(user.getTenantSchema())
            .tenantId(user.getSchoolId())
            .scope(maxScope)
            .permissions(perms)
            .build();
    }

    private User findActiveUser(String username) {
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();
        w.eq(User::getUsername, username).apply("deleted_at IS NULL");
        User u = userMapper.selectOne(w);
        if (u == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        if (!"ACTIVE".equals(u.getStatus())) throw new BusinessException(ResultCode.USER_DISABLED);
        return u;
    }
}
```

(refreshToken used in Task 9 — this Task already invokes it. If you implement Task 9 before this step, the dependency exists; otherwise stub `refreshTokenService.issue` returning UUID for this commit and finalize in Task 9.)

To keep tasks linear, **stub RefreshTokenService here** with a UUID generator:

```java
// Temporary stub — fully implemented in Task 9
@Service
public class RefreshTokenService {
    public String issue(Long userId) { return java.util.UUID.randomUUID().toString(); }
    public Long consume(String token) { throw new UnsupportedOperationException("Implemented in Task 9"); }
    public void revoke(String token) {}
}
```

- [ ] **Step 5: Run test to verify pass**

```bash
mvn test -pl auth-service -Dtest=AuthServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: all existing tests + new one pass.

- [ ] **Step 6: Commit**

```bash
git add backend/auth-service/src/main/java/com/gcrf/library/auth/service/AuthService.java \
        backend/auth-service/src/main/java/com/gcrf/library/auth/service/RefreshTokenService.java \
        backend/auth-service/src/main/java/com/gcrf/library/auth/dto/LoginResponse.java \
        backend/auth-service/src/test/java/com/gcrf/library/auth/service/AuthServiceTest.java
git commit -m "feat(auth): enrich JWT and LoginResponse with roles/tenant/scope/permissions"
```

---

### Task 9: RefreshTokenService — Redis 存储 + 旋转 + /refresh + /logout

替换 Task 8 的 stub；refresh token 旋转防重放。

**Files:**

- Modify: `backend/auth-service/src/main/java/com/gcrf/library/auth/service/RefreshTokenService.java`
- Modify: `backend/auth-service/src/main/java/com/gcrf/library/auth/service/AuthService.java` — refreshToken / logout
- Modify: `backend/auth-service/src/main/java/com/gcrf/library/auth/controller/AuthController.java`
- Create: `backend/auth-service/src/test/java/com/gcrf/library/auth/service/RefreshTokenServiceTest.java`

- [ ] **Step 1: Write failing test**

```java
// RefreshTokenServiceTest.java
package com.gcrf.library.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock RedissonClient redisson;
    @Mock RBucket<Long> bucket;

    @InjectMocks RefreshTokenService svc;

    @Test
    void issueWritesToRedisWith30DayTTL() {
        when(redisson.<Long>getBucket(any())).thenReturn(bucket);

        String token = svc.issue(42L);

        assertThat(token).matches("[0-9a-f-]{36}");
        verify(bucket).set(eq(42L), eq(30L), eq(TimeUnit.DAYS));
    }

    @Test
    void consumeReadsAndDeletes() {
        when(redisson.<Long>getBucket("refresh:abc")).thenReturn(bucket);
        when(bucket.get()).thenReturn(42L);

        Long userId = svc.consume("abc");

        assertThat(userId).isEqualTo(42L);
        verify(bucket).delete();
    }

    @Test
    void consumeUnknownThrowsBusinessException() {
        when(redisson.<Long>getBucket("refresh:bad")).thenReturn(bucket);
        when(bucket.get()).thenReturn(null);

        assertThatThrownBy(() -> svc.consume("bad"))
            .hasMessageContaining("refresh");
    }

    @Test
    void revokeDeletesKey() {
        when(redisson.<Long>getBucket("refresh:abc")).thenReturn(bucket);
        svc.revoke("abc");
        verify(bucket).delete();
    }
}
```

- [ ] **Step 2: Run failing**

```bash
mvn test -pl auth-service -Dtest=RefreshTokenServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: existing stub fails most cases.

- [ ] **Step 3: Full implementation**

Replace `RefreshTokenService.java` body:

```java
package com.gcrf.library.auth.service;

import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";
    private static final long TTL_DAYS = 30L;

    private final RedissonClient redisson;

    public String issue(Long userId) {
        String token = UUID.randomUUID().toString();
        RBucket<Long> bucket = redisson.getBucket(KEY_PREFIX + token);
        bucket.set(userId, TTL_DAYS, TimeUnit.DAYS);
        return token;
    }

    public Long consume(String token) {
        RBucket<Long> bucket = redisson.getBucket(KEY_PREFIX + token);
        Long userId = bucket.get();
        if (userId == null) {
            throw new BusinessException(ResultCode.TOKEN_INVALID.getCode(), "refresh token 无效或已过期");
        }
        bucket.delete();
        return userId;
    }

    public void revoke(String token) {
        RBucket<Long> bucket = redisson.getBucket(KEY_PREFIX + token);
        bucket.delete();
    }
}
```

- [ ] **Step 4: Replace AuthService.refreshToken() + add logout**

Replace existing `refreshToken` method in `AuthService.java`:

```java
public LoginResponse refreshToken(String refreshToken) {
    Long userId = refreshTokenService.consume(refreshToken);
    User user = userMapper.selectById(userId);
    if (user == null || !"ACTIVE".equals(user.getStatus())) {
        throw new BusinessException(ResultCode.USER_DISABLED);
    }
    return buildLoginResponse(user);  // rotates refresh too (issue called inside)
}

public void logout(String refreshToken) {
    if (refreshToken != null && !refreshToken.isBlank()) {
        refreshTokenService.revoke(refreshToken);
    }
}
```

Also delete the old `Thread.sleep(1000)` hack and the old token blacklist write (those were for access-token blacklisting which M1 punts).

- [ ] **Step 5: AuthController — /refresh and /logout match new contract**

Edit `AuthController.java`:

```java
@PostMapping("/refresh")
public Result<LoginResponse> refresh(@RequestBody RefreshRequest req) {
    return Result.success(authService.refreshToken(req.getRefreshToken()));
}

@PostMapping("/logout")
public Result<Void> logout(@RequestBody(required = false) RefreshRequest req) {
    String tk = req == null ? null : req.getRefreshToken();
    authService.logout(tk);
    return Result.success();
}

@GetMapping("/me")
public Result<LoginResponse> me() {
    Long uid = SecurityContextHolder.currentUserId();
    if (uid == null) throw new BusinessException(ResultCode.TOKEN_INVALID);
    User user = userService.getById(uid);  // reuse existing UserService
    return Result.success(authService.buildLoginResponseFromUser(user));
}
```

Create `RefreshRequest.java`:

```java
package com.gcrf.library.auth.dto;

import lombok.Data;

@Data
public class RefreshRequest {
    private String refreshToken;
}
```

Expose `buildLoginResponseFromUser` as public in AuthService (rename to avoid confusion):

```java
public LoginResponse buildLoginResponseFromUser(User user) { return buildLoginResponse(user); }
```

- [ ] **Step 6: Run test**

```bash
mvn test -pl auth-service -Dtest=RefreshTokenServiceTest,AuthServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: all pass.

- [ ] **Step 7: Commit**

```bash
git add backend/auth-service/src/main/java/com/gcrf/library/auth/service/RefreshTokenService.java \
        backend/auth-service/src/main/java/com/gcrf/library/auth/service/AuthService.java \
        backend/auth-service/src/main/java/com/gcrf/library/auth/controller/AuthController.java \
        backend/auth-service/src/main/java/com/gcrf/library/auth/dto/RefreshRequest.java \
        backend/auth-service/src/test/java/com/gcrf/library/auth/service/RefreshTokenServiceTest.java
git commit -m "feat(auth): add RefreshTokenService with Redis-backed rotation; /refresh and /logout endpoints"
```

---

### Task 10: RoleController + UserRoleController + PermissionController

只读角色 / 权限 + REGION_ADMIN 限定的用户角色分配端点。

**Files:**

- Create: `backend/auth-service/src/main/java/com/gcrf/library/auth/controller/RoleController.java`
- Create: `backend/auth-service/src/main/java/com/gcrf/library/auth/controller/UserRoleController.java`
- Create: `backend/auth-service/src/main/java/com/gcrf/library/auth/dto/AssignRoleRequest.java`
- Create: `backend/auth-service/src/main/java/com/gcrf/library/auth/dto/RoleVO.java`
- Create: `backend/auth-service/src/main/java/com/gcrf/library/auth/dto/RoleDetailVO.java`
- Create: `backend/auth-service/src/test/java/com/gcrf/library/auth/controller/RoleControllerIT.java`

- [ ] **Step 1: Write integration test (HTTP-level)**

```java
// RoleControllerIT.java
package com.gcrf.library.auth.controller;

import com.gcrf.library.auth.entity.User;
import com.gcrf.library.common.utils.JwtUtil;
import com.gcrf.library.auth.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class RoleControllerIT {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwtUtil;
    @Autowired UserMapper userMapper;

    private String tokenForAdmin() {
        User admin = userMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, "admin"));
        return jwtUtil.generateToken(admin.getId().toString(), Map.of(
            "userId", admin.getId(),
            "username", "admin",
            "roles", List.of("REGION_ADMIN"),
            "scope", "REGION"
        ));
    }

    private String tokenForLibrarian(Long uid) {
        return jwtUtil.generateToken(uid.toString(), Map.of(
            "userId", uid,
            "username", "lib1",
            "roles", List.of("LIBRARIAN"),
            "scope", "SCHOOL"
        ));
    }

    @Test
    void getRoles_returns10SystemRoles() throws Exception {
        mvc.perform(get("/api/v1/roles").header("Authorization", "Bearer " + tokenForAdmin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(10));
    }

    @Test
    void getPermissions_returns11() throws Exception {
        mvc.perform(get("/api/v1/permissions").header("Authorization", "Bearer " + tokenForAdmin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(11));
    }

    @Test
    void assignRole_byAdmin_succeeds() throws Exception {
        // assume seed admin user has id=1; we assign him a redundant role for the test
        mvc.perform(post("/api/v1/users/1/roles")
                .header("Authorization", "Bearer " + tokenForAdmin())
                .contentType("application/json")
                .content("{\"roleCode\":\"LIBRARIAN\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void assignRole_byLibrarian_403() throws Exception {
        mvc.perform(post("/api/v1/users/1/roles")
                .header("Authorization", "Bearer " + tokenForLibrarian(99L))
                .contentType("application/json")
                .content("{\"roleCode\":\"LIBRARIAN\"}"))
            .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2: Run failing**

```bash
mvn test -pl auth-service -Dtest=RoleControllerIT -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: COMPILATION FAILURE.

- [ ] **Step 3: DTOs**

```java
// RoleVO.java
package com.gcrf.library.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleVO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String scopeDefault;
    private Boolean isSystem;
    private Integer permissionCount;
    private Integer userCount;
}
```

```java
// RoleDetailVO.java
package com.gcrf.library.auth.dto;

import com.gcrf.library.auth.entity.Permission;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoleDetailVO {
    private RoleVO role;
    private List<Permission> permissions;
}
```

```java
// AssignRoleRequest.java
package com.gcrf.library.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssignRoleRequest {
    @NotBlank
    private String roleCode;     // 用 code 更友好（前端不必先查 id）
    private Long schoolId;       // null = 区域级
    private LocalDateTime expiresAt;
}
```

- [ ] **Step 4: RoleController + PermissionController**

```java
// RoleController.java
package com.gcrf.library.auth.controller;

import com.gcrf.library.auth.dto.RoleDetailVO;
import com.gcrf.library.auth.dto.RoleVO;
import com.gcrf.library.auth.entity.Role;
import com.gcrf.library.auth.service.PermissionService;
import com.gcrf.library.auth.service.RoleService;
import com.gcrf.library.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final PermissionService permissionService;

    @GetMapping
    public Result<List<RoleVO>> list() {
        return Result.success(roleService.listSystemRoles().stream()
            .map(this::toVO).toList());
    }

    @GetMapping("/{id}")
    public Result<RoleDetailVO> detail(@PathVariable Long id) {
        Role r = roleService.getById(id);
        return Result.success(RoleDetailVO.builder()
            .role(toVO(r))
            .permissions(permissionService.listForRole(id))
            .build());
    }

    private RoleVO toVO(Role r) {
        return RoleVO.builder()
            .id(r.getId())
            .code(r.getCode())
            .name(r.getName())
            .description(r.getDescription())
            .scopeDefault(r.getScopeDefault())
            .isSystem(r.getIsSystem())
            .build();
    }
}
```

```java
// PermissionController.java
package com.gcrf.library.auth.controller;

import com.gcrf.library.auth.entity.Permission;
import com.gcrf.library.auth.service.PermissionService;
import com.gcrf.library.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public Result<List<Permission>> list() {
        return Result.success(permissionService.listAll());
    }
}
```

- [ ] **Step 5: UserRoleController (REGION_ADMIN only)**

```java
// UserRoleController.java
package com.gcrf.library.auth.controller;

import com.gcrf.library.auth.dto.AssignRoleRequest;
import com.gcrf.library.auth.entity.Role;
import com.gcrf.library.auth.service.RoleService;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.common.security.annotation.RequireRole;
import com.gcrf.library.common.security.context.SecurityContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}/roles")
@RequiredArgsConstructor
public class UserRoleController {

    private final RoleService roleService;

    @GetMapping
    public Result<List<Role>> roles(@PathVariable Long userId) {
        return Result.success(roleService.rolesOfUser(userId));
    }

    @PostMapping
    @RequireRole({"REGION_ADMIN"})
    public Result<Void> assign(@PathVariable Long userId, @Valid @RequestBody AssignRoleRequest req) {
        Role r = roleService.listSystemRoles().stream()
            .filter(role -> role.getCode().equals(req.getRoleCode()))
            .findFirst()
            .orElseThrow(() -> new BusinessException(404, "角色不存在: " + req.getRoleCode()));
        Long operator = SecurityContextHolder.currentUserId();
        roleService.assignRole(userId, r.getId(), req.getSchoolId(), req.getExpiresAt(), operator);
        return Result.success();
    }

    @DeleteMapping("/{roleId}")
    @RequireRole({"REGION_ADMIN"})
    public Result<Void> revoke(@PathVariable Long userId, @PathVariable Long roleId,
                                @RequestParam(required = false) Long schoolId) {
        roleService.revokeRole(userId, roleId, schoolId);
        return Result.success();
    }
}
```

- [ ] **Step 6: Run test**

```bash
mvn test -pl auth-service -Dtest=RoleControllerIT -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: 4 tests pass.

- [ ] **Step 7: Commit**

```bash
git add backend/auth-service/src/main/java/com/gcrf/library/auth/controller/RoleController.java \
        backend/auth-service/src/main/java/com/gcrf/library/auth/controller/PermissionController.java \
        backend/auth-service/src/main/java/com/gcrf/library/auth/controller/UserRoleController.java \
        backend/auth-service/src/main/java/com/gcrf/library/auth/dto/AssignRoleRequest.java \
        backend/auth-service/src/main/java/com/gcrf/library/auth/dto/RoleVO.java \
        backend/auth-service/src/main/java/com/gcrf/library/auth/dto/RoleDetailVO.java \
        backend/auth-service/src/test/java/com/gcrf/library/auth/controller/RoleControllerIT.java
git commit -m "feat(auth): expose /api/v1/roles, /permissions, /users/{id}/roles endpoints (REGION_ADMIN-gated assignment)"
```

---

### Task 11: Business services wire in common-security

各业务服务（system / circulation / opac / org / reader / book / analytics / chat / recommend）已经把 `common-security` 当 transitive 依赖了；自动装配会装上 SecurityContextFilter。本 task 验证：每个服务启动时 filter 注册成功 + JWT 解析正确。本 task 只跑 smoke 测试，无源码改动；不通过则 debug 具体服务的 servlet 过滤链冲突。

**Files:**

- Modify: `backend/*/pom.xml` — 确保依赖 common-security（应已存在）
- Create: `backend/common/common-security/src/test/java/com/gcrf/library/common/security/AutoConfigSmokeTest.java`

- [ ] **Step 1: Verify common-security is on every service classpath**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
for s in auth-service book-service circulation-service reader-service system-service notification-service analytics-service chat-service recommend-service org-service opac-service; do
  printf "%-25s common-security: " "$s"
  if grep -q "common-security" backend/$s/pom.xml; then echo "✓"; else echo "✗ MISSING — add it"; fi
done
```

Expected: all ✓. If any ✗, add to that pom inside `<dependencies>`:

```xml
<dependency>
    <groupId>com.gcrf.library</groupId>
    <artifactId>common-security</artifactId>
</dependency>
```

- [ ] **Step 2: Compile all services**

```bash
cd backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn compile -DskipTests -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Add an AutoConfig smoke test on one representative service**

Pick `system-service` (has the most controllers). Create `backend/system-service/src/test/java/com/gcrf/library/system/SecurityFilterRegistrationTest.java`:

```java
package com.gcrf.library.system;

import com.gcrf.library.common.security.filter.SecurityContextFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SecurityFilterRegistrationTest {

    @Autowired(required = false)
    FilterRegistrationBean<SecurityContextFilter> securityFilterRegistration;

    @Test
    void securityContextFilterBeanIsRegistered() {
        assertThat(securityFilterRegistration).isNotNull();
        assertThat(securityFilterRegistration.getFilter()).isInstanceOf(SecurityContextFilter.class);
    }
}
```

```bash
mvn test -pl system-service -Dtest=SecurityFilterRegistrationTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add backend/system-service/src/test/java/com/gcrf/library/system/SecurityFilterRegistrationTest.java
# Plus any pom.xml fixes if a service was missing common-security
git commit -m "test(common): verify SecurityContextFilter auto-registers on system-service"
```

---

### Task 12: web-admin — 角色管理页 + 用户角色分配抽屉

新增 `/system/roles` 页面 + 在用户管理页加"角色"列 + 点击抽屉编辑。

**Files:**

- Create: `web-admin/src/api/role.js`
- Create: `web-admin/src/views/system/roles.vue`
- Create: `web-admin/src/views/system/UserRoleDrawer.vue`
- Modify: `web-admin/src/router/index.js`
- Modify: `web-admin/src/views/system/users.vue`

- [ ] **Step 1: API client**

```javascript
// web-admin/src/api/role.js
import request from "@/utils/request";

export function getRoles() {
  return request({ url: "/api/v1/roles", method: "get" });
}

export function getRoleDetail(id) {
  return request({ url: `/api/v1/roles/${id}`, method: "get" });
}

export function getPermissions() {
  return request({ url: "/api/v1/permissions", method: "get" });
}

export function getUserRoles(userId) {
  return request({ url: `/api/v1/users/${userId}/roles`, method: "get" });
}

export function assignRole(userId, payload) {
  return request({
    url: `/api/v1/users/${userId}/roles`,
    method: "post",
    data: payload,
  });
}

export function revokeRole(userId, roleId, schoolId) {
  return request({
    url: `/api/v1/users/${userId}/roles/${roleId}`,
    method: "delete",
    params: schoolId ? { schoolId } : {},
  });
}
```

- [ ] **Step 2: 角色管理页**

```vue
<!-- web-admin/src/views/system/roles.vue -->
<template>
  <div class="page-container">
    <div class="page-header">
      <h1 class="page-header-title">角色管理</h1>
      <p class="page-header-description">
        查看系统内置的 10 个角色与对应权限（M1 只读）
      </p>
    </div>

    <div class="lib-card">
      <div class="lib-card-body">
        <el-table
          v-loading="loading"
          :data="roles"
          stripe
          @row-click="onRowClick"
        >
          <el-table-column prop="code" label="角色编码" width="180" />
          <el-table-column prop="name" label="角色名称" width="160" />
          <el-table-column prop="scopeDefault" label="默认数据范围" width="140">
            <template #default="{ row }">
              <el-tag :type="scopeTagType(row.scopeDefault)" size="small">
                {{ row.scopeDefault }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="说明" />
          <el-table-column prop="isSystem" label="系统角色" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.isSystem" type="info" size="small">系统</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-drawer
      v-model="detailVisible"
      :title="selectedRole?.name"
      size="40%"
      destroy-on-close
    >
      <div v-if="detailLoading" v-loading="true" style="min-height: 200px" />
      <div v-else>
        <h3>权限列表（{{ permissions.length }}）</h3>
        <el-table :data="permissions" stripe>
          <el-table-column prop="code" label="权限编码" width="200" />
          <el-table-column prop="module" label="模块" width="120" />
          <el-table-column prop="action" label="操作" width="100" />
          <el-table-column prop="name" label="名称" />
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { getRoles, getRoleDetail } from "@/api/role";

const loading = ref(false);
const roles = ref([]);
const detailVisible = ref(false);
const detailLoading = ref(false);
const selectedRole = ref(null);
const permissions = ref([]);

const scopeTagType = (s) =>
  ({
    REGION: "danger",
    SCHOOL: "warning",
    GRADE: "",
    CLASS: "success",
    SELF: "info",
  })[s] || "";

const loadRoles = async () => {
  loading.value = true;
  try {
    const res = await getRoles();
    roles.value = res.code === 200 ? res.data || [] : [];
  } catch (e) {
    ElMessage.error("加载角色失败");
  } finally {
    loading.value = false;
  }
};

const onRowClick = async (row) => {
  selectedRole.value = row.role || row;
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    const res = await getRoleDetail(row.id);
    permissions.value = res.code === 200 ? res.data?.permissions || [] : [];
  } catch (e) {
    ElMessage.error("加载权限失败");
  } finally {
    detailLoading.value = false;
  }
};

onMounted(loadRoles);
</script>
```

- [ ] **Step 3: 用户角色抽屉**

```vue
<!-- web-admin/src/views/system/UserRoleDrawer.vue -->
<template>
  <el-drawer
    v-model="visible"
    :title="`分配角色 — ${user?.realName || user?.username || ''}`"
    size="40%"
  >
    <div v-loading="loading">
      <h3>当前角色</h3>
      <el-table :data="userRoles" stripe empty-text="尚未分配任何角色">
        <el-table-column prop="code" label="角色" width="160" />
        <el-table-column prop="name" label="名称" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" link @click="onRevoke(row)"
              >撤销</el-button
            >
          </template>
        </el-table-column>
      </el-table>

      <el-divider />

      <h3>新增角色</h3>
      <el-form :model="form" inline>
        <el-form-item label="角色">
          <el-select
            v-model="form.roleCode"
            placeholder="选择角色"
            style="width: 200px"
          >
            <el-option
              v-for="r in availableRoles"
              :key="r.code"
              :label="r.name"
              :value="r.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="form.expiresAt"
            type="datetime"
            placeholder="留空=永久"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onAssign" :disabled="!form.roleCode"
            >添加</el-button
          >
        </el-form-item>
      </el-form>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, computed, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { getRoles, getUserRoles, assignRole, revokeRole } from "@/api/role";

const props = defineProps({ modelValue: Boolean, user: Object });
const emit = defineEmits(["update:modelValue"]);

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit("update:modelValue", v),
});

const loading = ref(false);
const allRoles = ref([]);
const userRoles = ref([]);
const form = ref({ roleCode: "", expiresAt: null });

const availableRoles = computed(() => {
  const assigned = new Set(userRoles.value.map((r) => r.code));
  return allRoles.value.filter((r) => !assigned.has(r.code));
});

const load = async () => {
  if (!props.user?.id) return;
  loading.value = true;
  try {
    const [rolesRes, userRolesRes] = await Promise.all([
      getRoles(),
      getUserRoles(props.user.id),
    ]);
    allRoles.value = rolesRes.code === 200 ? rolesRes.data || [] : [];
    userRoles.value = userRolesRes.code === 200 ? userRolesRes.data || [] : [];
  } catch (e) {
    ElMessage.error("加载角色失败");
  } finally {
    loading.value = false;
  }
};

watch(
  () => [visible.value, props.user?.id],
  () => {
    if (visible.value) load();
  },
);

const onAssign = async () => {
  try {
    await assignRole(props.user.id, {
      roleCode: form.value.roleCode,
      expiresAt: form.value.expiresAt,
    });
    ElMessage.success("角色添加成功");
    form.value = { roleCode: "", expiresAt: null };
    load();
  } catch (e) {
    ElMessage.error("添加失败 — 需要 REGION_ADMIN 权限");
  }
};

const onRevoke = async (row) => {
  try {
    await ElMessageBox.confirm(`撤销 ${row.code} 吗？`, "提示", {
      type: "warning",
    });
    await revokeRole(props.user.id, row.id, null);
    ElMessage.success("已撤销");
    load();
  } catch (e) {
    if (e !== "cancel") ElMessage.error("撤销失败");
  }
};
</script>
```

- [ ] **Step 4: 注册路由**

In `web-admin/src/router/index.js`, find the `system` parent children array and add after `users` child:

```javascript
{
  path: 'roles',
  name: 'SystemRoles',
  component: () => import('@/views/system/roles.vue'),
  meta: { title: '角色权限' }
},
```

(Replace the existing `roles` route if it points to an old placeholder — the existing route file already has a `roles` entry pointing to `system/roles.vue` per the index.js we read earlier. Re-use that path; this task creates the matching view.)

- [ ] **Step 5: Wire drawer into users page**

In `web-admin/src/views/system/users.vue`:

Add a column to the table after the existing "邮箱" column:

```vue
<el-table-column label="角色" width="150">
  <template #default="{ row }">
    <el-button type="primary" link @click="openRoleDrawer(row)">分配角色</el-button>
  </template>
</el-table-column>
```

Add to `<script setup>`:

```javascript
import UserRoleDrawer from "./UserRoleDrawer.vue";

const roleDrawerVisible = ref(false);
const roleDrawerUser = ref(null);

const openRoleDrawer = (row) => {
  roleDrawerUser.value = row;
  roleDrawerVisible.value = true;
};
```

Inside the template, before closing `</div>`:

```vue
<UserRoleDrawer v-model="roleDrawerVisible" :user="roleDrawerUser" />
```

- [ ] **Step 6: Smoke test frontend build**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/web-admin
npm run build 2>&1 | tail -5
```

Expected: `built in Xs` (no errors).

- [ ] **Step 7: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add web-admin/src/api/role.js \
        web-admin/src/views/system/roles.vue \
        web-admin/src/views/system/UserRoleDrawer.vue \
        web-admin/src/router/index.js \
        web-admin/src/views/system/users.vue
git commit -m "feat(web-admin): add role mgmt page and user role assignment drawer"
```

---

### Task 13: 拆 system-service → auth-service Feign 链路

system-service.UserController 改本地 mapper 查 gcrf_region.users；删 UserManagementClient + 删 common-feign 依赖（昨天加的，拆完不需要了）。

**Files:**

- Create: `backend/system-service/src/main/java/com/gcrf/library/system/entity/User.java`
- Create: `backend/system-service/src/main/java/com/gcrf/library/system/mapper/UserLocalMapper.java`
- Modify: `backend/system-service/src/main/java/com/gcrf/library/system/controller/UserController.java`
- Delete: `backend/system-service/src/main/java/com/gcrf/library/system/client/UserManagementClient.java`
- Delete: `backend/system-service/src/main/java/com/gcrf/library/system/dto/response/UserPageVO.java`
- Modify: `backend/system-service/pom.xml` — 删 common-feign
- Modify: `backend/system-service/src/main/resources/application.yml` — datasource 指 gcrf_main / gcrf_region

- [ ] **Step 1: Create local User entity + mapper for gcrf_region.users**

```java
// backend/system-service/src/main/java/com/gcrf/library/system/entity/User.java
package com.gcrf.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("gcrf_region.users")
public class User {
    @TableId(type = IdType.AUTO) private Long id;
    @TableField("user_id") private String userId;
    private String username;
    private String password;
    private String email;
    private String phone;
    @TableField("user_type") private String userType;
    @TableField("avatar_url") private String avatarUrl;
    private String status;
    @TableField("last_login_time") private LocalDateTime lastLoginTime;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
    @TableField("org_node_id") private Long orgNodeId;
    @TableField("school_id") private Long schoolId;
}
```

```java
// UserLocalMapper.java
package com.gcrf.library.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.system.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserLocalMapper extends BaseMapper<User> {}
```

- [ ] **Step 2: Update system-service application.yml datasource**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/gcrf_main?currentSchema=gcrf_region,public&stringtype=unspecified
    username: postgres
    password: gcrf_secure_2024
```

And the prod env override in `deployment/k8s/10-services.yaml` for gcrf-system:

```yaml
- name: SPRING_DATASOURCE_URL
  value: "jdbc:postgresql://postgresql.edu-infra.svc.cluster.local:5432/gcrf_main?currentSchema=gcrf_region,public&stringtype=unspecified"
```

- [ ] **Step 3: Rewrite UserController to use local mapper**

```java
// backend/system-service/src/main/java/com/gcrf/library/system/controller/UserController.java
package com.gcrf.library.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.system.entity.User;
import com.gcrf.library.system.mapper.UserLocalMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/system/users")
@RequiredArgsConstructor
@Tag(name = "系统用户管理")
public class UserController {

    private final UserLocalMapper userMapper;

    @GetMapping
    public Result<PageResult<UserVO>> getUsers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        Page<User> p = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.like(User::getUsername, keyword).or().like(User::getUserId, keyword);
        }
        if (status != null && !status.isBlank()) w.eq(User::getStatus, status.toUpperCase());
        if (role != null && !role.isBlank()) w.eq(User::getUserType, role.toUpperCase());
        Page<User> result = userMapper.selectPage(p, w);

        PageResult<UserVO> page = PageResult.ofRecords(
            result.getTotal(), pageNum, pageSize,
            result.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return Result.success(page);
    }

    private UserVO toVO(User u) {
        UserVO vo = new UserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUserId());      // user_id is the login name in the UI sense
        vo.setRealName(u.getUsername());    // username is the display name
        vo.setEmail(u.getEmail());
        vo.setPhone(u.getPhone());
        vo.setUserType(u.getUserType());
        vo.setStatus(u.getStatus());
        vo.setLastLoginTime(u.getLastLoginTime());
        vo.setCreatedAt(u.getCreatedAt());
        return vo;
    }

    /** Keep VO as a static inner class for clarity. */
    @lombok.Data
    public static class UserVO {
        private Long id;
        private String username;
        private String realName;
        private String email;
        private String phone;
        private String userType;
        private String status;
        private java.time.LocalDateTime lastLoginTime;
        private java.time.LocalDateTime createdAt;
    }
}
```

- [ ] **Step 4: Delete obsolete Feign client + DTO**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git rm backend/system-service/src/main/java/com/gcrf/library/system/client/UserManagementClient.java
git rm backend/system-service/src/main/java/com/gcrf/library/system/dto/response/UserPageVO.java
# Old UserVO + DTOs no longer reachable — clean up if unused
rm -rf backend/system-service/src/main/java/com/gcrf/library/system/client/
```

- [ ] **Step 5: Remove common-feign from system-service pom**

Edit `backend/system-service/pom.xml`, delete the block added two days ago:

```xml
<!-- DELETE THIS BLOCK -->
<dependency>
    <groupId>com.gcrf.library</groupId>
    <artifactId>common-feign</artifactId>
</dependency>
```

Also delete any `<dependency>` for `spring-cloud-starter-openfeign` if it's not used by other code. Verify:

```bash
grep -rn "@FeignClient\|FeignClient" backend/system-service/src/main/java
```

If none, also delete `spring-cloud-starter-openfeign` and `spring-cloud-starter-loadbalancer` from system pom.

- [ ] **Step 6: Compile + run existing system-service tests**

```bash
cd backend
mvn clean package -pl system-service -am -Dmaven.test.skip=true
```

Expected: BUILD SUCCESS.

- [ ] **Step 7: Smoke test login + system/users**

Run a local prep: bring up testcontainer PG + auth + system services (skip if integration env not handy; trust unit tests). Instead, run a curl trace to verify wire:

```bash
# (after deployment in Task 14, do these in prod)
TOKEN=$(curl -sf -X POST http://192.168.1.19:31080/api/v1/auth/login \
   -H 'Content-Type: application/json' \
   -d '{"username":"admin","password":"admin123"}' \
 | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['accessToken'])")
curl -s -H "Authorization: Bearer $TOKEN" \
   "http://192.168.1.19:31080/api/v1/system/users?pageNum=1&pageSize=3" \
 | python3 -m json.tool | head -20

# Verify NO call to auth-service appears in system logs:
sshpass -p gcrf ssh ... "kubectl logs deployment/gcrf-system --tail=80" | grep -i "UserManagementClient\|auth-service"
# Expected: empty
```

- [ ] **Step 8: Commit**

```bash
git add backend/system-service/pom.xml \
        backend/system-service/src/main/java/com/gcrf/library/system/entity/User.java \
        backend/system-service/src/main/java/com/gcrf/library/system/mapper/UserLocalMapper.java \
        backend/system-service/src/main/java/com/gcrf/library/system/controller/UserController.java \
        backend/system-service/src/main/resources/application.yml \
        deployment/k8s/10-services.yaml
git commit -m "refactor(system): replace UserManagementClient Feign with local gcrf_region.users mapper"
```

---

### Task 14: 一次性数据迁移 SQL + 部署灰度

写迁移脚本；构建 + 三节点 import 新 auth + system 镜像；金丝雀滚动重启。

**Files:**

- Create: `scripts/migrate-auth-users-to-region.sql`
- Create: `/tmp/gcrf-deploy/Dockerfile-auth-service`
- Modify: `deployment/k8s/10-services.yaml` — 已在 Task 6 / 13 完成

- [ ] **Step 1: Write data migration SQL**

```sql
-- scripts/migrate-auth-users-to-region.sql
-- One-shot dblink copy from old auth_service.users to gcrf_region.users.
-- Run during maintenance window AFTER gcrf-auth image has been built but BEFORE rollout.
-- Assumes:
--   - You're connected to gcrf_main DB as a superuser
--   - The old auth_service DB still has the users table
--   - V001 has run on gcrf_region and seeded admin

CREATE EXTENSION IF NOT EXISTS dblink;

-- Set the connection password through psql variable (do NOT inline).
\set OLD_DB_CONN 'host=postgresql port=5432 dbname=gcrf_auth user=postgres password=' :'OLD_DB_PASSWORD'

DO $$
DECLARE
    rows_before INT;
    rows_after INT;
BEGIN
    SELECT count(*) INTO rows_before FROM gcrf_region.users;

    INSERT INTO gcrf_region.users (
        user_id, username, password, email, phone, user_type,
        avatar_url, status, last_login_time, last_login_ip,
        failed_login_count, locked_until, created_at, updated_at, deleted_at
    )
    SELECT
        user_id, username, password, email, phone, user_type,
        avatar_url, status,
        last_login_time::timestamptz, last_login_ip,
        COALESCE(failed_login_count,0), locked_until::timestamptz,
        created_at::timestamptz, updated_at::timestamptz, deleted_at::timestamptz
    FROM dblink(current_setting('OLD_DB_CONN'),
        'SELECT user_id, username, password, email, phone, user_type,
                avatar_url, status, last_login_time, last_login_ip,
                failed_login_count, locked_until, created_at, updated_at, deleted_at
         FROM users')
    AS old(user_id VARCHAR, username VARCHAR, password VARCHAR, email VARCHAR,
           phone VARCHAR, user_type VARCHAR, avatar_url VARCHAR, status VARCHAR,
           last_login_time TIMESTAMP, last_login_ip VARCHAR,
           failed_login_count INT, locked_until TIMESTAMP,
           created_at TIMESTAMP, updated_at TIMESTAMP, deleted_at TIMESTAMP)
    ON CONFLICT (user_id) DO NOTHING;

    SELECT count(*) INTO rows_after FROM gcrf_region.users;

    RAISE NOTICE 'Migration: rows_before=%, rows_after=%, inserted=%',
        rows_before, rows_after, (rows_after - rows_before);
END $$;

-- Reset sequence to max(id)+1 to avoid PK collisions on next insert.
SELECT setval(pg_get_serial_sequence('gcrf_region.users','id'),
              (SELECT COALESCE(MAX(id),0) + 1 FROM gcrf_region.users), false);

-- Sanity: number of users in old DB should match new (subtract admin since V001 seeded it).
SELECT
  (SELECT count(*) FROM dblink(current_setting('OLD_DB_CONN'), 'SELECT 1 FROM users') AS t(x INT)) AS old_count,
  (SELECT count(*) FROM gcrf_region.users) AS new_count;
```

To run during deploy window (in the document, not committed as runtime):

```bash
# On t1, kubectl exec the postgres pod:
PGPASSWORD=<password> psql -h postgresql.edu-infra.svc.cluster.local -U postgres -d gcrf_main \
   -v OLD_DB_PASSWORD=<password> -f /tmp/migrate-auth-users-to-region.sql
```

- [ ] **Step 2: Build auth-service jar + image**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn clean package -pl auth-service -am -Dmaven.test.skip=true
cp auth-service/target/auth-service-1.0.0-SNAPSHOT.jar /tmp/gcrf-deploy/auth-service.jar

cat > /tmp/gcrf-deploy/Dockerfile-auth-service <<'EOF'
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY auth-service.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
EOF

cd /tmp/gcrf-deploy
docker rmi gcrf/auth-service:latest 2>/dev/null
docker build --platform linux/amd64 -t gcrf/auth-service:latest -f Dockerfile-auth-service .
docker save gcrf/auth-service:latest | gzip > auth-service.tar.gz
```

- [ ] **Step 3: Also rebuild system-service (Task 13 changes)**

```bash
mvn clean package -pl system-service -am -Dmaven.test.skip=true
cp system-service/target/system-service-1.0.0-SNAPSHOT.jar /tmp/gcrf-deploy/system-service.jar
cd /tmp/gcrf-deploy
docker rmi gcrf/system-service:latest 2>/dev/null
docker build --platform linux/amd64 -t gcrf/system-service:latest -f Dockerfile-system-service .
docker save gcrf/system-service:latest | gzip > system-service.tar.gz
```

- [ ] **Step 4: Ship + import on all 3 nodes (sequential to avoid SSH lockout)**

```bash
for host in t2@192.168.1.19 t3@192.168.1.21 t1@192.168.1.20; do
  for img in auth-service system-service; do
    sshpass -p gcrf scp -o StrictHostKeyChecking=no -o PreferredAuthentications=password -o NumberOfPasswordPrompts=1 -o ConnectTimeout=10 \
       /tmp/gcrf-deploy/${img}.tar.gz $host:/tmp/
    sleep 2
  done
done

for host in t1@192.168.1.20 t2@192.168.1.19; do
  for img in auth-service system-service; do
    sshpass -p gcrf ssh -o StrictHostKeyChecking=no -o PreferredAuthentications=password -o NumberOfPasswordPrompts=1 -o ConnectTimeout=10 $host \
      "echo gcrf | sudo -S sh -c 'gunzip -c /tmp/${img}.tar.gz | ctr -n k8s.io images import -'"
    sleep 3
  done
done
# t3 sudo pattern is different (per prior notes)
for img in auth-service system-service; do
  sshpass -p gcrf ssh -o StrictHostKeyChecking=no -o PreferredAuthentications=password -o NumberOfPasswordPrompts=1 -o ConnectTimeout=10 t3@192.168.1.21 \
    "printf 'gcrf\n' | sudo -S sh -c 'gunzip -c /tmp/${img}.tar.gz | ctr -n k8s.io images import -'"
  sleep 3
done
```

- [ ] **Step 5: Run data migration on prod PG**

```bash
sshpass -p gcrf ssh -o StrictHostKeyChecking=no -o PreferredAuthentications=password t1@192.168.1.20 << 'SSH'
echo gcrf | sudo -S kubectl cp scripts/migrate-auth-users-to-region.sql gcrf-prod/postgresql-0:/tmp/ -n edu-infra
# Substitute OLD_DB_PASSWORD with the actual gcrf_auth DB password (from gcrf-db-secret).
echo gcrf | sudo -S kubectl exec -n edu-infra postgresql-0 -- bash -c \
  "PGPASSWORD=\$POSTGRES_PASSWORD psql -U postgres -d gcrf_main \
     -v OLD_DB_PASSWORD=\$POSTGRES_PASSWORD -f /tmp/migrate-auth-users-to-region.sql"
SSH
```

Expected output: `Migration: rows_before=1, rows_after=N, inserted=N-1` and `old_count == new_count`.

- [ ] **Step 6: Apply yaml + canary restart gcrf-auth (1 replica scale up to verify)**

```bash
sshpass -p gcrf ssh t1@192.168.1.20 "echo gcrf | sudo -S kubectl apply -f /tmp/10-services.yaml 2>&1 | tail -5"

# Scale auth up to 2 replicas for canary
sshpass -p gcrf ssh t1@192.168.1.20 "echo gcrf | sudo -S kubectl scale deployment/gcrf-auth --replicas=2 -n gcrf-prod"
sshpass -p gcrf ssh t1@192.168.1.20 "echo gcrf | sudo -S kubectl rollout status deployment/gcrf-auth -n gcrf-prod --timeout=180s"

# Verify login on new replica
curl -sf -X POST http://192.168.1.19:31080/api/v1/auth/login \
   -H 'Content-Type: application/json' \
   -d '{"username":"admin","password":"admin123"}' | python3 -m json.tool | head -20

# If 200 + roles=["REGION_ADMIN"] → scale down to 1
sshpass -p gcrf ssh t1@192.168.1.20 "echo gcrf | sudo -S kubectl scale deployment/gcrf-auth --replicas=1 -n gcrf-prod"
```

Expected: login returns `accessToken / refreshToken / roles=["REGION_ADMIN"] / scope="REGION"`.

- [ ] **Step 7: Restart gcrf-system to pick up new datasource + Feign-free image**

```bash
sshpass -p gcrf ssh t1@192.168.1.20 \
  "echo gcrf | sudo -S kubectl rollout restart deployment/gcrf-system -n gcrf-prod && \
   echo gcrf | sudo -S kubectl rollout status deployment/gcrf-system -n gcrf-prod --timeout=180s"
```

- [ ] **Step 8: Commit**

```bash
git add scripts/migrate-auth-users-to-region.sql
git commit -m "ops(infra): one-shot SQL to migrate auth_service.users → gcrf_region.users"
```

---

### Task 15: E2E 验证

跑 test-online.sh 确保通过率 ≥ 96%；额外验证：admin 角色分配 / refresh 旋转 / Feign 链路消失。

**Files:**

- Modify: `docs/deployment/TEST_REPORT_<TODAY>.md` — test-online.sh 自动生成

- [ ] **Step 1: Run test-online.sh baseline**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
bash deployment/k8s/test-online.sh http://192.168.1.19:31080 | tail -30
```

Expected: 通过率 ≥ 96%（基线一致），无新 FAIL。

- [ ] **Step 2: Plan-B1 专属 E2E（用 bash 手测）**

```bash
BASE=http://192.168.1.19:31080

echo "=== 1) 登录 admin → 富响应 ==="
LOGIN=$(curl -sf -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
   -d '{"username":"admin","password":"admin123"}')
echo $LOGIN | python3 -m json.tool | head -25
ACCESS=$(echo $LOGIN | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['accessToken'])")
REFRESH=$(echo $LOGIN | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['refreshToken'])")

echo ""
echo "=== 2) /auth/me 返回当前上下文 ==="
curl -sf -H "Authorization: Bearer $ACCESS" $BASE/api/v1/auth/me | python3 -m json.tool | head -15

echo ""
echo "=== 3) /roles 返回 10 个系统角色 ==="
curl -sf -H "Authorization: Bearer $ACCESS" $BASE/api/v1/roles | python3 -c "import json,sys; d=json.load(sys.stdin); print('roles count:', len(d['data']))"

echo ""
echo "=== 4) /permissions 返回 11 个 ==="
curl -sf -H "Authorization: Bearer $ACCESS" $BASE/api/v1/permissions | python3 -c "import json,sys; d=json.load(sys.stdin); print('perms count:', len(d['data']))"

echo ""
echo "=== 5) Refresh 旋转 — 旧 refresh 不可再用 ==="
REFRESH2=$(curl -sf -X POST $BASE/api/v1/auth/refresh -H 'Content-Type: application/json' \
   -d "{\"refreshToken\":\"$REFRESH\"}" | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['refreshToken'])")
echo "new refresh != old: $([ "$REFRESH" != "$REFRESH2" ] && echo YES || echo NO)"
# Try old refresh again — should fail
OLD_RESP=$(curl -s -X POST $BASE/api/v1/auth/refresh -H 'Content-Type: application/json' \
   -d "{\"refreshToken\":\"$REFRESH\"}")
echo "old refresh re-use rejected: $(echo $OLD_RESP | python3 -c "import json,sys; print(json.load(sys.stdin).get('code') != 200)")"

echo ""
echo "=== 6) /users 走 system-service 本地 mapper（不经 Feign）==="
sshpass -p gcrf ssh -o StrictHostKeyChecking=no -o PreferredAuthentications=password -o NumberOfPasswordPrompts=1 -o ConnectTimeout=10 t1@192.168.1.20 \
  "echo gcrf | sudo -S kubectl logs -n gcrf-prod deployment/gcrf-system --tail=200 --since=2m | grep -i 'UserManagementClient\|auth-service.*GET' | head -3"
# Expected: empty output (Feign 链路已拆)
curl -sf -H "Authorization: Bearer $ACCESS" "$BASE/api/v1/system/users?pageNum=1&pageSize=3" | python3 -c "import json,sys; d=json.load(sys.stdin); print('system/users HTTP code:', d.get('code'), 'records:', len(d.get('data',{}).get('records',[])))"
```

Expected: 6 checks all pass; old refresh rejected; system/users no Feign call.

- [ ] **Step 3: 浏览器手测**

打开 `http://192.168.1.19:31080`：

1. 用 admin / admin123 登录
2. 进入"系统管理 → 角色权限"页 — 应见 10 个角色 + 点击展开见权限列表
3. 进入"系统管理 → 用户管理"页 — 行末"分配角色"按钮打开抽屉，admin 已有 REGION_ADMIN
4. 在抽屉里尝试给 admin 加 LIBRARIAN — 应成功；列表刷新显示 2 个角色
5. 撤销 LIBRARIAN — 应成功；回到 1 个

- [ ] **Step 4: 保存测试报告**

测试通过则 commit 报告：

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
DATE=$(date +%Y%m%d)
ls -la docs/deployment/TEST_REPORT_${DATE}.md
git add docs/deployment/TEST_REPORT_${DATE}.md
git commit -m "test(docs): Plan-B1 deployment test report — 96%+ pass, IAM E2E verified"
```

---

### Task 16: Tag + 下线 auth_service DB

观察 1 周后下线旧 DB；tag v1.3.0-plan-B1。

**Files:**

- (none — operational)

- [ ] **Step 1: Tag**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git tag -a v1.3.0-plan-B1 -m "Plan-B1: 核心 IAM + RBAC

收口 M1 第 3 项交付（IAM SSO 账号密码部分）。Plan-B2（微信双模式公众号）随 M2 wechat-service 完成。

✅ JWT 富化（tenant/roles[]/scope claim），HS512 升 512-bit key
✅ 10 系统角色 + 11 模块权限 seed，REGION_ADMIN-only 分配工作流
✅ users 表中心化到 gcrf_region.users（auth_service DB 待下线）
✅ 标准 refresh token 30d + Redis 旋转防重放
✅ /auth/me 当前用户上下文
✅ 区域后台角色管理页 + 用户角色分配抽屉
✅ 拆掉 system-service→auth-service Feign 链路（昨日 4 层 bug 来源消除）

明确推迟：
- Plan-B2 微信公众号 OAuth + 小程序 wx.login + 双模式（M2）
- 自定义角色 CRUD + 功能模块×数据范围矩阵 UI（M2）
- 通用 @DataScope AOP / DataPermissionHandler（M2）"

git push origin master
git push origin v1.3.0-plan-B1
```

- [ ] **Step 2: 1 周观察期后下线 auth_service DB**

```
观察清单：
- gcrf-auth pod 无 OOM / crash 1 周
- 登录 / refresh / 用户管理无 ticket
- test-online.sh 每日跑通过率 ≥ 96%

满足后：
sshpass -p gcrf ssh t1@192.168.1.20 \
  "echo gcrf | sudo -S kubectl exec -n edu-infra postgresql-0 -- \
     psql -U postgres -d postgres -c 'DROP DATABASE gcrf_auth'"
```

不在本 plan 内做 drop，记录在 followups 文档。

- [ ] **Step 3: Followups 记录**

Append to `docs/followups.md`（创建若没有）:

```markdown
## Plan-B1 收尾

- [ ] 2026-05-18 起 1 周观察期，无问题后 `DROP DATABASE gcrf_auth`
- [ ] Plan-B2（微信双模式公众号 OAuth + 小程序 wx.login）随 M2 wechat-service 启动
- [ ] 自定义角色 CRUD（功能模块×数据范围矩阵 UI）随 M2 校园系统升级
```

```bash
git add docs/followups.md
git commit -m "docs(docs): record Plan-B1 follow-ups and drop-DB window"
git push
```

---

## Self-Review

### Spec coverage

| Spec 章节                                                        | 实现 task                                         |
| ---------------------------------------------------------------- | ------------------------------------------------- |
| §1 架构（auth-service 原地升级，common-security 加 filter/注解） | Task 1-4 + 6-10                                   |
| §2.1 users 表迁移 + 扩展字段                                     | Task 5（建）+ Task 6（实体）+ Task 14（数据迁移） |
| §2.2 auth_role + 10 系统角色 seed                                | Task 5                                            |
| §2.3 auth_permission + 11 权限 seed                              | Task 5                                            |
| §2.4 auth_role_permission + 默认映射                             | Task 5                                            |
| §2.5 auth_user_role + UNIQUE NULLS NOT DISTINCT                  | Task 5                                            |
| §2.6 数据迁移（dblink，~10 行）                                  | Task 14                                           |
| §3.1 IAM 端点 (login/refresh/logout/me/users/roles/permissions)  | Task 8-10                                         |
| §3.2 业务服务侧 SecurityContextFilter                            | Task 3 + 11                                       |
| §3.3 前端角色管理 + 用户角色抽屉                                 | Task 12                                           |
| §4.1 Access Token 富化 + HS512 512-bit                           | Task 1 + 8                                        |
| §4.2 Refresh Token Redis 30d 旋转                                | Task 9                                            |
| §4.3 Permissions 不进 JWT，业务侧 lazy 查 + Redis 缓存           | Task 4 (PermissionLookup) + Task 7 (@Cacheable)   |
| §4.4 三条核心流                                                  | Task 8-9                                          |
| §4.5 SecurityContextFilter 与 TenantContextFilter 协作           | Task 3 (order config)                             |
| §5 数据范围（SCHOOL 复用 Plan-A，REGION 显式 LIKE）              | Task 2 (Scope) + Task 4 (@RequireScope)           |
| §6 升级序列 + 任务估算                                           | Task 5-7 / 8-10 / 11-12 / 13-14 / 15-16           |

### Placeholder scan

✅ 0 TBD/TODO/FIXME；所有 step 含完整代码块。1 处需手工填值：`scripts/migrate-auth-users-to-region.sql` 的 `OLD_DB_PASSWORD` — 明确说明从 gcrf-db-secret 读，不算占位。

### Type consistency

| 跨 task 引用                                                                                                   | 一致性 |
| -------------------------------------------------------------------------------------------------------------- | ------ |
| `Scope` 枚举 (Task 2) — `@RequireScope(SCOPE)` (Task 4) — `SecurityContextHolder.hasScope` (Task 2)            | ✅     |
| `SecurityContext.builder()` 字段（userId/tenant/roles/scope/orgPath）— 用于 Filter (Task 3) / Aspect (Task 4)  | ✅     |
| `PermissionLookup` 接口 (Task 4) — `AuthPermissionLookup implements PermissionLookup` (Task 7)                 | ✅     |
| `RoleService.assignRole(userId, roleId, schoolId, expiresAt, operatorId)` (Task 7) — Controller 调用 (Task 10) | ✅     |
| `RefreshTokenService.issue/consume/revoke` (Task 9 stub→full) — AuthService 调用 (Task 8/9)                    | ✅     |
| `LoginResponse.accessToken / refreshToken / roles / tenant / scope / permissions` — front-end 字段 (Task 12)   | ✅     |

### Scope check

16 task / ~80 atomic steps / ~1.5-2 周。Spec 估算一致。单一可演示交付物（IAM/RBAC 基线）。✅ Plan-sized.

### Known limitations (intentional)

- M1 仅 REGION_ADMIN 可分配角色，SCHOOL_ADMIN 工作流 punt M2
- 自定义角色 CRUD（功能模块×数据范围矩阵）punt M2
- access token 黑名单 punt（30min 自然过期可接受；wechat-service 上线时再做 jti 黑名单）
- 通用 `@DataScope` AOP punt M2

---

## Execution Handoff

Plan complete and saved to `docs/plans/2026-05-11-plan-B1-iam-rbac.md`. Two execution options:

**1. Subagent-Driven (推荐)** — 每 task 派 fresh subagent + spec/quality 两阶段 review。同会话、快速迭代，预计 ~1.5-2 周 wall time。

**2. Inline Execution** — 当前会话内按 executing-plans 流程批量执行，含 checkpoint。

哪种方式？
