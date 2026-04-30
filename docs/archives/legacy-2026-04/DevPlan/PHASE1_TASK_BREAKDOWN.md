# GCRF Library Management System - Phase 1 Task Breakdown & Agent Assignment

**Phase**: Phase 1 - 核心服务完成 (Week 1-4)
**Created**: 2025-11-03
**Target**: 完成 Book/Circulation/Reader/System 四大核心服务
**Overall Goal**: Backend 42% → 85%

---

## 📋 Phase 1 Overview

### Objectives

- ✅ Complete Book Service (30% → 100%)
- ✅ Complete Circulation Service (20% → 100%)
- ✅ Complete Reader Service (25% → 100%)
- ✅ Complete System Service core features (15% → 60%)
- ✅ Create OpenAPI 3.0 specifications for all services
- ✅ Dockerize all completed services
- ✅ Achieve 80%+ test coverage

### Success Metrics

- 90 new API endpoints implemented
- 4 services fully functional
- All services containerized
- Unit test coverage ≥ 80%
- Integration tests passing
- Swagger UI documentation available

---

## 🗓️ Week 1: Book Service (30% → 100%)

### Day 1-2: OpenAPI Specifications & Architecture

**Primary Agent**: `backend-architect`
**Support Agents**: `api-documenter`, `java-pro`

#### Tasks:

1. **Create OpenAPI 3.0 Specifications** (All 7 Services)
   - Location: `DevPlan/05_API_SPECIFICATIONS/`
   - Files to create:
     - `gateway-service-api.yaml`
     - `auth-service-api.yaml`
     - `book-service-api.yaml` ⭐ Priority
     - `circulation-service-api.yaml`
     - `reader-service-api.yaml`
     - `system-service-api.yaml`
     - `notification-service-api.yaml`
   - Include: schemas, paths, security, examples
   - Reference: `DevPlan/05_API_SPECIFICATIONS/README.md`

2. **Review Book Service Architecture**
   - Analyze existing code structure
   - Identify missing components
   - Design database schema extensions (if needed)
   - Plan MinIO integration for file uploads

**Deliverables**:

- [ ] 7 OpenAPI 3.0 YAML files
- [ ] Book Service architecture review document
- [ ] Database migration scripts (if needed)

**Agent Assignment**:

```
backend-architect:
  - Lead: OpenAPI spec design
  - Review: Microservice boundaries
  - Design: MinIO file storage integration

api-documenter:
  - Create: All 7 OpenAPI specs from existing controllers
  - Validate: OpenAPI 3.0 compliance
  - Generate: Swagger UI documentation

java-pro:
  - Review: Existing Book Service code
  - Identify: Code quality issues
  - Recommend: Refactoring opportunities
```

---

### Day 3-5: Book Service API Implementation

**Primary Agent**: `fastapi-pro` (Backend development)
**Support Agents**: `database-architect`, `test-automator`

#### Tasks to Implement (8 new APIs):

1. **Book Category Management** (3 APIs)

   ```
   GET    /api/v1/books/categories          - 查询图书分类列表
   POST   /api/v1/books/categories          - 创建图书分类
   PUT    /api/v1/books/categories/{id}     - 更新图书分类
   DELETE /api/v1/books/categories/{id}     - 删除图书分类
   ```

2. **Inventory Management** (2 APIs)

   ```
   GET    /api/v1/books/{id}/inventory      - 查询图书库存
   PUT    /api/v1/books/{id}/inventory      - 调整图书库存
   ```

3. **Book Search** (1 API with PostgreSQL full-text search)

   ```
   POST   /api/v1/books/search              - 全文检索图书
   ```

4. **File Upload (MinIO Integration)** (2 APIs)
   ```
   POST   /api/v1/books/{id}/cover          - 上传图书封面
   POST   /api/v1/books/{id}/pdf            - 上传图书PDF
   GET    /api/v1/books/{id}/download       - 下载图书文件
   ```

**Implementation Steps per API**:

1. Create DTO classes (Request/Response)
2. Create/Update Entity classes
3. Create/Update Mapper interfaces
4. Implement Service layer logic
5. Implement Controller endpoints
6. Write unit tests (JUnit 5 + Mockito)
7. Write integration tests
8. Update OpenAPI annotations

**Deliverables**:

