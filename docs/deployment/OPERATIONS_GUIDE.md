# GCRF Library 部署运维完整指南

**版本**: 1.0.0
**更新日期**: 2025-11-02
**适用环境**: Production / Staging / Development

---

## 📚 目录

1. [概述](#概述)
2. [环境准备](#环境准备)
3. [基础设施部署](#基础设施部署)
4. [应用部署](#应用部署)
5. [监控系统](#监控系统)
6. [CI/CD自动化](#cicd自动化)
7. [日常运维](#日常运维)
8. [故障排查](#故障排查)
9. [性能优化](#性能优化)
10. [安全加固](#安全加固)
11. [附录](#附录)

---

## 概述

### 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         前端应用层                                │
│   Web管理端(Vue3)    微信小程序(uni-app)    自助终端(React)        │
└─────────────────────────────────────────────────────────────────┘
                                ↕ HTTPS
┌─────────────────────────────────────────────────────────────────┐
│                      API网关 (Gateway: 8080)                      │
└─────────────────────────────────────────────────────────────────┘
                                ↕
┌─────────────────────────────────────────────────────────────────┐
│                         微服务层                                  │
│  Auth(8081)  Book(8082)  Circulation(8083)  Reader(8084)        │
│  System(8085)  Notification(8090)                               │
└─────────────────────────────────────────────────────────────────┘
                                ↕
┌─────────────────────────────────────────────────────────────────┐
│                     基础设施层                                    │
│  Nacos(8848)  PostgreSQL(5432)  Redis(6379)  RabbitMQ(5672)    │
└─────────────────────────────────────────────────────────────────┘
                                ↕
┌─────────────────────────────────────────────────────────────────┐
│                      监控层                                       │
│  Prometheus(9090)  Grafana(3000)  Exporters                    │
└─────────────────────────────────────────────────────────────────┘
```

### 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 开发语言 | Java | 21 LTS |
| 框架 | Spring Boot | 3.2.2 |
| 微服务 | Spring Cloud | 2023.0.0 |
| 注册中心 | Nacos | 2.3.0 |
| 数据库 | PostgreSQL | 15+ |
| 缓存 | Redis | 7.x |
| 消息队列 | RabbitMQ | 3.12.x |
| 容器化 | Docker | 20.10+ |
| 监控 | Prometheus + Grafana | 2.48.0 + 10.2.2 |

### 部署规模

| 规模 | 图书册数 | 服务器配置 | 部署方式 |
|------|---------|-----------|----------|
| 小型 | < 1万册 | 4核8G × 1 | Docker Compose |
| 中型 | 1-10万册 | 8核16G × 2 | Docker Compose + LB |
| 大型 | > 10万册 | 按需 | Kubernetes |

---

## 环境准备

### 硬件要求

#### 最小配置 (开发环境)
- CPU: 4核
- 内存: 8GB
- 磁盘: 50GB SSD
- 网络: 100Mbps

#### 推荐配置 (生产环境)
- CPU: 8核以上
- 内存: 16GB以上
- 磁盘: 200GB SSD (数据盘单独挂载)
- 网络: 1Gbps

### 软件依赖

```bash
# 操作系统: Ubuntu 20.04+ / CentOS 8+ / macOS 12+

# 必需软件
- Docker 20.10+
- Docker Compose 2.x
- Java 21 (仅构建时需要)
- Maven 3.9+ (仅构建时需要)
- Git 2.x

# 可选工具
- jq (JSON处理)
- curl (API测试)
- psql (PostgreSQL客户端)
- redis-cli (Redis客户端)
```

### 环境安装

#### Ubuntu/Debian

```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# 安装Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 安装工具
sudo apt install -y git jq curl postgresql-client redis-tools

# 验证安装
docker --version
docker-compose --version
```

#### macOS

```bash
# 安装Homebrew
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 安装Docker Desktop
brew install --cask docker

# 安装工具
brew install git jq curl postgresql redis

# 验证安装
docker --version
docker-compose --version
```

### 端口规划

| 服务 | 端口 | 说明 |
|------|------|------|
| Gateway | 8080 | API网关入口 |
| Auth Service | 8081 | 认证服务 |
| Book Service | 8082 | 图书服务 |
| Circulation Service | 8083 | 流通服务 |
| Reader Service | 8084 | 读者服务 |
| System Service | 8085 | 系统服务 |
| Notification Service | 8090 | 通知服务 |
| Nacos | 8848 | 注册配置中心 |
| PostgreSQL | 5432 | 主数据库 |
| Redis | 6379 | 缓存 |
| RabbitMQ | 5672, 15672 | 消息队列 + 管理界面 |
| Prometheus | 9090 | 监控指标数据库 |
| Grafana | 3000 | 监控可视化 |
| Node Exporter | 9100 | 服务器指标 |

### 防火墙配置

```bash
# Ubuntu/Debian
sudo ufw allow 8080/tcp comment 'API Gateway'
sudo ufw allow 3000/tcp comment 'Grafana'
sudo ufw allow 9090/tcp comment 'Prometheus'
sudo ufw enable

# CentOS/RHEL
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --permanent --add-port=3000/tcp
sudo firewall-cmd --permanent --add-port=9090/tcp
sudo firewall-cmd --reload
```

---

## 基础设施部署

### 步骤1: 克隆项目

```bash
# 克隆仓库
git clone <repository-url> /opt/gcrf-library
cd /opt/gcrf-library

# 设置权限
sudo chown -R $USER:$USER /opt/gcrf-library
chmod +x deployment/scripts/*.sh
```

### 步骤2: 环境变量配置

```bash
# 创建环境变量文件
cp deployment/.env.example deployment/.env

# 编辑环境变量
vi deployment/.env
```

**关键配置项**:

```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=5432
DB_USER=postgres
DB_PASSWORD=your_secure_password

# Redis配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# Nacos配置
NACOS_SERVER=localhost:8848
NACOS_NAMESPACE=production
NACOS_USERNAME=nacos
NACOS_PASSWORD=your_nacos_password

# RabbitMQ配置
MQ_HOST=localhost
MQ_PORT=5672
MQ_USERNAME=admin
MQ_PASSWORD=your_mq_password
```

### 步骤3: 启动基础设施

```bash
cd deployment

# 启动PostgreSQL
docker-compose -f docker-compose.infrastructure.yml up -d postgres

# 等待PostgreSQL启动 (约10秒)
sleep 10

# 初始化数据库
./scripts/init-all-databases.sh

# 启动Redis
docker-compose -f docker-compose.infrastructure.yml up -d redis

# 启动RabbitMQ
docker-compose -f docker-compose.infrastructure.yml up -d rabbitmq

# 启动Nacos
docker-compose -f docker-compose.infrastructure.yml up -d nacos

# 验证所有服务运行
docker-compose -f docker-compose.infrastructure.yml ps
```

### 步骤4: 验证基础设施

```bash
# 检查PostgreSQL
psql -h localhost -U postgres -c "SELECT version();"

# 检查Redis
redis-cli ping

# 检查Nacos
curl http://localhost:8848/nacos/

# 检查RabbitMQ
curl http://localhost:15672/
```

---

## 应用部署

### 方式1: 使用预构建镜像 (推荐生产环境)

```bash
# 拉取镜像
docker pull gcrf-gateway:latest
docker pull gcrf-auth-service:latest
docker pull gcrf-book-service:latest
docker pull gcrf-circulation-service:latest
docker pull gcrf-reader-service:latest

# 启动所有服务
cd deployment
docker-compose -f docker-compose.services.yml up -d

# 查看日志
docker-compose -f docker-compose.services.yml logs -f
```

### 方式2: 从源码构建部署

#### 2.1 构建JAR文件

```bash
cd deployment/scripts

# 构建所有服务 (约5-10分钟)
./ci-build-all.sh

# 查看构建报告
cat ../../build-logs/build_report_*.txt
```

**构建选项**:

```bash
# 跳过测试 (快速构建)
./ci-build-all.sh --skip-tests

# 清理后构建
./ci-build-all.sh --clean

# 8线程并行构建
./ci-build-all.sh --parallel 8
```

#### 2.2 构建Docker镜像

```bash
# 构建所有服务镜像 (约10-15分钟)
./ci-docker-build.sh --tag latest

# 查看构建的镜像
docker images | grep gcrf
```

**构建选项**:

```bash
# 指定平台 (生产环境用amd64)
./ci-docker-build.sh --platform linux/amd64 --tag v1.0.0

# 构建并推送到镜像仓库
./ci-docker-build.sh --tag v1.0.0 --registry registry.example.com --push

# 不使用缓存 (完全重新构建)
./ci-docker-build.sh --no-cache --tag latest
```

#### 2.3 部署服务

```bash
# 部署所有服务 (滚动更新)
./deploy-services.sh --tag latest

# 部署特定服务
./deploy-services.sh --service auth-service --tag v1.0.0

# 蓝绿部署
./deploy-services.sh --strategy blue-green --tag v1.0.1

# 快速部署 (跳过健康检查)
./deploy-services.sh --no-health-check
```

### 验证部署

```bash
# 检查所有服务状态
docker ps | grep gcrf

# 检查Gateway健康状态
curl http://localhost:8080/actuator/health

# 检查所有服务注册到Nacos
curl -s "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=gateway-service"

# 测试API
curl http://localhost:8080/api/v1/health
```

---

## 监控系统

### 启动监控栈

```bash
cd deployment

# 启动Prometheus + Grafana + Exporters
./scripts/start-monitoring.sh

# 验证监控服务
curl http://localhost:9090/api/v1/targets  # Prometheus
curl http://localhost:3000/api/health      # Grafana
curl http://localhost:9100/metrics         # Node Exporter
```

### 访问监控界面

#### Prometheus (http://localhost:9090)

- **Targets**: http://localhost:9090/targets
  - 查看所有监控目标健康状态
  - 应该看到5个微服务 + 3个Exporter全部UP

- **Alerts**: http://localhost:9090/alerts
  - 查看所有告警规则
  - 70+告警规则 (Critical/Warning/Info)

- **查询示例**:
  ```promql
  # 服务健康状态
  up{job=~".*-service"}

  # HTTP响应时间 P95
  histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, application))

  # JVM堆内存使用率
  jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
  ```

#### Grafana (http://localhost:3000)

**首次登录**:
- 用户名: `admin`
- 密码: `admin`
- 系统会要求修改密码

**推荐仪表板**:

1. **Spring Boot 应用监控**
   - Dashboard ID: `4701` (JVM Micrometer)
   - 显示: JVM堆内存、GC、线程、类加载

2. **Microservices 监控**
   - Dashboard ID: `11159` (Spring Boot 2.1)
   - 显示: HTTP请求、响应时间、错误率

3. **PostgreSQL 监控**
   - Dashboard ID: `9628`
   - 显示: 连接数、查询性能、缓存命中率

4. **Redis 监控**
   - Dashboard ID: `11835`
   - 显示: 内存使用、命中率、连接数

5. **Node 服务器监控**
   - Dashboard ID: `1860`
   - 显示: CPU、内存、磁盘、网络

**导入仪表板**:

```bash
# 方式1: 通过UI导入
Grafana → Dashboards → Import → 输入Dashboard ID

# 方式2: 通过API导入
curl -X POST http://admin:admin@localhost:3000/api/dashboards/import \
  -H "Content-Type: application/json" \
  -d '{"dashboard": {"id": null}, "pluginId": "", "overwrite": true, "inputs": [{"name": "DS_PROMETHEUS", "type": "datasource", "pluginId": "prometheus", "value": "Prometheus"}], "folderId": 0}'
```

### 监控指标说明

#### 应用层指标

| 指标 | 说明 | 阈值 |
|------|------|------|
| `up` | 服务健康状态 | = 1 |
| `http_server_requests_seconds` | HTTP响应时间 | P95 < 200ms |
| `jvm_memory_used_bytes` | JVM堆内存使用 | < 85% |
| `jvm_gc_pause_seconds` | GC暂停时间 | < 100ms |
| `hikaricp_connections_active` | 数据库连接数 | < 80% |

#### 基础设施指标

| 指标 | 说明 | 阈值 |
|------|------|------|
| `node_cpu_seconds_total` | CPU使用率 | < 80% |
| `node_memory_MemAvailable_bytes` | 可用内存 | > 20% |
| `node_filesystem_avail_bytes` | 磁盘可用空间 | > 20% |
| `pg_up` | PostgreSQL健康状态 | = 1 |
| `redis_up` | Redis健康状态 | = 1 |

#### 业务指标

| 指标 | 说明 |
|------|------|
| `library_borrow_total` | 借书次数统计 |
| `library_return_total` | 还书次数统计 |
| `library_search_total` | 搜索次数统计 |
| `library_book_overdue_total` | 逾期图书数量 |

### 告警规则

**Critical (严重 - 立即处理)**:
- 服务宕机 (1分钟内)
- 数据库不可用
- 磁盘空间 < 10%
- 内存使用 > 95%

**Warning (警告 - 30分钟内处理)**:
- HTTP错误率 > 5%
- 响应时间 P95 > 2秒
- JVM堆内存 > 85%
- 数据库连接池 > 80%

**Info (提示 - 工作时间处理)**:
- 服务重启
- GC频繁 (> 10次/分钟)
- 业务异常增多

### 测试监控系统

```bash
cd deployment/scripts

# 运行完整监控测试 (8个测试阶段)
./test-monitoring.sh

# 只测试特定目标
./test-monitoring.sh --targets gateway-service,auth-service

# 生成测试报告
./test-monitoring.sh --report
```

**测试覆盖**:
- ✅ Docker网络连通性
- ✅ Prometheus目标健康状态
- ✅ 告警规则加载
- ✅ Grafana数据源连接
- ✅ Exporter指标采集
- ✅ 服务Actuator端点
- ✅ 告警规则触发测试
- ✅ 综合评分

---

## CI/CD自动化

### 完整CI/CD流程

```mermaid
graph LR
    A[代码提交] --> B[构建JAR]
    B --> C[运行测试]
    C --> D[构建镜像]
    D --> E[推送镜像]
    E --> F[部署服务]
    F --> G[健康检查]
    G --> H[通知]
```

### 一键部署脚本

创建 `deployment/scripts/full-deployment.sh`:

```bash
#!/bin/bash
set -e

echo "========================================"
echo "   GCRF Library - Full Deployment"
echo "========================================"
echo

# 1. 构建
echo "Step 1/5: Building JARs..."
./ci-build-all.sh --skip-tests --parallel 8

# 2. 测试
echo "Step 2/5: Running Tests..."
./ci-test-all.sh --parallel 4

# 3. 打包
echo "Step 3/5: Building Docker Images..."
./ci-docker-build.sh --tag latest --platform linux/amd64

# 4. 部署
echo "Step 4/5: Deploying Services..."
./deploy-services.sh --tag latest --strategy rolling

# 5. 验证
echo "Step 5/5: Verifying Deployment..."
sleep 30
curl -f http://localhost:8080/actuator/health || exit 1

echo
echo "✓ Deployment completed successfully!"
```

### Jenkins Pipeline 示例

```groovy
pipeline {
    agent any

    environment {
        IMAGE_TAG = "${BUILD_NUMBER}"
        REGISTRY = "registry.example.com"
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/your-org/gcrf-library.git'
            }
        }

        stage('Build') {
            steps {
                sh './deployment/scripts/ci-build-all.sh --skip-tests'
            }
        }

        stage('Test') {
            parallel {
                stage('Unit Tests') {
                    steps {
                        sh './deployment/scripts/ci-test-all.sh --unit-only'
                    }
                }
                stage('Integration Tests') {
                    steps {
                        sh './deployment/scripts/ci-test-all.sh --integration-only'
                    }
                }
            }
        }

        stage('Build Images') {
            steps {
                sh "./deployment/scripts/ci-docker-build.sh --tag ${IMAGE_TAG} --platform linux/amd64"
            }
        }

        stage('Push to Registry') {
            steps {
                sh "./deployment/scripts/ci-docker-build.sh --tag ${IMAGE_TAG} --registry ${REGISTRY} --push"
            }
        }

        stage('Deploy') {
            steps {
                sh "./deployment/scripts/deploy-services.sh --tag ${IMAGE_TAG}"
            }
        }

        stage('Health Check') {
            steps {
                script {
                    sleep(30)
                    sh 'curl -f http://localhost:8080/actuator/health'
                }
            }
        }
    }

    post {
        success {
            echo 'Deployment successful!'
        }
        failure {
            echo 'Deployment failed!'
        }
    }
}
```

### GitHub Actions 示例

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up Java 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Build JARs
      run: ./deployment/scripts/ci-build-all.sh --skip-tests

    - name: Run Tests
      run: ./deployment/scripts/ci-test-all.sh --parallel 4

    - name: Build Docker Images
      run: ./deployment/scripts/ci-docker-build.sh --tag ${{ github.sha }}

    - name: Deploy to Staging
      if: github.event_name == 'push' && github.ref == 'refs/heads/main'
      run: ./deployment/scripts/deploy-services.sh --tag ${{ github.sha }}

    - name: Health Check
      run: |
        sleep 30
        curl -f http://localhost:8080/actuator/health
```

---

## 日常运维

### 启动/停止服务

```bash
# 启动所有基础设施
cd deployment
docker-compose -f docker-compose.infrastructure.yml up -d

# 启动所有微服务
docker-compose -f docker-compose.services.yml up -d

# 启动监控栈
./scripts/start-monitoring.sh

# 停止所有服务
docker-compose -f docker-compose.services.yml down
docker-compose -f docker-compose.infrastructure.yml down
./scripts/stop-monitoring.sh

# 重启特定服务
docker-compose -f docker-compose.services.yml restart auth-service

# 查看服务日志
docker-compose -f docker-compose.services.yml logs -f auth-service
```

### 数据备份

```bash
cd deployment/scripts

# 全量备份 (数据库 + Redis + 文件)
./backup-volumes.sh

# 只备份PostgreSQL
./backup-volumes.sh --volumes postgres

# 只备份Redis
./backup-volumes.sh --volumes redis

# 备份到指定目录
./backup-volumes.sh --output /backup/gcrf-library

# 备份 + 压缩
./backup-volumes.sh --compress

# 查看备份文件
ls -lh ../backups/
```

**备份输出**:
```
backups/
├── backup_20251102_143520/
│   ├── postgres_gcrf_auth.sql
│   ├── postgres_gcrf_book.sql
│   ├── postgres_gcrf_circulation.sql
│   ├── postgres_gcrf_reader.sql
│   ├── postgres_gcrf_system.sql
│   ├── redis_dump.rdb
│   └── backup_manifest.txt
└── backup_20251102_143520.tar.gz
```

### 数据恢复

```bash
# 从最新备份恢复
./restore-volumes.sh

# 从指定备份恢复
./restore-volumes.sh --backup ../backups/backup_20251102_143520

# 只恢复PostgreSQL
./restore-volumes.sh --volumes postgres --backup ../backups/backup_20251102_143520

# 强制恢复 (覆盖现有数据)
./restore-volumes.sh --force
```

### 日志管理

```bash
# 查看实时日志
docker logs -f gcrf-auth-service

# 查看最近100行日志
docker logs --tail 100 gcrf-auth-service

# 查看特定时间范围日志
docker logs --since "2025-11-02T10:00:00" --until "2025-11-02T12:00:00" gcrf-auth-service

# 导出日志到文件
docker logs gcrf-auth-service > auth-service.log 2>&1

# 清理Docker日志
docker logs gcrf-auth-service --tail 0 > /dev/null 2>&1
```

### 配置更新

```bash
# 更新Nacos配置
curl -X POST "http://localhost:8848/nacos/v1/cs/configs" \
  -d "dataId=auth-service.yml" \
  -d "group=LIBRARY_GROUP" \
  -d "content=$(cat config/auth-service.yml)"

# 重启服务使配置生效
docker-compose -f docker-compose.services.yml restart auth-service

# 动态刷新配置 (Spring Cloud Config Refresh)
curl -X POST http://localhost:8081/actuator/refresh
```

### 服务扩容

```bash
# 扩容Auth Service到3个实例
docker-compose -f docker-compose.services.yml up -d --scale auth-service=3

# 验证实例数量
docker ps | grep auth-service

# 负载均衡自动生效 (通过Nacos服务发现)
```

---

## 故障排查

### 常见问题诊断

#### 1. 服务无法启动

**症状**: 容器启动后立即退出

**诊断步骤**:

```bash
# 1. 查看容器日志
docker logs gcrf-auth-service

# 2. 检查端口占用
lsof -i :8081

# 3. 检查资源限制
docker stats gcrf-auth-service

# 4. 检查配置
docker exec gcrf-auth-service cat /app/application.yml
```

**常见原因**:
- ✗ Java版本不匹配 → 确认使用Java 21
- ✗ 端口已被占用 → 杀死占用进程或修改端口
- ✗ 数据库连接失败 → 检查DB_HOST和DB_PASSWORD
- ✗ Nacos连接失败 → 确认Nacos已启动

#### 2. 服务注册失败

**症状**: Nacos控制台看不到服务实例

**诊断步骤**:

```bash
# 1. 检查Nacos服务状态
curl http://localhost:8848/nacos/v1/console/health/liveness

# 2. 检查服务配置
docker exec gcrf-auth-service env | grep NACOS

# 3. 查看注册日志
docker logs gcrf-auth-service | grep "Nacos"

# 4. 手动注册测试
curl -X POST "http://localhost:8848/nacos/v1/ns/instance" \
  -d "serviceName=test-service&ip=127.0.0.1&port=8080"
```

**常见原因**:
- ✗ Nacos地址配置错误
- ✗ 网络隔离 (Docker网络问题)
- ✗ Nacos认证失败

#### 3. 数据库连接失败

**症状**: 日志显示 "Connection refused" 或 "Authentication failed"

**诊断步骤**:

```bash
# 1. 测试PostgreSQL连接
psql -h localhost -U postgres -d gcrf_auth -c "SELECT 1"

# 2. 检查连接配置
docker exec gcrf-auth-service env | grep DB_

# 3. 查看PostgreSQL日志
docker logs gcrf-postgres-primary | tail -50

# 4. 检查数据库是否存在
psql -h localhost -U postgres -c "\l" | grep gcrf_
```

**常见原因**:
- ✗ 数据库未初始化
- ✗ 密码错误
- ✗ 防火墙阻止连接
- ✗ pg_hba.conf配置限制

#### 4. 响应时间慢

**症状**: API响应时间 > 2秒

**诊断步骤**:

```bash
# 1. 检查服务CPU/内存
docker stats gcrf-auth-service

# 2. 检查慢查询
psql -h localhost -U postgres -d gcrf_auth -c "
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;"

# 3. 检查Redis命中率
redis-cli INFO stats | grep keyspace

# 4. 查看Prometheus指标
curl "http://localhost:9090/api/v1/query?query=http_server_requests_seconds_bucket{application=\"auth-service\"}"
```

**常见原因**:
- ✗ 数据库索引缺失
- ✗ 未启用缓存
- ✗ 大量慢查询
- ✗ JVM GC频繁

#### 5. 内存溢出 (OOM)

**症状**: 容器重启频繁, 日志显示 "OutOfMemoryError"

**诊断步骤**:

```bash
# 1. 检查JVM堆配置
docker exec gcrf-auth-service java -XX:+PrintFlagsFinal -version | grep HeapSize

# 2. 生成堆转储
docker exec gcrf-auth-service jmap -dump:format=b,file=/tmp/heap.hprof 1

# 3. 分析堆内存使用
docker exec gcrf-auth-service jmap -histo:live 1 | head -20

# 4. 查看GC日志
docker logs gcrf-auth-service | grep "GC"
```

**解决方案**:
```yaml
# docker-compose.services.yml
services:
  auth-service:
    environment:
      JAVA_OPTS: "-Xms512m -Xmx1g -XX:MaxMetaspaceSize=256m"
```

### 监控故障排查

#### Prometheus无数据

```bash
# 1. 检查Targets状态
curl http://localhost:9090/api/v1/targets

# 2. 测试服务Metrics端点
curl http://localhost:8081/actuator/prometheus

# 3. 检查网络连通性
docker exec prometheus ping -c 3 auth-service

# 4. 重启Prometheus重新加载配置
docker restart prometheus
```

#### Grafana无法连接Prometheus

```bash
# 1. 测试数据源连接
curl -X POST http://admin:admin@localhost:3000/api/datasources/proxy/1/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{"query":"up"}'

# 2. 检查Grafana配置
docker exec grafana cat /etc/grafana/provisioning/datasources/prometheus.yml

# 3. 查看Grafana日志
docker logs grafana | grep -i error
```

### 日志排查技巧

```bash
# 搜索特定错误
docker logs gcrf-auth-service 2>&1 | grep -i "error"

# 查找异常堆栈
docker logs gcrf-auth-service 2>&1 | grep -A 20 "Exception"

# 统计错误数量
docker logs gcrf-auth-service 2>&1 | grep -c "ERROR"

# 查看最近30分钟日志
docker logs --since 30m gcrf-auth-service

# 实时监控关键字
docker logs -f gcrf-auth-service | grep --line-buffered "WARN\|ERROR"
```

---

## 性能优化

### JVM调优

```yaml
# docker-compose.services.yml
services:
  auth-service:
    environment:
      JAVA_OPTS: >-
        -Xms512m -Xmx1g
        -XX:MaxMetaspaceSize=256m
        -XX:+UseG1GC
        -XX:MaxGCPauseMillis=200
        -XX:+HeapDumpOnOutOfMemoryError
        -XX:HeapDumpPath=/tmp/heap_dump.hprof
        -Djava.security.egd=file:/dev/./urandom
```

### 数据库优化

```sql
-- 创建索引
CREATE INDEX idx_book_isbn ON t_book(isbn);
CREATE INDEX idx_circulation_reader ON t_circulation(reader_id);
CREATE INDEX idx_circulation_book ON t_circulation(book_copy_id);

-- 分析表
ANALYZE t_book;
ANALYZE t_circulation;

-- 查看执行计划
EXPLAIN ANALYZE SELECT * FROM t_book WHERE isbn = '978-7-111-12345-6';
```

### Redis缓存优化

```bash
# 设置合理的过期时间
# 热门图书: 1小时
# 用户权限: 30分钟
# 统计数据: 5分钟

# 启用Redis持久化
redis-cli CONFIG SET save "900 1 300 10 60 10000"

# 设置最大内存
redis-cli CONFIG SET maxmemory 2gb
redis-cli CONFIG SET maxmemory-policy allkeys-lru
```

### 连接池优化

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

---

## 安全加固

### 1. 网络隔离

```yaml
# docker-compose.infrastructure.yml
networks:
  backend:
    driver: bridge
    internal: true  # 内部网络,外部无法访问

  frontend:
    driver: bridge  # 前端网络,可暴露端口
```

### 2. 密码策略

```bash
# 使用强密码
# 最少12位, 包含大小写+数字+特殊字符

# 定期更换密码 (建议90天)

# 使用密钥管理系统
# 推荐: HashiCorp Vault, AWS Secrets Manager
```

### 3. HTTPS加密

```bash
# 生成自签名证书 (开发环境)
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout /etc/ssl/private/nginx-selfsigned.key \
  -out /etc/ssl/certs/nginx-selfsigned.crt

# 配置Nginx SSL (生产环境使用Let's Encrypt)
```

### 4. 防火墙规则

```bash
# 只允许必要端口
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp   # SSH
sudo ufw allow 80/tcp   # HTTP
sudo ufw allow 443/tcp  # HTTPS
sudo ufw allow 8080/tcp # API Gateway
sudo ufw enable
```

### 5. 日志脱敏

```java
// Logback配置
<configuration>
  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
      <layout class="com.gcrf.library.common.log.MaskingPatternLayout">
        <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        <maskPattern>\"password\"\s*:\s*\"(.*?)\"</maskPattern>
        <maskPattern>\"token\"\s*:\s*\"(.*?)\"</maskPattern>
      </layout>
    </encoder>
  </appender>
</configuration>
```

---

## 附录

### A. 脚本参考

| 脚本 | 位置 | 说明 |
|------|------|------|
| `ci-build-all.sh` | `deployment/scripts/` | 构建所有微服务JAR |
| `ci-test-all.sh` | `deployment/scripts/` | 运行所有测试 |
| `ci-docker-build.sh` | `deployment/scripts/` | 构建Docker镜像 |
| `deploy-services.sh` | `deployment/scripts/` | 部署服务 |
| `backup-volumes.sh` | `deployment/scripts/` | 备份数据 |
| `restore-volumes.sh` | `deployment/scripts/` | 恢复数据 |
| `start-monitoring.sh` | `deployment/scripts/` | 启动监控 |
| `stop-monitoring.sh` | `deployment/scripts/` | 停止监控 |
| `test-monitoring.sh` | `deployment/scripts/` | 测试监控 |

### B. 配置文件

| 文件 | 位置 | 说明 |
|------|------|------|
| `prometheus.yml` | `deployment/monitoring/prometheus/` | Prometheus配置 |
| `infrastructure-alerts.yml` | `deployment/monitoring/prometheus/alerts/` | 基础设施告警 |
| `service-alerts.yml` | `deployment/monitoring/prometheus/alerts/` | 服务告警 |
| `docker-compose.infrastructure.yml` | `deployment/` | 基础设施编排 |
| `docker-compose.services.yml` | `deployment/` | 微服务编排 |
| `docker-compose.monitoring.yml` | `deployment/` | 监控编排 |

### C. 相关文档

| 文档 | 位置 | 说明 |
|------|------|------|
| `MONITORING_GUIDE.md` | `docs/deployment/` | 监控系统详细指南 |
| `TROUBLESHOOTING_METRICS.md` | `docs/deployment/` | 监控故障排查 |
| `AUTOMATION_GUIDE.md` | `docs/deployment/` | 自动化脚本详细说明 |
| `GRAFANA_QUICKSTART.md` | `deployment/monitoring/` | Grafana快速开始 |
| `ALERTS_GUIDE.md` | `deployment/monitoring/` | 告警规则说明 |

### D. 常用命令速查

```bash
# 查看所有容器
docker ps -a

# 查看网络
docker network ls
docker network inspect gcrf-backend

# 查看卷
docker volume ls
docker volume inspect gcrf-postgres-data

# 查看镜像
docker images | grep gcrf

# 清理未使用资源
docker system prune -a --volumes

# 查看Docker磁盘使用
docker system df

# 导出/导入镜像
docker save gcrf-auth-service:latest | gzip > auth-service.tar.gz
gunzip -c auth-service.tar.gz | docker load
```

---

**文档版本**: 1.0.0
**最后更新**: 2025-11-02
**维护人**: DevOps Team
**反馈邮箱**: devops@gcrf-library.com

---

## 快速链接

- [MONITORING_GUIDE.md](./MONITORING_GUIDE.md) - 监控系统完整指南
- [AUTOMATION_GUIDE.md](./AUTOMATION_GUIDE.md) - 自动化脚本详细说明
- [TROUBLESHOOTING_METRICS.md](./TROUBLESHOOTING_METRICS.md) - 监控故障排查
- [CLAUDE.md](../../CLAUDE.md) - 开发指南
- [architect.md](../../backend/doc/architect.md) - 技术架构文档

