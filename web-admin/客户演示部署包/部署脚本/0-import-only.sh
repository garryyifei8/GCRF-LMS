#!/bin/bash
# 镜像导入脚本 - 仅导入镜像，不启动容器

set -e

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "╔════════════════════════════════════════════════════════════╗"
echo "║          Docker镜像导入工具 - 仅导入镜像                 ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# 返回到部署包根目录
cd "$(dirname "$0")/.."

echo -e "${YELLOW}=== 步骤1: 环境检查 ===${NC}"
echo ""

# 检查Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}✗ Docker未安装${NC}"
    echo "请先安装Docker: https://www.docker.com/get-started"
    exit 1
fi
echo -e "${GREEN}✓ Docker已安装${NC}"

# 检查Docker服务
if ! docker info &> /dev/null; then
    echo -e "${RED}✗ Docker服务未运行${NC}"
    echo "请启动Docker Desktop"
    exit 1
fi
echo -e "${GREEN}✓ Docker服务运行中${NC}"

echo ""
echo -e "${YELLOW}=== 步骤2: 查找镜像文件 ===${NC}"
echo ""

# 查找镜像文件
IMAGE_FILE=$(ls -t gcrf-web-admin-*.tar.gz 2>/dev/null | head -1)

if [ -z "$IMAGE_FILE" ]; then
    echo -e "${RED}✗ 未找到镜像文件${NC}"
    echo ""
    echo "请确保以下文件存在:"
    echo "  gcrf-web-admin-amd64-v1.0.0.tar.gz"
    exit 1
fi

echo -e "${GREEN}✓ 找到镜像文件: ${IMAGE_FILE}${NC}"
FILE_SIZE=$(du -h "$IMAGE_FILE" | cut -f1)
echo "  文件大小: ${FILE_SIZE}"

echo ""
echo -e "${YELLOW}=== 步骤3: 导入镜像 ===${NC}"
echo ""

echo -e "${BLUE}正在导入镜像...${NC}"
echo "这可能需要1-2分钟，请耐心等待..."
echo ""

START_TIME=$(date +%s)

if gunzip -c "$IMAGE_FILE" | docker load; then
    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))

    echo ""
    echo -e "${GREEN}✓ 镜像导入成功！${NC}"
    echo "导入耗时: ${DURATION}秒"
else
    echo ""
    echo -e "${RED}✗ 镜像导入失败${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}=== 步骤4: 验证镜像 ===${NC}"
echo ""

if docker images gcrf-library-web-admin:latest | grep -q gcrf-library-web-admin; then
    echo -e "${GREEN}✓ 镜像验证成功${NC}"
    echo ""
    docker images gcrf-library-web-admin:latest
else
    echo -e "${RED}✗ 镜像未找到${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   ✅ 镜像导入完成！            ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
echo ""

echo -e "${BLUE}下一步:${NC}"
echo ""
echo "1️⃣  手动启动容器:"
echo "   ${YELLOW}docker run -d --name gcrf-web-admin -p 3011:80 --platform linux/amd64 gcrf-library-web-admin:latest${NC}"
echo ""
echo "2️⃣  或使用快速启动脚本:"
echo "   ${YELLOW}./部署脚本/quick-start.sh${NC}"
echo ""
echo "3️⃣  或使用Docker Compose:"
echo "   ${YELLOW}docker-compose up -d${NC}"
echo ""

echo -e "${GREEN}🎉 镜像已准备就绪！${NC}"
echo ""
