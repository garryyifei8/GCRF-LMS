# GCRF Library 快速开始指南

**版本**: 1.0.0
**适用**: 开发者快速上手
**预计时间**: 30分钟

---

## 🚀 5分钟快速体验

### 前提条件

```bash
# 确认已安装
docker --version  # 需要 20.10+
docker-compose --version  # 需要 2.x
```

### 一键启动 (Demo模式)

```bash
# 1. 克隆项目
git clone <repository-url>
cd GCRF_LibraryManagementSystem

# 2. 启动所有服务 (包括基础设施+微服务+监控)
cd deployment
docker-compose -f docker-compose.infrastructure.yml up -d
sleep 30  # 等待PostgreSQL和Nacos启动
docker-compose -f docker-compose.services.yml up -d
./scripts/start-monitoring.sh

# 3. 验证服务
curl http://localhost:8080/actuator/health
```

### 访问服务

| 服务             | URL                         | 用户名 | 密码     |
| ---------------- | --------------------------- | ------ | -------- |
| **API Gateway**  | http://localhost:8080       | -      | -        |
| **Web管理端**    | http://localhost:3011       | admin  | admin123 |
| **Grafana监控**  | http://localhost:3000       | admin  | admin    |
| **Prometheus**   | http://localhost:9090       | -      | -        |
| **Nacos控制台**  | http://localhost:8848/nacos | nacos  | nacos    |
| **RabbitMQ管理** | http://localhost:15672      | admin  | admin123 |

### 测试API

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 登录获取Token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 获取图书列表
curl http://localhost:8080/api/v1/books?pageNum=1&pageSize=20 \
  -H "Authorization: Bearer <your-token>"
```

---

## 📖 详细开发环境搭建

### 步骤1: 环境准备

#### macOS

```bash
# 安装Homebrew
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 安装必需软件
brew install --cask docker
brew install git openjdk@21 maven

# 配置Java 21
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc
source ~/.zshrc
java -version  # 验证: 应显示 21.x.x
```

#### Ubuntu/Debian

```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker

# 安装Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 安装Java 21 + Maven
sudo apt install -y openjdk-21-jdk maven git

# 验证
docker --version
docker-compose --version
java -version
mvn -version
```

### 步骤2: 克隆项目

```bash
# 克隆到本地
git clone <repository-url>
cd GCRF_LibraryManagementSystem

# 查看项目结构
tree -L 2 -d
```

**项目结构**:

```
GCRF_LibraryManagementSystem/
├── backend/                # 后端微服务
│   ├── common/            # 公共模块
│   ├── gateway-service/   # API网关
│   ├── auth-service/      # 认证服务
│   ├── book-service/      # 图书服务
│   ├── circulation-service/  # 流通服务
│   ├── reader-service/    # 读者服务
│   └── ...
├── web-admin/             # 前端管理界面
├── deployment/            # 部署配置
│   ├── scripts/          # 自动化脚本
│   └── monitoring/       # 监控配置
└── docs/                  # 文档
```

### 步骤3: 启动基础设施

```bash
cd deployment

# 启动PostgreSQL + Redis + RabbitMQ + Nacos
docker-compose -f docker-compose.infrastructure.yml up -d

# 查看启动状态
docker-compose -f docker-compose.infrastructure.yml ps

# 应该看到4个服务全部Running:
# ✓ gcrf-postgres-primary
# ✓ gcrf-redis
# ✓ gcrf-rabbitmq
# ✓ gcrf-nacos
```

**等待服务就绪** (约30秒):

```bash
# 等待PostgreSQL就绪
until pg_isready -h localhost -U postgres; do sleep 1; done
echo "PostgreSQL is ready!"

# 等待Nacos就绪
until curl -sf http://localhost:8848/nacos/; do sleep 1; done
echo "Nacos is ready!"
```

### 步骤4: 初始化数据库

```bash
# 自动创建所有数据库
./scripts/init-all-databases.sh

# 验证数据库已创建
psql -h localhost -U postgres -l | grep gcrf_

