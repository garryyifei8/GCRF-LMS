#!/bin/bash
# Docker部署一键测试脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "╔════════════════════════════════════════════════════════════╗"
echo "║     GCRF智能图书馆管理系统 - Docker部署一键测试           ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# 返回到项目根目录
cd "$(dirname "$0")/.."

# 测试计数器
TESTS_PASSED=0
TESTS_FAILED=0

# 测试函数
run_test() {
    local test_name=$1
    local test_command=$2

    echo -ne "${YELLOW}[TEST] ${test_name}...${NC}"

    if eval "$test_command" > /dev/null 2>&1; then
        echo -e " ${GREEN}✓ PASSED${NC}"
        TESTS_PASSED=$((TESTS_PASSED+1))
        return 0
    else
        echo -e " ${RED}✗ FAILED${NC}"
        TESTS_FAILED=$((TESTS_FAILED+1))
        return 1
    fi
}

echo -e "${YELLOW}=== 环境检查 ===${NC}"
echo ""

# 检查Docker
run_test "Docker已安装" "command -v docker"
run_test "Docker服务运行中" "docker info"
run_test "Docker Compose已安装" "command -v docker-compose"

echo ""
echo -e "${YELLOW}=== 构建镜像 ===${NC}"
echo ""

# 构建镜像
echo -e "${BLUE}开始构建Docker镜像...${NC}"
if docker build -t gcrf-library-web-admin:test -f Dockerfile . ; then
    echo -e "${GREEN}✓ 镜像构建成功${NC}"
    TESTS_PASSED=$((TESTS_PASSED+1))
else
    echo -e "${RED}✗ 镜像构建失败${NC}"
    TESTS_FAILED=$((TESTS_FAILED+1))
    exit 1
fi

echo ""
echo -e "${YELLOW}=== 镜像检查 ===${NC}"
echo ""

# 检查镜像
run_test "镜像已创建" "docker images gcrf-library-web-admin:test --format '{{.Repository}}' | grep -q 'gcrf-library-web-admin'"

# 获取镜像信息
IMAGE_SIZE=$(docker images gcrf-library-web-admin:test --format "{{.Size}}")
echo -e "${BLUE}镜像大小: ${IMAGE_SIZE}${NC}"

echo ""
echo -e "${YELLOW}=== 启动容器 ===${NC}"
echo ""

# 清理旧容器
echo -e "${BLUE}清理旧容器...${NC}"
docker rm -f gcrf-web-admin-test 2>/dev/null || true

# 启动容器
echo -e "${BLUE}启动测试容器...${NC}"
if docker run -d \
    --name gcrf-web-admin-test \
    -p 3011:80 \
    gcrf-library-web-admin:test ; then
    echo -e "${GREEN}✓ 容器启动成功${NC}"
    TESTS_PASSED=$((TESTS_PASSED+1))
else
    echo -e "${RED}✗ 容器启动失败${NC}"
    TESTS_FAILED=$((TESTS_FAILED+1))
    exit 1
fi

# 等待容器就绪
echo -e "${BLUE}等待服务就绪...${NC}"
sleep 5

echo ""
echo -e "${YELLOW}=== 功能测试 ===${NC}"
echo ""

# 容器运行检查
run_test "容器正在运行" "docker ps | grep -q gcrf-web-admin-test"

# 健康检查
run_test "健康检查端点" "curl -f http://localhost:3011/health"

# 首页访问
run_test "首页可访问" "curl -I http://localhost:3011 2>&1 | grep -q '200 OK'"

# Mock Service Worker文件
run_test "MSW文件存在" "curl -I http://localhost:3011/mockServiceWorker.js 2>&1 | grep -q '200 OK'"

# Nginx进程
run_test "Nginx进程运行" "docker exec gcrf-web-admin-test pgrep -x nginx"

# 容器健康状态
echo -ne "${YELLOW}[TEST] 容器健康状态...${NC}"
sleep 10  # 等待健康检查
HEALTH_STATUS=$(docker inspect --format='{{.State.Health.Status}}' gcrf-web-admin-test 2>/dev/null || echo "none")
if [ "$HEALTH_STATUS" = "healthy" ] || [ "$HEALTH_STATUS" = "none" ]; then
    echo -e " ${GREEN}✓ PASSED (${HEALTH_STATUS})${NC}"
    TESTS_PASSED=$((TESTS_PASSED+1))
else
    echo -e " ${YELLOW}⚠ WARNING (${HEALTH_STATUS})${NC}"
fi

echo ""
echo -e "${YELLOW}=== 资源检查 ===${NC}"
echo ""

# 获取容器资源使用
STATS=$(docker stats gcrf-web-admin-test --no-stream --format "CPU: {{.CPUPerc}} | MEM: {{.MemUsage}}")
echo -e "${BLUE}资源使用: ${STATS}${NC}"

# 检查日志
echo -e "${BLUE}检查日志...${NC}"
LOGS=$(docker logs gcrf-web-admin-test 2>&1 | tail -5)
if echo "$LOGS" | grep -q "error"; then
    echo -e "${YELLOW}⚠ 发现错误日志${NC}"
    echo "$LOGS"
else
    echo -e "${GREEN}✓ 无明显错误${NC}"
fi

echo ""
echo -e "${YELLOW}=== 测试结果 ===${NC}"
echo ""

TOTAL_TESTS=$((TESTS_PASSED + TESTS_FAILED))
SUCCESS_RATE=$((TESTS_PASSED * 100 / TOTAL_TESTS))

echo -e "${BLUE}总测试数: ${TOTAL_TESTS}${NC}"
echo -e "${GREEN}通过: ${TESTS_PASSED}${NC}"
echo -e "${RED}失败: ${TESTS_FAILED}${NC}"
echo -e "${BLUE}成功率: ${SUCCESS_RATE}%${NC}"

echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║   🎉 所有测试通过! 部署成功!  🎉    ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${BLUE}访问信息:${NC}"
    echo "  🌐 访问地址: http://localhost:3011"
    echo ""
    echo -e "${BLUE}测试账号:${NC}"
    echo "  👤 管理员: admin / admin123"
    echo "  👤 馆员:   librarian / lib123"
    echo "  👤 操作员: operator / op123"
    echo ""
    echo -e "${BLUE}常用命令:${NC}"
    echo "  📋 查看日志: docker logs -f gcrf-web-admin-test"
    echo "  🔄 重启容器: docker restart gcrf-web-admin-test"
    echo "  🛑 停止容器: docker stop gcrf-web-admin-test"
    echo "  🗑️  删除容器: docker rm -f gcrf-web-admin-test"
    echo "  📦 导出镜像: ./docker/export-image.sh test"
    echo ""
    exit 0
else
    echo -e "${RED}╔════════════════════════════════════════╗${NC}"
    echo -e "${RED}║     ✗ 部分测试失败,请检查日志     ║${NC}"
    echo -e "${RED}╚════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${YELLOW}调试命令:${NC}"
    echo "  docker logs gcrf-web-admin-test"
    echo "  docker inspect gcrf-web-admin-test"
    echo "  docker exec -it gcrf-web-admin-test sh"
    echo ""
    exit 1
fi
