# GCRF图书馆管理系统 - 生产部署路线图

**版本**: 1.0.0
**创建日期**: 2025-11-03/
**目标**: 从当前65%完成度达到生产部署标准(90%+)
**总预计时间**: 14-17周 (3.5-4个月)

---

## 📊 当前状态 vs 目标状态

| 维度               | 当前         | 目标           | 差距       |
| ------------------ | ------------ | -------------- | ---------- |
| **后端服务完成度** | 42% (2/7)    | 100% (7/7)     | 58%        |
| **前后端集成**     | 0%           | 100%           | 100%       |
| **API端点实现**    | 38/128 (29%) | 128/128 (100%) | 90个待实现 |
| **Docker化率**     | 28% (2/7)    | 100% (7/7)     | 72%        |
| **测试覆盖**       | 52%          | 85%+           | 33%        |
| **生产就绪度**     | 52%          | 90%+           | 38%        |

---

## 🎯 MVP范围定义

### 核心功能 (必须包含)

1. ✅ 认证授权 (Auth Service) - 已完成
2. ⏳ 图书管理 (Book Service) - 30% → 100%
3. ⏳ 流通管理 (Circulation Service) - 20% → 100%
4. ⏳ 读者管理 (Reader Service) - 25% → 100%
5. ⏳ 基础系统管理 (System Service - 角色/权限/日志)

### 非MVP功能 (可延后)

- AI功能 (推荐/聊天/分析)
- 通知服务 (站内/邮件/短信)
- 高级系统管理 (字典/定时任务)
- 人脸识别
- 数据统计分析

---

## 📅 14周MVP路线图

### Phase 1: 核心服务完成 (Week 1-4)

#### Week 1: Book Service完善

**目标**: 30% → 100%

**任务清单**:

- [ ] **Day 1-2**: 创建OpenAPI 3.0规范 (所有7个服务)
  - 交付: `DevPlan/05_API_SPECIFICATIONS/*.yaml`
  - 工具: SpringDoc OpenAPI

- [ ] **Day 3-5**: 完成Book Service剩余API (7→15个)
  - 图书分类管理 (3个API)
  - 库存管理 (2个API)
  - 图书搜索 (全文检索, 1个API)
  - 文件上传 (MinIO集成, 2个API)

- [ ] **Day 6-7**: Docker化Book Service
  - 创建Dockerfile
  - 更新docker-compose.yml
  - 测试容器启动

**验收标准**:

- ✅ 15个API全部实现
- ✅ 单元测试覆盖80%+
- ✅ Docker镜像构建成功
- ✅ Swagger UI可访问

---

#### Week 2: Circulation Service完善

**目标**: 20% → 100%

**任务清单**:

- [ ] **Day 1-3**: 完成预约管理 (3个API)
  - POST `/api/v1/circulation/reserve` - 预约图书
  - DELETE `/api/v1/circulation/reserve/{id}` - 取消预约
  - GET `/api/v1/circulation/reserves` - 查询预约列表

- [ ] **Day 4-6**: 完成逾期罚金管理 (4个API)
  - GET `/api/v1/circulation/overdue` - 查询逾期记录
  - POST `/api/v1/circulation/overdue/{recordId}/fine` - 计算罚金
  - POST `/api/v1/circulation/fine/{fineId}/pay` - 支付罚金
  - POST `/api/v1/circulation/batch-return` - 批量归还

- [ ] **Day 7**: Docker化 + 统计API (5个)

**验收标准**:

- ✅ 17个API全部实现
- ✅ 库存一致性事务保证
- ✅ 单元测试覆盖80%+
- ✅ Docker镜像构建成功

---

#### Week 3: Reader Service完善

**目标**: 25% → 100%

**任务清单**:

- [ ] **Day 1-3**: 完成读者分类管理 (3个API)
  - GET `/api/v1/readers/types` - 查询读者分类
  - PUT `/api/v1/readers/{id}/type` - 修改读者分类
  - PUT `/api/v1/readers/{id}/borrow-limit` - 设置借阅限额

- [ ] **Day 4-6**: 完成读者统计与批量操作 (7个API)
  - GET `/api/v1/readers/{id}/borrow-history` - 借阅历史
  - GET `/api/v1/readers/{id}/statistics` - 读者统计
  - GET `/api/v1/readers/statistics/by-type` - 按类型统计
  - POST `/api/v1/readers/import` - 批量导入 (Excel)
  - GET `/api/v1/readers/export` - 导出数据

