package com.gcrf.library.recommend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis缓存配置
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key使用String序列化
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value使用JSON序列化
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 默认缓存配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // 各缓存的自定义配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 推荐结果缓存：1小时
        cacheConfigurations.put("recommendations", defaultConfig.entryTtl(Duration.ofHours(1)));

        // 热门图书缓存：30分钟
        cacheConfigurations.put("popular-books", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // 相似图书缓存：2小时
        cacheConfigurations.put("similar-books", defaultConfig.entryTtl(Duration.ofHours(2)));

        // 用户相似度缓存：24小时
        cacheConfigurations.put("user-similarity", defaultConfig.entryTtl(Duration.ofHours(24)));

        // 物品相似度缓存：24小时
        cacheConfigurations.put("item-similarity", defaultConfig.entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
