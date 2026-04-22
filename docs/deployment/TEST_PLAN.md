# GCRF 智能图书馆管理系统 — 上线测试计划

**版本：** v1.0
**环境：** K8s 测试集群（192.168.1.19:31080）
**命名空间：** gcrf-prod

---

## 测试范围

| 模块 | 测试项数 | 优先级 |
|------|---------|-------|
| T1 基础设施健康检查 | 5 | Critical |
| T2 认证模块 | 6 | Critical |
| T3 图书管理 | 8 | Critical |
| T4 读者管理 | 6 | Critical |
| T5 流通管理 | 8 | Critical |
| T6 系统管理 | 5 | High |
| T7 AI 智能 | 5 | Medium |
| T8 前端 UI | 6 | High |
| T9 安全测试 | 5 | Critical |
| T10 性能基线 | 4 | Medium |
| **总计** | **58** | |

---

## T1 基础设施健康检查

| # | 测试项 | 方法 | 预期结果 | 实际结果 | 状态 |
|---|--------|------|---------|---------|------|
| T1.1 | Web 前端可达 | `curl http://192.168.1.19:31080` | HTTP 200 | | |
| T1.2 | Pod 运行状态 | `kubectl get pods -n gcrf-prod` | ≥8/9 Running | | |
| T1.3 | PostgreSQL 连通 | `kubectl exec postgresql-0 -- psql -c "SELECT 1"` | 返回 1 | | |
| T1.4 | Redis 连通 | `kubectl exec redis -- redis-cli ping` | PONG | | |
| T1.5 | 教育平台共存 | `curl http://192.168.1.19:30080` | HTTP 200（不受影响） | | |

## T2 认证模块

| # | 测试项 | 方法 | 预期结果 |
|---|--------|------|---------|
| T2.1 | 正确密码登录 | POST `/api/v1/auth/login` body: `{username:admin, password:admin123}` | code=200, 返回 accessToken |
| T2.2 | 错误密码登录 | POST `/api/v1/auth/login` body: `{username:admin, password:wrong}` | code=5002, "用户名或密码错误" |
| T2.3 | 不存在用户 | POST `/api/v1/auth/login` body: `{username:nobody, password:123}` | code=5001, "用户不存在" |
| T2.4 | 空字段提交 | POST `/api/v1/auth/login` body: `{}` | code=400, 参数校验失败 |
| T2.5 | JWT 令牌有效性 | 登录获取 token → 用 token 访问受保护 API | 200 正常返回 |
| T2.6 | 登出 | POST `/api/v1/auth/logout` with Bearer token | code=200, 清除会话 |

## T3 图书管理

| # | 测试项 | 方法 | 预期结果 |
|---|--------|------|---------|
| T3.1 | 图书列表 | GET `/api/v1/books?pageNum=1&pageSize=10` | code=200, total≥10 |
| T3.2 | 分页 | GET `/api/v1/books?pageNum=2&pageSize=5` | code=200, 返回第2页 |
| T3.3 | 关键字搜索 | GET `/api/v1/books?keyword=Java` | code=200, 返回含"Java"的图书 |
| T3.4 | 图书详情 | GET `/api/v1/books/1` | code=200, 返回完整图书信息 |
| T3.5 | 检查可借 | GET `/api/v1/books/1/availability` | code=200, data=true |
| T3.6 | 不存在图书 | GET `/api/v1/books/99999` | code=404 或 data=null |
| T3.7 | 前端列表页 | 浏览器打开 `/books/list` | 表格显示图书数据 |
| T3.8 | 前端查看详情 | 点击"查看"按钮 | 弹窗显示 ISBN、书名、作者等 |

## T4 读者管理

| # | 测试项 | 方法 | 预期结果 |
|---|--------|------|---------|
| T4.1 | 读者列表 | GET `/api/v1/readers?pageNum=1&pageSize=10` | code=200, total=8 |
| T4.2 | 学生筛选 | GET `/api/v1/readers?readerType=student` | 仅返回学生 |
| T4.3 | 教师筛选 | GET `/api/v1/readers?readerType=teacher` | 仅返回教师 |
| T4.4 | 读者详情 | GET `/api/v1/readers/1` | code=200, 含姓名/电话/状态 |
| T4.5 | 读者状态验证 | GET `/api/v1/readers/1/validate-status` | code=200, data=true |
| T4.6 | 前端学生页 | 浏览器打开 `/readers/students` | 表格显示学生列表 |

## T5 流通管理（核心业务流程）

