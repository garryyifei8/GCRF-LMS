# 人脸识别API接口规范文档

## 1. 概述

### 1.1 文档说明
本文档定义了国创睿峰智能图书馆管理系统人脸识别相关的所有API接口规范，包括人脸注册、识别、管理等功能的详细接口定义。

### 1.2 基本信息
- **API版本**: v1.0.0
- **基础路径**: `https://api.library.gcrf.com/api/v1`
- **协议**: HTTPS
- **数据格式**: JSON / Multipart Form Data
- **字符编码**: UTF-8

### 1.3 通用规范

#### 1.3.1 请求头
```http
Content-Type: application/json | multipart/form-data
Authorization: Bearer {token}
X-Request-ID: {uuid}
X-Client-Version: {version}
X-Device-ID: {device_id}
```

#### 1.3.2 响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1696915200000,
  "request_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### 1.3.3 错误码定义
| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| 200 | 200 | 成功 |
| 400 | 400 | 请求参数错误 |
| 401 | 401 | 未授权/认证失败 |
| 403 | 403 | 禁止访问 |
| 404 | 404 | 资源不存在 |
| 409 | 409 | 资源冲突 |
| 429 | 429 | 请求过于频繁 |
| 500 | 500 | 服务器内部错误 |
| 503 | 503 | 服务暂时不可用 |

### 1.4 认证机制
- **JWT Token**: 用于API访问认证
- **API Key**: 用于服务间调用
- **签名验证**: 关键接口需要请求签名

## 2. 人脸认证接口

### 2.1 人脸注册

#### 2.1.1 注册新人脸
**接口地址**: `POST /face/register`

**接口说明**: 为用户注册新的人脸特征

**请求方式**: POST (multipart/form-data)

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| user_id | string | 是 | 用户ID |
| user_type | string | 是 | 用户类型(student/teacher/admin) |
| face_image | file | 是 | 人脸图片文件(支持jpg/png/jpeg, 最大5MB) |
| is_primary | boolean | 否 | 是否设为主人脸(默认false) |
| metadata | object | 否 | 附加元数据 |

**请求示例**:
```bash
curl -X POST https://api.library.gcrf.com/api/v1/face/register \
  -H "Authorization: Bearer {token}" \
  -F "user_id=STU2024001" \
  -F "user_type=student" \
  -F "face_image=@/path/to/image.jpg" \
  -F "is_primary=true"
```

**响应参数**:
| 参数名 | 类型 | 说明 |
|--------|------|------|
| face_id | string | 人脸特征ID |
| user_id | string | 用户ID |
| quality_score | float | 图片质量分数(0-1) |
| face_image_url | string | 存储的人脸图片URL |
| feature_count | integer | 该用户已注册的人脸数量 |
| created_at | string | 创建时间(ISO 8601) |

**响应示例**:
```json
{
  "code": 200,
  "message": "人脸注册成功",
  "data": {
    "face_id": "FACE_20251010_001",
    "user_id": "STU2024001",
    "quality_score": 0.95,
    "face_image_url": "https://storage.gcrf.com/faces/STU2024001_1.jpg",
    "feature_count": 1,
    "created_at": "2025-10-10T10:00:00Z"
  }
}
```

**错误响应**:
```json
{
  "code": 400,
  "message": "人脸质量不符合要求",
  "data": {
    "quality_issues": [
      "光线过暗",
      "人脸角度过大",
      "遮挡面积超过阈值"
    ],
    "quality_score": 0.45,
    "min_required_score": 0.70
  }
}
```

#### 2.1.2 批量注册
**接口地址**: `POST /face/batch-register`

**接口说明**: 批量注册多个用户的人脸

**请求方式**: POST (multipart/form-data)

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| faces | array | 是 | 人脸数据数组(最多10个) |
| faces[].user_id | string | 是 | 用户ID |
| faces[].user_type | string | 是 | 用户类型 |
| faces[].face_image | file | 是 | 人脸图片 |

**响应示例**:
```json
{
  "code": 200,
  "message": "批量注册完成",
  "data": {
    "total": 10,
    "success": 8,
    "failed": 2,
    "results": [
      {
        "user_id": "STU2024001",
        "status": "success",
        "face_id": "FACE_20251010_001"
      },
      {
        "user_id": "STU2024002",
        "status": "failed",
        "error": "人脸检测失败"
      }
    ]
  }
}
```

### 2.2 人脸登录

