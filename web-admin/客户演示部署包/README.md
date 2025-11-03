# GCRF智能图书馆管理系统 - 客户演示部署包 v1.0.0 (AMD64)

> **🎯 演示版本说明**
> 本部署包为**离线演示版本**，使用 **Mock Service Worker (MSW)** 技术在浏览器端模拟所有后端API。
> ✨ **无需后端服务** - 所有数据和接口均在前端模拟
> ✨ **开箱即用** - 导入镜像后即可体验完整功能
> ✨ **真实体验** - 模拟数据覆盖图书管理、读者管理、借阅流转等全部核心业务
>
> ⚠️ **首次访问提示**: 打开系统后会自动初始化 Mock Service Worker（约1-2秒），页面会自动刷新一次，这是正常现象。

## ⚠️ 重要：HTTPS 访问要求

**如果您需要通过 IP 地址访问系统** (例如: `http://192.168.2.121:3011`)，必须使用 HTTPS！

### 为什么需要 HTTPS？

Mock Service Worker 使用浏览器的 Service Worker API，出于安全原因仅支持：
- ✅ **HTTPS 网站** (`https://192.168.2.121:3011`) ⭐ 推荐
- ✅ **localhost** (`http://localhost:3011`)
- ❌ **HTTP + IP地址** (`http://192.168.2.121:3011`) ⚠️ 不支持

### 快速配置 HTTPS

```bash
# 1. 生成 SSL 证书（会提示输入服务器IP）
./部署脚本/generate-ssl-cert.sh

# 2. 启动 HTTPS 服务
docker-compose -f docker-compose-https.yml up -d

# 3. 访问系统
浏览器打开: https://192.168.2.121:3011
```

**详细说明**: 请查看 `HTTPS部署指南.md` 了解完整的 HTTPS 配置步骤和故障排查。

---

## 📦 快速开始(60秒)

**✨ 本镜像为 Linux/AMD64 原生架构**，适用于Intel/AMD处理器的Linux服务器。

**⚠️ 重要提示**: 必须先执行步骤1导入镜像，否则Docker会尝试从网络拉取镜像导致失败！

### 方式A: 使用一键导入脚本(推荐⭐)

```bash
cd /path/to/客户演示部署包
./部署脚本/0-import-only.sh
```

脚本会自动导入镜像并显示下一步操作指引。

### 方式B: 手动执行

#### 1. 导入镜像
```bash
cd /path/to/客户演示部署包
gunzip -c gcrf-web-admin-amd64-v1.0.0.tar.gz | docker load
```

#### 2. 验证镜像
```bash
docker images | grep gcrf-library-web-admin
# 应该看到: gcrf-library-web-admin   amd64   ...
```

#### 3. 启动服务
```bash
docker run -d \
  --name gcrf-web-admin \
  -p 3011:80 \
  --restart unless-stopped \
  gcrf-library-web-admin:amd64
```

#### 4. 访问系统
浏览器打开: **http://localhost:3011**

**登录账号**:
- 管理员: `admin` / `admin123`
- 馆员: `librarian` / `lib123`
- 操作员: `operator` / `op123`

---

## 📁 部署包内容

```
客户演示部署包/
├── README.md                              # 本文档(快速开始)⭐
├── HTTPS部署指南.md                       # HTTPS配置说明⭐⭐ (IP访问必看)
├── Mock_API_演示说明.md                   # Mock API 技术说明⭐⭐
├── gcrf-web-admin-amd64-v1.0.0.tar.gz    # Docker镜像文件(25MB AMD64)
├── docker-compose.yml                     # Docker Compose配置
├── nginx.conf                             # Nginx配置
├── healthcheck.sh                         # 健康检查脚本
├── 客户部署指南.md                        # 完整部署文档⭐
├── 一分钟快速开始.md                      # 快速参考
├── 跨平台部署说明.md                      # 平台兼容说明
├── 故障排查指南.md                        # 常见问题解决⭐
├── 遇到问题请先看这里.txt                  # 快速参考卡片⭐
└── 部署脚本/
    ├── 0-import-only.sh                  # 仅导入镜像脚本⭐
    ├── generate-ssl-cert.sh              # SSL证书生成脚本⭐ (HTTPS必需)
    ├── quick-start.sh                    # 一键启动脚本
    ├── import-image.sh                   # 交互式导入脚本
    └── clean.sh                          # 环境清理脚本
```

**📖 推荐阅读顺序**:
1. **README.md** (本文档) - 快速开始部署
2. **HTTPS部署指南.md** - 如果需要通过 IP 访问，必读！⭐⭐
3. **Mock_API_演示说明.md** - 了解 Mock API 工作原理和演示数据
4. **遇到问题请先看这里.txt** - 遇到错误时的快速参考
5. **故障排查指南.md** - 详细的问题排查步骤

---

## 🚀 四种部署方式

### 方式一: 仅导入镜像(推荐⭐遇到问题时使用)

```bash
./部署脚本/0-import-only.sh
```

- ✅ 只导入镜像，不启动容器
- ✅ 适合先导入，稍后手动启动
- ✅ 解决"Unable to find image"错误
- ✅ 清晰的步骤提示

