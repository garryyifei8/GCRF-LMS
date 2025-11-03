package com.gcrf.library.notification;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 通知服务启动类
 *
 * @author GCRF Team
 * @since 2025-10-13
 */
@SpringBootApplication(scanBasePackages = {
    "com.gcrf.library.notification",
    "com.gcrf.library.common"
})
@EnableDiscoveryClient
@MapperScan("com.gcrf.library.notification.mapper")
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
        System.out.println("""

            ====================================================================================================
                通知服务(Notification Service)启动成功！

                API 文档: http://localhost:8085/doc.html
                健康检查: http://localhost:8085/actuator/health

                Sprint 3 - Phase 1: 核心业务服务完善
            ====================================================================================================
            """);
    }
}
