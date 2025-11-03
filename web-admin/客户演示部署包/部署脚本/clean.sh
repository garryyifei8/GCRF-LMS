#!/bin/bash

# ==================================================================
# 国创睿峰智能图书馆管理系统 - 清理脚本
# 用途: 清理Docker容器、镜像和数据卷
# ==================================================================

set -e

echo "========================================="
echo "🧹 Docker清理工具"
echo "========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 配置变量
IMAGE_NAME="gcrf-library-web-admin"
CONTAINER_NAME="gcrf-web-admin"

echo ""
echo "⚠️  警告: 此操作将清理以下资源:"
echo "   • Docker容器: ${CONTAINER_NAME}"
echo "   • Docker镜像: ${IMAGE_NAME}"
echo "   • 相关数据卷"
echo ""

read -p "是否继续清理操作? (y/n) " -n 1 -r
echo

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "操作已取消"
    exit 0
fi

echo ""
echo "========================================="
echo "开始清理..."
echo "========================================="
echo ""

# 停止并删除容器
echo "📌 Step 1: 停止并删除容器..."
if docker ps -a | grep -q "${CONTAINER_NAME}"; then
    echo "正在停止容器 ${CONTAINER_NAME}..."
    docker stop ${CONTAINER_NAME} 2>/dev/null || true
    echo "正在删除容器 ${CONTAINER_NAME}..."
    docker rm ${CONTAINER_NAME} 2>/dev/null || true
    echo -e "${GREEN}✓ 容器已清理${NC}"
else
    echo -e "${YELLOW}ℹ 未找到运行中的容器${NC}"
fi

# 使用docker-compose清理
echo ""
echo "📌 Step 2: 清理Docker Compose资源..."
if [ -f "../docker-compose.yml" ]; then
    cd ..
    docker-compose down -v 2>/dev/null || true
    cd 部署脚本
    echo -e "${GREEN}✓ Docker Compose资源已清理${NC}"
else
    echo -e "${YELLOW}ℹ 未找到docker-compose.yml${NC}"
fi

# 删除镜像
echo ""
echo "📌 Step 3: 删除Docker镜像..."
if docker images | grep -q "${IMAGE_NAME}"; then
    echo "正在删除镜像 ${IMAGE_NAME}..."
    docker rmi ${IMAGE_NAME}:latest 2>/dev/null || true
    echo -e "${GREEN}✓ 镜像已删除${NC}"
else
    echo -e "${YELLOW}ℹ 未找到镜像${NC}"
fi

# 清理未使用的资源
echo ""
echo "📌 Step 4: 清理未使用的Docker资源..."
read -p "是否清理所有未使用的Docker资源（镜像、容器、网络、数据卷）? (y/n) " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "正在清理未使用的资源..."
    docker system prune -a -f --volumes
    echo -e "${GREEN}✓ 未使用资源已清理${NC}"
else
    echo "跳过清理未使用资源"
fi

# 显示清理后的状态
echo ""
echo "========================================="
echo -e "${GREEN}✅ 清理完成！${NC}"
echo "========================================="
echo ""
echo "当前Docker状态:"
echo ""
echo "容器列表:"
docker ps -a | grep "${CONTAINER_NAME}" || echo "  （未找到相关容器）"
echo ""
echo "镜像列表:"
docker images | grep "${IMAGE_NAME}" || echo "  （未找到相关镜像）"
echo ""

echo "========================================="
echo "💡 提示"
echo "========================================="
echo ""
echo "重新部署系统:"
echo "  ./quick-start.sh"
echo ""
echo "或手动构建:"
echo "  ./build-image.sh"
echo "  docker-compose up -d"
echo ""
