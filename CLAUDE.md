# GCRF Library Management System - Development Guidelines

**Project**: 国创睿峰智能图书馆管理系统 (GCRF Intelligent Library Management System)
**Architecture**: Microservices (Spring Cloud) + Vue 3 Frontend
**Last Updated**: 2025-10-25

---

## Project Overview

**Stack**:

- Backend: Spring Boot 3.2.2 + Spring Cloud 2023.0.0 + Spring Cloud Alibaba 2023.0.1.0
- Frontend: Vue 3 + Vite + Element Plus
- Infrastructure: PostgreSQL 15+, Redis 7.x, RabbitMQ 3.12.x, Nacos 2.3.x, MinIO

---

## Philosophy & Core Principles

### Incremental Progress Over Big Bangs

- Every commit must compile and pass tests
- Small, focused changes - single responsibility per commit
- Working code always - no half-finished features
- Test-driven when possible

### Learning from Existing Code

Before implementing:

1. Find 3 similar components in codebase
2. Study existing patterns
3. Use same libraries
4. Follow conventions

### Pragmatic Over Dogmatic

- Adapt to project reality
- Question abstractions
- Boring is better
- Context matters

### Clear Intent Over Clever Code

- Self-documenting code
- Explicit over implicit
- Single responsibility
- No magic

---

## Critical Technical Constraints

### ⚠️ Java Version: MUST Use Java 21

```bash
# Always set JAVA_HOME before Maven commands
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean compile
```

**Why**: Spring Boot 3.2.2 requires Java 21 features.

### ⚠️ Database: PostgreSQL ONLY (NOT MySQL)

**Why**: Uses PostgreSQL-specific features (JSONB, array types, partitioning, full-text search)

### ⚠️ Dependency Versions (Locked - DO NOT Change)

```xml
<spring-boot.version>3.2.2</spring-boot.version>
<spring-cloud.version>2023.0.0</spring-cloud.version>
<spring-cloud-alibaba.version>2023.0.1.0</spring-cloud-alibaba.version>
<mybatis-plus.version>3.5.9</mybatis-plus.version>
<postgresql.version>42.7.1</postgresql.version>
```

---

## Project Structure

### Backend (`/backend`)

```
backend/
├── common/                    # 共享模块
│   ├── common-core/          # 核心工具、统一响应、异常
│   ├── common-web/           # Web配置、拦截器
│   ├── common-security/      # JWT、Security
│   └── common-mybatis/       # MyBatis Plus配置
├── gateway-service/          # API网关 (8080)
├── auth-service/             # 认证服务 (8081)
├── book-service/             # 图书服务 (8082)
├── circulation-service/      # 流通服务 (8083)
├── reader-service/           # 读者服务 (8084)
├── system-service/           # 系统服务 (8085)
└── notification-service/     # 通知服务 (8086)
```

**Key Files**:

- `docs/architecture/architect.md` - **AUTHORITATIVE** technical spec (1570 lines)
- `docs/specs/2026-04-30-regional-platform-master-design.md` - **AUTHORITATIVE** for regional platform (M1+)
- `backend/IMPLEMENTATION_PLAN.md` - Delete when complete (一般工作 plan 放 `docs/plans/`)

### Frontend (`/web-admin`)

```
web-admin/
├── src/
│   ├── views/               # 页面组件
│   ├── components/          # 通用组件
│   ├── api/                 # API请求封装
│   ├── store/               # Pinia状态管理
│   ├── router/              # Vue Router
│   ├── mock/                # MSW Mock数据
│   │   ├── handlers/        # ⚠️ CRITICAL: 返回格式必须与前端一致
│   │   └── data/
│   └── utils/
└── Dockerfile               # Nginx生产部署
```

**⚠️ Document Authority**:

