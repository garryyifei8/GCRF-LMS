# GCRF Library Management System - E2E Test Cases

**Document Version**: 1.0
**Last Updated**: 2025-12-01
**Test Environment**: Development & Production
**Test Framework**: Custom Shell Scripts + Playwright + API Testing

---

## Table of Contents

1. [Overview](#overview)
2. [Test Environment Setup](#test-environment-setup)
3. [Core Business Flow Test Cases](#core-business-flow-test-cases)
4. [API Integration Test Cases](#api-integration-test-cases)
5. [Frontend E2E Test Cases](#frontend-e2e-test-cases)
6. [Performance & Load Test Cases](#performance--load-test-cases)
7. [Security Test Cases](#security-test-cases)
8. [Data Integrity Test Cases](#data-integrity-test-cases)
9. [Test Execution Guide](#test-execution-guide)
10. [Expected Results & Acceptance Criteria](#expected-results--acceptance-criteria)

---

## Overview

### Purpose

This document defines comprehensive end-to-end test cases for the GCRF Library Management System, covering all critical business flows, API integrations, frontend interactions, and system behaviors.

### Scope

- **Core Business Functions**: Authentication, Book Management, Circulation, Reader Management
- **Integration Testing**: Cross-service communication, database transactions
- **Frontend Testing**: UI interactions, user workflows
- **Performance Testing**: Load testing, stress testing, concurrent operations
- **Security Testing**: Authentication, authorization, input validation

### Test Levels

- **L1 - Smoke Tests**: Critical path verification (5-10 minutes)
- **L2 - Functional Tests**: Complete feature validation (30-60 minutes)
- **L3 - Integration Tests**: Cross-service and data flow testing (1-2 hours)
- **L4 - Performance Tests**: Load and stress testing (2-4 hours)

---

## Test Environment Setup

### Prerequisites

```bash
# Services running
- Gateway Service (port 8080)
- Auth Service (port 8081)
- Book Service (port 8082)
- Circulation Service (port 8083)
- Reader Service (port 8084)
- PostgreSQL (port 5432)
- Redis (port 6379)
- Nacos (port 8848)

# Test accounts
- Admin: username=admin, password=admin123
- Librarian: username=librarian, password=lib123
- Reader: username=reader001, password=reader123
```

### Test Data Requirements

```sql
-- Minimum test data
- 100+ books across different categories
- 50+ registered readers
- 20+ active borrowing records
- 10+ reservation records
- Sample fine records
```

### Environment Variables

```bash
export GATEWAY_URL="http://localhost:8080"
export ADMIN_USERNAME="admin"
export ADMIN_PASSWORD="admin123"
export TEST_ENV="dev"
```

---

## Core Business Flow Test Cases

### 1. User Authentication Flow

#### TC-AUTH-001: Admin Login

**Level**: L1 - Smoke Test
**Priority**: Critical
**Description**: Verify admin user can successfully login

**Preconditions**:

- Admin account exists in database
- Auth service is running

**Test Steps**:

1. Send POST request to `/api/v1/auth/login`
   ```json
   {
     "username": "admin",
     "password": "admin123"
   }
   ```
2. Verify response status code is 200
3. Verify response contains JWT token
4. Extract and store token for subsequent requests

**Expected Result**:

```json
{
  "code": 200,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "username": "admin",
    "roles": ["ADMIN"],
    "expiresIn": 86400
  }
}
```

**Acceptance Criteria**:

- ✅ Status code 200
- ✅ Valid JWT token returned
- ✅ Token contains user ID and roles
- ✅ Token expiration time is correct (24 hours)

---

#### TC-AUTH-002: Reader Login

**Level**: L1 - Smoke Test
**Priority**: Critical
**Description**: Verify reader user can successfully login

**Test Steps**:

1. Send POST request with reader credentials
2. Verify response contains token with READER role
3. Verify permissions are correctly set

**Expected Result**: 200 OK with reader token

---

#### TC-AUTH-003: Invalid Credentials

**Level**: L1 - Smoke Test
**Priority**: High
**Description**: Verify system rejects invalid credentials

**Test Steps**:

1. Send POST request with wrong password
2. Verify response status code is 401
3. Verify error message is returned

**Expected Result**:

```json
{
  "code": 401,
  "message": "Invalid username or password"
}
```

**Acceptance Criteria**:

- ✅ Status code 401
- ✅ Appropriate error message
- ✅ No token returned

---

#### TC-AUTH-004: Token Validation

**Level**: L2 - Functional Test
**Priority**: High
**Description**: Verify JWT token validation works correctly

**Test Steps**:

1. Login to get valid token
2. Send GET request to `/api/v1/auth/validate` with token
3. Verify token is validated successfully

**Expected Result**: 200 OK with user info

---

#### TC-AUTH-005: Token Refresh

**Level**: L2 - Functional Test
**Priority**: Medium
**Description**: Verify token refresh mechanism

**Test Steps**:

1. Login to get initial token
2. Send POST request to `/api/v1/auth/refresh` with current token
3. Verify new token is issued
4. Verify old token is invalidated (if configured)

**Expected Result**: 200 OK with new token

---

#### TC-AUTH-006: Logout

**Level**: L2 - Functional Test
**Priority**: Medium
**Description**: Verify user can logout successfully

**Test Steps**:

1. Login to get token
2. Send POST request to `/api/v1/auth/logout` with token
3. Verify token is blacklisted in Redis
4. Verify subsequent requests with old token are rejected

**Expected Result**:

- Logout returns 200 OK
- Token is invalidated
- Subsequent requests return 401

---

### 2. Book Management Flow

#### TC-BOOK-001: Create New Book

**Level**: L2 - Functional Test
**Priority**: Critical
**Description**: Verify admin can create a new book

**Preconditions**:

- Admin is logged in
- Valid category ID exists

**Test Steps**:

1. Get admin token via login
2. Send POST request to `/api/v1/books`
   ```json
   {
     "title": "Test Book Title",
     "author": "Test Author",
     "isbn": "978-1234567890",
     "publisher": "Test Publisher",
     "publishYear": 2024,
     "categoryId": 1,
     "totalCopies": 10,
     "availableCopies": 10,
     "shelfLocation": "A1-01",
     "description": "Test book description",
     "coverImage": "https://example.com/cover.jpg"
   }
   ```
3. Verify response status 200
4. Verify book ID is returned
5. Verify book is created in database

**Expected Result**:

```json
{
  "code": 200,
  "message": "Book created successfully",
  "data": {
    "id": 123,
    "title": "Test Book Title",
    "isbn": "978-1234567890",
    "status": "AVAILABLE",
    "createdAt": "2024-12-01T10:00:00"
  }
}
```

**Acceptance Criteria**:

- ✅ Book created with correct data
- ✅ Book ID is auto-generated
- ✅ Available copies equals total copies
- ✅ Status is set to AVAILABLE
- ✅ Audit fields (createdAt, createdBy) are populated

---

#### TC-BOOK-002: Get Book Details

**Level**: L1 - Smoke Test
**Priority**: High
**Description**: Verify retrieving book details by ID

**Test Steps**:

1. Create a book (or use existing book ID)
2. Send GET request to `/api/v1/books/{bookId}`
3. Verify response contains complete book information

**Expected Result**: 200 OK with book details

---

#### TC-BOOK-003: Update Book Information

**Level**: L2 - Functional Test
**Priority**: High
**Description**: Verify admin can update book information

**Test Steps**:

1. Create a book
2. Send PUT request to `/api/v1/books/{bookId}` with updated data
3. Verify response status 200
4. Retrieve book and verify changes are persisted

**Expected Result**: Book information is updated correctly

---

#### TC-BOOK-004: Search Books by Keyword

**Level**: L2 - Functional Test
**Priority**: High
**Description**: Verify book search functionality

**Test Steps**:

1. Send GET request to `/api/v1/books?keyword=Java&pageNum=1&pageSize=10`
2. Verify response contains matching books
3. Verify pagination works correctly
4. Verify search matches title, author, and ISBN

**Expected Result**:

```json
{
    "code": 200,
    "data": {
        "records": [...],
        "total": 25,
        "pageNum": 1,
        "pageSize": 10,
        "pages": 3
    }
}
```

---

#### TC-BOOK-005: Filter Books by Category

**Level**: L2 - Functional Test
**Priority**: Medium
**Description**: Verify filtering books by category

**Test Steps**:

1. Send GET request to `/api/v1/books?categoryId=1&pageNum=1&pageSize=20`
2. Verify all returned books belong to the specified category

**Expected Result**: Filtered results match category

---

#### TC-BOOK-006: Update Inventory

**Level**: L2 - Functional Test
**Priority**: High
**Description**: Verify inventory update (on/off shelf)

**Test Steps**:

1. Create a book with status AVAILABLE
2. Send PUT request to `/api/v1/books/{bookId}/status` with status=OFF_SHELF
3. Verify book status is updated
4. Verify book is not available for borrowing

**Expected Result**: Book status updated, not available for borrowing

---

#### TC-BOOK-007: Delete Book (Soft Delete)

**Level**: L2 - Functional Test
**Priority**: Medium
**Description**: Verify soft delete functionality

**Test Steps**:

1. Create a book
2. Send DELETE request to `/api/v1/books/{bookId}`
3. Verify response status 200
4. Verify book is marked as deleted (deleted=true in DB)
5. Verify book does not appear in search results

**Expected Result**: Book is soft deleted, not visible in searches

---

#### TC-BOOK-008: Bulk Import Books

**Level**: L3 - Integration Test
**Priority**: Medium
**Description**: Verify bulk import of books via CSV/Excel

**Test Steps**:

1. Prepare CSV file with 100 book records
2. Send POST request to `/api/v1/books/import` with file
3. Verify import job is created
4. Poll job status until complete
5. Verify all books are imported correctly

**Expected Result**: All valid records imported, error report for invalid records

---

### 3. Reader Management Flow

#### TC-READER-001: Reader Registration

**Level**: L1 - Smoke Test
**Priority**: Critical
**Description**: Verify new reader can register

**Preconditions**:

- Registration endpoint is accessible without authentication

**Test Steps**:

1. Send POST request to `/api/v1/readers/register`
   ```json
   {
     "username": "newreader001",
     "password": "SecurePass123",
     "email": "reader@example.com",
     "phone": "13800138000",
     "realName": "Zhang San",
     "idCard": "110101199001011234",
     "readerType": "STUDENT"
   }
   ```
2. Verify response status 200
3. Verify reader ID is returned
4. Verify library card is auto-generated
5. Verify reader can login with credentials

**Expected Result**:

```json
{
  "code": 200,
  "message": "Registration successful",
  "data": {
    "readerId": 456,
    "username": "newreader001",
    "cardNumber": "LIB-2024-456",
    "readerType": "STUDENT",
    "status": "ACTIVE"
  }
}
```

**Acceptance Criteria**:

- ✅ Reader account created
- ✅ Library card number auto-generated (format: LIB-YYYY-XXXXX)
- ✅ Password is encrypted
- ✅ Default borrow limit and duration set based on reader type
- ✅ Can login immediately after registration

---

#### TC-READER-002: Duplicate Username Prevention

**Level**: L2 - Functional Test
**Priority**: High
**Description**: Verify system prevents duplicate usernames

**Test Steps**:

1. Register a reader with username "testuser001"
2. Attempt to register another reader with same username
3. Verify registration fails with appropriate error

**Expected Result**:

```json
{
  "code": 400,
  "message": "Username already exists"
}
```

---

#### TC-READER-003: Get Reader Profile

**Level**: L2 - Functional Test
**Priority**: High
**Description**: Verify reader can view their profile

**Test Steps**:

1. Login as reader to get token
2. Send GET request to `/api/v1/readers/profile` with token
3. Verify response contains reader information (excluding password)

**Expected Result**: Complete profile information returned

---

#### TC-READER-004: Update Reader Profile

**Level**: L2 - Functional Test
**Priority**: Medium
**Description**: Verify reader can update their profile

**Test Steps**:

1. Login as reader
2. Send PUT request to `/api/v1/readers/profile` with updated info
   ```json
   {
     "email": "newemail@example.com",
     "phone": "13900139000",
     "address": "New Address"
   }
   ```
3. Verify response status 200
4. Retrieve profile and verify changes

**Expected Result**: Profile updated successfully

---

#### TC-READER-005: Change Password

**Level**: L2 - Functional Test
**Priority**: High
**Description**: Verify password change functionality

**Test Steps**:

1. Login as reader
2. Send POST request to `/api/v1/readers/change-password`
   ```json
   {
     "oldPassword": "OldPass123",
     "newPassword": "NewPass456"
   }
   ```
3. Verify response status 200
4. Logout and login with new password
5. Verify login succeeds with new password

**Expected Result**: Password changed, can login with new password

---

#### TC-READER-006: Admin Search Readers

**Level**: L2 - Functional Test
**Priority**: Medium
**Description**: Verify admin can search readers

**Test Steps**:

1. Login as admin
2. Send GET request to `/api/v1/readers?keyword=Zhang&pageNum=1&pageSize=20`
3. Verify response contains matching readers
4. Verify pagination works

**Expected Result**: Search results returned with pagination

---

#### TC-READER-007: Admin Update Reader Status

**Level**: L2 - Functional Test
**Priority**: High
**Description**: Verify admin can activate/suspend reader accounts

**Test Steps**:

1. Login as admin
2. Send PUT request to `/api/v1/readers/{readerId}/status`
   ```json
   {
     "status": "SUSPENDED",
     "reason": "Payment overdue"
   }
   ```
3. Verify reader status is updated
4. Verify suspended reader cannot borrow books

**Expected Result**: Reader status updated, borrowing restricted

---

### 4. Circulation Flow (Borrowing & Returning)

#### TC-CIRC-001: Borrow Book

**Level**: L1 - Smoke Test
**Priority**: Critical
**Description**: Verify reader can borrow a book

**Preconditions**:

- Book is available (availableCopies > 0)
- Reader has not exceeded borrow limit
- Reader has no overdue books or unpaid fines

**Test Steps**:

1. Login as admin/librarian
2. Send POST request to `/api/v1/circulation/borrow`
   ```json
   {
     "readerId": 456,
     "bookId": 123,
     "borrowDays": 30
   }
   ```
3. Verify response status 200
4. Verify borrow record is created
5. Verify book availableCopies is decremented
6. Verify due date is calculated correctly

**Expected Result**:

```json
{
  "code": 200,
  "message": "Book borrowed successfully",
  "data": {
    "borrowId": 789,
    "readerId": 456,
    "bookId": 123,
    "borrowDate": "2024-12-01T10:00:00",
    "dueDate": "2024-12-31T23:59:59",
    "status": "BORROWED"
  }
}
```

**Acceptance Criteria**:

- ✅ Borrow record created
- ✅ Book availableCopies decremented by 1
- ✅ Due date = borrowDate + borrowDays
- ✅ Reader's current borrow count incremented
- ✅ Transaction is atomic (all or nothing)

---

#### TC-CIRC-002: Borrow Limit Check

**Level**: L2 - Functional Test
**Priority**: High
**Description**: Verify system enforces borrow limit

**Test Steps**:

1. Check reader's borrow limit (e.g., 5 books for students)
2. Borrow maximum allowed books
3. Attempt to borrow one more book
4. Verify request is rejected

**Expected Result**:

```json
{
  "code": 400,
  "message": "Borrow limit exceeded. Maximum allowed: 5"
}
```

---

#### TC-CIRC-003: Renew Book

**Level**: L2 - Functional Test
**Priority**: High
**Description**: Verify book renewal functionality

**Preconditions**:

- Book is currently borrowed
- Book has not exceeded renewal limit (e.g., max 2 renewals)
- No reservations for this book

**Test Steps**:

1. Borrow a book
2. Send POST request to `/api/v1/circulation/renew`
   ```json
   {
     "borrowId": 789,
     "renewDays": 14
   }
   ```
3. Verify response status 200
4. Verify due date is extended
5. Verify renewal count is incremented

**Expected Result**:

```json
{
  "code": 200,
  "message": "Book renewed successfully",
  "data": {
    "borrowId": 789,
    "newDueDate": "2025-01-14T23:59:59",
    "renewalCount": 1,
    "maxRenewals": 2
  }
}
```

---

#### TC-CIRC-004: Return Book

**Level**: L1 - Smoke Test
**Priority**: Critical
**Description**: Verify book return process

**Test Steps**:

1. Borrow a book (or use existing borrow record)
2. Send POST request to `/api/v1/circulation/return`
   ```json
   {
     "borrowId": 789
   }
   ```
3. Verify response status 200
4. Verify borrow status is updated to RETURNED
5. Verify book availableCopies is incremented
6. Verify return date is recorded

**Expected Result**:

```json
{
  "code": 200,
  "message": "Book returned successfully",
  "data": {
    "borrowId": 789,
    "returnDate": "2024-12-15T14:30:00",
    "status": "RETURNED",
    "fine": 0
  }
}
```

**Acceptance Criteria**:

- ✅ Borrow record updated with return date
- ✅ Book availableCopies incremented
- ✅ Reader's current borrow count decremented
- ✅ Fine calculated if overdue

---

#### TC-CIRC-005: Overdue Fine Calculation

**Level**: L2 - Functional Test
**Priority**: High
**Description**: Verify overdue fine is calculated correctly

**Test Steps**:

1. Borrow a book with short due date (or manipulate due date in DB)
2. Wait for due date to pass (or set system date forward in test)
3. Return the book
4. Verify fine is calculated based on days overdue
5. Verify fine amount matches configured rate (e.g., 0.5 yuan/day)

**Expected Result**:

```json
{
  "code": 200,
  "message": "Book returned with fine",
  "data": {
    "borrowId": 789,
    "returnDate": "2025-01-05T10:00:00",
    "daysOverdue": 5,
    "fineAmount": 2.5,
    "status": "RETURNED_WITH_FINE"
  }
}
```

---

#### TC-CIRC-006: Get Borrow History

**Level**: L2 - Functional Test
**Priority**: Medium
**Description**: Verify reader can view borrow history

**Test Steps**:

1. Login as reader
2. Send GET request to `/api/v1/circulation/borrows?readerId={readerId}&pageNum=1&pageSize=10`
3. Verify response contains borrow records
4. Verify records include book details and status

**Expected Result**: Paginated list of borrow records

---

#### TC-CIRC-007: Admin View All Borrows

**Level**: L2 - Functional Test
**Priority**: Medium
**Description**: Verify admin can view all borrow records

**Test Steps**:

1. Login as admin
2. Send GET request to `/api/v1/circulation/borrows?status=BORROWED&pageNum=1&pageSize=20`
3. Verify response contains all active borrows
4. Support filtering by status, reader, book, date range

**Expected Result**: Filtered and paginated borrow records

---

### 5. Reservation Flow

#### TC-RESV-001: Make Reservation

**Level**: L2 - Functional Test
**Priority**: High
**Description**: Verify reader can reserve a book

**Preconditions**:

- Book exists but all copies are currently borrowed (availableCopies = 0)
- Reader has not exceeded reservation limit

**Test Steps**:

1. Login as reader
2. Send POST request to `/api/v1/circulation/reservations`
   ```json
   {
     "readerId": 456,
     "bookId": 123
   }
   ```
3. Verify response status 200
4. Verify reservation record is created
5. Verify reservation status is PENDING

**Expected Result**:

```json
{
  "code": 200,
  "message": "Reservation successful",
  "data": {
    "reservationId": 111,
    "readerId": 456,
    "bookId": 123,
    "reservationDate": "2024-12-01T10:00:00",
    "status": "PENDING",
    "expiresAt": "2024-12-08T23:59:59"
  }
}
```

---

#### TC-RESV-002: Cancel Reservation

**Level**: L2 - Functional Test
**Priority**: Medium
**Description**: Verify reader can cancel reservation

**Test Steps**:

1. Make a reservation
2. Send DELETE request to `/api/v1/circulation/reservations/{reservationId}`
3. Verify response status 200
4. Verify reservation status is updated to CANCELLED

**Expected Result**: Reservation cancelled successfully

---

#### TC-RESV-003: Reservation Notification

**Level**: L3 - Integration Test
**Priority**: Medium
**Description**: Verify reader is notified when reserved book becomes available

**Test Steps**:

1. Reader reserves a book (all copies borrowed)
2. Simulate another reader returning the book
3. Verify reservation status changes to AVAILABLE
4. Verify notification is sent to reader (email/SMS/in-app)
5. Verify reservation has pickup deadline (e.g., 3 days)

**Expected Result**: Reader notified, reservation status updated

---

#### TC-RESV-004: Reservation Expiration

**Level**: L3 - Integration Test
**Priority**: Medium
**Description**: Verify expired reservations are handled

**Test Steps**:

1. Reserve a book
2. Book becomes available
3. Wait for pickup deadline to pass (or manipulate time)
4. Run reservation expiration job
5. Verify reservation status changes to EXPIRED
6. Verify next reader in queue is notified (if any)

**Expected Result**: Expired reservation handled, next in queue notified

---

#### TC-RESV-005: Pickup Reserved Book

**Level**: L2 - Functional Test
**Priority**: High
**Description**: Verify reader can pick up reserved book

**Test Steps**:

1. Reserve a book
2. Book becomes available (status = AVAILABLE)
3. Reader comes to pick up
4. Librarian processes pickup via borrow endpoint
5. Verify borrow is created
6. Verify reservation status is updated to FULFILLED

**Expected Result**: Book borrowed, reservation fulfilled

---

### 6. Statistics & Reports

#### TC-STAT-001: Borrow Statistics

**Level**: L2 - Functional Test
**Priority**: Medium
**Description**: Verify borrow statistics API

**Test Steps**:

1. Login as admin
2. Send GET request to `/api/v1/analytics/borrow-stats?startDate=2024-01-01&endDate=2024-12-31`
3. Verify response contains:
   - Total borrows
   - Total returns
   - Currently borrowed
   - Average borrow duration

**Expected Result**: Accurate statistics returned

---

#### TC-STAT-002: Popular Books Report

**Level**: L2 - Functional Test
**Priority**: Medium
**Description**: Verify popular books report

**Test Steps**:

1. Send GET request to `/api/v1/analytics/popular-books?limit=10&period=30d`
2. Verify response contains top 10 most borrowed books in last 30 days
3. Verify books are sorted by borrow count (descending)

**Expected Result**:

```json
{
    "code": 200,
    "data": [
        {
            "bookId": 123,
            "title": "Popular Book 1",
            "borrowCount": 45,
            "rank": 1
        },
        ...
    ]
}
```

---

#### TC-STAT-003: Active Readers Report

**Level**: L2 - Functional Test
**Priority**: Medium
**Description**: Verify active readers statistics

**Test Steps**:

1. Send GET request to `/api/v1/analytics/active-readers?period=30d`
2. Verify response contains readers with most borrows
3. Verify calculation is accurate

**Expected Result**: Top active readers list returned

---

#### TC-STAT-004: Overdue Report

**Level**: L2 - Functional Test
**Priority**: High
**Description**: Verify overdue books report

**Test Steps**:

1. Login as admin
2. Send GET request to `/api/v1/analytics/overdue-books`
3. Verify response contains all overdue borrows
4. Verify includes reader info, book info, days overdue, fine amount

**Expected Result**: Complete overdue list with details

---

## API Integration Test Cases

### TC-INTEG-001: Cross-Service Authentication

**Level**: L3 - Integration Test
**Priority**: Critical
**Description**: Verify JWT token works across all services

**Test Steps**:

1. Login via auth-service to get token
2. Use same token to call book-service API
3. Use same token to call circulation-service API
4. Use same token to call reader-service API
5. Verify all services accept the token

**Expected Result**: Token accepted by all services

---

### TC-INTEG-002: Transaction Consistency

**Level**: L3 - Integration Test
**Priority**: Critical
**Description**: Verify distributed transaction consistency in borrow flow

**Test Steps**:

1. Simulate borrow operation
2. Verify operations across services:
   - Circulation service creates borrow record
   - Book service decrements availableCopies
   - Reader service increments borrow count
3. If any service fails, verify rollback occurs
4. Verify data consistency

**Expected Result**: All or nothing - transaction is atomic

---

### TC-INTEG-003: Service Communication Failure

**Level**: L3 - Integration Test
**Priority**: High
**Description**: Verify system handles service communication failures

**Test Steps**:

1. Stop book-service
2. Attempt to borrow a book
3. Verify appropriate error is returned
4. Verify no partial data is created
5. Restart book-service
6. Verify system recovers

**Expected Result**: Graceful degradation, error handled properly

---

## Frontend E2E Test Cases

### TC-UI-001: Login Page

**Level**: L1 - Smoke Test
**Priority**: Critical
**Description**: Verify login page functionality

**Test Steps** (Playwright):

```javascript
test("Admin can login via web interface", async ({ page }) => {
  await page.goto("http://localhost:3011/login");
  await page.fill('input[name="username"]', "admin");
  await page.fill('input[name="password"]', "admin123");
  await page.click('button[type="submit"]');
  await expect(page).toHaveURL(/.*dashboard/);
});
```

**Expected Result**: Login successful, redirected to dashboard

---

### TC-UI-002: Book Management Page

**Level**: L2 - Functional Test
**Priority**: High
**Description**: Verify book management UI workflow

**Test Steps**:

1. Login as admin
2. Navigate to Books page
3. Click "Add Book" button
4. Fill in book details form
5. Submit form
6. Verify success message
7. Verify new book appears in list

**Expected Result**: Book created via UI successfully

---

### TC-UI-003: Borrow Book UI Flow

**Level**: L2 - Functional Test
**Priority**: High
**Description**: Verify borrowing flow through UI

**Test Steps**:

1. Login as librarian
2. Navigate to Circulation > Borrow
3. Search and select reader
4. Search and select book
5. Set borrow duration
6. Submit form
7. Verify confirmation dialog
8. Verify borrow record appears

**Expected Result**: Borrow processed via UI

---

### TC-UI-004: Search Functionality

**Level**: L2 - Functional Test
**Priority**: Medium
**Description**: Verify search functionality in UI

**Test Steps**:

1. Navigate to Books page
2. Enter search keyword in search box
3. Press Enter or click Search button
4. Verify results are filtered
5. Verify pagination works
6. Clear search and verify full list returns

**Expected Result**: Search filters correctly

---

## Performance & Load Test Cases

### TC-PERF-001: Concurrent Login Load Test

**Level**: L4 - Performance Test
**Priority**: High
**Description**: Verify system handles concurrent logins

**Test Configuration**:

- Virtual Users: 100
- Duration: 5 minutes
- Ramp-up: 30 seconds

**Success Criteria**:

- 95th percentile response time < 500ms
- Error rate < 1%
- No memory leaks
- CPU usage < 80%

---

### TC-PERF-002: Book Search Under Load

**Level**: L4 - Performance Test
**Priority**: High
**Description**: Verify search performance under load

**Test Configuration**:

- Virtual Users: 200
- Search requests per second: 50
- Duration: 10 minutes

**Success Criteria**:

- 95th percentile < 1s
- Throughput > 45 req/s
- No database connection pool exhaustion

---

### TC-PERF-003: Concurrent Borrow Operations

**Level**: L4 - Performance Test
**Priority**: Critical
**Description**: Verify concurrent borrowing doesn't cause data inconsistency

**Test Steps**:

1. Create book with availableCopies = 1
2. Simulate 10 concurrent borrow requests for same book
3. Verify only 1 borrow succeeds
4. Verify 9 requests fail appropriately
5. Verify availableCopies = 0 (not negative)

**Expected Result**: Race condition handled, data remains consistent

---

## Security Test Cases

### TC-SEC-001: SQL Injection Prevention

**Level**: L3 - Security Test
**Priority**: Critical
**Description**: Verify system prevents SQL injection attacks

**Test Steps**:

1. Attempt login with SQL injection payload:
   ```
   username: admin' OR '1'='1
   password: anything
   ```
2. Verify login fails
3. Verify no database error is exposed

**Expected Result**: Attack prevented, safe error message returned

---

### TC-SEC-002: XSS Prevention

**Level**: L3 - Security Test
**Priority**: High
**Description**: Verify XSS attacks are prevented

**Test Steps**:

1. Create book with title containing script tag:
   ```
   <script>alert('XSS')</script>
   ```
2. View book details in UI
3. Verify script is not executed
4. Verify HTML is escaped

**Expected Result**: XSS prevented, content escaped

---

### TC-SEC-003: Unauthorized Access

**Level**: L2 - Security Test
**Priority**: Critical
**Description**: Verify unauthorized access is blocked

**Test Steps**:

1. Attempt to access admin API without token
2. Attempt to access with expired token
3. Attempt to access with reader token to admin endpoint
4. Verify all attempts are rejected with 401/403

**Expected Result**: All unauthorized attempts blocked

---

## Data Integrity Test Cases

### TC-DATA-001: Database Transaction Rollback

**Level**: L3 - Integration Test
**Priority**: Critical
**Description**: Verify database rollback on error

**Test Steps**:

1. Start borrow transaction
2. Simulate error midway (e.g., network timeout)
3. Verify entire transaction is rolled back
4. Verify no partial data remains

**Expected Result**: Complete rollback, data consistency maintained

---

### TC-DATA-002: Concurrent Update Handling

**Level**: L3 - Integration Test
**Priority**: High
**Description**: Verify optimistic locking prevents lost updates

**Test Steps**:

1. Retrieve book details (version = 1)
2. Update book from two different sessions simultaneously
3. Verify first update succeeds (version = 2)
4. Verify second update fails with version conflict

**Expected Result**: Lost update prevented by optimistic locking

---

## Test Execution Guide

### Running Tests

#### 1. Complete E2E Test Suite

```bash
cd deployment/scripts
./e2e-test.sh --full --report
```

#### 2. Quick Smoke Tests

```bash
./e2e-test.sh --quick
```

#### 3. Production Environment Tests

```bash
./e2e-test.sh --env prod --full
```

#### 4. Frontend E2E Tests

```bash
cd web-admin
npm run test:e2e
```

### Continuous Integration

```yaml
# .gitlab-ci.yml example
e2e_tests:
  stage: test
  script:
    - docker-compose up -d
    - ./deployment/scripts/wait-for-healthy.sh
    - ./deployment/scripts/e2e-test.sh --full --report
  artifacts:
    reports:
      junit: deployment/test-reports/*.xml
    paths:
      - deployment/test-reports/
```

---

## Expected Results & Acceptance Criteria

### Overall System Health

- ✅ All services respond to health checks
- ✅ Service discovery working (Nacos)
- ✅ Database connections stable
- ✅ Redis cache functional

### Functional Requirements

- ✅ All core business flows work end-to-end
- ✅ Error handling is graceful
- ✅ Validation messages are clear
- ✅ Data integrity is maintained

### Performance Requirements

- ✅ Login: < 500ms (95th percentile)
- ✅ Search: < 1s (95th percentile)
- ✅ Borrow/Return: < 2s (95th percentile)
- ✅ Concurrent operations: No race conditions

### Security Requirements

- ✅ Authentication required for protected endpoints
- ✅ Authorization enforced based on roles
- ✅ Input validation prevents injection attacks
- ✅ Sensitive data (passwords) encrypted

### Test Coverage

- ✅ Unit tests: > 80% code coverage
- ✅ Integration tests: All critical flows
- ✅ E2E tests: All user journeys
- ✅ Performance tests: Key operations under load

---

## Appendix

### Test Data Files

- `/deployment/test-data/books.csv` - Sample book data
- `/deployment/test-data/readers.json` - Sample reader data
- `/deployment/test-data/borrows.sql` - Sample borrow records

### Test Report Examples

- HTML Report: `/deployment/test-reports/e2e-report-*.html`
- JSON Report: `/deployment/test-reports/e2e-results-*.json`
- JUnit XML: `/deployment/test-reports/junit-*.xml` (for CI integration)

### Troubleshooting

- If tests fail due to timeout: Increase `TIMEOUT` in e2e-test.sh
- If services not ready: Run `./wait-for-healthy.sh` first
- If data conflicts: Run `./prepare-test-data.sh --clean` to reset

---

**Document Owner**: QA Team
**Review Cycle**: Monthly
**Last Review**: 2025-12-01
**Next Review**: 2026-01-01