- [ ] 8 new API endpoints functional
- [ ] MinIO client configuration
- [ ] PostgreSQL full-text search setup
- [ ] Unit tests (80%+ coverage)
- [ ] Integration tests

**Agent Assignment**:

```
fastapi-pro (java-pro):
  - Implement: All 8 Book Service APIs
  - Code: Follow Spring Boot 3.2.2 best practices
  - Integrate: MinIO for file storage
  - Setup: PostgreSQL full-text search (ts_vector)

database-architect:
  - Design: book_category table schema
  - Design: book_inventory table schema
  - Create: PostgreSQL full-text search indexes
  - Create: Migration scripts

test-automator:
  - Write: Unit tests for all new services
  - Write: Integration tests for API endpoints
  - Setup: Test data fixtures
  - Verify: 80%+ test coverage
```

---

### Day 6-7: Book Service Dockerization & Testing

**Primary Agent**: `deployment-engineer`
**Support Agents**: `devops-troubleshooter`, `test-automator`

#### Tasks:

1. **Create Dockerfile for Book Service**
   - Multi-stage build (deps → build → runtime)
   - Base image: `eclipse-temurin:21-jre-alpine`
   - Optimize layers for faster builds
   - Include health checks

2. **Update docker-compose.yml**
   - Add book-service configuration
   - Configure environment variables
   - Setup depends_on (postgres, nacos, redis)
   - Configure networks and volumes

3. **Testing & Validation**
   - Run all unit tests
   - Run integration tests
   - Test Docker container startup
   - Verify Nacos registration
   - Test API endpoints via Gateway

**Deliverables**:

- [ ] `backend/book-service/Dockerfile`
- [ ] Updated `docker-compose.yml`
- [ ] All tests passing (144+ tests)
- [ ] Docker image builds successfully
- [ ] Service registers with Nacos
- [ ] Swagger UI accessible at `http://localhost:8082/swagger-ui.html`

**Agent Assignment**:

```
deployment-engineer:
  - Create: Optimized Dockerfile
  - Update: docker-compose.yml
  - Configure: CI/CD pipeline for book-service
  - Test: Container startup and health checks

devops-troubleshooter:
  - Debug: Any container startup issues
  - Verify: Nacos service discovery
  - Check: PostgreSQL connectivity
  - Monitor: Resource usage

test-automator:
  - Run: Full test suite (mvn clean test)
  - Verify: Integration tests pass
  - Generate: Test coverage report
  - Document: Testing results
```

---

## 🗓️ Week 2: Circulation Service (20% → 100%)

### Day 1-3: Reservation Management

**Primary Agent**: `java-pro`
**Support Agents**: `database-architect`, `test-automator`

#### Tasks to Implement (3 new APIs):

```
POST   /api/v1/circulation/reserve              - 预约图书
DELETE /api/v1/circulation/reserve/{id}         - 取消预约
GET    /api/v1/circulation/reserves             - 查询预约列表 (分页)
GET    /api/v1/circulation/reserves/{id}        - 查询预约详情
```

**Business Logic**:

- Check book availability before reservation
- Max 3 active reservations per reader
- Reservation expires after 3 days
- Notify reader when book available
- Update book status (Available → Reserved)

**Database Schema**:

```sql
CREATE TABLE circulation_reserve (
    id BIGSERIAL PRIMARY KEY,
    reader_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    reserve_date TIMESTAMP NOT NULL,
    expire_date TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, FULFILLED, CANCELLED, EXPIRED
    notify_sent BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reader_id) REFERENCES reader(id),
    FOREIGN KEY (book_id) REFERENCES book(id)
);
```

**Deliverables**:

- [ ] 4 Reservation APIs implemented
- [ ] Database migration script
- [ ] Unit tests (80%+ coverage)
- [ ] Integration tests

**Agent Assignment**:

```
java-pro:
  - Implement: Reservation management logic
  - Handle: Business rule validation
  - Implement: Expiration logic
  - Write: Service layer + Controller

database-architect:
  - Design: circulation_reserve table
  - Create: Indexes for performance
  - Create: Migration scripts
  - Optimize: Query performance

test-automator:
  - Write: Unit tests for ReservationService
  - Write: Integration tests for Reservation APIs
  - Test: Edge cases (max reservations, expiration)
  - Verify: Test coverage ≥ 80%
```

