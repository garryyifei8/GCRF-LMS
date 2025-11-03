# HTTP 环境下的解决方案

## 🎯 问题说明

**客户要求**：使用 HTTP 访问（不使用 HTTPS）

**技术限制**：Service Worker 在 `http://IP地址` 环境下无法工作

**浏览器安全策略**：
- ✅ **HTTPS** - Service Worker 支持
- ✅ **http://localhost** - Service Worker 支持
- ❌ **http://192.168.2.121** - Service Worker **不支持**（浏览器强制限制）

---

## ✅ 推荐方案：使用 localhost 访问（HTTP，无需证书）

### 方案 A：服务器本地浏览器访问

**适用场景**：可以在服务器上打开浏览器

```bash
# 1. 部署最新镜像
cd /path/to/客户演示部署包
gunzip -c gcrf-web-admin-amd64-v1.0.0.tar.gz | docker load

# 2. 启动 HTTP 服务（标准端口 80）
docker run -d \
  --name gcrf-web-admin \
  -p 3011:80 \
  --restart unless-stopped \
  gcrf-library-web-admin:amd64

# 3. 在服务器上打开浏览器
firefox http://localhost:3011
# 或
google-chrome http://localhost:3011
```

**优点**：
- ✅ 使用 HTTP（无需 HTTPS 证书）
- ✅ Service Worker 正常工作
- ✅ Mock API 完全可用
- ✅ 无浏览器安全警告

---

### 方案 B：SSH 端口转发（推荐给远程用户）

**适用场景**：用户不在服务器本地，需要远程访问

#### 步骤 1：服务器部署（HTTP）

```bash
# 在服务器 192.168.2.121 上执行
cd /path/to/客户演示部署包
gunzip -c gcrf-web-admin-amd64-v1.0.0.tar.gz | docker load
docker run -d --name gcrf-web-admin -p 3011:80 gcrf-library-web-admin:amd64
```

#### 步骤 2：客户端 SSH 转发

**在客户的本地电脑上**执行（不是服务器）：

```bash
# Mac / Linux
ssh -L 3011:localhost:3011 user@192.168.2.121

# Windows (PowerShell)
ssh -L 3011:localhost:3011 user@192.168.2.121
```

**参数说明**：
- `-L 3011:localhost:3011` - 将本地 3011 端口转发到服务器 localhost:3011
- `user@192.168.2.121` - 替换为实际的服务器用户名和 IP

#### 步骤 3：访问系统

SSH 连接建立后，在**本地浏览器**打开：
```
http://localhost:3011
```

**优点**：
- ✅ 完全使用 HTTP（无需 HTTPS）
- ✅ 远程用户可以访问
- ✅ Service Worker 正常工作（因为访问 localhost）
- ✅ 数据通过 SSH 加密传输
- ✅ 无浏览器安全警告

---

### 方案 C：nginx 反向代理到 localhost（高级）

**适用场景**：需要让多个用户通过域名访问

#### 配置步骤

1. **在服务器上配置 hosts**：

```bash
# 编辑 /etc/hosts
echo "127.0.0.1 demo.gcrf-library.local" >> /etc/hosts
```

2. **nginx 配置**：

```nginx
server {
    listen 80;
    server_name demo.gcrf-library.local;

    location / {
        proxy_pass http://localhost:3011;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

3. **客户端配置 hosts**：

客户在自己电脑上添加：
```
192.168.2.121  demo.gcrf-library.local
```

4. **访问**：
```
http://demo.gcrf-library.local
```

**注意**：这个方案浏览器仍然会认为是远程 IP，Service Worker 可能无法工作。

---

## ⚠️ 不推荐方案：移除 Service Worker

如果**必须**通过 `http://192.168.2.121:3011` 访问，唯一办法是**重构代码**，移除 Service Worker 依赖。

### 需要的工作

1. **移除 MSW**，改用以下方案之一：
   - **方案 1**：nginx 提供静态 JSON Mock 数据
   - **方案 2**：使用 MirageJS（不依赖 Service Worker）
   - **方案 3**：前端内存 Mock（使用 axios-mock-adapter）

2. **工作量估算**：
   - 重构 Mock 层：4-6 小时
   - 测试验证：2-3 小时
   - 更新文档：1-2 小时
   - **总计**：7-11 小时

### 示例：使用 nginx 静态 Mock（快速方案）

