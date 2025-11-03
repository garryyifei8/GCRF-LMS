# Mock API 演示说明

## 📝 什么是 Mock API？

本演示系统使用 **Mock Service Worker (MSW)** 技术，在浏览器端完全模拟后端 API，实现**零依赖的离线演示**。

### 技术特点

- ✅ **浏览器端拦截** - 使用 Service Worker 在浏览器层面拦截 HTTP 请求
- ✅ **真实 HTTP 交互** - 前端代码无需修改，使用真实的 fetch/axios 调用
- ✅ **完整业务逻辑** - 模拟登录验证、权限控制、数据增删改查
- ✅ **持久化存储** - 在浏览器会话期间保持数据状态
- ✅ **零后端依赖** - 无需数据库、无需后端服务

---

## 🎭 模拟数据范围

### 1. 用户认证模块
- ✅ 用户登录/登出
- ✅ Token 验证
- ✅ 用户信息获取
- ✅ 密码修改

**测试账号**:
```
管理员: admin / admin123
馆员: librarian / lib123
操作员: operator / op123
```

### 2. 图书管理模块
- ✅ 图书目录浏览（分页、搜索、筛选）
- ✅ 图书详情查看
- ✅ 图书新增/编辑/删除
- ✅ 图书库存管理
- ✅ 图书分类管理

**模拟数据**: 100+ 本图书，涵盖文学、科技、历史等多个分类

### 3. 读者管理模块
- ✅ 学生读者管理
- ✅ 教师读者管理
- ✅ 读者卡管理
- ✅ 借阅历史查询

**模拟数据**: 50+ 名学生读者，20+ 名教师读者

### 4. 借阅流转模块
- ✅ 图书借阅
- ✅ 图书归还
- ✅ 借阅记录查询
- ✅ 预约管理
- ✅ 逾期处理

**模拟数据**: 200+ 条借阅记录，包含在借、已还、逾期等各种状态

### 5. 数据分析模块
- ✅ 借阅统计（日/周/月/年）
- ✅ 图书热度排行
- ✅ 读者活跃度分析
- ✅ 流转效率分析

**模拟数据**: 基于随机算法生成的真实统计图表

---

## 🚀 首次访问流程

### 正常流程
```
1. 打开 http://localhost:3011
   ↓
2. 系统检测到 Service Worker 未激活
   ↓
3. 显示"初始化中..."加载动画（1-2秒）
   ↓
4. 页面自动刷新
   ↓
5. Service Worker 激活完成
   ↓
6. 显示登录页面，可正常使用
```

### 浏览器控制台日志（正常情况）
```
[MSW] Loading 45 request handlers
[MSW] Auth handlers: 8
[MSW] Starting Mock Service Worker...
[MSW] Mock Service Worker started in production mode
[MSW] Service Worker registered but not controlling yet.
[MSW] This is the first visit. Page will reload in 1 second...
[MSW] Service Worker ready, reloading page...

--- 页面刷新后 ---

[MSW] Loading 45 request handlers
[MSW] Auth handlers: 8
[MSW] Starting Mock Service Worker...
[MSW] Mock Service Worker started in production mode
[MSW] Service Worker is already controlling the page
[App] Vue application mounted
```

---

## 🔍 验证 Mock API 工作状态

### 方法一：浏览器控制台检查
1. 按 **F12** 打开开发者工具
2. 切换到 **Console** 标签
3. 查看是否有 `[MSW]` 开头的日志
4. 确认看到 `Service Worker is already controlling the page`

### 方法二：Network 面板检查
1. 按 **F12** 打开开发者工具
2. 切换到 **Network** 标签
3. 登录系统
4. 查看 `/api/v1/auth/login` 请求
5. **正常情况**: 返回 JSON 格式数据，状态码 200
6. **异常情况**: 返回 HTML 格式（405 Not Allowed），说明 Service Worker 未激活

