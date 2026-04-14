package com.gcrf.library.recommend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 图书推荐服务启动类
 *
 * 提供基于协同过滤的图书推荐功能：
 * - User-based CF: 基于用户借阅行为相似度推荐
 * - Item-based CF: 基于图书被借阅模式相似度推荐
 * - Popular: 热门图书推荐
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
@SpringBootApplication(scanBasePackages = {
    "com.gcrf.library.recommend",
    "com.gcrf.library.common"
})
@EnableDiscoveryClient
@EnableScheduling
@MapperScan("com.gcrf.library.recommend.mapper")
public class RecommendServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecommendServiceApplication.class, args);
    }
}
