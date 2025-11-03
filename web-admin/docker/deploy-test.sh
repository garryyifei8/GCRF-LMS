#!/bin/bash
# 测试环境部署脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${GREEN}=== GCRF Library Web Admin - 测试环境部署 ===${NC}"
echo ""

# 返回到项目根目录
cd "$(dirname "$0")/.."

# 停止并删除旧容器
echo -e "${YELLOW}步骤 1/4: 清理旧容器...${NC}"
docker-compose -f docker-compose.test.yml down 2>/dev/null || true
echo -e "${GREEN}✓ 清理完成${NC}"

# 构建新镜像
echo -e "${YELLOW}步骤 2/4: 构建镜像...${NC}"
docker-compose -f docker-compose.test.yml build --no-cache

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ 镜像构建成功${NC}"
else
    echo -e "${RED}✗ 镜像构建失败${NC}"
    exit 1
fi

# 启动容器
echo -e "${YELLOW}步骤 3/4: 启动容器...${NC}"
docker-compose -f docker-compose.test.yml up -d

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ 容器启动成功${NC}"
else
    echo -e "${RED}✗ 容器启动失败${NC}"
    exit 1
fi

# 等待服务就绪
echo -e "${YELLOW}步骤 4/4: 等待服务就绪...${NC}"
sleep 5

# 健康检查
MAX_RETRIES=10
RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -f http://localhost:3011/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 服务健康检查通过${NC}"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT+1))
    echo "等待服务就绪... ($RETRY_COUNT/$MAX_RETRIES)"
    sleep 2
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo -e "${RED}✗ 服务启动超时${NC}"
    echo "查看日志:"
    docker-compose -f docker-compose.test.yml logs
    exit 1
fi

echo ""
echo -e "${GREEN}=== 部署成功 ===${NC}"
echo ""
echo -e "${BLUE}服务信息:${NC}"
echo "  访问地址: http://localhost:3011"
echo "  容器名称: gcrf-web-admin-test"
echo ""
echo -e "${BLUE}测试账号:${NC}"
echo "  管理员: admin / admin123"
echo "  馆员:   librarian / lib123"
echo "  操作员: operator / op123"
echo ""
echo -e "${BLUE}常用命令:${NC}"
echo "  查看日志: docker-compose -f docker-compose.test.yml logs -f"
echo "  停止服务: docker-compose -f docker-compose.test.yml stop"
echo "  重启服务: docker-compose -f docker-compose.test.yml restart"
echo "  删除容器: docker-compose -f docker-compose.test.yml down"
echo ""
