# Library Management System - Infrastructure Guide

## Overview

This directory contains the complete infrastructure setup for the Library Management System, including:

- MySQL 8.0 (Master-Slave Replication)
- Redis 7.0 (Caching)
- Elasticsearch 8.11 + Kibana (Search Engine)
- MinIO (Object Storage)
- RabbitMQ 3.12 (Message Queue)

## Quick Start

### Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- At least 4GB RAM available
- Ports 3306, 3307, 6379, 9200, 5601, 9000, 9001, 5672, 15672 available

### Installation

1. **Clone and navigate to infrastructure directory**

```bash
cd /path/to/library-backend/infrastructure
```

2. **Copy environment file**

```bash
cp .env.example .env
```

Edit `.env` file to customize credentials if needed.

3. **Create Docker network**

```bash
docker network create library-network
```

4. **Start all services**

```bash
docker-compose up -d
```

5. **Verify services are running**

```bash
docker-compose ps
```

All services should show status as "Up" and "healthy".

## Service Details

### MySQL (Master-Slave)

**Master Database:**
- Port: `3306`
- Root Password: `library_root_2024`
- App User: `library_app`
- App Password: `library_app_2024`

**Slave Database (Read Replica):**
- Port: `3307`
- Root Password: `library_root_2024`

**Databases Created:**
- library_auth
- library_book
- library_circulation
- library_reader
- library_system
- library_recommend
- library_nlp
- library_vision
- library_analytics
- library_notification
- library_file
- library_search

**Setup Replication:**

```bash
cd mysql
./setup-replication.sh
```

**Verify Replication:**

```bash
docker exec library-mysql-slave mysql -uroot -plibrary_root_2024 -e "SHOW SLAVE STATUS\G"
```

**Connection Strings:**

```properties
# Master (Write)
jdbc:mysql://localhost:3306/library_auth?useSSL=false&serverTimezone=Asia/Shanghai
username=library_app
password=library_app_2024

# Slave (Read)
jdbc:mysql://localhost:3307/library_auth?useSSL=false&serverTimezone=Asia/Shanghai
username=library_app
password=library_app_2024
```

### Redis

- Port: `6379`
- Password: `library_redis_2024`
- Max Memory: `1GB`
- Eviction Policy: `allkeys-lru`
- Persistence: RDB + AOF

**Connection:**

```bash
redis-cli -h localhost -p 6379 -a library_redis_2024
```

**Spring Boot Configuration:**

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: library_redis_2024
    database: 0
```

### Elasticsearch + Kibana

**Elasticsearch:**
- HTTP Port: `9200`
- Transport Port: `9300`
- Cluster Name: `library-es-cluster`
- JVM Memory: 512MB

**Kibana:**
- Port: `5601`
- URL: http://localhost:5601
- Language: Chinese (zh-CN)

**Install IK Analyzer Plugin:**

```bash
cd elasticsearch
./install-ik-plugin.sh
```

**Index Templates:**
- `library-books-*`: Book search index
- `library-readers-*`: Reader search index

**Test Elasticsearch:**

```bash
curl http://localhost:9200/_cluster/health?pretty
```

**Spring Boot Configuration:**

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
```

### MinIO (Object Storage)

- API Port: `9000`
- Console Port: `9001`
- Console URL: http://localhost:9001
- Username: `minioadmin`
- Password: `minioadmin2024`

**Buckets:**
- `avatars`: User avatars (public-read)
- `covers`: Book covers (public-read)
- `documents`: Documents (private)

**Initialize Buckets:**

```bash
cd minio
./init.sh
```

**Access URLs:**
- Console: http://localhost:9001
- API Endpoint: http://localhost:9000

**Spring Boot Configuration:**

```yaml
minio:
  endpoint: http://localhost:9000
  accessKey: minioadmin
  secretKey: minioadmin2024
  buckets:
    avatar: avatars
    cover: covers
    document: documents
```

### RabbitMQ