- **docs/architecture/architect.md** — 单校 GCRF 后端架构权威（1570 行，PostgreSQL schemas）
- **docs/specs/2026-04-30-regional-platform-master-design.md** — 区域平台主 spec（M1+ 权威）
- **docs/plans/** — 各 plan 实施细节（含已完成 v1.1.0-plan-A / v1.2.0-plan-C1 / v1.2.1-plan-C1.5）
- **docs/archives/legacy-2026-04/** — Phase-1 旧文档（含 ARCHITECTURE.md v2.2 overview / face-api 等）
- 冲突时：单校话题 follow architect.md；区域平台话题 follow master-design.md

---

## Development Process

### 1. Planning (For Complex Tasks: 3+ Steps)

1. Create/Update `IMPLEMENTATION_PLAN.md`
2. Break into 3-7 stages with:
   - Specific deliverable
   - Testable success criteria
   - Dependencies
   - Status: Not Started → In Progress → Complete
3. DELETE when all stages complete

### 2. Implementation Flow (TDD Cycle)

```
Understand → Test (red) → Implement (green) → Refactor → Verify → Commit
```

**Backend Example**:

```bash
# 1. Study existing patterns
ls -la reader-service/src/main/java/

# 2. Write test first (RED)
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl reader-service

# 3. Implement (GREEN)
# 4. Refactor & verify

# 5. Commit
git commit -m "feat(reader): implement reader registration

- Add ReaderService.register()
- Validate uniqueness
Refs: IMPLEMENTATION_PLAN.md Stage 2"
```

### 3. When Stuck (Maximum 3 Attempts Rule)

**⚠️ CRITICAL**: After 3 failed attempts, STOP and reassess.

**After 3 Failures**:

1. Document all attempts with errors and hypotheses
2. Research 2-3 similar implementations
3. Question fundamentals (right abstraction? simpler approach?)
4. Try different angle (different library/pattern/layer)

---

## Backend Development Standards

### Service Structure

```
[service-name]/
├── src/main/java/com/gcrf/library/[service]/
│   ├── Application.java              # Spring Boot启动类
│   ├── controller/*Controller.java   # @RestController
│   ├── service/*Service.java         # 接口
│   ├── service/impl/*ServiceImpl     # @Service实现
│   ├── mapper/*Mapper.java           # extends BaseMapper<Entity>
│   ├── domain/
│   │   ├── entity/*Entity.java       # @TableName
│   │   ├── dto/*DTO.java             # 请求
│   │   └── vo/*VO.java               # 响应
│   ├── config/*Config.java
│   └── constant/*Constants.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── application-prod.yml
└── src/test/java/
```

### Code Rules

**Controller**:

- Use `@RequiredArgsConstructor` (no `@Autowired`)
- All methods return `Result<T>` wrapper
- Use `@Valid` for validation
- Keep thin - delegate to service

**Service**:

- Use `@Slf4j` for logging
- Use `@Transactional(rollbackFor = Exception.class)` for writes
- Use `LambdaQueryWrapper` for type-safe queries
- Throw `BusinessException` for business errors
- Throw `SystemException` for infrastructure failures

**Entity**:

- `@TableName("table_name")` - snake_case
- `@TableId(type = IdType.AUTO)` for PKs
- `@TableField(fill = FieldFill.INSERT)` for audit fields
- `@TableLogic` for soft delete
- Use `LocalDateTime` (not `Date`)

**DTO/VO**:

- DTO: Requests with validation (`@NotBlank`, `@Email`, etc.)
- VO: Responses with only needed fields
- Never expose entity directly
- Include conversion methods (`toEntity()`, `from()`)

### Maven Commands

```bash
# Always set Java 21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Compile/Test/Package one module
mvn clean compile -pl reader-service
mvn test -pl reader-service
mvn clean package -pl reader-service -am

# Run service
cd reader-service
mvn spring-boot:run
```

---

## Frontend Development Standards

### Vue Component Rules

- Use `<script setup>` (Composition API)
- Use `ref()` for primitives, `reactive()` for objects
- Use `v-loading` for async states
- Handle errors with `ElMessage`
- API calls need `try/catch/finally`

### API Pattern (`src/api/*.js`)

```javascript
import request from "@/utils/request";

export function getReaderPage(params) {
  return request({ url: "/api/v1/readers", method: "get", params });
}

export function createReader(data) {
  return request({ url: "/api/v1/readers", method: "post", data });
}
```

**Rules**:

- One file per resource
- Named exports (not default)
- JSDoc comments
- `params` for query, `data` for body

### ⚠️ MSW Mock Handlers (CRITICAL)

**MUST match frontend component expectations EXACTLY**

```javascript
// src/mock/handlers/readers.js
http.get("/api/v1/readers", ({ request }) => {
  return HttpResponse.json({
    code: 200,
    data: {
      records, // ⚠️ MUST match component: data.records or data.list?
      total: 200,
      pageNum,
      pageSize,
    },
  });
});
```

**Verification**:

1. Check component: `res.data.records` or `res.data.list`?
2. Match mock response EXACTLY
3. Test in browser DevTools (F12 → Network)
4. Clear Service Worker cache after changes

**Service Worker Cache**:

```bash
# Hard refresh (best)
Ctrl+Shift+Delete → Clear cache/cookies

# Incognito mode (quick test)
# DevTools → Application → Service Workers → Unregister
```

### Build & Deploy

```bash
# Development
npm run dev        # http://localhost:3011

# Production
npm run build
npm run preview

# Docker (amd64 for deployment)
docker build --platform linux/amd64 -t gcrf-library-web-admin:amd64 .
docker save gcrf-library-web-admin:amd64 | gzip > gcrf-web-admin-v1.0.3.tar.gz
gunzip -c gcrf-web-admin-v1.0.3.tar.gz | docker load
docker run -d --name gcrf-web-admin -p 3011:80 gcrf-library-web-admin:amd64
```

---

## Common Pitfalls & Solutions

### 1. Database Mismatch

**Symptom**: SQL errors, missing JSONB
**Solution**: Use PostgreSQL 42.7.1, NOT MySQL

### 2. Java Version Errors

**Symptom**: Maven compile fails
**Solution**: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`

### 3. Mock Data Mismatch

**Symptom**: Frontend "No data" despite 200 OK
**Solution**: Mock returns `data.list` but component expects `data.records`
**Diagnosis**: F12 → Network → Check response structure

### 4. Service Worker Cache

**Symptom**: Changes not reflected after deploy
**Solution**: Clear cache or use incognito mode

### 5. MyBatis-Plus Pagination Not Working

**Symptom**: Returns all rows instead of page
**Solution**: Add `MybatisPlusInterceptor` with `PaginationInnerInterceptor(DbType.POSTGRE_SQL)`

### 6. Nacos Connection Refused

**Solution**: Start Nacos first (`./startup.sh -m standalone`)

### 7. CORS Errors

**Solution**: Add `CorsWebFilter` in Gateway config

---

## Git Workflow

### Commit Message Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**: `feat`, `fix`, `refactor`, `test`, `docs`, `style`, `chore`

**Example**:

```bash
git commit -m "feat(reader): add reader registration API

- Implement ReaderService.register()
- Validate card uniqueness
- Add unit tests

Refs: IMPLEMENTATION_PLAN.md Stage 2"
```

### Branch Strategy

```
main              # Production
├── develop       # Development
    ├── feature/reader-management
    └── bugfix/jwt-expiration
```

---

## Quality Gates (Every Commit)

### Definition of Done

- [ ] Code compiles without errors
- [ ] All existing tests pass
- [ ] New tests written
- [ ] Code follows conventions
- [ ] No linter warnings
- [ ] Clear commit message
- [ ] Matches IMPLEMENTATION_PLAN.md (if applicable)
- [ ] No TODOs without issue numbers
- [ ] Manual testing done (UI changes)

### Code Review Checklist

**Backend**:

- [ ] `@RequiredArgsConstructor` (no `@Autowired`)
- [ ] Write operations have `@Transactional`
- [ ] Use `BusinessException` / `SystemException`
- [ ] Use `LambdaQueryWrapper`
- [ ] Entities use `LocalDateTime`
- [ ] DTOs have validation

**Frontend**:

- [ ] `<script setup>` syntax
- [ ] API calls have `try/catch`
- [ ] Loading states shown
- [ ] Error messages with `ElMessage`
- [ ] Mock data matches component expectations
- [ ] No hardcoded URLs

---

## Emergency Procedures

### Build is Broken

```bash
java -version  # Should be 21
mvn clean
rm -rf ~/.m2/repository/com/gcrf
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean install
```

### Service Won't Start

```bash
lsof -i :8084                    # Check port
curl http://localhost:8848/nacos/  # Check Nacos
psql -h localhost -U postgres -d gcrf_reader -c "SELECT 1"  # Check DB
redis-cli ping                   # Check Redis
```

### Frontend Blank Page

```bash
# F12 → Console (check errors)
# F12 → Application → Service Workers → Unregister
# Ctrl+Shift+Delete → Clear cache
# F12 → Network → XHR (check response format)
npm run build && docker restart gcrf-web-admin
```

---

## Useful Commands Reference

### Backend (Maven)

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn clean compile               # Compile all
mvn clean compile -pl reader-service  # One module
mvn test -pl reader-service     # Test one module
mvn clean package -DskipTests   # Package all (use sparingly)
mvn dependency:tree -pl reader-service
cd reader-service && mvn spring-boot:run
```

### Frontend (npm)

```bash
npm install
npm run dev
npm run build
npm run preview
npm run lint
```

### Docker

```bash
docker build --platform linux/amd64 -t gcrf-library-web-admin:amd64 .
docker save gcrf-library-web-admin:amd64 | gzip > gcrf-web-admin.tar.gz
gunzip -c gcrf-web-admin.tar.gz | docker load
docker run -d --name gcrf-web-admin -p 3011:80 gcrf-library-web-admin:amd64
docker logs -f gcrf-web-admin
docker stop gcrf-web-admin && docker rm gcrf-web-admin
```

### PostgreSQL

```bash
psql -h localhost -p 5432 -U postgres -d gcrf_reader
\l           # List databases
\c gcrf_reader  # Connect
\dt          # List tables
\d reader    # Describe table
```

---

## Documentation & Resources

### Internal Docs

- **architect.md** (`/docs/architecture/architect.md`) - **AUTHORITATIVE** 单校后端架构 (1570 行)
- **regional-platform-master-design** (`/docs/specs/2026-04-30-*-design.md`) - **AUTHORITATIVE** 区域平台主 spec (M1+)
- **plans** (`/docs/plans/`) - 各 plan 实施细节
- **PRD** (`/docs/prd/`) - 4 平台产品文档
- **ADR** (`/docs/adr/`) - 架构决策记录（005 个）
- **archives** (`/docs/archives/legacy-2026-04/`) - 旧 Phase-1 文档归档（含 ARCHITECTURE.md v2.2 overview）
- **All documentation**: `/docs/` directory (centralized location)

### External Docs

- Spring Boot 3.2.2: https://docs.spring.io/spring-boot/docs/3.2.2/reference/html/
- Spring Cloud 2023.0.0: https://docs.spring.io/spring-cloud/docs/2023.0.0/reference/html/
- MyBatis-Plus: https://baomidou.com/pages/24112f/
- Vue 3: https://vuejs.org/guide/
- Element Plus: https://element-plus.org/en-US/
- MSW: https://mswjs.io/docs/

---

## Final Reminders

**ALWAYS**:

- Commit working code incrementally
- Use Java 21 for Maven builds
- Match mock data structure to frontend expectations
- Clear Service Worker cache after deployment
- Follow existing patterns in codebase
- Update IMPLEMENTATION_PLAN.md status
- Delete IMPLEMENTATION_PLAN.md when complete

**NEVER**:

- Use `--no-verify` to bypass hooks
- Disable tests instead of fixing
- Commit code that doesn't compile
- Change dependency versions without review
- Assume - verify with existing code
- Continue after 3 failed attempts (reassess first)

**WHEN IN DOUBT**:

- Study 3 similar implementations in codebase
- Read docs/architecture/architect.md (authoritative doc)
- Choose the boring, obvious solution
- Ask for help after documenting attempts

---

**Last Updated**: 2025-10-25
**Project Version**: 1.0.0-SNAPSHOT
**Phase**: Phase 1 - Foundation & Common Modules

- 项目所有文档放在doc文件夹下，开发计划文档放在DevPlan文件夹