---

### Day 4-6: Overdue & Fine Management

**Primary Agent**: `java-pro`
**Support Agents**: `database-architect`, `test-automator`

#### Tasks to Implement (4 new APIs):

```
GET    /api/v1/circulation/overdue              - 查询逾期记录 (分页)
POST   /api/v1/circulation/overdue/{recordId}/fine  - 计算罚金
POST   /api/v1/circulation/fine/{fineId}/pay   - 支付罚金
GET    /api/v1/circulation/fines               - 查询罚金记录
POST   /api/v1/circulation/batch-return        - 批量归还
```

**Business Logic**:

- Calculate fine: 1.0 CNY per day overdue
- Max fine: 50.0 CNY per book
- Cannot borrow new books with unpaid fines
- Support multiple payment methods (cash, card, online)
- Batch return with transaction consistency

**Database Schema**:

```sql
CREATE TABLE circulation_fine (
    id BIGSERIAL PRIMARY KEY,
    record_id BIGINT NOT NULL,
    reader_id BIGINT NOT NULL,
    fine_amount NUMERIC(10,2) NOT NULL,
    overdue_days INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL, -- UNPAID, PAID, WAIVED
    payment_method VARCHAR(20),  -- CASH, CARD, ONLINE, NULL
    paid_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (record_id) REFERENCES circulation_record(id),
    FOREIGN KEY (reader_id) REFERENCES reader(id)
);
```

**Deliverables**:

- [ ] 5 Fine management APIs implemented
- [ ] Database migration script
- [ ] Transaction consistency guaranteed
- [ ] Unit tests (80%+ coverage)
- [ ] Integration tests

**Agent Assignment**:

```
java-pro:
  - Implement: Fine calculation logic
  - Implement: Payment processing
  - Implement: Batch return with @Transactional
  - Handle: Edge cases (max fine, concurrent updates)

database-architect:
  - Design: circulation_fine table
  - Optimize: Indexes for overdue queries
  - Ensure: ACID properties for batch operations
  - Create: Migration scripts

test-automator:
  - Write: Unit tests for FineService
  - Write: Integration tests for Fine APIs
  - Test: Transaction rollback scenarios
  - Test: Concurrent payment handling
```

---

### Day 7: Statistics & Dockerization

**Primary Agent**: `deployment-engineer`
**Support Agents**: `java-pro`, `data-scientist`

#### Tasks:

1. **Statistics APIs** (5 new APIs)

   ```
   GET /api/v1/circulation/statistics/overview      - 流通总览
   GET /api/v1/circulation/statistics/by-month      - 按月统计
   GET /api/v1/circulation/statistics/top-books     - 热门图书
   GET /api/v1/circulation/statistics/by-reader     - 按读者统计
   GET /api/v1/circulation/statistics/by-category   - 按分类统计
   ```

2. **Dockerization**
   - Create Dockerfile
   - Update docker-compose.yml
   - Test container startup

**Deliverables**:

- [ ] 5 Statistics APIs implemented
- [ ] Dockerfile for circulation-service
- [ ] Updated docker-compose.yml
- [ ] All 17 Circulation APIs functional
- [ ] All tests passing
- [ ] Docker image builds successfully

**Agent Assignment**:

```
data-scientist:
  - Design: Statistical aggregation queries
  - Optimize: Query performance with PostgreSQL
  - Implement: Caching strategy (Redis)
  - Create: Statistical reports

java-pro:
  - Implement: Statistics Service layer
  - Implement: Controller endpoints
  - Add: Caching annotations (@Cacheable)
  - Write: Unit tests

deployment-engineer:
  - Create: Dockerfile for circulation-service
  - Update: docker-compose.yml
  - Test: Container deployment
  - Verify: Nacos registration
```

---

## 🗓️ Week 3: Reader Service (25% → 100%)

### Day 1-3: Reader Type & Limit Management

**Primary Agent**: `java-pro`
**Support Agents**: `database-architect`, `test-automator`

#### Tasks to Implement (3 new APIs):

