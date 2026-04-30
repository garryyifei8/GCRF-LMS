package com.gcrf.library.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关启动类
 *
 * @author 国创睿峰
 */
@EnableDiscoveryClient
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("====================================");
        System.out.println("========== 网关启动成功! ==========");
        System.out.println("========== Gateway Started! ========");
        System.out.println("====================================");
    }
}
