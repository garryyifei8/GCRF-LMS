package com.gcrf.library.book.config;

import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO配置
 *
 * @author GCRF Team
 * @date 2025-11-04
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "minio")
@Data
public class MinioConfig {

    private String endpoint = "http://localhost:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String bucketCovers = "library-covers";
    private String bucketPdfs = "library-pdfs";
    private Long maxCoverSize = 5242880L; // 5MB
    private Long maxPdfSize = 52428800L; // 50MB

    @Bean
    public MinioClient minioClient() {
        log.info("Initializing MinIO client: endpoint={}", endpoint);
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