# 应该看到5个数据库:
# ✓ gcrf_auth
# ✓ gcrf_book
# ✓ gcrf_circulation
# ✓ gcrf_reader
# ✓ gcrf_system
```

### 步骤5: 构建并启动微服务

#### 方式1: 使用预构建镜像 (推荐新手)

```bash
# 从镜像仓库拉取
docker pull gcrf-gateway:latest
docker pull gcrf-auth-service:latest
docker pull gcrf-book-service:latest
docker pull gcrf-circulation-service:latest
docker pull gcrf-reader-service:latest

# 启动所有微服务
docker-compose -f docker-compose.services.yml up -d

# 查看日志
docker-compose -f docker-compose.services.yml logs -f
```

#### 方式2: 从源码构建 (开发者)

```bash
# 设置Java 21 (⚠️ 重要!)
export JAVA_HOME=$(/usr/libexec/java_home -v 21)  # macOS
# export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64  # Linux

# 构建所有JAR文件 (约5-10分钟)
cd scripts
./ci-build-all.sh

# 构建Docker镜像 (约5分钟)
./ci-docker-build.sh --tag latest

# 启动服务
cd ..
docker-compose -f docker-compose.services.yml up -d
```

### 步骤6: 验证服务状态

```bash
# 1. 检查容器状态
docker ps | grep gcrf

# 应该看到5个微服务全部Running:
# ✓ gcrf-gateway-service
# ✓ gcrf-auth-service
# ✓ gcrf-book-service
# ✓ gcrf-circulation-service
# ✓ gcrf-reader-service

# 2. 检查服务注册到Nacos
curl -s "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=gateway-service" | jq

# 3. 健康检查
curl http://localhost:8080/actuator/health

# 4. 测试API
curl http://localhost:8080/api/v1/health
```

### 步骤7: 启动前端 (可选)

```bash
cd ../web-admin

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 访问: http://localhost:3011
```

### 步骤8: 启动监控 (可选但推荐)

```bash
cd ../deployment

# 启动Prometheus + Grafana
./scripts/start-monitoring.sh

# 访问Grafana
open http://localhost:3000
# 用户名: admin, 密码: admin
```

---

## 💡 常用开发任务

### 开发新功能

```bash
# 1. 切换到功能分支
git checkout -b feature/my-new-feature

# 2. 修改代码
cd backend/auth-service/src/main/java/...

# 3. 本地测试
cd backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn test -pl auth-service

# 4. 启动单个服务调试
cd auth-service
mvn spring-boot:run

# 5. 提交代码
git add .
git commit -m "feat(auth): add new feature"
git push origin feature/my-new-feature
```

### 调试服务

```bash
# 查看实时日志
docker logs -f gcrf-auth-service

# 进入容器
docker exec -it gcrf-auth-service bash

# 查看配置
docker exec gcrf-auth-service cat /app/application.yml

# 查看环境变量
docker exec gcrf-auth-service env | grep -E "DB_|NACOS_"
```

### 重启服务

```bash
# 重启单个服务
docker-compose -f docker-compose.services.yml restart auth-service

# 重启所有微服务
docker-compose -f docker-compose.services.yml restart

# 重新构建并启动
./scripts/ci-build-all.sh && \
./scripts/ci-docker-build.sh --tag latest && \
docker-compose -f docker-compose.services.yml up -d --force-recreate
```

### 查看监控指标

```bash
# Prometheus查询API响应时间
curl -s "http://localhost:9090/api/v1/query?query=http_server_requests_seconds_bucket" | jq

# 查看服务健康状态
curl -s "http://localhost:9090/api/v1/query?query=up" | jq

# 查看JVM内存使用
curl -s "http://localhost:9090/api/v1/query?query=jvm_memory_used_bytes" | jq
```

### 数据库操作

```bash
# 连接PostgreSQL
psql -h localhost -U postgres -d gcrf_auth

# 查看表结构
\d sys_user

# 执行查询
SELECT * FROM sys_user LIMIT 10;

# 退出
\q
```

### Redis操作

```bash
# 连接Redis
redis-cli -h localhost -p 6379

# 查看所有键
KEYS *

# 查看键值
GET cache:user:1

# 删除键
DEL cache:user:1

