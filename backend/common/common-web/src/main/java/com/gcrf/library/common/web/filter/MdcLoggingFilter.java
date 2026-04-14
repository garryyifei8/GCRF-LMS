package com.gcrf.library.common.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * MDC日志上下文过滤器
 * <p>
 * 用于在日志中添加分布式追踪和请求上下文信息:
 * <ul>
 *   <li>traceId - 分布式追踪ID (从请求头获取或自动生成)</li>
 *   <li>spanId - 链路跨度ID</li>
 *   <li>requestId - 请求唯一ID</li>
 *   <li>userId - 当前用户ID (从请求头获取)</li>
 *   <li>clientIp - 客户端IP地址</li>
 *   <li>service - 服务名称</li>
 * </ul>
 * <p>
 * 这些上下文信息会被Logback的JSON格式化器包含在日志输出中，
 * 便于在Loki/ELK等日志系统中进行过滤和关联查询。
 * <p>
 * 请求头约定:
 * <ul>
 *   <li>X-Trace-Id - 分布式追踪ID</li>
 *   <li>X-Span-Id - 链路跨度ID</li>
 *   <li>X-Request-Id - 请求ID</li>
 *   <li>X-User-Id - 用户ID</li>
 * </ul>
 *
 * @author GCRF Team
 * @date 2025-12-01
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    /**
     * MDC键常量 - 追踪ID
     */
    public static final String MDC_TRACE_ID = "traceId";

    /**
     * MDC键常量 - 跨度ID
     */
    public static final String MDC_SPAN_ID = "spanId";

    /**
     * MDC键常量 - 请求ID
     */
    public static final String MDC_REQUEST_ID = "requestId";

    /**
     * MDC键常量 - 用户ID
     */
    public static final String MDC_USER_ID = "userId";

    /**
     * MDC键常量 - 客户端IP
     */
    public static final String MDC_CLIENT_IP = "clientIp";

    /**
     * MDC键常量 - 服务名称
     */
    public static final String MDC_SERVICE = "service";

    /**
     * MDC键常量 - 请求路径
     */
    public static final String MDC_REQUEST_PATH = "requestPath";

    /**
     * MDC键常量 - 请求方法
     */
    public static final String MDC_REQUEST_METHOD = "requestMethod";

    /**
     * 请求头 - 追踪ID
     */
    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    /**
     * 请求头 - 跨度ID
     */
    private static final String HEADER_SPAN_ID = "X-Span-Id";

    /**
     * 请求头 - 请求ID
     */
    private static final String HEADER_REQUEST_ID = "X-Request-Id";

    /**
     * 请求头 - 用户ID
     */
    private static final String HEADER_USER_ID = "X-User-Id";

    /**
     * 服务名称 (从配置注入)
     */
    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 设置MDC上下文
            setupMdcContext(request, response);

            // 继续过滤器链
            filterChain.doFilter(request, response);
        } finally {
            // 清理MDC上下文，避免内存泄漏
            clearMdcContext();
        }
    }

    /**
     * 设置MDC上下文
     *
     * @param request  HTTP请求
     * @param response HTTP响应
     */
    private void setupMdcContext(HttpServletRequest request, HttpServletResponse response) {
        // 追踪ID - 从请求头获取或生成新的
        String traceId = request.getHeader(HEADER_TRACE_ID);
        if (!StringUtils.hasText(traceId)) {
            traceId = generateId();
        }
        MDC.put(MDC_TRACE_ID, traceId);
        response.setHeader(HEADER_TRACE_ID, traceId);

        // 跨度ID - 从请求头获取或生成新的
        String spanId = request.getHeader(HEADER_SPAN_ID);
        if (!StringUtils.hasText(spanId)) {
            spanId = generateShortId();
        }
        MDC.put(MDC_SPAN_ID, spanId);
        response.setHeader(HEADER_SPAN_ID, spanId);

        // 请求ID - 从请求头获取或使用traceId
        String requestId = request.getHeader(HEADER_REQUEST_ID);
        if (!StringUtils.hasText(requestId)) {
            requestId = traceId;
        }
        MDC.put(MDC_REQUEST_ID, requestId);
        response.setHeader(HEADER_REQUEST_ID, requestId);

        // 用户ID - 从请求头获取
        String userId = request.getHeader(HEADER_USER_ID);
        if (StringUtils.hasText(userId)) {
            MDC.put(MDC_USER_ID, userId);
        }

        // 客户端IP
        String clientIp = getClientIp(request);
        MDC.put(MDC_CLIENT_IP, clientIp);

        // 服务名称
        MDC.put(MDC_SERVICE, serviceName);

        // 请求路径
        MDC.put(MDC_REQUEST_PATH, request.getRequestURI());

        // 请求方法
        MDC.put(MDC_REQUEST_METHOD, request.getMethod());
    }

    /**
     * 清理MDC上下文
     */
    private void clearMdcContext() {
        MDC.remove(MDC_TRACE_ID);
        MDC.remove(MDC_SPAN_ID);
        MDC.remove(MDC_REQUEST_ID);
        MDC.remove(MDC_USER_ID);
        MDC.remove(MDC_CLIENT_IP);
        MDC.remove(MDC_SERVICE);
        MDC.remove(MDC_REQUEST_PATH);
        MDC.remove(MDC_REQUEST_METHOD);
    }

    /**
     * 生成唯一ID (32位)
     *
     * @return UUID字符串 (无连字符)
     */
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成短ID (16位)
     *
     * @return 短UUID字符串
     */
    private String generateShortId() {
        return UUID.randomUUID().toString().substring(0, 16).replace("-", "");
    }

    /**
     * 获取客户端真实IP地址
     * <p>
     * 支持代理和负载均衡环境
     *
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isValidIp(ip)) {
            // 多个IP时取第一个 (最初的客户端)
            if (ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        }

        ip = request.getHeader("X-Real-IP");
        if (isValidIp(ip)) {
            return ip;
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (isValidIp(ip)) {
            return ip;
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (isValidIp(ip)) {
            return ip;
        }

        ip = request.getHeader("HTTP_CLIENT_IP");
        if (isValidIp(ip)) {
            return ip;
        }

        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (isValidIp(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }

    /**
     * 检查IP是否有效
     *
     * @param ip IP地址
     * @return true表示有效
     */
    private boolean isValidIp(String ip) {
        return StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip);
    }

    /**
     * 设置当前用户ID到MDC
     * <p>
     * 可以在认证成功后调用此方法
     *
     * @param userId 用户ID
     */
    public static void setUserId(String userId) {
        if (StringUtils.hasText(userId)) {
            MDC.put(MDC_USER_ID, userId);
        }
    }

    /**
     * 获取当前追踪ID
     *
     * @return 追踪ID
     */
    public static String getTraceId() {
        return MDC.get(MDC_TRACE_ID);
    }

    /**
     * 获取当前请求ID
     *
     * @return 请求ID
     */
    public static String getRequestId() {
        return MDC.get(MDC_REQUEST_ID);
    }
}
