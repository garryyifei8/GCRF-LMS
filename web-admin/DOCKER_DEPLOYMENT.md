# GCRF智能图书馆管理系统 - Docker部署指南

## 📋 目录

- [快速开始](#快速开始)
- [构建镜像](#构建镜像)
- [部署测试环境](#部署测试环境)
- [生产环境部署](#生产环境部署)
- [镜像导出导入](#镜像导出导入)
- [故障排查](#故障排查)

---

## 🚀 快速开始

### 前置要求

- Docker 20.10+
- Docker Compose 2.0+
- 至少 2GB 可用内存
- 至少 5GB 可用磁盘空间

### 快速部署测试环境

```bash
cd web-admin

# 方式1: 使用部署脚本(推荐)
./docker/deploy-test.sh

# 方式2: 使用docker-compose
docker-compose -f docker-compose.test.yml up -d
```

访问 http://localhost:3011

**测试账号**:
- 管理员: `admin` / `admin123`
- 馆员: `librarian` / `lib123`
- 操作员: `operator` / `op123`

---

## 🔨 构建镜像

### 方式1: 使用构建脚本(推荐)

```bash
# 构建latest版本
./docker/build.sh

# 构建指定版本
./docker/build.sh 1.0.0

# 构建并推送到镜像仓库
REGISTRY=registry.example.com ./docker/build.sh 1.0.0
```

### 方式2: 手动构建

```bash
# 基础构建
docker build -t gcrf-library-web-admin:latest .

# 带构建参数
docker build \
  --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
  --build-arg VERSION=1.0.0 \
  -t gcrf-library-web-admin:1.0.0 \
  .
```

### 查看构建的镜像

```bash
docker images gcrf-library-web-admin
```

---

## 🧪 部署测试环境

### 使用测试环境配置

测试环境包含:
- ✅ Mock API服务(MSW)
- ✅ 完整演示数据(100本图书,200个读者,500条记录)
- ✅ 健康检查
- ✅ 日志挂载
- ✅ 自动重启

### 启动测试环境

```bash
# 使用脚本部署(推荐)
./docker/deploy-test.sh

# 或使用docker-compose
docker-compose -f docker-compose.test.yml up -d
```

### 查看日志

```bash
# 实时日志
docker-compose -f docker-compose.test.yml logs -f

# 查看最近100行
docker logs --tail 100 gcrf-web-admin-test
```

### 进入容器

```bash
docker exec -it gcrf-web-admin-test sh
```

### 停止测试环境

```bash
docker-compose -f docker-compose.test.yml down
```

---

## 🌐 生产环境部署

### 配置说明

生产环境建议:
- 使用外部nginx反向代理
- 配置HTTPS
- 挂载持久化日志
- 配置资源限制
- 连接真实后端API

### 生产环境docker-compose配置示例

```yaml
version: '3.8'

services:
  web-admin:
    image: gcrf-library-web-admin:1.0.0
    container_name: gcrf-web-admin-prod
    ports:
      - "80:80"
    environment:
      - TZ=Asia/Shanghai
      - NODE_ENV=production
    volumes:
      - ./logs:/var/log/nginx
      - ./nginx/prod.conf:/etc/nginx/conf.d/default.conf
    restart: always
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 512M
        reservations:
          cpus: '0.5'
          memory: 256M
    healthcheck:
      test: ["CMD", "/usr/local/bin/healthcheck.sh"]
      interval: 30s
      timeout: 3s
      retries: 3
    networks:
      - prod-network

networks:
  prod-network:
    external: true
```

### 使用nginx反向代理

```nginx
# /etc/nginx/sites-available/gcrf-library
upstream gcrf_web_admin {
    server localhost:3011;
}

server {
    listen 80;
    server_name library.example.com;

    # 重定向到HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name library.example.com;

    # SSL证书配置
    ssl_certificate /etc/nginx/ssl/library.example.com.crt;
    ssl_certificate_key /etc/nginx/ssl/library.example.com.key;

    # SSL配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # 日志
    access_log /var/log/nginx/gcrf-access.log;
    error_log /var/log/nginx/gcrf-error.log;

    location / {
        proxy_pass http://gcrf_web_admin;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket支持(如需要)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

---

## 📦 镜像导出导入

### 导出镜像

```bash
# 导出为tar文件
docker save gcrf-library-web-admin:latest -o gcrf-web-admin-latest.tar

# 压缩导出
docker save gcrf-library-web-admin:latest | gzip > gcrf-web-admin-latest.tar.gz
```

### 导入镜像

```bash
# 从tar文件导入
docker load -i gcrf-web-admin-latest.tar

# 从压缩文件导入
gunzip -c gcrf-web-admin-latest.tar.gz | docker load
```

### 推送到镜像仓库

```bash
# 登录镜像仓库
docker login registry.example.com

# 打标签
docker tag gcrf-library-web-admin:latest registry.example.com/gcrf/web-admin:latest

# 推送
docker push registry.example.com/gcrf/web-admin:latest
```

### 从镜像仓库拉取

```bash
# 拉取镜像
docker pull registry.example.com/gcrf/web-admin:latest

# 重新打标签(可选)
docker tag registry.example.com/gcrf/web-admin:latest gcrf-library-web-admin:latest
```

---

## 🔍 故障排查

### 容器无法启动

```bash
# 查看容器状态
docker ps -a | grep gcrf

# 查看容器日志
docker logs gcrf-web-admin-test

# 查看详细信息
docker inspect gcrf-web-admin-test
```

### 健康检查失败

```bash
# 查看健康状态
docker inspect --format='{{json .State.Health}}' gcrf-web-admin-test | jq

# 手动执行健康检查
docker exec gcrf-web-admin-test /usr/local/bin/healthcheck.sh

# 查看nginx进程
docker exec gcrf-web-admin-test ps aux | grep nginx
```

### 访问不了

```bash
# 检查端口映射
docker port gcrf-web-admin-test

# 测试健康检查端点
curl http://localhost:3011/health

# 测试首页
curl -I http://localhost:3011

# 查看nginx日志
docker exec gcrf-web-admin-test cat /var/log/nginx/error.log
```

### 构建失败

```bash
# 清理构建缓存
docker builder prune

# 无缓存构建
docker build --no-cache -t gcrf-library-web-admin:latest .

# 查看构建过程
docker build --progress=plain -t gcrf-library-web-admin:latest .
```

### 常见问题

**Q: 镜像太大怎么办?**

A: 使用多阶段构建已经优化了镜像大小,如需进一步优化:
- 删除不必要的依赖
- 使用alpine基础镜像
- 清理构建缓存

**Q: Mock数据不显示?**

A: 确认:
1. 构建时包含了mock目录
2. mockServiceWorker.js文件存在于dist目录
3. main.js中启用了Mock服务

**Q: 容器重启后数据丢失?**

A: Mock数据存储在内存中,重启会重置。生产环境应连接真实后端API。

**Q: 如何修改端口?**

A: 修改docker-compose.yml中的ports配置:
```yaml
ports:
  - "8080:80"  # 宿主机:容器
```

---

## 📊 监控和运维

### 资源监控

```bash
# 查看资源使用
docker stats gcrf-web-admin-test

# 查看容器详细信息
docker inspect gcrf-web-admin-test

# 查看网络
docker network inspect gcrf-test-network
```

### 日志管理

```bash
# 日志轮转配置
docker run -d \
  --log-driver json-file \
  --log-opt max-size=10m \
  --log-opt max-file=3 \
  gcrf-library-web-admin:latest

# 查看日志大小
docker inspect --format='{{.LogPath}}' gcrf-web-admin-test | xargs ls -lh
```

### 备份和恢复

```bash
# 备份容器
docker commit gcrf-web-admin-test gcrf-web-admin-backup:$(date +%Y%m%d)

# 导出配置
docker-compose -f docker-compose.test.yml config > backup-config.yml

# 备份日志
docker cp gcrf-web-admin-test:/var/log/nginx ./logs-backup/
```

---

## 🎯 性能优化

### Nginx缓存配置

在生产环境nginx.conf中添加:

```nginx
# 缓存配置
proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=my_cache:10m max_size=1g inactive=60m;
proxy_cache_key "$scheme$request_method$host$request_uri";

location / {
    proxy_cache my_cache;
    proxy_cache_valid 200 1h;
    proxy_cache_use_stale error timeout http_500 http_502 http_503 http_504;
    add_header X-Cache-Status $upstream_cache_status;
}
```

### 资源限制

```yaml
deploy:
  resources:
    limits:
      cpus: '1'
      memory: 512M
    reservations:
      cpus: '0.5'
      memory: 256M
```

---

## 🔐 安全建议

1. **使用非root用户运行**: 已在Dockerfile中配置nginx用户
2. **定期更新基础镜像**: 及时更新node和nginx版本
3. **扫描镜像漏洞**: 使用docker scan或第三方工具
4. **限制容器权限**: 不使用--privileged标志
5. **配置防火墙**: 只开放必要端口
6. **使用secrets**: 敏感信息使用Docker secrets

---

## 📞 技术支持

遇到问题:
1. 查看本文档故障排查章节
2. 检查容器日志
3. 查看GitHub Issues
4. 联系技术团队

---

## 📝 更新日志

### v1.0.0 (2025-10-18)
- ✅ 初始Docker化
- ✅ 多阶段构建优化
- ✅ 健康检查配置
- ✅ 测试环境配置
- ✅ 完整部署脚本

---

**文档版本**: v1.0.0
**最后更新**: 2025-10-18
**维护团队**: GCRF技术团队
