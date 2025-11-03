#!/bin/sh
# Docker容器健康检查脚本

# 检查nginx进程是否运行
if ! pgrep -x nginx > /dev/null; then
    echo "Nginx process not found"
    exit 1
fi

# 检查健康检查端点
if ! wget --no-verbose --tries=1 --spider http://localhost/health 2>&1; then
    echo "Health check endpoint failed"
    exit 1
fi

echo "Health check passed"
exit 0
