# GCRF Library Management System - API Reference

**Version**: 1.0.0
**Base URL**: `http://localhost:8080` (Development) | `https://api.gcrf-library.com` (Production)
**Last Updated**: 2024-11-02

---

## Table of Contents

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [Common Response Format](#common-response-format)
4. [API Endpoints](#api-endpoints)
   - [Authentication APIs](#authentication-apis)
   - [User Management APIs](#user-management-apis)
   - [Book Management APIs](#book-management-apis)
   - [Reader Management APIs](#reader-management-apis)
   - [Circulation Management APIs](#circulation-management-apis)
   - [Role Management APIs](#role-management-apis)
   - [System Health Check APIs](#system-health-check-apis)
5. [Error Codes](#error-codes)
6. [Rate Limiting](#rate-limiting)

---

## Overview

The GCRF (国创睿峰) Library Management System provides a comprehensive RESTful API for managing library operations including:

- **Authentication**: JWT-based user authentication and authorization
- **Book Catalog**: Complete book management with search and filters
- **Reader Management**: Library member registration and card management
- **Circulation**: Book borrowing, returning, and renewal operations
- **System Administration**: User, role, and permission management

**Architecture**: Microservices with Spring Cloud Gateway
**Authentication**: JWT Bearer tokens
**Response Format**: JSON with unified wrapper structure

---

## Authentication

### JWT Bearer Token

Most API endpoints require authentication using JWT (JSON Web Token) Bearer tokens.

**How to authenticate**:

1. **Login** using the `/api/v1/auth/login` endpoint to obtain an access token
2. **Include the token** in the `Authorization` header for subsequent requests:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Token Expiration**: Access tokens expire after 2 hours (7200 seconds)

**Token Refresh**: Use `/api/v1/auth/refresh` to get a new token before expiration

**Token Invalidation**: Use `/api/v1/auth/logout` to invalidate the token (adds to blacklist)

### Public Endpoints

The following endpoints do NOT require authentication:
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/refresh` - Refresh token
- `GET /api/v1/books/{id}` - Get book details (public catalog)
- `GET /api/v1/*/health` - Health check endpoints

---

## Common Response Format

All API responses follow a unified JSON structure:

### Success Response

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    // Response data varies by endpoint
  },
  "timestamp": 1698765432000,
  "traceId": "trace-abc123xyz"
}
```

**Fields**:
- `code` (integer): Response code (200 for success)
- `message` (string): Human-readable message
- `data` (any): Response data (null for operations with no return value)
- `timestamp` (integer): Unix timestamp in milliseconds
- `traceId` (string): Request trace ID for debugging

### Error Response

```json
{
  "code": 400,
  "message": "参数验证失败：用户名不能为空",
  "data": null,
  "timestamp": 1698765432000,
  "traceId": "trace-abc123xyz"
}
```

### Paginated Response

For list endpoints with pagination:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [...],      // Array of records
    "total": 100,          // Total number of records
    "pageNum": 1,          // Current page (1-based)
    "pageSize": 10,        // Page size
    "pages": 10            // Total pages
  },
  "timestamp": 1698765432000
}
```

---

## API Endpoints

### Authentication APIs

#### 1. User Login

**Endpoint**: `POST /api/v1/auth/login`
**Authentication**: Not required
**Description**: Authenticate user with username and password, returns JWT access token

**Request Body**:
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "userId": 1,
    "username": "admin",
    "userType": "ADMIN"
  },
  "timestamp": 1698765432000
}
```

**Error Responses**:
- `401 Unauthorized`: Invalid username or password
- `400 Bad Request`: Missing required fields

**Example using cURL**:
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

---

#### 2. Refresh Token

**Endpoint**: `POST /api/v1/auth/refresh`
**Authentication**: Not required
**Description**: Refresh JWT token before expiration

**Request Body**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "userId": 1,
    "username": "admin",
    "userType": "ADMIN"
  },
  "timestamp": 1698765432000
}
```

---

#### 3. User Logout

**Endpoint**: `POST /api/v1/auth/logout`
**Authentication**: Required
**Description**: Logout current user and invalidate JWT token

**Headers**:
```
Authorization: Bearer <your-token>
```

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1698765432000
}
```

---

#### 4. Get Current User Info

**Endpoint**: `GET /api/v1/auth/info`
**Authentication**: Required
**Description**: Retrieve detailed information of the currently authenticated user

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 1,
    "username": "admin",
    "userType": "ADMIN",
    "status": "ACTIVE",
    "email": "admin@gcrf-library.com",
    "phone": "13800138000",
    "createdAt": "2024-10-12T08:30:00",
    "updatedAt": "2024-10-28T15:20:00"
  },
  "timestamp": 1698765432000
}
```

---

#### 5. Validate Token

**Endpoint**: `GET /api/v1/auth/validate`
**Authentication**: Required
**Description**: Check if the provided JWT token is valid

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": true,
  "timestamp": 1698765432000
}
```

---

### User Management APIs

#### 1. List Users with Pagination

**Endpoint**: `GET /api/v1/users`
**Authentication**: Required (Admin only)
**Description**: Query user list with filters

**Query Parameters**:
- `pageNum` (integer, default: 1): Page number (1-based)
- `pageSize` (integer, default: 10): Page size (max: 100)
- `username` (string, optional): Username keyword (fuzzy search)
- `userType` (string, optional): User type (STUDENT, TEACHER, ADMIN)
- `status` (string, optional): Account status (ACTIVE, INACTIVE, LOCKED)

**Example Request**:
```bash
GET /api/v1/users?pageNum=1&pageSize=20&userType=STUDENT&status=ACTIVE
```

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "userId": 10,
        "username": "student001",
        "userType": "STUDENT",
        "status": "ACTIVE",
        "email": "student001@example.com",
        "phone": "13912345678",
        "createdAt": "2024-10-15T09:00:00"
      }
    ],
    "total": 50,
    "current": 1,
    "size": 20,
    "pages": 3
  },
  "timestamp": 1698765432000
}
```

---

#### 2. Create User

**Endpoint**: `POST /api/v1/users`
**Authentication**: Required (Admin only)
**Description**: Create a new user account

**Request Body**:
```json
{
  "username": "newuser",
  "password": "password123",
  "userType": "STUDENT",
  "email": "newuser@example.com",
  "phone": "13800138000"
}
```

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 11,
    "username": "newuser",
    "userType": "STUDENT",
    "status": "ACTIVE",
    "email": "newuser@example.com",
    "phone": "13800138000",
    "createdAt": "2024-11-02T10:30:00"
  },
  "timestamp": 1698765432000
}
```

---

#### 3. Update User

**Endpoint**: `PUT /api/v1/users/{userId}`
**Authentication**: Required (Admin only)
**Description**: Update user information

**Path Parameters**:
- `userId` (integer): User ID

**Request Body**:
```json
{
  "email": "updated@example.com",
  "phone": "13900139000",
  "status": "ACTIVE"
}
```

**Response** (200 OK): Same as Create User response

---

#### 4. Delete User

**Endpoint**: `DELETE /api/v1/users/{userId}`
**Authentication**: Required (Admin only)
**Description**: Soft delete user account

**Path Parameters**:
- `userId` (integer): User ID

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1698765432000
}
```

---

#### 5. Change Password

**Endpoint**: `PUT /api/v1/users/{userId}/password`
**Authentication**: Required
**Description**: Change user password

**Request Body**:
```json
{
  "oldPassword": "oldpass123",
  "newPassword": "newpass456"
}
```

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1698765432000
}
```

---

### Book Management APIs

#### 1. List Books with Pagination

**Endpoint**: `GET /api/v1/books`
**Authentication**: Not required
**Description**: Query book catalog with search, filters and pagination

**Query Parameters**:
- `pageNum` (integer, default: 1): Page number
- `pageSize` (integer, default: 10): Page size
- `keyword` (string, optional): Search keyword (title, author, ISBN)
- `classificationCode` (string, optional): Classification code filter
- `publisher` (string, optional): Publisher filter
- `status` (string, optional): Book status (ACTIVE, INACTIVE)

**Example Request**:
```bash
GET /api/v1/books?pageNum=1&pageSize=20&keyword=Spring&classificationCode=TP312
```

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "isbn": "9787115551234",
        "title": "Spring Boot实战",
        "author": "张三",
        "publisher": "人民邮电出版社",
        "publishDate": "2023-06-01",
        "price": 89.00,
        "totalQuantity": 10,
        "availableQuantity": 8,
        "status": "ACTIVE",
        "coverUrl": "https://cdn.gcrf-library.com/covers/book1.jpg"
      }
    ],
    "total": 150,
    "pageNum": 1,
    "pageSize": 20,
    "pages": 8
  },
  "timestamp": 1698765432000
}
```

---

#### 2. Get Book Details

**Endpoint**: `GET /api/v1/books/{id}`
**Authentication**: Not required
**Description**: Retrieve detailed information of a specific book

**Path Parameters**:
- `id` (integer): Book ID

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "isbn": "9787115551234",
    "title": "Spring Boot实战",
    "subtitle": "从入门到精通",
    "author": "张三",
    "translator": "李四",
    "publisher": "人民邮电出版社",
    "publishDate": "2023-06-01",
    "edition": "第1版",
    "pages": 520,
    "price": 89.00,
    "binding": "平装",
    "language": "Chinese",
    "classificationCode": "TP312",
    "subjectKeywords": "Java, Spring Boot, 微服务",
    "description": "本书全面介绍Spring Boot框架的核心概念和实战技巧...",
    "coverUrl": "https://cdn.gcrf-library.com/covers/book1.jpg",
    "totalQuantity": 10,
    "availableQuantity": 8,
    "status": "ACTIVE",
    "createdAt": "2024-10-12T08:30:00",
    "updatedAt": "2024-10-28T15:20:00"
  },
  "timestamp": 1698765432000
}
```

---

#### 3. Create Book

**Endpoint**: `POST /api/v1/books`
**Authentication**: Required (Admin/Librarian)
**Description**: Add a new book to the catalog

**Request Body**:
```json
{
  "isbn": "9787115551234",
  "title": "Spring Boot实战",
  "subtitle": "从入门到精通",
  "author": "张三",
  "publisher": "人民邮电出版社",
  "publishDate": "2023-06-01",
  "edition": "第1版",
  "pages": 520,
  "price": 89.00,
  "binding": "平装",
  "language": "Chinese",
  "classificationCode": "TP312",
  "subjectKeywords": "Java, Spring Boot, 微服务",
  "description": "本书全面介绍Spring Boot框架...",
  "coverUrl": "https://cdn.gcrf-library.com/covers/book1.jpg",
  "totalQuantity": 10,
  "availableQuantity": 10
}
```

**Response** (200 OK): Same as Get Book Details response

**Error Responses**:
- `400 Bad Request`: Validation errors (missing required fields, invalid ISBN format)
- `403 Forbidden`: Insufficient permissions

---

#### 4. Update Book

**Endpoint**: `PUT /api/v1/books/{id}`
**Authentication**: Required (Admin/Librarian)
**Description**: Update book information

**Path Parameters**:
- `id` (integer): Book ID

**Request Body** (partial update allowed):
```json
{
  "price": 79.00,
  "description": "Updated description...",
  "totalQuantity": 12,
  "availableQuantity": 10,
  "status": "ACTIVE"
}
```

**Response** (200 OK): Updated book details

---

#### 5. Delete Book

**Endpoint**: `DELETE /api/v1/books/{id}`
**Authentication**: Required (Admin)
**Description**: Soft delete a book from catalog

**Path Parameters**:
- `id` (integer): Book ID

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1698765432000
}
```

---

### Reader Management APIs

#### 1. List Readers with Pagination

**Endpoint**: `GET /api/v1/readers`
**Authentication**: Required
**Description**: Query reader list with filters

**Query Parameters**:
- `pageNum` (integer, default: 1): Page number
- `pageSize` (integer, default: 10): Page size
- `keyword` (string, optional): Search keyword (name, readerId, phone)
- `readerType` (string, optional): Reader type (STUDENT, TEACHER, STAFF, PUBLIC)
- `cardStatus` (string, optional): Card status (PENDING, ACTIVE, SUSPENDED, CANCELLED)

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "readerId": "R202400001",
        "name": "王小明",
        "readerType": "STUDENT",
        "cardStatus": "ACTIVE",
        "phone": "13912345678",
        "email": "wangxiaoming@example.com",
        "currentBorrowedCount": 3,
        "maxBorrowQuantity": 5
      }
    ],
    "total": 200,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 20
  },
  "timestamp": 1698765432000
}
```

---

#### 2. Get Reader Details

**Endpoint**: `GET /api/v1/readers/{id}`
**Authentication**: Required
**Description**: Retrieve detailed information of a specific reader

**Path Parameters**:
- `id` (integer): Reader ID

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "readerId": "R202400001",
    "name": "王小明",
    "gender": "MALE",
    "idCard": "110101199001011234",
    "phone": "13912345678",
    "email": "wangxiaoming@example.com",
    "readerType": "STUDENT",
    "department": "计算机科学与技术学院",
    "majorOrPosition": "软件工程",
    "cardStatus": "ACTIVE",
    "depositAmount": 10000,
    "currentBorrowedCount": 3,
    "maxBorrowQuantity": 5,
    "totalBorrowedCount": 25,
    "overdueCount": 0,
    "createdAt": "2024-09-01T10:00:00",
    "updatedAt": "2024-10-28T14:30:00"
  },
  "timestamp": 1698765432000
}
```

---

#### 3. Create Reader

**Endpoint**: `POST /api/v1/readers`
**Authentication**: Required
**Description**: Register a new library reader and generate library card

**Request Body**:
```json
{
  "readerId": "R202400001",
  "name": "王小明",
  "gender": "MALE",
  "idCard": "110101199001011234",
  "phone": "13912345678",
  "email": "wangxiaoming@example.com",
  "readerType": "STUDENT",
  "department": "计算机科学与技术学院",
  "majorOrPosition": "软件工程",
  "depositAmount": 10000,
  "maxBorrowQuantity": 5,
  "remarks": ""
}
```

**Validation Rules**:
- `readerId`: Required, max 50 characters, must be unique
- `name`: Required, max 100 characters
- `gender`: MALE, FEMALE, or OTHER
- `idCard`: Valid Chinese ID card format (18 digits)
- `phone`: Valid Chinese mobile number (11 digits, starts with 1)
- `email`: Valid email format
- `readerType`: Required (STUDENT, TEACHER, STAFF, PUBLIC)
- `depositAmount`: Non-negative integer (in cents/分)
- `maxBorrowQuantity`: 1-100

**Response** (200 OK): Same as Get Reader Details response

---

#### 4. Update Reader

**Endpoint**: `PUT /api/v1/readers/{id}`
**Authentication**: Required
**Description**: Update reader information

**Path Parameters**:
- `id` (integer): Reader ID

**Request Body** (partial update):
```json
{
  "phone": "13900139000",
  "email": "new.email@example.com",
  "department": "软件学院",
  "maxBorrowQuantity": 8
}
```

**Response** (200 OK): Updated reader details

---

#### 5. Activate Library Card

**Endpoint**: `POST /api/v1/readers/{id}/activate`
**Authentication**: Required
**Description**: Activate reader's library card to enable borrowing

**Path Parameters**:
- `id` (integer): Reader ID

**Response** (200 OK): Updated reader details with `cardStatus: "ACTIVE"`

---

#### 6. Suspend Library Card

**Endpoint**: `POST /api/v1/readers/{id}/suspend`
**Authentication**: Required
**Description**: Suspend reader's library card (report lost)

**Path Parameters**:
- `id` (integer): Reader ID

**Response** (200 OK): Updated reader details with `cardStatus: "SUSPENDED"`

---

#### 7. Cancel Library Card

**Endpoint**: `POST /api/v1/readers/{id}/cancel`
**Authentication**: Required
**Description**: Cancel reader's library card permanently

**Path Parameters**:
- `id` (integer): Reader ID

**Response** (200 OK): Updated reader details with `cardStatus: "CANCELLED"`

---

### Circulation Management APIs

#### 1. Borrow a Book

**Endpoint**: `POST /api/v1/circulation/borrow`
**Authentication**: Required
**Description**: Create a borrowing record and decrease book inventory

**Request Body**:
```json
{
  "readerId": 1,
  "bookId": 10
}
```

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 100,
    "readerId": 1,
    "bookId": 10,
    "borrowDate": "2024-10-28T14:30:00",
    "dueDate": "2024-11-27T14:30:00",
    "returnDate": null,
    "renewCount": 0,
    "status": 0,
    "overdueDays": 0,
    "fine": 0
  },
  "timestamp": 1698765432000
}
```

**Business Rules**:
- Reader must have ACTIVE card status
- Book must be available (availableQuantity > 0)
- Reader must not exceed maxBorrowQuantity
- Reader must not have overdue books
- Default borrowing period: 30 days

**Error Responses**:
- `400 Bad Request`: Validation errors, business rule violations
- `404 Not Found`: Reader or book not found

---

#### 2. Return a Book

**Endpoint**: `POST /api/v1/circulation/return/{recordId}`
**Authentication**: Required
**Description**: Complete a borrowing record and increase book inventory

**Path Parameters**:
- `recordId` (integer): Circulation record ID

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 100,
    "readerId": 1,
    "bookId": 10,
    "borrowDate": "2024-10-28T14:30:00",
    "dueDate": "2024-11-27T14:30:00",
    "returnDate": "2024-11-15T10:20:00",
    "renewCount": 0,
    "status": 1,
    "overdueDays": 0,
    "fine": 0
  },
  "timestamp": 1698765432000
}
```

**Business Rules**:
- If returned after due date, calculate overdue days and fine
- Fine rate: 0.5 yuan/day (stored in cents: 50 cents/day)
- Updates book availableQuantity (+1)

---

#### 3. Renew a Book

**Endpoint**: `POST /api/v1/circulation/renew/{recordId}`
**Authentication**: Required
**Description**: Extend the borrowing period

**Path Parameters**:
- `recordId` (integer): Circulation record ID

**Query Parameters**:
- `renewDays` (integer, default: 30): Number of days to extend (max: 90)

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 100,
    "readerId": 1,
    "bookId": 10,
    "borrowDate": "2024-10-28T14:30:00",
    "dueDate": "2024-12-27T14:30:00",
    "returnDate": null,
    "renewCount": 1,
    "status": 0,
    "overdueDays": 0,
    "fine": 0
  },
  "timestamp": 1698765432000
}
```

**Business Rules**:
- Book cannot be overdue
- Maximum renewal count: 3 times
- Extends dueDate by renewDays

---

#### 4. Get Reader's Circulation Records

**Endpoint**: `GET /api/v1/circulation/reader/{readerId}`
**Authentication**: Required
**Description**: Query circulation history for a specific reader

**Path Parameters**:
- `readerId` (integer): Reader ID

**Query Parameters**:
- `status` (integer, optional): Record status filter
  - `0` = Borrowed (currently borrowed)
  - `1` = Returned
  - `2` = Overdue

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 100,
      "readerId": 1,
      "bookId": 10,
      "borrowDate": "2024-10-28T14:30:00",
      "dueDate": "2024-11-27T14:30:00",
      "returnDate": null,
      "renewCount": 0,
      "status": 0,
      "overdueDays": 0,
      "fine": 0
    }
  ],
  "timestamp": 1698765432000
}
```

---

### Role Management APIs

#### 1. List Roles with Pagination

**Endpoint**: `GET /api/v1/roles`
**Authentication**: Required (Admin only)
**Description**: Query role list with filters

**Query Parameters**:
- `pageNum` (integer, default: 1): Page number
- `pageSize` (integer, default: 10): Page size
- `roleCode` (string, optional): Role code filter
- `roleName` (string, optional): Role name filter
- `status` (string, optional): Role status (ACTIVE, INACTIVE)

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "roleCode": "LIBRARIAN",
        "roleName": "图书管理员",
        "description": "负责图书入库、借还等日常管理",
        "status": "ACTIVE",
        "sortOrder": 10
      }
    ],
    "total": 5,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 1
  },
  "timestamp": 1698765432000
}
```

---

#### 2. Create Role

**Endpoint**: `POST /api/v1/roles`
**Authentication**: Required (Admin only)
**Description**: Create a new role

**Request Body**:
```json
{
  "roleCode": "LIBRARIAN",
  "roleName": "图书管理员",
  "description": "负责图书入库、借还等日常管理",
  "sortOrder": 10
}
```

**Response** (200 OK): Role details with ID and timestamps

---

#### 3. Assign Permissions to Role

**Endpoint**: `POST /api/v1/roles/{id}/permissions`
**Authentication**: Required (Admin only)
**Description**: Assign multiple permissions to a role

**Path Parameters**:
- `id` (integer): Role ID

**Request Body**:
```json
{
  "permissionIds": [1, 2, 3, 5, 8, 13]
}
```

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1698765432000
}
```

---

#### 4. Get Role Permissions

**Endpoint**: `GET /api/v1/roles/{id}/permissions`
**Authentication**: Required
**Description**: Retrieve list of permissions assigned to role

**Path Parameters**:
- `id` (integer): Role ID

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "permissionCode": "book:create",
      "permissionName": "创建图书",
      "permissionType": "API",
      "resourcePath": "/api/v1/books",
      "description": "允许创建新图书"
    }
  ],
  "timestamp": 1698765432000
}
```

---

### System Health Check APIs

All health check endpoints are public and do not require authentication.

#### Auth Service Health

```
GET /api/v1/auth/health
```

#### Book Service Health

```
GET /api/v1/books/health
```

#### Reader Service Health

```
GET /api/v1/readers/health
```

#### Circulation Service Health

```
GET /api/v1/circulation/health
```

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "Auth Service is running",
  "timestamp": 1698765432000
}
```

---

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 200 | 操作成功 | Success |
| 400 | 参数验证失败 | Validation error, missing or invalid parameters |
| 401 | 未提供认证令牌 / 令牌无效或已过期 | Authentication required or token invalid |
| 403 | 权限不足 | Insufficient permissions |
| 404 | 请求的资源不存在 | Resource not found |
| 500 | 服务器内部错误 | Internal server error |
| 1001 | 用户名或密码错误 | Invalid credentials |
| 1002 | 用户已被锁定 | User account locked |
| 2001 | 图书库存不足 | Book not available |
| 2002 | ISBN已存在 | Duplicate ISBN |
| 3001 | 读者证号已存在 | Reader ID already exists |
| 3002 | 已达到最大借阅数量 | Max borrow limit reached |
| 3003 | 存在未归还图书 | Has unreturned books |
| 4001 | 续借次数已达上限 | Max renewal count reached |
| 4002 | 图书已逾期，无法续借 | Book overdue, cannot renew |

For complete error code reference, see [ERROR_CODES.md](./ERROR_CODES.md)

---

## Rate Limiting

**Rate Limits**:
- Anonymous requests: 100 requests/hour
- Authenticated requests: 1000 requests/hour
- Admin requests: 5000 requests/hour

**Rate Limit Headers**:
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 995
X-RateLimit-Reset: 1698765432
```

When rate limit is exceeded:
```json
{
  "code": 429,
  "message": "请求过于频繁，请稍后再试",
  "data": null,
  "timestamp": 1698765432000
}
```

---

## Support

For API support and questions:
- **Email**: support@gcrf-library.com
- **Documentation**: https://docs.gcrf-library.com
- **Issue Tracker**: https://github.com/gcrf-library/issues

---

**Last Updated**: 2024-11-02
**Version**: 1.0.0