# 退出
EXIT
```

---

## 🔧 故障排查速查表

| 问题           | 诊断命令                                                                        | 解决方案                             |
| -------------- | ------------------------------------------------------------------------------- | ------------------------------------ |
| 服务无法启动   | `docker logs <container>`                                                       | 检查日志中的错误信息                 |
| 端口被占用     | `lsof -i :8081`                                                                 | 杀死占用进程或修改端口               |
| 数据库连接失败 | `psql -h localhost -U postgres`                                                 | 确认PostgreSQL已启动                 |
| Nacos无法连接  | `curl http://localhost:8848/nacos/`                                             | 确认Nacos已启动                      |
| 服务未注册     | `curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=auth-service` | 检查Nacos配置                        |
| 构建失败       | `mvn -version && java -version`                                                 | 确认使用Java 21                      |
| 内存不足       | `docker stats`                                                                  | 增加Docker内存限制                   |
| 磁盘空间不足   | `df -h`                                                                         | 清理Docker: `docker system prune -a` |

---

## 📚 下一步学习

### 必读文档

1. **[CLAUDE.md](./CLAUDE.md)** - 开发规范和最佳实践
2. **[architect.md](./backend/doc/architect.md)** - 系统架构详细设计
3. **[OPERATIONS_GUIDE.md](./docs/deployment/OPERATIONS_GUIDE.md)** - 完整部署运维指南
4. **[MONITORING_GUIDE.md](./docs/deployment/MONITORING_GUIDE.md)** - 监控系统详细说明

### 推荐学习路径

**第1周: 环境熟悉**

- ✅ 完成本快速开始指南
- ✅ 阅读CLAUDE.md开发规范
- ✅ 了解项目结构和技术栈
- ✅ 运行所有服务并访问各个管理界面

**第2周: 后端开发**

- ✅ 学习Spring Boot 3.2.2 + Spring Cloud
- ✅ 理解微服务架构和服务间通信
- ✅ 阅读architect.md理解数据库设计
- ✅ 实现一个简单的API接口

**第3周: 前端开发**

- ✅ 学习Vue 3 + Element Plus
- ✅ 理解前后端分离架构
- ✅ 学习Mock数据和API对接
- ✅ 实现一个简单的页面

**第4周: 部署运维**

- ✅ 学习Docker和Docker Compose
- ✅ 了解CI/CD自动化流程
- ✅ 学习监控系统使用
- ✅ 实践故障排查和性能优化

---

## 🆘 获取帮助

### 常见问题

**Q1: 服务启动后无法访问**

```bash
# 检查服务是否真正启动
docker ps | grep gcrf-auth-service

# 查看日志
docker logs gcrf-auth-service

# 检查端口监听
lsof -i :8081
```

**Q2: 数据库连接失败**

```bash
# 确认PostgreSQL运行
docker ps | grep postgres

# 测试连接
psql -h localhost -U postgres -c "SELECT 1"

# 检查密码配置
cat deployment/.env | grep DB_PASSWORD
```

**Q3: Nacos服务注册失败**

```bash
# 确认Nacos运行
curl http://localhost:8848/nacos/

# 检查服务配置
docker exec gcrf-auth-service cat /app/application.yml

# 查看注册日志
docker logs gcrf-auth-service | grep -i nacos
```

### 联系方式

- **GitHub Issues**: <repository-url>/issues
- **技术文档**: `docs/` 目录
- **开发规范**: `CLAUDE.md`

---

## 🎉 成功!

如果你完成了所有步骤,现在你应该:

- ✅ 所有基础设施服务运行正常
- ✅ 5个微服务全部注册到Nacos
- ✅ API Gateway响应正常
- ✅ 监控系统Grafana显示指标
- ✅ 前端管理界面可以访问

**接下来可以**:

- 🚀 开始你的第一个功能开发
- 📖 深入学习系统架构设计
- 🔧 尝试部署到生产环境
- 📊 探索监控和性能优化

---

**版本**: 1.0.0
**最后更新**: 2025-11-02
**维护人**: DevOps Team

**快速链接**:

- [完整部署指南](./docs/deployment/OPERATIONS_GUIDE.md)
- [开发规范](./CLAUDE.md)
- [架构设计](./backend/doc/architect.md)
- [监控指南](./docs/deployment/MONITORING_GUIDE.md)
