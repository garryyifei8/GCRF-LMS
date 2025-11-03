package com.gcrf.library.circulation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 流通管理服务启动类
 *
 * @author GCRF Team
 * @date 2025-10-12
 */
@SpringBootApplication(scanBasePackages = "com.gcrf.library")
@EnableDiscoveryClient
@EnableFeignClients
public class CirculationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CirculationServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("流通管理服务启动成功！");
        System.out.println("API文档地址: http://localhost:8083/doc.html");
        System.out.println("========================================");
    }
}
