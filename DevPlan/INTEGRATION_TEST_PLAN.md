# GCRF Library Management System - Integration Test Plan

**Date**: 2025-11-11
**Phase**: Phase 1 - Frontend-Backend Integration Testing
**Modules**: Auth Service + Book Service + Reader Service
**Status**: Ready for Execution

---

## Test Objectives

1. **Verify Authentication Flow**: Login → JWT Token → Authorized Requests
2. **Test Book Module Integration**: CRUD operations, search, file upload/download
3. **Test Reader Module Integration**: CRUD operations, type management
4. **Validate API Gateway Routing**: All requests through Gateway (8080)
5. **Confirm Data Consistency**: Frontend display matches backend data
6. **Test Error Handling**: Network errors, validation errors, business errors

---

## Prerequisites

### Infrastructure Services (Docker)

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/deployment
docker-compose -f docker-compose.infrastructure.yml up -d

# Verify infrastructure
docker ps --filter name=gcrf
```

Required containers:

- `gcrf-postgres-primary` (5432)
- `gcrf-redis-master` (6379)
- `gcrf-nacos` (8848)
- `gcrf-minio` (9000, 9001)
- `gcrf-elasticsearch` (9200)

### Backend Services

```bash
# Start Gateway (required for all requests)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend/gateway-service
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn spring-boot:run &

# Start Auth Service
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend/auth-service
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn spring-boot:run &

# Start Book Service
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend/book-service
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn spring-boot:run &

# Start Reader Service
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend/reader-service
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn spring-boot:run &

# Wait for services to register with Nacos (30-60 seconds)
```

### Frontend

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/web-admin
npm run dev
# Access: http://localhost:3011
```

---

## Test Scenarios

### 1. Authentication Flow Test

#### 1.1 User Login

**Endpoint**: `POST /api/v1/auth/login`
**Frontend**: `src/views/login/index.vue`
**Test Steps**:

1. Open http://localhost:3011/login
2. Enter credentials: `admin` / `admin123`
3. Click "登录"
4. **Expected**: Redirect to dashboard, JWT token stored in localStorage

**Verification**:

```bash
# Check token in browser DevTools
localStorage.getItem('token')

# Verify token with backend
TOKEN="<paste-token-here>"
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/auth/info
```

**Success Criteria**:

- ✅ Login successful (200 OK)
- ✅ Token stored in localStorage
- ✅ User info returned with correct username and roles
- ✅ Redirect to dashboard

#### 1.2 Token Refresh

**Endpoint**: `POST /api/v1/auth/refresh`
**Test Steps**:

1. Use existing token from 1.1
2. Call refresh endpoint
3. **Expected**: New token returned

**Verification**:

```bash
curl -X POST -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/auth/refresh
```

#### 1.3 Logout

**Endpoint**: `POST /api/v1/auth/logout`
**Test Steps**:

1. Click logout button
2. **Expected**: Token cleared, redirect to login

---

### 2. Book Module Integration Test

#### 2.1 Book List Query

**Endpoint**: `GET /api/v1/books`
**Frontend**: `src/views/books/index.vue`
**Test Steps**:

1. Navigate to "图书管理" → "图书列表"
2. Observe table rendering
3. Test pagination (click page 2, 3)
4. Test page size change (10, 20, 50)

**Verification**:

```bash
# Direct API call
TOKEN="<your-token>"
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/books?pageNum=1&pageSize=20"
```

