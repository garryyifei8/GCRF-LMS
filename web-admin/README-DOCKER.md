# Docker 部署指南

## 概述

本指南说明如何使用 Docker 部署国创睿峰智能图书馆管理系统的 Web 管理端前端应用。

## 特性

- **多阶段构建**: 使用 Node.js 构建，Nginx 运行，镜像体积小
- **Mock 数据**: 内置 Mock.js 模拟后端 API，无需真实后端即可测试
- **生产优化**: Gzip 压缩、静态资源缓存、健康检查
- **快速启动**: 使用 Docker Compose 一键启动

## 前置条件

- Docker 20.10+
- Docker Compose 2.0+

## Mock 数据说明

项目已配置 Mock.js 用于模拟后端 API，包含以下接口：

### 认证接口
- `POST /api/auth/login` - 用户登录
- `GET /api/auth/userInfo` - 获取用户信息
- `POST /api/auth/logout` - 用户登出

**测试账号**:
- 管理员: `admin` / `123456`
- 图书管理员: `librarian` / `123456`

### 仪表盘接口
- `GET /api/dashboard/stats` - 获取统计数据
- `GET /api/dashboard/borrowTrend` - 获取借阅趋势
- `GET /api/dashboard/categoryStats` - 获取图书分类统计
- `GET /api/dashboard/popularBooks` - 获取热门图书
- `GET /api/dashboard/recentBorrows` - 获取最新借阅记录

### 图书管理接口
- `GET /api/books` - 获取图书列表（支持分页、搜索）
- `GET /api/books/:id` - 获取图书详情
- `POST /api/books` - 新增图书
- `PUT /api/books/:id` - 更新图书
- `DELETE /api/books/:id` - 删除图书
- `DELETE /api/books/batch` - 批量删除图书

## 快速开始

### 方式一：使用 Docker Compose（推荐）

1. **启动服务**
   ```bash
   cd web-admin
   docker-compose up -d
   ```

2. **查看日志**
   ```bash
   docker-compose logs -f web-admin
   ```

3. **访问应用**

   打开浏览器访问: http://localhost:3000

   使用测试账号登录: `admin` / `123456`

4. **验证部署**
   ```bash
   # 健康检查
   curl http://localhost:3000/health

   # 应该返回: healthy
   ```

5. **停止服务**
   ```bash
   docker-compose down
   ```

### 方式二：使用 Docker 命令

1. **构建镜像**
   ```bash
   cd web-admin
   docker build -t gcrf-library-web-admin:latest .
   ```

2. **运行容器**
   ```bash
   docker run -d \
     --name gcrf-library-web-admin \
     -p 3000:80 \
     gcrf-library-web-admin:latest
   ```

3. **查看日志**
   ```bash
   docker logs -f gcrf-library-web-admin
   ```

4. **停止容器**
   ```bash
   docker stop gcrf-library-web-admin
   docker rm gcrf-library-web-admin
   ```

## 健康检查

容器提供健康检查端点：

```bash
curl http://localhost:3000/health
```

返回 `healthy` 表示服务正常运行。

## 目录结构

```
web-admin/
├── Dockerfile              # Docker 镜像构建文件
├── docker-compose.yml      # Docker Compose 配置
├── nginx.conf             # Nginx 配置文件
├── .dockerignore          # Docker 构建忽略文件
├── mock/                  # Mock 数据目录
│   ├── index.js          # Mock 配置入口
│   ├── user.js           # 用户相关 Mock
│   ├── dashboard.js      # 仪表盘 Mock
│   └── book.js           # 图书管理 Mock
└── src/                   # 源代码
```

## 配置说明

### 端口配置

默认端口: `3000`

修改端口（docker-compose.yml）:
```yaml
ports:
  - "8080:80"  # 改为 8080 端口
```

### Nginx 配置

主要配置项（nginx.conf）:
- **Gzip 压缩**: 优化传输性能
- **静态资源缓存**: 30 天缓存
- **SPA 路由**: 支持 Vue Router 的 history 模式
- **API 代理**: 可配置后端 API 代理

### 环境变量

可在 docker-compose.yml 中配置环境变量:

```yaml
environment:
  - NODE_ENV=production
  - API_BASE_URL=http://your-api-server
```

## 开发模式

如需在开发模式下运行（带 Mock 数据）:

```bash
# 本地开发
npm install
npm run dev
```

访问: http://localhost:3000

## 生产部署建议

### 1. 使用具体版本标签

```bash
docker build -t gcrf-library-web-admin:1.0.0 .
```

### 2. 配置反向代理