- AMQP Port: `5672`
- Management Port: `15672`
- Management UI: http://localhost:15672
- Username: `rabbitmq`
- Password: `rabbitmq2024`
- Virtual Host: `/library`

**Exchanges:**
- `library.topic.exchange` (topic)
- `library.direct.exchange` (direct)
- `library.fanout.exchange` (fanout)

**Queues:**
- `notification.queue`: Notification messages
- `log.queue`: System logs
- `analytics.queue`: Analytics events
- `email.queue`: Email notifications
- `sms.queue`: SMS notifications

**Initialize RabbitMQ:**

```bash
cd rabbitmq
./init.sh
```

**Spring Boot Configuration:**

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: rabbitmq
    password: rabbitmq2024
    virtual-host: /library
```

## Common Commands

### Start Services

```bash
# Start all services
docker-compose up -d

# Start specific service
docker-compose up -d mysql-master
docker-compose up -d redis
docker-compose up -d elasticsearch
docker-compose up -d minio
docker-compose up -d rabbitmq
```

### Stop Services

```bash
# Stop all services
docker-compose down

# Stop specific service
docker-compose stop mysql-master
```

### View Logs

```bash
# View all logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f mysql-master
docker-compose logs -f redis
docker-compose logs -f elasticsearch
docker-compose logs -f minio
docker-compose logs -f rabbitmq
```

### Check Service Status

```bash
# Check all services
docker-compose ps

# Check specific service health
docker-compose exec mysql-master mysqladmin -uroot -plibrary_root_2024 ping
docker-compose exec redis redis-cli ping
docker-compose exec elasticsearch curl -f http://localhost:9200/_cluster/health
docker-compose exec rabbitmq rabbitmq-diagnostics ping
```

### Restart Services

```bash
# Restart all services
docker-compose restart

# Restart specific service
docker-compose restart mysql-master
```

### Access Service Shells

```bash
# MySQL
docker-compose exec mysql-master mysql -uroot -plibrary_root_2024

# Redis
docker-compose exec redis redis-cli -a library_redis_2024

# Elasticsearch
docker-compose exec elasticsearch bash

# RabbitMQ
docker-compose exec rabbitmq rabbitmqctl status
```

## Port Mapping Summary

| Service | Port | Protocol | Description |
|---------|------|----------|-------------|
| MySQL Master | 3306 | TCP | MySQL database (write) |
| MySQL Slave | 3307 | TCP | MySQL database (read) |
| Redis | 6379 | TCP | Redis cache |
| Elasticsearch | 9200 | HTTP | Elasticsearch REST API |
| Elasticsearch | 9300 | TCP | Elasticsearch transport |
| Kibana | 5601 | HTTP | Kibana web UI |
| MinIO API | 9000 | HTTP | MinIO S3 API |
| MinIO Console | 9001 | HTTP | MinIO admin console |
| RabbitMQ AMQP | 5672 | TCP | RabbitMQ messaging |
| RabbitMQ Management | 15672 | HTTP | RabbitMQ admin UI |

## Data Persistence

All data is persisted to the host machine:

```
infrastructure/
├── mysql/
│   ├── master/data/    # MySQL master data
│   └── slave/data/     # MySQL slave data
├── redis/data/         # Redis data
├── elasticsearch/data/ # Elasticsearch data
├── minio/data/        # MinIO data
└── rabbitmq/data/     # RabbitMQ data
```

## Backup and Restore

### MySQL Backup

```bash
# Backup all databases
docker exec library-mysql-master mysqldump -uroot -plibrary_root_2024 --all-databases > backup.sql

# Backup specific database
docker exec library-mysql-master mysqldump -uroot -plibrary_root_2024 library_book > library_book_backup.sql

# Restore database
docker exec -i library-mysql-master mysql -uroot -plibrary_root_2024 < backup.sql
```

### Redis Backup

```bash
# Trigger save
docker exec library-redis redis-cli -a library_redis_2024 BGSAVE

