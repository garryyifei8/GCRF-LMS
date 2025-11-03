package com.gcrf.library.book;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 图书管理服务启动类
 *
 * @author GCRF Team
 * @date 2025-10-12
 */
@SpringBootApplication(scanBasePackages = "com.gcrf.library")
@EnableDiscoveryClient
public class BookServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("图书管理服务启动成功！");
        System.out.println("API文档地址: http://localhost:8082/doc.html");
        System.out.println("========================================");
    }
}
