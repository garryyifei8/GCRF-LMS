#!/bin/bash
# ====================================================================
# 配置文件验证脚本
# 用途: 验证所有配置文件的语法正确性
# ====================================================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ERRORS=0

echo "========================================="
echo "PostgreSQL 配置文件验证"
echo "========================================="

# 检查Docker和Docker Compose
echo -n "检查Docker... "
if command -v docker &> /dev/null; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗ Docker未安装${NC}"
    ERRORS=$((ERRORS+1))
fi

echo -n "检查Docker Compose... "
if command -v docker-compose &> /dev/null; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗ Docker Compose未安装${NC}"
    ERRORS=$((ERRORS+1))
fi

# 检查必需文件
echo ""
echo "检查必需文件:"
FILES=(
    "docker-compose.yml"
    "postgresql.conf"
    "pg_hba.conf"
    "pgbouncer.ini"
    "init-db.sql"
    "init-replication.sh"
    "setup-replica.sh"
    "backup-script.sh"
    "health-check.sh"
    "benchmark.sh"
    "start.sh"
    "stop.sh"
)

for file in "${FILES[@]}"; do
    echo -n "  ${file}... "
    if [ -f "${file}" ]; then
        echo -e "${GREEN}✓${NC}"
    else
        echo -e "${RED}✗ 缺失${NC}"
        ERRORS=$((ERRORS+1))
    fi
done

# 检查脚本执行权限
echo ""
echo "检查脚本执行权限:"
SCRIPTS=(
    "init-replication.sh"
    "setup-replica.sh"
    "backup-script.sh"
    "health-check.sh"
    "benchmark.sh"
    "start.sh"
    "stop.sh"
    "validate-config.sh"
)

for script in "${SCRIPTS[@]}"; do
    echo -n "  ${script}... "
    if [ -x "${script}" ]; then
        echo -e "${GREEN}✓ 可执行${NC}"
    else
        echo -e "${YELLOW}⚠ 不可执行${NC}"
        chmod +x "${script}" 2>/dev/null && echo "    已自动修复"
    fi
done

# 验证docker-compose.yml语法
echo ""
echo -n "验证docker-compose.yml语法... "
if docker-compose config > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗ 语法错误${NC}"
    ERRORS=$((ERRORS+1))
fi

# 检查备份目录
echo ""
echo "检查备份目录:"
BACKUP_DIRS=(
    "backup/full"
    "backup/incremental"
    "backup/wal"
    "backup/logs"
)

for dir in "${BACKUP_DIRS[@]}"; do
    echo -n "  ${dir}... "
    if [ -d "${dir}" ]; then
        echo -e "${GREEN}✓${NC}"
    else
        echo -e "${YELLOW}⚠ 不存在${NC}"
        mkdir -p "${dir}" && echo "    已自动创建"
    fi
done

# 检查.env文件
echo ""
echo -n "检查.env文件... "
if [ -f ".env" ]; then
    echo -e "${GREEN}✓ 存在${NC}"
else
    echo -e "${YELLOW}⚠ 不存在${NC}"
    echo "  提示: 复制.env.example为.env"
    if [ -f ".env.example" ]; then
        echo "  运行: cp .env.example .env"
    fi
fi

# 检查SQL语法（基础检查）
echo ""
echo -n "检查init-db.sql基础语法... "
if grep -q "CREATE DATABASE" init-db.sql && \
   grep -q "CREATE USER" init-db.sql && \
   grep -q "CREATE EXTENSION" init-db.sql; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗ 可能存在问题${NC}"
    ERRORS=$((ERRORS+1))
fi

# 统计信息
echo ""
echo "========================================="
if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✓ 所有检查通过！${NC}"
    echo ""
    echo "下一步操作:"
    echo "  1. 复制环境变量: cp .env.example .env"
    echo "  2. 修改密码: vim .env"
    echo "  3. 启动集群: ./start.sh"
    echo "  4. 健康检查: ./health-check.sh"
    echo "  5. 性能测试: ./benchmark.sh"
    exit 0
else
    echo -e "${RED}✗ 发现 ${ERRORS} 个错误${NC}"
    echo "请修复错误后重新运行验证"
    exit 1
fi
