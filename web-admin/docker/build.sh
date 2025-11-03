#!/bin/bash
# Docker镜像构建脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 配置
IMAGE_NAME="gcrf-library-web-admin"
VERSION=${1:-latest}
REGISTRY=${REGISTRY:-""}

echo -e "${GREEN}=== GCRF Library Web Admin - Docker Build ===${NC}"
echo ""

# 检查Docker是否安装
if ! command -v docker &> /dev/null; then
    echo -e "${RED}错误: Docker未安装${NC}"
    exit 1
fi

echo -e "${YELLOW}构建信息:${NC}"
echo "  镜像名称: ${IMAGE_NAME}"
echo "  版本标签: ${VERSION}"
echo "  镜像仓库: ${REGISTRY:-本地}"
echo ""

# 返回到项目根目录
cd "$(dirname "$0")/.."

# 构建镜像
echo -e "${YELLOW}步骤 1/3: 构建Docker镜像...${NC}"
if [ -n "$REGISTRY" ]; then
    FULL_IMAGE_NAME="${REGISTRY}/${IMAGE_NAME}:${VERSION}"
else
    FULL_IMAGE_NAME="${IMAGE_NAME}:${VERSION}"
fi

docker build \
    --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
    --build-arg VERSION=${VERSION} \
    -t ${FULL_IMAGE_NAME} \
    -f Dockerfile \
    .

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ 镜像构建成功${NC}"
else
    echo -e "${RED}✗ 镜像构建失败${NC}"
    exit 1
fi

# 同时打上latest标签
if [ "$VERSION" != "latest" ]; then
    echo -e "${YELLOW}步骤 2/3: 添加latest标签...${NC}"
    if [ -n "$REGISTRY" ]; then
        docker tag ${FULL_IMAGE_NAME} ${REGISTRY}/${IMAGE_NAME}:latest
    else
        docker tag ${FULL_IMAGE_NAME} ${IMAGE_NAME}:latest
    fi
fi

# 显示镜像信息
echo -e "${YELLOW}步骤 3/3: 镜像信息${NC}"
docker images ${IMAGE_NAME} --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"

echo ""
echo -e "${GREEN}=== 构建完成 ===${NC}"
echo ""
echo "下一步操作:"
echo "  1. 测试镜像: docker run -d -p 3011:80 --name gcrf-test ${FULL_IMAGE_NAME}"
echo "  2. 查看日志: docker logs -f gcrf-test"
echo "  3. 健康检查: docker inspect --format='{{json .State.Health}}' gcrf-test"
if [ -n "$REGISTRY" ]; then
    echo "  4. 推送镜像: docker push ${FULL_IMAGE_NAME}"
fi
echo "  5. 保存镜像: docker save ${FULL_IMAGE_NAME} -o gcrf-web-admin-${VERSION}.tar"
echo ""
