# GCRF Library 自动化脚本使用指南

**版本**: 1.0.0
**更新日期**: 2025-11-02

---

## 📋 目录

1. [概述](#概述)
2. [CI/CD 脚本](#cicd-脚本)
3. [部署脚本](#部署脚本)
4. [备份与恢复脚本](#备份与恢复脚本)
5. [监控脚本](#监控脚本)
6. [健康检查脚本](#健康检查脚本)
7. [完整CI/CD流程](#完整cicd流程)
8. [故障排查](#故障排查)

---

## 概述

GCRF Library 提供了一套完整的自动化脚本,涵盖从构建、测试、打包到部署的全流程。

### 脚本列表

| 脚本名称 | 用途 | 位置 |
|---------|------|------|
| `ci-build-all.sh` | 构建所有微服务 | `deployment/scripts/` |
| `ci-test-all.sh` | 运行所有测试 | `deployment/scripts/` |
| `ci-docker-build.sh` | 构建Docker镜像 | `deployment/scripts/` |
| `deploy-services.sh` | 部署服务 | `deployment/scripts/` |
| `backup-volumes.sh` | 备份数据卷 | `deployment/scripts/` |
| `restore-volumes.sh` | 恢复数据卷 | `deployment/scripts/` |
| `start-monitoring.sh` | 启动监控栈 | `deployment/scripts/` |
| `test-monitoring.sh` | 测试监控系统 | `deployment/scripts/` |

---

## CI/CD 脚本

### 1. ci-build-all.sh - 构建脚本

**用途**: 编译所有微服务,生成JAR文件

**基本用法**:
```bash
cd deployment/scripts
./ci-build-all.sh
```

**高级选项**:
```bash
# 跳过测试(快速构建)
./ci-build-all.sh --skip-tests

# 清理后重新构建
./ci-build-all.sh --clean

# 使用8个并行线程
./ci-build-all.sh --parallel 8

# 指定Maven profile
./ci-build-all.sh --profile prod
```

**输出**:
- JAR文件: `backend/*/target/*.jar`
- 构建日志: `build-logs/build_YYYYMMDD_HHMMSS.log`
- 构建报告: `build-logs/build_report_YYYYMMDD_HHMMSS.txt`

**预计时间**: 5-10分钟(取决于硬件)

---

### 2. ci-test-all.sh - 测试脚本

**用途**: 运行单元测试和集成测试

**基本用法**:
```bash
./ci-test-all.sh
```

**高级选项**:
```bash
# 只运行单元测试
./ci-test-all.sh --unit-only

# 只运行集成测试
./ci-test-all.sh --integration-only

# 测试特定服务
./ci-test-all.sh --service auth-service

# 生成覆盖率报告
./ci-test-all.sh --coverage

# 使用4个并行线程
./ci-test-all.sh --parallel 4
```

**输出**:
- 测试日志: `test-logs/test_YYYYMMDD_HHMMSS.log`
- 测试报告: `test-logs/test_report_YYYYMMDD_HHMMSS.txt`
- 覆盖率报告: `backend/*/target/site/jacoco/index.html`

**预计时间**: 3-8分钟

---

### 3. ci-docker-build.sh - Docker构建脚本

**用途**: 构建所有微服务的Docker镜像

**基本用法**:
```bash
./ci-docker-build.sh
```

**高级选项**:
```bash
# 指定镜像标签
./ci-docker-build.sh --tag v1.0.0

# 指定平台(用于跨平台构建)
./ci-docker-build.sh --platform linux/amd64

# 构建特定服务
./ci-docker-build.sh --service gateway-service

# 不使用缓存
./ci-docker-build.sh --no-cache

# 构建并推送到镜像仓库
./ci-docker-build.sh --registry registry.example.com --push

# 并行构建
./ci-docker-build.sh --parallel 4
```

**输出**:
- Docker镜像: `gcrf-*:latest` (本地Docker)
- 构建日志: `docker-build-logs/docker_build_YYYYMMDD_HHMMSS.log`
- 构建报告: `docker-build-logs/docker_build_report_YYYYMMDD_HHMMSS.txt`

**预计时间**: 10-20分钟

---

## 部署脚本

### deploy-services.sh - 服务部署脚本

**用途**: 部署或更新微服务

**基本用法**:
```bash
./deploy-services.sh
```

**高级选项**:
```bash
# 部署特定服务
./deploy-services.sh --service auth-service

# 指定镜像标签
./deploy-services.sh --tag v1.0.0

# 部署策略
./deploy-services.sh --strategy rolling  # 滚动更新(默认)
./deploy-services.sh --strategy blue-green  # 蓝绿部署
./deploy-services.sh --strategy recreate  # 重建

# 不等待健康检查
./deploy-services.sh --no-health-check

# 回滚到上一个版本
./deploy-services.sh --rollback

# 模拟运行(不实际部署)
./deploy-services.sh --dry-run
```

**部署流程**:
1. 备份当前容器
2. 拉取新镜像
3. 启动新容器
4. 健康检查(60秒超时)
5. 清理备份容器

**输出**:
- 部署日志: `deployment-logs/deploy_YYYYMMDD_HHMMSS.log`

**预计时间**: 2-5分钟

---

## 备份与恢复脚本

### 1. backup-volumes.sh - 备份脚本

**用途**: 备份PostgreSQL、Redis、Nacos数据卷

**基本用法**:
```bash
./backup-volumes.sh
```

**高级选项**:
```bash
# 指定备份目录
./backup-volumes.sh --backup-dir /path/to/backups

# 只备份特定组件
./backup-volumes.sh --component postgres
./backup-volumes.sh --component redis
./backup-volumes.sh --component nacos

# 压缩备份
./backup-volumes.sh --compress
```

**备份内容**:
- PostgreSQL数据库(所有数据库)
- Redis持久化文件(RDB + AOF)
- Nacos配置和服务注册信息
- 监控数据(Prometheus + Grafana)

**备份位置**: `backups/backup_YYYYMMDD_HHMMSS/`

**预计时间**: 1-5分钟(取决于数据量)

---

### 2. restore-volumes.sh - 恢复脚本

**用途**: 从备份恢复数据

**基本用法**:
```bash
./restore-volumes.sh --backup-dir backups/backup_20251102_143000
```

**高级选项**:
```bash
# 只恢复特定组件
./restore-volumes.sh --backup-dir backups/backup_20251102_143000 --component postgres

# 恢复前确认
./restore-volumes.sh --backup-dir backups/backup_20251102_143000 --confirm
```

**注意事项**:
- ⚠️ 恢复会覆盖现有数据,请谨慎操作
- 建议先备份当前数据再恢复
- 恢复前停止所有服务

**预计时间**: 2-10分钟

---

## 监控脚本

### 1. start-monitoring.sh - 启动监控

**用途**: 启动Prometheus + Grafana监控栈

**基本用法**:
```bash
./start-monitoring.sh
```

**启动内容**:
- Prometheus (Port 9090)
- Grafana (Port 3000, admin/admin)
- Node Exporter (Port 9100)
- PostgreSQL Exporter (Port 9187)
- Redis Exporter (Port 9121)

**预计时间**: 30秒

---

### 2. test-monitoring.sh - 测试监控

**用途**: 验证监控系统是否正常工作

**基本用法**:
```bash
./test-monitoring.sh
```

**测试内容**(8个阶段):
1. 容器健康检查
2. Prometheus验证
3. Grafana验证
4. Exporter验证
5. 服务Actuator端点验证
6. 指标数据验证
7. 告警规则语法验证
8. 数据持久化验证

**输出**: 测试报告(通过/失败统计)

**预计时间**: 1-2分钟

---

## 健康检查脚本

### health-check.sh - 全栈健康检查

**用途**: 检查所有组件的健康状态

**基本用法**:
```bash
./health-check.sh
```

**检查内容**:
- Docker容器状态
- PostgreSQL连接
- Redis连接
- Nacos服务发现
- 微服务Actuator端点
- 监控系统状态

**输出**: 健康状态报告

**预计时间**: 30秒

---

## 完整CI/CD流程

### 场景1: 从零开始部署

```bash
# 1. 构建所有服务
cd deployment/scripts
./ci-build-all.sh --clean

# 2. 运行测试
./ci-test-all.sh

# 3. 构建Docker镜像
./ci-docker-build.sh --tag v1.0.0

# 4. 启动基础设施
./start-stack.sh

# 5. 启动监控
./start-monitoring.sh

# 6. 部署服务
./deploy-services.sh --tag v1.0.0

# 7. 验证部署
./health-check.sh
./test-monitoring.sh
```

**总时间**: 约30-45分钟

---

### 场景2: 更新单个服务

```bash
# 1. 构建特定服务
./ci-build-all.sh --service auth-service --skip-tests

# 2. 测试该服务
./ci-test-all.sh --service auth-service

# 3. 构建Docker镜像
./ci-docker-build.sh --service auth-service --tag v1.0.1

# 4. 滚动更新
./deploy-services.sh --service auth-service --tag v1.0.1

# 5. 验证
curl http://localhost:8081/actuator/health
```

**总时间**: 约5-10分钟

---

### 场景3: 灾难恢复

```bash
# 1. 停止所有服务
./stop-stack.sh

# 2. 恢复数据
./restore-volumes.sh --backup-dir backups/backup_20251102_120000

# 3. 重启基础设施
./start-stack.sh

# 4. 重新部署服务
./deploy-services.sh

# 5. 验证
./health-check.sh
```

**总时间**: 约15-30分钟

---

### 场景4: 持续集成(GitHub Actions示例)

```yaml
# .github/workflows/ci.yml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up Java 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'

      - name: Build
        run: |
          cd deployment/scripts
          ./ci-build-all.sh --skip-tests

      - name: Test
        run: ./ci-test-all.sh --coverage

      - name: Build Docker Images
        run: ./ci-docker-build.sh --tag ${{ github.sha }}

      - name: Push to Registry
        run: |
          ./ci-docker-build.sh --tag ${{ github.sha }} \
            --registry ghcr.io/${{ github.repository_owner }} \
            --push

  deploy:
    needs: build-and-test
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to Production
        run: |
          ssh deploy@prod-server << 'EOF'
            cd /opt/gcrf-library/deployment/scripts
            ./deploy-services.sh --tag ${{ github.sha }}
          EOF
```

---

## 故障排查

### 问题1: 构建失败

**症状**: `ci-build-all.sh` 报错

**诊断**:
```bash
# 检查Java版本
java -version  # 应该是21

# 检查Maven
mvn -version

# 查看构建日志
tail -100 build-logs/build_YYYYMMDD_HHMMSS.log

# 检查磁盘空间
df -h
```

**解决**:
```bash
# 设置正确的JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# 清理Maven缓存
rm -rf ~/.m2/repository/com/gcrf

# 重新构建
./ci-build-all.sh --clean
```

---

### 问题2: 测试失败

**症状**: `ci-test-all.sh` 有测试失败

**诊断**:
```bash
# 查看测试日志
tail -200 test-logs/test_YYYYMMDD_HHMMSS.log

# 查看特定服务测试日志
tail -200 test-logs/test_auth-service_YYYYMMDD_HHMMSS.log

# 检查Docker是否运行(集成测试需要)
docker ps
```

**解决**:
```bash
# 先只运行单元测试
./ci-test-all.sh --unit-only

# 启动测试依赖的基础设施
./start-stack.sh

# 再运行集成测试
./ci-test-all.sh --integration-only
```

---

### 问题3: Docker构建失败

**症状**: `ci-docker-build.sh` 失败

**诊断**:
```bash
# 检查Docker守护进程
docker ps

# 检查JAR文件是否存在
ls -lh backend/*/target/*.jar

# 检查磁盘空间
df -h

# 查看构建日志
tail -200 docker-build-logs/docker_build_YYYYMMDD_HHMMSS.log
```

**解决**:
```bash
# 确保先构建JAR
./ci-build-all.sh

# 清理Docker缓存
docker system prune -a

# 重新构建镜像
./ci-docker-build.sh --no-cache
```

---

### 问题4: 部署失败

**症状**: `deploy-services.sh` 失败

**诊断**:
```bash
# 检查容器状态
docker ps -a

# 检查服务日志
docker logs gcrf-auth-service --tail 100

# 检查网络
docker network ls
docker network inspect gcrf-backend-network

# 查看部署日志
tail -200 deployment-logs/deploy_YYYYMMDD_HHMMSS.log
```

**解决**:
```bash
# 回滚到上一个版本
./deploy-services.sh --rollback

# 或者重新部署
docker-compose -f deployment/docker-compose.services.yml down
./deploy-services.sh
```

---

## 最佳实践

### 1. 开发环境

```bash
# 每天开发前
./health-check.sh

# 修改代码后
./ci-build-all.sh --service <your-service> --skip-tests
./ci-test-all.sh --service <your-service>

# 本地测试
docker-compose -f deployment/docker-compose.services.yml up -d <your-service>
```

---

### 2. 生产环境

```bash
# 部署前
1. 创建备份
   ./backup-volumes.sh --compress

2. 在测试环境验证
   ./ci-test-all.sh
   ./health-check.sh

3. 部署(使用蓝绿部署)
   ./deploy-services.sh --strategy blue-green --tag v1.0.0

4. 验证
   ./health-check.sh
   ./test-monitoring.sh

5. 监控
   # 访问 Grafana: http://localhost:3000
   # 检查 Prometheus Alerts: http://localhost:9090/alerts
```

---

### 3. 定期维护

```bash
# 每日
- 检查监控告警
- 查看服务日志

# 每周
- 运行健康检查: ./health-check.sh
- 创建备份: ./backup-volumes.sh
- 清理旧日志: find logs/ -mtime +7 -delete

# 每月
- 更新依赖
- 运行完整测试: ./ci-test-all.sh
- 审查监控指标趋势
```

---

## 相关文档

- [监控指南](./MONITORING_GUIDE.md) - 监控系统使用
- [故障排查](./TROUBLESHOOTING_METRICS.md) - 故障诊断
- [部署指南](./DEPLOYMENT_GUIDE.md) - 详细部署步骤

---

**版本**: 1.0.0
**最后更新**: 2025-11-02
**维护团队**: GCRF Library DevOps Team
