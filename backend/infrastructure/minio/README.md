# MinIO 对象存储服务

## 服务说明

MinIO是一个高性能的对象存储服务，兼容Amazon S3 API，用于存储图书馆系统的各类文件。

## 配置说明

### 访问信息
- **API地址**: http://localhost:9000
- **Console地址**: http://localhost:9001
- **用户名**: minioadmin
- **密码**: gcrf_minio_2024

### Bucket列表
- **avatars**: 用户头像（公开访问）
- **covers**: 图书封面（公开访问）
- **documents**: 文档资料（私有）
- **backups**: 系统备份（私有）

## 快速启动

```bash
# 启动MinIO服务
cd backend/infrastructure/minio
docker-compose up -d

# 检查服务状态
docker-compose ps

# 查看日志
docker logs gcrf-minio
```

## 验证部署

### 1. 检查服务健康状态
```bash
curl http://localhost:9000/minio/health/live
```

### 2. 访问Web控制台
浏览器访问: http://localhost:9001
- 用户名: minioadmin
- 密码: gcrf_minio_2024

### 3. 查看Bucket列表
```bash
# 使用mc客户端
docker run --rm --network gcrf-network \
  minio/mc:latest \
  alias set myminio http://minio:9000 minioadmin gcrf_minio_2024

docker run --rm --network gcrf-network \
  minio/mc:latest \
  ls myminio
```

## Spring Boot集成

### 1. 添加依赖
```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.7</version>
</dependency>
```

### 2. 配置文件
```yaml
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: gcrf_minio_2024
  bucket:
    avatars: avatars
    covers: covers
    documents: documents
    backups: backups
```

### 3. Java配置类示例
```java
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
```

### 4. 文件上传示例
```java
@Service
public class FileService {

    @Autowired
    private MinioClient minioClient;

    public String uploadFile(String bucketName, String objectName,
                            InputStream inputStream, String contentType)
            throws Exception {
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .stream(inputStream, inputStream.available(), -1)
                .contentType(contentType)
                .build()
        );

        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .object(objectName)
                .expiry(7, TimeUnit.DAYS)
                .build()
        );
    }
}
```

## API使用示例

### 上传文件
```bash
curl -X PUT http://localhost:9000/covers/book-001.jpg \
  -H "Authorization: AWS4-HMAC-SHA256 ..." \
  --data-binary @book-cover.jpg
```

### 下载文件
```bash
curl http://localhost:9000/covers/book-001.jpg -O
```

### 删除文件
```bash
curl -X DELETE http://localhost:9000/covers/book-001.jpg \
  -H "Authorization: AWS4-HMAC-SHA256 ..."
```

## 最佳实践

### 1. 文件命名规范
- 使用UUID作为文件名前缀，避免重名
- 保留原始文件扩展名
- 示例: `{uuid}_{timestamp}.{ext}`

### 2. 访问权限管理
- 头像和封面设置为公开访问
- 敏感文档设置为私有访问
- 使用预签名URL提供临时访问

### 3. 性能优化
- 使用分块上传处理大文件
- 启用CDN加速静态资源访问
- 设置合理的缓存策略

### 4. 备份策略
- 定期备份bucket数据
- 使用版本控制功能
- 配置生命周期策略清理过期文件

## 监控和维护

### 查看存储使用情况
```bash
docker exec gcrf-minio mc admin info myminio
```

### 查看Bucket统计
```bash
docker exec gcrf-minio mc du myminio/avatars
docker exec gcrf-minio mc du myminio/covers
```

### 清理过期文件
```bash
# 设置生命周期规则，自动删除30天前的临时文件
docker exec gcrf-minio mc ilm add \
  --expiry-days 30 \
  myminio/temp
```

## 常见问题

### 1. 文件上传失败
- 检查网络连接
- 确认access-key和secret-key正确
- 检查bucket是否存在

### 2. 无法访问文件
- 检查bucket访问策略
- 确认文件路径正确
- 查看MinIO日志

### 3. 磁盘空间不足
- 清理不必要的文件
- 增加存储卷大小
- 配置生命周期策略

## 安全建议

1. **生产环境**:
   - 修改默认密码
   - 启用HTTPS
   - 限制网络访问
   - 启用审计日志

2. **访问控制**:
   - 使用IAM策略管理权限
   - 最小权限原则
   - 定期审计访问日志

3. **数据保护**:
   - 启用版本控制
   - 配置备份策略
   - 使用加密存储

## 相关链接

- [MinIO官方文档](https://min.io/docs/minio/linux/index.html)
- [MinIO Java SDK](https://min.io/docs/minio/linux/developers/java/minio-java.html)
- [AWS S3 API兼容性](https://docs.aws.amazon.com/AmazonS3/latest/API/Welcome.html)