#### 2.2.1 人脸识别登录
**接口地址**: `POST /face/login`

**接口说明**: 使用人脸进行身份认证登录

**请求方式**: POST (multipart/form-data)

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| face_image | file | 是 | 待识别的人脸图片 |
| user_type | string | 否 | 限定用户类型范围 |
| device_id | string | 否 | 设备ID |
| location | object | 否 | 位置信息 |
| location.latitude | float | 否 | 纬度 |
| location.longitude | float | 否 | 经度 |

**请求示例**:
```bash
curl -X POST https://api.library.gcrf.com/api/v1/face/login \
  -F "face_image=@/path/to/face.jpg" \
  -F "user_type=student" \
  -F "device_id=DEVICE_001"
```

**响应参数**:
| 参数名 | 类型 | 说明 |
|--------|------|------|
| user_id | string | 识别到的用户ID |
| user_name | string | 用户姓名 |
| user_type | string | 用户类型 |
| department | string | 部门/院系 |
| similarity_score | float | 相似度分数(0-1) |
| access_token | string | 访问令牌 |
| refresh_token | string | 刷新令牌 |
| expires_in | integer | 令牌有效期(秒) |
| permissions | array | 用户权限列表 |

**响应示例**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "user_id": "STU2024001",
    "user_name": "张三",
    "user_type": "student",
    "department": "计算机科学与技术学院",
    "similarity_score": 0.98,
    "access_token": "eyJhbGciOiJIUzI1NiIs...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
    "expires_in": 7200,
    "permissions": ["book.borrow", "book.return", "book.search"]
  }
}
```

#### 2.2.2 活体检测
**接口地址**: `POST /face/liveness`

**接口说明**: 进行活体检测，防止照片/视频攻击

**请求方式**: POST (multipart/form-data)

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| face_images | array | 是 | 多张人脸图片(3-5张) |
| action_type | string | 否 | 动作类型(blink/mouth/yaw/pitch) |
| session_id | string | 否 | 会话ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "活体检测通过",
  "data": {
    "is_alive": true,
    "confidence": 0.99,
    "face_token": "TOKEN_20251010_001",
    "valid_until": "2025-10-10T10:05:00Z"
  }
}
```

### 2.3 人脸验证

#### 2.3.1 1:1人脸验证
**接口地址**: `POST /face/verify`

**接口说明**: 验证人脸是否属于指定用户

**请求方式**: POST (multipart/form-data)

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| user_id | string | 是 | 待验证的用户ID |
| face_image | file | 是 | 待验证的人脸图片 |
| threshold | float | 否 | 相似度阈值(默认0.8) |

**响应示例**:
```json
{
  "code": 200,
  "message": "验证成功",
  "data": {
    "is_match": true,
    "similarity": 0.95,
    "threshold": 0.80,
    "face_id": "FACE_20251010_001"
  }
}
```

#### 2.3.2 1:N人脸搜索
**接口地址**: `POST /face/search`

**接口说明**: 在指定范围内搜索匹配的人脸

**请求方式**: POST (multipart/form-data)

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| face_image | file | 是 | 待搜索的人脸图片 |
| group_id | string | 否 | 搜索组ID |
| user_type | string | 否 | 限定用户类型 |
| top_k | integer | 否 | 返回Top K结果(默认5) |
| threshold | float | 否 | 最低相似度阈值 |

**响应示例**:
```json
{
  "code": 200,
  "message": "搜索完成",
  "data": {
    "search_count": 1000,
    "match_count": 3,
    "matches": [
      {
        "user_id": "STU2024001",
        "user_name": "张三",
        "similarity": 0.95,
        "face_id": "FACE_20251010_001"
      },
      {
        "user_id": "STU2024002",
        "user_name": "李四",
        "similarity": 0.82,
        "face_id": "FACE_20251010_002"
      }
    ]
  }
}
```

## 3. 人脸管理接口

### 3.1 人脸更新

#### 3.1.1 更新人脸特征
**接口地址**: `PUT /face/{user_id}`

**接口说明**: 更新用户的人脸特征

**请求方式**: PUT (multipart/form-data)

**路径参数**:
| 参数名 | 类型 | 说明 |
|--------|------|------|
| user_id | string | 用户ID |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| face_image | file | 是 | 新的人脸图片 |
| face_id | string | 否 | 要更新的特定人脸ID |
| replace_all | boolean | 否 | 是否替换所有人脸 |

