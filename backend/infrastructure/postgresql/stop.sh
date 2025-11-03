#!/bin/bash
# ====================================================================
# PostgreSQL 集群停止脚本
# ====================================================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "========================================="
echo "PostgreSQL 集群停止"
echo "========================================="

# 检查是否保留数据
if [ "$1" = "--clean" ] || [ "$1" = "-c" ]; then
    echo -e "${RED}警告: 将删除所有数据！${NC}"
    read -p "确认删除所有数据？(yes/N) " -r
    if [ "$REPLY" = "yes" ]; then
        echo -e "${YELLOW}停止并删除所有容器、网络和数据卷...${NC}"
        docker-compose down -v
        rm -rf backup/full/* backup/incremental/* backup/wal/* backup/logs/*
        echo -e "${GREEN}✓ 集群已停止，数据已清理${NC}"
    else
        echo "操作已取消"
        exit 0
    fi
else
    echo -e "${YELLOW}停止集群（保留数据）...${NC}"
    docker-compose down
    echo -e "${GREEN}✓ 集群已停止，数据已保留${NC}"
    echo ""
    echo "提示:"
    echo "  重新启动: ./start.sh"
    echo "  完全清理: ./stop.sh --clean"
fi

echo ""