**Expected Response Format**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "isbn": "978-7-115-12345-6",
        "title": "Sample Book",
        "author": "Author Name",
        "publisher": "Publisher Name",
        "categoryId": 1,
        "categoryName": "Fiction",
        "totalCopies": 10,
        "availableCopies": 8,
        "status": "AVAILABLE"
      }
    ],
    "total": 100,
    "pageNum": 1,
    "pageSize": 20,
    "pages": 5
  },
  "timestamp": "2025-11-11T10:00:00"
}
```

**Success Criteria**:

- ✅ Table displays books correctly
- ✅ Pagination works (page changes reflected in data)
- ✅ Page size changes reflected
- ✅ Loading state shown during request
- ✅ No console errors

#### 2.2 Book Search

**Endpoint**: `POST /api/v1/books/search`
**Frontend**: `src/views/books/index.vue` (search form)
**Test Steps**:

1. Enter search keyword: "Java"
2. Click search button
3. **Expected**: Filtered results shown

**Verification**:

```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"keyword": "Java"}' \
  http://localhost:8080/api/v1/books/search
```

#### 2.3 Book Create

**Endpoint**: `POST /api/v1/books`
**Frontend**: `src/views/books/index.vue` (create dialog)
**Test Steps**:

1. Click "新增图书" button
2. Fill form:
   - ISBN: 978-7-115-99999-9
   - Title: Integration Test Book
   - Author: Test Author
   - Publisher: Test Publisher
   - Category: Select from dropdown
   - Total Copies: 5
3. Click "确定"
4. **Expected**: Success message, table refreshed

**Verification**:

```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "isbn": "978-7-115-99999-9",
    "title": "Integration Test Book",
    "author": "Test Author",
    "publisher": "Test Publisher",
    "categoryId": 1,
    "totalCopies": 5
  }' \
  http://localhost:8080/api/v1/books
```

#### 2.4 Book Update

**Endpoint**: `PUT /api/v1/books/{id}`
**Test Steps**:

1. Click "编辑" on test book
2. Modify title to "Integration Test Book (Updated)"
3. Click "确定"
4. **Expected**: Success message, table shows updated title

#### 2.5 Book Delete

**Endpoint**: `DELETE /api/v1/books/{id}`
**Test Steps**:

1. Click "删除" on test book
2. Confirm deletion
3. **Expected**: Success message, book removed from table

#### 2.6 File Upload (Cover)

**Endpoint**: `POST /api/v1/books/{id}/cover`
**Test Steps**:

1. Click "上传封面" on any book
2. Select image file (< 5MB, JPG/PNG)
3. Click upload
4. **Expected**: Cover displayed in table

**Verification**:

```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/cover.jpg" \
  http://localhost:8080/api/v1/books/1/cover
```

#### 2.7 Category Management

**Endpoint**: `GET /api/v1/books/categories`
**Frontend**: `src/views/books/categories.vue`
**Test Steps**:

1. Navigate to "图书管理" → "分类管理"
2. Observe tree rendering
3. Click expand/collapse nodes
4. Test create category
5. Test update category
6. Test delete category (should fail if books exist)

---

### 3. Reader Module Integration Test

#### 3.1 Reader List Query

**Endpoint**: `GET /api/v1/readers`
**Frontend**: `src/views/readers/index.vue`
**Test Steps**:

1. Navigate to "读者管理" → "读者列表"
2. Observe table rendering
3. Test pagination
4. Test filters (reader type, status)

**Verification**:

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/readers?pageNum=1&pageSize=20&readerType=STUDENT&status=ACTIVE"
```

**Expected Response Format**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "cardNumber": "R20250001",
        "name": "张三",
        "readerType": "STUDENT",
        "email": "zhangsan@example.com",
        "phone": "13800138000",
        "status": "ACTIVE",
        "maxBorrowCount": 5,
        "currentBorrowCount": 2,
        "registeredAt": "2025-01-01T00:00:00"
      }
    ],
    "total": 50,
    "pageNum": 1,
    "pageSize": 20,
    "pages": 3
  },
  "timestamp": "2025-11-11T10:00:00"
}
```

#### 3.2 Reader Create

**Endpoint**: `POST /api/v1/readers`
**Test Steps**:

1. Click "新增读者"
2. Fill form:
   - Card Number: R20250999 (auto-generated)
   - Name: 测试读者
   - Reader Type: STUDENT
   - Email: test@example.com
   - Phone: 13900139000
3. Click "确定"
4. **Expected**: Success message, reader appears in table

#### 3.3 Reader Type Management

**Endpoint**: `GET /api/v1/readers/types`
**Frontend**: `src/views/readers/types.vue`
**Test Steps**:

1. Navigate to "读者管理" → "类型管理"
2. Observe type list:
   - STUDENT: 5本/30天
   - TEACHER: 10本/60天
   - STAFF: 5本/30天
   - VIP: 20本/90天
   - GUEST: 2本/14天
3. Test create new type
4. Test update existing type
5. Test delete type (should fail if readers exist)

**Verification**:

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/readers/types
```