```
GET    /api/v1/readers/types                    - 查询读者分类列表
POST   /api/v1/readers/types                    - 创建读者分类
PUT    /api/v1/readers/types/{id}               - 更新读者分类
DELETE /api/v1/readers/types/{id}               - 删除读者分类
PUT    /api/v1/readers/{id}/type                - 修改读者分类
PUT    /api/v1/readers/{id}/borrow-limit        - 设置借阅限额
```

**Business Logic**:

- Reader types: Student, Teacher, Staff, VIP, Guest
- Different borrow limits per type:
  - Student: 5 books, 30 days
  - Teacher: 10 books, 60 days
  - Staff: 5 books, 30 days
  - VIP: 20 books, 90 days
  - Guest: 2 books, 14 days

**Database Schema**:

```sql
CREATE TABLE reader_type (
    id BIGSERIAL PRIMARY KEY,
    type_name VARCHAR(50) NOT NULL UNIQUE,
    max_borrow_count INTEGER NOT NULL DEFAULT 5,
    max_borrow_days INTEGER NOT NULL DEFAULT 30,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add to reader table
ALTER TABLE reader ADD COLUMN type_id BIGINT REFERENCES reader_type(id);
ALTER TABLE reader ADD COLUMN custom_borrow_limit INTEGER;
ALTER TABLE reader ADD COLUMN custom_borrow_days INTEGER;
```

**Deliverables**:

- [ ] 6 Reader type management APIs
- [ ] Database migration scripts
- [ ] Unit tests (80%+ coverage)
- [ ] Integration tests

**Agent Assignment**:

```
java-pro:
  - Implement: ReaderTypeService
  - Implement: Borrow limit validation logic
  - Handle: Type change cascading updates
  - Write: Service + Controller layers

database-architect:
  - Design: reader_type table
  - Create: Foreign key constraints
  - Create: Migration scripts
  - Seed: Default reader types data

test-automator:
  - Write: Unit tests for ReaderTypeService
  - Write: Integration tests for Type APIs
  - Test: Limit validation edge cases
  - Verify: Test coverage ≥ 80%
```

---

### Day 4-6: Reader Statistics & Batch Operations

**Primary Agent**: `java-pro`
**Support Agents**: `data-scientist`, `test-automator`

#### Tasks to Implement (7 new APIs):

```
GET    /api/v1/readers/{id}/borrow-history      - 查询借阅历史 (分页)
GET    /api/v1/readers/{id}/statistics          - 查询读者统计
GET    /api/v1/readers/statistics/by-type       - 按类型统计读者
GET    /api/v1/readers/statistics/active        - 活跃读者统计
POST   /api/v1/readers/import                   - 批量导入读者 (Excel)
GET    /api/v1/readers/export                   - 导出读者数据 (Excel)
GET    /api/v1/readers/template                 - 下载导入模板
```

**Technical Requirements**:

1. **Excel Import/Export**
   - Use Apache POI library
   - Support .xlsx format
   - Validation: Email, phone, card number uniqueness
   - Error handling: Return failed rows with reasons
   - Max import size: 1000 rows per batch

2. **Statistics Aggregation**
   - Reader count by type
   - Active readers (borrowed in last 30 days)
   - Top borrowers (most books borrowed)
   - Borrow frequency trends

**Dependencies to Add** (`pom.xml`):

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.3</version>
</dependency>
```

**Deliverables**:

- [ ] 7 new APIs implemented
- [ ] Excel import/export functionality
- [ ] Statistics aggregation queries
- [ ] Unit tests (80%+ coverage)
- [ ] Integration tests
- [ ] Excel template file

**Agent Assignment**:

```
java-pro:
  - Implement: Excel import/export service
  - Implement: Batch validation logic
  - Implement: Statistics service
  - Handle: Large file processing (streaming)

data-scientist:
  - Design: Statistics aggregation queries
  - Optimize: Query performance
  - Implement: Redis caching for stats
  - Create: Statistical dashboards

test-automator:
  - Write: Unit tests for import/export
  - Write: Integration tests for batch APIs
  - Test: Large file uploads (1000+ rows)
  - Test: Statistics accuracy
