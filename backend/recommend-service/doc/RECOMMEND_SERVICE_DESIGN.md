# GCRF图书推荐服务技术设计文档

## 1. 概述

### 1.1 服务简介
推荐服务(recommend-service)是GCRF智能图书馆管理系统的核心AI功能模块，基于协同过滤算法为读者提供个性化图书推荐。

### 1.2 技术栈
- **框架**: Spring Boot 3.2.2 + Spring Cloud 2023.0.0
- **数据库**: PostgreSQL 15+
- **缓存**: Redis 7.x
- **服务注册**: Nacos 2.3.x
- **Java版本**: 21

### 1.3 服务端口
- 推荐服务: 8087
- 网关入口: 8080 (通过 `/api/v1/recommend/**` 路由)

---

## 2. 系统架构

### 2.1 整体架构
```
                    ┌─────────────────┐
                    │  API Gateway    │
                    │    (8080)       │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │ Recommend       │
                    │ Service (8087)  │
                    └────────┬────────┘
                             │
         ┌───────────┬───────┴───────┬───────────┐
         │           │               │           │
         ▼           ▼               ▼           ▼
   ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
   │ PostgreSQL│ │  Redis   │ │ Nacos    │ │ Other    │
   │ (gcrf_   │ │ (缓存)   │ │(服务注册)│ │ Services │
   │ recommend)│ │          │ │          │ │          │
   └──────────┘ └──────────┘ └──────────┘ └──────────┘
```

### 2.2 模块结构
```
recommend-service/
├── src/main/java/com/gcrf/library/recommend/
│   ├── RecommendServiceApplication.java    # 启动类
│   ├── algorithm/                          # 推荐算法
│   │   ├── SimilarityCalculator.java       # 相似度计算器
│   │   ├── UserBasedCF.java                # 用户协同过滤
│   │   ├── ItemBasedCF.java                # 物品协同过滤
│   │   ├── PopularRecommender.java         # 热门推荐
│   │   └── HybridRecommender.java          # 混合推荐
│   ├── config/                             # 配置类
│   │   ├── RedisConfig.java                # Redis缓存配置
│   │   ├── MyBatisPlusConfig.java          # MyBatis Plus配置
│   │   └── RecommendSecurityConfig.java    # 安全配置
│   ├── controller/                         # 控制器
│   │   └── RecommendController.java        # 推荐API
│   ├── dto/                                # 数据传输对象
│   │   ├── request/
│   │   │   ├── RecommendRequest.java
│   │   │   └── BatchRecommendRequest.java
│   │   └── response/
│   │       ├── RecommendationVO.java
│   │       └── RecommendStatsVO.java
│   ├── entity/                             # 实体类
│   │   ├── BorrowHistory.java              # 借阅历史
│   │   ├── UserSimilarity.java             # 用户相似度
│   │   ├── ItemSimilarity.java             # 物品相似度
│   │   └── RecommendationLog.java          # 推荐日志
│   ├── mapper/                             # Mapper接口
│   │   ├── BorrowHistoryMapper.java
│   │   ├── UserSimilarityMapper.java
│   │   ├── ItemSimilarityMapper.java
│   │   └── RecommendationLogMapper.java
│   └── service/                            # 服务层
│       ├── RecommendService.java
│       └── impl/RecommendServiceImpl.java
└── src/main/resources/
    ├── application.yml                     # 配置文件
    └── db/migration/                       # 数据库迁移脚本
        ├── V1__init_recommend_tables.sql
        └── V2__insert_sample_data.sql
```

---

## 3. 推荐算法

### 3.1 用户协同过滤 (User-based CF)

#### 算法原理
1. 计算用户间的相似度（基于借阅历史）
2. 找到与目标用户最相似的K个邻居
3. 推荐邻居借阅过但目标用户未借阅的图书
4. 按加权评分排序

#### 相似度计算
使用余弦相似度：
```
cos(A,B) = (A · B) / (||A|| × ||B||)
```

