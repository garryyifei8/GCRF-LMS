# 新人上手指南

> 本指南帮助新开发者快速搭建本地开发环境。

## 前提条件

| 工具       | 版本   | 用途              |
| ---------- | ------ | ----------------- |
| Java       | 21     | 后端微服务        |
| Maven      | 3.9+   | Java 构建         |
| Node.js    | 20+    | 前端构建          |
| npm        | 10+    | 前端包管理        |
| Docker     | 20.10+ | 基础设施服务      |
| PostgreSQL | 15+    | 数据库            |
| Redis      | 7.x    | 缓存              |
| Nacos      | 2.3.x  | 服务注册/配置中心 |

## 快速启动

### 1. 克隆项目

```bash
git clone <repository-url>
cd GCRF_LibraryManagementSystem
```

### 2. 安装工程化工具

```bash
npm install  # 根目录：安装 husky, commitlint, lint-staged
```

### 3. 启动基础设施

```bash
cd deployment
docker-compose -f docker-compose.infrastructure.yml up -d
```

等待 PostgreSQL 和 Nacos 完全启动（约 30 秒）。

### 4. 构建后端

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd backend
mvn clean compile
```

### 5. 启动前端

```bash
cd web-admin
npm install
npm run dev  # http://localhost:3011
```

## 常用命令

| 命令                          | 位置         | 用途               |
| ----------------------------- | ------------ | ------------------ |
| `mvn clean compile`           | `backend/`   | 编译后端           |
| `mvn test -pl reader-service` | `backend/`   | 测试单个服务       |
| `npm run dev`                 | `web-admin/` | 启动前端开发服务器 |
| `npm run lint`                | `web-admin/` | 前端代码检查       |
| `npm run test`                | `web-admin/` | 前端单元测试       |
| `npm run build`               | `web-admin/` | 前端生产构建       |

## 提交规范

本项目使用 Conventional Commits，提交时会自动校验：

```
<type>(<scope>): <subject>

# 示例
feat(book): add search by ISBN
fix(web-admin): fix pagination display
docs(docs): update API documentation
```

**可用 scope:** gateway, auth, book, circulation, reader, system, notification, recommend, chat, analytics, common, web-admin, infra, docs

## 更多文档

- [架构文档](../architecture/architect.md) — 权威技术规格（1570 行）
- [ADR 记录](../adr/README.md) — 架构决策记录
