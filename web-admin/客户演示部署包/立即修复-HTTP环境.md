# 🚨 立即修复 - HTTP 环境（不使用 HTTPS）

## 📋 问题现状

- **访问地址**: `http://192.168.2.121:3011`
- **问题**: 登录显示"请求失败"
- **要求**: 必须使用 HTTP（不用 HTTPS）

---

## ✅ 一键修复（30秒）

### 执行命令

```bash
# 1. 进入部署包目录
cd /path/to/客户演示部署包

# 2. 运行修复脚本
./部署脚本/quick-fix-http.sh
```

脚本会自动：
- ✅ 停止旧容器
- ✅ 导入最新镜像（如需要）
- ✅ 启动使用 nginx Mock API 的版本
- ✅ 验证部署成功

---

## 📖 手动部署（如果脚本失败）

### 步骤 1: 停止旧容器

```bash
cd /path/to/客户演示部署包
docker stop gcrf-web-admin 2>/dev/null
docker rm gcrf-web-admin 2>/dev/null
```

### 步骤 2: 导入最新镜像

```bash
# 如果镜像不存在或需要更新
gunzip -c gcrf-web-admin-amd64-v1.0.0.tar.gz | docker load
```

### 步骤 3: 启动容器（使用 nginx Mock API）

```bash
# 重要：使用 -v 参数挂载 nginx-mock-api.conf
docker run -d \
  --name gcrf-web-admin \
  -p 3011:80 \
  -v "$(pwd)/nginx-mock-api.conf:/etc/nginx/conf.d/default.conf:ro" \
  --restart unless-stopped \
  gcrf-library-web-admin:amd64
```

### 步骤 4: 验证部署

```bash
# 检查容器状态
docker ps | grep gcrf-web-admin

# 测试健康检查
curl http://localhost:3011/health
# 应返回: OK

# 测试登录接口
curl -X POST http://localhost:3011/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
# 应返回: {"code":200,"message":"登录成功",...}
```

---

## 🌐 访问系统

### 本地访问
```
http://localhost:3011
```

### 远程访问
```
http://192.168.2.121:3011
```

（将 IP 替换为实际服务器 IP）

### 登录账号
```
用户名: admin
密码: admin123
```

---

## 🔍 问题排查

### 问题 1: 容器启动失败

**检查日志**:
```bash
docker logs gcrf-web-admin
```

**常见原因**:
- 端口 3011 被占用
- nginx 配置文件路径错误

**解决方法**:
```bash
# 检查端口占用
lsof -i :3011

# 检查配置文件是否存在
ls -la nginx-mock-api.conf
```

### 问题 2: 登录仍然失败

**检查请求路径**:

打开浏览器控制台 (F12) → Network 标签，查看登录请求：

```
✅ 正确: /api/v1/auth/login POST [200 OK]
❌ 错误: /api/api/v1/auth/login POST [405]
```

如果仍然看到 `/api/api/...` 路径，说明：
1. 容器使用的是旧镜像
2. 需要重新导入最新镜像

**解决方法**:
```bash
# 删除旧镜像
docker rmi gcrf-library-web-admin:amd64

# 重新导入
gunzip -c gcrf-web-admin-amd64-v1.0.0.tar.gz | docker load

# 重启容器
docker stop gcrf-web-admin && docker rm gcrf-web-admin
# 然后执行步骤3
```

### 问题 3: 浏览器显示空白

**清除缓存**:
1. 按 Ctrl+Shift+Delete
2. 选择"缓存图像和文件"
3. 点击"清除数据"
4. 刷新页面 (F5)

---

## ⚠️ nginx Mock API 限制

本方案使用 nginx 直接返回 Mock 数据，有以下限制：

### 支持的功能 ✅
- ✅ 用户登录
- ✅ 获取用户信息
- ✅ 基础数据展示

### 不支持的功能 ⚠️
- ⚠️ 数据分页（返回固定数据）
- ⚠️ 数据搜索（返回固定数据）
- ⚠️ 数据编辑（返回成功但不保存）
- ⚠️ 复杂业务逻辑

如需完整功能，请使用以下方案：

---

## 💡 完整功能方案

### 方案 A：localhost 访问（推荐）

**在服务器本地访问**:
```bash
# 使用标准部署（无需 nginx Mock API）
docker run -d -p 3011:80 --name gcrf-web-admin gcrf-library-web-admin:amd64

# 在服务器上打开浏览器
firefox http://localhost:3011
```

**优点**:
- ✅ 完整的 Mock Service Worker 功能
- ✅ 所有业务逻辑完整
- ✅ 100+ 图书, 200+ 借阅记录
- ✅ 使用 HTTP（无需 HTTPS）

---

### 方案 B：SSH 端口转发（远程访问）

**客户在本地电脑执行**:
```bash
# 建立 SSH 转发
ssh -L 3011:localhost:3011 user@192.168.2.121

# 保持连接，打开浏览器
http://localhost:3011
```

**优点**:
- ✅ 远程访问
- ✅ 完整功能
- ✅ 使用 HTTP（无需 HTTPS）
- ✅ 安全连接（SSH 加密）

详见：`HTTP环境解决方案.md`

---

### 方案 C：HTTPS 部署

**如果必须通过 IP 访问并需要完整功能**:

```bash
./部署脚本/generate-ssl-cert.sh
# 输入: 192.168.2.121

docker-compose -f docker-compose-https.yml up -d
```

访问：`https://192.168.2.121:3011`

详见：`HTTPS部署指南.md`

---

## 📊 方案对比

| 方案 | HTTP | 远程访问 | 完整功能 | 复杂度 |
|------|------|----------|----------|--------|
| **nginx Mock API** | ✅ | ✅ | ⚠️ 有限 | ⭐ |
| **localhost** | ✅ | ❌ | ✅ | ⭐ |
| **SSH 转发** | ✅ | ✅ | ✅ | ⭐⭐ |
| **HTTPS** | ❌ | ✅ | ✅ | ⭐⭐ |

---

## 🎯 快速决策

### 如果只需要演示登录功能
→ 使用 **nginx Mock API** (本方案)

### 如果需要完整的业务功能演示
→ 使用 **localhost** 或 **SSH 转发**

### 如果客户坚持要通过 IP 访问且需要完整功能
→ 使用 **HTTPS 部署**

---

## 📞 获取帮助

### 文档资源
- `HTTP环境解决方案.md` - HTTP 环境完整方案
- `HTTPS部署指南.md` - HTTPS 配置指南
- `紧急修复说明_v1.0.2.md` - 问题诊断详情

### 技术支持
- 📧 support@gcrf-library.com
- 📞 400-xxx-xxxx

---

## ✅ 验证成功

部署成功后，您应该能够：

- [x] 访问 http://192.168.2.121:3011
- [x] 看到登录页面
- [x] 使用 admin/admin123 登录
- [x] 进入系统仪表盘
- [x] 看到基础数据展示

如果遇到问题，请查看**问题排查**部分。

---

**© 2025 国创睿峰科技有限公司**

**版本**: v1.0
**更新日期**: 2025-10-21
**适用场景**: HTTP 环境快速修复