| # | 测试项 | 方法 | 预期结果 |
|---|--------|------|---------|
| T5.1 | 借阅列表 | GET `/api/v1/borrows?pageNum=1&pageSize=10` | code=200, 返回借阅记录 |
| T5.2 | 借书操作 | POST `/api/v1/borrows/borrow` body: `{readerId:X, bookId:Y}` | code=200, 创建借阅记录 |
| T5.3 | 借书后库存减少 | GET `/api/v1/books/Y/availability` | available_quantity 减 1 |
| T5.4 | 续借 | POST `/api/v1/borrows/renew` body: `{borrowId:X, renewDays:14}` | code=200, 应还日期延长 |
| T5.5 | 还书 | POST `/api/v1/borrows/return` body: `{borrowId:X, payFine:false}` | code=200, 状态变 RETURNED |
| T5.6 | 还书后库存恢复 | GET `/api/v1/books/Y/availability` | available_quantity 加 1 |
| T5.7 | 预约列表 | GET `/api/v1/reserves?pageNum=1&pageSize=10` | code=200 |
| T5.8 | 前端流通记录页 | 浏览器打开 `/circulation/records` | 显示借阅统计+列表 |

## T6 系统管理

| # | 测试项 | 方法 | 预期结果 |
|---|--------|------|---------|
| T6.1 | 角色列表 | GET `/api/v1/system/roles` | code=200, ≥3个角色 |
| T6.2 | 部门列表 | GET `/api/v1/system/departments` | 返回部门数据 |
| T6.3 | 菜单树 | GET `/api/v1/system/menus/tree` | code=200, 树形菜单 |
| T6.4 | 前端角色页 | 浏览器打开 `/system/roles` | 显示角色+权限树 |
| T6.5 | 前端部门页 | 浏览器打开 `/system/departments` | 显示部门列表 |

## T7 AI 智能模块

| # | 测试项 | 方法 | 预期结果 |
|---|--------|------|---------|
| T7.1 | 热门问题 | GET `/api/v1/chat/hot-questions?limit=5` | code=200, 返回问题列表 |
| T7.2 | 聊天统计 | GET `/api/v1/chat/stats` | code=200, 含 totalQuestions |
| T7.3 | 发送消息 | POST `/api/v1/chat/message` body: `{content:"如何借书?", sessionId:"test"}` | code=200, 返回回复 |
| T7.4 | 分析概览 | GET `/api/v1/analytics/overview` | code=200, 含统计数据 |
| T7.5 | 前端聊天页 | 浏览器打开 `/ai/chat` | 聊天界面 + 热门问题 |

## T8 前端 UI 测试

| # | 测试项 | 方法 | 预期结果 |
|---|--------|------|---------|
| T8.1 | 登录页渲染 | 打开 `/login` | 显示用户名/密码输入框 + "立即登录" |
| T8.2 | 登录跳转 | 输入 admin/admin123 → 点登录 | 跳转到 Dashboard |
| T8.3 | Dashboard 仪表板 | 登录后首页 | 显示统计卡片 + 图表 |
| T8.4 | 侧边栏导航 | 点击各菜单项 | 正确跳转到对应页面 |
| T8.5 | 路由守卫 | 未登录访问 `/books/list` | 重定向到 `/login` |
| T8.6 | 登出流程 | 点头像 → 退出登录 → 确认 | 跳转回 `/login` |

## T9 安全测试

| # | 测试项 | 方法 | 预期结果 |
|---|--------|------|---------|
| T9.1 | 无 token 访问 API | GET `/api/v1/books` 不带 Authorization | 返回数据（当前无网关鉴权） 或 401 |
| T9.2 | SQL 注入 | GET `/api/v1/books?keyword=' OR 1=1 --` | 不返回全部数据，无 SQL 错误 |
| T9.3 | XSS 测试 | 搜索 `<script>alert(1)</script>` | 不执行脚本，正常返回 |
| T9.4 | 密码不明文 | 查看登录响应 body | 不包含密码字段 |
| T9.5 | CORS 策略 | 跨域请求测试 | 仅允许同源请求 |

## T10 性能基线

| # | 测试项 | 方法 | 预期结果 |
|---|--------|------|---------|
| T10.1 | 首页加载时间 | `curl -w "%{time_total}" /login` | < 3 秒 |
| T10.2 | API 响应时间 | `curl -w "%{time_total}" /api/v1/books` | < 1 秒 |
| T10.3 | 登录响应时间 | `curl -w "%{time_total}" POST /api/v1/auth/login` | < 2 秒 |
| T10.4 | 并发测试 | 10 并发请求图书列表 | 全部 200，平均 < 2 秒 |

---

## 通过标准

| 级别 | 标准 |
|------|------|
| **可上线** | T1-T5 全部 PASS，T6-T10 PASS≥80% |
| **有条件上线** | T1-T5 PASS≥90%，已知问题有 workaround |
| **不可上线** | T1-T5 任何 FAIL 无 workaround |
