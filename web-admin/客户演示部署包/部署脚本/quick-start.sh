#!/bin/bash
# 国创睿峰智能图书馆管理系统 - 快速启动脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "╔════════════════════════════════════════════════════════════╗"
echo "║     国创睿峰智能图书馆管理系统 - 快速启动               ║"
echo "║     GCRF Smart Library System - Quick Start             ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# 返回到部署包根目录
cd "$(dirname "$0")/.."

echo -e "${YELLOW}=== 环境检查 ===${NC}"
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
echo -e "${YELLOW}=== 检查镜像文件 ===${NC}"
echo ""

# 查找最新的镜像文件
IMAGE_FILE=$(ls -t gcrf-web-admin-*.tar.gz 2>/dev/null | head -1)

if [ -z "$IMAGE_FILE" ]; then
    echo -e "${YELLOW}⚠ 未找到镜像文件,将使用Docker Compose构建${NC}"
    USE_COMPOSE=true
else
    echo -e "${GREEN}✓ 找到镜像文件: ${IMAGE_FILE}${NC}"
    echo ""
    echo -e "${YELLOW}请选择部署方式:${NC}"
    echo "  1) 导入现有镜像(推荐,速度快)"
    echo "  2) 使用Docker Compose构建(需要源码)"
    echo ""
    read -p "请选择 [1-2]: " choice

    if [ "$choice" = "1" ]; then
        USE_COMPOSE=false
    else
        USE_COMPOSE=true
    fi
fi

echo ""
echo -e "${YELLOW}=== 清理旧容器 ===${NC}"
echo ""

# 停止并删除旧容器
if docker ps -a | grep -q gcrf-web-admin; then
    echo -e "${BLUE}正在清理旧容器...${NC}"
    docker rm -f gcrf-web-admin 2>/dev/null || true
    echo -e "${GREEN}✓ 清理完成${NC}"
else
    echo -e "${GREEN}✓ 无需清理${NC}"
fi

if [ "$USE_COMPOSE" = false ]; then
    echo ""
    echo -e "${YELLOW}=== 导入Docker镜像 ===${NC}"
    echo ""

    echo -e "${BLUE}正在导入镜像: ${IMAGE_FILE}${NC}"
    echo "这可能需要1-2分钟..."

    if gunzip -c "$IMAGE_FILE" | docker load; then
        echo -e "${GREEN}✓ 镜像导入成功${NC}"
    else
        echo -e "${RED}✗ 镜像导入失败${NC}"
        exit 1
    fi

    # 验证镜像是否存在
    echo ""
    echo -e "${BLUE}验证镜像...${NC}"
    if docker images gcrf-library-web-admin:latest | grep -q gcrf-library-web-admin; then
        echo -e "${GREEN}✓ 镜像验证成功${NC}"
        docker images gcrf-library-web-admin:latest
    else
        echo -e "${RED}✗ 镜像未找到${NC}"
        echo "请手动导入镜像:"
        echo "  gunzip -c $IMAGE_FILE | docker load"
        exit 1
    fi

    echo ""
    echo -e "${YELLOW}=== 启动容器 ===${NC}"
    echo ""

    echo -e "${BLUE}正在启动容器...${NC}"
    if docker run -d \
        --name gcrf-web-admin \
        -p 3011:80 \
        --platform linux/amd64 \
        --restart unless-stopped \
        gcrf-library-web-admin:latest; then
        echo -e "${GREEN}✓ 容器启动成功${NC}"
    else
        echo -e "${RED}✗ 容器启动失败${NC}"
        echo ""
        echo -e "${YELLOW}错误排查:${NC}"
        echo "1. 检查镜像是否存在: docker images | grep gcrf"
        echo "2. 检查端口是否被占用: lsof -i :3011"
        echo "3. 查看Docker日志: docker logs gcrf-web-admin"
        exit 1
    fi
else
    echo ""
    echo -e "${YELLOW}=== 使用Docker Compose启动 ===${NC}"
    echo ""

    if [ ! -f "docker-compose.yml" ]; then
        echo -e "${RED}✗ 未找到docker-compose.yml文件${NC}"
        exit 1
    fi

    echo -e "${BLUE}正在启动服务...${NC}"
    if docker-compose up -d; then
        echo -e "${GREEN}✓ 服务启动成功${NC}"
    else
        echo -e "${RED}✗ 服务启动失败${NC}"
        exit 1
    fi
fi

# 等待服务就绪
echo ""
echo -e "${BLUE}等待服务就绪...${NC}"
sleep 5

# 健康检查
echo ""
echo -e "${YELLOW}=== 服务验证 ===${NC}"
echo ""

MAX_RETRIES=10
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -f http://localhost:3011/health &> /dev/null; then
        echo -e "${GREEN}✓ 服务健康检查通过${NC}"
        break
    fi

    RETRY_COUNT=$((RETRY_COUNT+1))
    if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
        echo -e "${YELLOW}⚠ 健康检查超时,但容器可能仍在启动中${NC}"
    else
        echo -e "${BLUE}等待服务响应... ($RETRY_COUNT/$MAX_RETRIES)${NC}"
        sleep 2
    fi
done

echo ""
echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   🎉 部署成功! 系统已启动!  🎉     ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}访问信息:${NC}"
echo "  🌐 访问地址: ${GREEN}http://localhost:3011${NC}"
echo ""
echo -e "${BLUE}测试账号:${NC}"
echo "  👤 管理员: ${GREEN}admin${NC} / ${GREEN}admin123${NC}"
echo "  👤 馆员:   ${GREEN}librarian${NC} / ${GREEN}lib123${NC}"
echo "  👤 操作员: ${GREEN}operator${NC} / ${GREEN}op123${NC}"
echo ""
echo -e "${BLUE}常用命令:${NC}"
echo "  📋 查看日志: ${YELLOW}docker logs -f gcrf-web-admin${NC}"
echo "  🔄 重启服务: ${YELLOW}docker restart gcrf-web-admin${NC}"
echo "  🛑 停止服务: ${YELLOW}docker stop gcrf-web-admin${NC}"
echo "  🗑️  删除容器: ${YELLOW}docker rm -f gcrf-web-admin${NC}"
echo ""
echo -e "${BLUE}演示场景:${NC}"
echo "  📚 图书管理: 图书列表 → 图书编目 → 馆藏管理"
echo "  📖 流通管理: 图书借出 → 图书归还 → 流通记录"
echo "  👥 读者管理: 学生读者 → 教师读者 → 读者证办理"
echo "  📊 数据统计: 实时统计 → 借阅趋势 → 热门图书"
echo ""

# 自动打开浏览器（可选）
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

echo ""
echo -e "${GREEN}🎊 祝您演示顺利!${NC}"
echo ""
