# PostgreSQL 集群快速启动指南

## 5分钟快速上手

### 1. 启动集群

```bash
cd backend/infrastructure/postgresql
./start.sh
```

等待1-2分钟完成初始化。

### 2. 验证安装

```bash
# 健康检查
./health-check.sh

# 查看数据库列表
docker exec -it gcrf-postgres-primary psql -U postgres -c "\l"
```

### 3. 连接数据库

```bash
# 连接认证服务数据库
psql -h localhost -p 5432 -U auth_user -d auth_service
# 密码: auth_pass_2024

# 或通过PgBouncer连接池
psql -h localhost -p 6432 -U auth_user -d auth_service
```

### 4. (可选) 性能测试

```bash
./benchmark.sh
```

---

## 常用命令

```bash
# 启动集群
./start.sh

# 停止集群（保留数据）
./stop.sh

# 停止并清理所有数据
./stop.sh --clean

# 健康检查
./health-check.sh

# 性能测试
./benchmark.sh

# 查看主库日志
docker-compose logs -f postgres-primary

# 查看所有服务状态
docker-compose ps
```

---

## 连接信息

### 端口

- 主库: `localhost:5432`
- 从库1: `localhost:5433`
- 从库2: `localhost:5434`
- PgBouncer: `localhost:6432`

### 12个微服务数据库

| 数据库 | 用户名 | 密码 |
|-------|--------|------|
| auth_service | auth_user | auth_pass_2024 |
| book_service | book_user | book_pass_2024 |
| reader_service | reader_user | reader_pass_2024 |
| circulation_service | circulation_user | circulation_pass_2024 |
| system_service | system_user | system_pass_2024 |
| recommend_service | recommend_user | recommend_pass_2024 |
| nlp_service | nlp_user | nlp_pass_2024 |
| vision_service | vision_user | vision_pass_2024 |
| analytics_service | analytics_user | analytics_pass_2024 |
| notification_service | notification_user | notification_pass_2024 |
| file_service | file_user | file_pass_2024 |
| search_service | search_user | search_pass_2024 |

---

## 应用程序连接

### Node.js (pg)

```javascript
const { Pool } = require('pg');

const pool = new Pool({
  host: 'localhost',
  port: 6432,  // 使用PgBouncer
  database: 'auth_service',
  user: 'auth_user',
  password: 'auth_pass_2024',
  max: 20,
  idleTimeoutMillis: 30000,
});
```

### Python (psycopg2)

```python
import psycopg2

conn = psycopg2.connect(
    host="localhost",
    port=6432,  # 使用PgBouncer
    database="auth_service",
    user="auth_user",
    password="auth_pass_2024"
)
```

### Java (JDBC)

```java
String url = "jdbc:postgresql://localhost:6432/auth_service";
Properties props = new Properties();
props.setProperty("user", "auth_user");
props.setProperty("password", "auth_pass_2024");
Connection conn = DriverManager.getConnection(url, props);
```

### Go (pgx)

```go
import "github.com/jackc/pgx/v5"

conn, err := pgx.Connect(context.Background(),
    "postgres://auth_user:auth_pass_2024@localhost:6432/auth_service")
```

---

## 生产环境配置

### 1. 修改密码

```bash
vim .env
```

修改以下变量：
- `POSTGRES_PASSWORD`
- `REPLICATION_PASSWORD`
- 各微服务用户密码

### 2. 限制访问IP

编辑 `pg_hba.conf`，将 `0.0.0.0/0` 改为具体IP段：

```conf
host all all 10.0.1.0/24 scram-sha-256
```

### 3. 启用SSL

在 `postgresql.conf` 中启用SSL：

```ini
ssl = on
ssl_cert_file = '/path/to/server.crt'
ssl_key_file = '/path/to/server.key'
```

---

## 故障处理

### 集群无法启动

```bash
# 查看日志
docker-compose logs

# 重新初始化
./stop.sh --clean
./start.sh
```

### 从库复制异常

```bash
# 检查复制状态
docker exec gcrf-postgres-primary psql -U postgres -c "SELECT * FROM pg_stat_replication;"

# 重建从库
docker-compose stop postgres-replica1
docker volume rm gcrf-postgres-replica1-data
docker-compose up -d postgres-replica1
```

---

## 更多帮助

详细文档请查看: [README.md](README.md)

相关问题请联系后端架构团队。
