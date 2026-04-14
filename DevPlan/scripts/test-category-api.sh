#!/bin/bash

# 分类管理API测试脚本
# Date: 2025-11-12

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 测试计数
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_test() {
    echo -e "${YELLOW}[Test $((TOTAL_TESTS + 1))] $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
    ((PASSED_TESTS++))
    ((TOTAL_TESTS++))
}

print_failure() {
    echo -e "${RED}❌ $1${NC}"
    echo -e "${RED}   Response: $2${NC}"
    ((FAILED_TESTS++))
    ((TOTAL_TESTS++))
}

# 获取Token
TOKEN=$(cat /tmp/token.txt)
if [ -z "$TOKEN" ]; then
    echo -e "${RED}❌ 无法获取Token，请先登录${NC}"
    exit 1
fi

BASE_URL="http://localhost:8082"

print_header "分类管理API测试"
echo "Base URL: $BASE_URL"
echo "Token: ${TOKEN:0:30}..."
echo ""

# ========================================
# Test 1: 获取所有分类（树形结构）
# ========================================
print_test "获取分类树"
RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
    "$BASE_URL/api/v1/books/categories?treeMode=true")

CODE=$(echo $RESPONSE | jq -r '.code // 0')
if [ "$CODE" -eq 200 ]; then
    COUNT=$(echo $RESPONSE | jq -r '.data | length')
    print_success "获取分类树成功 (共 $COUNT 个顶级分类)"
else
    print_failure "获取分类树失败" "$RESPONSE"
fi

# ========================================
# Test 2: 获取分类列表（平铺）
# ========================================
print_test "获取分类列表（平铺）"
RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
    "$BASE_URL/api/v1/books/categories?treeMode=false")

CODE=$(echo $RESPONSE | jq -r '.code // 0')
if [ "$CODE" -eq 200 ]; then
    COUNT=$(echo $RESPONSE | jq -r '.data | length')
    print_success "获取分类列表成功 (共 $COUNT 个分类)"
else
    print_failure "获取分类列表失败" "$RESPONSE"
fi

# ========================================
# Test 3: 创建新分类
# ========================================
print_test "创建新分类"
RESPONSE=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "categoryName": "测试分类",
        "categoryCode": "TEST001",
        "description": "这是一个测试分类",
        "sortOrder": 999,
        "status": "ACTIVE"
    }' \
    "$BASE_URL/api/v1/books/categories")

CODE=$(echo $RESPONSE | jq -r '.code // 0')
if [ "$CODE" -eq 200 ]; then
    CATEGORY_ID=$(echo $RESPONSE | jq -r '.data.id')
    CATEGORY_NAME=$(echo $RESPONSE | jq -r '.data.categoryName')
    print_success "创建分类成功 (ID: $CATEGORY_ID, 名称: $CATEGORY_NAME)"
else
    print_failure "创建分类失败" "$RESPONSE"
    CATEGORY_ID=""
fi

# ========================================
# Test 4: 获取分类详情
# ========================================
if [ -n "$CATEGORY_ID" ]; then
    print_test "获取分类详情 (ID: $CATEGORY_ID)"
    RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/api/v1/books/categories/$CATEGORY_ID")

    CODE=$(echo $RESPONSE | jq -r '.code // 0')
    if [ "$CODE" -eq 200 ]; then
        NAME=$(echo $RESPONSE | jq -r '.data.categoryName')
        print_success "获取分类详情成功 (名称: $NAME)"
    else
        print_failure "获取分类详情失败" "$RESPONSE"
    fi
else
    print_test "获取分类详情 (SKIPPED - 无分类ID)"
    print_failure "获取分类详情跳过" "未创建分类"
fi