```

---

### Day 7: Face Recognition APIs (Optional) & Dockerization

**Primary Agent**: `deployment-engineer`
**Support Agents**: `ai-engineer` (if implementing face recognition)

#### Tasks:

1. **Face Recognition APIs** (Optional - 2 APIs)

   ```
   POST   /api/v1/readers/{id}/face/upload      - 上传人脸照片
   POST   /api/v1/readers/face/recognize        - 人脸识别登录
   ```

   - If implementing: Integrate with OpenCV or cloud API (Aliyun/Tencent)
   - If skipping: Mark as Future Enhancement

2. **Dockerization**
   - Create Dockerfile
   - Update docker-compose.yml
   - Test all 20 Reader APIs

**Deliverables**:

- [ ] Face recognition APIs (optional)
- [ ] Dockerfile for reader-service
- [ ] Updated docker-compose.yml
- [ ] All 20 Reader APIs functional
- [ ] All tests passing
- [ ] Docker image builds successfully

**Agent Assignment**:

```
ai-engineer (optional):
  - Research: Face recognition libraries
  - Design: Face data storage strategy
  - Implement: Face upload and recognition
  - Integrate: With external API if needed

deployment-engineer:
  - Create: Dockerfile for reader-service
  - Update: docker-compose.yml
  - Test: Container deployment
  - Verify: Nacos registration
  - Document: Deployment process
```

---

## 🗓️ Week 4: System Service Core Features (15% → 60%)

### Day 1-2: Department Management

**Primary Agent**: `java-pro`
**Support Agents**: `database-architect`, `test-automator`

#### Tasks to Implement (7 new APIs):

```
GET    /api/v1/system/departments               - 查询部门列表 (树形)
GET    /api/v1/system/departments/{id}          - 查询部门详情
POST   /api/v1/system/departments               - 创建部门
PUT    /api/v1/system/departments/{id}          - 更新部门
DELETE /api/v1/system/departments/{id}          - 删除部门 (级联检查)
GET    /api/v1/system/departments/{id}/users    - 查询部门用户
POST   /api/v1/system/departments/{id}/sort     - 调整部门排序
```

**Database Schema**:

```sql
CREATE TABLE sys_department (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT,  -- NULL for root departments
    dept_name VARCHAR(100) NOT NULL,
    dept_code VARCHAR(50) UNIQUE,
    leader_id BIGINT,  -- Department leader user ID
    phone VARCHAR(20),
    email VARCHAR(100),
    sort_order INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES sys_department(id),
    FOREIGN KEY (leader_id) REFERENCES sys_user(id)
);

-- Add to sys_user table
ALTER TABLE sys_user ADD COLUMN dept_id BIGINT REFERENCES sys_department(id);
```

**Business Logic**:

- Support multi-level department hierarchy (max 5 levels)
- Cascade delete check (cannot delete if has sub-departments or users)
- Department code must be unique
- Sort order for display in tree structure

**Deliverables**:

- [ ] 7 Department management APIs
- [ ] Tree structure query optimization
- [ ] Database migration scripts
- [ ] Unit tests (80%+ coverage)
- [ ] Integration tests

**Agent Assignment**:

```
java-pro:
  - Implement: DepartmentService with tree structure
  - Implement: Cascade delete validation
  - Implement: Recursive tree query
  - Write: Service + Controller layers

database-architect:
  - Design: sys_department table
  - Optimize: Tree query performance (CTE)
  - Create: Indexes for parent_id
  - Create: Migration scripts

test-automator:
  - Write: Unit tests for DepartmentService
  - Write: Integration tests for Dept APIs
  - Test: Tree structure integrity
  - Test: Cascade delete validation
```

---

### Day 3-4: Role Management

**Primary Agent**: `java-pro`
**Support Agents**: `backend-security-coder`, `test-automator`

#### Tasks to Implement (8 new APIs):

```
GET    /api/v1/system/roles                     - 查询角色列表 (分页)
GET    /api/v1/system/roles/{id}                - 查询角色详情
POST   /api/v1/system/roles                     - 创建角色
PUT    /api/v1/system/roles/{id}                - 更新角色
DELETE /api/v1/system/roles/{id}                - 删除角色 (级联检查)
GET    /api/v1/system/roles/{id}/permissions    - 查询角色权限
POST   /api/v1/system/roles/{id}/permissions    - 分配角色权限
GET    /api/v1/system/roles/{id}/users          - 查询角色用户
```

**Database Schema**:

```sql
CREATE TABLE sys_role (
    id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL UNIQUE,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    sort_order INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_role_permission (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES sys_permission(id) ON DELETE CASCADE,
    UNIQUE (role_id, permission_id)
);

CREATE TABLE sys_user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
    UNIQUE (user_id, role_id)
);

