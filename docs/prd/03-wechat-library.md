# 区域中小学图书馆微信图书馆 PRD

**日期：** 2026-04-30
**状态：** Approved
**主用户：** 学生 / 家长（教师可选）
**主 spec：** [`docs/specs/2026-04-30-regional-platform-master-design.md`](../specs/2026-04-30-regional-platform-master-design.md)

---

## 1. 形态

- **微信公众号** (订阅号或服务号)：消息推送 + 底部菜单 + 一键跳小程序
- **微信小程序**：所有交互（独立教育类目，学校名义提交）
- **技术**：uni-app 单源码（详见 [ADR-003](../adr/ADR-003-wechat-frontend-uniapp.md)）

## 2. 角色与权限

| 角色          | 权限                                            |
| ------------- | ----------------------------------------------- |
| 学生          | 借阅查询 / 预约 / 续借 / 测评 / 进馆预约 / 通知 |
| 家长 (PARENT) | 查看子女阅读情况 + 收到子女到期/预约通知        |
| 教师          | 同学生 + 班级阅读概览                           |

## 3. 功能模块（来自需求 1-10）

### 3.1 账号绑定

**功能**：

- 微信扫码登录 → 与读者证号绑定
- 一卡通号绑定（通过学校 SSO）
- 家长绑定子女（输入子女姓名 + 学校 + 年级 + 验证码）
- 解绑

**关键 API**：

- `POST /api/v1/wx/oauth/login`（公众号 OAuth 静默 / 小程序 wx.login）
- `POST /api/v1/wx/bind/cardNumber`
- `POST /api/v1/wx/bind/parent-child`
- `DELETE /api/v1/wx/bind`

### 3.2 图书检索 + 借阅查询

**功能**：

- 检索（同 OPAC 但限本校 + 已绑定身份）
- 我的借阅（在借 / 历史）
- 续借（命中续借规则）

**关键 API**：

- `GET /api/v1/wx/search?q=`
- `GET /api/v1/wx/my/borrows?status=active|all`
- `POST /api/v1/wx/renew/{borrowId}`

### 3.3 在线预约

**功能**：

- 选定图书 + 选取库位 → 提交预约
- 我的预约（待履约 / 已取书 / 已取消）
- 预约提醒（图书到货 / 取书截止前 1 天）

**关键 API**：

- `POST /api/v1/wx/reservations`
- `GET /api/v1/wx/my/reservations`
- `DELETE /api/v1/wx/reservations/{id}`

### 3.4 阅读情况报告

**功能**：

- 学生本人或家长视角
- 时间窗口：本月 / 本学期 / 本学年
- 数据：借阅量 / 阅读分类分布 / 阅读量同年级排名

**关键 API**：

- `GET /api/v1/wx/my/reading-report?range=`
- `GET /api/v1/wx/children/{studentId}/reading-report?range=`（家长）

### 3.5 书单推荐

**功能**：

- 平台书单（区域推荐 / 学段推荐 / 节日主题）
- 书单详情：书目列表 + 导读资源（图文 / 音视频）

**关键 API**：

- `GET /api/v1/wx/booklists?type=&grade=`
- `GET /api/v1/wx/booklists/{id}`
- `GET /api/v1/wx/booklists/{id}/guide`（导读资源）

### 3.6 阅读能力测评

**功能**：

- 在线题库测评（按年级匹配题目）
- 提交答卷 → 自动评分 + 能力分析
- 历史测评成绩

**关键 API**：

- `GET /api/v1/wx/reading-tests?grade=`
- `GET /api/v1/wx/reading-tests/{id}/questions`
- `POST /api/v1/wx/reading-tests/{id}/submit`
- `GET /api/v1/wx/my/test-results`

### 3.7 进馆预约 ⭐

**功能**：

- 查看本校图书馆开放时段
- 预约时段（带容量限制）
- 我的进馆预约（含二维码核销）

**关键 API**：

- `GET /api/v1/wx/visit-slots?school=&date=`
- `POST /api/v1/wx/visit-reservation`
- `GET /api/v1/wx/my/visit-reservations`

### 3.8 通知中心

**功能**：

- **订阅消息**（小程序）：到期 / 预约领书 / 剔旧消息 / 测评结果
- **服务通知**（公众号）：同上，模板消息
- 公告栏（学校 + 区域级公告）

**关键 API**：

- `POST /api/v1/wx/subscribe-message/template-id`（前端拿模板 id）
- `POST /api/v1/wx/subscribe-message/send`（云端调用，定时推送）
- `GET /api/v1/wx/notifications`
- `POST /api/v1/wx/notifications/{id}/read`

### 3.9 校园公众号集成

**功能**：

- 公众号底部菜单：图书检索 / 我的借阅 / 进馆预约 / 帮助
- 关注公众号自动绑定（OAuth 静默）
- 一键跳小程序（菜单或文章中嵌入小程序卡片）

### 3.10 群体消息推送

**功能**（云端管理员触发）：

- 全校 / 指定年级 / 指定班级 / 指定个人
- 推送内容：通用消息 / 书单推荐 / 借阅提醒
- 推送日志

**关键 API**：

- `POST /api/v1/wx/push/group`（管理员）
- `GET /api/v1/wx/push/logs`

## 4. 关键流程

```
学生借阅查询 (小程序)：
  打开小程序 → wx.login() → 后端换 token
    → GET /api/v1/wx/my/borrows → 列表展示
    → 点续借 → POST /api/v1/wx/renew/{id}
    → 后端调内部 circulation-service.renew()
    → 成功 → 推送订阅消息（新到期日）

家长查看子女阅读：
  公众号底部菜单 → OAuth 静默授权
    → 后端识别 user.identity = PARENT
    → GET /wx/children/list
    → 选子女 → GET /wx/children/{id}/reading-report
```

## 5. 非功能要求

- 启动到首屏 < 2 秒
- 主要操作（检索、借阅查询）< 500ms
- 离线展示已缓存数据（兜底）
- 适配 iOS 13+ / Android 8+

## 6. 技术栈

- 前端：uni-app + Vue 3 + Pinia + uni-ui
- 后端：`wechat-service` (8093) + 内部调 `circulation-service` / `reader-service` / `analytics-service`
- 推送：`notification-service` (8086) 调用微信开放平台

## 7. 验收标准

- [ ] 公众号 + 小程序双端编译通过
- [ ] 微信 OAuth 静默登录 + 读者证绑定流程
- [ ] 借阅查询 / 预约 / 续借 闭环
- [ ] 家长账号查阅子女阅读报告
- [ ] 订阅消息 + 服务通知到达率 > 95%
- [ ] 进馆预约 + 二维码核销

## 8. 关联文档

- ADR-003 微信前端：[../adr/ADR-003-wechat-frontend-uniapp.md](../adr/ADR-003-wechat-frontend-uniapp.md)
- 通知架构：详见 `notification-service` 在 [`../architecture/00-overview.md`](../architecture/00-overview.md) 的角色
