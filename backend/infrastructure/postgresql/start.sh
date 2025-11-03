#!/bin/bash
# ====================================================================
# PostgreSQL 集群快速启动脚本
# ====================================================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "========================================="
echo -e "${BLUE}国创睿峰智能图书馆管理系统${NC}"
echo -e "${BLUE}PostgreSQL 集群启动${NC}"
echo "========================================="

# 检查Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}错误: Docker未安装${NC}"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}错误: Docker Compose未安装${NC}"
    exit 1
fi

# 检查.env文件
if [ ! -f .env ]; then
    echo -e "${YELLOW}警告: .env文件不存在，使用默认配置${NC}"
    echo -e "${YELLOW}建议: 复制.env.example为.env并修改密码${NC}"
    cp .env.example .env
fi

# 设置脚本执行权限
chmod +x *.sh 2>/dev/null || true

# 创建备份目录
mkdir -p backup/full backup/incremental backup/wal backup/logs

echo ""
echo -e "${YELLOW}启动PostgreSQL集群...${NC}"
docker-compose up -d

echo ""
echo -e "${YELLOW}等待服务启动...${NC}"
sleep 10

# 检查服务状态
echo ""
echo -e "${YELLOW}检查服务状态...${NC}"
docker-compose ps

# 等待主库就绪
echo ""
echo -e "${YELLOW}等待主库初始化...${NC}"
RETRIES=30
while [ $RETRIES -gt 0 ]; do
    if docker exec gcrf-postgres-primary pg_isready -U postgres &>/dev/null; then
        echo -e "${GREEN}✓ 主库已就绪${NC}"
        break
    fi
    RETRIES=$((RETRIES-1))
    echo "等待主库启动... (剩余重试: $RETRIES)"
    sleep 2
done

if [ $RETRIES -eq 0 ]; then
    echo -e "${RED}✗ 主库启动超时${NC}"
    exit 1
fi

# 等待从库就绪
echo ""
echo -e "${YELLOW}等待从库初始化...${NC}"
sleep 15

# 显示数据库列表
echo ""
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}PostgreSQL集群启动成功！${NC}"
echo -e "${GREEN}=========================================${NC}"
echo ""
echo "数据库连接信息:"
echo "  主库: localhost:5432"
echo "  从库1: localhost:5433"
echo "  从库2: localhost:5434"
echo "  PgBouncer连接池: localhost:6432"
echo ""
echo "管理员账号:"
echo "  用户名: postgres"
echo "  密码: 见.env文件"
echo ""
echo "12个微服务数据库已创建:"
docker exec gcrf-postgres-primary psql -U postgres -d postgres -c "
SELECT datname FROM pg_database WHERE datname LIKE '%_service' ORDER BY datname;
" 2>/dev/null || echo "  (初始化中...)"

echo ""
echo "常用命令:"
echo "  查看日志: docker-compose logs -f postgres-primary"
echo "  健康检查: ./health-check.sh"
echo "  性能测试: ./benchmark.sh"
echo "  停止集群: docker-compose down"
echo "  完全清理: docker-compose down -v"
echo ""
echo -e "${YELLOW}提示: 首次启动可能需要1-2分钟完成数据库初始化${NC}"
echo ""