- [ ] **Day 7**: Docker化 + 人脸识别API (可选, 2个)

**验收标准**:

- ✅ 20个API全部实现
- ✅ Excel导入导出功能正常
- ✅ 单元测试覆盖80%+
- ✅ Docker镜像构建成功

---

#### Week 4: System Service核心功能

**目标**: 15% → 60% (仅MVP范围)

**任务清单**:

- [ ] **Day 1-2**: 完善部门管理 (7个API)
- [ ] **Day 3-4**: 完善角色管理 (8个API)
- [ ] **Day 5-6**: 完善权限管理 (7个API)
- [ ] **Day 7**: 完善日志管理 (11个API) + Docker化

**验收标准**:

- ✅ MVP核心33个API实现 (部门/角色/权限/日志)
- ✅ 基于RBAC的权限控制
- ✅ 单元测试覆盖70%+
- ✅ Docker镜像构建成功

---

### Phase 2: 前后端集成 (Week 5-6)

#### Week 5: Mock API替换

**目标**: 将前端Mock API替换为真实后端调用

**任务清单**:

- [ ] **Day 1**: Auth模块集成 (已完成后端)
  - 关闭MSW Mock: `VITE_USE_MOCK=false`
  - 测试登录/登出/刷新流程
  - 修复前端Bug

- [ ] **Day 2**: Books模块集成
  - 对比Mock API与真实API响应格式
  - 修改前端代码 (如需要)
  - 测试CRUD + 搜索功能

- [ ] **Day 3**: Readers模块集成
  - 测试读者管理全流程
  - 测试借书卡管理

- [ ] **Day 4-5**: Circulation模块集成
  - 测试借阅/归还/续借流程
  - 测试预约/逾期处理
  - 验证库存一致性

- [ ] **Day 6-7**: System模块集成
  - 测试角色权限管理
  - 测试日志查询
  - 测试菜单权限

**验收标准**:

- ✅ 前端连接真实后端
- ✅ 核心业务流程端到端测试通过
- ✅ 无Critical Bug

---

#### Week 6: 端到端测试

**目标**: 验证10+核心业务流程

**任务清单**:

- [ ] **Day 1-2**: 搭建Playwright测试环境
  - 安装Playwright
  - 配置测试环境 (Docker Compose)
  - 初始化测试数据

- [ ] **Day 3-5**: 编写E2E测试脚本 (10个场景)
  1. 用户登录与登出
  2. 图书创建与查询
  3. 完整借还书流程 (核心)
  4. 图书续借流程
  5. 读者生命周期管理
  6. 图书预约流程
  7. 逾期处理流程
  8. 角色权限验证
  9. 批量操作 (导入/导出)
  10. 统计报表生成

- [ ] **Day 6-7**: 修复E2E测试发现的Bug

**验收标准**:

- ✅ 10个E2E测试全部通过
- ✅ E2E测试通过率 > 95%
- ✅ 核心流程无阻塞性Bug

---

### Phase 3: API契约测试 (Week 7)

**任务清单**:

- [ ] **Day 1-3**: 为128个API创建契约定义
  - Auth: 11个
  - Book: 15个
  - Circulation: 17个
  - Reader: 20个
  - System: 33个 (MVP范围)
  - 其他: 32个 (可延后)

- [ ] **Day 4-5**: 集成Spring Cloud Contract
  - 添加Maven依赖
  - 配置契约测试插件
  - 生成契约测试代码

- [ ] **Day 6-7**: 运行契约测试并修复不一致

**验收标准**:

- ✅ MVP范围96个API契约测试100%通过
- ✅ 响应格式完全一致
- ✅ 集成到CI/CD Pipeline

---

### Phase 4: 性能优化 (Week 8-9)

#### Week 8: 性能测试

**任务清单**:

- [ ] **Day 1-2**: 配置JMeter/k6测试脚本
  - 登录并发测试 (100用户)
  - 图书查询性能测试 (50用户, 10万数据)
  - 借还书高并发测试 (100用户)

- [ ] **Day 3-5**: 执行性能基准测试
  - 识别性能瓶颈
  - 生成性能报告

- [ ] **Day 6-7**: 性能优化 (第一轮)
  - 数据库索引优化
  - 查询优化 (避免N+1)
  - Redis缓存热点数据

**验收标准**:

- ✅ P95响应时间 < 500ms
- ✅ 支持100并发用户
- ✅ TPS > 500
- ✅ 错误率 < 0.1%

