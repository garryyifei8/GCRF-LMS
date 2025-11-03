#!/bin/bash
# HTTP 环境快速修复脚本
# 不依赖 Service Worker，使用 nginx 直接提供 Mock API

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "╔════════════════════════════════════════════════════════════╗"
echo "║       HTTP 环境快速修复 - 不依赖 Service Worker         ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# 返回到部署包根目录
cd "$(dirname "$0")/.."

echo -e "${YELLOW}=== 说明 ===${NC}"
echo ""
echo "本脚本将部署一个不依赖 Service Worker 的版本："
echo "• 使用 nginx 直接返回 Mock API 数据"
echo "• 可以在 HTTP 环境下工作（不需要 HTTPS）"
echo "• 可以通过 IP 地址访问（如 http://192.168.2.121:3011）"
echo ""
echo "⚠️  限制："
echo "• Mock 数据功能有限（仅基础接口）"
echo "• 无法提供完整的业务逻辑模拟"
echo ""
read -p "是否继续? (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "操作取消"
    exit 0
fi

echo ""
echo -e "${YELLOW}=== 步骤1: 停止旧容器 ===${NC}"
echo ""

if docker ps -a | grep -q gcrf-web-admin; then
    echo "停止并删除旧容器..."
    docker stop gcrf-web-admin 2>/dev/null || true
    docker rm gcrf-web-admin 2>/dev/null || true
    echo -e "${GREEN}✓ 旧容器已删除${NC}"
else
    echo "没有发现旧容器"
fi

echo ""
echo -e "${YELLOW}=== 步骤2: 检查镜像 ===${NC}"
echo ""

if ! docker images | grep -q "gcrf-library-web-admin.*amd64"; then
    echo "镜像不存在，正在导入..."
    if [ -f "gcrf-web-admin-amd64-v1.0.0.tar.gz" ]; then
        gunzip -c gcrf-web-admin-amd64-v1.0.0.tar.gz | docker load
        echo -e "${GREEN}✓ 镜像导入成功${NC}"
    else
        echo -e "${RED}✗ 找不到镜像文件: gcrf-web-admin-amd64-v1.0.0.tar.gz${NC}"
        echo "请确保在部署包目录中运行此脚本"
        exit 1
    fi
else
    echo -e "${GREEN}✓ 镜像已存在${NC}"
fi

echo ""
echo -e "${YELLOW}=== 步骤3: 启动容器（使用 nginx Mock API）===${NC}"
echo ""

# 检查 nginx-mock-api.conf 是否存在
if [ ! -f "nginx-mock-api.conf" ]; then
    echo -e "${RED}✗ 找不到 nginx-mock-api.conf 配置文件${NC}"
    exit 1
fi

# 启动容器，挂载 nginx Mock API 配置
docker run -d \
  --name gcrf-web-admin \
  -p 3011:80 \
  -v "$(pwd)/nginx-mock-api.conf:/etc/nginx/conf.d/default.conf:ro" \
  --restart unless-stopped \
  gcrf-library-web-admin:amd64

echo -e "${GREEN}✓ 容器启动成功${NC}"

echo ""
echo -e "${YELLOW}=== 步骤4: 验证部署 ===${NC}"
echo ""

# 等待容器启动
sleep 3

# 检查容器状态
if docker ps | grep -q gcrf-web-admin; then
    echo -e "${GREEN}✓ 容器正在运行${NC}"
else
    echo -e "${RED}✗ 容器启动失败${NC}"
    echo "查看日志:"
    docker logs gcrf-web-admin
    exit 1
fi

# 测试健康检查
echo ""
echo "测试健康检查..."
if curl -s http://localhost:3011/health | grep -q "OK"; then
    echo -e "${GREEN}✓ 健康检查通过${NC}"
else
    echo -e "${RED}✗ 健康检查失败${NC}"
    exit 1
fi

# 测试登录接口
echo ""
echo "测试登录接口..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:3011/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

if echo "$LOGIN_RESPONSE" | grep -q "\"code\":200"; then
    echo -e "${GREEN}✓ 登录接口测试成功${NC}"
    echo "响应: $LOGIN_RESPONSE" | head -c 200
    echo "..."
else
    echo -e "${RED}✗ 登录接口测试失败${NC}"
    echo "响应: $LOGIN_RESPONSE"
fi

echo ""
echo ""
echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   ✅ HTTP 环境部署成功！        ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
echo ""

echo -e "${BLUE}=== 访问信息 ===${NC}"
echo ""

# 获取服务器 IP
SERVER_IP=$(hostname -I 2>/dev/null | awk '{print $1}' || echo "YOUR_SERVER_IP")

echo "📍 访问地址:"
echo "   http://localhost:3011  (服务器本地)"
echo "   http://${SERVER_IP}:3011  (远程访问)"
echo ""
echo "👤 登录账号:"
echo "   用户名: admin"
echo "   密码: admin123"
echo ""

echo -e "${YELLOW}⚠️  重要提示${NC}"
echo ""
echo "本版本使用 nginx 直接提供 Mock API，功能有限："
echo "• ✅ 可以登录"
echo "• ✅ 可以查看基础数据"
echo "• ⚠️  部分高级功能可能无法使用"
echo ""
echo "如需完整功能，请使用以下方案之一："
echo "1. 在服务器本地访问 http://localhost:3011"
echo "2. 使用 SSH 端口转发（见 HTTP环境解决方案.md）"
echo "3. 使用 HTTPS 部署（见 HTTPS部署指南.md）"
echo ""

echo -e "${BLUE}=== 查看日志 ===${NC}"
echo "docker logs -f gcrf-web-admin"
echo ""

echo -e "${BLUE}=== 停止服务 ===${NC}"
echo "docker stop gcrf-web-admin"
echo ""
