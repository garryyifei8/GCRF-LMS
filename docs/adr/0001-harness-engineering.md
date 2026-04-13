# ADR-0001: Harness Engineering 工程化配置

**状态:** Accepted
**日期:** 2026-04-13

## 背景

项目缺乏统一的代码格式、提交规范、Git hooks 和 CI 质量门禁。团队参考内部标准配置手册（适用于 pnpm + React + FastAPI），需适配到本项目的 Maven + Spring Boot + Vue 3 架构。

## 决策

1. **根目录统一管理** — 根目录 `package.json` 管理工程化工具（husky, commitlint, lint-staged），`web-admin/` 保留业务依赖
2. **渐进式采用** — ESLint 保持现有 v8 配置，不升级 flat config（风险高）
3. **跳过不适用模块** — Ruff (无 Python)、pytest (无 Python)、GCP CD (使用 Harbor)、knip (不适用于 Java)
4. **新增 CI workflow** — 与现有 build-and-push.yml 和 security-scan.yml 并行，专注 PR 质量门禁
5. **Dependabot 三生态** — npm + maven + github-actions

## 后果

- 所有提交消息强制 Conventional Commits 格式
- 前端文件提交前自动 lint + format
- PR 需通过 lint、test、build 四个 job 才可合并
- 依赖自动更新，减少安全风险