-- Seed default roles
INSERT INTO sys_role (role_name, role_code, description) VALUES
('超级管理员', 'SUPER_ADMIN', '系统最高权限'),
('图书管理员', 'LIBRARIAN', '图书管理权限'),
('普通用户', 'USER', '基础查询权限');
```

**Business Logic**:

- RBAC (Role-Based Access Control)
- One user can have multiple roles
- One role can have multiple permissions
- Cascade delete protection (cannot delete role if has users)
- Role code immutable after creation

**Deliverables**:

- [ ] 8 Role management APIs
- [ ] RBAC permission assignment
- [ ] Database migration scripts
- [ ] Unit tests (80%+ coverage)
- [ ] Integration tests

**Agent Assignment**:

```
java-pro:
  - Implement: RoleService with RBAC
  - Implement: Permission assignment logic
  - Implement: Cascade validation
  - Write: Service + Controller layers

backend-security-coder:
  - Design: RBAC security model
  - Implement: Permission checking interceptor
  - Review: Security vulnerabilities
  - Document: RBAC usage guide

test-automator:
  - Write: Unit tests for RoleService
  - Write: Integration tests for Role APIs
  - Test: RBAC permission scenarios
  - Test: Cascade delete protection
```

---

### Day 5-6: Permission Management

**Primary Agent**: `java-pro`
**Support Agents**: `backend-security-coder`, `test-automator`

#### Tasks to Implement (7 new APIs):

```
GET    /api/v1/system/permissions               - 查询权限列表 (树形)
GET    /api/v1/system/permissions/{id}          - 查询权限详情
POST   /api/v1/system/permissions               - 创建权限
PUT    /api/v1/system/permissions/{id}          - 更新权限
DELETE /api/v1/system/permissions/{id}          - 删除权限
GET    /api/v1/system/permissions/tree          - 查询权限树 (菜单权限)
POST   /api/v1/system/permissions/{id}/sort     - 调整权限排序
```

**Database Schema**:

```sql
CREATE TABLE sys_permission (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT,  -- NULL for root permissions
    permission_name VARCHAR(100) NOT NULL,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    permission_type VARCHAR(20) NOT NULL, -- MENU, BUTTON, API
    path VARCHAR(200),  -- Frontend route path (for MENU)
    component VARCHAR(200),  -- Frontend component path
    icon VARCHAR(100),  -- Menu icon
    sort_order INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES sys_permission(id)
);

-- Seed core permissions
INSERT INTO sys_permission (permission_name, permission_code, permission_type, path, icon, sort_order) VALUES
('系统管理', 'system', 'MENU', '/system', 'Setting', 1),
('用户管理', 'system:user', 'MENU', '/system/user', 'User', 1),
('用户查询', 'system:user:query', 'BUTTON', NULL, NULL, 1),
('用户新增', 'system:user:add', 'BUTTON', NULL, NULL, 2),
('角色管理', 'system:role', 'MENU', '/system/role', 'UserFilled', 2),
('权限管理', 'system:permission', 'MENU', '/system/permission', 'Lock', 3);
```

**Permission Types**:

- **MENU**: Frontend menu items (visible in sidebar)
- **BUTTON**: Frontend button permissions (show/hide buttons)
- **API**: Backend API endpoints (access control)

**Business Logic**:

- Support hierarchical permission tree (max 3 levels)
- Permission code unique constraint
- Used by frontend for dynamic menu rendering
- Used by backend for API access control

**Deliverables**:

- [ ] 7 Permission management APIs
- [ ] Tree structure query optimization
- [ ] Database migration scripts with seed data
- [ ] Unit tests (80%+ coverage)
- [ ] Integration tests

**Agent Assignment**:

```
java-pro:
  - Implement: PermissionService with tree structure
  - Implement: Dynamic permission tree query
  - Implement: Permission type validation
  - Write: Service + Controller layers

backend-security-coder:
  - Design: Permission-based access control
  - Implement: API permission interceptor
  - Create: Permission checking annotations
  - Document: Permission system guide

