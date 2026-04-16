package com.gcrf.library.book.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import org.springframework.context.annotation.Profile;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis缓存配置
 *
 * @author GCRF Team
 */
@Configuration
@EnableCaching
@Profile("!test")
public class CacheConfig {

    /**
     * 缓存名称常量
     */
    public static final String CACHE_BOOK = "book";
    public static final String CACHE_BOOK_DETAIL = "book:detail";
    public static final String CACHE_CATEGORY = "category";
    public static final String CACHE_CATEGORY_TREE = "category:tree";
    public static final String CACHE_POPULAR_BOOKS = "popular:books";
    public static final String CACHE_ISBN_LOOKUP = "isbn:lookup";

    /**
     * 缓存过期时间
     */
    private static final Duration BOOK_CACHE_TTL = Duration.ofHours(1);
    private static final Duration CATEGORY_CACHE_TTL = Duration.ofHours(6);
    private static final Duration POPULAR_BOOKS_TTL = Duration.ofMinutes(30);
    private static final Duration ISBN_LOOKUP_TTL = Duration.ofHours(24);
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 创建ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // 使用JSON序列化
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 设置key和value的序列化规则
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 创建ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 默认配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        // 针对不同缓存设置不同的过期时间
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(CACHE_BOOK, defaultConfig.entryTtl(BOOK_CACHE_TTL));
        cacheConfigurations.put(CACHE_BOOK_DETAIL, defaultConfig.entryTtl(BOOK_CACHE_TTL));
        cacheConfigurations.put(CACHE_CATEGORY, defaultConfig.entryTtl(CATEGORY_CACHE_TTL));
        cacheConfigurations.put(CACHE_CATEGORY_TREE, defaultConfig.entryTtl(CATEGORY_CACHE_TTL));
        cacheConfigurations.put(CACHE_POPULAR_BOOKS, defaultConfig.entryTtl(POPULAR_BOOKS_TTL));
        cacheConfigurations.put(CACHE_ISBN_LOOKUP, defaultConfig.entryTtl(ISBN_LOOKUP_TTL));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
