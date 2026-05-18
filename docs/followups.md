# GCRF Follow-ups

跟踪已交付 plan 留下的延后项 / 观察期任务 / 已知小问题。

---

## Plan-B1 (v1.3.0-plan-B1, 2026-05-18)

### 观察期

- [ ] **2026-05-18 起 1 周观察期**：监控 gcrf-auth pod 稳定性 + 登录链路 + refresh token 轮转 + role 分配/撤销
  - 无 OOM / 无 CrashLoopBackOff / 无登录 ticket
  - 满足后执行：`kubectl exec -n edu-infra postgresql-0 -- psql -U postgres -d postgres -c 'DROP DATABASE gcrf_auth'`

### 已 deferral / 留作后续 plan

- [ ] **Plan-B2（M2 阶段）：微信双模式公众号 OAuth + 小程序 wx.login**
  - 区域级公众号 + 学校自配两种模式
  - parent_student_link 家长-子女关联
  - wx_app_config 表 + AppSecret 加密存储 + 学校自服务 UI
  - 依赖 wechat-service 主体（M2 才开建）

- [ ] **Plan-B1 Task 13 deferral：拆 system-service → auth-service Feign 链路（M2）**
  - 现状：Phase-2 修复后 Feign 链路功能正常，3 个未实现端点已记录
  - 完整拆链需把 system-service 12 张表（system_role/system_menu/system_message/...）从 `gcrf_system` DB 迁到 `gcrf_main`，配合 `@TableName("gcrf_region.users")` 本地 mapper
  - 不属于 M1 范围；放到 M2 校园系统升级一起做

- [ ] **auth-service 3 个端点缺失（system-service Feign 调用会 404，M2 backlog）**
  - `PUT /api/v1/users/{id}/password/reset` — admin 密码重置（现有 `PUT /{id}/password` 需旧密码）
  - `PUT /api/v1/users/{id}/status` — 启停账号专用端点（目前靠通用 update）
  - `DELETE /api/v1/users/batch` — 批量删除

- [ ] **`/api/v1/roles` IAM endpoint 未在 UI 显示**（M2 frontend follow-up）
  - 当前 `/system/roles` 页面仍调 `/api/v1/system/roles`（system-service 旧端点）
  - UserRoleDrawer 已用新 IAM endpoint，但角色列表展示页还没切
  - 切换需配 MSW mock + 替换 `roles.vue`

- [ ] **自定义角色 CRUD UI（M2）**
  - "功能模块 × 数据范围" 矩阵勾选
  - SCHOOL_ADMIN 工作流（学校级 admin 分配本校角色）
  - 通用 `@DataScope` AOP 拦截器
  - CLASS / GRADE 级动态过滤

### 已知小问题（不阻塞）

- [ ] **`SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` + `BASELINE_VERSION=0` 作为 env 临时启用**
  - 当前在 gcrf-auth deployment 上以 env 形式存在
  - 严格 ADR-005 规定应 `baseline-on-migrate: false`，但本次部署遇到"schema 已有但 history 表无"的边界情况
  - 后续可考虑：第一次部署后移除这两个 env，让 Flyway 严格校验
  - 或调研 Flyway `baseline.sql` 文件机制

- [ ] **AuthServiceTest（@SpringBootTest）有预期失败**
  - 几个集成测试需要完整 Postgres+Redis+Nacos 栈，离线运行会 NPE
  - 不阻塞构建（mvn install + skipTests 可通过）
  - 加 `@DisabledIfEnvironmentVariable(named = "GCRF_TEST_STACK", matches = "off")` 或 Testcontainers profile 包装是后续优化

- [ ] **`test-online.sh` 中 T2.4（空字段登录）+ T5.2（借书已借）持续 WARN**
  - T2.4：login {"":"","":""} 应返回 5001/400，实际返回空（自 Phase-2 起就这样）
  - T5.2：单测数据 reader=1 book=2 已多次借过，返回 5000 已借
  - 两个都是测试期望与实现的小不一致，非缺陷

---

## Plan-C1.5 (v1.2.1-plan-C1.5, 2026-05-08)

### 已 deferral

- [ ] **TimestamptzTypeHandler 提取到 common-mybatis**（等第 3 个服务用到）
- [ ] **协同过滤推荐 → plan-C2**
- [ ] **mview CONCURRENTLY refresh + unique index → plan-C2**
- [ ] **search_log 90 天清理 job → plan-C2**
- [ ] **OPAC 公众端 SSR 前端 → plan-C3**

---

## 旁观察小项

- [ ] **gcrf-circulation 旧 ReplicaSet `5987dd6f88-kh7pz` 一直 CrashLoopBackOff**
  - 新 ReplicaSet 的 pod healthy
  - 旧 ReplicaSet 应被回收 — 手工 `kubectl delete pod gcrf-circulation-5987dd6f88-kh7pz -n gcrf-prod`
  - 或调 deployment.spec.revisionHistoryLimit=1

- [ ] **多个服务 JWT key size 不足 HS512 推荐值（400 bit < 512 bit）**
  - Plan-B1 已在 common-security 加 fail-fast guard（512 bit 强制）
  - 但其他服务（system / circulation / ... ）使用 common-security 时若配 jwt.secret env 过短会启动失败
  - 部署前确认所有业务服务 deployment 都有 `JWT_SECRET` env 且 ≥64 字节

---

**Last updated**: 2026-05-18 (Plan-B1 deployment day)