test-automator:
  - Write: Unit tests for PermissionService
  - Write: Integration tests for Permission APIs
  - Test: Tree structure integrity
  - Test: Permission type scenarios
```

---

### Day 7: Audit Log Management & Dockerization

**Primary Agent**: `java-pro`
**Support Agents**: `deployment-engineer`, `observability-engineer`

#### Tasks:

1. **Audit Log APIs** (11 new APIs)

   ```
   GET    /api/v1/system/logs/login               - 查询登录日志 (分页)
   GET    /api/v1/system/logs/operation           - 查询操作日志 (分页)
   GET    /api/v1/system/logs/error               - 查询错误日志 (分页)
   GET    /api/v1/system/logs/access              - 查询访问日志 (分页)
   GET    /api/v1/system/logs/{id}                - 查询日志详情
   DELETE /api/v1/system/logs                     - 批量删除日志 (by IDs)
   POST   /api/v1/system/logs/export              - 导出日志 (Excel)
   GET    /api/v1/system/logs/statistics          - 日志统计概览
   GET    /api/v1/system/logs/statistics/by-user  - 按用户统计
   GET    /api/v1/system/logs/statistics/by-type  - 按类型统计
   POST   /api/v1/system/logs/cleanup             - 清理过期日志 (admin only)
   ```

2. **Log Collection Strategy**
   - Use AOP (Aspect-Oriented Programming) for automatic logging
   - Create custom annotation `@AuditLog`
   - Log all controller method calls
   - Capture: IP, user, action, params, result, duration, timestamp

3. **Database Schema**:

   ```sql
   CREATE TABLE sys_log (
       id BIGSERIAL PRIMARY KEY,
       log_type VARCHAR(20) NOT NULL, -- LOGIN, OPERATION, ERROR, ACCESS
       user_id BIGINT,
       username VARCHAR(100),
       ip_address VARCHAR(50),
       action VARCHAR(200) NOT NULL,
       method VARCHAR(10),  -- HTTP method
       request_uri VARCHAR(500),
       params TEXT,  -- JSON format
       result TEXT,  -- JSON format or error message
       status VARCHAR(20),  -- SUCCESS, FAILURE
       duration_ms INTEGER,  -- Execution time in milliseconds
       error_message TEXT,
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
   );

   -- Indexes for query performance
   CREATE INDEX idx_log_type ON sys_log(log_type);
   CREATE INDEX idx_log_user_id ON sys_log(user_id);
   CREATE INDEX idx_log_created_at ON sys_log(created_at);
   ```

4. **Dockerization**
   - Create Dockerfile for system-service
   - Update docker-compose.yml
   - Test all 33 System APIs

**Deliverables**:

- [ ] 11 Audit log APIs implemented
- [ ] AOP logging aspect created
- [ ] `@AuditLog` custom annotation
- [ ] Database migration scripts
- [ ] Dockerfile for system-service
- [ ] Updated docker-compose.yml
- [ ] All 33 System APIs functional
- [ ] All tests passing (70%+ coverage)
- [ ] Docker image builds successfully

**Agent Assignment**:

```
java-pro:
  - Implement: LogService with pagination
  - Implement: AOP logging aspect
  - Create: @AuditLog custom annotation
  - Implement: Log export to Excel
  - Write: Service + Controller layers

observability-engineer:
  - Design: Log retention policy
  - Implement: Log aggregation queries
  - Optimize: Log query performance
  - Create: Log statistics dashboard

deployment-engineer:
  - Create: Dockerfile for system-service
  - Update: docker-compose.yml
  - Test: Container deployment
  - Verify: Nacos registration
  - Document: System service deployment
