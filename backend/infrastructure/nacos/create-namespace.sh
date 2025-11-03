#!/bin/bash

# Nacos命名空间自动创建脚本
# 作者: 王五
# 日期: 2025-10-11
# 描述: 自动创建dev、test、prod三个命名空间

set -e

# 配置参数
NACOS_SERVER="http://localhost:8848"
NACOS_USERNAME="nacos"
NACOS_PASSWORD="nacos"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 等待Nacos启动
wait_for_nacos() {
    log_info "等待Nacos服务启动..."
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if curl -f -s "${NACOS_SERVER}/nacos/v1/console/health/readiness" > /dev/null 2>&1; then
            log_info "Nacos服务已就绪"
            return 0
        fi

        log_warn "Nacos未就绪，等待中... (${attempt}/${max_attempts})"
        sleep 2
        attempt=$((attempt + 1))
    done

    log_error "Nacos服务启动超时"
    return 1
}

# 获取Access Token
get_access_token() {
    log_info "获取Access Token..."

    local response=$(curl -s -X POST \
        "${NACOS_SERVER}/nacos/v1/auth/login" \
        -d "username=${NACOS_USERNAME}&password=${NACOS_PASSWORD}")

    ACCESS_TOKEN=$(echo "$response" | grep -o '"accessToken":"[^"]*' | sed 's/"accessToken":"//')

    if [ -z "$ACCESS_TOKEN" ]; then
        log_error "获取Access Token失败"
        return 1
    fi

    log_info "Access Token获取成功"
    return 0
}

# 创建命名空间
create_namespace() {
    local namespace_id=$1
    local namespace_name=$2
    local namespace_desc=$3

    log_info "创建命名空间: ${namespace_name} (${namespace_id})"

    local response=$(curl -s -X POST \
        "${NACOS_SERVER}/nacos/v1/console/namespaces" \
        -d "customNamespaceId=${namespace_id}" \
        -d "namespaceName=${namespace_name}" \
        -d "namespaceDesc=${namespace_desc}" \
        -d "accessToken=${ACCESS_TOKEN}")

    if echo "$response" | grep -q "true"; then
        log_info "命名空间 ${namespace_name} 创建成功"
        return 0
    elif echo "$response" | grep -q "already"; then
        log_warn "命名空间 ${namespace_name} 已存在"
        return 0
    else
        log_error "命名空间 ${namespace_name} 创建失败: ${response}"
        return 1
    fi
}

# 列出所有命名空间
list_namespaces() {
    log_info "当前所有命名空间:"

    local response=$(curl -s -X GET \
        "${NACOS_SERVER}/nacos/v1/console/namespaces?accessToken=${ACCESS_TOKEN}")

    echo "$response" | grep -o '"namespace":"[^"]*","namespaceShowName":"[^"]*"' | \
        sed 's/"namespace":"\([^"]*\)","namespaceShowName":"\([^"]*\)"/  - \1: \2/' || true
}

# 主函数
main() {
    echo "================================================"
    echo "  Nacos命名空间自动创建脚本"
    echo "  服务器: ${NACOS_SERVER}"
    echo "================================================"
    echo ""

    # 等待Nacos启动
    if ! wait_for_nacos; then
        exit 1
    fi

    echo ""

    # 获取Access Token
    if ! get_access_token; then
        exit 1
    fi

    echo ""

    # 创建命名空间
    create_namespace "dev" "开发环境" "开发环境配置，用于本地开发和调试"
    create_namespace "test" "测试环境" "测试环境配置，用于功能测试和集成测试"
    create_namespace "prod" "生产环境" "生产环境配置，正式上线使用"

    echo ""

    # 列出所有命名空间
    list_namespaces

    echo ""
    log_info "命名空间创建完成!"
    echo ""
    echo "访问Nacos控制台: ${NACOS_SERVER}/nacos"
    echo "用户名: ${NACOS_USERNAME}"
    echo "密码: ${NACOS_PASSWORD}"
    echo "================================================"
}

# 执行主函数
main