创建 nginx 配置提供静态 JSON：

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;

    # 登录接口 Mock
    location = /api/v1/auth/login {
        default_type application/json;
        return 200 '{
            "code": 200,
            "message": "登录成功",
            "data": {
                "token": "mock-token-12345",
                "user": {
                    "id": 1,
                    "username": "admin",
                    "realName": "系统管理员"
                }
            }
        }';
    }

    # 其他 API 端点...需要为每个接口创建配置
}
```

**缺点**：
- 需要为 45+ 个 API 端点逐一配置
- 无法处理复杂业务逻辑
- 维护成本高

---

## 🎯 推荐决策流程

### 情况 1：演示对象可以访问服务器

**推荐**：方案 A（服务器本地浏览器）

```bash
# 服务器上执行
docker run -d -p 3011:80 --name gcrf-web-admin gcrf-library-web-admin:amd64
firefox http://localhost:3011
```

### 情况 2：演示对象在远程，熟悉 SSH

**推荐**：方案 B（SSH 端口转发）

```bash
# 客户本地执行
ssh -L 3011:localhost:3011 user@192.168.2.121
# 然后访问 http://localhost:3011
```

### 情况 3：必须通过 IP 地址访问，不能用 localhost

**只有两个选择**：
1. **使用 HTTPS**（见 `HTTPS部署指南.md`）
2. **重构移除 Service Worker**（需要 7-11 小时开发时间）

---

## 🔧 快速测试 - 确认最新镜像

在进行任何方案之前，先确认是否已部署最新 v1.0.2 镜像：

```bash
# 1. 检查当前镜像
docker images | grep gcrf-library-web-admin

# 2. 删除旧容器和镜像
docker stop gcrf-web-admin 2>/dev/null
docker rm gcrf-web-admin 2>/dev/null
docker rmi gcrf-library-web-admin:amd64

# 3. 导入最新镜像
cd /path/to/客户演示部署包
gunzip -c gcrf-web-admin-amd64-v1.0.0.tar.gz | docker load

# 4. 验证镜像
docker images gcrf-library-web-admin:amd64
# 查看 CREATED 时间应该是最新的

# 5. 启动容器
docker run -d -p 3011:80 --name gcrf-web-admin gcrf-library-web-admin:amd64

# 6. 测试 localhost
curl http://localhost:3011/health
# 应返回: OK
```

---

## 📊 方案对比

| 方案 | HTTP | Service Worker | 远程访问 | 复杂度 | 推荐度 |
|------|------|----------------|----------|--------|--------|
| **localhost + 本地浏览器** | ✅ | ✅ | ❌ | ⭐ | ⭐⭐⭐⭐⭐ |
| **SSH 端口转发** | ✅ | ✅ | ✅ | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| **HTTPS** | ❌ | ✅ | ✅ | ⭐⭐ | ⭐⭐⭐⭐ |
| **重构移除 SW** | ✅ | ❌ | ✅ | ⭐⭐⭐⭐⭐ | ⭐⭐ |

---

## 💡 立即可行的方案

### 最快速（1分钟）：本地访问

```bash
# 在服务器 192.168.2.121 上执行
docker run -d -p 3011:80 --name gcrf-web-admin gcrf-library-web-admin:amd64

# 在服务器上打开浏览器
firefox http://localhost:3011

# 登录测试
用户名: admin
密码: admin123
```

### 适合远程演示（3分钟）：SSH 转发

```bash
# 客户在自己电脑上执行
ssh -L 3011:localhost:3011 user@192.168.2.121

# 保持 SSH 连接，打开浏览器
http://localhost:3011

# 登录测试
用户名: admin
密码: admin123
```

---

## ❓ 常见问题

### Q: SSH 转发会很慢吗？
A: 不会。数据量很小（静态文件 + JSON），延迟几乎感觉不到。

### Q: SSH 断开后还能访问吗？
A: 不能。需要保持 SSH 连接。可以使用 `autossh` 保持连接稳定。

### Q: 多个用户可以同时 SSH 转发吗？
A: 可以。每个用户在自己电脑上建立独立的 SSH 转发。

### Q: 有没有办法在不修改代码的情况下支持 http://IP 访问？
A: 没有。这是浏览器的安全限制，任何前端技术都无法绕过。

---

## 📞 技术支持

如需协助配置 SSH 转发或其他方案，请联系：

- 📧 技术支持: support@gcrf-library.com
- 📞 客服热线: 400-xxx-xxxx

---

**© 2025 国创睿峰科技有限公司**

**文档版本**: v1.0
**更新日期**: 2025-10-21
**适用场景**: HTTP 环境演示部署
