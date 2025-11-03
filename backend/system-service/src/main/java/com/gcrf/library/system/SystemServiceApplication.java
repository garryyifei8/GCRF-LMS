package com.gcrf.library.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 系统服务启动类
 *
 * @author GCRF Team
 * @since 2025-10-13
 */
@SpringBootApplication(scanBasePackages = {
    "com.gcrf.library.system",
    "com.gcrf.library.common"
})
@EnableDiscoveryClient
@MapperScan("com.gcrf.library.system.mapper")
public class SystemServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemServiceApplication.class, args);
        System.out.println("""

            ====================================================================================================
                系统服务(System Service)启动成功！

                API 文档: http://localhost:8084/doc.html
                健康检查: http://localhost:8084/actuator/health

                Sprint 3 - Phase 1: 核心业务服务完善
            ====================================================================================================
            """);
    }
}