---

#### Week 9: 性能优化 (第二轮)

**任务清单**:

- [ ] **Day 1-3**: 深度优化
  - 连接池配置 (HikariCP)
  - JVM参数调优
  - 异步处理 (通知发送)

- [ ] **Day 4-5**: 压力测试
  - 测试系统极限 (200用户)
  - 识别崩溃点

- [ ] **Day 6-7**: 监控优化
  - 配置Prometheus告警阈值
  - 优化Grafana Dashboard
  - 测试告警触发

**验收标准**:

- ✅ 系统稳定支持100用户
- ✅ 监控告警正常触发
- ✅ 性能报告完成

---

### Phase 5: 安全加固 (Week 10)

**任务清单**:

- [ ] **Day 1-2**: 认证授权测试
  - Token过期测试
  - Token篡改测试
  - 角色权限测试
  - 越权访问测试

- [ ] **Day 3-4**: SQL注入与XSS测试
  - 登录SQL注入测试
  - 图书查询SQL注入测试
  - XSS存储型测试
  - XSS反射型测试

- [ ] **Day 5-6**: 修复安全漏洞
  - 补充输入验证
  - 加强权限检查
  - 配置CSP

- [ ] **Day 7**: HTTPS配置
  - 生成SSL证书
  - 配置Nginx HTTPS
  - 测试HTTPS访问

**验收标准**:

- ✅ 无Critical安全漏洞
- ✅ 认证授权机制完善
- ✅ HTTPS配置成功
- ✅ 安全测试报告完成

---

### Phase 6: 生产环境准备 (Week 11-12)

#### Week 11: 生产配置

**任务清单**:

- [ ] **Day 1-2**: 生产环境配置
  - 配置生产数据库 (主从复制)
  - 配置Redis持久化
  - 配置Nacos生产配置

- [ ] **Day 3-4**: Docker Compose生产编排
  - 优化docker-compose.prod.yml
  - 配置资源限制 (CPU/内存)
  - 配置健康检查

- [ ] **Day 5-6**: 数据库初始化
  - 执行DDL脚本
  - 初始化基础数据 (用户/角色/权限)
  - 执行数据迁移

- [ ] **Day 7**: 生产环境烟雾测试
  - 启动所有服务
  - 验证服务注册到Nacos
  - 测试核心API

**验收标准**:

- ✅ 生产配置完善
- ✅ 所有服务正常启动
- ✅ 数据库初始化成功
- ✅ 核心API可访问

---

#### Week 12: 灰度发布与监控

**任务清单**:

- [ ] **Day 1-2**: 灰度发布策略
  - 配置Nginx负载均衡
  - 配置灰度发布规则
  - 测试灰度切换

- [ ] **Day 3-4**: 生产监控配置
  - 验证Prometheus采集
  - 验证Grafana Dashboard
  - 配置告警规则 (70+)
  - 配置告警通知 (邮件/钉钉)

- [ ] **Day 5-6**: 生产测试
  - 执行完整E2E测试 (生产环境)
  - 性能测试 (生产数据)
  - 安全测试

- [ ] **Day 7**: 上线准备
  - 准备上线检查清单
  - 备份数据库
  - 准备回滚方案

**验收标准**:

- ✅ 灰度发布策略就绪
- ✅ 监控告警正常工作
- ✅ 生产环境测试通过
- ✅ 上线检查清单完成

---

### Phase 7: 正式上线 (Week 13-14)

#### Week 13: 上线与稳定性观察

**Day 1 (上线日)**:

- 08:00 - 执行上线检查清单
- 09:00 - 数据库最后备份
- 10:00 - 灰度发布 (10%流量)
- 12:00 - 观察监控指标
- 14:00 - 灰度扩大 (50%流量)
- 16:00 - 全量发布 (100%流量)
- 18:00 - 上线总结

**Day 2-7**:

- 7x24小时监控
- 快速响应生产问题
- 每日监控报告
- 用户反馈收集

**验收标准**:

- ✅ 上线过程顺利
- ✅ 无Critical Bug
- ✅ 监控指标正常
- ✅ 用户反馈积极

---

#### Week 14: 稳定性优化与交付

**任务清单**:

- [ ] **Day 1-3**: 修复生产问题
  - 分析用户反馈
  - 修复非Critical Bug
  - 优化用户体验

- [ ] **Day 4-5**: 文档交付
  - 用户使用手册
  - 运维手册更新
  - API文档发布

