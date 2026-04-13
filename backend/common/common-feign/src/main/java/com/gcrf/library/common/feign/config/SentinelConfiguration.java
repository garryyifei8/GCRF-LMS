package com.gcrf.library.common.feign.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import java.io.PrintWriter;

/**
 * Sentinel熔断器配置
 *
 * @author GCRF Team
 * @since 2025-12-01
 */
@Slf4j
@Configuration
@ConditionalOnClass(BlockExceptionHandler.class)
@ConditionalOnProperty(prefix = "library.feign.sentinel", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(FeignProperties.class)
public class SentinelConfiguration {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Sentinel异常处理器
     * 统一处理各种Sentinel限流/熔断异常
     */
    @Bean
    @ConditionalOnMissingBean
    public BlockExceptionHandler sentinelBlockExceptionHandler() {
        log.info("初始化Sentinel异常处理器");
        return (request, response, e) -> handleBlockException(request, response, e);
    }

    /**
     * 处理Sentinel限流/熔断异常
     */
    private void handleBlockException(HttpServletRequest request,
                                       HttpServletResponse response,
                                       BlockException e) throws Exception {
        log.warn("Sentinel触发限流/熔断: uri={}, type={}, rule={}",
                request.getRequestURI(),
                e.getClass().getSimpleName(),
                e.getRule());

        Result<Void> result = buildBlockResult(e);

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        try (PrintWriter writer = response.getWriter()) {
            writer.write(objectMapper.writeValueAsString(result));
            writer.flush();
        }
    }

    /**
     * 根据异常类型构建响应结果
     */
    private Result<Void> buildBlockResult(BlockException e) {
        if (e instanceof FlowException) {
            return Result.error(429, "请求过于频繁，请稍后重试");
        }
        if (e instanceof DegradeException) {
            return Result.error(503, "服务暂时不可用，已触发熔断保护");
        }
        if (e instanceof ParamFlowException) {
            return Result.error(429, "参数限流，请稍后重试");
        }
        if (e instanceof SystemBlockException) {
            return Result.error(503, "系统保护规则触发，请稍后重试");
        }
        if (e instanceof AuthorityException) {
            return Result.error(403, "授权规则不通过");
        }
        return Result.error(429, "请求被限流，请稍后重试");
    }
}
