# Quick Start Guide

## TL;DR - Start Everything in 5 Minutes

```bash
cd /path/to/library-backend/infrastructure

# 1. Start all services
./start.sh

# 2. Setup MySQL replication
cd mysql && ./setup-replication.sh && cd ..

# 3. Install Elasticsearch IK plugin (optional but recommended)
cd elasticsearch && ./install-ik-plugin.sh && cd ..

# 4. Initialize MinIO buckets
cd minio && ./init.sh && cd ..

# 5. Verify RabbitMQ
cd rabbitmq && ./init.sh && cd ..

# 6. Verify everything is working
./verify.sh
```

Done! All services are now running.

## Access URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| MySQL Master | localhost:3306 | root / library_root_2024 |
| MySQL Slave | localhost:3307 | root / library_root_2024 |
| Redis | localhost:6379 | password: library_redis_2024 |
| Elasticsearch | http://localhost:9200 | No auth |
| Kibana | http://localhost:5601 | No auth |
| MinIO Console | http://localhost:9001 | minioadmin / minioadmin2024 |
| RabbitMQ Management | http://localhost:15672 | rabbitmq / rabbitmq2024 |

## Test Connections

### MySQL

```bash
# Connect to master
mysql -h 127.0.0.1 -P 3306 -u library_app -plibrary_app_2024 library_auth

# Connect to slave (read-only)
mysql -h 127.0.0.1 -P 3307 -u library_app -plibrary_app_2024 library_auth
```

### Redis

```bash
redis-cli -h localhost -p 6379 -a library_redis_2024
# Test: SET test "hello"
# Test: GET test
```

### Elasticsearch

```bash
# Check cluster health
curl http://localhost:9200/_cluster/health?pretty

# Test IK analyzer
curl -X POST "localhost:9200/_analyze?pretty" -H 'Content-Type: application/json' -d'
{
  "analyzer": "ik_smart",
  "text": "图书馆管理系统"
}'
```

### MinIO

```bash
# Using MinIO Client (mc)
mc alias set local http://localhost:9000 minioadmin minioadmin2024
mc ls local
```

### RabbitMQ

```bash
# List queues
curl -u rabbitmq:rabbitmq2024 http://localhost:15672/api/queues/%2Flibrary
```

## Common Operations

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f mysql-master
docker-compose logs -f redis
docker-compose logs -f elasticsearch
```

### Stop Everything

```bash
./stop.sh
```

### Restart a Service

```bash
docker-compose restart mysql-master
docker-compose restart redis
```

## Spring Boot Configuration

Add these to your `application.yml`:

```yaml
# MySQL - Master (Write)
spring:
  datasource:
    master:
      jdbc-url: jdbc:mysql://localhost:3306/library_auth?useSSL=false&serverTimezone=Asia/Shanghai
      username: library_app
      password: library_app_2024
      driver-class-name: com.mysql.cj.jdbc.Driver

    # MySQL - Slave (Read)
    slave:
      jdbc-url: jdbc:mysql://localhost:3307/library_auth?useSSL=false&serverTimezone=Asia/Shanghai
      username: library_app
      password: library_app_2024
      driver-class-name: com.mysql.cj.jdbc.Driver

# Redis
  redis:
    host: localhost
    port: 6379
    password: library_redis_2024
    database: 0
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0

# Elasticsearch
  elasticsearch:
    uris: http://localhost:9200

# RabbitMQ
  rabbitmq:
    host: localhost
    port: 5672
    username: rabbitmq
    password: rabbitmq2024
    virtual-host: /library

# MinIO
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin2024
  buckets:
    avatar: avatars
    cover: covers
    document: documents
```

## Troubleshooting

### Port Already in Use

```bash
# Find process using port
lsof -i :3306

# Kill process
kill -9 <PID>
```

### Services Not Healthy

```bash
# Check logs
docker-compose logs [service-name]

# Restart service
docker-compose restart [service-name]
```

### Clean Start (Remove All Data)

```bash
docker-compose down -v
rm -rf mysql/*/data redis/data elasticsearch/data minio/data rabbitmq/data
./start.sh
```

## Next Steps

1. Read the full [README.md](README.md) for detailed documentation
2. Configure your Spring Boot application with the connection details above
3. Run the application and test database connectivity
4. Check the monitoring dashboards (Kibana, MinIO Console, RabbitMQ Management)

---

For detailed documentation, see [README.md](README.md)
