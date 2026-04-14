# Chat Service - 智能问答服务

## 概述

Chat Service 是 GCRF 智能图书馆管理系统的智能问答模块，提供基于知识库的 FAQ 问答功能。

## 功能特性

### 1. 意图识别
- 基于关键词匹配和相似度计算
- 支持多种意图类型：借阅咨询、还书咨询、续借咨询、逾期咨询等
- 实体提取：书名、作者、读者类型、时间等

### 2. FAQ 知识库
- 分类管理：借阅规则、开馆时间、罚款规则、预约说明、读者证办理等
- 支持富文本答案（HTML格式）
- 关键词和意图标签关联
- 优先级排序

### 3. 会话管理
- 会话 ID 追踪
- 消息历史记录
- 会话上下文保持

### 4. 反馈系统
- 有帮助/无帮助反馈
- 反馈统计
- FAQ 效果分析

### 5. 热门问题统计
- 实时统计提问频率
- 热门问题排行榜

## 技术架构

### 技术栈
- Spring Boot 3.2.2
- MyBatis Plus 3.5.9
- PostgreSQL 15+
- Redis (会话缓存)
- HanLP (中文NLP)
- Apache Commons Text (字符串相似度)

### 核心组件

```
chat-service/
├── controller/
│   └── ChatController.java          # API 控制器
├── service/
│   ├── ChatService.java             # 服务接口
│   └── impl/ChatServiceImpl.java    # 服务实现
├── engine/
│   ├── ChatBotEngine.java           # 聊天机器人引擎
│   ├── IntentRecognizer.java        # 意图识别器
│   ├── FaqMatcher.java              # FAQ 匹配器
│   └── IntentResult.java            # 意图识别结果
├── entity/
│   ├── FaqCategory.java             # FAQ 分类
│   ├── FaqKnowledge.java            # FAQ 知识库
│   ├── ChatIntent.java              # 意图定义
│   ├── ChatSession.java             # 会话记录
│   ├── ChatMessage.java             # 会话消息
│   ├── ChatFeedback.java            # 反馈记录
│   └── HotQuestionStats.java        # 热门问题统计
├── mapper/
│   └── *Mapper.java                 # MyBatis Mapper
├── dto/
│   ├── request/                     # 请求 DTO
│   └── response/                    # 响应 VO
└── config/
    └── *Config.java                 # 配置类
```

## API 接口

### 1. 发送消息
```http
POST /api/v1/chat/message
Content-Type: application/json

{
  "sessionId": "sess_xxx",  // 可选，新会话不传
  "content": "借书期限是多久？",
  "readerId": 123  // 可选
}
```

### 2. 获取对话历史
```http
GET /api/v1/chat/history/{sessionId}
```

### 3. 提交反馈
```http
POST /api/v1/chat/feedback
Content-Type: application/json

{
  "sessionId": "sess_xxx",
  "messageId": 123,
  "faqId": 456,
  "feedbackType": "helpful",  // helpful, unhelpful, report
  "comment": "回答很有帮助"
}
```

### 4. 获取热门问题
```http
GET /api/v1/chat/hot-questions?limit=10
```

### 5. 获取对话统计
```http
GET /api/v1/chat/stats
```

### 6. 刷新缓存
```http
POST /api/v1/chat/cache/refresh
```

## 数据库设计

### 表结构

| 表名 | 描述 |
|------|------|
| faq_category | FAQ 分类表 |
| faq_knowledge | FAQ 知识库表 |
| chat_intent | 意图定义表 |
| chat_session | 会话记录表 |
| chat_message | 会话消息表 |
| chat_feedback | 反馈记录表 |
| hot_question_stats | 热门问题统计表 |

### 初始数据
- 8 个 FAQ 分类
- 12 个意图定义
- 15+ 条初始 FAQ 知识

## 配置说明

### application.yml
```yaml
server:
  port: 8088

spring:
  application:
    name: chat-service
  datasource:
    url: jdbc:postgresql://localhost:5432/gcrf_chat

chat:
  session:
    timeout-minutes: 30
    max-history-messages: 50
  intent:
    min-confidence: 0.6
    fallback-threshold: 0.3
  cache:
    faq-ttl: 3600
    intent-ttl: 1800
```

## 部署说明

### 1. 创建数据库
```sql
CREATE DATABASE gcrf_chat;
```

### 2. 执行数据库迁移
```bash
psql -U postgres -d gcrf_chat -f src/main/resources/db/V1__init_chat_schema.sql
psql -U postgres -d gcrf_chat -f src/main/resources/db/V2__init_faq_data.sql
```

### 3. 编译运行
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn clean package -pl chat-service -am -DskipTests
java -jar target/chat-service-1.0.0-SNAPSHOT.jar
```

## 扩展说明

### 添加新意图
1. 在 `chat_intent` 表中添加意图定义
2. 设置匹配模式 patterns（关键词数组）
3. 设置 action_type：
   - `FAQ_LOOKUP`: 查询 FAQ 知识库
   - `API_CALL`: 调用外部 API
   - `TRANSFER`: 转人工
   - `NONE`: 直接响应

### 添加新 FAQ
1. 确定 FAQ 分类
2. 添加问题和答案（支持 HTML）
3. 设置关键词和意图标签
4. 调用 `/api/v1/chat/cache/refresh` 刷新缓存

## 版本历史

- v1.0.0 (2025-11-30): 初始版本
  - 基础意图识别
  - FAQ 知识库匹配
  - 会话管理
  - 反馈系统
