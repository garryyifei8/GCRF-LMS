# GCRF Integration Test - Quick Start Guide

**Date**: 2025-11-11
**Purpose**: Frontend-Backend Integration Testing
**Modules**: Auth + Books + Readers
**Estimated Time**: 30-45 minutes

---

## Quick Start (5 Steps)

### Step 1: Start Infrastructure Services (2 minutes)

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/deployment

# Start PostgreSQL, Redis, Nacos, MinIO, Elasticsearch
docker-compose -f docker-compose.infrastructure.yml up -d

# Wait for services to be ready
sleep 30

# Verify
docker ps --filter name=gcrf
```

**Expected Output**: 5 containers running (postgres, redis, nacos, minio, elasticsearch)

---

### Step 2: Start Backend Services (3-5 minutes)

Open **4 terminal windows** and run each service:

**Terminal 1 - Gateway Service** (Port 8080):

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend/gateway-service
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn spring-boot:run
```

**Terminal 2 - Auth Service** (Port 8081):

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend/auth-service
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn spring-boot:run
```

**Terminal 3 - Book Service** (Port 8082):

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend/book-service
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn spring-boot:run
```

**Terminal 4 - Reader Service** (Port 8084):

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend/reader-service
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn spring-boot:run
```

**Wait for all services to register with Nacos** (look for "Nacos registry succeeded" in logs)

---

### Step 3: Verify Service Registration (1 minute)

**Open Nacos Console**: http://localhost:8848/nacos
**Login**: nacos / nacos

Navigate to **服务管理 → 服务列表**, verify these services are registered:

- ✅ `gateway-service`
- ✅ `auth-service`
- ✅ `book-service`
- ✅ `reader-service`

All should show **健康实例数: 1**

---

### Step 4: Start Frontend (1 minute)

**Terminal 5 - Frontend**:

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/web-admin
npm run dev
```

**Expected Output**:

```
  VITE v5.x ready in XXX ms

  ➜  Local:   http://localhost:3011/
  ➜  Network: use --host to expose
```

---

### Step 5: Run Automated Tests (2 minutes)

**Terminal 6 - Test Script**:

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/DevPlan/scripts
./integration-test.sh
```

**Expected Output**:

```
========================================
GCRF Integration Test Suite
========================================

1. Prerequisites Check
✅ Gateway is running on port 8080
✅ Auth Service is running on port 8081
✅ Book Service is running on port 8082
✅ Reader Service is running on port 8084

2. Auth Module Tests (3 tests)
[1/15] Testing user login
✅ User login successful
[2/15] Testing get user info
✅ Get user info successful (username: admin)
[3/15] Testing token refresh
✅ Token refresh successful

3. Book Module Tests (7 tests)
[4/15] Testing get book list
✅ Get book list successful (total: X books)
...

5. Test Summary
Total Tests: 15
Passed: 15
Failed: 0
Success Rate: 100.00%

🎉 All tests passed!
```

---

## Manual Testing in Browser (10 minutes)

### Test 1: Authentication Flow

1. **Open**: http://localhost:3011
2. **Login**: admin / admin123
3. **Verify**: Redirect to dashboard
4. **Check DevTools** (F12):
   - **Console**: No errors
   - **Network**: 200 OK for `/api/v1/auth/login`
   - **Application** → **Local Storage**: `token` exists

---

### Test 2: Book Management

1. **Navigate**: 侧边栏 → 图书管理 → 图书列表
2. **Verify**: Table displays books
3. **Test Search**:
   - Enter "Java" in search box
   - Click "搜索"
   - Verify: Filtered results
4. **Test Create**:
   - Click "新增图书"
   - Fill form:
     ```
     ISBN: 978-7-115-12345-6
     Title: Test Book
     Author: Test Author
     Publisher: Test Publisher
     Category: (select any)
     Total Copies: 5
     ```
   - Click "确定"
   - Verify: Success message, book appears in table
5. **Test Edit**:
   - Click "编辑" on test book
   - Change title to "Test Book (Updated)"
   - Click "确定"
   - Verify: Title updated in table
6. **Test Delete**:
   - Click "删除" on test book
   - Confirm deletion
   - Verify: Book removed from table

**Check DevTools Network Tab**:

- All requests: 200 OK
- All requests to: http://localhost:8080/api/v1/books/\*
- Response format matches frontend expectations

---

### Test 3: Reader Management

1. **Navigate**: 侧边栏 → 读者管理 → 读者列表
2. **Verify**: Table displays readers
3. **Test Filters**:
   - Select "学生" in reader type dropdown
   - Select "正常" in status dropdown
   - Click "搜索"
   - Verify: Filtered results
4. **Test Create**:
   - Click "新增读者"
   - Fill form:
     ```
     Name: 测试读者
     Reader Type: 学生
     Email: test@example.com
     Phone: 13900139000
     Gender: 男
     ```
   - Click "确定"
   - Verify: Success message, reader appears in table
5. **Test Reader Type Management**:
   - Navigate: 读者管理 → 类型管理
   - Verify: Shows 5 types (STUDENT, TEACHER, STAFF, VIP, GUEST)
   - Check configuration (e.g., STUDENT: 5本/30天)

---

## Troubleshooting

### Issue 1: Services Won't Start

```bash
# Check ports
lsof -i :8080,8081,8082,8084