**响应示例**:
```json
{
  "code": 200,
  "message": "人脸更新成功",
  "data": {
    "face_id": "FACE_20251010_003",
    "user_id": "STU2024001",
    "quality_score": 0.96,
    "previous_face_id": "FACE_20251010_001",
    "updated_at": "2025-10-10T11:00:00Z"
  }
}
```

#### 3.1.2 删除人脸特征
**接口地址**: `DELETE /face/{user_id}`

**接口说明**: 删除用户的人脸特征

**请求方式**: DELETE

**路径参数**:
| 参数名 | 类型 | 说明 |
|--------|------|------|
| user_id | string | 用户ID |

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| face_id | string | 否 | 删除特定人脸ID |
| delete_all | boolean | 否 | 删除所有人脸 |

**响应示例**:
```json
{
  "code": 200,
  "message": "人脸删除成功",
  "data": {
    "deleted_count": 2,
    "deleted_face_ids": ["FACE_20251010_001", "FACE_20251010_002"]
  }
}
```

### 3.2 人脸查询

#### 3.2.1 获取用户人脸列表
**接口地址**: `GET /face/list/{user_id}`

**接口说明**: 获取指定用户的所有人脸信息

**请求方式**: GET

**路径参数**:
| 参数名 | 类型 | 说明 |
|--------|------|------|
| user_id | string | 用户ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "user_id": "STU2024001",
    "total": 2,
    "faces": [
      {
        "face_id": "FACE_20251010_001",
        "face_image_url": "https://storage.gcrf.com/faces/001.jpg",
        "quality_score": 0.95,
        "is_primary": true,
        "created_at": "2025-10-10T10:00:00Z"
      },
      {
        "face_id": "FACE_20251010_002",
        "face_image_url": "https://storage.gcrf.com/faces/002.jpg",
        "quality_score": 0.92,
        "is_primary": false,
        "created_at": "2025-10-10T10:30:00Z"
      }
    ]
  }
}
```

#### 3.2.2 获取人脸详情
**接口地址**: `GET /face/detail/{face_id}`

**接口说明**: 获取特定人脸的详细信息

**请求方式**: GET

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "face_id": "FACE_20251010_001",
    "user_id": "STU2024001",
    "face_image_url": "https://storage.gcrf.com/faces/001.jpg",
    "thumbnail_url": "https://storage.gcrf.com/faces/001_thumb.jpg",
    "quality_score": 0.95,
    "quality_details": {
      "blur": 0.1,
      "brightness": 0.8,
      "occlusion": 0.05,
      "pose": {
        "yaw": 5.2,
        "pitch": 3.1,
        "roll": 1.5
      }
    },
    "is_primary": true,
    "status": "active",
    "created_at": "2025-10-10T10:00:00Z",
    "updated_at": "2025-10-10T10:00:00Z"
  }
}
```

## 4. 头像管理接口

### 4.1 头像上传

#### 4.1.1 上传用户头像
**接口地址**: `POST /avatar/upload`

**接口说明**: 上传用户头像图片

**请求方式**: POST (multipart/form-data)

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| user_id | string | 是 | 用户ID |
| user_type | string | 是 | 用户类型 |
| avatar | file | 是 | 头像文件(最大2MB) |
| crop_params | object | 否 | 裁剪参数 |

**响应示例**:
```json
{
  "code": 200,
  "message": "头像上传成功",
  "data": {
    "avatar_id": "AVT_20251010_001",
    "avatar_url": "https://storage.gcrf.com/avatars/001.jpg",
    "thumbnail_url": "https://storage.gcrf.com/avatars/001_thumb.jpg",
    "file_size": 156789,
    "mime_type": "image/jpeg"
  }
}
```

#### 4.1.2 更新用户头像
**接口地址**: `PUT /avatar/{user_id}`

**请求方式**: PUT (multipart/form-data)

**响应示例**:
```json
{
  "code": 200,
  "message": "头像更新成功",
  "data": {
    "avatar_id": "AVT_20251010_002",
    "avatar_url": "https://storage.gcrf.com/avatars/002.jpg",
    "previous_avatar_id": "AVT_20251010_001"
  }
}
```

### 4.2 图书封面管理

#### 4.2.1 上传图书封面
**接口地址**: `POST /book/cover/upload`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| book_id | string | 是 | 图书ID |
| isbn | string | 否 | ISBN号 |
| cover | file | 是 | 封面图片 |