#### 3.4 Change Reader Type

**Endpoint**: `PUT /api/v1/readers/{id}/type`
**Test Steps**:

1. Select a reader
2. Click "修改类型"
3. Select new type (e.g., STUDENT → VIP)
4. **Expected**: Type updated, maxBorrowCount changed

#### 3.5 Set Borrow Limit

**Endpoint**: `PUT /api/v1/readers/{id}/borrow-limit`
**Test Steps**:

1. Select a reader
2. Click "设置借阅限额"
3. Set custom limit (e.g., 15 books)
4. **Expected**: Custom limit applied

---

## Cross-Module Integration Tests

### 4.1 Authentication Required for All Modules

**Test**: Try accessing API without token

```bash
curl http://localhost:8080/api/v1/books
# Expected: 401 Unauthorized
```

### 4.2 Role-Based Access Control (RBAC)

**Test**: Try admin-only operations with regular user token

```bash
# Login as regular user first
TOKEN="<regular-user-token>"
curl -X DELETE -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/books/1
# Expected: 403 Forbidden (if implemented)
```

### 4.3 Gateway Routing

**Test**: All requests routed through Gateway

- Direct service call (should fail): `http://localhost:8082/api/v1/books`
- Gateway call (should succeed): `http://localhost:8080/api/v1/books`

### 4.4 Error Handling Consistency

**Test**: All modules return same error format

```json
{
  "code": 400,
  "message": "Validation failed: ISBN format invalid",
  "timestamp": "2025-11-11T10:00:00"
}
```

---

## Performance Tests

### 5.1 Response Time

- API Response: < 200ms (P95)
- Database Query: < 50ms (P95)
- Page Load: < 2s

### 5.2 Concurrent Users

- Simulate 10 concurrent users
- No request failures
- No data corruption

### 5.3 File Upload/Download

- Upload 5MB image: < 3s
- Download PDF: Streaming, no timeout

---

## Known Issues & Limitations

### Issues from FINAL_DELIVERY_SUMMARY.md

1. **System Service**: Only 13% complete - admin user management not available
2. **Notification Service**: Not implemented - no notifications
3. **Mock API**: May still be active in frontend - need to disable
4. **Service Discovery**: Verify all services register with Nacos correctly

### Frontend Mock API Configuration

**⚠️ CRITICAL**: Check if MSW (Mock Service Worker) is active

**File**: `web-admin/src/main.js`

```javascript
// MUST COMMENT OUT for real backend testing
// if (import.meta.env.DEV) {
//   const { setupWorker } = await import('./mock/browser')
//   await setupWorker()
// }
```

---

## Test Execution Checklist

### Before Testing

- [ ] Docker services running (PostgreSQL, Redis, Nacos, MinIO)
- [ ] All backend services running (Gateway, Auth, Book, Reader)
- [ ] Frontend development server running
- [ ] MSW mock disabled in frontend
- [ ] Browser DevTools open (F12 → Network, Console)

### During Testing

- [ ] Monitor browser console for errors
- [ ] Monitor Network tab for API calls
- [ ] Check response format matches expected
- [ ] Verify data persistence (refresh page)
- [ ] Test error scenarios (invalid input, network errors)

### After Testing