# Kill processes if needed
lsof -ti :8080 | xargs kill -9

# Check Java version
java -version  # Should be 21

# Set JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

### Issue 2: Services Not Registering with Nacos

```bash
# Check Nacos
curl http://localhost:8848/nacos/

# Check service logs
# Look for: "Nacos registry succeeded"

# Wait longer (services take 30-60s to register)
```

### Issue 3: Frontend Shows "Network Error"

```bash
# Check Gateway
curl http://localhost:8080/actuator/health

# Check CORS configuration in Gateway
# Should allow http://localhost:3011

# Check browser DevTools → Console for errors
```

### Issue 4: 401 Unauthorized

```bash
# Token expired - login again
# Or check token in localStorage
localStorage.getItem('token')

# Verify token with backend
curl -H "Authorization: Bearer <token>" \
  http://localhost:8080/api/v1/auth/info
```

### Issue 5: MSW Still Active (Mock API)

**Symptom**: API calls intercepted by MSW instead of going to backend

**Solution**:

1. Open `web-admin/src/main.js`
2. Verify MSW code is commented out (lines 43-113)
3. Verify line 117-118 show:
   ```javascript
   console.log("[App] MSW disabled - using real backend API");
   mountApp();
   ```
4. Hard refresh browser: Ctrl+Shift+R (Windows) or Cmd+Shift+R (Mac)
5. Clear Service Worker:
   - F12 → Application → Service Workers
   - Unregister all
   - Reload page

---

## Test Checklist

### Before Testing

- [ ] Docker daemon running
- [ ] Infrastructure services running (5 containers)
- [ ] All backend services running (4 services)
- [ ] All services registered in Nacos
- [ ] Frontend development server running
- [ ] Browser DevTools open (F12)

### During Testing

- [ ] No console errors
- [ ] All API requests go through Gateway (8080)
- [ ] All requests return 200 OK (or expected error code)
- [ ] Response format matches frontend expectations
- [ ] Data persists (refresh page to verify)
- [ ] Loading states work correctly
- [ ] Error messages display correctly

### After Testing

- [ ] All automated tests passed (15/15)
- [ ] All manual tests completed
- [ ] No data inconsistencies found
- [ ] Document any issues found
- [ ] Create GitHub issues for bugs

---

## Expected Results Summary

### Automated Test Results

| Module    | Tests  | Expected Pass Rate |
| --------- | ------ | ------------------ |
| Auth      | 3      | 100%               |
| Books     | 7      | 100%               |
| Readers   | 5      | 100%               |
| **Total** | **15** | **100%**           |

### Manual Test Results

| Feature      | Expected Behavior                    |
| ------------ | ------------------------------------ |
| Login        | Redirect to dashboard, token stored  |
| Book List    | Display all books with pagination    |
| Book Search  | Filter by keyword                    |
| Book CRUD    | Create, read, update, delete working |
| Reader List  | Display all readers with filters     |
| Reader CRUD  | Create, read, update, delete working |
| Reader Types | Display 5 types with configurations  |

---

## Next Steps After Testing

1. **Document Issues**: Create `DevPlan/INTEGRATION_TEST_RESULTS.md`
2. **Fix Critical Bugs**: Priority fixes for any failures
3. **Performance Check**: Verify response times < 200ms
4. **Security Audit**: Check JWT expiration, input validation
5. **Update Delivery Summary**: Add test results to `FINAL_DELIVERY_SUMMARY.md`

---

## Reference Documents

- **Full Test Plan**: `DevPlan/INTEGRATION_TEST_PLAN.md`
- **Test Script**: `DevPlan/scripts/integration-test.sh`
- **API Specifications**: `DevPlan/05_API_SPECIFICATIONS/`
- **Architecture**: `docs/architecture/architect.md`
- **Development Guidelines**: `CLAUDE.md`

---

## Support

**Issues Found?**

- Document in `DevPlan/INTEGRATION_TEST_RESULTS.md`
- Include: Screenshot, request/response, expected vs actual
- Provide: Steps to reproduce

**Questions?**

- Check INTEGRATION_TEST_PLAN.md for detailed scenarios
- Review FINAL_DELIVERY_SUMMARY.md for known limitations
- Check service logs for errors

---

**Status**: ⏳ Ready for Execution
**Estimated Duration**: 30-45 minutes
**Last Updated**: 2025-11-11
**Prepared By**: Claude Code Agent
