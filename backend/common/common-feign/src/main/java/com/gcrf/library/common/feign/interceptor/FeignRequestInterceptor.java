package com.gcrf.library.common.feign.interceptor;

import com.gcrf.library.common.feign.constant.FeignConstants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Feign请求拦截器
 * 用于在服务间调用时传递请求头（认证信息、追踪ID等）
 *
 * @author GCRF Team
 * @since 2025-12-01
 */
@Slf4j
public class FeignRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // 标记请求来源为Feign调用
        template.header(FeignConstants.HEADER_SOURCE, FeignConstants.SOURCE_FEIGN);

        // 获取当前请求上下文
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            // 如果没有请求上下文（如异步调用），生成新的追踪ID
            String traceId = generateTraceId();
            template.header(FeignConstants.HEADER_TRACE_ID, traceId);
            log.debug("Feign请求(无上下文): url={}, traceId={}",
                    template.url(), traceId);
            return;
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) requestAttributes;
        HttpServletRequest request = attributes.getRequest();

        // 传递Authorization头
        String authorization = request.getHeader(FeignConstants.HEADER_AUTHORIZATION);
        if (authorization != null && !authorization.isEmpty()) {
            template.header(FeignConstants.HEADER_AUTHORIZATION, authorization);
            log.debug("Feign传递Authorization头");
        }

        // 传递用户ID头
        String userId = request.getHeader(FeignConstants.HEADER_USER_ID);
        if (userId != null && !userId.isEmpty()) {
            template.header(FeignConstants.HEADER_USER_ID, userId);
        }

        // 传递用户名头
        String username = request.getHeader(FeignConstants.HEADER_USERNAME);
        if (username != null && !username.isEmpty()) {
            template.header(FeignConstants.HEADER_USERNAME, username);
        }

        // 传递租户ID头
        String tenantId = request.getHeader(FeignConstants.HEADER_TENANT_ID);
        if (tenantId != null && !tenantId.isEmpty()) {
            template.header(FeignConstants.HEADER_TENANT_ID, tenantId);
        }

        // 传递或生成追踪ID
        String traceId = request.getHeader(FeignConstants.HEADER_TRACE_ID);
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
        }
        template.header(FeignConstants.HEADER_TRACE_ID, traceId);

        log.debug("Feign请求: url={}, method={}, traceId={}",
                template.url(), template.method(), traceId);
    }

    /**
     * 生成追踪ID
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
