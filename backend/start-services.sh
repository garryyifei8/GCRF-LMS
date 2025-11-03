#!/bin/bash

# GCRF Library Management System - 微服务启动脚本
# 按照依赖顺序启动各个服务

# 设置Java环境
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH=$JAVA_HOME/bin:$PATH

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
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

# 检查端口是否被占用
check_port() {
    local port=$1
    if lsof -i :$port > /dev/null 2>&1; then
        log_warn "端口 $port 已被占用，尝试停止占用进程..."
        lsof -ti :$port | xargs kill -9 2>/dev/null
        sleep 2
    fi
}

# 启动单个服务
start_service() {
    local service_name=$1
    local service_port=$2
    local wait_time=${3:-15}

    log_info "启动 $service_name (端口: $service_port)..."

    # 检查并清理端口
    check_port $service_port

    # 启动服务
    cd $service_name
    JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn spring-boot:run > logs/${service_name}.log 2>&1 &
    local pid=$!
    cd ..

    log_info "等待 $service_name 启动 (PID: $pid)..."
    sleep $wait_time

    # 检查服务是否启动成功
    if lsof -i :$service_port > /dev/null 2>&1; then
        log_info "✅ $service_name 启动成功！"
        return 0
    else
        log_error "❌ $service_name 启动失败，请检查日志: $service_name/logs/${service_name}.log"
        return 1
    fi
}

# 主函数
main() {
    log_info "=================================="
    log_info "GCRF图书馆管理系统 - 微服务启动"
    log_info "=================================="

    # 检查基础设施
    log_info "检查基础设施状态..."

    # 检查PostgreSQL
    if docker ps | grep -q gcrf-postgres-primary; then
        log_info "✅ PostgreSQL 运行中"
    else
        log_error "❌ PostgreSQL 未运行，请先启动"
        exit 1
    fi

    # 检查Redis
    if docker ps | grep -q gcrf-redis-master; then
        log_info "✅ Redis 运行中"
    else
        log_error "❌ Redis 未运行，请先启动"
        exit 1
    fi

    # 检查Nacos
    if docker ps | grep -q gcrf-nacos-server; then
        log_info "✅ Nacos 运行中"
    else
        log_error "❌ Nacos 未运行，请先启动"
        exit 1
    fi

    log_info "所有基础设施就绪，开始启动微服务..."
    echo ""

    # 创建日志目录
    for service in gateway-service auth-service book-service reader-service circulation-service; do
        mkdir -p $service/logs
    done

    # 按顺序启动服务
    # 1. Gateway (核心路由)
    start_service "gateway-service" 8080 20 || exit 1
    echo ""

    # 2. Auth Service (认证服务)
    start_service "auth-service" 8081 15 || exit 1
    echo ""

    # 3. Book Service (图书服务)
    start_service "book-service" 8082 15 || exit 1
    echo ""

    # 4. Reader Service (读者服务)
    start_service "reader-service" 8084 15 || exit 1
    echo ""

    # 5. Circulation Service (流通服务)
    start_service "circulation-service" 8083 15 || exit 1
    echo ""

    log_info "=================================="
    log_info "所有服务启动完成！"
    log_info "=================================="
    echo ""
    log_info "服务访问地址："
    log_info "  - API Gateway:   http://localhost:8080"
    log_info "  - Auth Service:  http://localhost:8081"
    log_info "  - Book Service:  http://localhost:8082"
    log_info "  - Circulation:   http://localhost:8083"
    log_info "  - Reader Service: http://localhost:8084"
    log_info "  - Nacos Console: http://localhost:8848/nacos (nacos/nacos)"
    echo ""
    log_info "查看日志："
    log_info "  tail -f gateway-service/logs/gateway-service.log"
    log_info "  tail -f auth-service/logs/auth-service.log"
    echo ""
}

# 执行主函数
main