导入后可使用docker run或docker-compose启动。

### 方式二: 使用一键脚本(最快⭐)

```bash
./部署脚本/quick-start.sh
```

- ✅ 全自动化
- ✅ 智能环境检查
- ✅ 自动导入和启动
- ✅ 健康验证

**注意**: 如果遇到"Unable to find image"错误，先使用方式一导入镜像。

### 方式三: 使用Docker Compose

```bash
# 1. 导入镜像(必须先执行!)
gunzip -c gcrf-web-admin-amd64-v1.0.0.tar.gz | docker load

# 2. 启动服务
docker-compose up -d

# 3. 查看状态
docker-compose ps
```

### 方式四: 交互式导入

```bash
./部署脚本/import-image.sh
```

按照脚本提示选择镜像并启动。

---

## 💡 重要说明

### 关于平台架构

**✨ 本镜像为 Linux/AMD64 原生架构**

| 平台 | 支持 | 说明 |
|-----|-----|------|
| Intel/AMD (Linux) | ✅ 原生 | **最佳性能，推荐使用** |
| Intel/AMD (Windows Docker) | ✅ 原生 | WSL2环境原生支持 |
| Apple Silicon (M1/M2/M3) | ✅ 兼容 | Rosetta模拟，性能略降 |

**在Intel/AMD Linux服务器上运行**（推荐部署环境）：

```bash
docker run -d \
  --name gcrf-web-admin \
  -p 3011:80 \
  --restart unless-stopped \
  gcrf-library-web-admin:amd64
```

**镜像优势**:
- ✅ 原生AMD64架构，无需模拟
- ✅ 最佳性能，适合生产环境
- ✅ 体积小（24MB），下载快速
- ✅ 兼容所有主流Linux发行版

详细说明请查看: `跨平台部署说明.md`

---

## 🎯 演示数据

系统内置完整演示数据：

- **图书**: 100本(包括《三体》《活着》《百年孤独》等)
- **读者**: 200人(学生、教师、职工、公众)
- **流通记录**: 600条(借阅、预约、续借等各种状态)
- **完整统计**: 趋势分析、排行榜、分类统计

---

## 📖 推荐演示流程

### 快速演示(15分钟)
1. **登录首页** (2分钟) - 查看统计概览
2. **图书管理** (5分钟) - 搜索、查看、编目
3. **流通管理** (5分钟) - 借书、还书、查记录
4. **数据统计** (3分钟) - 趋势图表、排行榜

### 完整演示(45分钟)
查看`客户部署指南.md`中的7个详细演示场景。

---

## 🔧 常用命令

```bash
# 查看日志
docker logs -f gcrf-web-admin

# 重启服务
docker restart gcrf-web-admin

# 停止服务
docker stop gcrf-web-admin

# 删除容器
docker rm -f gcrf-web-admin

# 清理环境
./部署脚本/clean.sh
```

---

## ❓ 常见问题

### Q: 提示"Unable to find image"或Docker尝试从网络拉取镜像？
**A**: 镜像还没有导入。请先手动导入：
```bash
# 进入部署包目录
cd /path/to/客户演示部署包

# 导入镜像
gunzip -c gcrf-web-admin-amd64-v1.0.0.tar.gz | docker load

# 验证
docker images | grep gcrf-library-web-admin

# 然后启动
docker run -d --name gcrf-web-admin -p 3011:80 gcrf-library-web-admin:amd64
```

### Q: 提示平台不匹配？
**A**: 添加 `--platform linux/amd64` 参数或使用提供的脚本（已自动处理）。

### Q: 端口3011被占用？
**A**: 修改`docker-compose.yml`中的端口映射，或使用其他端口：
```bash
docker run -d -p 8080:80 --platform linux/amd64 ...
```

### Q: 页面显示空白？
**A**: 清除浏览器缓存(Ctrl+Shift+Delete)，或使用无痕模式。

### Q: 无法访问？
**A**:
```bash
# 检查容器状态
docker ps | grep gcrf-web-admin

# 检查健康状态
curl http://localhost:3011/health
```

**详细故障排查**: 请查看 `故障排查指南.md` 获取完整的问题解决方案。

---

## 📞 技术支持

- **完整文档**: `客户部署指南.md` (详细17KB文档)
- **技术支持**: support@gcrf-library.com
- **客服电话**: 400-xxx-xxxx (工作日 9:00-18:00)

---

## 📊 系统要求

- **Docker**: 20.10+
- **内存**: 256MB+
- **磁盘**: 1GB+
- **CPU**: 0.5核+
- **浏览器**: Chrome 90+, Firefox 88+, Safari 14+, Edge 90+

---

## ✅ 验证清单

部署后请验证：

- [ ] 访问 http://localhost:3011
- [ ] 健康检查: `curl http://localhost:3011/health` 返回 "healthy"
- [ ] 可使用admin/admin123登录
- [ ] 可查看图书列表
- [ ] Mock数据正常显示

---

**版本**: v1.0.0
**发布日期**: 2025-10-20
**© 2025 国创睿峰科技有限公司**
