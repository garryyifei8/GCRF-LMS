# 🚀 快速开始指南

## 前提条件

- ✅ Docker 已安装并运行
- ✅ Docker Compose 已安装

## 一键启动

```bash
cd web-admin
docker-compose up -d
```

## 验证部署

### 方式一：使用测试脚本（推荐）

```bash
./test-deployment.sh
```

**输出示例**:
```
✓ Docker 正在运行
✓ 容器正在运行
✓ 健康检查通过
✓ HTTP 200 OK
✓ index.html 加载成功
✅ 所有测试通过！
```

### 方式二：手动验证

1. **检查容器状态**
   ```bash
   docker ps | grep gcrf-library-web-admin
   ```
   应该看到: `Up X minutes`

2. **测试健康检查**
   ```bash
   curl http://localhost:3000/health
   ```
   应该返回: `healthy`

3. **访问应用**

   打开浏览器: http://localhost:3000

4. **登录测试**
   - 管理员: `admin` / `123456`
   - 图书管理员: `librarian` / `123456`

## 常用命令

### 查看日志
```bash
# 实时查看
docker logs -f gcrf-library-web-admin

# 查看最后 50 行
docker logs --tail 50 gcrf-library-web-admin
```

### 重启服务
```bash
docker-compose restart
```

### 停止服务
```bash
docker-compose down
```

### 重新构建
```bash
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

## 功能测试清单

### ✅ 登录页面
- [ ] 访问 http://localhost:3000
- [ ] 输入账号密码
- [ ] 点击登录
- [ ] 检查是否跳转到仪表盘

### ✅ 仪表盘
- [ ] 查看统计卡片（总藏书量、读者总数等）
- [ ] 查看借阅趋势图表
- [ ] 查看图书分类统计饼图
- [ ] 查看热门图书列表
- [ ] 查看最新借阅记录

### ✅ 图书管理
- [ ] 点击"图书管理" → "图书列表"
- [ ] 查看图书列表（Mock 数据）
- [ ] 测试搜索功能
- [ ] 测试分页功能

### ✅ 界面交互
- [ ] 点击左侧菜单折叠/展开
- [ ] 切换不同菜单项
- [ ] 检查面包屑导航
- [ ] 点击用户头像下拉菜单

## 故障排查

### 问题 1: 容器无法启动
```bash
# 查看日志
docker logs gcrf-library-web-admin

# 检查端口占用
lsof -i :3000

# 重新构建
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### 问题 2: JavaScript 错误
如果浏览器控制台出现 "Cannot access 'Fl' before initialization" 错误：

**解决方案**: 查看 `FIX-INITIALIZATION-ERROR.md` 文档

这个问题已在最新版本修复，重新构建即可：
```bash
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### 问题 3: 页面空白
1. 打开浏览器开发者工具 (F12)
2. 查看 Console 标签是否有错误
3. 查看 Network 标签是否有资源加载失败
4. 检查容器日志

### 问题 4: Mock 数据不显示
**原因**: Mock 数据仅在开发模式有效，生产构建使用静态数据

**确认方法**:
1. 检查网络请求（F12 → Network）
2. API 请求应该返回 Mock 数据
3. 如果请求失败，检查 Nginx 配置

## Mock 数据说明

当前使用 Mock.js 模拟以下 API：

### 用户认证
- `POST /api/auth/login` - 登录
- `GET /api/auth/userInfo` - 获取用户信息
- `POST /api/auth/logout` - 登出

### 仪表盘
- `GET /api/dashboard/stats` - 统计数据
- `GET /api/dashboard/borrowTrend` - 借阅趋势
- `GET /api/dashboard/categoryStats` - 分类统计
- `GET /api/dashboard/popularBooks` - 热门图书
- `GET /api/dashboard/recentBorrows` - 最新借阅

### 图书管理
- `GET /api/books` - 图书列表（支持分页、搜索）
- `GET /api/books/:id` - 图书详情
- `POST /api/books` - 新增图书
- `PUT /api/books/:id` - 更新图书
- `DELETE /api/books/:id` - 删除图书

## 性能指标

### 构建产物
- `element-plus`: ~900KB (gzip: ~276KB)
- `echarts`: ~822KB (gzip: ~270KB)
- `vue-vendor`: ~116KB (gzip: ~45KB)
- `vendor`: ~335KB (gzip: ~116KB)

### 加载性能
- 首屏加载: < 2s
- Gzip 压缩: ✅ 已启用
- 静态资源缓存: ✅ 30天

## 下一步

### 开发后端
1. 参考 `/library-backend` 目录
2. 启动后端微服务
3. 配置 Nginx 代理连接真实 API

### 连接真实后端
1. 编辑 `nginx.conf`
2. 取消注释 API 代理配置（第 66-75 行）
3. 修改 `proxy_pass` 地址
4. 重新构建镜像

```nginx
location /api/ {
    proxy_pass http://your-backend-host:8080/;
    # ...其他配置
}
```

## 相关文档

- 📘 **部署成功指南**: `DEPLOYMENT-SUCCESS.md`
- 📗 **Docker 详细文档**: `README-DOCKER.md`
- 📕 **错误修复说明**: `FIX-INITIALIZATION-ERROR.md`
- 📙 **项目架构**: `/doc/architect.md`

## 技术支持

遇到问题？查看：
1. 📋 容器日志: `docker logs gcrf-library-web-admin`
2. 🌐 Nginx 日志: `./logs/nginx/`
3. 📚 文档目录: 上述相关文档
4. 🐛 Issue 追踪: (根据项目实际情况添加)

---

**最后更新**: 2025-10-10
**版本**: 1.0.0
**状态**: ✅ 已修复所有已知问题，稳定运行
