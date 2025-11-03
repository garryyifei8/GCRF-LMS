# 国创睿峰智能图书馆管理系统 - 后端架构设计方案

**版本**: v1.0
**设计日期**: 2025-10-10
**设计单位**: 国创睿峰科技有限公司
**文档状态**: 正式版

---

## 文档概述

本文档详细描述了"国创睿峰智能图书馆管理系统"的后端架构设计方案，重点关注**前端轻量化、业务逻辑后置、高扩展性和易维护性**的架构设计原则。

## 目录

1. [整体架构设计](#一整体架构设计)
2. [微服务拆分方案](#二微服务拆分方案)
3. [API网关设计](#三api网关设计)
4. [数据存储方案](#四数据存储方案)
5. [服务间通信设计](#五服务间通信设计)
6. [AI服务集成架构](#六ai服务集成架构)
7. [可扩展性设计](#七可扩展性设计)
8. [部署架构](#八部署架构)
9. [技术选型建议](#九技术选型建议)
10. [架构演进路线](#十架构演进路线)
11. [关键技术决策与最佳实践](#十一关键技术决策与最佳实践)
12. [总结与建议](#十二总结与建议)

---

## 一、整体架构设计

### 1.1 架构模式

采用**微服务架构 + 领域驱动设计(DDD)**，确保服务边界清晰、职责单一、易于扩展。

#### 架构分层图

```
┌─────────────────────────────────────────────────────────┐
│                  前端层 - Presentation Layer             │
├─────────────────────────────────────────────────────────┤
│  Web管理端        微信小程序       自助借还机终端         │
│  (Vue.js 3)      (uni-app)        (React)              │
└─────────────────────────────────────────────────────────┘
                        ↕ HTTPS/WebSocket
┌─────────────────────────────────────────────────────────┐
│                  网关层 - Gateway Layer                  │
├─────────────────────────────────────────────────────────┤
│          API Gateway (Spring Cloud Gateway)             │
│     负载均衡 | 路由转发 | 认证鉴权 | 限流熔断             │
└─────────────────────────────────────────────────────────┘
                        ↕
┌─────────────────────────────────────────────────────────┐
│                应用服务层 - Service Layer                │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  【业务微服务】                                          │
│  - auth-service        (认证授权服务)                    │
│  - book-service        (图书服务)                        │
│  - circulation-service (流通服务)                        │
│  - reader-service      (读者服务)                        │
│  - system-service      (系统服务)                        │
│                                                         │
│  【AI智能微服务】                                        │
│  - recommend-service   (推荐服务)                        │
│  - nlp-service         (NLP服务)                         │
│  - vision-service      (视觉识别服务)                    │
│  - analytics-service   (数据分析服务)                    │
│                                                         │
│  【基础服务】                                            │
│  - notification-service (通知服务)                       │
│  - file-service         (文件服务)                       │
│  - search-service       (搜索服务)                       │
│                                                         │
└─────────────────────────────────────────────────────────┘
                        ↕
┌─────────────────────────────────────────────────────────┐
│                   数据层 - Data Layer                    │
├─────────────────────────────────────────────────────────┤
│  MySQL        Redis      MongoDB      Elasticsearch     │
│ (业务数据)    (缓存)    (日志/埋点)    (全文检索)         │
└─────────────────────────────────────────────────────────┘
                        ↕
┌─────────────────────────────────────────────────────────┐
│                 AI能力层 - AI Engine Layer               │
├─────────────────────────────────────────────────────────┤
│  推荐引擎     NLP引擎      计算机视觉     语音引擎        │
│ (TensorFlow) (HuggingFace) (OpenCV)    (讯飞/百度)       │
└─────────────────────────────────────────────────────────┘
```

### 1.2 分层架构

采用经典的**四层架构**，职责清晰：

| 层级 | 职责 | 技术实现 |
|------|------|----------|
| **接入层** | 协议适配、路由、鉴权、限流 | Spring Cloud Gateway, Sentinel |
| **服务层** | 业务逻辑、服务编排、数据聚合 | Spring Boot, Spring Cloud Alibaba |
| **数据层** | 数据持久化、缓存、检索 | PostgreSQL, Redis, ES, MongoDB |
| **能力层** | AI算法、第三方服务集成 | Python FastAPI, Java调用 |

---

## 二、微服务拆分方案

### 2.1 服务拆分原则

基于**领域驱动设计(DDD)**划分服务边界：

1. **单一职责**：每个服务聚焦一个业务领域
2. **高内聚低耦合**：服务内部高内聚，服务间松耦合
3. **独立部署**：每个服务可独立部署和扩展
4. **数据自治**：每个服务管理自己的数据库

### 2.2 核心微服务清单

| 服务名称 | 端口 | 职责边界 | 数据库 | 关键API |
|---------|------|---------|--------|---------|
| **auth-service** | 8081 | 用户认证、授权、JWT签发、权限管理 | PostgreSQL(user) | `/api/auth/login`<br/>`/api/auth/token/refresh`<br/>`/api/auth/permissions` |
| **book-service** | 8082 | 图书编目、典藏、剔旧、盘点、条码管理 | PostgreSQL(book) | `/api/books`<br/>`/api/books/{id}`<br/>`/api/books/catalog`<br/>`/api/books/inventory` |
| **circulation-service** | 8083 | 借阅、归还、预约、续借、逾期处理 | PostgreSQL(circulation) | `/api/circulation/borrow`<br/>`/api/circulation/return`<br/>`/api/circulation/reserve` |
| **reader-service** | 8084 | 读者管理、办证、人脸采集、读者画像 | PostgreSQL(reader) | `/api/readers`<br/>`/api/readers/{id}`<br/>`/api/readers/card`<br/>`/api/readers/face` |
| **system-service** | 8085 | 系统配置、参数管理、数据备份、用户管理 | PostgreSQL(system) | `/api/system/config`<br/>`/api/system/users`<br/>`/api/system/backup` |
| **recommend-service** | 8086 | AI推荐引擎、协同过滤、深度学习推荐 | Redis, MongoDB | `/api/recommend/personal`<br/>`/api/recommend/similar`<br/>`/api/recommend/hot` |
| **nlp-service** | 8087 | 智能问答、意图识别、实体抽取、对话管理 | ES, MongoDB | `/api/nlp/qa`<br/>`/api/nlp/intent`<br/>`/api/nlp/voice` |
| **vision-service** | 8088 | 人脸识别、OCR识别、条码识别、封面识别 | Redis(缓存) | `/api/vision/face/detect`<br/>`/api/vision/ocr`<br/>`/api/vision/barcode` |
| **analytics-service** | 8089 | 数据统计、报表生成、趋势预测、决策支持 | MongoDB, ES | `/api/analytics/dashboard`<br/>`/api/analytics/report`<br/>`/api/analytics/predict` |
| **notification-service** | 8090 | 消息通知、到期提醒、微信推送、短信发送 | Redis, PostgreSQL | `/api/notification/send`<br/>`/api/notification/wechat`<br/>`/api/notification/sms` |
| **file-service** | 8091 | 文件上传、图片处理、OSS存储 | MinIO/OSS | `/api/file/upload`<br/>`/api/file/download` |
| **search-service** | 8092 | 全文检索、语义搜索、图书检索 | Elasticsearch | `/api/search/books`<br/>`/api/search/suggest` |

### 2.3 服务间依赖关系

```
流通服务 (circulation-service)
  ├── 依赖 → 图书服务 (book-service)
  ├── 依赖 → 读者服务 (reader-service)
  └── 依赖 → 通知服务 (notification-service)

推荐服务 (recommend-service)
  ├── 依赖 → 图书服务 (book-service)
  ├── 依赖 → 读者服务 (reader-service)
  └── 依赖 → 流通服务 (circulation-service)

NLP服务 (nlp-service)
  ├── 依赖 → 搜索服务 (search-service)
  └── 依赖 → 图书服务 (book-service)

数据分析服务 (analytics-service)
  ├── 依赖 → 流通服务 (circulation-service)
  ├── 依赖 → 图书服务 (book-service)
  └── 依赖 → 读者服务 (reader-service)

所有服务 → 认证服务 (auth-service)
```

---

## 三、API网关设计

### 3.1 网关职责

使用**Spring Cloud Gateway**作为统一入口，承担以下职责：

| 功能模块 | 实现方案 | 说明 |
|---------|---------|------|
| **路由转发** | 基于Path、Header、参数路由 | 统一路由规则，支持动态路由 |
| **认证鉴权** | JWT Token验证 | 所有请求验证Token有效性 |
| **权限控制** | RBAC角色权限验证 | 基于角色和资源的细粒度权限 |
| **限流降级** | Sentinel流控规则 | QPS限流、熔断降级、系统保护 |
| **协议转换** | HTTP转gRPC/WebSocket | 支持多协议适配 |
| **日志追踪** | SkyWalking链路追踪 | 分布式链路追踪和性能监控 |
| **参数校验** | 全局参数校验 | 统一参数验证、签名验证 |
| **灰度发布** | 基于权重的灰度路由 | 支持A/B测试、金丝雀发布 |

### 3.2 网关路由配置示例

```yaml
# application.yml
spring:
  cloud:
    gateway:
      routes:
        # 认证服务路由
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/auth/**
          filters:
            - StripPrefix=1

        # 图书服务路由（需要鉴权）
        - id: book-service
          uri: lb://book-service
          predicates:
            - Path=/api/books/**
          filters:
            - StripPrefix=1
            - AuthFilter  # 自定义鉴权过滤器

        # 流通服务路由（需要鉴权）
        - id: circulation-service
          uri: lb://circulation-service
          predicates:
            - Path=/api/circulation/**
          filters:
            - StripPrefix=1
            - AuthFilter
            - RateLimitFilter  # 限流

        # AI推荐服务路由
        - id: recommend-service
          uri: lb://recommend-service
          predicates:
            - Path=/api/recommend/**
          filters:
            - StripPrefix=1
            - CacheFilter  # 缓存

      # 全局过滤器
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Origin
        - AddRequestHeader=X-Request-Source, Gateway
```

### 3.3 认证鉴权流程

```
客户端 → API网关 → 认证服务 → Redis缓存 → 业务服务

流程说明：
1. 客户端请求API (携带JWT Token)
2. 网关提取Token并检查Redis黑名单
3. 如果Token有效，解析获取用户信息
4. 验证权限(RBAC)
5. 转发请求到业务服务(添加用户信息)
6. 返回响应

异常处理：
- Token无效 → 401 Unauthorized
- 权限不足 → 403 Forbidden
```

### 3.4 限流策略

使用**Sentinel**实现多维度限流：

| 限流维度 | 限流规则 | QPS阈值 | 降级策略 |
|---------|---------|---------|---------|
| **全局限流** | 总体QPS | 10000 | 返回503错误 |
| **服务限流** | 按服务限流 | 2000/服务 | 快速失败 |
| **接口限流** | 按接口限流 | 100-500/接口 | 排队等待 |
| **用户限流** | 按用户ID限流 | 50/秒/用户 | 返回429错误 |
| **IP限流** | 按IP限流 | 100/秒/IP | 黑名单 |

---

## 四、数据存储方案

### 4.1 数据库选型与分配

| 数据类型 | 存储系统 | 用途 | 数据量级 | 访问特征 |
|---------|---------|------|---------|---------|
| **业务数据** | PostgreSQL 15+ | 图书、读者、流通、系统配置 | 百万级 | 读写均衡、事务性强 |
| **缓存数据** | Redis 6.0 | 热点数据、会话、分布式锁 | 十万级 | 高频读、低延迟 |
| **搜索数据** | Elasticsearch 7.x | 全文检索、图书搜索、日志分析 | 百万级 | 读多写少、复杂查询 |
| **行为数据** | MongoDB | 用户行为、推荐日志、埋点数据 | 千万级 | 写多读少、灵活结构 |
| **文件数据** | MinIO/OSS | 图书封面、读者照片、文件 | TB级 | 读多写少、大文件 |

### 4.2 PostgreSQL数据库设计

#### 4.2.1 数据库拆分策略

采用**单库多schema**（中小型）或**多库**（大型）策略：

```
library_system (单库模式)
├── book_db          # 图书schema
│   ├── t_book       # 图书表
│   ├── t_book_copy  # 图书副本表
│   ├── t_category   # 分类表
│   └── t_inventory  # 盘点表
│
├── reader_db        # 读者schema
│   ├── t_reader     # 读者表
│   ├── t_reader_card # 读者证表
│   └── t_face_feature # 人脸特征表
│
├── circulation_db   # 流通schema
│   ├── t_circulation # 流通记录表
│   ├── t_reservation # 预约表
│   └── t_fine        # 罚款表
│
└── system_db        # 系统schema
    ├── t_user        # 用户表
    ├── t_role        # 角色表
    ├── t_permission  # 权限表
    └── t_config      # 配置表
```

#### 4.2.2 核心表设计示例

```sql
-- 图书表 (PostgreSQL优化版)
CREATE TABLE t_book (
    book_id BIGSERIAL PRIMARY KEY,
    isbn VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(100),
    publisher VARCHAR(100),
    category VARCHAR(50),
    cover_url VARCHAR(255),
    summary TEXT,
    keywords VARCHAR(255),
    tags JSONB,  -- PostgreSQL原生JSONB类型
    total_copies INT DEFAULT 1,
    available_copies INT DEFAULT 1,
    borrow_count INT DEFAULT 0,
    status SMALLINT DEFAULT 0,  -- 0-正常 1-停用
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_book_isbn ON t_book(isbn);
CREATE INDEX idx_book_category ON t_book(category);
CREATE INDEX idx_book_title ON t_book(title);
CREATE INDEX idx_book_tags ON t_book USING GIN(tags);  -- JSONB的GIN索引
CREATE INDEX idx_book_title_author ON t_book USING GIN(to_tsvector('chinese', title || ' ' || COALESCE(author, '')));  -- 全文检索索引

-- 添加注释
COMMENT ON TABLE t_book IS '图书表';
COMMENT ON COLUMN t_book.book_id IS '图书ID';
COMMENT ON COLUMN t_book.isbn IS 'ISBN号';
COMMENT ON COLUMN t_book.tags IS 'AI生成标签(JSONB)';

-- 流通记录表 (分区表优化)
CREATE TABLE t_circulation (
    circulation_id BIGSERIAL,
    book_id BIGINT NOT NULL,
    reader_id BIGINT NOT NULL,
    barcode VARCHAR(50) NOT NULL,
    borrow_date TIMESTAMP NOT NULL,
    due_date TIMESTAMP NOT NULL,
    return_date TIMESTAMP,
    status SMALLINT DEFAULT 0,  -- 0-借出 1-归还 2-逾期
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (circulation_id, borrow_date)
) PARTITION BY RANGE (borrow_date);

-- 创建分区
CREATE TABLE t_circulation_2023 PARTITION OF t_circulation
    FOR VALUES FROM ('2023-01-01') TO ('2024-01-01');

CREATE TABLE t_circulation_2024 PARTITION OF t_circulation
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');

CREATE TABLE t_circulation_2025 PARTITION OF t_circulation
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');

CREATE TABLE t_circulation_default PARTITION OF t_circulation
    DEFAULT;

-- 创建索引
CREATE INDEX idx_circulation_book ON t_circulation(book_id);
CREATE INDEX idx_circulation_reader ON t_circulation(reader_id);
CREATE INDEX idx_circulation_barcode ON t_circulation(barcode);
CREATE INDEX idx_circulation_status_borrow ON t_circulation(status, borrow_date);

-- 添加注释
COMMENT ON TABLE t_circulation IS '流通记录表(按年分区)';
```

### 4.3 Redis缓存策略

#### 4.3.1 缓存分类

| 缓存类型 | Key设计 | TTL | 更新策略 | 使用场景 |
|---------|--------|-----|---------|---------|
| **热点图书** | `book:hot:{bookId}` | 1小时 | 主动更新 | 图书详情高频访问 |
| **读者信息** | `reader:{readerId}` | 30分钟 | 被动失效 | 读者信息缓存 |
| **推荐列表** | `recommend:{readerId}:{scene}` | 10分钟 | 定时刷新 | 个性化推荐 |
| **搜索结果** | `search:{query}:page:{n}` | 5分钟 | LRU淘汰 | 搜索结果缓存 |
| **JWT Token** | `token:{userId}` | 2小时 | 主动删除 | Token有效性验证 |
| **限流计数** | `rate:{userId}:{api}` | 1秒-1分钟 | 过期自动删除 | 接口限流 |
| **分布式锁** | `lock:{resource}` | 10秒 | 自动释放 | 并发控制 |

#### 4.3.2 缓存更新模式

**Cache-Aside模式示例：**

```java
// 读取
public Book getBookById(Long bookId) {
    String key = "book:hot:" + bookId;

    // 1. 先查缓存
    Book book = redisTemplate.opsForValue().get(key);
    if (book != null) {
        return book;
    }

    // 2. 缓存未命中，查数据库
    book = bookMapper.selectById(bookId);
    if (book != null) {
        // 3. 写入缓存
        redisTemplate.opsForValue().set(key, book, 1, TimeUnit.HOURS);
    }

    return book;
}

// 更新
public void updateBook(Book book) {
    // 1. 更新数据库
    bookMapper.updateById(book);

    // 2. 更新缓存
    String key = "book:hot:" + book.getBookId();
    redisTemplate.opsForValue().set(key, book, 1, TimeUnit.HOURS);

    // 3. 发送MQ消息通知其他服务
    rabbitTemplate.convertAndSend("book.update", book.getBookId());
}
```

### 4.4 Elasticsearch检索设计

#### 4.4.1 索引结构

```json
{
  "mappings": {
    "properties": {
      "book_id": { "type": "long" },
      "isbn": { "type": "keyword" },
      "title": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "author": {
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "category": { "type": "keyword" },
      "keywords": {
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "tags": { "type": "keyword" },
      "summary": {
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "borrow_count": { "type": "integer" },
      "available": { "type": "boolean" },
      "created_at": { "type": "date" }
    }
  }
}
```

### 4.5 MongoDB行为数据设计

```javascript
// 用户行为集合
db.user_behavior.insertOne({
  _id: ObjectId(),
  reader_id: 1001,
  book_id: 5001,
  behavior_type: "view",  // view/search/collect/borrow/rate
  duration: 120,  // 停留时长(秒)
  device: "mobile",  // web/mobile/kiosk
  timestamp: ISODate("2025-10-10T10:30:00Z"),
  metadata: {
    search_query: "人工智能",
    from_recommend: true,
    recommend_algorithm: "collaborative_filtering"
  }
});

// 索引优化
db.user_behavior.createIndex({ reader_id: 1, timestamp: -1 });
db.user_behavior.createIndex({ book_id: 1, behavior_type: 1 });
db.user_behavior.createIndex({ timestamp: 1 }, { expireAfterSeconds: 7776000 });  // 90天TTL
```

---

## 五、服务间通信设计

### 5.1 通信方式选型

| 通信场景 | 通信方式 | 技术选型 | 使用场景 |
|---------|---------|---------|---------|
| **同步调用** | RESTful API | OpenFeign | 实时查询、CRUD操作 |
| **同步调用** | gRPC | grpc-spring-boot-starter | 服务间高性能调用 |
| **异步消息** | 消息队列 | RabbitMQ | 事件通知、任务解耦 |
| **事件流** | 事件流 | Kafka | 日志采集、数据同步 |
| **实时通信** | WebSocket | Spring WebSocket | 实时推送、通知 |

### 5.2 同步调用：OpenFeign

```java
// 图书服务Feign客户端
@FeignClient(name = "book-service", fallback = BookServiceFallback.class)
public interface BookServiceClient {

    @GetMapping("/books/{id}")
    Book getBookById(@PathVariable("id") Long id);

    @PostMapping("/books/{id}/reduce-stock")
    void reduceStock(@PathVariable("id") Long id, @RequestParam Integer count);
}

// 熔断降级
@Component
public class BookServiceFallback implements BookServiceClient {

    @Override
    public Book getBookById(Long id) {
        // 降级逻辑：返回缓存数据或默认值
        return Book.builder()
            .bookId(id)
            .title("服务暂时不可用")
            .build();
    }

    @Override
    public void reduceStock(Long id, Integer count) {
        // 降级逻辑：记录失败，后续补偿
        log.error("减库存失败，bookId={}, count={}", id, count);
    }
}
```

### 5.3 异步消息：RabbitMQ

#### 5.3.1 Exchange和Queue设计

```
Exchange拓扑:
┌─────────────────────────────────────────┐
│         book.topic.exchange              │
│         (Topic Exchange)                 │
└─────────────────────────────────────────┘
    │
    ├── book.created  → book.created.queue
    ├── book.updated  → book.updated.queue
    ├── book.deleted  → book.deleted.queue
    │
    └── book.#        → book.sync.queue (同步到ES/缓存)

┌─────────────────────────────────────────┐
│      circulation.direct.exchange         │
│         (Direct Exchange)                │
└─────────────────────────────────────────┘
    │
    ├── borrow  → circulation.borrow.queue
    ├── return  → circulation.return.queue
    └── overdue → circulation.overdue.queue

┌─────────────────────────────────────────┐
│      notification.fanout.exchange        │
│         (Fanout Exchange)                │
└─────────────────────────────────────────┘
    │
    ├── notification.wechat.queue
    ├── notification.sms.queue
    └── notification.email.queue
```

#### 5.3.2 消息生产者

```java
@Service
public class BookEventPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 发布图书创建事件
    public void publishBookCreated(Book book) {
        BookCreatedEvent event = BookCreatedEvent.builder()
            .bookId(book.getBookId())
            .isbn(book.getIsbn())
            .title(book.getTitle())
            .timestamp(System.currentTimeMillis())
            .build();

        rabbitTemplate.convertAndSend(
            "book.topic.exchange",
            "book.created",
            event
        );
    }
}
```

#### 5.3.3 消息消费者

```java
@Component
public class BookEventConsumer {

    @Autowired
    private ElasticsearchService esService;

    // 同步到ES
    @RabbitListener(queues = "book.sync.queue")
    public void syncToES(BookCreatedEvent event) {
        try {
            Book book = bookMapper.selectById(event.getBookId());
            esService.indexBook(book);
            log.info("图书同步到ES成功: {}", event.getBookId());
        } catch (Exception e) {
            log.error("图书同步到ES失败", e);
            throw new AmqpRejectAndDontRequeueException("同步失败", e);
        }
    }
}
```

### 5.4 事件驱动架构流程

```
用户 → 流通服务 → 消息队列 → [图书服务、通知服务、推荐服务、分析服务]

流程说明：
1. 用户归还图书
2. 流通服务更新借阅记录
3. 发布归还事件到消息队列
4. 返回成功响应给用户
5. 图书服务消费事件：更新库存
6. 通知服务消费事件：发送微信通知
7. 推荐服务消费事件：更新推荐模型
8. 分析服务消费事件：更新统计数据
```

---

## 六、AI服务集成架构

### 6.1 AI服务解耦设计

**核心原则**：AI能力作为独立服务，通过标准API提供能力，与业务系统松耦合。

```
业务服务层
├── 图书服务 (book-service)
├── 流通服务 (circulation-service)
└── 读者服务 (reader-service)
    ↓ HTTP/gRPC
AI能力层 (Python FastAPI)
├── 推荐API (recommend-api)
│   ├── 协同过滤模型
│   └── 深度学习模型
├── NLP API (nlp-api)
│   └── BERT模型
└── 视觉API (vision-api)
    ├── 人脸识别模型
    └── OCR模型
    ↓
数据层
├── 特征存储 (Redis)
├── 模型存储 (MinIO)
└── 训练日志 (MongoDB)
```

### 6.2 推荐服务架构

#### 6.2.1 推荐服务API设计 (Python FastAPI)

```python
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List

app = FastAPI(title="Recommendation Service")

class RecommendRequest(BaseModel):
    reader_id: int
    scene: str  # home/detail/search
    limit: int = 20

class RecommendResponse(BaseModel):
    book_ids: List[int]
    scores: List[float]
    algorithm: str

@app.post("/recommend/personal", response_model=RecommendResponse)
async def get_personal_recommendation(request: RecommendRequest):
    """个性化推荐"""
    try:
        # 多路召回
        cf_candidates = collaborative_filtering_recall(request.reader_id, 100)
        content_candidates = content_based_recall(request.reader_id, 100)
        hot_candidates = hot_recall(50)

        # 特征工程
        features = build_features(request.reader_id, candidates)

        # 模型预测
        scores = wide_deep_model.predict(features)

        # 排序过滤
        top_books = rank_and_filter(scores, request.limit)

        return RecommendResponse(
            book_ids=[b['book_id'] for b in top_books],
            scores=[b['score'] for b in top_books],
            algorithm="wide_deep"
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
```

#### 6.2.2 Java调用AI服务

```java
@Service
public class RecommendService {

    @Autowired
    private RestTemplate restTemplate;

    private static final String RECOMMEND_API_URL = "http://recommend-ai-service:8000";

    public List<Book> getPersonalRecommendation(Long readerId, String scene) {
        try {
            // 构建请求
            RecommendRequest request = RecommendRequest.builder()
                .readerId(readerId)
                .scene(scene)
                .limit(20)
                .build();

            // 调用AI服务
            ResponseEntity<RecommendResponse> response = restTemplate.postForEntity(
                RECOMMEND_API_URL + "/recommend/personal",
                request,
                RecommendResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                List<Long> bookIds = response.getBody().getBookIds();
                return bookService.getBatchBooks(bookIds);
            }

            // 降级：返回热门推荐
            return getHotBooks(20);

        } catch (Exception e) {
            log.error("推荐服务调用失败", e);
            return getHotBooks(20);
        }
    }
}
```

### 6.3 NLP服务架构

```python
@app.post("/nlp/qa")
async def question_answering(question: str, reader_id: int = None):
    """智能问答"""
    # 1. 意图识别
    intent = intent_classifier.predict(question)

    # 2. 实体抽取
    entities = ner_model.extract(question)

    # 3. 根据意图路由
    if intent == "book_query":
        books = search_books(entities)
        answer = generate_book_answer(books)
    elif intent == "rule_query":
        answer = query_rules(entities)
    elif intent == "recommend":
        books = call_recommend_service(reader_id)
        answer = generate_recommend_answer(books)
    else:
        answer = search_faq(question)

    return {"answer": answer, "intent": intent, "entities": entities}
```

### 6.4 视觉识别服务

```python
@app.post("/vision/face/detect")
async def detect_face(image: UploadFile):
    """人脸检测和特征提取"""
    image_bytes = await image.read()

    # 人脸检测
    faces = face_detector.detect(image_bytes)

    if len(faces) == 0:
        raise HTTPException(status_code=400, detail="未检测到人脸")

    # 特征提取
    feature = face_encoder.encode(faces[0])

    return {
        "face_count": len(faces),
        "feature_vector": feature.tolist(),
        "quality_score": calculate_quality(faces[0])
    }

@app.post("/vision/ocr")
async def ocr_recognition(image: UploadFile, type: str = "isbn"):
    """OCR识别"""
    image_bytes = await image.read()

    if type == "isbn":
        result = isbn_recognizer.recognize(image_bytes)
    elif type == "barcode":
        result = barcode_recognizer.recognize(image_bytes)
    else:
        result = ocr_engine.recognize(image_bytes)

    return {"text": result, "confidence": 0.95}
```

---

## 七、可扩展性设计

### 7.1 插件化架构

采用**SPI（Service Provider Interface）机制**实现插件化扩展：

```java
// 1. 定义插件接口
public interface RecommendStrategy {
    String getName();
    List<Long> recommend(Long readerId, int limit);
}

// 2. 实现插件
@Component
public class CollaborativeFilteringStrategy implements RecommendStrategy {
    @Override
    public String getName() {
        return "collaborative_filtering";
    }

    @Override
    public List<Long> recommend(Long readerId, int limit) {
        return cfRecommend(readerId, limit);
    }
}

@Component
public class ContentBasedStrategy implements RecommendStrategy {
    @Override
    public String getName() {
        return "content_based";
    }

    @Override
    public List<Long> recommend(Long readerId, int limit) {
        return contentRecommend(readerId, limit);
    }
}

// 3. 插件管理器
@Service
public class RecommendStrategyManager {

    @Autowired
    private List<RecommendStrategy> strategies;

    public List<Long> recommend(String strategyName, Long readerId, int limit) {
        RecommendStrategy strategy = strategies.stream()
            .filter(s -> s.getName().equals(strategyName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("未找到推荐策略"));

        return strategy.recommend(readerId, limit);
    }
}
```

### 7.2 配置化设计

```yaml
# application.yml - 业务规则配置化
library:
  rules:
    # 借阅规则
    borrow:
      student:
        max_books: 5
        borrow_days: 30
        max_renew: 2
      teacher:
        max_books: 10
        borrow_days: 60
        max_renew: 3

    # 逾期规则
    overdue:
      grace_days: 3
      fine_per_day: 0.1
      max_fine: 50.0

  # 推荐策略配置
  recommend:
    strategies:
      - name: collaborative_filtering
        weight: 0.4
      - name: content_based
        weight: 0.3
      - name: hot_recall
        weight: 0.3
    update_frequency: "0 0 2 * * ?"  # 每天凌晨2点更新
```

```java
// 配置类
@Data
@Configuration
@ConfigurationProperties(prefix = "library.rules")
public class LibraryRulesConfig {
    private BorrowRules borrow;
    private OverdueRules overdue;
}

// 使用配置
@Service
public class CirculationService {

    @Autowired
    private LibraryRulesConfig rulesConfig;

    public BorrowResult borrow(Long readerId, Long bookId) {
        Reader reader = readerService.getById(readerId);

        // 动态获取借阅规则
        ReaderRule rule = reader.getType() == ReaderType.STUDENT
            ? rulesConfig.getBorrow().getStudent()
            : rulesConfig.getBorrow().getTeacher();

        // 检查借阅限制
        if (reader.getCurrentBorrowCount() >= rule.getMaxBooks()) {
            throw new BorrowException("已达借阅上限: " + rule.getMaxBooks());
        }

        // 计算应还日期
        LocalDateTime dueDate = LocalDateTime.now().plusDays(rule.getBorrowDays());

        // 执行借阅逻辑...
    }
}
```

---

## 八、部署架构

### 8.1 单机部署方案（中小型图书馆）

**适用场景**：日均访问量 < 10000，馆藏 < 10万册

```
┌─────────────────────────────────────────────────┐
│         服务器 (16GB RAM, 8核CPU, 500GB SSD)    │
├─────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────┐   │
│  │ Nginx (80/443)                          │   │
│  │ - 静态资源托管                           │   │
│  │ - 反向代理                               │   │
│  │ - SSL终止                                │   │
│  └─────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────┐   │
│  │ Spring Boot应用 (8080-8092)             │   │
│  │ - 所有微服务打包成单个JAR               │   │
│  │ - 或Docker Compose部署                  │   │
│  └─────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────┐   │
│  │ MySQL 8.0 (3306)                        │   │
│  │ - InnoDB存储引擎                        │   │
│  │ - 每日备份                               │   │
│  └─────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────┐   │
│  │ Redis 6.0 (6379)                        │   │
│  │ - AOF持久化                              │   │
│  └─────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────┐   │
│  │ Elasticsearch 7.x (9200)                │   │
│  │ - 单节点模式                             │   │
│  └─────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

#### Docker Compose配置

```yaml
version: '3.8'

services:
  # Nginx
  nginx:
    image: nginx:latest
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/conf.d:/etc/nginx/conf.d
      - ./static:/usr/share/nginx/html
    depends_on:
      - gateway

  # API网关
  gateway:
    image: library-gateway:1.0.0
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    depends_on:
      - nacos
      - redis

  # 核心服务（聚合部署）
  core-services:
    image: library-core-services:1.0.0
    ports:
      - "8081-8085:8081-8085"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    depends_on:
      - mysql
      - redis

  # MySQL
  mysql:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: library_system
    volumes:
      - mysql_data:/var/lib/mysql

  # Redis
  redis:
    image: redis:6.0
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data

  # Elasticsearch
  elasticsearch:
    image: elasticsearch:7.17.0
    ports:
      - "9200:9200"
    environment:
      - discovery.type=single-node
    volumes:
      - es_data:/usr/share/elasticsearch/data

volumes:
  mysql_data:
  redis_data:
  es_data:
```

### 8.2 集群部署方案（中大型图书馆）

**适用场景**：日均访问量 > 50000，馆藏 > 50万册

```
负载均衡层
├── Nginx 1 (主负载均衡)
└── Nginx 2 (备负载均衡, Keepalived)
    ↓
网关集群
├── Gateway 1
├── Gateway 2
└── Gateway 3
    ↓
服务集群
├── 图书服务集群 (book-service × 2)
├── 流通服务集群 (circulation-service × 3)
└── 推荐服务集群 (recommend-service × 2)
    ↓
数据库集群
├── MySQL主从集群
│   ├── Master
│   ├── Slave 1
│   └── Slave 2
├── Redis集群
│   ├── Master
│   ├── Slave 1
│   └── Slave 2
└── Elasticsearch集群
    ├── Node 1 (Master)
    ├── Node 2 (Data)
    └── Node 3 (Data)
```

#### Kubernetes部署配置

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: book-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: book-service
  template:
    metadata:
      labels:
        app: book-service
    spec:
      containers:
      - name: book-service
        image: library/book-service:1.0.0
        ports:
        - containerPort: 8082
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8082
          initialDelaySeconds: 60
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8082
          initialDelaySeconds: 30
---
# hpa.yaml (水平自动扩展)
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: book-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: book-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

---

## 九、技术选型建议

### 9.1 核心技术栈

| 技术领域 | 推荐选型 | 理由 | 备选方案 |
|---------|---------|------|---------|
| **后端框架** | Spring Boot 2.7+ | 生态成熟、社区活跃、开发效率高 | Quarkus, Micronaut |
| **微服务框架** | Spring Cloud Alibaba | 国内场景优化、中文文档丰富 | Spring Cloud Netflix |
| **API网关** | Spring Cloud Gateway | 异步非阻塞、性能优异 | Kong, APISIX |
| **服务注册** | Nacos | 注册+配置中心、动态配置 | Eureka, Consul |
| **ORM框架** | MyBatis Plus | 简化CRUD、代码生成、插件丰富 | JPA, jOOQ |
| **认证鉴权** | Spring Security + JWT | 安全可靠、功能完善 | Shiro, Sa-Token |
| **限流熔断** | Sentinel | 阿里开源、规则丰富、控制台友好 | Hystrix, Resilience4j |
| **消息队列** | RabbitMQ | 可靠性高、功能丰富、适合中小规模 | Kafka, RocketMQ |
| **任务调度** | XXL-Job | 分布式调度、界面友好、轻量级 | Elastic-Job, Quartz |
| **链路追踪** | SkyWalking | 无侵入、APM功能强大 | Zipkin, Jaeger |
| **API文档** | Knife4j | Swagger增强、界面美观 | SpringDoc |

### 9.2 AI/ML技术栈

| 功能模块 | 推荐技术 | 理由 | 备选方案 |
|---------|---------|------|---------|
| **推荐算法** | Surprise + TensorFlow | 协同过滤+深度学习组合 | LightFM, PyTorch |
| **NLP框架** | HuggingFace Transformers | 预训练模型丰富、易用 | spaCy, AllenNLP |
| **人脸识别** | Face++ / 百度AI | 云服务稳定、准确率高 | OpenCV + dlib |
| **OCR识别** | PaddleOCR | 国产开源、中文支持好 | Tesseract, 百度OCR |
| **语音识别** | 科大讯飞 / 百度AI | 中文识别准确、服务稳定 | Google Speech API |
| **模型服务** | FastAPI + Uvicorn | 高性能异步、易于集成 | Flask |

### 9.3 前端技术栈建议

| 端 | 推荐技术 | 理由 |
|----|---------|------|
| **Web管理端** | Vue 3 + Element Plus + Vite | 组件丰富、开发效率高、打包快 |
| **移动端** | uni-app + uView UI | 一次开发多端发布、社区活跃 |
| **数据可视化** | ECharts + AntV G2 | 图表丰富、性能优异 |
| **状态管理** | Pinia | Vue 3官方推荐、轻量级 |

---

## 十、架构演进路线

### 10.1 MVP阶段（0-3个月）

**目标**：快速上线核心功能，验证业务模式

**架构特点**：
- 单体应用 + 微服务混合模式
- 核心业务微服务化，AI能力暂时单独部署
- 单机部署或最小集群

**功能范围**：
- 认证授权、图书管理、流通管理、读者管理
- 基础数据统计、简单推荐（基于规则）
- Web管理端 + 小程序基础版

**技术实现**：
- 前端: Vue 3 + uni-app
- 后端: Spring Boot单体 (可拆分为3-5个微服务)
- 数据库: MySQL单机 + Redis单机
- 部署: Docker Compose / 单台服务器

### 10.2 完整版阶段（3-6个月）

**目标**：完善AI能力，优化用户体验

**架构特点**：
- 全面微服务化
- AI服务独立部署（Python FastAPI）
- 引入消息队列解耦
- 主从数据库 + Redis集群

**功能范围**：
- AI智能推荐（协同过滤 + 深度学习）
- 智能问答助手（BERT模型）
- 人脸识别、OCR识别
- 完整的数据分析看板

**技术实现**：
- 前端: Vue 3 + uni-app完整版
- 后端: 8-10个微服务
- AI服务: Python FastAPI (推荐、NLP、视觉)
- 数据库: MySQL主从 + Redis Sentinel + ES集群
- 消息队列: RabbitMQ
- 部署: Docker Swarm / K8s

### 10.3 智能化升级阶段（6-12个月）

**目标**：全面AI赋能，打造行业标杆

**架构特点**：
- 服务网格（Istio）治理
- 智能盘点、预测性采购
- 知识图谱构建
- 多租户SaaS化

**功能范围**：
- 语音交互界面
- RFID智能盘点
- 知识图谱语义检索
- 预测性采购建议
- 区域图书馆联盟

**技术实现**：
- 前端: Vue 3 + uni-app + 自助终端
- 后端: 12-15个微服务 + 服务网格
- AI服务: TensorFlow Serving + 模型管理平台
- 数据库: MySQL分库分表 + Redis集群 + ES集群 + Neo4j
- 消息队列: Kafka
- 部署: Kubernetes + Istio

---

## 十一、关键技术决策与最佳实践

### 11.1 数据一致性保证

#### 11.1.1 分布式事务方案

采用**Saga模式**处理跨服务事务：

```java
// 借阅流程Saga编排
@Service
public class BorrowSagaOrchestrator {

    @Transactional
    public BorrowResult executeBorrowSaga(BorrowRequest request) {
        // 1. 检查读者资格
        readerService.checkBorrowEligibility(request.getReaderId());

        // 2. 锁定图书库存
        boolean lockSuccess = bookService.lockStock(request.getBookId());

        try {
            // 3. 创建借阅记录
            CirculationRecord record = circulationService.createBorrowRecord(request);

            // 4. 扣减库存
            bookService.reduceStock(request.getBookId());

            // 5. 发送通知
            notificationService.sendBorrowNotification(record);

            return BorrowResult.success(record);

        } catch (Exception e) {
            // 补偿：释放库存锁定
            if (lockSuccess) {
                bookService.unlockStock(request.getBookId());
            }
            throw new BorrowException("创建借阅记录失败", e);
        }
    }
}
```

#### 11.1.2 幂等性保证

```java
@Service
public class IdempotentService {

    @Autowired
    private RedisTemplate redisTemplate;

    // 基于Redis的幂等性检查
    public boolean checkIdempotent(String requestId, long ttl) {
        String key = "idempotent:" + requestId;

        // SETNX保证原子性
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            key,
            "1",
            ttl,
            TimeUnit.SECONDS
        );

        return Boolean.TRUE.equals(success);
    }
}
```

### 11.2 性能优化策略

#### 11.2.1 缓存预热

```java
@Component
public class CacheWarmer implements ApplicationListener<ContextRefreshedEvent> {

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("开始缓存预热...");

        // 预热热门图书
        List<Book> hotBooks = bookService.getTopBorrowedBooks(100);
        hotBooks.forEach(book -> {
            String key = "book:hot:" + book.getBookId();
            redisTemplate.opsForValue().set(key, book, 1, TimeUnit.HOURS);
        });

        log.info("缓存预热完成");
    }
}
```

#### 11.2.2 批量查询优化

```java
@Service
public class BookService {

    // 批量查询避免N+1问题
    public List<Book> getBooksWithDetails(List<Long> bookIds) {
        // 1. 批量查询图书基本信息
        List<Book> books = bookMapper.selectBatchIds(bookIds);

        // 2. 提取所有作者ID
        Set<Long> authorIds = books.stream()
            .map(Book::getAuthorId)
            .collect(Collectors.toSet());

        // 3. 批量查询作者信息
        Map<Long, Author> authorMap = authorMapper.selectBatchIds(authorIds)
            .stream()
            .collect(Collectors.toMap(Author::getAuthorId, a -> a));

        // 4. 组装数据
        books.forEach(book -> {
            book.setAuthor(authorMap.get(book.getAuthorId()));
        });

        return books;
    }
}
```

### 11.3 安全最佳实践

#### 11.3.1 敏感数据脱敏

```java
@Component
public class DataMaskingService {

    // 手机号脱敏
    public String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    // 身份证脱敏
    public String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 15) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(14);
    }
}
```

#### 11.3.2 SQL注入防护

```java
// 使用MyBatis Plus防止SQL注入
@Service
public class BookSearchService {

    public List<Book> searchBooks(String keyword, String category) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();

        // 安全的模糊查询
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w
                .like(Book::getTitle, keyword)
                .or()
                .like(Book::getAuthor, keyword)
            );
        }

        // 安全的等值查询
        if (StringUtils.isNotBlank(category)) {
            wrapper.eq(Book::getCategory, category);
        }

        return bookMapper.selectList(wrapper);
    }
}
```

### 11.4 监控与告警

```yaml
# Prometheus监控配置
management:
  endpoints:
    web:
      exposure:
        include: "*"
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
```

```java
// 自定义监控指标
@Component
public class BusinessMetrics {

    private final Counter borrowCounter;
    private final Timer borrowTimer;

    public BusinessMetrics(MeterRegistry registry) {
        // 借阅次数计数器
        this.borrowCounter = Counter.builder("library.borrow.count")
            .description("Total borrow count")
            .register(registry);

        // 借阅耗时计时器
        this.borrowTimer = Timer.builder("library.borrow.duration")
            .description("Borrow operation duration")
            .register(registry);
    }
}
```

---

## 十二、总结与建议

### 12.1 架构核心优势

1. **前端轻量化**：业务逻辑全部后置，前端只负责展示和交互
2. **高扩展性**：微服务架构 + 插件化设计，易于功能扩展
3. **易维护性**：服务边界清晰、代码模块化、配置化管理
4. **高性能**：多级缓存 + 异步处理 + 数据库优化
5. **AI能力解耦**：AI服务独立部署，与业务系统松耦合

### 12.2 实施建议

1. **从MVP开始**：先实现核心功能，快速验证，再逐步完善
2. **渐进式微服务化**：不要一开始就拆分过细，根据业务复杂度逐步拆分
3. **重视数据质量**：AI推荐效果依赖数据质量，做好数据采集和清洗
4. **持续优化**：定期review架构，根据业务变化调整
5. **文档先行**：API文档、部署文档、运维手册要完善

### 12.3 风险控制

| 风险点 | 应对措施 |
|-------|---------|
| **微服务复杂度** | 使用服务网格简化治理，统一监控和追踪 |
| **分布式事务** | 采用Saga模式，业务补偿机制 |
| **AI服务稳定性** | 降级策略，返回规则推荐或热门推荐 |
| **性能瓶颈** | 多级缓存、读写分离、异步处理 |
| **数据安全** | 敏感数据脱敏、加密存储、权限控制 |

### 12.4 关键指标

| 指标类型 | 目标值 | 监控方式 |
|---------|--------|---------|
| **响应时间** | P95 < 500ms | Prometheus + Grafana |
| **可用性** | 99.5% | 健康检查 + 告警 |
| **并发能力** | 500+ QPS | 压测验证 |
| **推荐准确率** | > 75% (MVP) > 85% (完整版) | A/B测试 + CTR分析 |
| **错误率** | < 0.1% | 日志监控 + 告警 |

---

## 附录

### A. 相关文档

- [产品需求文档 (PRD.md)](./PRD.md)
- [业务流程图 (业务流程图.md)](./业务流程图.md)
- [用例图 (用例图.md)](./用例图.md)
- [项目进度 (progress.md)](../progress.md)

### B. 变更记录

| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|----------|--------|
| v1.0 | 2025-10-10 | 初始版本，完整架构设计 | 架构团队 |

---

**文档结束**

*本文档为国创睿峰智能图书馆管理系统后端架构设计正式版本。如有疑问或建议，请联系架构团队。*
