package com.gcrf.library.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 认证授权服务启动类
 *
 * @author 张三
 * @date 2025-10-11
 */
@SpringBootApplication(scanBasePackages = "com.gcrf.library")
@EnableDiscoveryClient
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("认证授权服务启动成功！");
        System.out.println("API文档地址: http://localhost:8081/doc.html");
        System.out.println("========================================");
    }
}
