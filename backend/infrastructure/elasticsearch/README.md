# Elasticsearch 8.11 集群部署

## 架构说明

- **集群模式**: 3节点集群
- **节点1**: elasticsearch-node1 (端口9200) - 主节点
- **节点2**: elasticsearch-node2 (端口9201)
- **节点3**: elasticsearch-node3 (端口9202)
- **Kibana**: Web管理界面 (端口5601)

## 配置说明

### 集群配置
- **集群名称**: gcrf-es-cluster
- **密码**: gcrf_es_2024
- **内存配置**: 每节点1GB堆内存
- **安全**: 启用认证，禁用SSL（开发环境）

### 性能配置
- **Memory Lock**: 启用，防止内存交换
- **File Descriptors**: 65536
- **分片策略**: 默认自动分配

## 快速启动

```bash
# 启动Elasticsearch集群
cd backend/infrastructure/elasticsearch
docker-compose up -d

# 检查集群状态
docker-compose ps

# 查看日志
docker-compose logs -f elasticsearch-node1
```

## 验证部署

### 1. 检查集群健康状态
```bash
curl -u elastic:gcrf_es_2024 http://localhost:9200/_cluster/health?pretty
```

期待输出：
```json
{
  "cluster_name" : "gcrf-es-cluster",
  "status" : "green",
  "number_of_nodes" : 3,
  "number_of_data_nodes" : 3
}
```

### 2. 查看节点信息
```bash
curl -u elastic:gcrf_es_2024 http://localhost:9200/_cat/nodes?v
```

### 3. 访问Kibana
浏览器访问: http://localhost:5601
- 用户名: elastic
- 密码: gcrf_es_2024

## 创建索引模板

### 图书索引模板
```bash
curl -u elastic:gcrf_es_2024 -X PUT "http://localhost:9200/books" -H 'Content-Type: application/json' -d'
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "analysis": {
      "analyzer": {
        "ik_max_word_analyzer": {
          "type": "ik_max_word"
        },
        "ik_smart_analyzer": {
          "type": "ik_smart"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "id": { "type": "keyword" },
      "title": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "author": {
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "isbn": { "type": "keyword" },
      "publisher": { "type": "text" },
      "publish_date": { "type": "date" },
      "category": { "type": "keyword" },
      "description": {
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "tags": { "type": "keyword" },
      "created_at": { "type": "date" },
      "updated_at": { "type": "date" }
    }
  }
}'
```

### 读者索引模板
```bash
curl -u elastic:gcrf_es_2024 -X PUT "http://localhost:9200/readers" -H 'Content-Type: application/json' -d'
{
  "settings": {
    "number_of_shards": 2,
    "number_of_replicas": 1,
    "analysis": {
      "analyzer": {
        "ik_max_word_analyzer": {
          "type": "ik_max_word"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "id": { "type": "keyword" },
      "name": {
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "reader_number": { "type": "keyword" },
      "type": { "type": "keyword" },
      "department": { "type": "keyword" },
      "phone": { "type": "keyword" },
      "email": { "type": "keyword" },
      "created_at": { "type": "date" }
    }
  }
}'
```

## 安装IK分词器

IK分词器需要手动安装到每个节点：

```bash
# 进入容器
docker exec -it gcrf-elasticsearch-node1 bash

# 安装IK分词器
./bin/elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.11.0/elasticsearch-analysis-ik-8.11.0.zip

# 退出并重启容器
exit
docker restart gcrf-elasticsearch-node1

# 对其他两个节点重复上述步骤
```

或者使用脚本批量安装：
```bash
cd backend/infrastructure/elasticsearch
./install-ik-plugin.sh
```

## 测试IK分词器

```bash
curl -u elastic:gcrf_es_2024 -X POST "http://localhost:9200/_analyze?pretty" -H 'Content-Type: application/json' -d'
{
  "analyzer": "ik_max_word",
  "text": "图书馆管理系统"
}'
```

## 性能优化建议

1. **生产环境配置**:
   - 增加堆内存至2-4GB
   - 启用SSL安全传输
   - 配置集群快照备份

2. **索引优化**:
   - 合理设置分片数量（建议不超过节点数的3倍）
   - 使用别名管理索引版本
   - 定期清理过期数据

3. **查询优化**:
   - 使用缓存
   - 避免深度分页
   - 使用Scroll API处理大量数据

## 常用命令

```bash
# 停止服务
docker-compose down

# 查看集群状态
curl -u elastic:gcrf_es_2024 http://localhost:9200/_cluster/stats?pretty

# 查看所有索引
curl -u elastic:gcrf_es_2024 http://localhost:9200/_cat/indices?v

# 删除索引
curl -u elastic:gcrf_es_2024 -X DELETE http://localhost:9200/books

# 查看集群设置
curl -u elastic:gcrf_es_2024 http://localhost:9200/_cluster/settings?pretty
```

## 故障排查

### 1. 节点无法加入集群
- 检查网络连接: `docker network inspect gcrf-network`
- 检查节点日志: `docker logs gcrf-elasticsearch-node1`
- 确保所有节点的cluster.name一致

### 2. 集群状态为yellow
- 通常是副本分片未分配
- 检查节点数量是否足够
- 查看未分配分片: `curl -u elastic:gcrf_es_2024 http://localhost:9200/_cat/shards?v`

### 3. 内存不足
- 调整ES_JAVA_OPTS
- 增加Docker容器内存限制
- 优化索引分片配置

## 监控指标

- 集群健康状态: green/yellow/red
- 节点数量: 应为3
- 活跃分片数
- 文档总数
- 查询QPS
- 索引吞吐量

## 相关链接

- [Elasticsearch官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/8.11/index.html)
- [IK分词器GitHub](https://github.com/medcl/elasticsearch-analysis-ik)
- [Kibana用户指南](https://www.elastic.co/guide/en/kibana/8.11/index.html)
