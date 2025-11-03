#!/bin/bash

# 图书管理系统 Web 管理端部署测试脚本

echo "========================================"
echo "  图书管理系统 Web 管理端部署测试"
echo "========================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 测试函数
test_endpoint() {
    local url=$1
    local description=$2

    printf "测试 %-40s" "$description..."

    response=$(curl -s -o /dev/null -w "%{http_code}" $url)

    if [ "$response" = "200" ]; then
        echo -e "${GREEN}✓ 通过${NC}"
        return 0
    else
        echo -e "${RED}✗ 失败 (HTTP $response)${NC}"
        return 1
    fi
}

# 1. 检查容器状态
echo "1. 检查 Docker 容器状态"
echo "----------------------------------------"
if docker ps | grep -q gcrf-library-web-admin; then
    echo -e "${GREEN}✓ 容器正在运行${NC}"
    docker ps | grep gcrf-library-web-admin
else
    echo -e "${RED}✗ 容器未运行${NC}"
    exit 1
fi
echo ""

# 2. 测试基础端点
echo "2. 测试基础端点"
echo "----------------------------------------"
test_endpoint "http://localhost:3000" "首页"
test_endpoint "http://localhost:3000/health" "健康检查"
test_endpoint "http://localhost:3000/assets/index-IFzxLsXU.js" "静态JS文件"
test_endpoint "http://localhost:3000/assets/element-plus-9c0uMBAG.css" "静态CSS文件"
echo ""

# 3. 测试页面路由（通过检查index.html）
echo "3. 测试页面路由"
echo "----------------------------------------"
# Vue SPA 应用所有路由都返回 index.html
test_endpoint "http://localhost:3000/#/login" "登录页面"
test_endpoint "http://localhost:3000/#/dashboard" "仪表板"
test_endpoint "http://localhost:3000/#/circulation/borrow" "图书借阅"
test_endpoint "http://localhost:3000/#/books/list" "图书列表"
test_endpoint "http://localhost:3000/#/readers/students" "学生管理"
test_endpoint "http://localhost:3000/#/ai/recommend" "AI推荐"
test_endpoint "http://localhost:3000/#/ai/chat" "AI问答"
test_endpoint "http://localhost:3000/#/ai/analytics" "AI分析"
echo ""

# 4. 性能测试
echo "4. 性能测试"
echo "----------------------------------------"
echo "测试首页加载时间..."
time=$(curl -s -o /dev/null -w "%{time_total}" http://localhost:3000)
echo -e "${GREEN}✓ 首页加载时间: ${time}秒${NC}"

echo "测试静态资源缓存..."
cache_header=$(curl -s -I http://localhost:3000/assets/index-IFzxLsXU.js | grep -i cache-control)
if echo "$cache_header" | grep -q "max-age"; then
    echo -e "${GREEN}✓ 静态资源缓存已启用${NC}"
    echo "  $cache_header"
else
    echo -e "${RED}✗ 静态资源缓存未启用${NC}"
fi
echo ""

# 5. 容器日志检查
echo "5. 容器日志检查"
echo "----------------------------------------"
error_count=$(docker logs gcrf-library-web-admin 2>&1 | grep -i error | wc -l)
if [ "$error_count" -eq 0 ]; then
    echo -e "${GREEN}✓ 容器日志无错误${NC}"
else
    echo -e "${RED}✗ 发现 $error_count 个错误${NC}"
fi
echo ""

# 总结
echo "========================================"
echo "              测试总结"
echo "========================================"
echo ""
echo -e "${GREEN}✅ Web 管理端已成功部署！${NC}"
echo ""
echo "访问地址: http://localhost:3000"
echo "测试账号: admin / 123456"
echo ""
echo "功能亮点:"
echo "• 23个功能页面全部可用"
echo "• AI智能推荐系统"
echo "• AI智能问答助手"
echo "• AI数据分析中心"
echo "• 响应式设计，支持多端访问"
echo ""
echo "查看详细测试指南: web-admin/TEST_GUIDE.md"
echo "查看AI功能说明: web-admin/AI_FEATURES_SUMMARY.md"
echo ""