#### 配置参数
```yaml
recommend.algorithm.user-cf:
  min-common-items: 3       # 最少共同借阅数
  max-neighbors: 50         # 最大邻居数
  similarity-threshold: 0.1 # 相似度阈值
```

### 3.2 物品协同过滤 (Item-based CF)

#### 算法原理
1. 计算图书间的相似度（基于共同借阅用户）
2. 根据用户历史借阅，找到相似图书
3. 按相似度加权推荐

#### 相似度计算
使用调整余弦相似度（消除用户评分偏差）：
```
sim(i,j) = Σu(rui - r̄u)(ruj - r̄u) / √(Σu(rui - r̄u)²) × √(Σu(ruj - r̄u)²)
```

#### 配置参数
```yaml
recommend.algorithm.item-cf:
  min-common-users: 5         # 最少共同借阅用户数
  max-similar-items: 100      # 最大相似物品数
  similarity-threshold: 0.1   # 相似度阈值
```

### 3.3 热门推荐 (Popular)

#### 算法原理
基于时间窗口内的借阅统计，推荐借阅量最高的图书。
适用于解决新用户冷启动问题。

#### 配置参数
```yaml
recommend.algorithm.popular:
  time-window-days: 30  # 时间窗口（天）
  max-results: 50       # 最大结果数
```

### 3.4 混合推荐 (Hybrid)

#### 融合策略
结合多种算法，根据用户特征动态调整权重：

**活跃用户（借阅>=5本）**:
- User-based CF: 40%
- Item-based CF: 40%
- Popular: 20%

**新用户（借阅<5本）**:
- User-based CF: 20%
- Item-based CF: 30%
- Popular: 50%

---

## 4. API接口