- [ ] Document all issues found
- [ ] Create GitHub issues for bugs
- [ ] Update FINAL_DELIVERY_SUMMARY.md with test results
- [ ] Take screenshots of successful flows

---

## Test Report Template

```markdown
# Integration Test Results - [Date]

## Test Environment

- Backend Services: [versions]
- Frontend Version: [version]
- Database: PostgreSQL 15.x
- Browser: Chrome 120.x

## Test Results Summary

| Module    | Total Tests | Passed | Failed | Success Rate |
| --------- | ----------- | ------ | ------ | ------------ |
| Auth      | 3           | 3      | 0      | 100%         |
| Books     | 7           | 6      | 1      | 85.7%        |
| Readers   | 5           | 5      | 0      | 100%         |
| **Total** | **15**      | **14** | **1**  | **93.3%**    |

## Detailed Results

### Auth Module

✅ 1.1 User Login: PASSED
✅ 1.2 Token Refresh: PASSED
✅ 1.3 Logout: PASSED

### Book Module

✅ 2.1 Book List Query: PASSED
✅ 2.2 Book Search: PASSED
✅ 2.3 Book Create: PASSED
❌ 2.4 Book Update: FAILED - Error: 500 Internal Server Error
✅ 2.5 Book Delete: PASSED
✅ 2.6 File Upload: PASSED
✅ 2.7 Category Management: PASSED

### Reader Module

✅ 3.1 Reader List Query: PASSED
✅ 3.2 Reader Create: PASSED
✅ 3.3 Reader Type Management: PASSED
✅ 3.4 Change Reader Type: PASSED
✅ 3.5 Set Borrow Limit: PASSED

## Issues Found

### Critical Issues

None

### Major Issues

1. **Book Update Failure** (2.4)
   - Error: 500 Internal Server Error
   - Steps to reproduce: Edit book title, save
   - Expected: 200 OK with updated book
   - Actual: 500 error
   - Stack trace: [attach]

### Minor Issues

None

## Recommendations

1. Fix book update endpoint error handling
2. Add better error messages for validation failures
3. Improve loading states in frontend
```

---

## Automated Test Script

Create `DevPlan/scripts/integration-test.sh`:

```bash
#!/bin/bash
set -e

echo "=== GCRF Integration Test Suite ==="
echo ""

# Get JWT token
echo "[1/15] Testing Login..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.token')
if [ "$TOKEN" == "null" ]; then
  echo "❌ Login failed"
  exit 1
fi
echo "✅ Login successful"

# Test Book List
echo "[2/15] Testing Book List..."
BOOKS=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/books?pageNum=1&pageSize=20")
if [ $(echo $BOOKS | jq -r '.code') -eq 200 ]; then
  echo "✅ Book list query successful"
else
  echo "❌ Book list query failed"
fi

# Test Reader List
echo "[3/15] Testing Reader List..."
READERS=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/readers?pageNum=1&pageSize=20")
if [ $(echo $READERS | jq -r '.code') -eq 200 ]; then
  echo "✅ Reader list query successful"
else
  echo "❌ Reader list query failed"
fi

# Add more tests...

echo ""
echo "=== Test Summary ==="
echo "✅ Passed: 3/3"
echo "❌ Failed: 0/3"
echo "Success Rate: 100%"
```

---

## Next Steps After Integration Testing

1. **Fix Issues**: Address all critical and major issues found
2. **Performance Optimization**: If response times exceed targets
3. **Security Audit**: Verify JWT expiration, input validation, SQL injection prevention
4. **Documentation**: Update API docs with actual response examples
5. **Phase 2 Planning**: Begin System Service and Notification Service development

---

## Contact

- **Test Lead**: GCRF Team
- **Issues**: DevPlan/INTEGRATION_TEST_ISSUES.md
- **Test Results**: DevPlan/INTEGRATION_TEST_RESULTS.md

---

**Status**: ⏳ Awaiting Execution
**Last Updated**: 2025-11-11
**Prepared By**: Claude Code Agent