**响应示例**:
```json
{
  "code": 200,
  "message": "封面上传成功",
  "data": {
    "cover_id": "COV_20251010_001",
    "cover_url": "https://storage.gcrf.com/books/covers/001.jpg",
    "thumbnail_url": "https://storage.gcrf.com/books/covers/001_thumb.jpg"
  }
}
```

## 5. 统计分析接口

### 5.1 识别统计

#### 5.1.1 获取识别统计数据
**接口地址**: `GET /face/statistics`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| start_date | string | 是 | 开始日期 |
| end_date | string | 是 | 结束日期 |
| user_type | string | 否 | 用户类型筛选 |
| group_by | string | 否 | 分组方式(day/week/month) |

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total_recognitions": 10000,
    "success_count": 9500,
    "success_rate": 0.95,
    "average_response_time": 235,
    "unique_users": 1500,
    "daily_stats": [
      {
        "date": "2025-10-10",
        "recognition_count": 500,
        "success_rate": 0.96,
        "avg_response_time": 220
      }
    ],
    "user_type_distribution": {
      "student": 8000,
      "teacher": 1500,
      "admin": 500
    },
    "peak_hours": [
      {"hour": 8, "count": 800},
      {"hour": 12, "count": 750},
      {"hour": 18, "count": 650}
    ]
  }
}
```

#### 5.1.2 获取异常记录
**接口地址**: `GET /face/anomalies`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| start_date | string | 是 | 开始日期 |
| end_date | string | 是 | 结束日期 |
| anomaly_type | string | 否 | 异常类型 |
| page | integer | 否 | 页码 |
| size | integer | 否 | 每页大小 |

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 50,
    "page": 1,
    "size": 20,
    "anomalies": [
      {
        "anomaly_id": "ANO_20251010_001",
        "type": "multiple_failed_attempts",
        "user_id": "STU2024001",
        "description": "连续5次识别失败",
        "risk_level": "high",
        "occurred_at": "2025-10-10T09:30:00Z",
        "device_id": "DEVICE_001",
        "ip_address": "192.168.1.100"
      }
    ]
  }
}
```

## 6. 系统管理接口

### 6.1 配置管理

#### 6.1.1 获取系统配置
**接口地址**: `GET /face/config`

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "recognition_threshold": 0.80,
    "liveness_threshold": 0.95,
    "quality_threshold": 0.70,
    "max_face_per_user": 3,
    "face_expire_days": 365,
    "rate_limit": {
      "login_attempts": 10,
      "time_window": 300
    },
    "security": {
      "enable_liveness": true,
      "enable_multi_face": true,
      "enable_face_update": true
    }
  }
}
```

#### 6.1.2 更新系统配置
**接口地址**: `PUT /face/config`

**请求参数**:
```json
{
  "recognition_threshold": 0.85,
  "liveness_threshold": 0.96,
  "security": {
    "enable_liveness": true
  }
}
```

### 6.2 日志查询

#### 6.2.1 查询识别日志
**接口地址**: `GET /face/logs`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| user_id | string | 否 | 用户ID筛选 |
| start_time | string | 是 | 开始时间 |
| end_time | string | 是 | 结束时间 |
| result | string | 否 | 结果筛选(success/failed) |
| page | integer | 否 | 页码 |
| size | integer | 否 | 每页大小 |

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 1000,
    "page": 1,
    "size": 20,
    "logs": [
      {
        "log_id": "LOG_20251010_001",
        "user_id": "STU2024001",
        "user_name": "张三",
        "recognition_type": "login",
        "result": "success",
        "similarity_score": 0.95,
        "response_time": 235,
        "device_info": {
          "device_id": "DEVICE_001",
          "device_type": "kiosk",
          "ip_address": "192.168.1.100"
        },
        "created_at": "2025-10-10T09:00:00Z"
      }
    ]
  }
}
```

## 7. WebSocket实时接口

### 7.1 实时人脸识别
**接口地址**: `ws://api.library.gcrf.com/ws/face/realtime`

**连接参数**:
```javascript
const ws = new WebSocket('wss://api.library.gcrf.com/ws/face/realtime', {
  headers: {
    'Authorization': 'Bearer {token}'
  }
});
```

**消息格式**:
```json
// 客户端发送
{
  "action": "recognize",
  "data": {
    "image": "base64_encoded_image",
    "session_id": "SESSION_001"
  }
}

// 服务端响应
{
  "type": "recognition_result",
  "data": {
    "user_id": "STU2024001",
    "user_name": "张三",
    "similarity": 0.95,
    "timestamp": "2025-10-10T10:00:00Z"
  }
}
```

