package com.gcrf.library.opac;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.gcrf.library")
@MapperScan("com.gcrf.library.opac.mapper")
public class OpacServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpacServiceApplication.class, args);
    }
}
