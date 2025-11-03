# Nacos 服务注册与配置中心

## 概述

Nacos (Dynamic Naming and Configuration Service) 是阿里巴巴开源的服务注册与配置中心，为微服务架构提供服务发现、配置管理和服务管理能力。

## 架构说明

本项目采用 Nacos 2.2.3 版本，使用 MySQL 8.0 作为配置存储，支持以下功能：

- **服务注册与发现**: 微服务自动注册，实现服务间调用
- **配置管理**: 集中管理所有微服务的配置
- **命名空间隔离**: dev、test、prod 三个环境隔离
- **权限认证**: 启用用户认证，保障安全性

## 快速启动

### 1. 启动 Nacos 服务

```bash
# 进入Nacos目录
cd backend/infrastructure/nacos

# 启动服务（首次启动会自动初始化MySQL数据库）
docker-compose up -d

# 查看启动日志
docker-compose logs -f nacos-server

# 等待服务就绪（约30-60秒）
# 看到 "Nacos started successfully" 表示启动成功
```

### 2. 验证服务状态

```bash
# 检查服务健康状态
curl http://localhost:8848/nacos/v1/console/health/readiness

# 预期返回: "SUCCESS"
```

### 3. 创建命名空间

```bash
# 执行命名空间创建脚本
./create-namespace.sh

# 脚本会自动创建以下命名空间:
# - dev (开发环境)
# - test (测试环境)
# - prod (生产环境)
```

### 4. 访问 Nacos 控制台

- **URL**: http://localhost:8848/nacos
- **用户名**: nacos
- **密码**: nacos
- **默认命名空间**: public

## 命名空间说明

| 命名空间ID | 名称 | 用途 | 配置示例 |
|-----------|------|------|---------|
| dev | 开发环境 | 本地开发和调试 | 连接本地MySQL/Redis |
| test | 测试环境 | 功能测试和集成测试 | 连接测试服务器 |
| prod | 生产环境 | 正式上线使用 | 连接生产服务器 |
| public | 公共环境 | 默认命名空间 | 不建议使用 |

## 微服务配置

### 1. 添加 Nacos 依赖

在微服务的 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
```

### 2. 配置 application.yml

```yaml
spring:
  application:
    name: your-service-name
  cloud:
    nacos:
      # 服务注册配置
      discovery:
        server-addr: localhost:8848
        namespace: dev  # 命名空间ID
        group: DEFAULT_GROUP
        username: nacos
        password: nacos
      # 配置中心配置
      config:
        server-addr: localhost:8848
        namespace: dev
        group: DEFAULT_GROUP
        file-extension: yaml
        username: nacos
        password: nacos
        refresh-enabled: true  # 支持动态刷新
```

### 3. 在 Nacos 中创建配置

登录 Nacos 控制台，选择对应的命名空间，创建配置：

**配置示例**:
- **Data ID**: `your-service-name-dev.yaml`
- **Group**: `DEFAULT_GROUP`
- **配置格式**: `YAML`

```yaml
server:
  port: 8081

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/library_auth?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password

# JWT配置
jwt:
  secret: your_secret_key_here
  expiration: 7200  # 2小时
  refresh-expiration: 604800  # 7天
```

## 服务注册列表

以下是所有需要注册到 Nacos 的微服务：

| 服务名 | 端口 | 说明 |
|--------|------|------|
| library-gateway | 8080 | API网关 |
| auth-service | 8081 | 认证服务 |
| book-service | 8082 | 图书服务 |
| circulation-service | 8083 | 流通服务 |
| reader-service | 8084 | 读者服务 |
| system-service | 8085 | 系统服务 |
| recommend-service | 8086 | 推荐服务 |
| nlp-service | 8087 | NLP服务 |
| vision-service | 8088 | 视觉识别服务 |
| analytics-service | 8089 | 数据分析服务 |
| notification-service | 8090 | 通知服务 |
| file-service | 8091 | 文件服务 |
| search-service | 8092 | 搜索服务 |

## 配置管理最佳实践

### 1. 配置分层

建议将配置分为以下层次：

- **公共配置** (shared-config.yaml): 所有服务共享的配置
- **服务专属配置** (service-name-{env}.yaml): 每个服务的专属配置
- **本地配置** (application-local.yml): 本地开发覆盖配置

### 2. 配置优先级

配置加载优先级（从高到低）：

1. 本地配置文件 (application-local.yml)
2. Nacos 配置中心 (service-name-{env}.yaml)
3. Nacos 公共配置 (shared-config.yaml)
4. 默认配置 (application.yml)

### 3. 敏感信息加密

对于数据库密码、API密钥等敏感信息，建议使用 Nacos 的加密功能：

```bash
# 使用Nacos提供的加密工具
# 或使用Jasypt等第三方加密库
```

## 动态配置刷新

### 1. 启用配置刷新

在需要动态刷新的配置类上添加 `@RefreshScope` 注解：

```java
@RestController
@RefreshScope
public class ConfigController {

    @Value("${custom.property}")
    private String customProperty;

    @GetMapping("/config")
    public String getConfig() {
        return customProperty;
    }
}
```

### 2. 配置监听

可以通过 `@NacosValue` 注解实现配置变更监听：

```java
@NacosValue(value = "${custom.property}", autoRefreshed = true)
private String customProperty;
```

## 运维管理

### 1. 启动/停止服务

```bash
# 启动服务
docker-compose up -d

# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 查看日志
docker-compose logs -f nacos-server
```

### 2. 数据备份

```bash
# 备份MySQL数据
docker exec nacos-mysql mysqldump -unacos -pnacos123456 nacos > nacos_backup_$(date +%Y%m%d).sql

# 恢复MySQL数据
docker exec -i nacos-mysql mysql -unacos -pnacos123456 nacos < nacos_backup_20251011.sql
```

### 3. 性能优化

**JVM参数调整** (适用于生产环境):

在 `docker-compose.yml` 中修改：

```yaml
environment:
  JVM_XMS: 1g      # 初始堆内存
  JVM_XMX: 1g      # 最大堆内存
  JVM_XMN: 512m    # 新生代内存
```

**MySQL连接池优化**:

```yaml
MYSQL_SERVICE_DB_PARAM: >-
  characterEncoding=utf8
  &connectTimeout=10000
  &socketTimeout=30000
  &autoReconnect=true
  &useSSL=false
  &allowPublicKeyRetrieval=true
  &maxPoolSize=50
  &minPoolSize=10
```

### 4. 集群部署

生产环境建议使用集群模式（3节点以上）。修改 `docker-compose.yml`:

```yaml
environment:
  MODE: cluster  # 修改为cluster模式
  NACOS_SERVERS: nacos1:8848 nacos2:8848 nacos3:8848
```

详细集群部署文档参考：https://nacos.io/zh-cn/docs/cluster-mode-quick-start.html

## 常见问题

### 1. Nacos启动失败

**现象**: 容器启动后立即退出

**原因**:
- MySQL未就绪
- 数据库连接配置错误
- 端口被占用

**解决**:
```bash
# 查看日志
docker-compose logs nacos-server

# 检查MySQL状态
docker-compose ps nacos-mysql

# 检查端口占用
lsof -i :8848
```

### 2. 服务注册失败

**现象**: 微服务启动成功，但Nacos控制台看不到服务

**原因**:
- Nacos地址配置错误
- 命名空间配置错误
- 认证信息错误

**解决**:
```yaml
# 检查配置是否正确
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848  # 确认地址正确
        namespace: dev                # 确认命名空间存在
        username: nacos
        password: nacos
```

### 3. 配置无法刷新

**现象**: 修改Nacos配置后，微服务未生效

**原因**:
- 未添加 `@RefreshScope` 注解
- 配置监听未启用

**解决**:
```java
@RefreshScope  // 添加此注解
@RestController
public class YourController {
    @Value("${your.property}")
    private String property;
}
```

### 4. Nacos控制台无法访问

**现象**: http://localhost:8848/nacos 无法打开

**原因**:
- 服务未启动
- 端口映射错误
- 防火墙阻止

**解决**:
```bash
# 检查服务状态
docker-compose ps

# 检查端口映射
docker-compose port nacos-server 8848

# 测试连接
curl http://localhost:8848/nacos/
```

## 监控告警

### 1. 健康检查

```bash
# 检查Nacos服务健康状态
curl http://localhost:8848/nacos/v1/console/health/readiness

# 检查MySQL连接状态
docker exec nacos-mysql mysqladmin ping -h localhost -unacos -pnacos123456
```

### 2. 监控指标

Nacos提供Prometheus监控指标：

```bash
# 访问监控指标
curl http://localhost:8848/nacos/actuator/prometheus
```

### 3. 告警配置

建议配置以下告警：

- Nacos服务不可用
- 配置变更失败
- 服务注册失败
- MySQL连接失败

## 安全加固

### 1. 修改默认密码

登录Nacos控制台后，立即修改默认密码：

```
控制台 -> 权限控制 -> 用户列表 -> nacos用户 -> 修改密码
```

### 2. 启用鉴权

在 `docker-compose.yml` 中确保启用鉴权：

```yaml
NACOS_AUTH_ENABLE: true
NACOS_AUTH_TOKEN: 修改为随机生成的长密钥（≥32位）
```

### 3. 限制访问IP

如果部署在公网，建议使用防火墙限制访问IP：

```bash
# 仅允许内网访问
sudo ufw allow from 192.168.0.0/16 to any port 8848
```

## 版本升级

### 升级步骤

1. **备份数据**
   ```bash
   docker exec nacos-mysql mysqldump -unacos -pnacos123456 nacos > backup.sql
   ```

2. **停止服务**
   ```bash
   docker-compose down
   ```

3. **修改版本号**
   ```yaml
   # 修改docker-compose.yml中的版本
   image: nacos/nacos-server:v2.3.0  # 新版本
   ```

4. **启动新版本**
   ```bash
   docker-compose up -d
   ```

5. **验证服务**
   ```bash
   curl http://localhost:8848/nacos/v1/console/health/readiness
   ```

## 参考文档

- [Nacos官方文档](https://nacos.io/zh-cn/docs/what-is-nacos.html)
- [Spring Cloud Alibaba文档](https://spring-cloud-alibaba-group.github.io/github-pages/hoxton/zh-cn/index.html)
- [Docker Compose文档](https://docs.docker.com/compose/)

## 技术支持

如有问题，请联系：

- **负责人**: 王五（Java高级工程师）
- **项目**: 国创睿峰智能图书馆管理系统
- **创建日期**: 2025-10-11

---

**版本**: v1.0.0
**最后更新**: 2025-10-11
