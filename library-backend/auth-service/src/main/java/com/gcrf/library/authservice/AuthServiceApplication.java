package com.gcrf.library.authservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 认证服务启动类
 *
 * @author 国创睿峰
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.gcrf.library.api")
@MapperScan("com.gcrf.library.authservice.mapper")
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
        System.out.println("====================================");
        System.out.println("====== 认证服务启动成功! ==========");
        System.out.println("====== Auth Service Started! =======");
        System.out.println("====================================");
    }
}