# Copy RDB file
cp redis/data/dump.rdb redis/data/dump.rdb.backup
```

### Elasticsearch Snapshot

```bash
# Register repository
curl -X PUT "localhost:9200/_snapshot/backup_repo" -H 'Content-Type: application/json' -d'
{
  "type": "fs",
  "settings": {
    "location": "/usr/share/elasticsearch/backup"
  }
}'

# Create snapshot
curl -X PUT "localhost:9200/_snapshot/backup_repo/snapshot_1?wait_for_completion=true"
```

## Monitoring

### Health Checks

```bash
# MySQL
curl -f http://localhost:3306 || echo "MySQL is down"

# Redis
docker exec library-redis redis-cli -a library_redis_2024 ping

# Elasticsearch
curl -f http://localhost:9200/_cluster/health?pretty

# MinIO
curl -f http://localhost:9000/minio/health/live

# RabbitMQ
curl -u rabbitmq:rabbitmq2024 http://localhost:15672/api/health/checks/alarms
```

### Resource Usage

```bash
# View container stats
docker stats

# View specific container
docker stats library-mysql-master library-redis library-elasticsearch
```

## Troubleshooting

### Services Won't Start

1. Check if ports are already in use:
```bash
lsof -i :3306
lsof -i :6379
lsof -i :9200
```

2. Check Docker logs:
```bash
docker-compose logs [service-name]
```

3. Verify Docker network:
```bash
docker network ls
docker network inspect library-network
```

### MySQL Replication Issues

```bash
# Check slave status
docker exec library-mysql-slave mysql -uroot -plibrary_root_2024 -e "SHOW SLAVE STATUS\G"

# Reset replication
docker exec library-mysql-slave mysql -uroot -plibrary_root_2024 -e "STOP SLAVE; RESET SLAVE; START SLAVE;"
```

### Elasticsearch Performance

```bash
# Check cluster health
curl http://localhost:9200/_cluster/health?pretty

# Check node stats
curl http://localhost:9200/_nodes/stats?pretty

# Clear cache
curl -X POST "localhost:9200/_cache/clear"
```

### Disk Space Issues

```bash
# Check Docker disk usage
docker system df

# Clean up unused resources
docker system prune -a
```

## Security Considerations

### Development Environment

This configuration is optimized for **development only**. For production:

1. **Change all default passwords**
2. **Enable SSL/TLS for all services**
3. **Enable Elasticsearch security features**
4. **Configure proper firewall rules**
5. **Use secrets management (e.g., Docker secrets, Vault)**
6. **Enable audit logging**
7. **Implement network segmentation**
8. **Regular security updates**

### Production Deployment

For production, consider:

- Using managed cloud services (RDS, ElastiCache, etc.)
- Implementing proper HA/DR strategies
- Setting up monitoring and alerting (Prometheus, Grafana)
- Implementing backup automation
- Using container orchestration (Kubernetes)
- Implementing proper access controls and RBAC

## Performance Tuning

### MySQL

Edit `mysql/master/conf/my.cnf` and `mysql/slave/conf/my.cnf`:

```ini
innodb_buffer_pool_size = 2G  # 70-80% of available RAM
max_connections = 1000
```

### Redis

Edit `redis/redis.conf`:

```conf
maxmemory 2gb
maxmemory-policy allkeys-lru
```

### Elasticsearch

Edit `elasticsearch/config/elasticsearch.yml`:

```yaml
# Increase heap size in docker-compose.yml
ES_JAVA_OPTS: "-Xms1g -Xmx1g"
```

## Support

For issues or questions:
- Check service logs: `docker-compose logs [service]`
- Review documentation: `/docs`
- Contact: Data Engineering Team

## Version History

- **v1.0.0** (2025-10-11): Initial infrastructure setup
  - MySQL 8.0 with master-slave replication
  - Redis 7.0 with persistence
  - Elasticsearch 8.11 with IK analyzer
  - MinIO object storage
  - RabbitMQ 3.12 message queue

---

Created by: Li Si (Data Engineer)
Date: 2025-10-11
Sprint: Sprint 1 (S1.1-S1.7)
