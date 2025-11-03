package com.gcrf.library.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j配置类
 *
 * @author 张三
 * @date 2025-10-11
 */
@Configuration
public class Knife4jConfiguration {

    @Value("${spring.application.name:library-service}")
    private String applicationName;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName + " API文档")
                        .version("1.0.0")
                        .description("国创睿峰智能图书馆管理系统 - " + applicationName)
                        .contact(new Contact()
                                .name("国创睿峰科技")
                                .email("support@gcrf.com")
                                .url("https://www.gcrf.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
