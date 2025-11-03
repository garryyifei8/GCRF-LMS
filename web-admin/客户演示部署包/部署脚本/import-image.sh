#!/bin/bash

# ==================================================================
# 国创睿峰智能图书馆管理系统 - Docker镜像导入脚本
# 用途: 导入Docker镜像tar文件并启动容器（客户端使用）
# ==================================================================

set -e

echo "========================================="
echo "📥 Docker镜像导入工具"
echo "========================================="

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 配置变量
IMAGE_NAME="gcrf-library-web-admin"
IMAGE_TAG="latest"
FULL_IMAGE_NAME="${IMAGE_NAME}:${IMAGE_TAG}"

# 检查Docker是否安装
echo ""
echo "📌 检查Docker环境..."
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ 错误: Docker未安装${NC}"
    echo ""
    echo "请先安装Docker:"
    echo "  macOS/Windows: https://www.docker.com/get-started"
    echo "  Linux: curl -fsSL https://get.docker.com | sh"
    exit 1
fi

echo -e "${GREEN}✓ Docker已安装${NC}"
docker --version

# 查找tar文件
echo ""
echo "📌 查找镜像文件..."
TAR_FILE=$(find .. -maxdepth 1 -name "gcrf-web-admin*.tar" | head -1)

if [ -z "$TAR_FILE" ]; then
    echo -e "${RED}❌ 错误: 未找到镜像文件${NC}"
    echo ""
    echo "请确保以下文件存在:"
    echo "  gcrf-web-admin-*.tar"
    echo ""
    echo "或手动指定文件:"
    read -p "请输入镜像文件路径: " TAR_FILE

    if [ ! -f "$TAR_FILE" ]; then
        echo -e "${RED}❌ 文件不存在: ${TAR_FILE}${NC}"
        exit 1
    fi
fi

echo -e "${GREEN}✓ 找到镜像文件: ${TAR_FILE}${NC}"

# 获取文件大小
FILE_SIZE=$(du -h "$TAR_FILE" | cut -f1)
echo "  文件大小: ${FILE_SIZE}"

# 确认导入
echo ""
read -p "是否导入镜像? (y/n) " -n 1 -r
echo

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "操作已取消"
    exit 0
fi

# 开始导入
echo ""
echo "========================================="
echo "🔄 开始导入镜像..."
echo "========================================="
echo ""
echo -e "${YELLOW}⏳ 正在导入，请耐心等待...${NC}"

START_TIME=$(date +%s)

if docker load -i "$TAR_FILE"; then
    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))

    echo ""
    echo "========================================="
    echo -e "${GREEN}✅ 镜像导入成功！${NC}"
    echo "========================================="
    echo ""
    echo "导入耗时: ${DURATION}秒"
    echo ""

    # 显示镜像信息
    echo "镜像信息:"
    docker images ${IMAGE_NAME}

else
    echo ""
    echo -e "${RED}❌ 镜像导入失败${NC}"
    exit 1
fi

# 询问是否启动容器
echo ""
read -p "是否立即启动容器? (y/n) " -n 1 -r
echo

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "========================================="
    echo "💡 手动启动说明"
    echo "========================================="
    echo ""
    echo "使用Docker Compose启动（推荐）:"
    echo "  docker-compose up -d"
    echo ""
    echo "或使用Docker命令启动:"
    echo "  docker run -d -p 3011:80 --name gcrf-web-admin ${FULL_IMAGE_NAME}"
    echo ""
    exit 0
fi

# 停止并删除旧容器
echo ""
echo "📌 清理旧容器..."
docker stop gcrf-web-admin 2>/dev/null || true
docker rm gcrf-web-admin 2>/dev/null || true

# 启动容器
echo ""
echo "📌 启动容器..."

if docker run -d \
    --name gcrf-web-admin \
    -p 3011:80 \
    --platform linux/amd64 \
    --restart unless-stopped \
    ${FULL_IMAGE_NAME}; then

    echo -e "${GREEN}✓ 容器启动成功${NC}"

    # 等待服务就绪
    echo ""
    echo "📌 等待服务就绪..."
    echo -e "${YELLOW}⏳ 正在初始化...${NC}"

    MAX_RETRY=30
    RETRY_COUNT=0

    while [ $RETRY_COUNT -lt $MAX_RETRY ]; do
        if curl -f http://localhost:3011/health > /dev/null 2>&1; then
            echo -e "${GREEN}✓ 服务已就绪${NC}"
            break
        fi
        RETRY_COUNT=$((RETRY_COUNT + 1))
        echo -n "."
        sleep 2
    done

    if [ $RETRY_COUNT -eq $MAX_RETRY ]; then
        echo ""
        echo -e "${YELLOW}⚠ 服务启动超时，请手动检查${NC}"
        echo "   docker logs gcrf-web-admin"
    fi

    # 显示成功信息
    echo ""
    echo "========================================="
    echo -e "${GREEN}✅ 部署成功！${NC}"
    echo "========================================="
    echo ""
    echo -e "${GREEN}🌐 访问地址:${NC}"
    echo "   http://localhost:3011"
    echo ""
    echo -e "${GREEN}👤 默认账号:${NC}"
    echo "   管理员: admin / admin123"
    echo "   馆员:   librarian / lib123"
    echo "   操作员: operator / op123"
    echo ""
    echo -e "${GREEN}📋 容器状态:${NC}"
    docker ps | grep gcrf-web-admin
    echo ""
    echo -e "${YELLOW}💡 提示:${NC}"
    echo "   • 查看日志: docker logs -f gcrf-web-admin"
    echo "   • 停止服务: docker stop gcrf-web-admin"
    echo "   • 重启服务: docker restart gcrf-web-admin"
    echo ""

    # 询问是否打开浏览器
    read -p "是否在浏览器中打开系统? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        if command -v open &> /dev/null; then
            open http://localhost:3011
        elif command -v xdg-open &> /dev/null; then
            xdg-open http://localhost:3011
        elif command -v start &> /dev/null; then
            start http://localhost:3011
        else
            echo "请手动在浏览器中打开: http://localhost:3011"
        fi
    fi

else
    echo -e "${RED}❌ 容器启动失败${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}🎉 部署完成！${NC}"
echo ""
