#!/bin/bash
# Docker镜像导出脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 配置
IMAGE_NAME="gcrf-library-web-admin"
VERSION=${1:-latest}
OUTPUT_DIR=${2:-./}

echo -e "${GREEN}=== GCRF Library Web Admin - 镜像导出 ===${NC}"
echo ""

# 检查镜像是否存在
if ! docker images ${IMAGE_NAME}:${VERSION} --format "{{.Repository}}" | grep -q "${IMAGE_NAME}"; then
    echo -e "${RED}错误: 镜像 ${IMAGE_NAME}:${VERSION} 不存在${NC}"
    echo "请先构建镜像: ./docker/build.sh ${VERSION}"
    exit 1
fi

# 创建输出目录
mkdir -p ${OUTPUT_DIR}

# 生成文件名
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
FILENAME="gcrf-web-admin-${VERSION}-${TIMESTAMP}.tar.gz"
OUTPUT_FILE="${OUTPUT_DIR}/${FILENAME}"

echo -e "${YELLOW}导出信息:${NC}"
echo "  镜像名称: ${IMAGE_NAME}:${VERSION}"
echo "  输出文件: ${OUTPUT_FILE}"
echo ""

# 导出镜像
echo -e "${YELLOW}步骤 1/3: 导出Docker镜像...${NC}"
docker save ${IMAGE_NAME}:${VERSION} | gzip > ${OUTPUT_FILE}

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ 镜像导出成功${NC}"
else
    echo -e "${RED}✗ 镜像导出失败${NC}"
    exit 1
fi

# 计算文件大小和MD5
echo -e "${YELLOW}步骤 2/3: 生成校验信息...${NC}"
FILE_SIZE=$(du -h ${OUTPUT_FILE} | cut -f1)
MD5_SUM=$(md5sum ${OUTPUT_FILE} | cut -d' ' -f1)

echo -e "${GREEN}✓ 校验信息生成完成${NC}"

# 生成元数据文件
echo -e "${YELLOW}步骤 3/3: 生成元数据文件...${NC}"
METADATA_FILE="${OUTPUT_FILE}.meta.txt"

cat > ${METADATA_FILE} <<EOF
GCRF Library Web Admin - Docker镜像导出信息
=============================================

镜像信息:
  名称: ${IMAGE_NAME}
  版本: ${VERSION}
  导出时间: $(date '+%Y-%m-%d %H:%M:%S')

文件信息:
  文件名: ${FILENAME}
  大小: ${FILE_SIZE}
  MD5: ${MD5_SUM}

导入命令:
  gunzip -c ${FILENAME} | docker load

部署命令:
  docker run -d -p 3011:80 --name gcrf-web-admin ${IMAGE_NAME}:${VERSION}

测试账号:
  管理员: admin / admin123
  馆员: librarian / lib123
  操作员: operator / op123

访问地址:
  http://localhost:3011

技术支持:
  GCRF技术团队
EOF

echo -e "${GREEN}✓ 元数据文件生成完成${NC}"

echo ""
echo -e "${GREEN}=== 导出完成 ===${NC}"
echo ""
echo "导出文件:"
echo "  镜像文件: ${OUTPUT_FILE}"
echo "  元数据: ${METADATA_FILE}"
echo ""
echo "文件信息:"
echo "  大小: ${FILE_SIZE}"
echo "  MD5: ${MD5_SUM}"
echo ""
echo "下一步操作:"
echo "  1. 将文件传输到目标服务器"
echo "  2. 在目标服务器上导入镜像: gunzip -c ${FILENAME} | docker load"
echo "  3. 运行容器: docker run -d -p 3011:80 ${IMAGE_NAME}:${VERSION}"
echo ""
