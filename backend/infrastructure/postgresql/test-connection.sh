#!/bin/bash
# ====================================================================
# 数据库连接测试脚本
# 用途: 测试所有12个微服务数据库的连接
# ====================================================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-6432}"  # 默认使用PgBouncer

echo "========================================="
echo "数据库连接测试"
echo "========================================="
echo "连接方式: ${PGHOST}:${PGPORT} (通过PgBouncer)"
echo ""

# 数据库列表
declare -A databases=(
    ["auth_service"]="auth_user:auth_pass_2024"
    ["book_service"]="book_user:book_pass_2024"
    ["reader_service"]="reader_user:reader_pass_2024"
    ["circulation_service"]="circulation_user:circulation_pass_2024"
    ["system_service"]="system_user:system_pass_2024"
    ["recommend_service"]="recommend_user:recommend_pass_2024"
    ["nlp_service"]="nlp_user:nlp_pass_2024"
    ["vision_service"]="vision_user:vision_pass_2024"
    ["analytics_service"]="analytics_user:analytics_pass_2024"
    ["notification_service"]="notification_user:notification_pass_2024"
    ["file_service"]="file_user:file_pass_2024"
    ["search_service"]="search_user:search_pass_2024"
)

SUCCESS=0
FAILED=0

# 测试每个数据库
for db in "${!databases[@]}"; do
    IFS=':' read -r user password <<< "${databases[$db]}"
    echo -n "测试 ${db} (用户: ${user})... "

    export PGPASSWORD="${password}"
    if psql -h "${PGHOST}" -p "${PGPORT}" -U "${user}" -d "${db}" -c "SELECT 1;" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 成功${NC}"
        SUCCESS=$((SUCCESS+1))
    else
        echo -e "${RED}✗ 失败${NC}"
        FAILED=$((FAILED+1))
    fi
done

# 测试扩展
echo ""
echo "========================================="
echo "扩展功能测试"
echo "========================================="

export PGPASSWORD="auth_pass_2024"

echo -n "测试 uuid-ossp 扩展... "
if psql -h "${PGHOST}" -p "${PGPORT}" -U auth_user -d auth_service -c "SELECT uuid_generate_v4();" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 可用${NC}"
else
    echo -e "${RED}✗ 不可用${NC}"
fi

echo -n "测试 pg_trgm 扩展... "
if psql -h "${PGHOST}" -p "${PGPORT}" -U auth_user -d auth_service -c "SELECT similarity('hello', 'hallo');" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 可用${NC}"
else
    echo -e "${RED}✗ 不可用${NC}"
fi

# 测试连接池
echo ""
echo "========================================="
echo "PgBouncer连接池状态"
echo "========================================="

export PGPASSWORD="${POSTGRES_PASSWORD:-gcrf_secure_2024}"
psql -h "${PGHOST}" -p "${PGPORT}" -U postgres -d pgbouncer -c "SHOW POOLS;" 2>/dev/null || echo "无法连接到PgBouncer管理接口"

# 汇总
echo ""
echo "========================================="
echo "测试结果汇总"
echo "========================================="
echo "成功: ${SUCCESS}/12"
echo "失败: ${FAILED}/12"

if [ ${FAILED} -eq 0 ]; then
    echo -e "${GREEN}✓ 所有数据库连接正常！${NC}"
    exit 0
else
    echo -e "${RED}✗ 部分数据库连接失败${NC}"
    exit 1
fi