在生产环境中，建议在前面配置 Nginx 或其他反向代理：

```nginx
server {
    listen 80;
    server_name library.gcrf.com;

    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 3. HTTPS 配置

使用 Let's Encrypt 配置 SSL 证书：

```bash
certbot --nginx -d library.gcrf.com
```

### 4. 资源限制

在 docker-compose.yml 中限制资源使用：

```yaml
deploy:
  resources:
    limits:
      cpus: '0.5'
      memory: 512M
    reservations:
      cpus: '0.25'
      memory: 256M
```

## 故障排查

### 1. 容器无法启动

检查日志:
```bash
docker logs gcrf-library-web-admin
```

### 2. 端口被占用

```bash
# 检查端口占用
lsof -i :3000

# 修改 docker-compose.yml 中的端口
```

### 3. 构建失败

清理 Docker 缓存:
```bash
docker builder prune
docker build --no-cache -t gcrf-library-web-admin:latest .
```

### 4. 网络问题

如果 npm install 失败，已配置国内镜像源，或手动指定:

```dockerfile
RUN npm install --registry=https://registry.npmmirror.com
```

## 性能优化

1. **启用 Gzip**: 已在 nginx.conf 中配置
2. **静态资源缓存**: 30 天缓存策略
3. **代码分割**: Vite 自动进行代码分割
4. **镜像分层**: 多阶段构建减小镜像体积

## 监控

### 查看资源使用

```bash
docker stats gcrf-library-web-admin
```

### 查看容器信息

```bash
docker inspect gcrf-library-web-admin
```

## 更新部署

```bash
# 1. 停止并删除旧容器
docker-compose down

# 2. 重新构建镜像
docker-compose build --no-cache

# 3. 启动新容器
docker-compose up -d
```

## 备份与恢复

### 导出镜像

```bash
docker save gcrf-library-web-admin:latest -o web-admin.tar
```

### 导入镜像

```bash
docker load -i web-admin.tar
```

## 已修复的问题

### 1. ✅ Logo 图片缺失
- **问题**: 引用了不存在的 `/logo.svg` 文件导致构建失败
- **修复**: 将 logo 替换为文本 "GCRF"
- **影响文件**: `src/views/login/index.vue`, `src/layouts/MainLayout.vue`

### 2. ✅ JavaScript 初始化错误
- **问题**: `Cannot access 'Fl' before initialization`
- **原因**: Element Plus 被重复导入（全局导入 + 自动导入）
- **修复**: 移除自动导入配置，仅保留 main.js 中的全局导入
- **修改文件**: `vite.config.js`

### 3. ✅ Terser 依赖缺失
- **问题**: Vite 3+ 需要手动安装 terser 进行代码压缩
- **修复**: 改用 esbuild 压缩（Vite 内置，更快）
- **修改文件**: `vite.config.js` (minify: 'esbuild')

### 4. ✅ Nginx 代理配置错误
- **问题**: 代理到不存在的 `library-gateway` 主机导致容器重启
- **修复**: 注释掉 API 代理配置（使用 Mock 数据时不需要）
- **修改文件**: `nginx.conf`

## 常见问题

**Q: Mock 数据在生产环境会生效吗？**

A: Mock 插件仅在开发模式（`vite serve`）下启用，生产构建不包含 Mock 代码。当前 Docker 部署使用的是生产构建，所有数据都是打包时的静态 Mock 数据。

**Q: 为什么出现 "Cannot access 'Fl' before initialization" 错误？**

A: 这个问题已经修复。原因是 Element Plus 被导入了两次（自动导入 + 手动导入）。现在只使用手动导入，问题已解决。

**Q: 如何连接真实后端 API？**

A: 编辑 `nginx.conf`，取消注释第 66-75 行的 API 代理配置，修改 `proxy_pass` 地址为真实后端地址，然后重新构建镜像。

**Q: 如何自定义 Mock 数据？**

A: 编辑 `mock/` 目录下的 JS 文件，添加或修改接口和数据。注意：仅在开发模式有效。

## 技术栈

- **构建工具**: Vite 5.0
- **前端框架**: Vue 3.4
- **UI 组件库**: Element Plus 2.5
- **Mock 工具**: Mock.js + vite-plugin-mock
- **Web 服务器**: Nginx 1.25
- **容器化**: Docker + Docker Compose

## 支持

如有问题，请联系:
- 公司: 国创睿峰科技有限公司
- 邮箱: support@gcrf.com
- 网站: https://www.gcrf.com

## 许可证

Copyright © 2025 国创睿峰科技有限公司. All rights reserved.
