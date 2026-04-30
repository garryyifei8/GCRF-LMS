# ADR-003: 微信前端形态 = 公众号 H5 + 小程序（uni-app 单源码）

**Status**: Accepted
**Date**: 2026-04-30
**Decider**: GCRF 项目组
**Related**: [主 spec](../specs/2026-04-30-regional-platform-master-design.md) / [03-wechat-library PRD](../prd/03-wechat-library.md)

---

## Context

需求里明确"集成校园公众号" + "支持借阅到期/预约提醒/进馆预约"等通知。
学校大多有公众号但用得轻；学生家长平时用微信扫一扫、看消息；
但深度交互（在线预约 / 阅读测评 / 进馆预约）在公众号 H5 体验不佳。

## Considered Options

### A. 仅公众号 H5

- ✅ 开发量小、复用 Web 端代码
- ❌ H5 体验一般，扫码、定位、文件能力受限
- ❌ 服务通知模板限制多

### B. 仅微信小程序

- ✅ 交互最佳，能力全（蓝牙、近场预约）
- ❌ 与"集成校园公众号"诉求割裂
- ❌ 公众号粉丝引导转换成本高

### C. 公众号 + 小程序双形态（uni-app）（选定）

- 公众号承担：账号绑定入口、底部菜单、消息推送
- 小程序承担：所有交互（检索/借阅/预约/测评/进馆预约/书单）
- 公众号菜单一键跳小程序，体验流畅
- 满足"集成校园公众号" + 现代化体验
- ⚠ 双前端要维护，但 uni-app 一套代码可同时编译

## Decision

**选择 C**：公众号 + 小程序双形态，使用 **uni-app 单源码**编译。

## Rationale

- 中国 K12 现状：学校有公众号、学生用小程序，二者结合是主流模式
- uni-app 一套 Vue3 源码 + 编译宏 (`#ifdef MP-WEIXIN`) 输出公众号 H5 + 微信小程序
- 后续需要 App 端时可直接编译 iOS/Android 原生（不浪费投入）
- 微信开放平台审核：公众号容易过；小程序申请教育类目（学校名义提交）

## Consequences

### Positive

- 一套代码两个端，维护成本最低
- 公众号承担消息触达 + 入口；小程序承担深度交互
- 后续可再编译 App 端

### Negative / Risks

- uni-app 部分 API 在公众号 H5 上有限制（蓝牙等仅小程序）
- 需要熟悉 uni-app 编译宏和条件编译
- 小程序审核周期较长（教育类目）

### Mitigation

- 蓝牙借书机等硬件交互**仅在小程序端**实现（条件编译）
- 公众号 H5 仅做基础借阅查询 + 消息中心（兜底）
- 提前准备小程序教育类目资料（学校公函、ICP）

## Implementation Notes

- 项目结构：`web-wechat/`（独立目录，不与 web-admin 混）
- 技术栈：Vue 3 + uni-app + Pinia + uni-ui
- 编译命令：
  - `pnpm dev:h5` → 公众号 H5（`web-wechat/dist/h5`）
  - `pnpm dev:mp-weixin` → 微信小程序（`web-wechat/dist/mp-weixin`）
- 后端：所有 `/api/v1/wx/*` 由 `wechat-service` 处理
- 微信登录：使用 OAuth2 静默授权（公众号）/ `wx.login()` (小程序)
- 消息通知：订阅消息（小程序） + 服务通知（公众号），由 `notification-service` 调度

---

**Last Updated**: 2026-04-30
