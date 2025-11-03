package com.gcrf.library.common;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Test application for MyBatis configuration tests
 *
 * @author Claude Code
 * @date 2025-10-27
 */
@SpringBootApplication
@MapperScan("com.gcrf.library.common.integration.mapper")
public class TestMybatisApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestMybatisApplication.class, args);
    }
}
