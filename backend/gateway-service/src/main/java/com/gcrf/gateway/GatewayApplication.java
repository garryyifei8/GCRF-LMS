package com.gcrf.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * API网关服务启动类
 *
 * @author GCRF Team
 * @date 2025-10-12
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"com.gcrf.gateway", "com.gcrf.library.common"})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("========================================");
        System.out.println("API网关服务启动成功！");
        System.out.println("网关地址: http://localhost:8080");
        System.out.println("========================================");
    }
}