# ========================================
# Test 5: 更新分类
# ========================================
if [ -n "$CATEGORY_ID" ]; then
    print_test "更新分类 (ID: $CATEGORY_ID)"
    RESPONSE=$(curl -s -X PUT -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "categoryName": "测试分类（已更新）",
            "description": "这是更新后的测试分类",
            "status": "ACTIVE"
        }' \
        "$BASE_URL/api/v1/books/categories/$CATEGORY_ID")

    CODE=$(echo $RESPONSE | jq -r '.code // 0')
    if [ "$CODE" -eq 200 ]; then
        NAME=$(echo $RESPONSE | jq -r '.data.categoryName')
        print_success "更新分类成功 (新名称: $NAME)"
    else
        print_failure "更新分类失败" "$RESPONSE"
    fi
else
    print_test "更新分类 (SKIPPED - 无分类ID)"
    print_failure "更新分类跳过" "未创建分类"
fi

# ========================================
# Test 6: 创建子分类
# ========================================
if [ -n "$CATEGORY_ID" ]; then
    print_test "创建子分类 (父ID: $CATEGORY_ID)"
    RESPONSE=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"parentId\": $CATEGORY_ID,
            \"categoryName\": \"测试子分类\",
            \"categoryCode\": \"TEST001-001\",
            \"description\": \"这是一个测试子分类\",
            \"sortOrder\": 1,
            \"status\": \"ACTIVE\"
        }" \
        "$BASE_URL/api/v1/books/categories")

    CODE=$(echo $RESPONSE | jq -r '.code // 0')
    if [ "$CODE" -eq 200 ]; then
        CHILD_ID=$(echo $RESPONSE | jq -r '.data.id')
        CHILD_NAME=$(echo $RESPONSE | jq -r '.data.categoryName')
        LEVEL=$(echo $RESPONSE | jq -r '.data.level')
        print_success "创建子分类成功 (ID: $CHILD_ID, 名称: $CHILD_NAME, 层级: $LEVEL)"
    else
        print_failure "创建子分类失败" "$RESPONSE"
        CHILD_ID=""
    fi
else
    print_test "创建子分类 (SKIPPED - 无父分类ID)"
    print_failure "创建子分类跳过" "未创建父分类"
    CHILD_ID=""
fi

# ========================================
# Test 7: 删除子分类
# ========================================
if [ -n "$CHILD_ID" ]; then
    print_test "删除子分类 (ID: $CHILD_ID)"
    RESPONSE=$(curl -s -X DELETE -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/api/v1/books/categories/$CHILD_ID")

    CODE=$(echo $RESPONSE | jq -r '.code // 0')
    if [ "$CODE" -eq 200 ]; then
        print_success "删除子分类成功"
    else
        print_failure "删除子分类失败" "$RESPONSE"
    fi
else
    print_test "删除子分类 (SKIPPED - 无子分类ID)"
    print_failure "删除子分类跳过" "未创建子分类"
fi

# ========================================
# Test 8: 删除父分类
# ========================================
if [ -n "$CATEGORY_ID" ]; then
    print_test "删除父分类 (ID: $CATEGORY_ID)"
    RESPONSE=$(curl -s -X DELETE -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/api/v1/books/categories/$CATEGORY_ID")

    CODE=$(echo $RESPONSE | jq -r '.code // 0')
    if [ "$CODE" -eq 200 ]; then
        print_success "删除父分类成功"
    else
        print_failure "删除父分类失败" "$RESPONSE"
    fi
else
    print_test "删除父分类 (SKIPPED - 无分类ID)"
    print_failure "删除父分类跳过" "未创建分类"
fi

# ========================================
# 测试总结
# ========================================
print_header "测试总结"
echo ""
echo "总测试数: $TOTAL_TESTS"
echo -e "${GREEN}通过: $PASSED_TESTS${NC}"
echo -e "${RED}失败: $FAILED_TESTS${NC}"

if [ $TOTAL_TESTS -gt 0 ]; then
    SUCCESS_RATE=$(echo "scale=2; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)
    echo "成功率: ${SUCCESS_RATE}%"
fi

echo ""
if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}🎉 所有测试通过！${NC}"
    exit 0
else
    echo -e "${RED}⚠️  部分测试失败，请查看上面的详细信息。${NC}"
    exit 1
fi