### 方法三：使用诊断页面
访问: **http://localhost:3011/debug.html**

此页面会自动检测：
- ✅ Service Worker 支持
- ✅ Service Worker 注册状态
- ✅ Service Worker 控制状态
- ✅ API 拦截测试

---

## ❓ 常见问题

### Q1: 首次访问页面一直显示"初始化中..."
**原因**: Service Worker 激活时间过长（通常不超过5秒）

**解决**:
1. 等待5秒
2. 如果仍未刷新，手动刷新页面（F5）
3. 清除浏览器缓存后重试（Ctrl+Shift+Delete）

### Q2: 登录后显示"网络连接失败"
**原因**: Service Worker 未正确激活

**解决**:
1. 打开浏览器控制台（F12）
2. 查看是否有 `[MSW]` 日志
3. 如果没有，说明 Service Worker 未加载
4. 清除缓存后刷新页面
5. 使用无痕模式测试

### Q3: 数据修改后刷新页面丢失
**原因**: Mock 数据仅存储在浏览器会话中

**说明**: 这是演示版本的正常行为。所有模拟数据在以下情况会重置：
- 刷新页面
- 关闭浏览器标签
- 清除浏览器缓存

**生产环境**: 真实后端会将数据持久化到数据库

### Q4: 不同浏览器标签数据不同步
**原因**: 每个标签独立运行 Service Worker

**说明**: 这是演示版本的正常行为。每个浏览器标签维护独立的模拟数据状态。

**生产环境**: 真实后端确保所有客户端数据一致

### Q5: 某些功能提示"开发中"
**原因**: 部分高级功能仅在完整版中提供

**演示功能**: 覆盖核心业务流程的 80% 功能
**完整版本**: 包含报表导出、批量操作、系统配置等高级特性

---

## 🎯 演示建议

### 推荐演示流程
1. **登录系统** - 使用 admin / admin123
2. **首页概览** - 查看仪表盘数据统计
3. **图书管理** - 浏览图书目录，查看详情
4. **读者管理** - 查看学生/教师列表
5. **借阅操作** - 执行借书/还书流程
6. **数据分析** - 查看统计图表
7. **权限演示** - 使用不同角色账号登录对比

### 演示重点
- ✨ **流畅的用户体验** - 响应式界面，操作流畅
- ✨ **完整的业务流程** - 借阅流转全链路演示
- ✨ **丰富的数据可视化** - ECharts 图表展示
- ✨ **权限控制** - 不同角色看到不同功能
- ✨ **现代化设计** - Element Plus UI 组件库

---

## 🔧 技术架构

### 前端技术栈
- **Vue 3.4** - 渐进式前端框架
- **Element Plus** - 企业级 UI 组件库
- **Vue Router 4** - 前端路由管理
- **Pinia** - 状态管理
- **ECharts** - 数据可视化
- **Axios** - HTTP 客户端

### Mock 技术栈
- **MSW 2.11** - Service Worker API 拦截
- **@faker-js/faker** - 随机数据生成
- **Mock Handlers** - 自定义业务逻辑

### 部署技术栈
- **Docker** - 容器化部署
- **Nginx 1.25** - 静态文件服务
- **Vite 5.4** - 前端构建工具

---

## 📞 技术支持

### 遇到问题？
1. 查看 **故障排查指南.md**
2. 访问诊断页面: http://localhost:3011/debug.html
3. 检查浏览器控制台日志
4. 查看 Docker 容器日志: `docker logs gcrf-web-admin`

### 联系我们
- 📧 技术支持: support@gcrf-library.com
- 📞 客服热线: 400-xxx-xxxx
- 📄 文档中心: https://docs.gcrf-library.com

---

**版本信息**:
- 演示包版本: v1.0.0
- 构建日期: 2025-10-21
- 架构: Linux/AMD64
- Mock API: MSW 2.11.5

© 2025 国创睿峰科技有限公司
