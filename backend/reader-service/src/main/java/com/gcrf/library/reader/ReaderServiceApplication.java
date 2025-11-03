package com.gcrf.library.reader;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 读者服务启动类
 *
 * @author GCRF Team
 * @since 2025-10-13
 */
@SpringBootApplication(scanBasePackages = {
    "com.gcrf.library.reader",
    "com.gcrf.library.common"
})
@EnableDiscoveryClient
@MapperScan("com.gcrf.library.reader.mapper")
public class ReaderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReaderServiceApplication.class, args);
        System.out.println("""

            ====================================================================================================
                读者服务(Reader Service)启动成功！

                API 文档: http://localhost:8083/doc.html
                健康检查: http://localhost:8083/actuator/health

                Sprint 3 - Phase 1: 核心业务服务完善
            ====================================================================================================
            """);
    }
}
