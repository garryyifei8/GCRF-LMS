package com.gcrf.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis配置类 - Gateway限流与Token黑名单存储
 *
 * @author GCRF Team
 * @date 2025-12-01
 */
@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    /**
     * 创建Redisson客户端Bean
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        log.info("初始化Gateway Redisson客户端: {}:{}, database={}", redisHost, redisPort, redisDatabase);

        Config config = new Config();
        String address = String.format("redis://%s:%d", redisHost, redisPort);

        config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisDatabase)
                .setPassword(redisPassword.isEmpty() ? null : redisPassword)
                .setConnectionPoolSize(64)
                .setConnectionMinimumIdleSize(10)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(10000)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        RedissonClient client = Redisson.create(config);
        log.info("Gateway Redisson客户端初始化完成");

        return client;
    }
}