### 4.1 获取读者个性化推荐
```
GET /api/v1/recommend/books/{readerId}
```

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| readerId | Long | 是 | 读者ID（路径参数） |
| limit | Integer | 否 | 推荐数量，默认20 |
| algorithm | String | 否 | 算法类型，默认HYBRID |
| scene | String | 否 | 推荐场景，默认HOMEPAGE |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "readerId": 1,
      "readerName": "张三",
      "bookId": 5,
      "bookTitle": "数据结构与算法",
      "author": "严蔚敏",
      "score": 0.85,
      "algorithm": "HYBRID",
      "scene": "HOMEPAGE",
      "reason": "基于你的阅读历史智能推荐"
    }
  ]
}
```

### 4.2 获取热门图书
```
GET /api/v1/recommend/popular
```

### 4.3 获取相似图书
```
GET /api/v1/recommend/similar/{bookId}
```

### 4.4 批量生成推荐
```
POST /api/v1/recommend/batch
```

### 4.5 获取推荐统计
```
GET /api/v1/recommend/stats
```

### 4.6 记录推荐点击
```
POST /api/v1/recommend/click?readerId=1&bookId=5
```

### 4.7 重新计算相似度矩阵
```
POST /api/v1/recommend/recompute
```

---

## 5. 数据库设计

### 5.1 借阅历史表 (borrow_history)
```sql
CREATE TABLE borrow_history (
    id BIGSERIAL PRIMARY KEY,
    reader_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    book_title VARCHAR(500),
    category_code VARCHAR(50),
    borrow_time TIMESTAMP NOT NULL,
    return_time TIMESTAMP,
    borrow_days INTEGER,
    rating INTEGER CHECK (rating >= 1 AND rating <= 5),
    implicit_rating DECIMAL(3,2) DEFAULT 3.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 5.2 用户相似度矩阵 (user_similarity)
```sql
CREATE TABLE user_similarity (
    id BIGSERIAL PRIMARY KEY,
    user_id_a BIGINT NOT NULL,
    user_id_b BIGINT NOT NULL,
    similarity_score DECIMAL(10,8) NOT NULL,
    common_items_count INTEGER DEFAULT 0,
    calculated_at TIMESTAMP,
    CONSTRAINT uk_user_similarity UNIQUE (user_id_a, user_id_b)
);
```

### 5.3 物品相似度矩阵 (item_similarity)
```sql
CREATE TABLE item_similarity (
    id BIGSERIAL PRIMARY KEY,
    book_id_a BIGINT NOT NULL,
    book_id_b BIGINT NOT NULL,
    similarity_score DECIMAL(10,8) NOT NULL,
    common_users_count INTEGER DEFAULT 0,
    calculated_at TIMESTAMP,
    CONSTRAINT uk_item_similarity UNIQUE (book_id_a, book_id_b)
);
```

### 5.4 推荐日志表 (recommendation_log)
```sql
CREATE TABLE recommendation_log (
    id BIGSERIAL PRIMARY KEY,
    reader_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    score DECIMAL(10,8),
    algorithm VARCHAR(50) NOT NULL,
    scene VARCHAR(50),
    reason TEXT,
    clicked BOOLEAN DEFAULT FALSE,
    borrowed BOOLEAN DEFAULT FALSE,
    recommended_at TIMESTAMP NOT NULL
);
```

---

## 6. 缓存策略

### 6.1 缓存配置
```yaml
recommendations:      TTL=1小时    # 用户推荐结果
popular-books:        TTL=30分钟   # 热门图书
similar-books:        TTL=2小时    # 相似图书
user-similarity:      TTL=24小时   # 用户相似度
item-similarity:      TTL=24小时   # 物品相似度
```

### 6.2 缓存Key设计
```
recommendations:{readerId}:{algorithm}:{limit}
popular-books:{limit}
similar-books:{bookId}:{limit}
```

---

## 7. 部署说明

### 7.1 数据库初始化
```bash
# 创建数据库
psql -h localhost -U postgres -c "CREATE DATABASE gcrf_recommend;"

# 执行迁移脚本
psql -h localhost -U postgres -d gcrf_recommend -f V1__init_recommend_tables.sql
psql -h localhost -U postgres -d gcrf_recommend -f V2__insert_sample_data.sql
```

### 7.2 服务启动
```bash
# 设置Java 21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# 编译
cd backend
mvn clean package -pl recommend-service -am -DskipTests

# 启动
cd recommend-service
mvn spring-boot:run
```

### 7.3 Docker部署
```bash
# 构建镜像
docker build -t gcrf-recommend-service:latest .

# 运行容器
docker run -d \
  --name gcrf-recommend-service \
  -p 8087:8087 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/gcrf_recommend \
  gcrf-recommend-service:latest
```

---

## 8. 性能优化

### 8.1 相似度矩阵预计算
通过定时任务离线计算相似度矩阵，避免实时计算开销。

### 8.2 分页加载
批量推荐使用分页，避免一次返回过多数据。

### 8.3 缓存预热
服务启动时预热热门图书缓存。

### 8.4 异步日志
推荐日志异步写入，不影响推荐响应时间。

---

## 9. 监控指标

### 9.1 推荐效果指标
- **准确率(Precision)**: 借阅转化数/推荐数
- **点击率(CTR)**: 点击数/推荐数
- **转化率**: 借阅数/点击数

### 9.2 系统性能指标
- 推荐响应时间 (P99 < 200ms)
- 缓存命中率 (目标 > 80%)
- 相似度计算时间

---

## 10. 后续优化方向

1. **深度学习推荐**: 引入神经网络模型（如Wide & Deep）
2. **实时推荐**: 基于实时借阅流的在线学习
3. **多目标优化**: 平衡准确性、多样性、新颖性
4. **A/B测试框架**: 支持多种算法的效果对比实验
5. **冷启动优化**: 基于用户画像的内容推荐

---

**文档版本**: 1.0.0
**最后更新**: 2025-11-26
**作者**: GCRF Team
