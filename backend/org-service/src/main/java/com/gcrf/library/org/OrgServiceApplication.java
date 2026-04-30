package com.gcrf.library.org;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 组织树服务启动类
 *
 * @author GCRF Team
 * @since 2025-04-30
 */
@SpringBootApplication(scanBasePackages = {"com.gcrf.library.org", "com.gcrf.library.common"})
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.gcrf.library.org.mapper")
public class OrgServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrgServiceApplication.class, args);
    }
}