```

---

## 📊 Phase 1 Summary & Success Criteria

### Completion Checklist

**Week 1: Book Service** ✅

- [ ] 15 Book APIs fully functional
- [ ] MinIO integration for file uploads
- [ ] PostgreSQL full-text search working
- [ ] Unit test coverage ≥ 80%
- [ ] Docker image builds successfully
- [ ] Swagger UI accessible
- [ ] Nacos service registration confirmed

**Week 2: Circulation Service** ✅

- [ ] 17 Circulation APIs fully functional
- [ ] Reservation management working
- [ ] Fine calculation and payment processing
- [ ] Batch operations with transaction consistency
- [ ] Statistics APIs with Redis caching
- [ ] Unit test coverage ≥ 80%
- [ ] Docker image builds successfully

**Week 3: Reader Service** ✅

- [ ] 20 Reader APIs fully functional
- [ ] Reader type management
- [ ] Excel import/export functionality
- [ ] Statistics aggregation
- [ ] Face recognition (optional)
- [ ] Unit test coverage ≥ 80%
- [ ] Docker image builds successfully

**Week 4: System Service** ✅

- [ ] 33 System APIs fully functional (MVP scope)
- [ ] Department management (7 APIs)
- [ ] Role management with RBAC (8 APIs)
- [ ] Permission management (7 APIs)
- [ ] Audit log system with AOP (11 APIs)
- [ ] Unit test coverage ≥ 70%
- [ ] Docker image builds successfully

### Overall Phase 1 Metrics

| Metric                    | Target                                  | Status          |
| ------------------------- | --------------------------------------- | --------------- |
| Backend Services Complete | 4/7 (Book, Circulation, Reader, System) | ⏳ In Progress  |
| Backend Completion        | 85%                                     | ⏳ Target       |
| New API Endpoints         | 90 APIs                                 | ⏳ To Implement |
| Total API Endpoints       | 128 APIs                                | ⏳ Target       |
| Docker Images             | 6/7 services                            | ⏳ Target       |
| Test Coverage             | ≥ 80% backend                           | ⏳ Target       |
| OpenAPI Specs             | 7 services documented                   | ⏳ Target       |
| Swagger UI                | All services accessible                 | ⏳ Target       |

---

## 🚀 Post-Phase 1: Next Steps

After completing Phase 1, we proceed to:

**Phase 2: Frontend-Backend Integration (Week 5-6)**

- Replace Mock APIs with real backend calls
- End-to-end testing with Playwright
- Bug fixes and optimization

**Phase 3: API Contract Testing (Week 7)**

- Spring Cloud Contract tests
- Validate all 128 API endpoints
- Ensure Mock-Real API consistency

**Phase 4: Performance & Security (Week 8-10)**

- Load testing with JMeter
- Security hardening
- Production environment setup

---

## 📝 Agent Coordination & Communication

### Daily Standups (Async)

Each agent should report:

1. Yesterday's completed tasks
2. Today's planned tasks
3. Blockers or dependencies

### Code Review Protocol

- All PRs require review by `code-reviewer` agent
- Security-sensitive code reviewed by `backend-security-coder`
- Database changes reviewed by `database-architect`

### Testing Strategy

- Unit tests: Written by implementing agent
- Integration tests: Coordinated by `test-automator`
- Test coverage reports: Generated daily

### Documentation Requirements

- OpenAPI specs: Updated with each API implementation
- README: Updated with setup instructions
- Architecture decisions: Logged in `doc/decisions/`

---

## 🎯 Risk Management

### High-Risk Items

1. **MinIO Integration** (Week 1)
   - Risk: First-time integration
   - Mitigation: Allocate 1 day for setup and testing

2. **Transaction Consistency** (Week 2)
   - Risk: Complex distributed transactions
   - Mitigation: Use Spring `@Transactional` with proper isolation

3. **Excel Large File Import** (Week 3)
   - Risk: Memory issues with large files
   - Mitigation: Implement streaming upload (Apache POI SAX)

4. **AOP Logging Performance** (Week 4)
   - Risk: Logging overhead slowing APIs
   - Mitigation: Async logging with RabbitMQ

### Dependency Management

- All database migrations must complete before API implementation
- OpenAPI specs should be created before API coding starts
- Dockerization should happen after all tests pass

---

## 📞 Support & Escalation

### Agent Escalation Path

1. **Technical Blockers**: Escalate to `architect-review` agent
2. **Performance Issues**: Escalate to `performance-engineer`
3. **Security Concerns**: Escalate to `security-auditor`
4. **DevOps Issues**: Escalate to `devops-troubleshooter`

### Critical Failure Protocol

If any agent is blocked for > 2 days:

1. Document the blocker in detail
2. Propose 2-3 alternative approaches
3. Request architect review
4. Adjust timeline if necessary

---

**Last Updated**: 2025-11-03
**Document Owner**: Full-Stack Development Team
**Review Cycle**: Weekly during Phase 1 execution
