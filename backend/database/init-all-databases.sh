#!/bin/bash

# =========================================
# 数据库初始化脚本
# 用途: 创建所有微服务数据库并执行表结构脚本
# =========================================

set -e  # 遇到错误立即退出

# 数据库连接配置
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_USER=${DB_USER:-postgres}
DB_PASSWORD=${DB_PASSWORD:-gcrf_secure_2024}

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}国创睿峰图书馆管理系统 - 数据库初始化${NC}"
echo -e "${GREEN}=========================================${NC}"
echo ""

# 数据库列表(Sprint 2需要的数据库)
DATABASES=(
    "auth_service"
    "book_service"
    "circulation_service"
    "reader_service"
)

# 创建数据库函数
create_database() {
    local db_name=$1
    echo -e "${YELLOW}[1/2] 创建数据库: ${db_name}${NC}"

    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d postgres -c \
        "SELECT 1 FROM pg_database WHERE datname = '$db_name'" | grep -q 1 || \
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d postgres -c \
        "CREATE DATABASE $db_name ENCODING 'UTF8' LC_COLLATE='en_US.utf8' LC_CTYPE='en_US.utf8';"

    echo -e "${GREEN}✓ 数据库 $db_name 创建成功${NC}"
}

# 执行SQL脚本函数
execute_schema() {
    local db_name=$1
    local schema_file=$2

    if [ -f "$schema_file" ]; then
        echo -e "${YELLOW}[2/2] 执行表结构脚本: ${schema_file}${NC}"
        PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $db_name -f $schema_file
        echo -e "${GREEN}✓ 表结构创建成功${NC}"
    else
        echo -e "${RED}✗ SQL脚本不存在: ${schema_file}${NC}"
        return 1
    fi
}

# 获取当前脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCHEMA_DIR="${SCRIPT_DIR}/schema"

echo -e "${YELLOW}SQL脚本目录: ${SCHEMA_DIR}${NC}"
echo ""

# 初始化计数器
SUCCESS_COUNT=0
FAIL_COUNT=0

# 遍历数据库列表并初始化
for i in "${!DATABASES[@]}"; do
    db_name="${DATABASES[$i]}"
    schema_file="${SCHEMA_DIR}/0$((i+1))_${db_name}.sql"

    echo -e "${GREEN}=========================================${NC}"
    echo -e "${GREEN}[$((i+1))/${#DATABASES[@]}] 初始化数据库: ${db_name}${NC}"
    echo -e "${GREEN}=========================================${NC}"

    # 创建数据库
    if create_database "$db_name"; then
        # 执行表结构脚本
        if execute_schema "$db_name" "$schema_file"; then
            ((SUCCESS_COUNT++))
            echo -e "${GREEN}✓ $db_name 初始化完成${NC}"
        else
            ((FAIL_COUNT++))
            echo -e "${RED}✗ $db_name 表结构创建失败${NC}"
        fi
    else
        ((FAIL_COUNT++))
        echo -e "${RED}✗ $db_name 数据库创建失败${NC}"
    fi

    echo ""
done

# 输出统计信息
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}初始化完成统计${NC}"
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}成功: ${SUCCESS_COUNT}/${#DATABASES[@]}${NC}"
if [ $FAIL_COUNT -gt 0 ]; then
    echo -e "${RED}失败: ${FAIL_COUNT}/${#DATABASES[@]}${NC}"
fi
echo ""

# 验证数据库和表
echo -e "${YELLOW}验证数据库和表...${NC}"
for db_name in "${DATABASES[@]}"; do
    echo -e "${YELLOW}数据库 ${db_name} 的表:${NC}"
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $db_name -c \
        "SELECT schemaname, tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;" | head -20
    echo ""
done

if [ $FAIL_COUNT -eq 0 ]; then
    echo -e "${GREEN}✓ 所有数据库初始化成功!${NC}"
    exit 0
else
    echo -e "${RED}✗ 部分数据库初始化失败,请检查错误信息${NC}"
    exit 1
fi
