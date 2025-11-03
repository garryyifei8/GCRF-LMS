#!/bin/bash

# ==================================================================
# 国创睿峰智能图书馆管理系统 - Elasticsearch IK分词器安装脚本
# 版本: Elasticsearch 8.11.0
# ==================================================================

set -e

echo "========================================="
echo "Elasticsearch IK分词器批量安装"
echo "========================================="

IK_VERSION="8.11.0"
IK_URL="https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v${IK_VERSION}/elasticsearch-analysis-ik-${IK_VERSION}.zip"

# 节点列表
NODES=("gcrf-elasticsearch-node1" "gcrf-elasticsearch-node2" "gcrf-elasticsearch-node3")

for NODE in "${NODES[@]}"; do
    echo ""
    echo "正在为节点 ${NODE} 安装IK分词器..."

    # 检查容器是否运行
    if ! docker ps | grep -q "${NODE}"; then
        echo "警告: 容器 ${NODE} 未运行，跳过..."
        continue
    fi

    # 安装IK分词器
    docker exec ${NODE} bash -c "
        cd /usr/share/elasticsearch &&
        ./bin/elasticsearch-plugin install ${IK_URL} --batch
    "

    if [ $? -eq 0 ]; then
        echo "✓ ${NODE} IK分词器安装成功"
    else
        echo "✗ ${NODE} IK分词器安装失败"
        exit 1
    fi
done

echo ""
echo "========================================="
echo "IK分词器安装完成，正在重启节点..."
echo "========================================="

# 重启所有节点
for NODE in "${NODES[@]}"; do
    echo "重启节点: ${NODE}"
    docker restart ${NODE}
done

echo ""
echo "等待集群恢复..."
sleep 30

echo ""
echo "========================================="
echo "验证IK分词器安装"
echo "========================================="

# 测试IK分词器
echo ""
echo "测试ik_max_word分词器:"
curl -s -u elastic:gcrf_es_2024 -X POST "http://localhost:9200/_analyze?pretty" -H 'Content-Type: application/json' -d'{
  "analyzer": "ik_max_word",
  "text": "国创睿峰智能图书馆管理系统"
}'

echo ""
echo ""
echo "测试ik_smart分词器:"
curl -s -u elastic:gcrf_es_2024 -X POST "http://localhost:9200/_analyze?pretty" -H 'Content-Type: application/json' -d'{
  "analyzer": "ik_smart",
  "text": "国创睿峰智能图书馆管理系统"
}'

echo ""
echo "========================================="
echo "IK分词器安装和验证完成！"
echo "========================================="
