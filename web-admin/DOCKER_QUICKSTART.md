# Docker 快速开始指南

## 🎯 一键测试部署

### 完整测试流程(推荐)

```bash
# 1. 进入项目目录
cd web-admin

# 2. 构建镜像
./docker/build.sh latest

# 3. 部署测试环境
./docker/deploy-test.sh

# 4. 访问系统
# 打开浏览器访问: http://localhost:3011
# 使用账号: admin / admin123
```

### 快速命令

```bash
# 启动
docker-compose -f docker-compose.test.yml up -d

# 查看日志
docker-compose -f docker-compose.test.yml logs -f

# 停止
docker-compose -f docker-compose.test.yml down

# 重启
docker-compose -f docker-compose.test.yml restart
```

---

## 📦 镜像导出(用于离线部署)

### 导出镜像文件

```bash
# 导出最新版本镜像
./docker/export-image.sh latest

# 导出到指定目录
./docker/export-image.sh latest /path/to/output/

# 导出指定版本
./docker/export-image.sh 1.0.0
```

导出后会生成:
- `gcrf-web-admin-{version}-{timestamp}.tar.gz` - 镜像文件
- `gcrf-web-admin-{version}-{timestamp}.tar.gz.meta.txt` - 元数据信息

### 导入镜像文件

在目标服务器上:

```bash
# 1. 上传镜像文件到服务器

# 2. 导入镜像
gunzip -c gcrf-web-admin-latest-20251018_120000.tar.gz | docker load

# 3. 验证导入
docker images | grep gcrf-library-web-admin

# 4. 运行容器
docker run -d -p 3011:80 --name gcrf-web-admin gcrf-library-web-admin:latest

# 5. 访问测试
curl http://localhost:3011/health
```

---

## 🧪 测试环境说明

### 包含功能

- ✅ 完整的前端应用
- ✅ Mock API服务(自动启用)
- ✅ 演示数据(100本图书,200个读者,500条流通记录)
- ✅ 健康检查
- ✅ 日志输出

### 测试账号

| 用户名 | 密码 | 角色 | 权限 |
|--------|------|------|------|
| admin | admin123 | 管理员 | 全部权限 |
| librarian | lib123 | 馆员 | 借阅、图书、读者 |
| operator | op123 | 操作员 | 借还书 |

### 演示数据

**图书数据**:
- 100本精选图书
- 包含《三体》、《活着》、《百年孤独》等知名作品
- 完整的书籍信息(ISBN、封面、简介等)

**读者数据**:
- 200个读者(学生、教师、职工)
- 完整的个人信息和借阅历史

**流通记录**:
- 500条借阅记录
- 100条预约记录
- 包含正常、逾期、续借等各种状态

---

## 🔍 健康检查

### 检查方法

```bash
# 方式1: 使用curl
curl http://localhost:3011/health

# 方式2: 使用docker命令
docker inspect --format='{{json .State.Health}}' gcrf-web-admin-test

# 方式3: 进入容器检查
docker exec gcrf-web-admin-test /usr/local/bin/healthcheck.sh
```

### 健康状态

- `healthy` - 服务正常
- `unhealthy` - 服务异常
- `starting` - 启动中

---

## 📊 资源使用

### 镜像大小

- 构建镜像: ~150MB
- 压缩后: ~50MB

### 运行资源

- CPU: ~0.1核(空闲时)
- 内存: ~50MB(空闲时)
- 磁盘: ~150MB

### 推荐配置

- CPU: 0.5核以上
- 内存: 256MB以上
- 磁盘: 1GB以上

---

## 🛠️ 常用操作

### 查看日志

```bash
# 实时日志
docker logs -f gcrf-web-admin-test

# 最近100行
docker logs --tail 100 gcrf-web-admin-test

# 查看nginx错误日志
docker exec gcrf-web-admin-test cat /var/log/nginx/error.log
```

### 进入容器调试

```bash
# 进入容器
docker exec -it gcrf-web-admin-test sh

# 查看nginx配置
cat /etc/nginx/conf.d/default.conf

# 查看文件列表
ls -la /usr/share/nginx/html

# 测试nginx配置
nginx -t

# 重载nginx
nginx -s reload
```

### 重启服务

```bash
# 重启容器
docker restart gcrf-web-admin-test

# 重启nginx(在容器内)
docker exec gcrf-web-admin-test nginx -s reload
```

---

## ❌ 停止和清理

### 停止容器

```bash
# 停止但不删除
docker stop gcrf-web-admin-test

# 停止并删除
docker rm -f gcrf-web-admin-test

# 使用docker-compose停止
docker-compose -f docker-compose.test.yml down
```

### 清理资源

```bash
# 删除镜像
docker rmi gcrf-library-web-admin:latest

# 清理未使用的镜像
docker image prune

# 清理所有未使用的资源
docker system prune -a
```

---

## 🆘 故障排查

### 问题1: 容器启动失败

```bash
# 查看容器状态
docker ps -a | grep gcrf

# 查看错误日志
docker logs gcrf-web-admin-test

# 检查端口占用
lsof -i:3011
# 或
netstat -tlnp | grep 3011
```

### 问题2: 无法访问

```bash
# 检查容器是否运行
docker ps | grep gcrf

# 检查端口映射
docker port gcrf-web-admin-test

# 测试健康检查
curl http://localhost:3011/health

# 检查防火墙
sudo ufw status
```

### 问题3: Mock数据不显示

```bash
# 检查dist目录是否包含mockServiceWorker.js
docker exec gcrf-web-admin-test ls -la /usr/share/nginx/html/ | grep mock

# 查看浏览器控制台
# 应该看到: [MSW] Mock Service Worker started

# 检查网络请求
# 打开浏览器开发者工具 > Network
```

---

## 📞 获取帮助

- 详细文档: `DOCKER_DEPLOYMENT.md`
- 问题反馈: GitHub Issues
- 技术支持: GCRF技术团队

---

**快速参考卡片**

```bash
# 构建
./docker/build.sh

# 部署
./docker/deploy-test.sh

# 导出
./docker/export-image.sh latest

# 查看日志
docker logs -f gcrf-web-admin-test

# 停止
docker-compose -f docker-compose.test.yml down
```

---

**版本**: v1.0.0
**更新**: 2025-10-18
