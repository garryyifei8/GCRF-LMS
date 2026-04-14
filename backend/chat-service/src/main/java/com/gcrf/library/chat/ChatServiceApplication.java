package com.gcrf.library.chat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智能问答服务启动类
 *
 * 提供基于知识库的图书馆FAQ问答功能：
 * - 意图识别与实体提取
 * - FAQ知识库匹配
 * - 会话上下文管理
 * - 热门问题统计
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@SpringBootApplication(scanBasePackages = {
    "com.gcrf.library.chat",
    "com.gcrf.library.common"
})
@EnableDiscoveryClient
@EnableScheduling
@MapperScan("com.gcrf.library.chat.mapper")
public class ChatServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }
}