## 8. 批量操作接口

### 8.1 批量导入
**接口地址**: `POST /face/batch/import`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| import_file | file | 是 | ZIP压缩包(包含图片和元数据) |
| mode | string | 否 | 导入模式(create/update/upsert) |

**响应示例**:
```json
{
  "code": 200,
  "message": "批量导入任务已创建",
  "data": {
    "task_id": "TASK_20251010_001",
    "status": "processing",
    "total_count": 100,
    "check_status_url": "/api/v1/face/batch/status/TASK_20251010_001"
  }
}
```

### 8.2 批量导出
**接口地址**: `POST /face/batch/export`

**请求参数**:
```json
{
  "user_ids": ["STU2024001", "STU2024002"],
  "user_type": "student",
  "include_images": true,
  "format": "zip"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "导出任务已创建",
  "data": {
    "task_id": "TASK_20251010_002",
    "download_url": "https://storage.gcrf.com/exports/TASK_20251010_002.zip",
    "expires_at": "2025-10-11T10:00:00Z"
  }
}
```

## 9. SDK集成示例

### 9.1 Java SDK
```java
// Maven依赖
<dependency>
    <groupId>com.gcrf.library</groupId>
    <artifactId>face-recognition-sdk</artifactId>
    <version>1.0.0</version>
</dependency>

// 使用示例
import com.gcrf.library.face.FaceClient;
import com.gcrf.library.face.model.*;

public class FaceRecognitionExample {
    public static void main(String[] args) {
        // 初始化客户端
        FaceClient client = new FaceClient.Builder()
            .apiKey("your_api_key")
            .apiSecret("your_api_secret")
            .baseUrl("https://api.library.gcrf.com")
            .build();

        // 人脸注册
        RegisterRequest request = new RegisterRequest();
        request.setUserId("STU2024001");
        request.setUserType("student");
        request.setFaceImage(new File("/path/to/image.jpg"));

        RegisterResponse response = client.register(request);
        System.out.println("Face ID: " + response.getFaceId());

        // 人脸登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setFaceImage(new File("/path/to/face.jpg"));

        LoginResponse loginResponse = client.login(loginRequest);
        System.out.println("User: " + loginResponse.getUserName());
        System.out.println("Token: " + loginResponse.getAccessToken());
    }
}
```

### 9.2 Python SDK
```python
# pip install gcrf-face-recognition

from gcrf_face import FaceClient
from gcrf_face.models import RegisterRequest, LoginRequest

# 初始化客户端
client = FaceClient(
    api_key='your_api_key',
    api_secret='your_api_secret',
    base_url='https://api.library.gcrf.com'
)

# 人脸注册
with open('/path/to/image.jpg', 'rb') as f:
    response = client.register(
        user_id='STU2024001',
        user_type='student',
        face_image=f
    )
    print(f"Face ID: {response['face_id']}")

# 人脸登录
with open('/path/to/face.jpg', 'rb') as f:
    response = client.login(face_image=f)
    print(f"User: {response['user_name']}")
    print(f"Token: {response['access_token']}")

# 批量操作
faces = [
    {'user_id': 'STU2024001', 'image_path': '/path/to/image1.jpg'},
    {'user_id': 'STU2024002', 'image_path': '/path/to/image2.jpg'}
]
results = client.batch_register(faces)
```

### 9.3 JavaScript SDK
```javascript
// npm install @gcrf/face-recognition

import { FaceClient } from '@gcrf/face-recognition';

// 初始化客户端
const client = new FaceClient({
  apiKey: 'your_api_key',
  apiSecret: 'your_api_secret',
  baseUrl: 'https://api.library.gcrf.com'
});

// 人脸注册
async function registerFace() {
  const formData = new FormData();
  formData.append('user_id', 'STU2024001');
  formData.append('user_type', 'student');
  formData.append('face_image', fileInput.files[0]);

  try {
    const response = await client.register(formData);
    console.log('Face ID:', response.face_id);
  } catch (error) {
    console.error('Registration failed:', error);
  }
}

// 人脸登录
async function loginWithFace() {
  const formData = new FormData();
  formData.append('face_image', fileInput.files[0]);

  try {
    const response = await client.login(formData);
    console.log('User:', response.user_name);
    localStorage.setItem('token', response.access_token);
  } catch (error) {
    console.error('Login failed:', error);
  }
}

// WebSocket实时识别
const ws = client.createRealtimeConnection();

ws.on('open', () => {
  console.log('Connected to realtime recognition');
});

ws.on('recognition', (data) => {
  console.log('Recognized user:', data.user_name);
});

ws.recognize(imageBlob);
```