- [ ] **Day 6-7**: 项目交付
  - 项目总结报告
  - 交付物清单
  - 培训材料准备

**验收标准**:

- ✅ 生产环境稳定
- ✅ 文档完整
- ✅ 项目交付完成

---

## 📋 MVP上线检查清单

### 基础设施 (Infrastructure)

- [ ] PostgreSQL主从复制配置 + 测试
- [ ] Redis持久化配置 + 测试
- [ ] RabbitMQ集群配置 + 测试
- [ ] Nacos生产配置 + 测试
- [ ] MinIO对象存储 + 测试

### 服务部署 (Services)

- [ ] Gateway Service部署 + 健康检查
- [ ] Auth Service部署 + 健康检查
- [ ] Book Service部署 + 健康检查
- [ ] Circulation Service部署 + 健康检查
- [ ] Reader Service部署 + 健康检查
- [ ] System Service部署 + 健康检查
- [ ] 所有服务注册到Nacos

### 前端部署 (Frontend)

- [ ] Vue生产构建 (`npm run build`)
- [ ] Nginx配置 + HTTPS
- [ ] 前端连接生产后端
- [ ] Service Worker缓存配置

### 数据库 (Database)

- [ ] 所有DDL脚本执行成功
- [ ] 基础数据初始化 (用户/角色/权限)
- [ ] 数据库备份计划配置
- [ ] 数据库主从同步验证

### 监控告警 (Monitoring)

- [ ] Prometheus采集所有服务指标
- [ ] Grafana Dashboard配置完成
- [ ] 70+告警规则配置
- [ ] 告警通知渠道测试 (邮件/钉钉)

### 安全 (Security)

- [ ] JWT密钥配置 (生产环境独立)
- [ ] 数据库密码加密存储
- [ ] HTTPS证书配置
- [ ] 防火墙规则配置
- [ ] 敏感数据脱敏

### 性能 (Performance)

- [ ] 数据库索引创建
- [ ] Redis缓存预热
- [ ] JVM参数调优
- [ ] 连接池配置优化
- [ ] 性能测试达标 (P95<500ms)

### 测试 (Testing)

- [ ] 单元测试覆盖85%+ (后端)
- [ ] E2E测试10+场景通过
- [ ] API契约测试100%通过
- [ ] 性能测试达标
- [ ] 安全测试无Critical漏洞

### 文档 (Documentation)

- [ ] OpenAPI规范完成 (7个服务)
- [ ] Swagger UI可访问
- [ ] 运维手册更新
- [ ] 用户使用手册
- [ ] 上线SOP文档

### 灰度发布 (Canary Deployment)

- [ ] 灰度发布策略定义
- [ ] Nginx负载均衡配置
- [ ] 灰度流量规则配置
- [ ] 回滚方案准备

### 备份恢复 (Backup & Recovery)

- [ ] 数据库定时备份配置
- [ ] 备份恢复测试
- [ ] 灾难恢复计划
- [ ] 回滚脚本准备

---

## 🚀 快速启动 (Week 1 立即执行)

### 今日任务 (Day 1)

1. **创建OpenAPI规范** (2小时)

   ```bash
   cd DevPlan/05_API_SPECIFICATIONS/
   vi common/schemas.yaml    # 通用数据模型
   vi auth-api.yaml          # Auth Service API规范
   ```

2. **完成Book Service API** (6小时)
   - 实现图书分类API (3个)
   - 实现库存管理API (2个)
   - 单元测试编写

### 本周目标

- ✅ OpenAPI规范完成 (7个服务)
- ✅ Book Service完善 (30% → 100%)
- ✅ Book Service Docker化
- ✅ Swagger UI集成

---

## 📊 里程碑时间线

```
Week 1-4: 核心服务完成 (Book/Circulation/Reader/System)
     ↓
Week 5-6: 前后端集成 + E2E测试
     ↓
Week 7: API契约测试 (128个端点)
     ↓
Week 8-9: 性能测试与优化
     ↓
Week 10: 安全加固 + HTTPS配置
     ↓
Week 11-12: 生产环境准备 + 监控配置
     ↓
Week 13: 正式上线 + 灰度发布
     ↓
Week 14: 稳定性优化 + 项目交付
```

---

**创建人**: Claude Code Agent
**创建日期**: 2025-11-03
**下一步**: 创建API规范模板 (`05_API_SPECIFICATIONS/README.md`)
