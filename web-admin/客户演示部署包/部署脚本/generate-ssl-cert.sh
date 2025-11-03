#!/bin/bash
# 生成自签名 SSL 证书用于 HTTPS 演示

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "╔════════════════════════════════════════════════════════════╗"
echo "║       生成 SSL 自签名证书 (用于 HTTPS 演示)             ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# 返回到部署包根目录
cd "$(dirname "$0")/.."

echo -e "${YELLOW}=== 重要说明 ===${NC}"
echo ""
echo "本脚本将生成自签名 SSL 证书，用于启用 HTTPS 访问。"
echo ""
echo "⚠️  为什么需要 HTTPS?"
echo "   Mock Service Worker 使用浏览器的 Service Worker API"
echo "   出于安全原因，Service Worker 仅在以下环境可用:"
echo "   ✅ HTTPS 网站"
echo "   ✅ localhost (http://localhost)"
echo "   ❌ HTTP 的 IP 地址 (http://192.168.x.x) - 不支持"
echo ""
echo "⚠️  自签名证书的限制:"
echo "   • 浏览器会显示安全警告"
echo "   • 需要用户手动信任证书"
echo "   • 仅适合演示环境,不适合生产环境"
echo ""
read -p "是否继续生成证书? (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "操作取消"
    exit 0
fi

echo ""
echo -e "${YELLOW}=== 步骤1: 获取服务器信息 ===${NC}"
echo ""

# 获取服务器IP
echo "请输入服务器的 IP 地址或域名 (例如: 192.168.2.121)"
read -p "IP/域名: " SERVER_HOST

if [ -z "$SERVER_HOST" ]; then
    echo -e "${RED}✗ IP/域名不能为空${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}=== 步骤2: 生成证书 ===${NC}"
echo ""

# 创建 ssl 目录
mkdir -p ssl
cd ssl

# 生成私钥
echo "正在生成私钥..."
openssl genrsa -out gcrf-demo.key 2048 2>/dev/null

# 生成证书签名请求配置
cat > openssl.cnf <<EOF
[req]
default_bits = 2048
prompt = no
default_md = sha256
distinguished_name = dn
req_extensions = v3_req

[dn]
C=CN
ST=Beijing
L=Beijing
O=GCRF Library Demo
OU=IT Department
CN=${SERVER_HOST}

[v3_req]
subjectAltName = @alt_names

[alt_names]
DNS.1 = ${SERVER_HOST}
DNS.2 = localhost
IP.1 = ${SERVER_HOST}
IP.2 = 127.0.0.1
EOF

# 生成证书
echo "正在生成证书..."
openssl req -new -x509 -days 365 \
    -key gcrf-demo.key \
    -out gcrf-demo.crt \
    -config openssl.cnf \
    -extensions v3_req 2>/dev/null

# 清理临时文件
rm openssl.cnf

echo ""
echo -e "${GREEN}✓ SSL 证书生成成功！${NC}"
echo ""
echo "证书文件位置:"
echo "  私钥: $(pwd)/gcrf-demo.key"
echo "  证书: $(pwd)/gcrf-demo.crt"
echo ""

echo -e "${YELLOW}=== 步骤3: 创建 HTTPS Docker Compose 配置 ===${NC}"
echo ""

cd ..

# 创建 HTTPS 版本的 docker-compose
cat > docker-compose-https.yml <<EOF
version: '3.8'

services:
  web-admin:
    image: gcrf-library-web-admin:amd64
    container_name: gcrf-web-admin-https
    ports:
      - "3011:443"
    volumes:
      - ./ssl/gcrf-demo.crt:/etc/nginx/ssl/server.crt:ro
      - ./ssl/gcrf-demo.key:/etc/nginx/ssl/server.key:ro
      - ./nginx-https.conf:/etc/nginx/conf.d/default.conf:ro
    environment:
      - TZ=Asia/Shanghai
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--no-check-certificate", "-q", "-O", "/dev/null", "https://localhost"]
      interval: 30s
      timeout: 3s
      retries: 3
      start_period: 10s
    networks:
      - gcrf-network

networks:
  gcrf-network:
    driver: bridge
    name: gcrf-network
EOF

# 创建 HTTPS nginx 配置
cat > nginx-https.conf <<EOF
server {
    listen 443 ssl;
    listen [::]:443 ssl;
    server_name ${SERVER_HOST};

    # SSL 证书配置
    ssl_certificate /etc/nginx/ssl/server.crt;
    ssl_certificate_key /etc/nginx/ssl/server.key;

    # SSL 协议配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # 其他配置与标准版本相同
    root /usr/share/nginx/html;
    index index.html;

    # Gzip 压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css text/xml text/javascript
               application/x-javascript application/xml+rss application/json
               application/javascript image/svg+xml;

    # SPA 路由支持
    location / {
        try_files \$uri \$uri/ /index.html;
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }

    # 静态资源缓存
    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # 健康检查端点
    location /health {
        access_log off;
        return 200 "OK";
        add_header Content-Type text/plain;
    }
}
EOF

echo -e "${GREEN}✓ HTTPS 配置文件创建成功！${NC}"
echo ""

echo -e "${YELLOW}=== 步骤4: 启动 HTTPS 服务 ===${NC}"
echo ""

echo "现在可以使用以下命令启动 HTTPS 服务:"
echo ""
echo -e "${BLUE}docker-compose -f docker-compose-https.yml up -d${NC}"
echo ""

echo -e "${YELLOW}=== 步骤5: 访问系统 ===${NC}"
echo ""
echo "启动后，使用以下地址访问:"
echo -e "${GREEN}https://${SERVER_HOST}:3011${NC}"
echo ""
echo "⚠️  首次访问时，浏览器会显示安全警告（因为是自签名证书）"
echo ""
echo "解决方法:"
echo "  Chrome: 点击 '高级' -> '继续访问 ${SERVER_HOST}(不安全)'"
echo "  Firefox: 点击 '高级' -> '接受风险并继续'"
echo "  Edge: 点击 '高级' -> '继续前往 ${SERVER_HOST}(不安全)'"
echo ""

echo -e "${YELLOW}=== 完整部署命令 ===${NC}"
echo ""
echo "1. 导入镜像（如果还未导入）:"
echo "   gunzip -c gcrf-web-admin-amd64-v1.0.0.tar.gz | docker load"
echo ""
echo "2. 启动 HTTPS 服务:"
echo "   docker-compose -f docker-compose-https.yml up -d"
echo ""
echo "3. 访问系统:"
echo "   https://${SERVER_HOST}:3011"
echo ""

echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   ✅ HTTPS 证书配置完成！      ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
echo ""
