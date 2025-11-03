//# Reader Service 项目骨架创建计划

## 项目概述
创建 reader-service 微服务项目骨架，这是 Sprint 3 Phase 1 的第一个服务，负责读者管理、读者证管理、借阅历史和积分系统。

## 技术栈
- Spring Boot 3.2.2 (父POM使用3.x，非2.7.x)
- Spring Cloud Alibaba 2023.0.1.0
- MyBatis Plus 3.5.9
- PostgreSQL 15
- Nacos (服务注册和配置中心)
- Sentinel (限流熔断)
- Java 17

## 任务列表

### 1. 项目结构创建
- [ ] 创建 reader-service 目录
- [ ] 创建标准 Maven 项目结构
- [ ] 创建包结构: controller, service, mapper, entity, dto

### 2. Maven 配置
- [ ] 创建 pom.xml，继承父 POM
- [ ] 添加必要依赖: common modules, PostgreSQL, Nacos, Sentinel
- [ ] 配置构建插件
- [ ] 更新父 POM 的 modules 列表

### 3. 实体类和数据库映射
- [ ] 创建 Reader 实体类 (对应 readers 表)
- [ ] 创建 ReaderCard 实体类 (对应 reader_cards 表)
- [ ] 创建 ReaderPoint 实体类 (对应 reader_points 表)
- [ ] 使用 MyBatis Plus 注解配置

### 4. DTO 类
- [ ] 创建 ReaderDTO (读者信息展示)
- [ ] 创建 ReaderQueryDTO (查询参数)
- [ ] 创建 CreateReaderRequest (创建读者请求)
- [ ] 创建 UpdateReaderRequest (更新读者请求)
- [ ] 创建 ReaderCardDTO (读者证信息)
- [ ] 创建 ReaderPointDTO (积分信息)
- [ ] 创建 BorrowHistoryDTO (借阅历史)

### 5. Mapper 接口
- [ ] 创建 ReaderMapper 接口
- [ ] 创建 ReaderCardMapper 接口
- [ ] 创建 ReaderPointMapper 接口
- [ ] 创建 MyBatis XML 映射文件 (如需自定义SQL)

### 6. Service 层
- [ ] 创建 ReaderService 接口 (定义业务方法)
- [ ] 创建 ReaderServiceImpl 实现类 (实现 CRUD、分类、读者证管理等)
- [ ] 创建 ReaderCardService 接口和实现
- [ ] 创建 ReaderPointService 接口和实现

### 7. Controller 层
- [ ] 创建 ReaderController (RESTful API)
- [ ] 添加 Swagger/Knife4j 注解
- [ ] 实现读者 CRUD 接口
- [ ] 实现读者证管理接口
- [ ] 实现借阅历史查询接口
- [ ] 实现积分查询接口

### 8. 启动类和配置
- [ ] 创建 ReaderServiceApplication 启动类
- [ ] 创建 application.yml (基础配置)
- [ ] 创建 bootstrap.yml (Nacos 配置)
- [ ] 创建 application-dev.yml (开发环境配置)

### 9. 通用组件 (如不存在于 common)
- [ ] 验证 common-core 中的 Result 响应类
- [ ] 验证 common-web 中的全局异常处理
- [ ] 验证 common-mybatis 中的分页配置

### 10. 测试和验证
- [ ] 验证项目可以正常编译
- [ ] 验证 Maven 依赖正确解析
- [ ] 创建基本的单元测试类

## API 接口设计

### 读者管理
- POST   /api/v1/readers              - 创建读者
- GET    /api/v1/readers/{id}         - 查询读者详情
- GET    /api/v1/readers              - 分页查询读者列表
- PUT    /api/v1/readers/{id}         - 更新读者信息
- DELETE /api/v1/readers/{id}         - 删除读者(软删除)

### 读者证管理
- POST   /api/v1/readers/card         - 办理读者证
- PUT    /api/v1/readers/card/reissue - 补办读者证
- DELETE /api/v1/readers/card/{id}    - 注销读者证

### 查询接口
- GET    /api/v1/readers/{id}/history - 查询借阅历史
- GET    /api/v1/readers/{id}/points  - 查询积分

## 数据库表 (已存在)
- readers - 读者主表
- reader_cards - 读者证表
- reader_points - 积分记录表

## 读者分类
- STUDENT - 学生读者
- TEACHER - 教师读者
- SOCIAL - 社会读者

## 注意事项
1. 使用父 POM 的 Spring Boot 3.2.2，不是 2.7.x
2. PostgreSQL 驱动使用 org.postgresql:postgresql
3. 继承 common 模块的通用功能
4. 所有代码使用 Lombok 简化
5. 参数校验使用 @Valid 和 Jakarta Validation
6. 统一返回 Result<T> 格式
7. 使用 Knife4j 生成 API 文档

---

## Review Section
(将在任务完成后填写)
