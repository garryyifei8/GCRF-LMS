# 国创睿峰智能图书馆管理系统 - 后端服务

## 项目简介

国创睿峰智能图书馆管理系统是一款面向中小型图书馆的现代化、智能化管理平台。本项目为后端微服务部分，采用Spring Boot + Spring Cloud Alibaba微服务架构，融合AI技术，为图书馆提供从图书编目、典藏、流通到读者服务的全流程智能化解决方案。

## 技术架构

### 技术栈

| 技术 | 版本 | 说明 |
|-----|------|------|
| JDK | 11+ | Java开发环境 |
| Spring Boot | 2.7.18 | 基础框架 |
| Spring Cloud | 2021.0.8 | 微服务框架 |
| Spring Cloud Alibaba | 2021.0.5.0 | 微服务组件 |
| Nacos | 2.x | 服务注册与配置中心 |
| Sentinel | 1.8.x | 限流熔断 |
| PostgreSQL | 15+ | 关系型数据库 |
| Redis | 6.0+ | 缓存 |
| MyBatis Plus | 3.5.5 | ORM框架 |
| RabbitMQ | 3.x | 消息队列 |
| Elasticsearch | 7.x | 搜索引擎 |
| MongoDB | 5.x | 文档数据库 |

### 微服务架构

```
┌─────────────────────────────────────────────────────────┐
│                    前端层                                │
│   Web管理端 (Vue 3)    |    小程序 (uni-app)            │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                  API网关 (8080)                          │
│              library-gateway                             │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                   业务微服务                             │
│  ┌──────────────────────────────────────────────────┐  │
│  │ auth-service (8081)        认证授权服务          │  │
│  │ book-service (8082)        图书服务              │  │
│  │ circulation-service (8083) 流通服务              │  │
│  │ reader-service (8084)      读者服务              │  │
│  │ system-service (8085)      系统服务              │  │
│  └──────────────────────────────────────────────────┘  │
│                                                         │
│                   AI智能微服务                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │ recommend-service (8086)   推荐服务              │  │
│  │ nlp-service (8087)         NLP服务               │  │
│  │ vision-service (8088)      视觉识别服务          │  │
│  │ analytics-service (8089)   数据分析服务          │  │
│  └──────────────────────────────────────────────────┘  │
│                                                         │
│                   基础服务                              │
│  ┌──────────────────────────────────────────────────┐  │
│  │ notification-service (8090) 通知服务             │  │
│  │ file-service (8091)         文件服务             │  │
│  │ search-service (8092)       搜索服务             │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## 项目结构

```
library-backend/
├── library-common           # 公共模块
│   └── src/main/java/com/gcrf/library/common/
│       ├── constant/        # 常量
│       ├── enums/           # 枚举
│       ├── exception/       # 异常
│       ├── result/          # 统一返回结果
│       └── utils/           # 工具类
│
├── library-api              # API模块 (Feign接口)
│   └── src/main/java/com/gcrf/library/api/
│       ├── dto/             # 数据传输对象
│       ├── vo/              # 视图对象
│       └── feign/           # Feign客户端
│
├── library-gateway          # API网关 (8080)
│
├── auth-service             # 认证授权服务 (8081)
├── book-service             # 图书服务 (8082)
├── circulation-service      # 流通服务 (8083)
├── reader-service           # 读者服务 (8084)
├── system-service           # 系统服务 (8085)
│
├── recommend-service        # 推荐服务 (8086)
├── nlp-service              # NLP服务 (8087)
├── vision-service           # 视觉识别服务 (8088)
├── analytics-service        # 数据分析服务 (8089)
│
├── notification-service     # 通知服务 (8090)
├── file-service             # 文件服务 (8091)
├── search-service           # 搜索服务 (8092)
│
└── pom.xml                  # Maven父项目配置
```

## 快速开始

### 环境准备

1. **JDK 11+**
   ```bash
   java -version
   ```

2. **Maven 3.6+**
   ```bash
   mvn -version
   ```

3. **PostgreSQL 15+**
   ```bash
   # 创建数据库
   CREATE DATABASE library_user WITH ENCODING 'UTF8';
   CREATE DATABASE library_book WITH ENCODING 'UTF8';
   CREATE DATABASE library_circulation WITH ENCODING 'UTF8';
   CREATE DATABASE library_reader WITH ENCODING 'UTF8';
   CREATE DATABASE library_system WITH ENCODING 'UTF8';
   CREATE DATABASE library_notification WITH ENCODING 'UTF8';
   ```

4. **Redis 6.0+**
   ```bash
   redis-server
   ```

5. **Nacos 2.x** (服务注册与配置中心)
   ```bash
   # 下载Nacos
   wget https://github.com/alibaba/nacos/releases/download/2.2.3/nacos-server-2.2.3.tar.gz

   # 启动Nacos (单机模式)
   cd nacos/bin
   sh startup.sh -m standalone

   # 访问控制台
   http://localhost:8848/nacos
   # 用户名密码: nacos/nacos
   ```

### 编译项目

```bash
# 进入项目根目录
cd library-backend

