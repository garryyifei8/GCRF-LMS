# GCRF 图书馆管理系统 - HTTPS 部署指南

## 📌 为什么需要 HTTPS？

### Service Worker 安全限制

本演示系统使用 **Mock Service Worker (MSW)** 技术在浏览器端模拟后端 API。由于浏览器安全策略，Service Worker 仅在以下环境中可用：

- ✅ **HTTPS 网站** (推荐用于演示)
- ✅ **localhost** (`http://localhost:3011`)
- ❌ **HTTP + IP地址** (`http://192.168.x.x:3011`) ⚠️ 不支持

### 症状表现

如果您在 `http://192.168.x.x:3011` 访问系统时遇到以下问题：

- ❌ 登录时提示"网络连接失败"
- ❌ 浏览器控制台无 `[MSW]` 相关日志
- ❌ API 请求返回 404 或网络错误

**原因**: Service Worker 无法在 HTTP 非 localhost 环境中注册和运行。

---

## 🚀 解决方案一：使用 HTTPS (推荐)

### 步骤 1: 生成 SSL 证书

运行自动化脚本：

```bash
cd /path/to/客户演示部署包
./部署脚本/generate-ssl-cert.sh
```

脚本会提示您输入：
- **服务器 IP 或域名** (例如: `192.168.2.121`)

脚本将自动：
1. 生成自签名 SSL 证书（私钥 + 证书）
2. 创建 `ssl/` 目录存放证书
3. 生成 `docker-compose-https.yml` 配置
4. 生成 `nginx-https.conf` 配置

### 步骤 2: 启动 HTTPS 服务

```bash
# 确保已导入镜像
gunzip -c gcrf-web-admin-amd64-v1.0.0.tar.gz | docker load

# 启动 HTTPS 服务
docker-compose -f docker-compose-https.yml up -d
```

### 步骤 3: 访问系统

浏览器打开: **https://192.168.2.121:3011**

**首次访问提示**:
浏览器会显示安全警告（因为是自签名证书），这是正常现象。

**解决方法**:
- **Chrome**: 点击 "高级" → "继续访问 192.168.2.121(不安全)"
- **Firefox**: 点击 "高级" → "接受风险并继续"
- **Edge**: 点击 "高级" → "继续前往 192.168.2.121(不安全)"

### 步骤 4: 验证部署

1. 打开浏览器控制台 (F12)
2. 刷新页面
3. 应该看到 `[MSW] Mocking enabled` 日志
4. 登录测试 (admin / admin123)

---

## 🔧 解决方案二：使用 localhost 访问

如果您无法使用 HTTPS，可以通过 localhost 访问：

### 方式 A: 在服务器本地访问

如果您可以登录到服务器：

```bash
# 启动服务 (HTTP 即可)
docker run -d --name gcrf-web-admin -p 3011:80 gcrf-library-web-admin:amd64

# 在服务器上打开浏览器
firefox http://localhost:3011
```

### 方式 B: SSH 端口转发 (推荐)

从您的本地电脑通过 SSH 转发访问：

```bash
# 在本地电脑执行
ssh -L 3011:localhost:3011 user@192.168.2.121

# 然后在本地浏览器打开
http://localhost:3011
```

**优点**:
- ✅ 无需 HTTPS
- ✅ 无需证书
- ✅ Service Worker 可正常工作

---

## 📋 方案对比

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **HTTPS** | 任何设备可访问<br/>真实演示环境 | 需要证书<br/>浏览器警告 | 客户演示<br/>多人访问 |
| **Localhost** | 无需证书<br/>无安全警告 | 仅服务器本地<br/>或需SSH | 单人测试<br/>开发调试 |
| **SSH转发** | 无需证书<br/>安全连接 | 需要SSH权限 | 远程访问<br/>开发测试 |

---

## 🔍 故障排查

### 问题 1: HTTPS 访问后仍无法登录

**检查步骤**:

1. 确认使用 HTTPS 协议:
   ```
   ✅ https://192.168.2.121:3011
   ❌ http://192.168.2.121:3011
   ```

2. 查看浏览器控制台 (F12):
   ```javascript
   // 应该看到
   [MSW] Mocking enabled

   // 如果没有,执行:
   navigator.serviceWorker.getRegistrations()
   ```

3. 清除浏览器缓存:
   - Chrome: Ctrl+Shift+Delete → 清除缓存
   - 或使用无痕模式测试

### 问题 2: 证书生成失败

**检查**:
```bash
# 确认 openssl 已安装
which openssl

# Mac 安装
brew install openssl

# Ubuntu 安装
sudo apt-get install openssl
```

### 问题 3: 端口 3011 被占用

**修改端口**:

编辑 `docker-compose-https.yml`:
```yaml
ports:
  - "8443:443"  # 改为其他端口
```

然后访问: `https://192.168.2.121:8443`

### 问题 4: 容器无法启动

**检查日志**:
```bash
# 查看容器状态
docker ps -a | grep gcrf-web-admin

# 查看详细日志
docker logs gcrf-web-admin-https

# 检查证书文件
ls -la ssl/
```

---

## 📝 完整部署示例

### 场景：客户演示环境 (IP: 192.168.2.121)

```bash
# 1. 进入部署包目录
cd /path/to/客户演示部署包

# 2. 导入镜像
gunzip -c gcrf-web-admin-amd64-v1.0.0.tar.gz | docker load

# 3. 生成 SSL 证书
./部署脚本/generate-ssl-cert.sh
# 输入: 192.168.2.121
# 确认: y

# 4. 启动 HTTPS 服务
docker-compose -f docker-compose-https.yml up -d

# 5. 验证部署
curl -k https://192.168.2.121:3011/health
# 应返回: OK

# 6. 查看日志
docker logs -f gcrf-web-admin-https
```

### 访问系统

1. 浏览器打开: `https://192.168.2.121:3011`
2. 接受安全警告
3. 等待 1-2 秒初始化
4. 页面自动刷新
5. 登录: `admin` / `admin123`

---

## 🎯 验证清单

部署 HTTPS 后请验证：

- [ ] 浏览器地址栏显示 `https://`
- [ ] 访问后自动显示"初始化中..."
- [ ] 1-2秒后页面自动刷新
- [ ] F12 控制台显示 `[MSW] Mocking enabled`
- [ ] 可以使用 admin/admin123 登录
- [ ] 登录后可以看到仪表盘数据
- [ ] 图书列表可以正常加载

---

## 📞 技术支持

### 常见问题

1. **Q**: 自签名证书安全吗？
   **A**: 仅用于演示环境，不建议生产使用。生产环境请使用 CA 签发的证书。

2. **Q**: 可以使用 Let's Encrypt 免费证书吗？
   **A**: 可以，但需要公网域名。内网 IP 只能使用自签名证书。

3. **Q**: 如何信任自签名证书避免警告？
   **A**: 可以将证书导入系统信任列表，但演示环境直接接受警告更方便。

### 联系我们

- 📧 技术支持: support@gcrf-library.com
- 📞 客服热线: 400-xxx-xxxx
- 🌐 在线文档: https://docs.gcrf-library.com

---

## 📚 相关文档

- `README.md` - 快速开始指南
- `Mock_API_演示说明.md` - Mock API 技术说明
- `故障排查指南.md` - 详细故障排查
- `部署包使用指南.md` - 完整使用手册

---

**© 2025 国创睿峰科技有限公司**
**版本**: v1.0.0
**更新日期**: 2025-10-21
