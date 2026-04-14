package com.gcrf.library.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 数据分析服务启动类
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@SpringBootApplication(scanBasePackages = {"com.gcrf.library.analytics", "com.gcrf.library.common"})
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
public class AnalyticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}