## 10. 错误处理

### 10.1 业务错误码
| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| 40001 | 人脸图片质量不合格 | 提示用户重新拍摄 |
| 40002 | 未检测到人脸 | 确保人脸在画面中 |
| 40003 | 检测到多个人脸 | 确保画面中只有一个人 |
| 40004 | 活体检测失败 | 提示用户配合完成动作 |
| 40101 | 人脸未注册 | 引导用户先注册人脸 |
| 40102 | 人脸匹配失败 | 重新尝试或使用其他登录方式 |
| 40301 | 账户已锁定 | 联系管理员解锁 |
| 40302 | 权限不足 | 检查用户权限配置 |
| 40901 | 人脸已存在 | 使用更新接口替换 |
| 42901 | 请求过于频繁 | 稍后重试 |
| 50001 | 人脸识别服务异常 | 使用备用登录方式 |
| 50301 | 服务暂时不可用 | 稍后重试 |

### 10.2 错误响应格式
```json
{
  "code": 40001,
  "message": "人脸图片质量不合格",
  "data": {
    "error_code": "FACE_QUALITY_LOW",
    "error_details": {
      "quality_score": 0.45,
      "issues": ["光线过暗", "模糊度过高"],
      "suggestion": "请在光线充足的环境下重新拍摄"
    }
  },
  "timestamp": 1696915200000,
  "request_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

## 11. 性能指标

### 11.1 SLA承诺
| 指标 | 目标值 | 说明 |
|------|--------|------|
| API可用性 | ≥ 99.9% | 月度统计 |
| 识别准确率 | ≥ 99.5% | FAR < 0.001% |
| 平均响应时间 | < 300ms | P50延迟 |
| P95响应时间 | < 500ms | 95分位延迟 |
| P99响应时间 | < 1000ms | 99分位延迟 |
| 并发支持 | 1000 QPS | 单节点 |
| 活体检测准确率 | ≥ 99% | 防攻击成功率 |

### 11.2 限流策略
| 接口类型 | 限流配置 | 说明 |
|----------|----------|------|
| 人脸注册 | 10次/分钟/用户 | 防止恶意注册 |
| 人脸登录 | 20次/分钟/IP | 防暴力破解 |
| 人脸搜索 | 100次/分钟/Token | 资源保护 |
| 批量操作 | 5次/小时/账户 | 防止滥用 |
| 实时识别 | 30fps/连接 | 流量控制 |

## 12. 安全规范

### 12.1 数据传输安全
- 所有API必须使用HTTPS协议
- 敏感数据字段需要额外加密
- 支持请求签名验证(HMAC-SHA256)

### 12.2 数据存储安全
- 人脸特征向量AES-256加密存储
- 原始图片添加水印保护
- 定期清理过期数据

### 12.3 访问控制
- API Key + Secret认证
- JWT Token有效期2小时
- IP白名单(可选)
- 设备绑定(可选)

### 12.4 隐私保护
- 遵循GDPR/个人信息保护法
- 用户可申请删除个人数据
- 数据最小化原则
- 明确告知和授权

## 13. 版本管理

### 13.1 版本策略
- 版本格式: v{major}.{minor}.{patch}
- URL版本: /api/v1/...
- 向后兼容保证期: 12个月
- 废弃通知期: 6个月

### 13.2 版本历史
| 版本 | 发布日期 | 主要变更 |
|------|----------|----------|
| v1.0.0 | 2025-10-10 | 初始版本发布 |
| v1.1.0 | 计划中 | 添加3D活体检测 |
| v1.2.0 | 计划中 | 支持口罩识别 |

## 14. 联系支持

### 技术支持
- 邮箱: tech-support@gcrf.com
- 电话: 400-xxx-xxxx
- 工单系统: https://support.gcrf.com

### 开发者资源
- API文档: https://api.library.gcrf.com/docs
- SDK下载: https://github.com/gcrf/face-sdk
- 示例代码: https://github.com/gcrf/face-examples
- 开发者论坛: https://forum.gcrf.com

### 问题反馈
- GitHub Issues: https://github.com/gcrf/face-api/issues
- 产品建议: product@gcrf.com