# 编译整个项目
mvn clean install -DskipTests
```

### 启动服务

**启动顺序：**

1. **启动基础设施**
   - MySQL
   - Redis
   - Nacos

2. **启动网关**
   ```bash
   cd library-gateway
   mvn spring-boot:run
   # 或者
   java -jar target/library-gateway.jar
   ```

3. **启动业务微服务** (可同时启动)
   ```bash
   # 认证服务
   cd auth-service && mvn spring-boot:run

   # 图书服务
   cd book-service && mvn spring-boot:run

   # 流通服务
   cd circulation-service && mvn spring-boot:run

   # 读者服务
   cd reader-service && mvn spring-boot:run

   # 系统服务
   cd system-service && mvn spring-boot:run
   ```

4. **访问服务**
   - 网关地址: http://localhost:8080
   - Nacos控制台: http://localhost:8848/nacos
   - API文档: http://localhost:808{服务端口}/doc.html

## 服务说明

### 业务微服务

#### 1. auth-service (认证授权服务)
- **端口**: 8081
- **职责**: 用户认证、JWT签发、权限管理
- **核心功能**:
  - 用户登录/登出
  - Token生成与验证
  - 权限控制(RBAC)

#### 2. book-service (图书服务)
- **端口**: 8082
- **职责**: 图书编目、典藏、剔旧、盘点
- **核心功能**:
  - 图书CRUD
  - 智能编目(ISBN检索)
  - 图书盘点
  - 条码管理

#### 3. circulation-service (流通服务)
- **端口**: 8083
- **职责**: 借阅、归还、预约、续借
- **核心功能**:
  - 图书借出/归还
  - 逾期处理
  - 预约管理
  - 续借管理

#### 4. reader-service (读者服务)
- **端口**: 8084
- **职责**: 读者管理、办证、人脸采集
- **核心功能**:
  - 读者CRUD
  - 读者证办理
  - 人脸信息采集
  - 读者画像

#### 5. system-service (系统服务)
- **端口**: 8085
- **职责**: 系统配置、用户管理、数据备份
- **核心功能**:
  - 系统参数配置
  - 用户管理
  - 角色权限管理
  - 数据备份

### AI智能微服务

#### 6. recommend-service (推荐服务)
- **端口**: 8086
- **职责**: AI智能推荐引擎
- **核心功能**:
  - 协同过滤推荐
  - 内容推荐
  - 深度学习推荐

#### 7. nlp-service (NLP服务)
- **端口**: 8087
- **职责**: 智能问答、意图识别
- **核心功能**:
  - 智能问答
  - 意图识别
  - 实体抽取
  - 多轮对话

#### 8. vision-service (视觉识别服务)
- **端口**: 8088
- **职责**: 人脸识别、OCR识别
- **核心功能**:
  - 人脸检测与识别
  - ISBN/条码识别
  - 图书封面识别

#### 9. analytics-service (数据分析服务)
- **端口**: 8089
- **职责**: 数据统计、趋势预测
- **核心功能**:
  - 馆情分析
  - 借阅统计
  - 趋势预测
  - 决策支持

### 基础服务

#### 10. notification-service (通知服务)
- **端口**: 8090
- **职责**: 消息通知、到期提醒
- **核心功能**:
  - 微信推送
  - 短信通知
  - 邮件通知

#### 11. file-service (文件服务)
- **端口**: 8091
- **职责**: 文件上传、图片处理
- **核心功能**:
  - 文件上传/下载
  - 图片压缩
  - OSS存储

#### 12. search-service (搜索服务)
- **端口**: 8092
- **职责**: 全文检索、语义搜索
- **核心功能**:
  - 图书全文检索
  - 搜索建议
  - 语义搜索

## 开发规范

### 代码规范

1. **包结构规范**
   ```
   com.gcrf.library.{服务名}/
   ├── controller/      # 控制器
   ├── service/         # 业务逻辑
   │   └── impl/        # 实现类
   ├── mapper/          # MyBatis Mapper
   ├── entity/          # 实体类
   ├── dto/             # 数据传输对象
   ├── vo/              # 视图对象
   ├── config/          # 配置类
   └── utils/           # 工具类
   ```

2. **命名规范**
   - 类名: 大驼峰 `BookController`
   - 方法名: 小驼峰 `getBookById`
   - 常量: 全大写下划线 `MAX_BOOK_COUNT`
   - 包名: 全小写 `com.gcrf.library`

3. **注释规范**
   - 类注释: 说明类的功能和作者
   - 方法注释: 说明方法功能、参数、返回值
   - 复杂逻辑: 添加行内注释

### API规范

1. **RESTful风格**
   - GET: 查询
   - POST: 新增
   - PUT: 修改
   - DELETE: 删除

2. **URL规范**
   ```
   GET    /api/books          # 获取图书列表
   GET    /api/books/{id}     # 获取单个图书
   POST   /api/books          # 创建图书
   PUT    /api/books/{id}     # 更新图书
   DELETE /api/books/{id}     # 删除图书
   ```

3. **统一返回格式**
   ```json
   {
     "code": 200,
     "message": "成功",
     "data": {}
   }
   ```

### 数据库规范

1. **表命名**: `t_` + 表名 (如: `t_book`)
2. **字段命名**: 下划线分隔 (如: `book_id`)
3. **主键**: 统一使用 `id` 或 `{表名}_id`
4. **时间戳**: `created_at`, `updated_at`

## 部署

### Docker部署

```bash
# 构建镜像
docker build -t library-gateway:latest ./library-gateway
docker build -t auth-service:latest ./auth-service

# 运行容器
docker run -d -p 8080:8080 --name library-gateway library-gateway:latest
docker run -d -p 8081:8081 --name auth-service auth-service:latest
```

### Docker Compose部署

参考 `docker-compose.yml` 文件

### Kubernetes部署

参考 `k8s/` 目录下的配置文件

## 常见问题

### 1. Nacos连接失败
**原因**: Nacos服务未启动或地址配置错误
**解决**:
- 检查Nacos是否启动: `netstat -an | grep 8848`
- 检查配置文件中的Nacos地址

### 2. 数据库连接失败
**原因**: 数据库未启动或连接配置错误
**解决**:
- 检查PostgreSQL服务
- 检查用户名密码
- 检查数据库是否存在

### 3. Redis连接失败
**原因**: Redis服务未启动
**解决**: 启动Redis `redis-server`

## 联系我们

- 公司: 国创睿峰科技有限公司
- 邮箱: support@gcrf.com
- 网站: https://www.gcrf.com

## 许可证

Copyright © 2025 国创睿峰科技有限公司. All rights reserved.
