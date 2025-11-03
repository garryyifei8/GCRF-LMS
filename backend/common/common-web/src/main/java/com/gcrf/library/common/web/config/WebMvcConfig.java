package com.gcrf.library.common.web.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gcrf.library.common.web.interceptor.LogInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

/**
 * Spring MVC配置类
 * <p>
 * 提供以下功能:
 * <ul>
 *   <li>配置Jackson消息转换器，统一JSON序列化/反序列化规则</li>
 *   <li>注册日志拦截器，记录请求和响应信息</li>
 *   <li>设置默认字符编码为UTF-8</li>
 *   <li>配置日期格式和时区</li>
 * </ul>
 *
 * @author 张三
 * @date 2025-10-23
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LogInterceptor logInterceptor;

    /**
     * 配置消息转换器
     * <p>
     * 自定义Jackson ObjectMapper配置:
     * <ul>
     *   <li>支持Java 8时间API (LocalDateTime, LocalDate等)</li>
     *   <li>忽略未知属性，避免反序列化失败</li>
     *   <li>不序列化null值字段</li>
     *   <li>使用统一的日期格式: yyyy-MM-dd HH:mm:ss</li>
     *   <li>设置时区为Asia/Shanghai</li>
     * </ul>
     *
     * @param converters 消息转换器列表
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper = new ObjectMapper();

        // 注册Java 8时间模块
        objectMapper.registerModule(new JavaTimeModule());

        // 忽略未知属性
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 不序列化null值
        objectMapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);

        // 禁用将日期写为时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 设置日期格式
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        // 设置时区
        objectMapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        converter.setObjectMapper(objectMapper);
        converters.add(0, converter);

        log.info("Jackson消息转换器配置完成");
    }

    /**
     * 注册拦截器
     * <p>
     * 注册日志拦截器，记录所有HTTP请求和响应:
     * <ul>
     *   <li>拦截所有路径 (/**)</li>
     *   <li>排除静态资源路径</li>
     *   <li>排除Swagger/Knife4j路径</li>
     *   <li>排除健康检查路径</li>
     * </ul>
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 静态资源
                        "/static/**",
                        "/public/**",
                        "/resources/**",
                        "/webjars/**",
                        // Swagger/Knife4j
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/doc.html",
                        // 健康检查
                        "/actuator/**",
                        "/health/**"
                );

        log.info("日志拦截器注册完成");
    }
}
